# 项目上传自动语言检测功能说明

## 功能概述

在上传项目时，系统会自动检测项目的编程语言，并将检测结果存储到数据库的 `project.language` 字段中。

## 实现方式

### 1. 检测流程

当用户通过 `/project/uploadProject` 接口上传项目时：

```
上传文件 → 解压到服务器 → 调用语言检测 → 创建项目记录（包含检测到的语言）
```

### 2. 语言检测策略

**优先使用 Flask API 检测**（准确度高）：
- 调用 `http://127.0.0.1:5000/parse/get_primary_language`
- 使用优化的算法分析项目结构
- 支持17+种编程语言

**回退到本地文件扫描**（Flask服务不可用时）：
- 扫描项目文件特征
- 根据配置文件和源码文件判断语言类型
- 支持：Java, Python, Go, Rust, PHP, Ruby, Erlang, C/C++, JavaScript

### 3. 支持的语言

| 语言 | 检测特征 |
|------|---------|
| Java | pom.xml, build.gradle, *.java |
| Python | requirements.txt, setup.py, *.py |
| Go | go.mod, go.sum, *.go |
| Rust | Cargo.toml, *.rs |
| PHP | composer.json, *.php |
| Ruby | Gemfile, *.rb |
| Erlang | rebar.config, *.erl |
| JavaScript | package.json, *.js, *.ts |
| C/C++ | CMakeLists.txt, Makefile, *.c, *.cpp |

## 使用指南

### 前端上传项目（推荐）

使用 `/project/uploadProject` 接口：

```javascript
const formData = new FormData();
formData.append('file', projectZipFile);
formData.append('name', '项目名称');
formData.append('description', '项目描述');
formData.append('companyId', 1);
formData.append('riskThreshold', 5); // 可选

fetch('http://localhost:8081/project/uploadProject', {
  method: 'POST',
  body: formData
})
.then(res => res.json())
.then(data => {
  console.log('检测到的语言:', data.data.detectedLanguage);
  console.log('项目上传成功');
});
```

**返回示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "status": "analyzing",
    "message": "项目上传成功，检测到语言: python",
    "detectedLanguage": "python",
    "filePath": "D:\\kuling\\upload\\xxx-xxx-xxx"
  }
}
```

### 命令行测试

```bash
# 上传Python项目
curl -X POST "http://localhost:8081/project/uploadProject" \
  -F "file=@your-project.zip" \
  -F "name=Python测试项目" \
  -F "description=测试Python语言检测" \
  -F "companyId=1"

# 上传Go项目
curl -X POST "http://localhost:8081/project/uploadProject" \
  -F "file=@go-project.zip" \
  -F "name=Go测试项目" \
  -F "description=测试Go语言检测" \
  -F "companyId=1"
```

## 验证语言检测结果

### 方法1: 查询数据库

```sql
-- 查看最近上传的项目及其语言
SELECT id, name, language, create_time
FROM project
ORDER BY id DESC
LIMIT 10;
```

### 方法2: 调用项目信息接口

```bash
curl "http://localhost:8081/project/info?projectid=1"
```

返回的 `language` 字段即为检测结果。

## 测试已有项目

对于已经在数据库中的项目（language字段为java），可以通过扫描接口更新语言：

```bash
curl -X POST "http://localhost:8081/project/scan" \
  -H "Content-Type: application/json" \
  -d '{
    "projectPath": "D:/kuling/upload/66dd438b-44bb-4cf0-98ab-5f302c461099",
    "projectId": 32
  }'
```

该接口会：
1. 检测项目语言
2. 更新 `project.language` 字段
3. 解析项目依赖
4. 保存到white-list表

## 代码实现关键点

### 1. ProjectController.java (第59-109行)

```java
@PostMapping("/uploadProject")
public RespBean uploadProject(
        @RequestParam("file") MultipartFile file,
        @RequestParam("name") String name,
        @RequestParam("description") String description,
        @RequestParam("companyId") int companyId) {

    // 上传文件并自动检测语言
    Map<String, Object> uploadResult = projectService.uploadFileWithLanguageDetection(file);
    String filePath = (String) uploadResult.get("filePath");
    String detectedLanguage = (String) uploadResult.get("language");

    // 使用检测到的语言创建项目
    projectService.createProject(name, description, detectedLanguage,
                                 riskThresholdValue, companyId, filePath);

    return RespBean.success(...);
}
```

### 2. ProjectUtil.java - detectProjectType()

**优先Flask API**:
```java
private String detectLanguageUsingFlaskAPI(String projectPath) throws Exception {
    String url = FLASK_BASE_URL + "/parse/get_primary_language?project_folder=" + encodedPath;
    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
    Map<String, Object> responseBody = objectMapper.readValue(response.getBody(), Map.class);
    return (String) responseBody.get("language");
}
```

**回退本地扫描**:
```java
private String detectLanguageByFileScanning(Path path) throws IOException {
    // 扫描文件特征
    Files.walk(path, 3).forEach(file -> {
        // 检测各种语言特征文件
    });

    // 按优先级返回
    if (hasJava[0]) return "java";
    if (hasRust[0]) return "rust";
    // ...
}
```

## 重启服务后测试

1. **重启Spring Boot服务**
2. **确保Flask服务运行在 http://127.0.0.1:5000**
3. **上传测试项目**
4. **验证数据库中的language字段**

## 常见问题

**Q: 如果Flask服务未启动会怎样？**
A: 系统会自动回退到本地文件扫描模式，仍能检测常见语言。

**Q: 检测结果不准确怎么办？**
A: 可以手动调用 `/project/update` 接口更新language字段，或检查Flask服务是否正常。

**Q: 支持混合语言项目吗？**
A: 系统会检测主要语言（占比最高的语言）。

## 测试数据

数据库中现有的测试项目：

| ID | 项目名 | 当前language | 实际应该是 |
|----|--------|-------------|-----------|
| 28 | erlang测试解析项目 | java | erlang |
| 29 | go测试解析项目 | java | go |
| 30 | php测试解析项目 | java | php |
| 31 | rust测试解析项目 | java | rust |
| 32 | python测试解析项目 | java | python |
| 33 | ruby测试解析项目 | java | ruby |

可以通过扫描接口批量更新这些项目的语言字段。
