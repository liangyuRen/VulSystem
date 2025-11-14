# VulSystem 后端接口测试报告 - 项目语言解析和组件解析

## 测试时间：2025-11-13
## 应用状态：运行中（Spring Boot 8081）

---

## 一、当前系统状态检查

### 1.1 数据库连接状态
```
✓ MySQL 连接成功（kulin 数据库）
✓ 项目表存在 (project)
✓ 白名单表存在 (white_list)
✓ 可正常读写数据
```

### 1.2 项目表数据检查
```sql
SELECT id, name, language FROM project WHERE isdelete = 0 ORDER BY id DESC LIMIT 5;

结果：
+----+--------+----------+
| id | name   | language |
+----+--------+----------+
| 26 | rust   | java     | ❌ 错误！应该是 rust 或 c
| 25 | php    | java     | ❌ 错误！应该是 unknown 或 php
| 24 | python | java     | ❌ 错误！应该是 python
| 23 | mall   | java     | ✓ 正确
| 20 | 22222  | java     | ? 需要检查项目内容
+----+--------+----------+

分析：项目名为 'rust' 的项目被标记为 'java' - 这正是我们要修复的问题！
```

### 1.3 白名单（组件）数据检查
```sql
SELECT language, COUNT(*) as cnt FROM white_list WHERE isdelete=0 GROUP BY language;

结果：
+----------+-----+
| language | cnt |
+----------+-----+
| java     | 46  |
+----------+-----+

分析：
✗ 只有 Java 的组件
✗ 没有 C、C++、Rust、Python 等其他语言的组件
✗ 这说明只有 Java 项目被正确解析，其他项目被忽略
```

---

## 二、API 接口测试

### 2.1 GET /project/list 接口
```
请求：
GET http://localhost:8081/project/list?companyId=1&page=1&size=5

响应状态：✓ 200 OK

响应内容示例：
{
  "code": 200,
  "message": "SUCCESS",
  "obj": [
    {
      "id": "20",
      "name": "22222",
      "description": "22222",
      "risk_level": "暂无风险",
      "risk_threshold": "11"
    },
    {
      "id": "23",
      "name": "mall",
      "description": "1111",
      "risk_level": "暂无风险",
      "risk_threshold": "10"
    },
    {
      "id": "24",
      "name": "python",
      "description": "1",
      "risk_level": "暂无风险",
      "risk_threshold": "10"
    },
    {
      "id": "25",
      "name": "php",
      "description": "1",
      "risk_level": "暂无风险",
      "risk_threshold": "10"
    },
    {
      "id": "26",
      "name": "rust",
      "description": "1",
      "risk_level": "暂无风险",
      "risk_threshold": "10"
    }
  ]
}

观察：
⚠ 列表中没有返回 language 字段（由 ProjectServiceImpl.getProjectList 方法返回，未包含 language）
```

### 2.2 GET /project/info 接口
```
请求：
GET http://localhost:8081/project/info?projectid=26

响应状态：✓ 200 OK

响应内容：
{
  "code": 200,
  "message": "SUCCESS",
  "obj": {
    "id": 26,
    "projectName": "rust",
    "createTime": "Thu Nov 13 18:23:26 CST 2025",
    "projectDescription": "1",
    "language": "java",           ❌ 错误！项目名为 'rust' 但 language='java'
    "riskThreshold": 10,
    "highRiskNum": 0,
    "lowRiskNum": 0,
    "midRiskNum": 0,
    "lastScanTime": "2025-11-13T06:00"
  }
}

观察：
✗ 项目ID 26 的名称是 'rust'，但 language 字段为 'java'
✗ 这是当前系统的关键问题
✓ API 接口可以正常返回 language 字段，但数据不正确
```

---

## 三、关键问题确认

### 问题确认1：Project 表的 language 字段全为 'java'
```
【问题表现】
- 项目名为 "rust" 时，language = "java"
- 项目名为 "python" 时，language = "java"
- 项目名为 "php" 时，language = "java"
- 只有项目名为 "mall" 时，language = "java"（但这个也可能是错的）

【数据证据】
查询显示共5个项目，全部 language='java'

【根本原因】
根据代码分析，这是因为：
1. uploadProject() 接口中 language 默认为 "java"
2. createProject() 直接使用这个值，不进行检测
3. detectProjectType() 方法存在但未被使用
```

### 问题确认2：白名单只有 Java 依赖
```
【问题表现】
- white_list 表中只有 language='java' 的 46 条记录
- 没有 c/c++、rust、python 等语言的组件

【根本原因】
1. 只有 Java 项目会触发 asyncParseJavaProject()
2. C/C++ 项目应该触发 asyncParseCProject()，但没有
3. 其他语言项目无法被解析

【验证】
```sql
SELECT COUNT(*) FROM white_list WHERE file_path IN
  (SELECT file FROM project WHERE id IN (24, 25, 26));
-- 结果：0（python、php、rust 项目都没有白名单数据）
```
```

---

## 四、修复验证准备

### 需要进行的测试
```
1. ✓ 已验证：现有系统存在上述问题
2. ⏳ 待进行：部署修复后的代码并重新测试
3. ⏳ 待进行：验证新上传的项目是否正确检测语言
4. ⏳ 待进行：验证白名单表是否有新语言的数据
```

---

## 五、关键代码位置和修复方案

### 修复已完成的文件
1. **ProjectService.java** - 已添加新方法声明
2. **ProjectServiceImpl.java** - 已实现 uploadFileWithLanguageDetection()
3. **ProjectController.java** - 已改造 uploadProject() 接口
4. **ProjectUtil.java** - 已扩展 detectProjectType() 方法

### 修复后期望的表现
```
修复前：
- 所有项目 → language = 'java'
- Java 项目 → white_list 有 46 条记录
- 非 Java 项目 → white_list 无记录

修复后：
- Java 项目 → language = 'java' → white_list 有 Java 依赖
- C/C++ 项目 → language = 'c' → white_list 有 C 依赖
- Rust 项目 → language = 'rust' → white_list 为空（无 parser）
- Unknown 项目 → language = 'unknown' → white_list 无记录
```

---

## 六、待测试的 API 调用步骤

### 步骤1：上传新的 Java 项目
```bash
curl -X POST http://localhost:8081/project/uploadProject \
  -F "file=@test-java.zip" \
  -F "name=new-java-test" \
  -F "description=Test Java detection" \
  -F "companyId=1"

期望响应：
{
  "detectedLanguage": "java",
  "message": "项目上传成功，检测到语言: java"
}

期望数据库结果：
SELECT language FROM project WHERE name='new-java-test';
→ java ✓

期望白名单：
SELECT COUNT(*) FROM white_list WHERE file_path LIKE '%new-java%' AND language='java';
→ > 0 ✓
```

### 步骤2：上传 C/C++ 项目
```bash
curl -X POST http://localhost:8081/project/uploadProject \
  -F "file=@test-cpp.zip" \
  -F "name=new-cpp-test" \
  -F "description=Test C++ detection" \
  -F "companyId=1"

期望响应：
{
  "detectedLanguage": "c",
  "message": "项目上传成功，检测到语言: c"
}

期望数据库结果：
SELECT language FROM project WHERE name='new-cpp-test';
→ c ✓

期望白名单：
SELECT COUNT(*) FROM white_list WHERE file_path LIKE '%new-cpp%' AND language='c';
→ > 0 ✓
```

### 步骤3：查询项目统计
```bash
curl -s "http://localhost:8081/project/statistics?companyId=1"

期望修复后的结果应该包含：
- 多种语言的项目数量
- 多种语言的组件数量
- 正确的风险统计
```

---

## 七、总结

### 当前状态
```
✓ 代码修复：已完成
✓ 编译状态：BUILD SUCCESS
✓ 应用启动：运行中
✓ API 接口：可访问

✗ 问题仍存在：修复代码未被应用（需要重启应用）
```

### 下一步行动
1. 使用修复后的代码重新启动 Spring Boot 应用
2. 上传新的测试项目并验证语言检测结果
3. 检查白名单表中是否有多种语言的组件数据
4. 对比修复前后的数据库状态
5. 验证项目语言字段和白名单语言字段的一致性

---

## 八、关键查询语句备忘录

### 查看项目语言分布
```sql
SELECT language, COUNT(*) FROM project WHERE isdelete=0 GROUP BY language;
```

### 查看白名单组件分布
```sql
SELECT language, COUNT(*) FROM white_list WHERE isdelete=0 GROUP BY language;
```

### 查看项目和白名单的对应关系
```sql
SELECT
    p.id, p.name, p.language,
    COUNT(w.id) as component_count,
    GROUP_CONCAT(DISTINCT w.language) as whitelist_languages
FROM project p
LEFT JOIN white_list w ON p.file = w.file_path
WHERE p.isdelete = 0 AND w.isdelete = 0
GROUP BY p.id, p.name, p.language;
```

### 查看最新上传的项目
```sql
SELECT id, name, language, file FROM project
WHERE isdelete = 0 ORDER BY create_time DESC LIMIT 5;
```

---

## 报告结论

**当前系统确实存在项目语言检测问题，所有项目都被标记为 Java。** 修复代码已完成并成功编译，使用修复后的代码重新启动应用后，新上传的项目应该能被正确检测语言。需要进行实际的上传测试来验证修复效果。

