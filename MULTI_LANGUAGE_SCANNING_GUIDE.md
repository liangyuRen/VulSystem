# 多语言项目扫描和White-list管理系统

## 概述

实现了一个完整的多语言项目扫描和依赖管理系统，支持Python、PHP、Rust、JavaScript、Java、Go等多种编程语言。

### 核心功能

1. **自动语言检测** - 使用Flask智能检测项目的编程语言
2. **语言特定解析** - 根据检测到的语言调用对应的依赖解析器
3. **统一存储** - 将所有依赖统一存储到white-list表
4. **语言查询** - 支持按项目或按语言查询依赖

## 系统架构

```
┌─────────────────────────────────────────────────────────────────┐
│                     多语言项目扫描工作流                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  Step 1: 接收请求                                                 │
│  ├─ 项目路径: /path/to/project                                   │
│  └─ 项目ID: 123                                                   │
│                                                                   │
│  Step 2: 语言检测 (调用Flask API)                                 │
│  ├─ 调用 /parse/get_primary_language                             │
│  ├─ 返回: "python" / "php" / "javascript" / "rust" 等            │
│  └─ 确定要使用的解析器                                            │
│                                                                   │
│  Step 3: 依赖解析 (调用对应语言的Flask API)                      │
│  ├─ Python: /parse/python_parse                                  │
│  ├─ PHP: /parse/php_parse                                        │
│  ├─ JavaScript: /parse/javascript_parse                          │
│  ├─ Rust: /parse/rust_parse                                      │
│  ├─ Java: /parse/pom_parse                                       │
│  └─ 返回: [{name, version}, ...]                                │
│                                                                   │
│  Step 4: 存储到White-list                                        │
│  ├─ 遍历依赖列表                                                  │
│  ├─ 检查重复                                                      │
│  ├─ 插入数据库                                                    │
│  └─ 返回保存成功数                                                │
│                                                                   │
│  Step 5: 返回结果                                                 │
│  └─ JSON格式的扫描结果                                            │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

## 支持的编程语言

| 语言 | 检测方式 | 依赖管理器 | 配置文件 | 解析端点 |
|------|---------|----------|---------|---------|
| Python | .py文件 | pip | requirements.txt, setup.py, pyproject.toml | /parse/python_parse |
| PHP | .php文件 | composer | composer.json | /parse/php_parse |
| JavaScript | .js文件 | npm | package.json | /parse/javascript_parse |
| Rust | .rs文件 | cargo | Cargo.toml | /parse/rust_parse |
| Java | .java文件 | maven | pom.xml | /parse/pom_parse |
| Go | .go文件 | go mod | go.mod | /parse/go_parse |
| Ruby | .rb文件 | gems | Gemfile | /parse/ruby_parse |
| Erlang | .erl文件 | rebar | rebar.config | /parse/erlang_parse |

## API 端点

### 1. 扫描项目

**请求**
```bash
curl -X POST http://localhost:8081/project/scan \
  -H "Content-Type: application/json" \
  -d '{
    "projectPath": "/path/to/project",
    "projectId": 1
  }'
```

**响应示例 (成功)**
```json
{
  "code": 200,
  "message": "Successfully scanned and saved 25 dependencies to white-list",
  "success": true,
  "data": {
    "projectPath": "/path/to/project",
    "projectId": 1,
    "detectedLanguage": "python",
    "dependencyCount": 25,
    "savedCount": 25,
    "dependencies": [
      {
        "name": "requests",
        "version": "2.28.0"
      },
      {
        "name": "flask",
        "version": "2.2.0"
      },
      ...
    ]
  }
}
```

**响应示例 (失败)**
```json
{
  "code": 400,
  "message": "Unsupported language: cobol",
  "success": false,
  "error": "..."
}
```

### 2. 获取项目White-list (所有语言)

**请求**
```bash
curl http://localhost:8081/project/whitelist/1
```

**响应**
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
      "createdTime": "2025-11-13 15:00:00",
      "remark": "Auto-detected from python project scan"
    },
    ...
  ]
}
```

### 3. 获取特定语言的White-list

**请求**
```bash
curl http://localhost:8081/project/whitelist/1/python
curl http://localhost:8081/project/whitelist/1/javascript
curl http://localhost:8081/project/whitelist/1/php
```

**响应** - 同上，但仅包含指定语言的依赖

## 实现细节

### MultiLanguageProjectScanService

位置: `backend/src/main/java/com/nju/backend/service/project/impl/MultiLanguageProjectScanService.java`

**主要方法:**

1. `scanProject(String projectPath, Long projectId)` - 扫描项目主入口
   - 检测语言
   - 选择对应解析器
   - 解析依赖
   - 保存到数据库

2. `detectProjectLanguage(String projectPath)` - 调用Flask语言检测API

3. `parseDependencies(String projectPath, String parserEndpoint)` - 调用Flask依赖解析API

4. `saveToWhiteList(List<ProjectDependency> dependencies, Long projectId, String language)` - 保存到数据库

5. `getProjectWhiteList(Long projectId)` - 查询所有依赖

6. `getProjectWhiteListByLanguage(Long projectId, String language)` - 查询特定语言依赖

### MultiLanguageProjectScanController

位置: `backend/src/main/java/com/nju/backend/controller/MultiLanguageProjectScanController.java`

**端点:**

1. `POST /project/scan` - 扫描项目
2. `GET /project/whitelist/{projectId}` - 获取全部White-list
3. `GET /project/whitelist/{projectId}/{language}` - 获取特定语言White-list

### WhiteList 实体类

位置: `backend/src/main/java/com/nju/backend/repository/po/WhiteList.java`

**主要字段:**

- `id` - 主键
- `projectId` - 所属项目ID
- `componentName` - 组件名称
- `componentVersion` - 组件版本
- `language` - 编程语言
- `packageManager` - 包管理器 (pip, npm, composer等)
- `status` - 审批状态
- `createdTime` - 创建时间
- `remark` - 备注

## 数据库表结构

```sql
CREATE TABLE white_list (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  component_name VARCHAR(255) NOT NULL,
  component_version VARCHAR(100),
  name VARCHAR(255),
  file_path VARCHAR(500),
  description VARCHAR(1000),
  language VARCHAR(50),
  package_manager VARCHAR(50),
  status VARCHAR(50) DEFAULT 'APPROVED',
  created_time DATETIME,
  remark VARCHAR(500),
  isdelete INT DEFAULT 0,

  INDEX idx_project_id (project_id),
  INDEX idx_component_name (component_name),
  INDEX idx_language (language),
  UNIQUE KEY uq_project_component_version (project_id, component_name, component_version)
);
```

## 使用示例

### 1. 扫描Python项目

```bash
curl -X POST http://localhost:8081/project/scan \
  -H "Content-Type: application/json" \
  -d '{
    "projectPath": "/path/to/python/project",
    "projectId": 1
  }'
```

响应:
```json
{
  "code": 200,
  "message": "Successfully scanned and saved 15 dependencies to white-list",
  "success": true,
  "data": {
    "detectedLanguage": "python",
    "dependencyCount": 15,
    "savedCount": 15,
    "dependencies": [
      {"name": "requests", "version": "2.28.0"},
      {"name": "django", "version": "4.1.0"},
      ...
    ]
  }
}
```

### 2. 扫描PHP项目

```bash
curl -X POST http://localhost:8081/project/scan \
  -H "Content-Type: application/json" \
  -d '{
    "projectPath": "/path/to/php/project",
    "projectId": 2
  }'
```

### 3. 扫描JavaScript项目

```bash
curl -X POST http://localhost:8081/project/scan \
  -H "Content-Type: application/json" \
  -d '{
    "projectPath": "/path/to/node/project",
    "projectId": 3
  }'
```

### 4. 查询White-list

```bash
# 获取项目1的所有依赖
curl http://localhost:8081/project/whitelist/1

# 仅获取Python依赖
curl http://localhost:8081/project/whitelist/1/python

# 仅获取JavaScript依赖
curl http://localhost:8081/project/whitelist/1/javascript
```

## 错误处理

### 常见错误

**错误 1: 项目不存在**
```json
{
  "code": 500,
  "message": "Error during scanning",
  "success": false,
  "error": "Failed to detect language. HTTP 404"
}
```

**错误 2: 不支持的语言**
```json
{
  "code": 400,
  "message": "Unsupported language: cobol",
  "success": false
}
```

**错误 3: projectPath为空**
```json
{
  "code": 400,
  "message": "projectPath is required",
  "success": false
}
```

## 性能优化建议

1. **批量扫描** - 支持同时扫描多个项目
2. **缓存** - 缓存已检测的语言信息
3. **异步处理** - 大型项目可用异步任务处理
4. **数据库优化** - 为project_id和component_name创建索引

## 常见问题

**Q1: 如何扫描混合语言项目?**
A: 当前实现扫描主要语言。对于混合项目，可以多次调用API扫描不同的子目录。

**Q2: 如何更新已扫描的依赖?**
A: 当前实现会跳过重复的依赖。可以删除旧记录后重新扫描。

**Q3: 如何支持自定义包管理器?**
A: 修改PACKAGE_MANAGERS映射表并添加对应的解析器端点。

**Q4: 依赖版本为"unknown"怎么办?**
A: 某些项目管理器未在配置中指定版本时会出现。可手动修改或使用默认版本。

## 扩展建议

1. **添加更多语言** - 按照现有模式添加新语言的检测和解析
2. **描述获取** - 从PyPI/NPM/Maven等官方库获取组件描述
3. **安全检查** - 集成漏洞数据库检查依赖安全性
4. **依赖关系图** - 构建依赖之间的关系图

---

最后更新: 2025-11-14
版本: 2.0 (多语言支持)
