# 后端API快速参考指南

## Flask API (端口 5000)

### 1. 基础检查
```bash
curl http://127.0.0.1:5000/vulnerabilities/test
# 返回: {"code":200,"message":"Server is running normally","status":"OK"}
```

### 2. 获取漏洞数据
```bash
# GitHub漏洞
curl http://127.0.0.1:5000/vulnerabilities/github

# AVD漏洞 (Android)
curl http://127.0.0.1:5000/vulnerabilities/avd

# NVD漏洞 (National Vulnerability Database)
curl http://127.0.0.1:5000/vulnerabilities/nvd
```

### 3. LLM查询
```bash
# 使用Qwen模型 (推荐)
curl "http://127.0.0.1:5000/llm/query?query=What%20is%20XSS&model=qwen"

# 使用DeepSeek模型
curl "http://127.0.0.1:5000/llm/query?query=SQL%20injection&model=deepseek"
```

### 4. 获取修复建议 (异步)
```bash
# 提交任务
TASK=$(curl -X POST http://127.0.0.1:5000/llm/repair/suggestion \
  -F "vulnerability_name=XSS" \
  -F "vulnerability_desc=Cross-site Scripting" \
  -F "model=qwen" | grep -o '"task_id":"[^"]*' | cut -d'"' -f4)

# 检查状态
curl http://127.0.0.1:5000/llm/repair/suggestion/status/$TASK

# 获取结果
curl http://127.0.0.1:5000/llm/repair/suggestion/result/$TASK
```

### 5. 检测编程语言
```bash
# 获取主要语言
curl "http://127.0.0.1:5000/parse/get_primary_language?project_folder=C:/Users/xxx/project"

# 检测所有语言
curl "http://127.0.0.1:5000/parse/detect_languages?project_folder=C:/Users/xxx/project"
```

### 6. 代码解析 (多语言)
```bash
# Java项目 (Maven)
curl "http://127.0.0.1:5000/parse/pom_parse?project_folder=/path/to/project"

# Go项目
curl "http://127.0.0.1:5000/parse/go_parse?project_folder=/path/to/project"

# Python项目
curl "http://127.0.0.1:5000/parse/python_parse?project_folder=/path/to/project"

# JavaScript/Node.js项目
curl "http://127.0.0.1:5000/parse/javascript_parse?project_folder=/path/to/project"

# PHP项目
curl "http://127.0.0.1:5000/parse/php_parse?project_folder=/path/to/project"

# Ruby项目
curl "http://127.0.0.1:5000/parse/ruby_parse?project_folder=/path/to/project"

# Rust项目
curl "http://127.0.0.1:5000/parse/rust_parse?project_folder=/path/to/project"

# Erlang项目
curl "http://127.0.0.1:5000/parse/erlang_parse?project_folder=/path/to/project"
```

### 7. 统一解析 (自动检测语言)
```bash
curl "http://127.0.0.1:5000/parse/unified_parse?project_folder=/path/to/project&project_id=123"
```

### 8. 漏洞检测
```bash
curl -X POST http://127.0.0.1:5000/vulnerabilities/detect \
  -H "Content-Type: application/json" \
  -d '{
    "dependencies": [
      {"name": "log4j-core", "version": "2.14.1"},
      {"name": "spring-web", "version": "5.3.0"}
    ]
  }'
```

---

## Spring Boot API (端口 8081)

### 1. 用户登录
```bash
curl -X POST http://localhost:8081/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

### 2. 用户注册
```bash
curl -X POST http://localhost:8081/user/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "email": "user@example.com",
    "password": "SecurePass123",
    "phone": "13800000000"
  }'
```

### 3. 获取项目列表
```bash
curl http://localhost:8081/project \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## Python示例

### 使用Python进行API调用

```python
import requests
import json

# Flask基础URL
FLASK_URL = "http://127.0.0.1:5000"
SPRINGBOOT_URL = "http://localhost:8081"

# 1. 检查服务器状态
response = requests.get(f"{FLASK_URL}/vulnerabilities/test")
print(response.json())

# 2. 获取GitHub漏洞
response = requests.get(f"{FLASK_URL}/vulnerabilities/github")
vulnerabilities = response.json()

# 3. LLM查询
response = requests.get(f"{FLASK_URL}/llm/query", params={
    "query": "What is SQL injection?",
    "model": "qwen"
})
result = response.json()
print(result['obj'])

# 4. 检测项目语言
response = requests.get(f"{FLASK_URL}/parse/get_primary_language", params={
    "project_folder": "/path/to/project"
})
language_info = response.json()
print(f"主要语言: {language_info['language']}")

# 5. 统一解析项目
response = requests.get(f"{FLASK_URL}/parse/unified_parse", params={
    "project_folder": "/path/to/project"
})
parse_result = response.json()
print(f"发现 {parse_result['summary']['total_dependencies']} 个依赖")

# 6. 用户登录
response = requests.post(f"{SPRINGBOOT_URL}/user/login", json={
    "username": "admin",
    "password": "admin123"
})
login_result = response.json()
token = login_result.get('obj', {}).get('token')
```

---

## 常见问题

### Q1: Flask服务器无响应?
**A**: 检查Flask是否运行在5000端口
```bash
netstat -tlnp | grep 5000
```

### Q2: Spring Boot服务器无响应?
**A**: 检查Spring Boot是否运行在8081端口
```bash
netstat -tlnp | grep 8081
```

### Q3: 路径参数如何正确编码?
**A**: 使用URL编码
```bash
# 例如: C:\Users\test\project 应编码为 C%3A%5CUsers%5Ctest%5Cproject
curl "http://127.0.0.1:5000/parse/pom_parse?project_folder=C%3A%5CUsers%5Ctest%5Cproject"
```

### Q4: 异步任务如何查询结果?
**A**:
1. 提交任务，获得task_id
2. 轮询status端点检查状态
3. 状态为completed时，从result端点获取结果

### Q5: 如何处理CORS错误?
**A**: Flask已配置CORS支持，允许所有来源的请求

---

## 测试脚本

### 快速测试
```bash
cd /path/to/VulSystem
bash quick_api_test.sh
```

### 完整测试
```bash
cd /path/to/VulSystem
bash test_all_apis.sh
```

### Python测试 (需要requests库)
```bash
pip install requests
python3 test_api.py
```

---

## API应答码参考

| 状态码 | 含义 | 说明 |
|--------|------|------|
| 200 | OK | 请求成功 |
| 202 | Accepted | 异步任务已接受 |
| 400 | Bad Request | 请求参数错误 |
| 404 | Not Found | 资源不存在 |
| 405 | Method Not Allowed | 使用了错误的HTTP方法 |
| 500 | Server Error | 服务器内部错误 |

---

## 性能优化建议

1. **批量操作**: 使用统一解析接口而不是逐个调用单语言解析器
2. **缓存结果**: 对不经常变化的数据进行缓存
3. **异步处理**: 对LLM查询使用异步任务，避免阻塞
4. **超时设置**: 设置合理的请求超时时间
5. **并发限制**: 对并发请求进行限制，避免服务器过载

---

## 更多信息

查看详细文档: `COMPLETE_API_TEST_DOCUMENTATION.md`
查看测试报告: `FINAL_API_TEST_SUMMARY.md`

