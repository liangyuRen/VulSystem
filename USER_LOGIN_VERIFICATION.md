# 用户验证和登录测试指南

## 概述

这个指南将帮助您：
1. 查看数据库中已注册的用户
2. 用正确的用户名和密码进行登录测试
3. 验证登录接口是否正常工作

## 快速开始

### 方式 1: 直接测试登录（已知用户和密码）

```bash
# 测试单个用户登录
./test_login.sh <username> <password>

# 例子:
./test_login.sh admin admin
./test_login.sh john 123456
```

### 方式 2: 交互式登录测试

```bash
./test_login.sh

# 然后按提示输入用户名和密码
```

### 方式 3: 查看数据库中的用户

```bash
# 查询所有用户信息
mysql -u root -p kulin < query_users.sql

# 或者直接运行 SQL 命令
mysql -u root -p kulin -e "SELECT user_name, email, role FROM user WHERE isdelete = 0;"
```

## 详细步骤

### 步骤 1: 查看数据库中的用户

首先，确认数据库中有哪些用户：

```bash
# 连接到 MySQL
mysql -u root -p

# 然后在 MySQL 提示符中运行:
USE kulin;
SELECT user_name, email, phone, role FROM user WHERE isdelete = 0;
```

**输出示例**:
```
+-----------+---------------------------+----------+-------+
| user_name | email                     | phone    | role  |
+-----------+---------------------------+----------+-------+
| admin     | admin@vulsystem.local     | 13800... | admin |
| user1     | user1@example.com         | 13800... | user  |
| user2     | user2@example.com         | 13800... | user  |
+-----------+---------------------------+----------+-------+
```

### 步骤 2: 确定正确的密码

**重要**: 密码在数据库中是 BCrypt 哈希值，不是明文存储。您需要知道：

1. **对于通过注册 API 创建的用户**:
   - 密码就是注册时输入的明文密码
   - 例如: 如果注册时输入 "password123"，那就用 "password123" 登录

2. **对于通过 SQL 脚本创建的用户**:
   - 如果使用了 `CREATE_ADMIN_USER.sql` 脚本创建的 admin 账户
   - 密码是: `admin`

3. **对于其他用户**:
   - 需要查看注册记录或向用户询问

### 步骤 3: 进行登录测试

使用 test_login.sh 脚本进行测试：

```bash
# 使脚本可执行
chmod +x test_login.sh

# 测试 admin 用户
./test_login.sh admin admin

# 预期输出:
# ✅ 登录成功！
# 用户信息:
#   ID: 1
#   用户名: admin
#   邮箱: admin@vulsystem.local
#   ...
```

### 步骤 4: 验证登录接口响应

成功的登录响应应该包含:

```json
{
  "code": 0,
  "msg": "OK",
  "ok": true,
  "data": {
    "id": 1,
    "userName": "admin",
    "email": "admin@vulsystem.local",
    "phone": "13800000000",
    "role": "admin",
    "companyName": "company",
    "teamName": "admin",
    "companyId": 1,
    "isVip": 1,
    "isvalid": 1,
    "isdelete": 0
  }
}
```

失败的登录响应应该是:

```json
{
  "code": 500,
  "msg": "用户名或密码错误",
  "ok": false
}
```

## 常见场景

### 场景 1: 新注册的用户登录

如果通过注册 API 创建了新用户，使用注册时的凭证进行登录:

```bash
# 假设注册信息:
# 用户名: john
# 密码: john123456

./test_login.sh john john123456
```

### 场景 2: 测试多个用户

```bash
# 创建一个测试脚本来测试多个用户
cat > test_all_users.sh << 'EOF'
#!/bin/bash

# 测试多个用户的登录
USERS=(
    "admin:admin"
    "user1:password1"
    "user2:password2"
)

for user_pair in "${USERS[@]}"; do
    USERNAME=$(echo $user_pair | cut -d: -f1)
    PASSWORD=$(echo $user_pair | cut -d: -f2)

    echo "测试用户: $USERNAME"
    ./test_login.sh "$USERNAME" "$PASSWORD"
    echo ""
done
EOF

chmod +x test_all_users.sh
./test_all_users.sh
```

### 场景 3: 使用 curl 直接调用 API

```bash
# 直接调用登录 API
curl -X GET http://localhost:8081/user/login \
  -d "username=admin&password=admin"

# 查看用户信息
curl -X GET http://localhost:8081/user/info \
  -d "username=admin"
```

## 故障排除

### 问题 1: "后端服务不可用"

**症状**: curl 返回 "Connection refused"

**解决方案**:
```bash
# 检查后端是否运行
curl -v http://localhost:8081/user/login -d "username=test&password=test"

# 如果无法连接，启动后端:
cd backend
mvn spring-boot:run -DskipTests

# 或使用 JAR 文件:
java -jar backend/target/backend-*.jar
```

### 问题 2: "用户名不存在"

**症状**: 登录返回 "用户名不存在"

**解决方案**:
```bash
# 确认用户确实存在
mysql -u root -p kulin -e "SELECT user_name FROM user WHERE user_name = 'john' AND isdelete = 0;"

# 如果没有返回结果，说明用户不存在或被删除了
```

### 问题 3: "用户名或密码错误"

**症状**: 用户存在，但登录失败

**解决方案**:
```bash
# 1. 确认密码是否正确
#    - 对于通过 API 注册的用户，使用注册时的原始密码
#    - 对于通过 SQL 脚本创建的用户，检查脚本中的密码

# 2. 检查数据库中的密码哈希
mysql -u root -p kulin -e "SELECT user_name, password FROM user WHERE user_name = 'john';"

# 3. 检查用户是否被激活
mysql -u root -p kulin -e "SELECT user_name, isvalid, isdelete FROM user WHERE user_name = 'john';"
```

### 问题 4: "数据库连接失败"

**症状**: MySQL 连接失败

**解决方案**:
```bash
# 检查 MySQL 服务状态
sudo systemctl status mysql
# 或
sudo service mysql status

# 启动 MySQL（如果未启动）
sudo systemctl start mysql
# 或
sudo service mysql start

# 验证 MySQL 凭证
mysql -u root -p -e "SELECT 1;"
```

## API 端点详细说明

### 登录接口

**端点**: `/user/login`

**方法**: GET

**参数**:
- `username` (string): 用户名
- `password` (string): 密码

**请求示例**:
```bash
GET /user/login?username=admin&password=admin HTTP/1.1
Host: localhost:8081
```

**响应示例** (成功):
```json
{
  "code": 0,
  "msg": "OK",
  "ok": true,
  "data": {
    "id": 1,
    "userName": "admin",
    "email": "admin@vulsystem.local",
    ...
  }
}
```

**响应示例** (失败):
```json
{
  "code": 500,
  "msg": "用户名或密码错误",
  "ok": false
}
```

### 用户信息接口

**端点**: `/user/info`

**方法**: GET

**参数**:
- `username` (string): 用户名

**请求示例**:
```bash
GET /user/info?username=admin HTTP/1.1
Host: localhost:8081
```

**响应**: 返回用户信息（与登录相同的用户对象）

## 导出用户列表

如果需要生成一个用户列表进行批量测试:

```bash
# 导出为 CSV 格式
mysql -u root -p kulin \
  -e "SELECT user_name, email FROM user WHERE isdelete = 0;" \
  --csv > users.csv

# 导出为 JSON 格式
mysql -u root -p kulin \
  -e "SELECT user_name, email, role FROM user WHERE isdelete = 0 LIMIT 10;" \
  -X > users.xml

# 然后用脚本处理这些文件进行批量测试
```

## 密码重置（如需要）

如果需要重置用户密码:

```bash
# 1. 生成新的 BCrypt 哈希密码（使用 Java）
# 或者使用在线工具: https://www.bcryptcalculator.com/

# 2. 更新数据库
mysql -u root -p kulin -e \
  "UPDATE user SET password = '\$2a\$10\$...' WHERE user_name = 'john';"

# 3. 然后用新密码登录测试
./test_login.sh john newpassword
```

## 测试脚本列表

| 脚本 | 用途 |
|------|------|
| `test_login.sh` | 单个或交互式用户登录测试 |
| `verify_users_and_login.sh` | 查询所有用户并逐个测试登录 |
| `query_users.sql` | 查询数据库中的用户信息 |
| `test_registration.sh` | 测试新用户注册流程 |

## 总结

本指南提供了完整的用户验证和登录测试方法。关键步骤是：

1. ✅ 查看数据库中的用户
2. ✅ 确认用户名和密码
3. ✅ 使用 test_login.sh 进行登录测试
4. ✅ 验证 API 响应是否正确

如有任何问题，请参考故障排除部分。
