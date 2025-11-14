# 多语言依赖解析系统 - 测试指南

## 📋 目录
1. [测试前准备](#测试前准备)
2. [快速测试](#快速测试)
3. [详细测试步骤](#详细测试步骤)
4. [测试结果验证](#测试结果验证)
5. [常见问题](#常见问题)

---

## 测试前准备

### 1. 确保服务运行

#### 启动Flask服务
```bash
cd flask-service
python app.py
```

验证Flask服务：
```bash
curl http://localhost:5000/vulnerabilities/test
```

预期响应：
```json
{
    "code": 200,
    "message": "Server is running normally",
    "status": "OK"
}
```

#### 启动Spring Boot服务
```bash
cd backend
mvn spring-boot:run
```

或直接运行编译好的jar：
```bash
java -jar backend/target/backend-0.0.1-SNAPSHOT.jar
```

### 2. 准备测试项目

为每种语言准备一个测试项目：

```
C:/test/
├── java-project/           # Java项目（包含pom.xml）
├── python-project/         # Python项目（包含requirements.txt）
├── go-project/             # Go项目（包含go.mod）
├── rust-project/           # Rust项目（包含Cargo.toml）
├── javascript-project/     # JavaScript项目（包含package.json）
├── php-project/            # PHP项目（包含composer.json）
├── ruby-project/           # Ruby项目（包含Gemfile）
└── erlang-project/         # Erlang项目（包含rebar.config）
```

### 3. 创建测试项目（数据库）

在数据库中创建一个测试项目：

```sql
INSERT INTO project (name, description, language, risk_threshold, is_delete, create_time, file)
VALUES ('测试项目', '用于测试多语言解析', 'java', 0, 0, NOW(), 'C:/test/java-project');
```

记下生成的项目ID，后续测试会用到。

---

## 快速测试

### Windows系统

运行批处理脚本：
```cmd
test_multi_language_parsing.bat
```

### Linux/Mac系统

运行Shell脚本：
```bash
chmod +x test_multi_language_parsing.sh
./test_multi_language_parsing.sh
```

---

## 详细测试步骤

### 方式1：使用REST API测试

#### 步骤1：测试单一语言解析

```bash
# 测试Java项目解析
curl -X POST http://localhost:8081/project/reparse \
  -d "projectId=1" \
  -d "language=java"

# 测试Python项目解析
curl -X POST http://localhost:8081/project/reparse \
  -d "projectId=1" \
  -d "language=python"

# 测试Go项目解析
curl -X POST http://localhost:8081/project/reparse \
  -d "projectId=1" \
  -d "language=go"
```

预期响应：
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "status": "parsing",
        "message": "已触发python项目依赖解析，正在后台处理...",
        "language": "python",
        "projectId": 1,
        "projectName": "测试项目"
    }
}
```

#### 步骤2：测试批量解析

```bash
curl -X POST http://localhost:8081/project/reparse/multiple \
  -d "projectId=1" \
  -d "languages=java,python,go,rust"
```

预期响应：
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "status": "success",
        "message": "成功触发4个语言的解析任务",
        "successCount": 4
    }
}
```

#### 步骤3：直接测试Flask API

```bash
# 测试Python解析API
curl "http://localhost:5000/parse/python_parse?project_folder=C:/test/python-project"

# 测试Go解析API
curl "http://localhost:5000/parse/go_parse?project_folder=C:/test/go-project"

# 测试JavaScript解析API
curl "http://localhost:5000/parse/javascript_parse?project_folder=C:/test/javascript-project"
```

预期响应格式（以Python为例）：
```json
[
    {
        "name": "requests",
        "version": "2.28.0",
        "description": "HTTP library"
    },
    {
        "name": "flask",
        "version": "2.0.1",
        "description": "Web framework"
    }
]
```

### 方式2：使用JUnit测试

运行Java测试类：

```bash
cd backend
mvn test -Dtest=MultiLanguageParsingTest
```

或在IDE中运行 `MultiLanguageParsingTest.java` 中的测试方法。

### 方式3：使用Postman测试

#### 导入Postman Collection

创建以下请求：

1. **测试Java解析**
   - Method: POST
   - URL: `http://localhost:8081/project/reparse`
   - Body (form-data):
     - projectId: 1
     - language: java

2. **测试Python解析**
   - Method: POST
   - URL: `http://localhost:8081/project/reparse`
   - Body (form-data):
     - projectId: 1
     - language: python

3. **测试批量解析**
   - Method: POST
   - URL: `http://localhost:8081/project/reparse/multiple`
   - Body (form-data):
     - projectId: 1
     - languages: java,python,go

---

## 测试结果验证

### 1. 检查Spring Boot日志

查看控制台输出，应该看到类似以下日志：

```
========================================
开始解析PYTHON项目
项目路径: C:/test/python-project
========================================
→ 调用Flask API: http://localhost:5000/parse/python_parse
→ 完整URL: http://localhost:5000/parse/python_parse?project_folder=C%3A%2Ftest%2Fpython-project
✓ API响应接收成功，长度: 1234 字符
✓ 成功解析出依赖库数量: 15
========================================
✓ PYTHON项目解析完成
  总依赖数: 15
  成功插入: 15
  重复跳过: 0
  插入失败: 0
  耗时: 523 ms
========================================
```

### 2. 检查数据库

查询 `white_list` 表：

```sql
-- 查看所有语言的依赖统计
SELECT language, COUNT(*) as count
FROM white_list
WHERE isdelete = 0
GROUP BY language;

-- 查看具体依赖
SELECT id, name, language, file_path, description
FROM white_list
WHERE language = 'python' AND isdelete = 0
LIMIT 10;
```

预期结果示例：

| language   | count |
|------------|-------|
| java       | 25    |
| python     | 15    |
| go         | 30    |
| rust       | 18    |
| javascript | 42    |
| php        | 12    |
| ruby       | 8     |
| erlang     | 5     |

### 3. 验证数据完整性

检查插入的数据是否完整：

```sql
-- 检查必填字段是否有空值
SELECT *
FROM white_list
WHERE name IS NULL OR name = ''
   OR file_path IS NULL OR file_path = ''
   OR language IS NULL OR language = '';

-- 应该返回0条记录
```

---

## 各语言测试案例

### Java项目测试

**测试项目结构**：
```
java-project/
└── pom.xml
```

**pom.xml示例**：
```xml
<project>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <version>2.7.0</version>
        </dependency>
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>8.0.28</version>
        </dependency>
    </dependencies>
</project>
```

**测试命令**：
```bash
curl -X POST http://localhost:8081/project/reparse \
  -d "projectId=1" \
  -d "language=java"
```

**预期结果**：
- white_list表中应该有2条记录
- name字段分别为: spring-boot-starter-web, mysql-connector-java
- language字段为: java

### Python项目测试

**测试项目结构**：
```
python-project/
└── requirements.txt
```

**requirements.txt示例**：
```
requests==2.28.0
flask==2.0.1
numpy==1.23.0
pandas==1.4.2
```

**测试命令**：
```bash
curl -X POST http://localhost:8081/project/reparse \
  -d "projectId=1" \
  -d "language=python"
```

**预期结果**：
- white_list表中应该有4条记录
- name字段分别为: requests, flask, numpy, pandas
- language字段为: python

### Go项目测试

**测试项目结构**：
```
go-project/
├── go.mod
└── go.sum
```

**go.mod示例**：
```go
module example.com/myapp

go 1.20

require (
    github.com/gin-gonic/gin v1.9.0
    github.com/go-sql-driver/mysql v1.7.0
    gorm.io/gorm v1.25.0
)
```

**测试命令**：
```bash
curl -X POST http://localhost:8081/project/reparse \
  -d "projectId=1" \
  -d "language=go"
```

**预期结果**：
- white_list表中应该有3条记录
- name字段包含: github.com/gin-gonic/gin 等
- language字段为: go

### Rust项目测试

**测试项目结构**：
```
rust-project/
└── Cargo.toml
```

**Cargo.toml示例**：
```toml
[package]
name = "my-rust-project"
version = "0.1.0"

[dependencies]
serde = "1.0"
tokio = { version = "1.28", features = ["full"] }
actix-web = "4.3"
```

**测试命令**：
```bash
curl -X POST http://localhost:8081/project/reparse \
  -d "projectId=1" \
  -d "language=rust"
```

**预期结果**：
- white_list表中应该有3条记录
- name字段分别为: serde, tokio, actix-web
- language字段为: rust

### JavaScript项目测试

**测试项目结构**：
```
javascript-project/
└── package.json
```

**package.json示例**：
```json
{
  "name": "my-js-project",
  "dependencies": {
    "express": "^4.18.0",
    "axios": "^1.4.0",
    "lodash": "^4.17.21",
    "moment": "^2.29.4"
  }
}
```

**测试命令**：
```bash
curl -X POST http://localhost:8081/project/reparse \
  -d "projectId=1" \
  -d "language=javascript"
```

**预期结果**：
- white_list表中应该有4条记录
- name字段分别为: express, axios, lodash, moment
- language字段为: javascript

### PHP项目测试

**测试项目结构**：
```
php-project/
└── composer.json
```

**composer.json示例**：
```json
{
  "require": {
    "php": ">=7.4",
    "laravel/framework": "^9.0",
    "guzzlehttp/guzzle": "^7.5"
  }
}
```

**测试命令**：
```bash
curl -X POST http://localhost:8081/project/reparse \
  -d "projectId=1" \
  -d "language=php"
```

### Ruby项目测试

**测试项目结构**：
```
ruby-project/
└── Gemfile
```

**Gemfile示例**：
```ruby
source 'https://rubygems.org'

gem 'rails', '~> 7.0'
gem 'pg', '~> 1.4'
gem 'redis', '~> 5.0'
```

**测试命令**：
```bash
curl -X POST http://localhost:8081/project/reparse \
  -d "projectId=1" \
  -d "language=ruby"
```

### Erlang项目测试

**测试项目结构**：
```
erlang-project/
└── rebar.config
```

**rebar.config示例**：
```erlang
{deps, [
    {cowboy, "2.9.0"},
    {jsx, "3.1.0"}
]}.
```

**测试命令**：
```bash
curl -X POST http://localhost:8081/project/reparse \
  -d "projectId=1" \
  -d "language=erlang"
```

---

## 常见问题

### 问题1：Flask服务连接失败

**症状**：
```
✗ Flask服务连接失败
  错误: Connection refused
```

**解决方案**：
1. 检查Flask服务是否运行：
   ```bash
   netstat -an | findstr 5000
   ```

2. 启动Flask服务：
   ```bash
   cd flask-service
   python app.py
   ```

3. 检查防火墙设置

### 问题2：解析返回空结果

**症状**：
```
⚠ 未解析出任何依赖库
```

**可能原因**：
1. 项目中没有依赖配置文件
2. 依赖配置文件格式不正确
3. Flask解析器未正确实现

**解决方案**：
1. 检查项目是否包含配置文件：
   ```bash
   ls C:/test/python-project
   # 应该看到 requirements.txt
   ```

2. 手动测试Flask接口：
   ```bash
   curl "http://localhost:5000/parse/python_parse?project_folder=C:/test/python-project"
   ```

3. 检查Flask日志输出

### 问题3：数据库写入失败

**症状**：
```
✗ 插入失败: 依赖名称 - Duplicate entry
```

**解决方案**：
1. 检查是否有唯一索引冲突
2. 清理重复数据：
   ```sql
   DELETE FROM white_list
   WHERE id NOT IN (
       SELECT MIN(id)
       FROM white_list
       GROUP BY name, file_path, language
   );
   ```

### 问题4：异步任务未执行

**症状**：日志中没有解析输出

**解决方案**：
1. 检查 `@Async` 注解是否存在
2. 验证 `@EnableAsync` 是否在启动类配置
3. 确认线程池配置正确
4. 增加等待时间后再查询数据库

### 问题5：编码问题

**症状**：依赖名称中文显示乱码

**解决方案**：
1. 确保数据库字符集为UTF-8：
   ```sql
   ALTER TABLE white_list CONVERT TO CHARACTER SET utf8mb4;
   ```

2. 确保Flask返回UTF-8编码：
   ```python
   return jsonify(data), 200, {'Content-Type': 'application/json; charset=utf-8'}
   ```

---

## 性能测试

### 测试大型项目解析

准备一个包含100+依赖的项目，测试解析性能：

```bash
# 记录开始时间
date

# 执行解析
curl -X POST http://localhost:8081/project/reparse \
  -d "projectId=1" \
  -d "language=java"

# 等待完成
sleep 10

# 查看结果
date
mysql -e "SELECT COUNT(*) FROM white_list WHERE language='java';"
```

### 并发测试

同时解析多个项目：

```bash
# 并发解析3个项目
curl -X POST http://localhost:8081/project/reparse -d "projectId=1&language=java" &
curl -X POST http://localhost:8081/project/reparse -d "projectId=2&language=python" &
curl -X POST http://localhost:8081/project/reparse -d "projectId=3&language=go" &

# 等待所有任务完成
wait

# 检查结果
mysql -e "SELECT language, COUNT(*) FROM white_list GROUP BY language;"
```

---

## 测试报告模板

### 测试执行记录

| 测试项 | 语言 | 项目路径 | 依赖数量 | 执行时间 | 状态 | 备注 |
|--------|------|---------|---------|---------|------|------|
| 1 | Java | C:/test/java-project | 25 | 1.2s | ✓ | 正常 |
| 2 | Python | C:/test/python-project | 15 | 0.8s | ✓ | 正常 |
| 3 | Go | C:/test/go-project | 30 | 1.5s | ✓ | 正常 |
| 4 | Rust | C:/test/rust-project | 18 | 1.0s | ✓ | 正常 |
| 5 | JavaScript | C:/test/js-project | 42 | 1.8s | ✓ | 正常 |
| 6 | PHP | C:/test/php-project | 12 | 0.9s | ✓ | 正常 |
| 7 | Ruby | C:/test/ruby-project | 8 | 0.7s | ✓ | 正常 |
| 8 | Erlang | C:/test/erlang-project | 5 | 0.6s | ✓ | 正常 |

### 测试结论

- **总测试数**: 8
- **通过数**: 8
- **失败数**: 0
- **通过率**: 100%

---

## 附录

### A. 测试数据清理脚本

```sql
-- 清理所有测试数据
DELETE FROM white_list WHERE file_path LIKE 'C:/test/%';

-- 清理特定语言的测试数据
DELETE FROM white_list WHERE language = 'java' AND file_path LIKE 'C:/test/%';
```

### B. 快速验证脚本

```bash
#!/bin/bash

# 快速验证所有语言是否正确插入数据库

languages=("java" "python" "go" "rust" "javascript" "php" "ruby" "erlang")

for lang in "${languages[@]}"; do
    count=$(mysql -N -e "SELECT COUNT(*) FROM white_list WHERE language='$lang' AND isdelete=0;")
    echo "$lang: $count 个依赖"
done
```

---

**测试完成后记得清理测试数据，避免影响生产环境！**
