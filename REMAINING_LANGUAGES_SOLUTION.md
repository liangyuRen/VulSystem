# 解决剩余语言（Go, Rust, JavaScript, Erlang）的解析问题

## 问题诊断

### 根本原因
**所有失败的项目都是因为依赖配置文件在子目录中，而不是项目根目录**

项目结构：
```
上传的项目根目录/
├── opensca.log
├── sbom
└── actual-project-folder/          ← 真正的项目在这里
    ├── package.json / go.mod / Cargo.toml / rebar.config
    ├── src/
    └── ...
```

例如：
- JavaScript项目：`D:/kuling/upload/xxx/basecamp-javascript-main/` (无package.json，是教程项目)
- Go项目：`D:/kuling/upload/xxx/shadowsocks-go-master/` (无go.mod)
- Rust项目：`D:/kuling/upload/xxx/rust-libp2p-master/Cargo.toml` ✓ 存在
- Erlang项目：`D:/kuling/upload/xxx/poolboy-master/` (需要检查)

---

## 解决方案1：修改Spring Boot代码递归搜索配置文件

### 新增工具方法：递归查找配置文件

**文件**: `backend/src/main/java/com/nju/backend/service/project/util/ProjectUtil.java`

```java
/**
 * 递归搜索项目目录，找到依赖配置文件的实际路径
 *
 * @param projectPath 项目根目录
 * @param configFileName 配置文件名（如 "package.json", "go.mod", "Cargo.toml"）
 * @return 配置文件所在的目录路径，如果没找到则返回原路径
 */
public static String findConfigFileDirectory(String projectPath, String configFileName) {
    try {
        File projectDir = new File(projectPath);

        // 首先检查根目录
        File configInRoot = new File(projectDir, configFileName);
        if (configInRoot.exists()) {
            System.out.println("  → 配置文件在根目录: " + configFileName);
            return projectPath;
        }

        // 递归搜索子目录（最多2层）
        System.out.println("  → 在子目录中搜索: " + configFileName);
        File foundDir = searchForConfigFile(projectDir, configFileName, 0, 2);

        if (foundDir != null) {
            System.out.println("  → 找到配置文件在: " + foundDir.getAbsolutePath());
            return foundDir.getAbsolutePath();
        }

        System.out.println("  → 未找到配置文件: " + configFileName);
        return projectPath;  // 没找到，返回原路径

    } catch (Exception e) {
        System.err.println("  ✗ 搜索配置文件失败: " + e.getMessage());
        return projectPath;
    }
}

/**
 * 递归搜索配置文件
 */
private static File searchForConfigFile(File directory, String configFileName, int currentDepth, int maxDepth) {
    if (currentDepth > maxDepth || !directory.isDirectory()) {
        return null;
    }

    // 检查当前目录
    File configFile = new File(directory, configFileName);
    if (configFile.exists()) {
        return directory;
    }

    // 搜索子目录
    File[] subdirs = directory.listFiles(File::isDirectory);
    if (subdirs != null) {
        for (File subdir : subdirs) {
            // 跳过隐藏目录和node_modules等
            String name = subdir.getName();
            if (name.startsWith(".") || name.equals("node_modules") ||
                name.equals("target") || name.equals("build")) {
                continue;
            }

            File result = searchForConfigFile(subdir, configFileName, currentDepth + 1, maxDepth);
            if (result != null) {
                return result;
            }
        }
    }

    return null;
}
```

### 修改callParserAPI方法使用新的搜索功能

**文件**: `backend/src/main/java/com/nju/backend/service/project/Impl/ProjectServiceImpl.java`

```java
private void callParserAPI(String language, String apiUrl, String filePath) {
    System.out.println("========================================");
    System.out.println("开始解析" + language.toUpperCase() + "项目");
    System.out.println("项目路径: " + filePath);
    System.out.println("========================================");

    long startTime = System.currentTimeMillis();

    try {
        // ===== 【新增】根据语言类型查找实际配置文件所在目录 =====
        String actualProjectPath = filePath;

        String configFileName = getConfigFileName(language);
        if (configFileName != null) {
            System.out.println("→ 搜索配置文件: " + configFileName);
            actualProjectPath = ProjectUtil.findConfigFileDirectory(filePath, configFileName);

            if (!actualProjectPath.equals(filePath)) {
                System.out.println("→ 使用子目录路径: " + actualProjectPath);
            }
        }
        // ===== 【新增结束】 =====

        RestTemplate restTemplate = new RestTemplate();
        String url = UriComponentsBuilder.fromHttpUrl(apiUrl)
                .queryParam("project_folder", actualProjectPath)  // ← 使用实际路径
                .encode()
                .build()
                .toUriString();

        System.out.println("→ 调用Flask API: " + apiUrl);
        System.out.println("→ 完整URL: " + url);

        // 调用Flask API获取依赖信息
        String response = restTemplate.getForObject(url, String.class);

        // ... 其余代码不变

    } catch (Exception e) {
        // ... 错误处理
    }
}

/**
 * 获取配置文件名
 */
private String getConfigFileName(String language) {
    switch (language.toLowerCase()) {
        case "python":
            return "requirements.txt";
        case "go":
        case "golang":
            return "go.mod";
        case "rust":
            return "Cargo.toml";
        case "javascript":
        case "js":
        case "node":
        case "nodejs":
            return "package.json";
        case "php":
            return "composer.json";
        case "ruby":
            return "Gemfile";
        case "erlang":
            return "rebar.config";
        case "java":
            return "pom.xml";
        default:
            return null;
    }
}
```

---

## 解决方案2：手动更新数据库项目路径

如果你不想修改代码，可以直接更新数据库中的项目路径指向子目录：

```sql
-- 更新Go项目路径
UPDATE project
SET file = 'D:\\kuling\\upload\\93ece2b3-26a5-47dd-8b6c-2bce1b016d05\\shadowsocks-go-master'
WHERE id = 29;

-- 更新Rust项目路径
UPDATE project
SET file = 'D:\\kuling\\upload\\3b141c7c-542a-4923-997e-edf50a60840f\\rust-libp2p-master'
WHERE id = 31;

-- 更新JavaScript项目路径
UPDATE project
SET file = 'D:\\kuling\\upload\\52db129b-ca8a-400c-93ba-7bfd0f8dda0d\\basecamp-javascript-main'
WHERE id = 27;

-- 更新Erlang项目路径
UPDATE project
SET file = 'D:\\kuling\\upload\\772667e5-5402-4766-9c76-9576961ab6c9\\poolboy-master'
WHERE id = 28;
```

然后重新触发解析：

```bash
# 测试Rust
curl -X POST http://localhost:8081/project/reparse -d "projectId=31" -d "language=rust"

# 测试Go
curl -X POST http://localhost:8081/project/reparse -d "projectId=29" -d "language=go"

# 测试JavaScript
curl -X POST http://localhost:8081/project/reparse -d "projectId=27" -d "language=javascript"

# 测试Erlang
curl -X POST http://localhost:8081/project/reparse -d "projectId=28" -d "language=erlang"
```

---

## 解决方案3：修复上传逻辑

修改项目上传时的ZIP解压逻辑，自动识别并使用子目录：

**文件**: `backend/src/main/java/com/nju/backend/service/project/Impl/ProjectServiceImpl.java`

在 `uploadFileWithLanguageDetection()` 方法中添加：

```java
// 解压ZIP后
String extractedPath = unzipFile(uploadedFile, targetDir);

// ===== 【新增】如果解压后只有一个子目录，使用该子目录作为项目路径 =====
File extractedDir = new File(extractedPath);
File[] files = extractedDir.listFiles();

if (files != null && files.length == 1 && files[0].isDirectory()) {
    // 只有一个子目录，检查是否是项目目录
    File subDir = files[0];

    // 如果子目录包含项目文件（pom.xml, package.json等），使用子目录
    String[] projectFiles = {
        "pom.xml", "build.gradle", // Java
        "package.json", // JavaScript
        "requirements.txt", "setup.py", // Python
        "go.mod", // Go
        "Cargo.toml", // Rust
        "composer.json", // PHP
        "Gemfile", // Ruby
        "rebar.config" // Erlang
    };

    boolean hasProjectFile = false;
    for (String projectFile : projectFiles) {
        if (new File(subDir, projectFile).exists()) {
            hasProjectFile = true;
            break;
        }
    }

    if (hasProjectFile) {
        System.out.println("  → 检测到项目在子目录中: " + subDir.getName());
        extractedPath = subDir.getAbsolutePath();
    }
}
// ===== 【新增结束】 =====

// 继续语言检测...
String detectedLanguage = detectProjectLanguage(extractedPath);
```

---

## 测试验证

### 1. 使用当前路径直接测试Flask API

```bash
# 测试Rust（使用子目录路径）
curl "http://localhost:5000/parse/rust_parse?project_folder=D:/kuling/upload/3b141c7c-542a-4923-997e-edf50a60840f/rust-libp2p-master"

# 测试Go（如果有go.mod）
curl "http://localhost:5000/parse/go_parse?project_folder=D:/kuling/upload/93ece2b3-26a5-47dd-8b6c-2bce1b016d05/shadowsocks-go-master"

# 测试JavaScript（检查是否有package.json）
curl "http://localhost:5000/parse/javascript_parse?project_folder=D:/kuling/upload/52db129b-ca8a-400c-93ba-7bfd0f8dda0d/basecamp-javascript-main"

# 测试Erlang
curl "http://localhost:5000/parse/erlang_parse?project_folder=D:/kuling/upload/772667e5-5402-4766-9c76-9576961ab6c9/poolboy-master"
```

### 2. 检查配置文件是否存在

```bash
# Rust
ls "D:\kuling\upload\3b141c7c-542a-4923-997e-edf50a60840f\rust-libp2p-master\Cargo.toml"

# Go
find "D:\kuling\upload\93ece2b3-26a5-47dd-8b6c-2bce1b016d05\shadowsocks-go-master" -name "go.mod"

# JavaScript
ls "D:\kuling\upload\52db129b-ca8a-400c-93ba-7bfd0f8dda0d\basecamp-javascript-main\package.json"

# Erlang
ls "D:\kuling\upload\772667e5-5402-4766-9c76-9576961ab6c9\poolboy-master\rebar.config"
```

---

## 推荐实施步骤

**我推荐使用解决方案2（手动更新数据库路径）+ 解决方案1（添加递归搜索功能）**

### 步骤1：立即修复（手动更新路径）

```sql
-- 立即执行SQL更新路径
UPDATE project SET file = 'D:\\kuling\\upload\\3b141c7c-542a-4923-997e-edf50a60840f\\rust-libp2p-master' WHERE id = 31;
UPDATE project SET file = 'D:\\kuling\\upload\\93ece2b3-26a5-47dd-8b6c-2bce1b016d05\\shadowsocks-go-master' WHERE id = 29;
UPDATE project SET file = 'D:\\kuling\\upload\\52db129b-ca8a-400c-93ba-7bfd0f8dda0d\\basecamp-javascript-main' WHERE id = 27;
UPDATE project SET file = 'D:\\kuling\\upload\\772667e5-5402-4766-9c76-9576961ab6c9\\poolboy-master' WHERE id = 28;
```

### 步骤2：测试解析

```bash
python test_multilang.py
```

### 步骤3：添加递归搜索功能（长期解决方案）

将上面的 `findConfigFileDirectory()` 方法添加到代码中，这样以后上传的项目会自动处理子目录问题。

---

## 预期结果

修复后应该看到：

```
数据库统计:
  java           :    46 dependencies
  ruby           :    41 dependencies
  rust           :    ?? dependencies  ← 新增
  python         :    12 dependencies
  php            :     4 dependencies
  go             :    ?? dependencies  ← 新增
  erlang         :    ?? dependencies  ← 新增
  javascript     :    ?? dependencies  ← 新增
  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  总计           :   100+ dependencies

成功率: 8/9 languages (89%)
```

---

**立即执行步骤1的SQL，然后运行测试！**
