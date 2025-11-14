# White-list 组件描述存储机制详解

## 1. 现有White-list表结构

### 数据库表定义 (white_list)

```sql
CREATE TABLE white_list (
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL,              -- 组件名称 (e.g., "requests")
  file_path VARCHAR(500),                  -- 文件路径 (e.g., "/path/to/requirements.txt")
  description VARCHAR(1000),               -- 组件描述 (关键字段!)
  language VARCHAR(50),                    -- 编程语言 (e.g., "python", "java")
  isdelete INT DEFAULT 0                   -- 删除标记 (0=未删除, 1=已删除)
);
```

### Java实体类 (WhiteList.java)

```java
@Data
@TableName("white_list")
public class WhiteList {
    private int id;              // 主键
    private String name;         // 组件名称
    @TableField("file_path")
    private String filePath;     // 文件路径
    private String description;  // 组件描述 ← 这里存储描述
    private String language;     // 编程语言
    private int isdelete;        // 删除标记
}
```

---

## 2. 组件描述的存储方式

### 数据流程图

```
┌─────────────────────────────────────────────────────────────────┐
│                     Python项目扫描流程                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  Step 1: Flask解析Python项目                                    │
│  ├─ 调用 /parse/python_parse                                   │
│  ├─ 返回: [{name: "requests", version: "2.28.0"}, ...]        │
│  └─ 此时: 暂无描述信息                                          │
│                                                                   │
│  Step 2: 处理依赖列表                                           │
│  ├─ 循环遍历每个依赖                                            │
│  ├─ 为每个依赖生成描述信息                                      │
│  └─ 描述来源:                                                    │
│      a) 从Flask响应中获取 (如果有)                             │
│      b) 调用外部API获取 (PyPI API等)                          │
│      c) 使用默认描述                                            │
│                                                                   │
│  Step 3: 构建WhiteList对象                                      │
│  ├─ name = "requests"                                           │
│  ├─ version = "2.28.0" (存在name中或新增)                      │
│  ├─ description = "HTTP Library for Python"  ← 关键!           │
│  ├─ language = "python"                                         │
│  └─ filePath = "/path/to/requirements.txt"                     │
│                                                                   │
│  Step 4: 插入数据库                                             │
│  ├─ INSERT INTO white_list VALUES (...)                         │
│  └─ 成功! ✓                                                      │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. 改进的Python项目扫描服务 (支持描述)

### 修改 PythonProjectScanService.java

需要添加以下方法来获取和存储组件描述：

```java
/**
 * 为依赖获取详细描述
 * 策略:
 * 1. 检查Flask返回的数据中是否包含description
 * 2. 查询PyPI API获取官方描述
 * 3. 使用缓存避免重复查询
 * 4. 失败时使用默认描述
 */
private String getComponentDescription(PythonDependency dep) {
    // 1. 检查Flask返回的描述
    if (dep.getDescription() != null && !dep.getDescription().isEmpty()) {
        return dep.getDescription();
    }

    // 2. 查询PyPI API
    try {
        String description = queryPyPIDescription(dep.getName());
        if (description != null) {
            return description;
        }
    } catch (Exception e) {
        logger.warn("Failed to fetch PyPI description for: " + dep.getName());
    }

    // 3. 使用默认描述
    return generateDefaultDescription(dep);
}

/**
 * 从PyPI API查询组件描述
 */
private String queryPyPIDescription(String packageName) throws Exception {
    String url = "https://pypi.org/pypi/" + packageName + "/json";
    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

    if (response.getStatusCode().is2xxSuccessful()) {
        Map<String, Object> data = objectMapper.readValue(response.getBody(), Map.class);
        Map<String, Object> info = (Map<String, Object>) data.get("info");
        if (info != null) {
            return (String) info.get("summary");
        }
    }
    return null;
}

/**
 * 生成默认描述
 */
private String generateDefaultDescription(PythonDependency dep) {
    return String.format("Python package: %s v%s (Auto-detected)",
        dep.getName(), dep.getVersion());
}

/**
 * 修改保存方法 - 添加描述
 */
private int saveToWhiteList(List<PythonDependency> dependencies, Long projectId) {
    int savedCount = 0;

    for (PythonDependency dep : dependencies) {
        try {
            WhiteList whiteListEntry = new WhiteList();
            whiteListEntry.setName(dep.getName());
            whiteListEntry.setDescription(getComponentDescription(dep));  // ← 获取描述
            whiteListEntry.setLanguage("python");
            whiteListEntry.setFilePath("/path/to/requirements.txt");  // 可改进为实际路径
            whiteListEntry.setIsdelete(0);

            if (!existsInWhiteList(whiteListEntry)) {
                whiteListMapper.insert(whiteListEntry);
                savedCount++;
            }
        } catch (Exception e) {
            logger.error("Failed to save: " + dep.getName(), e);
        }
    }

    return savedCount;
}
```

---

## 4. 数据库存储示例

### 插入的实际数据

```sql
INSERT INTO white_list (id, name, file_path, description, language, isdelete)
VALUES
(1, 'requests', '/project/requirements.txt', 'A simple, yet elegant HTTP Library.', 'python', 0),
(2, 'flask', '/project/requirements.txt', 'A lightweight WSGI web application framework.', 'python', 0),
(3, 'numpy', '/project/requirements.txt', 'Fundamental package for array computing with Python.', 'python', 0),
(4, 'pandas', '/project/requirements.txt', 'Powerful data structures for data analysis, time series, and statistics.', 'python', 0),
(5, 'django', '/project/requirements.txt', 'A high-level Python web framework.', 'python', 0);
```

### 查询结果

```json
[
  {
    "id": 1,
    "name": "requests",
    "filePath": "/project/requirements.txt",
    "description": "A simple, yet elegant HTTP Library.",
    "language": "python",
    "isdelete": 0
  },
  {
    "id": 2,
    "name": "flask",
    "filePath": "/project/requirements.txt",
    "description": "A lightweight WSGI web application framework.",
    "language": "python",
    "isdelete": 0
  }
]
```

---

## 5. 改进方案 - 扩展白名单表结构

### 建议的新表结构 (white_list_v2)

```sql
CREATE TABLE white_list_v2 (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id BIGINT NOT NULL,                    -- 项目ID (新增)
  component_name VARCHAR(255) NOT NULL,          -- 组件名称
  component_version VARCHAR(100),                -- 组件版本 (新增)
  language VARCHAR(50),                          -- 编程语言
  package_manager VARCHAR(50),                   -- 包管理器 (新增: pip, npm等)
  description VARCHAR(2000),                     -- 组件描述 (扩大容量)
  source_url VARCHAR(500),                       -- 源URL (新增: PyPI, NPM等链接)
  source_type VARCHAR(50),                       -- 数据来源 (新增: pypi, npm, maven等)
  status VARCHAR(50) DEFAULT 'APPROVED',         -- 审批状态 (新增)
  created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  remark VARCHAR(500),                           -- 备注
  isdelete INT DEFAULT 0,

  INDEX idx_project_id (project_id),
  INDEX idx_component_name (component_name),
  UNIQUE KEY uq_project_component_version (project_id, component_name, component_version)
);
```

### 对应的Java实体

```java
@Data
@TableName("white_list_v2")
public class WhiteListV2 {
    private Long id;
    private Long projectId;
    private String componentName;
    private String componentVersion;
    private String language;
    private String packageManager;
    private String description;          // 详细描述
    private String sourceUrl;            // 源地址
    private String sourceType;           // 数据来源
    private String status;               // 审批状态
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    private String remark;
    private Integer isdelete;
}
```

---

## 6. 组件描述的多个来源

### 6.1 Flask API返回的描述

如果Flask的Python解析器已提供描述：
```json
{
  "obj": [
    {
      "name": "requests",
      "version": "2.28.0",
      "description": "A simple, yet elegant HTTP Library."  // ← Flask提供
    }
  ]
}
```

### 6.2 PyPI API获取

```python
# Python代码示例 (或Java HttpClient)
import requests

response = requests.get('https://pypi.org/pypi/requests/json')
data = response.json()
description = data['info']['summary']
print(description)  # "A simple, yet elegant HTTP Library."
```

### 6.3 NPM API获取

```bash
# 获取NPM包信息
curl https://registry.npmjs.org/express
# 返回 description 字段
```

### 6.4 Maven API获取

```bash
# 获取Maven组件信息
curl https://repo.maven.apache.org/maven2/.../metadata.xml
```

---

## 7. 实现步骤

### Step 1: 修改白名单实体类

```java
@Data
@TableName("white_list")
public class WhiteList {
    private int id;
    private String name;
    @TableField("file_path")
    private String filePath;
    private String description;        // 保留现有字段
    private String language;
    private int isdelete;

    // 新增辅助字段 (可选)
    @TableField(exist = false)
    private String version;            // 版本信息

    @TableField(exist = false)
    private String sourceUrl;          // 源链接
}
```

### Step 2: 修改保存逻辑

在 `PythonProjectScanService.java` 的 `saveToWhiteList` 方法中：

```java
private int saveToWhiteList(List<PythonDependency> dependencies, Long projectId) {
    int savedCount = 0;

    for (PythonDependency dep : dependencies) {
        try {
            WhiteList entry = new WhiteList();
            entry.setName(dep.getName());
            entry.setLanguage("python");
            entry.setFilePath("/path/to/project");

            // 关键: 获取并设置描述
            String description = getComponentDescription(dep);
            entry.setDescription(description);
            entry.setIsdelete(0);

            if (!existsInWhiteList(entry)) {
                whiteListMapper.insert(entry);
                savedCount++;
            }
        } catch (Exception e) {
            logger.error("Error saving component", e);
        }
    }

    return savedCount;
}
```

### Step 3: 添加描述获取方法

```java
private String getComponentDescription(PythonDependency dep) {
    // 优先级:
    // 1. Flask返回的描述
    // 2. PyPI API获取
    // 3. 默认描述

    if (dep.getDescription() != null && !dep.getDescription().isEmpty()) {
        return dep.getDescription();
    }

    try {
        String pypiDesc = fetchFromPyPI(dep.getName());
        if (pypiDesc != null) {
            return pypiDesc;
        }
    } catch (Exception e) {
        logger.warn("PyPI fetch failed for: " + dep.getName());
    }

    return String.format("Python package: %s", dep.getName());
}

private String fetchFromPyPI(String packageName) throws Exception {
    String url = "https://pypi.org/pypi/" + URLEncoder.encode(packageName, "UTF-8") + "/json";

    try {
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class, 3000);
        if (response.getStatusCode().is2xxSuccessful()) {
            Map data = objectMapper.readValue(response.getBody(), Map.class);
            Map info = (Map) data.get("info");
            return (String) info.get("summary");
        }
    } catch (Exception e) {
        logger.debug("PyPI API error: " + e.getMessage());
    }

    return null;
}
```

---

## 8. 查询White-list的接口

### 获取所有白名单条目

```bash
GET /white-list/all
```

```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "name": "requests",
      "filePath": "/project/requirements.txt",
      "description": "A simple, yet elegant HTTP Library.",
      "language": "python"
    },
    {
      "id": 2,
      "name": "flask",
      "filePath": "/project/requirements.txt",
      "description": "A lightweight WSGI web application framework.",
      "language": "python"
    }
  ]
}
```

### 按语言查询

```bash
GET /white-list?language=python
```

### 按项目查询

```bash
GET /white-list?projectId=1
```

---

## 9. 常见问题

**Q1: 描述字段为空怎么办?**
A: 按优先级填充:
1. Flask返回的描述
2. 外部API (PyPI/NPM/Maven)
3. 生成默认描述

**Q2: 如何处理长描述?**
A: 在数据库中使用TEXT类型，或在Java中使用VARCHAR(2000以上)

**Q3: 如何更新已有的描述?**
A: 实现更新接口或定期爬取最新信息

**Q4: 如何处理私有包?**
A: 支持配置私有源URL (如私有Nexus, Artifactory等)

---

*最后更新: 2025-11-13*

