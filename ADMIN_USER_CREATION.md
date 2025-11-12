# 管理员账户创建指南

## 快速开始

有两种方式可以创建管理员账户：

### 方式 1: 通过 API 脚本（推荐）

```bash
# 使脚本可执行
chmod +x create_admin_user.sh

# 运行脚本
./create_admin_user.sh
```

脚本会自动：
1. 检查后端是否运行
2. 调用注册 API
3. 测试登录是否成功
4. 显示登录凭证

### 方式 2: 直接调用 API

```bash
# 注册管理员账户
curl -X POST http://localhost:8081/user/register \
  -d "username=admin&email=admin@vulsystem.local&password=admin&phone=13800000000"

# 测试登录
curl -X GET http://localhost:8081/user/login \
  -d "username=admin&password=admin"
```

### 方式 3: 直接修改数据库（如果 API 注册失败）

```bash
# 1. 登录 MySQL
mysql -u root -p kulin

# 2. 执行 SQL 脚本
source CREATE_ADMIN_USER.sql;

# 3. 验证
SELECT * FROM user WHERE user_name = 'admin';

# 4. 退出
exit;
```

## 管理员账户信息

创建后的管理员账户信息：

| 字段 | 值 |
|------|-----|
| 用户名 | `admin` |
| 密码 | `admin` |
| 邮箱 | `admin@vulsystem.local` |
| 电话 | `13800000000` |
| 角色 | `admin` |
| 公司 ID | 1 |
| VIP | 是 |
| 激活状态 | 已激活 |

## 验证成功

创建成功的标志：

```bash
# API 返回
{
  "code": 0,
  "msg": "注册成功，请使用用户名和密码登录",
  "ok": true
}

# 登录返回（应该包含用户信息）
{
  "code": 0,
  "msg": "OK",
  "ok": true,
  "data": {
    "id": ...,
    "userName": "admin",
    "email": "admin@vulsystem.local",
    ...
  }
}
```

## 故障排除

### 问题 1: "邮箱已被注册"

**原因**: admin 账户已经存在

**解决方案**:

```sql
-- 查看是否存在
SELECT * FROM user WHERE user_name = 'admin';

-- 如果存在，删除旧账户
DELETE FROM user WHERE user_name = 'admin' AND email = 'admin@vulsystem.local';

-- 然后重新注册或执行 SQL 脚本
```

### 问题 2: "后端服务不可用"

**原因**: Spring Boot 后端未启动或端口错误

**解决方案**:

```bash
# 检查后端是否运行
curl -v http://localhost:8081/user/login

# 如果返回 Connection refused，需要启动后端
# 在 VulSystem 目录下
mvn spring-boot:run -DskipTests

# 或者使用 JAR 文件
java -jar backend/target/backend-0.0.1-SNAPSHOT.jar
```

### 问题 3: "邮箱格式不正确"

**原因**: 使用了无效的邮箱地址

**解决方案**: 使用标准的邮箱格式，如 `admin@example.com`

### 问题 4: 注册成功但登录失败

**原因**: 可能是数据库事务未提交或密码加密问题

**解决方案**:

```bash
# 使用 SQL 脚本直接在数据库中创建账户
mysql -u root -p kulin < CREATE_ADMIN_USER.sql
```

## 修改管理员密码

如果需要修改密码，请参考 `USER_REGISTRATION_FIX.md` 中关于密码加密的说明。

BCrypt 密码加密需要使用 Spring Security 的 `BCryptPasswordEncoder`。

## 删除管理员账户（如需要）

```sql
-- 永久删除
DELETE FROM user WHERE user_name = 'admin';

-- 或者软删除（保留历史记录）
UPDATE user SET isdelete = 1 WHERE user_name = 'admin';
```

## 更多信息

- 用户注册细节: 查看 `USER_REGISTRATION_FIX.md`
- API 文档: 查看 `BUG_FIX_SUMMARY.md`
- 测试脚本: 运行 `test_registration.sh`
