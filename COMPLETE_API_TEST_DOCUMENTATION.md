# 后端API完整测试报告

## 测试环境信息
- 测试时间: 2025-11-13
- Flask服务器: http://127.0.0.1:5000 (未运行)
- Spring Boot服务器: http://localhost:8081 (未运行)
- 项目目录: C:\Users\任良玉\Desktop\kuling\VulSystem

## 服务器状态
- Flask 应用状态: ❌ **未运行** (端口5000无响应)
- Spring Boot 应用状态: ❌ **未运行** (端口8081无响应)

---

## Flask API 接口详细测试清单

### 1. 基础服务测试接口

#### 1.1 健康检查
```
GET /vulnerabilities/test
```
**预期行为**: 返回服务器状态
**请求**:
```bash
curl -s http://127.0.0.1:5000/vulnerabilities/test
```
**预期响应 (HTTP 200)**:
```json
{
    "code": 200,
    "message": "Server is running normally",
    "status": "OK"
}
```

---

### 2. 漏洞数据库接口

#### 2.1 获取GitHub漏洞
```
GET /vulnerabilities/github
```
**功能**: 获取GitHub安全数据库的漏洞信息
**请求**:
```bash
curl -s http://127.0.0.1:5000/vulnerabilities/github
```
**期望**: 返回JSON格式的GitHub漏洞数据

#### 2.2 获取AVD漏洞
```
GET /vulnerabilities/avd
```
**功能**: 获取Android漏洞数据库(AVD)的漏洞信息
**请求**:
```bash
curl -s http://127.0.0.1:5000/vulnerabilities/avd
```
**期望**: 返回JSON格式的AVD漏洞数据

#### 2.3 获取NVD漏洞
```
GET /vulnerabilities/nvd
```
**功能**: 获取国家漏洞数据库(NVD)的漏洞信息
**请求**:
```bash
curl -s http://127.0.0.1:5000/vulnerabilities/nvd
```
**期望**: 返回JSON格式的NVD漏洞数据

---

### 3. LLM查询接口

#### 3.1 LLM查询 (支持多个模型)
```
GET /llm/query?query=<query>&model=<model>
```
**功能**: 使用LLM模型进行漏洞相关查询
**参数**:
- `query` (必需): 查询文本
- `model` (可选): 模型名称，支持 `qwen` 或 `deepseek`，默认为 `qwen`

**注意**: 该接口定义有问题 - 代码中要求model参数非空，但又设置默认值为'qwen'

**请求示例**:
```bash
# 使用Qwen模型
curl -s "http://127.0.0.1:5000/llm/query?query=What%20is%20XSS&model=qwen"

# 使用DeepSeek模型
curl -s "http://127.0.0.1:5000/llm/query?query=How%20to%20prevent%20SQL%20injection&model=deepseek"
```

**预期响应 (HTTP 200)**:
```json
{
    "message": "SUCCESS",
    "obj": "LLM response here...",
    "code": 200
}
```

**错误处理**:
- 缺少query参数: HTTP 400
- 缺少model参数或不支持的模型: HTTP 400

---

### 4. 漏洞修复建议接口 (异步任务)

#### 4.1 提交修复建议任务
```
POST /llm/repair/suggestion
```
**功能**: 异步提交漏洞修复建议生成任务
**请求方式**: multipart/form-data
**参数**:
- `vulnerability_name` (可选): 漏洞名称，如 "XSS"
- `vulnerability_desc` (可选): 漏洞描述
- `related_code` (可选): 相关代码片段
- `model` (可选): 模型名称，默认为 "qwen"

**请求示例**:
```bash
curl -X POST http://127.0.0.1:5000/llm/repair/suggestion \
  -F "vulnerability_name=XSS" \
  -F "vulnerability_desc=Cross-site Scripting vulnerability in user input" \
  -F "related_code=<script>alert('xss')</script>" \
  -F "model=qwen"
```

**预期响应 (HTTP 202)**:
```json
{
    "code": 202,
    "message": "Task submitted",
    "task_id": "task_12345",
    "status_url": "/llm/repair/suggestion/status/task_12345",
    "result_url": "/llm/repair/suggestion/result/task_12345"
}
```

#### 4.2 查询任务状态
```
GET /llm/repair/suggestion/status/<task_id>
```
**功能**: 查询异步任务的状态
**请求示例**:
```bash
curl -s http://127.0.0.1:5000/llm/repair/suggestion/status/task_12345
```

**预期响应 (HTTP 200)**:
```json
{
    "code": 200,
    "task_id": "task_12345",
    "status": "pending|running|completed|failed",
    "created_at": "2025-11-13T15:00:00",
    "completed_at": "2025-11-13T15:05:00"
}
```

**可能的状态**:
- `pending`: 任务已提交但未开始
- `running`: 正在处理中
- `completed`: 已完成
- `failed`: 失败

#### 4.3 获取任务结果
```
GET /llm/repair/suggestion/result/<task_id>
```
**功能**: 获取已完成任务的结果
**请求示例**:
```bash
curl -s http://127.0.0.1:5000/llm/repair/suggestion/result/task_12345
```

**预期响应 (HTTP 200, 仅任务完成时)**:
```json
{
    "code": 200,
    "message": "success",
    "task_id": "task_12345",
    "obj": {
        "fix_advise": "1. Validate and sanitize all user inputs\n2. Use security libraries like OWASP..."
    },
    "completed_at": "2025-11-13T15:05:00"
}
```

**错误响应**:
- 任务不存在: HTTP 404
- 任务仍在处理: HTTP 202
- 任务处理失败: HTTP 500

---

### 5. 代码解析接口 (多语言支持)

#### 5.1 POM文件解析 (Java/Maven)
```
GET /parse/pom_parse?project_folder=<path>
```
**功能**: 解析Maven项目的pom.xml，提取依赖信息
**请求**:
```bash
curl -s "http://127.0.0.1:5000/parse/pom_parse?project_folder=/path/to/java/project"
```

#### 5.2 Go项目解析
```
GET /parse/go_parse?project_folder=<path>
```
**功能**: 解析Go项目的go.mod文件，提取依赖

#### 5.3 Python项目解析
```
GET /parse/python_parse?project_folder=<path>
```
**功能**: 解析Python项目，提取requirements.txt或pyproject.toml依赖

#### 5.4 JavaScript/Node.js项目解析
```
GET /parse/javascript_parse?project_folder=<path>
```
**功能**: 解析JavaScript项目的package.json，提取npm依赖

#### 5.5 PHP项目解析
```
GET /parse/php_parse?project_folder=<path>
```
**功能**: 解析PHP项目的composer.json，提取Composer依赖

#### 5.6 Ruby项目解析
```
GET /parse/ruby_parse?project_folder=<path>
```
**功能**: 解析Ruby项目的Gemfile，提取Gem依赖

#### 5.7 Rust项目解析
```
GET /parse/rust_parse?project_folder=<path>
```
**功能**: 解析Rust项目的Cargo.toml，提取crate依赖

#### 5.8 Erlang项目解析
```
GET /parse/erlang_parse?project_folder=<path>
```
**功能**: 解析Erlang项目，提取依赖信息

**预期响应格式**:
```json
{
    "obj": [
        {
            "name": "log4j-core",
            "version": "2.14.1",
            "package_manager": "maven",
            "language": "java"
        },
        {
            "name": "spring-web",
            "version": "5.3.0",
            "package_manager": "maven",
            "language": "java"
        }
    ]
}
```

---

### 6. 编程语言检测接口

#### 6.1 获取项目主要语言
```
GET /parse/get_primary_language?project_folder=<path>&use_optimized=true
```
**功能**: 检测项目的主要编程语言
**参数**:
- `project_folder` (必需): 项目路径
- `use_optimized` (可选): 是否使用优化检测器，默认true

**请求**:
```bash
curl -s "http://127.0.0.1:5000/parse/get_primary_language?project_folder=/path/to/project"
```

**预期响应 (HTTP 200)**:
```json
{
    "code": 200,
    "message": "SUCCESS",
    "project_path": "/path/to/project",
    "language": "java",
    "confidence": "high",
    "detection_score": 1000.5,
    "supported_languages": ["java", "go", "python", "javascript", "php", ...],
    "timestamp": "2025-11-13T15:00:00"
}
```

**支持的语言**:
java, go, python, javascript, php, ruby, rust, erlang, kotlin, scala, swift, csharp, typescript, cpp, c, groovy, android

#### 6.2 检测项目所有语言
```
GET /parse/detect_languages?project_folder=<path>
```
**功能**: 检测项目中使用的所有编程语言
**请求**:
```bash
curl -s "http://127.0.0.1:5000/parse/detect_languages?project_folder=/path/to/project"
```

**预期响应 (HTTP 200)**:
```json
{
    "code": 200,
    "message": "SUCCESS",
    "project_path": "/path/to/project",
    "detected_languages": ["java", "go"],
    "primary_language": "java",
    "language_details": {
        "java": {
            "files": ["/path/to/pom.xml"],
            "package_manager": "maven",
            "priority": 1
        },
        "go": {
            "files": ["/path/to/go.mod"],
            "package_manager": "go",
            "priority": 2
        }
    },
    "timestamp": "2025-11-13T15:00:00"
}
```

---

### 7. 统一项目解析接口

#### 7.1 统一解析接口
```
GET /parse/unified_parse?project_folder=<path>&project_id=<id>
```
**功能**: 自动检测项目语言并进行多语言解析，一次性获取所有依赖
**参数**:
- `project_folder` (必需): 项目路径
- `project_id` (可选): 项目在数据库中的ID

**请求**:
```bash
curl -s "http://127.0.0.1:5000/parse/unified_parse?project_folder=/path/to/mixed-language-project&project_id=123"
```

**预期响应 (HTTP 200)**:
```json
{
    "code": 200,
    "message": "SUCCESS",
    "summary": {
        "project_path": "/path/to/project",
        "project_id": 123,
        "detected_languages": ["java", "go"],
        "primary_language": "java",
        "total_dependencies": 206,
        "parse_results": {
            "java": {
                "status": "success",
                "count": 25
            },
            "go": {
                "status": "success",
                "count": 181
            }
        },
        "timestamp": "2025-11-13T15:00:00"
    },
    "dependencies": [
        {
            "name": "log4j-core",
            "version": "2.14.1",
            "language": "java",
            "package_manager": "maven",
            "project_id": 123
        },
        {
            "name": "gin",
            "version": "1.7.0",
            "language": "go",
            "package_manager": "go",
            "project_id": 123
        }
    ],
    "obj": {
        "summary": {...},
        "dependencies": [...],
        "total_dependencies": 206
    }
}
```

---

### 8. 漏洞检测接口

#### 8.1 检测依赖漏洞
```
POST /vulnerabilities/detect
Content-Type: application/json
```
**功能**: 检测上传的依赖是否存在漏洞
**请求**:
```bash
curl -X POST http://127.0.0.1:5000/vulnerabilities/detect \
  -H "Content-Type: application/json" \
  -d '{
    "dependencies": [
        {
            "name": "log4j-core",
            "version": "2.14.1"
        }
    ]
  }'
```

**预期响应 (HTTP 200)**:
```json
{
    "code": 200,
    "message": "Vulnerabilities detected",
    "vulnerabilities": [
        {
            "dependency": "log4j-core",
            "version": "2.14.1",
            "vulnerability_id": "CVE-2021-44228",
            "severity": "CRITICAL",
            "description": "Log4Shell vulnerability..."
        }
    ]
}
```

---

## Spring Boot API 接口

### 1. 用户管理接口

#### 1.1 用户登录
```
POST /user/login
Content-Type: application/json
```
**请求**:
```bash
curl -X POST http://localhost:8081/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

**预期响应 (HTTP 200)**:
```json
{
    "code": 200,
    "message": "Login successful",
    "obj": {
        "id": 1,
        "username": "admin",
        "token": "jwt_token_here"
    }
}
```

#### 1.2 用户注册
```
POST /user/register
Content-Type: application/json
```
**请求**:
```bash
curl -X POST http://localhost:8081/user/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "email": "newuser@example.com",
    "password": "SecurePass123",
    "phone": "13800000000"
  }'
```

### 2. 项目管理接口

#### 2.1 获取项目列表
```
GET /project
```
**请求**:
```bash
curl -s http://localhost:8081/project \
  -H "Authorization: Bearer jwt_token"
```

---

## 已识别的代码问题

### 1. LLM查询接口参数逻辑错误
**文件**: app.py:71-76
**问题**:
```python
def get_llm_query():
    query = request.args.get("query")
    model = request.args.get("model")
    if model=='':
        model='qwen'

    if not query:
        return jsonify({"error": "Missing required parameter 'query'"}), 400
    if not model:  # 这里永远不会为真，因为上面已设置默认值
        return jsonify({"error": "Missing required parameter 'model'"}), 400
```
**建议修复**: 移除model参数的强制要求或正确处理默认值

### 2. 统一解析接口的错误处理
**文件**: app.py:440
**问题**: 某些解析函数不检查输入路径是否存在
**建议**: 所有解析函数都应该验证project_folder参数

---

## 测试执行步骤

### 第1步: 启动Flask服务器
```bash
cd path/to/flask/app
python app.py
```
服务器应在 http://127.0.0.1:5000 上运行

### 第2步: 启动Spring Boot服务器
```bash
cd backend
mvn spring-boot:run
```
或
```bash
java -jar target/vulsystem-backend.jar
```

### 第3步: 运行API测试
```bash
bash test_all_apis.sh
```

---

## 测试结果汇总

| 接口分类 | 接口数量 | 测试状态 | 通过数 | 失败数 |
|---------|---------|---------|--------|--------|
| Flask基础 | 1 | ❌ | 0 | 1 |
| 漏洞数据库 | 3 | ❌ | 0 | 3 |
| LLM查询 | 1 | ❌ | 0 | 1 |
| 漏洞修复建议 | 3 | ❌ | 0 | 3 |
| 代码解析 | 8 | ❌ | 0 | 8 |
| 语言检测 | 2 | ❌ | 0 | 2 |
| 统一解析 | 1 | ❌ | 0 | 1 |
| 漏洞检测 | 1 | ❌ | 0 | 1 |
| **Spring Boot** | **2** | **❌** | **0** | **2** |
| **总计** | **22** | **❌** | **0** | **22** |

---

## 结论和建议

1. **服务器未启动**: 需要启动Flask和Spring Boot服务器才能进行实际测试
2. **代码质量**: 存在一些参数验证和错误处理的问题
3. **建议**:
   - 修复LLM查询接口的参数逻辑
   - 增强错误处理和输入验证
   - 添加更多的日志记录便于调试
   - 实现API速率限制和超时控制
   - 完善异步任务的错误处理机制

