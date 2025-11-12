# 用户注册 Bug 修复方案

## 问题描述

用户在注册时遇到以下现象：
1. 注册接口返回 SQL 约束违反异常：`Duplicate entry '211850116@smail.nju.edu.cn' for key 'user.uk_email'`
2. 前端显示注册成功
3. 用注册的用户名和密码登录时失败

## 根本原因分析

### 原始代码问题

**UserController.java (第 31-57 行):**
```java
// 原始代码 - 缺乏输入验证
userService.register(user);
return RespBean.success();  // 总是返回成功，即使注册失败
```

**UserServiceImpl.java (第 49-53 行):**
```java
// 原始代码 - 没有检查重复
@Override
public void register(User user) {
    String encodedPassword = passwordEncoder.encode(user.getPassword());
    user.setPassword(encodedPassword);
    userMapper.insert(user);  // 直接插入，如果邮箱重复会抛异常
}
```

### 问题链条

```
1. 用户注册时，邮箱已存在于数据库
   ↓
2. userMapper.insert(user) 抛出 SQLIntegrityConstraintViolationException
   ↓
3. UserController 捕获异常并返回 RespBean.success()（错误！）
   ↓
4. 前端收到成功消息
   ↓
5. 用户登录时，因为实际没有插入新用户（异常回滚），所以登录失败
```

### 数据库约束

```sql
-- 用户表 (kulin.user)
CREATE TABLE `user` (
    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `email` VARCHAR(255) NOT NULL,
    ...
    UNIQUE KEY `uk_email` (`email`)  -- 邮箱唯一约束
)
```

邮箱被唯一约束保护，同一邮箱无法注册两次。

## 解决方案

### 1. 改进 UserServiceImpl - 前置检查 (已实现)

```java
@Override
public void register(User user) {
    // 检查邮箱是否已存在
    QueryWrapper<User> emailQuery = new QueryWrapper<>();
    emailQuery.eq("email", user.getEmail());
    User existingUser = userMapper.selectOne(emailQuery);

    if (existingUser != null) {
        throw new RuntimeException("该邮箱已被注册，请使用其他邮箱");
    }

    // 检查用户名是否已存在
    QueryWrapper<User> usernameQuery = new QueryWrapper<>();
    usernameQuery.eq("user_name", user.getUserName());
    User existingUsername = userMapper.selectOne(usernameQuery);

    if (existingUsername != null) {
        throw new RuntimeException("该用户名已被注册，请使用其他用户名");
    }

    // 加密密码并插入
    String encodedPassword = passwordEncoder.encode(user.getPassword());
    user.setPassword(encodedPassword);

    try {
        userMapper.insert(user);
    } catch (Exception e) {
        // 作为最后的安全网
        if (e.getMessage() != null && e.getMessage().contains("Duplicate entry")) {
            if (e.getMessage().contains("uk_email")) {
                throw new RuntimeException("该邮箱已被注册，请使用其他邮箱");
            }
        }
        throw e;
    }
}
```

**优点：**
- 在执行 INSERT 前检查邮箱/用户名是否存在
- 提供清晰的错误消息
- 即使数据库约束有变化也有备用处理

### 2. 改进 UserController - 输入验证和正确的错误返回 (已实现)

```java
@PostMapping("/register")
public RespBean register(
        @RequestParam("username") String name,
        @RequestParam("email") String email,
        @RequestParam("password") String password,
        @RequestParam("phone") String phone){
    try {
        // 参数验证
        if (name == null || name.trim().isEmpty()) {
            return RespBean.error(RespBeanEnum.ERROR, "用户名不能为空");
        }
        if (email == null || email.trim().isEmpty()) {
            return RespBean.error(RespBeanEnum.ERROR, "邮箱不能为空");
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            return RespBean.error(RespBeanEnum.ERROR, "邮箱格式不正确");
        }
        if (password == null || password.trim().isEmpty()) {
            return RespBean.error(RespBeanEnum.ERROR, "密码不能为空");
        }
        if (password.length() < 6) {
            return RespBean.error(RespBeanEnum.ERROR, "密码长度至少为6个字符");
        }
        if (phone == null || phone.trim().isEmpty()) {
            return RespBean.error(RespBeanEnum.ERROR, "电话号码不能为空");
        }

        // 创建用户对象并注册
        User user = new User();
        user.setPassword(password);
        user.setCompanyName("company");
        user.setUserName(name);
        user.setEmail(email);
        user.setPhone(phone);
        user.setRole("user");
        user.setIsVip(0);
        user.setCompanyId(1);  // 所有用户都属于公司ID为1
        // ... 其他字段设置

        userService.register(user);
        return RespBean.success("注册成功，请使用用户名和密码登录");

    } catch (RuntimeException e) {
        // 捕获业务异常，返回明确的错误信息
        return RespBean.error(RespBeanEnum.ERROR, e.getMessage());
    } catch (Exception e) {
        // 捕获其他异常（如数据库异常）
        String errorMsg = "注册失败";
        if (e.getMessage() != null) {
            if (e.getMessage().contains("Duplicate entry")) {
                errorMsg = "注册信息已存在，请检查邮箱或用户名";
            } else if (e.getMessage().contains("Constraint")) {
                errorMsg = "注册信息违反约束条件，请检查输入";
            }
        }
        return RespBean.error(RespBeanEnum.ERROR, errorMsg);
    }
}
```

**改进点：**
- 对所有输入参数进行验证
- 邮箱格式验证
- 密码长度检查
- **关键：异常发生时返回 error，而不是 success**
- 错误消息清晰有用

## 测试步骤

### 测试场景 1：新用户正常注册

```bash
curl -X POST http://localhost:8081/user/register \
  -d "username=testuser&email=test@example.com&password=123456&phone=13800138000"

# 预期返回：
{
  "code": 0,
  "msg": "注册成功，请使用用户名和密码登录",
  "ok": true
}
```

### 测试场景 2：邮箱已存在

```bash
# 第二次用同样的邮箱注册
curl -X POST http://localhost:8081/user/register \
  -d "username=testuser2&email=test@example.com&password=123456&phone=13800138001"

# 预期返回：
{
  "code": 500,
  "msg": "该邮箱已被注册，请使用其他邮箱",
  "ok": false
}
```

### 测试场景 3：无效的邮箱格式

```bash
curl -X POST http://localhost:8081/user/register \
  -d "username=testuser3&email=invalidemail&password=123456&phone=13800138002"

# 预期返回：
{
  "code": 500,
  "msg": "邮箱格式不正确",
  "ok": false
}
```

### 测试场景 4：注册后成功登录

```bash
# 注册成功后
curl -X GET http://localhost:8081/user/login \
  -d "username=testuser&password=123456"

# 预期：返回用户信息和 token
```

## 数据库清理（如需）

如果数据库中有重复的邮箱导致问题，可以执行以下 SQL：

```sql
-- 查看重复邮箱
SELECT email, COUNT(*) as count FROM user
WHERE email IS NOT NULL AND email != ''
GROUP BY email
HAVING COUNT(*) > 1;

-- 删除特定邮箱的旧记录（保留最新的）
DELETE FROM user
WHERE email = '211850116@smail.nju.edu.cn'
  AND id < (
    SELECT MAX(id) FROM (
      SELECT MAX(id) as max_id FROM user
      WHERE email = '211850116@smail.nju.edu.cn'
    ) t
  );

-- 或者清空用户表重新开始
TRUNCATE TABLE user;
```

## 前端应对

前端应该：
1. **检查响应码** - 不要仅凭消息判断成功/失败
2. **检查 `ok` 或 `code` 字段** - 确保业务操作成功
3. **显示返回的 `msg` 信息** - 为用户提供有意义的反馈
4. **失败时不跳转登录页** - 停留在注册页面让用户重试

```javascript
// 错误的做法
fetch('/user/register', {method: 'POST', ...})
  .then(r => r.json())
  .then(data => {
    // 这是错的！不要只看 http 状态码
    window.location.href = '/login';
  });

// 正确的做法
fetch('/user/register', {method: 'POST', ...})
  .then(r => r.json())
  .then(data => {
    if (data.code === 0 && data.ok === true) {
      // 注册成功，跳转登录
      alert(data.msg);  // "注册成功，请使用用户名和密码登录"
      window.location.href = '/login';
    } else {
      // 注册失败，显示错误信息
      alert('注册失败: ' + data.msg);
      // 停留在注册页面
    }
  });
```

## 关键修改总结

| 问题 | 原始代码 | 修复后 |
|------|--------|--------|
| 邮箱重复检查 | 无 | 前置 SELECT 检查 + 异常处理 |
| 用户名重复检查 | 无 | 前置 SELECT 检查 |
| 输入验证 | 无 | 参数非空、格式、长度验证 |
| 错误返回 | 总返回 success | 异常时返回 error |
| 错误消息 | 泛泛而谈 | 具体说明原因 |
| 用户体验 | 显示注册成功但登录失败 | 清晰的错误提示 |

## 验证清单

- [ ] 新用户能成功注册
- [ ] 注册后能用相同用户名和密码登录
- [ ] 重复邮箱不能再次注册，返回明确错误信息
- [ ] 不合法的邮箱格式被拒绝
- [ ] 空密码被拒绝
- [ ] 短密码（<6字符）被拒绝
- [ ] 空用户名被拒绝
- [ ] 前端正确处理失败响应，不跳转登录页
