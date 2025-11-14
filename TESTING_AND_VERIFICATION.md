# 语言检测修复 - 测试和验收指南

## 📋 修复总结

### 修改的文件
1. **ProjectServiceImpl.java**
   - 保留原 `uploadFile()` 方法向后兼容
   - 新增 `uploadFileWithLanguageDetection()` 方法实现精确检测和异步解析

2. **ProjectService.java**
   - 添加新方法 `uploadFileWithLanguageDetection()` 接口声明

3. **ProjectController.java**
   - 改造 `uploadProject()` 接口，移除前端 language 参数
   - 调用新的 `uploadFileWithLanguageDetection()` 获取检测结果
   - 使用检测到的语言创建项目而非前端参数

4. **ProjectUtil.java**
   - 扩展 `detectProjectType()` 方法，支持更多语言
   - 添加对 Python, Rust, Go, Node.js 的检测

### 核心改进
```
修复前：
uploadProject() → uploadFile() → 硬编码"java" → createProject("java")
                              ↓ (异步解析不执行)
                         所有项目都标记为 java

修复后：
uploadProject() → uploadFileWithLanguageDetection() → detectProjectType()
                            ↓
                      返回 {filePath, language}
                            ↓
                    createProject(language)  ✓ 使用检测结果
                            ↓ (异步解析正确执行)
                 正确的 Parser 被调用，白名单被正确保存
```

---

## ✅ 测试计划

### 前置条件
- MySQL 服务运行中
- Flask 后端服务运行（/parse/pom_parse, /parse/c_parse）
- Spring Boot 后端服务已启动

### 测试用例

#### 测试1：Java 项目检测
```
项目信息：
- 上传项目：huaweicloud-sdk-java-dis（Maven项目）
- 预期检测语言：java
- 预期白名单：有 Java 依赖

测试步骤：
1. POST /project/uploadProject
   {
     "file": huaweicloud-sdk-java-dis.zip,
     "name": "test-java-project",
     "description": "Java test",
     "companyId": 1
   }

2. 检查响应：
   {
     "detectedLanguage": "java",
     "message": "项目上传成功，检测到语言: java"
   }

3. 数据库验证：
   SELECT id, name, language FROM project WHERE name='test-java-project';
   → language 应该是 'java' ✓

4. 白名单验证：
   SELECT COUNT(*), language FROM white_list
   WHERE file_path LIKE '%test-java%';
   → 应该有 Java 依赖，language='java' ✓
```

#### 测试2：C/C++ 项目检测
```
项目信息：
- 上传项目：某个 C/C++ 项目（包含 CMakeLists.txt 或 .c/.cpp 文件）
- 预期检测语言：c
- 预期白名单：有 C/C++ 依赖

测试步骤：
1. POST /project/uploadProject
   {
     "file": cpp-project.zip,
     "name": "test-cpp-project",
     "description": "C++ test",
     "companyId": 1
   }

2. 检查响应：
   → detectedLanguage: "c"

3. 数据库验证：
   SELECT language FROM project WHERE name='test-cpp-project';
   → language = 'c' ✓

4. 白名单验证：
   SELECT COUNT(*), language FROM white_list
   WHERE file_path LIKE '%test-cpp%';
   → language='c' ✓
```

#### 测试3：Rust 项目检测（验证现有问题修复）
```
项目信息：
- 已存在的项目：id=26, name='rust'（之前错误标记为 java）
- 预期检测语言：rust
- 当前问题：database 中 language='java'，但应该是 'rust'

现场测试：
1. 重新创建该项目的测试版本：
   POST /project/uploadProject
   {
     "file": rustdesk-master.zip,
     "name": "test-rust-project",
     "description": "Rust test",
     "companyId": 1
   }

2. 检查响应：
   → detectedLanguage: "rust"
   → message: "项目上传成功，检测到语言: rust"

3. 数据库验证：
   SELECT id, name, language FROM project WHERE name='test-rust-project';
   → language = 'rust' ✓（不再是 'java'）

4. 对比修复前后：
   修复前：所有项目的 language = 'java'
   修复后：项目的 language 与检测结果一致
```

#### 测试4：未知语言项目
```
项目信息：
- 上传不包含已知特征的项目
- 预期检测语言：unknown

测试步骤：
1. POST /project/uploadProject
   {
     "file": unknown-project.zip,
     "name": "test-unknown",
     "companyId": 1
   }

2. 检查响应：
   → detectedLanguage: "unknown"
   → message: "项目上传成功，检测到语言: unknown"

3. 数据库验证：
   SELECT language FROM project WHERE name='test-unknown';
   → language = 'unknown' ✓

4. 验证：
   → 无异步解析触发（应该在日志中看到 "不支持的项目类型"）
   → white_list 表中无相关记录 ✓
```

---

## 🔍 验证标准

### 后台日志检查

#### 上传成功时应该看到：
```
=== uploadProject 接口被调用 ===
文件名: xxx.zip
项目名: xxx
companyId: 1

步骤1: 开始上传并检测语言...
文件解压完成，路径: D:\kuling\upload\xxxxx

DEBUG: 检测项目类型，路径: D:\kuling\upload\xxxxx
DEBUG: 发现Java特征文件: pom.xml
DEBUG: 检测结果 => java
✓ 检测到项目语言: java

步骤2: 文件上传成功
  - 文件路径: D:\kuling\upload\xxxxx
  - 检测语言: java

步骤3: 开始创建项目，使用检测到的语言: java
步骤4: 项目创建成功

准备触发异步解析，语言类型: java
✓ 启动Java项目解析任务

开始解析Java项目: D:\kuling\upload\xxxxx
调用POM解析API: http://localhost:5000/parse/pom_parse?project_folder=...
POM解析响应长度: xxxx
解析出依赖库数量: xx
成功插入依赖库数量: xx
```

### 数据库检查清单

#### Project 表
```sql
-- 检查语言字段是否正确
SELECT id, name, language, file FROM project
WHERE is_delete = 0
ORDER BY create_time DESC
LIMIT 10;

预期：
- java 项目 → language = 'java'
- c 项目 → language = 'c'
- rust 项目 → language = 'rust'
- 不再所有项目都是 'java'
```

#### WhiteList 表
```sql
-- 检查依赖的语言是否与项目一致
SELECT
  p.name as project_name,
  p.language as project_language,
  COUNT(w.id) as dependency_count,
  GROUP_CONCAT(DISTINCT w.language) as whitelist_languages
FROM project p
LEFT JOIN white_list w ON p.file = w.file_path
WHERE p.is_delete = 0 AND w.isdelete = 0
GROUP BY p.id, p.name, p.language
ORDER BY p.create_time DESC
LIMIT 10;

预期：
- project_language 和 whitelist_languages 应该一致
- 不存在 language mismatch 的情况
```

---

## 🚀 快速测试脚本

### 场景1：验证 Java 项目识别
```bash
# 查看已有的 Java 项目
find "D:\kuling\upload\huaweicloud-sdk-java-dis" -name "pom.xml" -o -name "*.java"

# 创建测试请求（使用 curl）
curl -X POST http://localhost:8081/project/uploadProject \
  -F "file=@test-java.zip" \
  -F "name=test-java" \
  -F "description=Testing Java detection" \
  -F "companyId=1"

# 验证数据库
mysql -u root -p kulin -e "
SELECT language FROM project WHERE name='test-java';"
```

### 场景2：验证 Rust 项目识别
```bash
# 查看现有 Rust 项目的文件
find "D:\kuling\upload\a3034e5e-3f78-4e36-bebc-da92209d246c" \
  -name "Cargo.toml" -o -name "*.rs" | head -5

# 验证修复：同一项目重新上传应该检测为 rust 而不是 java
curl -X POST http://localhost:8081/project/uploadProject \
  -F "file=@rustdesk.zip" \
  -F "name=test-rust" \
  -F "description=Testing Rust detection" \
  -F "companyId=1"

# 验证结果
mysql -u root -p kulin -e "
SELECT id, name, language FROM project WHERE name='test-rust';"
```

---

## 📊 预期修复效果对比

### 修复前（当前状态）
```
数据库状态：
+-----+--------+----------+---------------------------------+
| id  | name   | language | file                            |
+-----+--------+----------+---------------------------------+
| 26  | rust   | java     | D:\kuling\upload\a3034... (✗)  |
| 25  | php    | java     | D:\kuling\upload\b39b... (✗)  |
| 24  | python | java     | D:\kuling\upload\ab37... (✗)  |
| 23  | mall   | java     | D:\kuling\upload\9c79... (✓)  |
+-----+--------+----------+---------------------------------+

白名单状态：
+----------+-------------------+
| language | dependency_count  |
+----------+-------------------+
| java     | 46                |
+----------+-------------------+
问题：只有 java，其他语言的项目没有依赖数据
```

### 修复后（预期）
```
数据库状态：
+-----+----------+----------+---------------------------------+
| id  | name     | language | file                            |
+-----+----------+----------+---------------------------------+
| 30  | rust     | rust     | D:\kuling\upload\new1... (✓)   |
| 29  | cpp      | c        | D:\kuling\upload\new2... (✓)   |
| 28  | python   | python   | D:\kuling\upload\new3... (✓)   |
| 27  | java     | java     | D:\kuling\upload\new4... (✓)   |
+-----+----------+----------+---------------------------------+

白名单状态：
+----------+-------------------+
| language | dependency_count  |
+----------+-------------------+
| java     | 46                |
| c        | 12                |
| rust     | 8                 |
| python   | 5                 |
+----------+-------------------+
改进：每种语言的项目都有对应的依赖数据
```

---

## 🔧 故障排查

### 问题：所有项目仍然检测为 'java'
**可能原因**：
1. 应用未重新启动（旧代码仍在运行）
2. detectProjectType() 方法有 bug

**排查步骤**：
```bash
# 1. 确认应用已重启
ps aux | grep java

# 2. 查看后台日志是否显示新的检测逻辑
tail -f logs/application.log | grep "检测项目类型"

# 3. 手动测试检测逻辑
```

### 问题：项目检测为 'unknown'
**可能原因**：
1. 项目文件不标准
2. detectProjectType() 的检测规则不完整

**排查步骤**：
```bash
# 查看项目中实际有哪些文件
find "项目路径" -type f -name "pom.xml" -o -name "*.java" \
  -o -name "Cargo.toml" -o -name "*.rs" | head -20
```

### 问题：异步解析未触发
**症状**：white_list 表中没有新数据

**可能原因**：
1. Flask 端服务未启动或无法访问
2. 项目语言检测为 'unknown'
3. 异步线程池未配置

**排查步骤**：
```bash
# 1. 检查 Flask 服务
curl http://localhost:5000/parse/pom_parse?project_folder=...

# 2. 查看 Spring Boot 日志中的异步线程执行情况
grep -i "async" logs/application.log

# 3. 检查后台是否看到 "启动Java项目解析任务" 的日志
```

---

## ✨ 验收标准总结

| 测试项 | 预期结果 | 验证方式 |
|--------|---------|--------|
| Java 项目检测 | language='java' | SELECT language FROM project WHERE name='test-java' |
| Rust 项目检测 | language='rust' | SELECT language FROM project WHERE name='test-rust' |
| C/C++ 项目检测 | language='c' | SELECT language FROM project WHERE name='test-cpp' |
| 未知项目 | language='unknown' | 应无异步解析，无白名单数据 |
| 日志完整性 | 显示检测过程和结果 | 后台日志包含 "检测项目类型" 和 "✓ 检测到项目语言" |
| 白名单语言一致 | project.language = whitelist.language | 按项目分组统计，无不匹配 |
| API 响应 | 返回 detectedLanguage | 响应包含 "detectedLanguage" 字段 |

---

## 📝 测试记录模板

```
测试日期：2025-11-13
测试人员：[名称]
测试环境：Windows, MySQL, Spring Boot, Flask

测试用例1：Java 项目
□ 上传成功
□ 检测语言正确（java）
□ 数据库记录正确
□ 白名单已插入
□ 日志显示异步解析

测试用例2：Rust 项目
□ 上传成功
□ 检测语言正确（rust）
□ 数据库记录正确
□ 日志显示 "不支持的项目类型" 或正确的 parser 调用

测试用例3：C/C++ 项目
□ 上传成功
□ 检测语言正确（c）
□ 数据库记录正确
□ 白名单已插入

测试用例4：未知项目
□ 上传成功
□ 检测语言为 unknown
□ 数据库记录正确
□ 日志显示 "不支持的项目类型"

总体结论：□ 通过 / □ 需要修复
```
