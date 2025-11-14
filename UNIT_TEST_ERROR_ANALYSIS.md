# VulSystem 单元测试错误分析

**分析时间**: 2025-11-13
**测试文件**: `UserTest.java`
**错误数量**: 2个

---

## 📋 错误概览

| 错误 | 测试方法 | 错误类型 | 严重级别 |
|------|--------|--------|--------|
| 错误1 | `loginTest()` | `TooManyResultsException` | 🔴 高 |
| 错误2 | `test()` | `RuntimeException` | 🟡 中 |

---

## 🔴 错误 1: 登录测试 - TooManyResultsException

### 问题描述

```
org.apache.ibatis.exceptions.TooManyResultsException:
Expected one result (or null) to be returned by selectOne(), but found: 3
```

### 根本原因

在 `UserServiceImpl.login()` 第29-30行：
```java
QueryWrapper<User> queryWrapper = new QueryWrapper<>();
queryWrapper.eq("user_name", username);
User user = userMapper.selectOne(queryWrapper);  // ❌ 返回了多个结果
```

**关键问题**:
- 测试代码使用 `username = "test"` 和 `password = "123456"`
- 数据库中存在**多个用户名为 "test" 的用户** (找到了3个)
- `selectOne()` 期望返回**最多1条记录**，但返回了3条记录
- 这违反了单一性约束

### 具体情况分析

从测试代码看：
```java
@Test
public void loginTest() {
    String companyName = "test";  // ❌ 应该是 username，不是 companyName
    String password = "123456";
    UserVO userVO = userService.login(companyName, password);  // 传错参数
}
```

**问题1**: 参数名错误
- 变量定义为 `companyName = "test"`
- 但传给 `login()` 方法，该方法期望的是 `username`
- 意外地匹配了多个 "test" 用户

**问题2**: 数据库设计
- `user_name` 列可能没有 **UNIQUE 约束**
- 允许存在重复的用户名

### 修复方案

**选项 A: 修复测试代码（推荐）**
```java
@Test
public void loginTest() {
    String username = "testUser";  // 使用实际存在的唯一用户名
    String password = "123456";
    UserVO userVO = userService.login(username, password);
    System.out.println(userVO);
}
```

**选项 B: 修复数据库 Schema（根本解决）**
```sql
-- 为 user_name 添加唯一约束
ALTER TABLE user
ADD UNIQUE KEY uk_user_name (user_name);
```

**选项 C: 修复查询逻辑（添加额外条件）**
```java
// 在 UserServiceImpl.login() 中添加 company_id 条件
QueryWrapper<User> queryWrapper = new QueryWrapper<>();
queryWrapper.eq("user_name", username)
            .eq("company_id", companyId);  // 添加公司ID筛选
User user = userMapper.selectOne(queryWrapper);
```

---

## 🟡 错误 2: 注册测试 - RuntimeException

### 问题描述

```
java.lang.RuntimeException: 该邮箱已被注册，请使用其他邮箱
```

### 根本原因

测试代码在 `test()` 方法中：
```java
user.setEmail("test@test.com");  // 邮箱已存在
userService.register(user);      // ❌ 抛出异常
```

**根本问题**:
1. 测试多次运行，同一邮箱被重复注册
2. 注册逻辑正确地检测到重复邮箱并抛出异常
3. 测试没有处理这个异常或使用唯一邮箱

### 修复方案

**选项 A: 使用唯一邮箱（推荐）**
```java
@Test
public void test() {
    User user = new User();
    user.setId(2);
    user.setCompanyId(1);
    user.setEmail("test_" + System.currentTimeMillis() + "@test.com");  // 唯一邮箱
    user.setUserName("testUser_" + System.currentTimeMillis());         // 唯一用户名
    user.setPhone("18888888888");
    user.setPassword("123456");
    user.setCompanyName("test");
    user.setActivationTime(Date.valueOf(LocalDate.now()));
    user.setIsVip(0);
    user.setTeam("team");
    user.setRole("role");
    user.setIsdelete(0);
    user.setIsvalid(0);
    user.setConfirmCode("123456");

    userService.register(user);
    System.out.println("注册成功");
}
```

**选项 B: 清理测试数据**
```java
@Test
public void test() {
    // 先删除测试邮箱
    QueryWrapper<User> deleteQuery = new QueryWrapper<>();
    deleteQuery.eq("email", "test@test.com");
    userService.delete(deleteQuery);  // 需要实现 delete 方法

    // 再执行注册
    User user = new User();
    // ... 设置用户信息
    userService.register(user);
}
```

**选项 C: 测试异常情况**
```java
@Test
public void testDuplicateEmailRegistration() {
    User user = new User();
    user.setEmail("test@test.com");
    // ... 其他字段

    // 期望抛出异常
    assertThrows(RuntimeException.class, () -> {
        userService.register(user);
    }, "该邮箱已被注册，请使用其他邮箱");
}
```

---

## 📝 完整的修复后测试代码

```java
package com.nju.backend;

import com.nju.backend.config.vo.UserVO;
import com.nju.backend.repository.po.User;
import com.nju.backend.service.user.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.Date;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class UserTest {

    @Autowired
    private UserServiceImpl userService;

    /**
     * 测试用户注册 - 使用唯一的邮箱和用户名
     */
    @Test
    public void testRegistration() {
        String timestamp = String.valueOf(System.currentTimeMillis());

        User user = new User();
        user.setCompanyId(1);
        user.setEmail("test_" + timestamp + "@test.com");           // 唯一邮箱
        user.setUserName("testUser_" + timestamp);                   // 唯一用户名
        user.setPhone("18888888888");
        user.setPassword("123456");
        user.setCompanyName("test");
        user.setActivationTime(Date.valueOf(LocalDate.now()));
        user.setIsVip(0);
        user.setTeam("team");
        user.setRole("role");
        user.setIsdelete(0);
        user.setIsvalid(0);
        user.setConfirmCode("123456");

        // 执行注册
        assertDoesNotThrow(() -> userService.register(user));
        System.out.println("✅ 注册成功");
    }

    /**
     * 测试用户登录 - 使用正确的唯一用户名
     */
    @Test
    public void testLogin() {
        String username = "testUser";  // 使用已存在且唯一的用户名
        String password = "123456";

        assertDoesNotThrow(() -> {
            UserVO userVO = userService.login(username, password);
            assertNotNull(userVO, "用户信息不应为空");
            System.out.println("✅ 登录成功: " + userVO);
        });
    }

    /**
     * 测试重复邮箱注册异常
     */
    @Test
    public void testDuplicateEmailException() {
        User user = new User();
        user.setCompanyId(1);
        user.setEmail("test@test.com");  // 已存在的邮箱
        user.setUserName("duplicateTest_" + System.currentTimeMillis());
        user.setPhone("18888888888");
        user.setPassword("123456");
        user.setCompanyName("test");

        // 期望抛出异常
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> userService.register(user));

        assertTrue(exception.getMessage().contains("该邮箱已被注册"),
            "应该返回邮箱已注册的错误消息");
        System.out.println("✅ 异常处理正确: " + exception.getMessage());
    }
}
```

---

## 🔧 根本问题汇总

### 数据库设计问题

1. **缺少唯一约束**
   - `user_name` 列应有 UNIQUE 约束
   - `email` 列应有 UNIQUE 约束

2. **建议的 DDL 修复**
```sql
-- 检查当前约束
SHOW INDEX FROM user;

-- 添加唯一约束
ALTER TABLE user ADD UNIQUE KEY uk_user_name (user_name);
ALTER TABLE user ADD UNIQUE KEY uk_email (email);
```

### 测试设计问题

1. **测试不幂等**
   - 多次运行测试会失败
   - 应该使用唯一标识符或清理数据

2. **缺少异常测试**
   - 没有验证异常情况
   - 应该添加负面测试用例

---

## ✅ 修复检查清单

- [ ] 修改 `UserTest.loginTest()` 使用正确的用户名
- [ ] 修改 `UserTest.test()` 使用唯一的邮箱和用户名
- [ ] 添加 UNIQUE 约束到数据库
- [ ] 添加异常测试用例
- [ ] 验证测试可重复运行
- [ ] 运行 `mvn test` 确保所有测试通过

---

## 📌 推荐修复优先级

**优先级 1 - 立即修复** (5分钟)
- 修改测试代码使用唯一数据
- 运行测试验证

**优先级 2 - 本周修复** (15分钟)
- 在数据库添加 UNIQUE 约束
- 完整测试套件

**优先级 3 - 下周改进** (30分钟)
- 添加完整的异常测试
- 实现测试数据清理机制

---

## 参考

- **测试文件**: `backend/src/test/java/com/nju/backend/UserTest.java`
- **实现文件**: `backend/src/main/java/com/nju/backend/service/user/impl/UserServiceImpl.java`
- **数据库表**: `user` 表

---

**分析完成时间**: 2025-11-13 17:01
**分析工具**: Claude Code
