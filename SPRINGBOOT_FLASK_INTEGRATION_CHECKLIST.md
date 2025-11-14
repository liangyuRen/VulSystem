# SpringBoot-Flask 集成测试清单

**目标**: 验证后端与Flask端的接口集成是否正确

---

## 快速集成验证

### 第1步: 验证Flask服务健康状态

```bash
curl -X GET http://localhost:5000/vulnerabilities/test
# 预期返回:
# {
#   "code": 200,
#   "message": "Server is running normally",
#   "status": "OK"
# }
```

### 第2步: 验证SpringBoot后端连接

```bash
curl -X GET http://localhost:8081/project/list?companyId=1&page=1&size=10
# 预期返回: 项目列表 (JSON)
```

### 第3步: 测试文件上传和语言检测

```bash
# 创建测试ZIP (包含pom.xml)
cd /tmp
mkdir test-java-project
cd test-java-project
echo '<project><modelVersion>4.0.0</modelVersion></project>' > pom.xml

# 压缩为ZIP
zip -r test-java.zip pom.xml

# 上传到SpringBoot
curl -X POST http://localhost:8081/project/uploadProject \
  -F "file=@test-java.zip" \
  -F "name=TestJavaProject" \
  -F "description=Language detection test" \
  -F "companyId=1"

# 预期返回:
# {
#   "code": 200,
#   "message": "操作成功",
#   "data": {
#     "projectId": 0,
#     "status": "pending",
#     "message": "项目上传成功，正在分析..."
#   }
# }
```

---

## 集成问题排查

### 问题1: Flask /parse/pom_parse 返回错误

**症状**: 
```
VulnerabilityJobHandler 日志显示:
"POM解析API返回空响应，项目路径: D:\kuling\upload\..."
```

**可能原因**:
1. Flask服务未启动
2. Flask接口URL错误
3. 依赖解析代码有bug

**检查步骤**:
```bash
# 1. 测试Flask接口直接调用
curl -X POST "http://localhost:5000/parse/pom_parse?project_folder=D:\kuling\upload\test" 

# 2. 检查Flask日志是否有错误
# 3. 确认项目路径是否正确
# 4. 验证返回的JSON格式
```

### 问题2: 漏洞检测接口调用失败

**症状**:
```
VulnerabilityJobHandler 日志:
"Flask服务返回5xx错误"
"HTTP 错误（状态码 500）"
```

**可能原因**:
1. getLabels() 函数异常
2. Unicode编码问题
3. 模型调用失败

**检查步骤**:
```bash
# 1. 测试Flask漏洞检测接口
curl -X POST http://localhost:5000/vulnerabilities/detect \
  -H "Content-Type: application/json" \
  -d '{
    "cve_id": "CVE-2024-1234",
    "desc": "Test vulnerability description",
    "white_list": "[{\"name\":\"jackson\",\"language\":\"java\",\"pojectid\":\"1\"}]",
    "company": "test",
    "detect_strategy": "exact",
    "similarityThreshold": "0.8",
    "language": "java"
  }'

# 2. 检查返回值是否为分号分隔的字符串
# 3. 查看Flask标准输出和错误输出
```

### 问题3: 白名单格式错误

**症状**:
```
漏洞检测后，没有创建任何漏洞记录
```

**可能原因**:
1. white_list JSON格式不对
2. 依赖库名称不匹配
3. language字段缺失

**检查步骤**:
```sql
-- 查看white_list表中的数据格式
SELECT id, name, language, file_path FROM white_list LIMIT 5;

-- 检查是否有Java依赖
SELECT COUNT(*) FROM white_list WHERE language='java';

-- 检查是否有C依赖
SELECT COUNT(*) FROM white_list WHERE language='c' OR language='c/c++';
```

### 问题4: 项目语言未被正确设置

**症状**:
```
项目上传后，language字段为 "java" (即使是C项目)
```

**可能原因**:
1. detectProjectType() 返回值不正确
2. uploadFile() 中的语言检测逻辑有问题
3. 异步解析任务未执行

**检查步骤**:
```java
// 在 ProjectUtil.java 中添加调试日志
System.out.println("DEBUG: 检测到的项目类型: " + projectType);

// 检查Spring Boot日志
// 应该看到: "启动Java项目解析任务" 或 "启动C/C++项目解析任务"
```

---

## 数据库验证清单

### 检查1: 验证Project表中的language字段

```sql
-- 应该看到 java, c/c++, unknown 等值
SELECT DISTINCT language FROM project;

-- 检查最近上传的项目
SELECT id, name, language, file FROM project ORDER BY create_time DESC LIMIT 5;
```

### 检查2: 验证WhiteList表中的依赖库

```sql
-- Java依赖应该有 language='java'
SELECT COUNT(*) as java_deps FROM white_list WHERE language='java';
SELECT COUNT(*) as c_deps FROM white_list WHERE language='c' OR language='c/c++';

-- 检查某个项目的白名单
SELECT name, language, file_path FROM white_list 
WHERE file_path LIKE '%d41b8699-0b7e-44d8-85c4-49a425966a7b%' 
LIMIT 10;
```

### 检查3: 验证Vulnerability表中的language字段

```sql
-- 应该有按语言分类的漏洞
SELECT language, COUNT(*) as count FROM vulnerability 
GROUP BY language;

-- 检查具体的漏洞记录
SELECT id, name, language, risk_level FROM vulnerability 
ORDER BY time DESC LIMIT 10;
```

### 检查4: 查询项目统计

```bash
# 调用后端的统计接口
curl "http://localhost:8081/project/statistics?companyId=1" | python3 -m json.tool

# 预期包含:
# - cVulnerabilityNum: C语言漏洞数
# - javaVulnerabilityNum: Java语言漏洞数
```

---

## 端到端集成测试流程

### 步骤1: 准备测试数据

```bash
# 1. 创建两个测试项目
#    - test-java-project (包含pom.xml)
#    - test-c-project (包含Makefile)

# 2. 将其压缩为ZIP文件

# 3. 上传到SpringBoot
curl -X POST http://localhost:8081/project/uploadProject \
  -F "file=@test-java.zip" \
  -F "name=JavaTestProj" \
  -F "description=Java project test" \
  -F "companyId=1"

curl -X POST http://localhost:8081/project/uploadProject \
  -F "file=@test-c.zip" \
  -F "name=CTestProj" \
  -F "description=C project test" \
  -F "companyId=1"
```

### 步骤2: 验证解析结果

```bash
# 1. 等待3-5秒，让异步任务完成

# 2. 查询项目列表
curl "http://localhost:8081/project/list?companyId=1&page=1&size=20" | python3 -m json.tool

# 3. 检查language字段
#    JavaTestProj 应该是 "java"
#    CTestProj 应该是 "c" 或 "c/c++"
```

### 步骤3: 手动触发漏洞检测

```bash
# 1. 通过XXL-Job控制台或直接调用
curl -X POST "http://localhost:8081/...xxl-job-admin..."

# 或者在数据库中插入测试漏洞数据
INSERT INTO vulnerability_report (cveId, vulnerabilityName, description, riskLevel) 
VALUES ('CVE-2024-TEST', 'Test Vulnerability', 'Test Description', 'HIGH');

# 2. 等待自动触发或手动触发任务

# 3. 查看日志中是否有:
#    - "API调用成功"
#    - "插入新漏洞报告"
#    - 语言分别的处理记录
```

### 步骤4: 验证最终结果

```bash
# 1. 查询项目的漏洞
curl "http://localhost:8081/project/getVulnerabilities?id=23"

# 2. 查询统计数据
curl "http://localhost:8081/project/statistics?companyId=1"

# 3. 数据库中应该有:
#    - Vulnerability 记录，language字段正确设置
#    - ProjectVulnerability 关联记录
#    - VulnerabilityReportVulnerability 关联记录
```

---

## 常见集成问题和解决方案

### 问题: Flask接口超时

**解决方案**:
1. 增加RestTemplate超时时间
2. 检查Flask服务是否响应缓慢
3. 确保项目路径正确，避免扫描超大项目

### 问题: 字符编码问题

**解决方案**:
```java
// 在 ProjectUtil.java 中
// 已经处理了 GBK/UTF-8 编码转换

// 在 Flask 中
# -*- coding: utf-8 -*-
import sys
import io

if sys.stdout.encoding != 'utf-8':
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
```

### 问题: 异步任务未执行

**解决方案**:
1. 确认 Spring @EnableAsync 已启用
2. 检查 projectAnalysisExecutor 线程池配置
3. 查看 ApplicationContext 是否正确注入

---

## 日志查看技巧

### SpringBoot 日志关键字

```
# 文件上传
uploadProject 接口被调用
文件上传成功，路径

# 项目检测
DEBUG: 检测项目类型
启动Java项目解析任务
启动C/C++项目解析任务

# Flask调用
调用POM解析API
调用C项目解析API
解析出依赖库数量

# 漏洞检测
API调用成功，CVE
插入新漏洞报告
检测到的依赖库
```

### Flask 日志关键字

```
[pom_parse] 开始解析
[c_parse] 开始解析
[漏洞检测] 接收到请求
[getLabels] 执行中
[LLM调用] 开始
```

---

## 集成完成检查表

- [ ] Flask服务启动且健康
- [ ] SpringBoot连接Flask成功
- [ ] 文件上传接口工作正常
- [ ] Java项目被正确检测并调用 /parse/pom_parse
- [ ] C项目被正确检测并调用 /parse/c_parse
- [ ] 依赖库被正确保存到 white_list
- [ ] 依赖库包含正确的 language 字段
- [ ] 漏洞检测接口被正确调用
- [ ] 漏洞记录被创建，language 字段正确设置
- [ ] 统计接口返回正确的语言分类数据
- [ ] 前端能显示项目和漏洞信息
- [ ] 日志中无异常或错误

---

**准备好进行集成测试了吗？** 🚀
