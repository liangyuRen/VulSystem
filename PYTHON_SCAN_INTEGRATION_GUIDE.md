# Python项目扫描和White-list集成指南

## 概述

实现了一个完整的工作流程，用于扫描Python项目、解析依赖并将其存储到white-list表。

### 工作流程

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Python项目扫描流程                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  Step 1: 接收请求                                                   │
│  ├─ 项目路径: /path/to/python/project                              │
│  └─ 项目ID: 123                                                     │
│                                                                       │
│  Step 2: 语言检测 (调用Flask API)                                   │
│  ├─ 调用 /parse/get_primary_language                               │
│  ├─ 确认项目类型为Python                                           │
│  └─ 返回: "python"                                                  │
│                                                                       │
│  Step 3: 依赖解析 (调用Flask API)                                   │
│  ├─ 调用 /parse/python_parse                                       │
│  ├─ 解析 requirements.txt, pyproject.toml 等                       │
│  └─ 返回: [{"name": "requests", "version": "2.28.0"}, ...]        │
│                                                                       │
│  Step 4: 存储到White-list                                          │
│  ├─ 遍历依赖列表                                                    │
│  ├─ 检查是否已存在 (避免重复)                                       │
│  ├─ 插入到 white_list 表                                           │
│  └─ 返回: 保存成功的条目数                                          │
│                                                                       │
│  Step 5: 返回结果                                                   │
│  └─ JSON格式的扫描结果                                              │
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 技术实现

### 1. Java Service层 (`PythonProjectScanService.java`)

**位置**: `backend/src/main/java/com/nju/backend/service/project/impl/PythonProjectScanService.java`

**主要方法**:

```java
// 扫描Python项目
public PythonScanResult scanPythonProject(String projectPath, Long projectId)

// 检测项目语言
private String detectProjectLanguage(String projectPath)

// 解析Python依赖
private List<PythonDependency> parsePythonDependencies(String projectPath)

// 保存到White-list
private int saveToWhiteList(List<PythonDependency> dependencies, Long projectId)

// 获取项目White-list记录
public List<WhiteList> getProjectWhiteList(Long projectId)
```

### 2. Controller层 (`PythonScanController.java`)

**位置**: `backend/src/main/java/com/nju/backend/controller/PythonScanController.java`

**API端点**:

```
POST /python/scan
GET /python/whitelist/{projectId}
```

---

## API使用指南

### 1. 扫描Python项目

**请求**:
```bash
curl -X POST http://localhost:8081/python/scan \
  -H "Content-Type: application/json" \
  -d '{
    "projectPath": "/path/to/python/project",
    "projectId": 1
  }'
```

**请求参数**:
- `projectPath` (必需): Python项目的完整路径
- `projectId` (必需): 项目在数据库中的ID

**响应示例** (成功):
```json
{
  "code": 200,
  "message": "Successfully scanned and saved 25 dependencies to white-list",
  "success": true,
  "data": {
    "projectPath": "/path/to/python/project",
    "projectId": 1,
    "detectedLanguage": "python",
    "dependencyCount": 25,
    "savedCount": 25,
    "dependencies": [
      {
        "name": "requests",
        "version": "2.28.0",
        "packageManager": "pip",
        "language": "python"
      },
      {
        "name": "flask",
        "version": "2.2.0",
        "packageManager": "pip",
        "language": "python"
      },
      ...
    ]
  }
}
```

**响应示例** (失败 - 非Python项目):
```json
{
  "code": 400,
  "message": "Project is not a Python project. Detected: java",
  "success": false
}
```

**错误码**:
- `200`: 成功
- `400`: 请求参数错误或项目类型不符
- `500`: 服务器内部错误

---

### 2. 获取项目White-list记录

**请求**:
```bash
curl http://localhost:8081/python/whitelist/1
```

**路径参数**:
- `projectId`: 项目ID

**响应示例**:
```json
{
  "code": 200,
  "message": "Success",
  "data": [
    {
      "id": 1,
      "projectId": 1,
      "componentName": "requests",
      "componentVersion": "2.28.0",
      "language": "python",
      "packageManager": "pip",
      "status": "APPROVED",
      "createdTime": "2025-11-13T15:00:00",
      "remark": "Auto-detected from Python project scan"
    },
    {
      "id": 2,
      "projectId": 1,
      "componentName": "flask",
      "componentVersion": "2.2.0",
      "language": "python",
      "packageManager": "pip",
      "status": "APPROVED",
      "createdTime": "2025-11-13T15:00:00",
      "remark": "Auto-detected from Python project scan"
    },
    ...
  ]
}
```

---

## 数据库架构

### White-list表结构

```sql
CREATE TABLE white_list (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  component_name VARCHAR(255) NOT NULL,
  component_version VARCHAR(100),
  language VARCHAR(50),
  package_manager VARCHAR(50),
  status VARCHAR(50) DEFAULT 'APPROVED',
  created_time DATETIME,
  updated_time DATETIME,
  remark VARCHAR(500),
  INDEX idx_project_id (project_id),
  INDEX idx_component_name (component_name),
  UNIQUE KEY uq_project_component_version (project_id, component_name, component_version)
);
```

---

## 集成步骤

### Step 1: 添加Java类

1. 将 `PythonProjectScanService.java` 复制到:
   ```
   backend/src/main/java/com/nju/backend/service/project/impl/
   ```

2. 将 `PythonScanController.java` 复制到:
   ```
   backend/src/main/java/com/nju/backend/controller/
   ```

### Step 2: 添加依赖 (pom.xml)

确保已包含以下依赖:
```xml
<!-- Web -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- RestTemplate支持 -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>

<!-- JSON处理 -->
<dependency>
  <groupId>com.fasterxml.jackson.core</groupId>
  <artifactId>jackson-databind</artifactId>
</dependency>

<!-- MyBatis-Plus -->
<dependency>
  <groupId>com.baomidou</groupId>
  <artifactId>mybatis-plus-boot-starter</artifactId>
</dependency>
```

### Step 3: 配置RestTemplate

在Spring Boot配置类中添加:
```java
@Configuration
public class RestClientConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

### Step 4: 创建White-list Mapper

```java
package com.nju.backend.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nju.backend.repository.po.WhiteList;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WhiteListMapper extends BaseMapper<WhiteList> {
}
```

### Step 5: 创建White-list实体类

```java
package com.nju.backend.repository.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("white_list")
public class WhiteList {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;
    private String componentName;
    private String componentVersion;
    private String language;
    private String packageManager;
    private String status;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    private String remark;

    // Getters and Setters...
}
```

### Step 6: 编译和部署

```bash
cd backend
mvn clean package
java -jar target/vulsystem-backend.jar
```

---

## 测试示例

### 使用Python脚本测试

```python
import requests
import json

# 扫描Python项目
response = requests.post(
    "http://localhost:8081/python/scan",
    json={
        "projectPath": "C:/Users/xxx/MyPythonProject",
        "projectId": 1
    }
)

result = response.json()
if result['success']:
    print(f"✓ 扫描成功！")
    print(f"  - 检测语言: {result['data']['detectedLanguage']}")
    print(f"  - 发现依赖: {result['data']['dependencyCount']}")
    print(f"  - 保存条目: {result['data']['savedCount']}")

    for dep in result['data']['dependencies'][:5]:
        print(f"  - {dep['name']} == {dep['version']}")
else:
    print(f"✗ 扫描失败: {result['message']}")

# 获取White-list
response = requests.get("http://localhost:8081/python/whitelist/1")
whitelist = response.json()['data']
print(f"\nWhite-list包含 {len(whitelist)} 项:")
for item in whitelist:
    print(f"  - {item['componentName']} ({item['componentVersion']})")
```

### 使用curl测试

```bash
# 扫描项目
curl -X POST http://localhost:8081/python/scan \
  -H "Content-Type: application/json" \
  -d '{
    "projectPath": "C:/Users/xxx/MyPythonProject",
    "projectId": 1
  }' | python3 -m json.tool

# 查看结果
curl http://localhost:8081/python/whitelist/1 | python3 -m json.tool
```

---

## 工作原理

### 核心流程详解

1. **语言检测**
   - 调用Flask的 `/parse/get_primary_language` API
   - 传入项目路径 (URL编码)
   - 返回: 检测到的编程语言 (如 "python")

2. **依赖解析**
   - 调用Flask的 `/parse/python_parse` API
   - 解析项目中的Python依赖管理文件:
     - `requirements.txt`
     - `setup.py`
     - `pyproject.toml`
     - `Pipfile`
   - 返回: JSON格式的依赖列表

3. **去重检查**
   - 查询数据库中是否已存在该组件
   - 关键字段: `project_id`, `component_name`, `component_version`
   - 防止重复插入相同的依赖

4. **数据持久化**
   - 将依赖信息存储到 `white_list` 表
   - 标记为 "APPROVED" 状态
   - 记录扫描时间和备注

---

## 注意事项

1. **Flask服务必须运行**
   - Flask应该在 http://127.0.0.1:5000 上运行
   - 如果Flask地址不同，需要修改 `PythonProjectScanService` 中的常量

2. **路径编码**
   - Windows路径需要URL编码处理
   - 例如: `C:\Users\test\project` → `C%3A%5CUsers%5Ctest%5Cproject`

3. **超时设置**
   - Flask API调用可能耗时较长
   - 建议设置适当的超时时间
   - 可在 `RestTemplate` 配置中调整

4. **错误处理**
   - 确保有适当的异常处理机制
   - 检查Flask服务的可用性
   - 验证项目路径的有效性

---

## 性能优化建议

1. **批量操作**
   - 支持批量扫描多个Python项目
   - 可以在控制器中添加批处理接口

2. **缓存**
   - 缓存已扫描项目的结果
   - 避免重复调用Flask API

3. **异步处理**
   - 大型项目的扫描可以异步执行
   - 使用消息队列 (RabbitMQ, Kafka等)

4. **数据库优化**
   - 为 `project_id` 和 `component_name` 创建索引
   - 定期清理过期的white-list记录

---

## 常见问题

**Q1: 如何修改Flask服务的地址?**
A: 修改 `PythonProjectScanService.java` 中的常量:
```java
private static final String FLASK_BASE_URL = "http://your-flask-server:5000";
```

**Q2: 如何支持其他编程语言?**
A: 按照相同的模式创建新的Service类 (如 `JavaProjectScanService`、`GoProjectScanService`等)

**Q3: 已存在的white-list记录如何更新?**
A: 当前实现会跳过已存在的记录。如需更新，可以修改 `saveToWhiteList` 方法的逻辑

**Q4: 如何批量导入white-list?**
A: 可以添加新的接口接受CSV文件或JSON数组，批量插入数据库

---

*最后更新: 2025-11-13*
*版本: 1.0*

