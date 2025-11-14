# 白名单表实时更新验证指南

## 目标
展示在项目上传过程中，white_list表如何被实时更新为新语言的组件数据。

---

## 关键代码位置

### 1. 白名单组件保存的核心代码

**文件**: `ProjectServiceImpl.java`
**方法**: `callParserAPI()` (第826-866行)
**最关键的部分**（第852-860行）：

```java
int insertCount = 0;
for (WhiteList whiteList : whiteLists) {
    whiteList.setFilePath(filePath);        // 设置项目文件路径
    whiteList.setLanguage(language);        // ✓ 保存语言（这是关键！）
    whiteList.setIsdelete(0);               // 标记为有效
    if (whiteListMapper.insert(whiteList) > 0) {
        insertCount++;
    }
}
System.out.println("成功插入" + language + "依赖库数量: " + insertCount);
```

### 2. 调用流程

```
uploadFileWithLanguageDetection()
  ↓
switch(detectedLanguage) {
  case "python":
    asyncParsePythonProject(filePath);  ← 触发异步
    break;
}
  ↓
asyncParsePythonProject(filePath)  @Async
  ↓
callParserAPI("python", "http://localhost:5000/parse/python_parse", filePath)
  ↓
whiteListMapper.insert(whiteList)  ← 写入数据库
```

---

## 完整的数据库更新演示

### 场景：上传Python项目 `my-app.zip`

#### 阶段1：项目文件处理（同步）

```
前端请求：
POST /project/uploadProject

参数：
  file = my-app.zip（包含requirements.txt、setup.py、main.py）
  name = my-python-app
  companyId = 1

后端处理：
1. 解压文件到：D:\kuling\upload\12345678\
2. 检测语言：
   ✓ 找到 requirements.txt
   ✓ 找到 setup.py
   ✓ 找到 *.py 文件
   → 返回 "python"

3. 创建项目记录：
   SQL: INSERT INTO project(name, language, file)
        VALUES('my-python-app', 'python', 'D:\kuling\upload\12345678\')
   结果：project 表新增 id=30
```

#### 阶段2：异步解析与白名单更新（@Async）

```
触发：asyncParsePythonProject(filePath)
调用：callParserAPI("python", "http://localhost:5000/parse/python_parse", filePath)

步骤1：构建Flask请求URL
  https://localhost:5000/parse/python_parse?project_folder=D%3A%5Ckuling%5Cupload%5C12345678%5C

步骤2：调用Flask解析器
  Flask接收请求 → 读取requirements.txt → 解析依赖列表
  Flask返回JSON：
  [
    {
      "name": "requests",
      "version": "2.28.0"
    },
    {
      "name": "numpy",
      "version": "1.23.0"
    },
    {
      "name": "pandas",
      "version": "1.4.0"
    },
    {
      "name": "flask",
      "version": "2.1.0"
    }
  ]

步骤3：解析JSON并保存到white_list
  projectUtil.parseJsonData(response)
  → List<WhiteList> = [WhiteList对象1, WhiteList对象2, WhiteList对象3, WhiteList对象4]

步骤4：遍历列表插入数据库（关键！）

  ─── 第1次循环 ───
  whiteList.name = "requests"
  whiteList.version = "2.28.0"
  whiteList.setFilePath("D:\kuling\upload\12345678\")
  whiteList.setLanguage("python")                    ← ✓ 关键设置
  whiteList.setIsdelete(0)
  whiteListMapper.insert(whiteList)
  → SQL: INSERT INTO white_list(name, version, language, file_path, isdelete)
         VALUES('requests', '2.28.0', 'python', 'D:\kuling\upload\12345678\', 0)
  → 数据库响应：✓ 插入成功，id=47

  ─── 第2次循环 ───
  whiteList.name = "numpy"
  whiteList.version = "1.23.0"
  whiteList.setFilePath("D:\kuling\upload\12345678\")
  whiteList.setLanguage("python")                    ← ✓ 关键设置
  whiteList.setIsdelete(0)
  whiteListMapper.insert(whiteList)
  → SQL: INSERT INTO white_list(name, version, language, file_path, isdelete)
         VALUES('numpy', '1.23.0', 'python', 'D:\kuling\upload\12345678\', 0)
  → 数据库响应：✓ 插入成功，id=48

  ─── 第3次循环 ───
  [类似过程]
  → id=49

  ─── 第4次循环 ───
  [类似过程]
  → id=50

步骤5：输出统计日志
  System.out.println("成功插入python依赖库数量: 4");
```

---

## 数据库最终状态

### Project 表

```sql
SELECT * FROM project WHERE name='my-python-app';

+----+------------------+-------+---------+--------------------------------+
| id | name             | desc  | language| file                           |
+----+------------------+-------+---------+--------------------------------+
| 30 | my-python-app    | ...   | python  | D:\kuling\upload\12345678\     |
+----+------------------+-------+---------+--------------------------------+
```

### White_list 表

```sql
SELECT * FROM white_list
WHERE file_path='D:\kuling\upload\12345678\'
ORDER BY id DESC;

+----+----------+----------+----------+-----------------------------+--------+
| id | name     | version  | language | file_path                   | isdelete|
+----+----------+----------+----------+-----------------------------+--------+
| 50 | flask    | 2.1.0    | python   | D:\kuling\upload\12345678\  | 0      |
| 49 | pandas   | 1.4.0    | python   | D:\kuling\upload\12345678\  | 0      |
| 48 | numpy    | 1.23.0   | python   | D:\kuling\upload\12345678\  | 0      |
| 47 | requests | 2.28.0   | python   | D:\kuling\upload\12345678\  | 0      |
+----+----------+----------+----------+-----------------------------+--------+
```

### 白名单语言统计

```sql
SELECT language, COUNT(*) as count
FROM white_list
WHERE isdelete=0
GROUP BY language;

+----------+-------+
| language | count |
+----------+-------+
| java     | 46    |  ← 原有的Java项目组件
| python   | 4     |  ← 新上传的Python项目组件
+----------+-------+
```

---

## 验证数据库更新是否成功的SQL查询

### 查询1：验证项目语言被正确保存

```sql
SELECT id, name, language, create_time
FROM project
WHERE name='my-python-app';

期望结果：
id=30, name='my-python-app', language='python'
```

### 查询2：验证项目对应的组件被保存

```sql
SELECT COUNT(*) as component_count
FROM white_list
WHERE file_path='D:\kuling\upload\12345678\'
AND isdelete=0;

期望结果：4（或更多，取决于项目中声明的依赖）
```

### 查询3：验证新语言出现在白名单中

```sql
SELECT DISTINCT language
FROM white_list
WHERE isdelete=0
ORDER BY language;

修复前结果：java
修复后结果：java, python（或java, python, rust, go等）
```

### 查询4：验证Python组件的具体内容

```sql
SELECT name, version, language
FROM white_list
WHERE language='python'
AND isdelete=0
ORDER BY name;

期望结果：
+----------+----------+----------+
| name     | version  | language |
+----------+----------+----------+
| flask    | 2.1.0    | python   |
| numpy    | 1.23.0   | python   |
| pandas   | 1.4.0    | python   |
| requests | 2.28.0   | python   |
+----------+----------+----------+
```

### 查询5：验证项目与组件的对应关系

```sql
SELECT
    p.id,
    p.name as project_name,
    p.language as project_lang,
    COUNT(w.id) as component_count,
    GROUP_CONCAT(DISTINCT w.name) as components
FROM project p
LEFT JOIN white_list w ON p.file = w.file_path AND w.isdelete=0
WHERE p.name='my-python-app'
GROUP BY p.id;

期望结果：
id=30, project_name='my-python-app', project_lang='python'
component_count=4
components='requests,numpy,pandas,flask'
```

---

## 关键验证点

### ✅ 验证点1：语言检测正确性
```sql
-- 检查项目是否被标记为正确的语言
SELECT name, language FROM project WHERE isdelete=0;

验证：python项目应该显示 language='python'，不应该是'java'
```

### ✅ 验证点2：白名单组件保存
```sql
-- 检查白名单中是否有新语言的组件
SELECT COUNT(*) FROM white_list WHERE language='python' AND isdelete=0;

验证：应该 > 0（表示有Python组件被保存）
```

### ✅ 验证点3：语言标签一致性
```sql
-- 检查项目和白名单的语言是否匹配
SELECT DISTINCT p.language
FROM project p
LEFT JOIN white_list w ON p.file = w.file_path
WHERE p.isdelete=0 AND p.language != w.language;

验证：应该返回空结果（表示语言标签一致）
```

### ✅ 验证点4：多语言支持
```sql
-- 检查白名单中有多少种语言
SELECT COUNT(DISTINCT language) as language_count FROM white_list WHERE isdelete=0;

修复前：1（只有java）
修复后：2+（java + python + ...其他语言）
```

---

## 实时监控数据库更新

### 监控脚本（持续观察）

```bash
#!/bin/bash

echo "监控前 - 白名单语言分布："
mysql -h localhost -u root -p15256785749rly kulin << SQL
SELECT language, COUNT(*) as count FROM white_list GROUP BY language;
SQL

echo ""
echo "上传Python项目..."
curl -X POST http://localhost:8081/project/uploadProject \
  -F "file=@test_python.zip" \
  -F "name=test-python" \
  -F "description=Test" \
  -F "companyId=1"

echo ""
echo "等待异步处理（5秒）..."
sleep 5

echo ""
echo "监控后 - 白名单语言分布："
mysql -h localhost -u root -p15256785749rly kulin << SQL
SELECT language, COUNT(*) as count FROM white_list GROUP BY language;
SQL

echo ""
echo "查看新增的Python组件："
mysql -h localhost -u root -p15256785749rly kulin << SQL
SELECT name, version, language FROM white_list WHERE language='python' LIMIT 10;
SQL
```

---

## 后端日志输出示例

当上传Python项目时，你会看到以下后端输出（通常在2-10秒内异步输出）：

```
=== uploadProject 接口被调用 ===
文件名: test_python.zip
项目名: test-python
companyId: 1

步骤1: 开始上传并检测语言...
文件解压完成，路径: D:\kuling\upload\abc123def456\
✓ 检测到项目语言: python
准备触发异步解析，语言类型: python
✓ 启动Python项目解析任务
步骤2: 文件上传成功
  - 文件路径: D:\kuling\upload\abc123def456\
  - 检测语言: python
步骤3: 开始创建项目，使用检测到的语言: python
步骤4: 项目创建成功

[5秒后 - 异步线程输出]
开始解析python项目: D:\kuling\upload\abc123def456\
调用python解析API: http://localhost:5000/parse/python_parse?project_folder=D%3A%5C...
python解析响应长度: 521
解析出依赖库数量: 4                    ← Flask返回了4个组件
成功插入python依赖库数量: 4             ← 全部成功保存到数据库！
```

---

## 最关键的代码行

下面是整个流程中最关键的3行代码：

### 行1：检测项目语言（ProjectServiceImpl.java:223）
```java
String detectedLanguage = projectUtil.detectProjectType(filePath);
```
→ 返回 "python"

### 行2：路由到Python解析（ProjectServiceImpl.java:249）
```java
asyncParsePythonProject(filePath);
```
→ 触发调用Flask parser

### 行3：保存到数据库（ProjectServiceImpl.java:855）
```java
whiteList.setLanguage(language);  // language = "python"
```
→ 确保white_list表中的language字段被正确设置为"python"

### 行4：插入数据库（ProjectServiceImpl.java:857）
```java
whiteListMapper.insert(whiteList);
```
→ 执行SQL: INSERT INTO white_list(name, version, language, file_path, isdelete) VALUES(..., 'python', ...)

---

## 总结

白名单表的实时更新过程：

1. **前端上传** → Python项目文件
2. **后端检测** → detectProjectType() 返回 "python"
3. **异步调用** → asyncParsePythonProject(filePath)
4. **Flask解析** → 返回JSON组件列表
5. **数据库保存** → 逐条插入white_list表，language='python'
6. **数据验证** → SQL查询可以看到新增的Python组件

**关键证明**：通过SQL查询 `SELECT * FROM white_list WHERE language='python'`，你会看到实时插入的组件数据。

---

**文档版本**：2.0
**最后更新**：2025-11-13
**重点**：理解 `whiteList.setLanguage(language)` 和 `whiteListMapper.insert(whiteList)` 这两行是如何实现数据库实时更新的
