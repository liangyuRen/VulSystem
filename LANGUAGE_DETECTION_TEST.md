# 语言检测和项目解析流程测试报告

## 当前流程分析

### 上传流程图
```
uploadProject API
    ↓
uploadFile()
    ↓
unzipAndSaveFile() → 得到项目路径
    ↓
calcLanguagePercentByFileSize() → 检测语言
    ↓
判断并调用异步解析 (asyncParseJavaProject / asyncParseCProject)
    ↓
将依赖写入 WhiteList 表
```

## 发现的问题

### ❌ 问题1：detectProjectType() 方法未被使用
**文件**：ProjectUtil.java:557-625
**现象**：定义了检测方法但从未被调用
**影响**：项目语言检测不够准确，只能通过文件大小比例推断

### ❌ 问题2：语言检测逻辑有缺陷
**文件**：ProjectServiceImpl.java:204-232 (uploadFile 方法)
```java
// 问题代码：
if (languagePercent.size() == 2) {
    // 当检测到2种语言时，随意取第一个
    projectType = entry.getKey();
} else {
    // 当只有1种或多于2种时，返回JSON字符串
    projectType = ProjectUtil.mapToJson(languagePercent);  // ❌ JSON字符串！
}
```
**原因**：条件判断逻辑不清楚
**后果**：projectType 可能是JSON字符串，导致后续的 `equals("java")` 判断全部失败

### ❌ 问题3：Project 表的 language 字段使用不当
**文件**：ProjectController.java:81-82
```java
String projectLanguage = (language != null && !language.isEmpty()) ? language : "java";
// ❌ 直接使用前端参数或默认为java，完全忽视了检测结果
```
**原因**：前端默认发送"java"，后端没有重写为检测结果
**后果**：所有项目的 language 字段都是 "java"，即使是C项目也被标记为java

### ❌ 问题4：uploadFile() 方法没有返回检测到的语言
**文件**：ProjectServiceImpl.java:204-232
**问题**：
- uploadFile() 只返回文件路径，没有返回检测到的语言
- 无法将检测结果传递给 createProject()

### ❌ 问题5：异步解析的触发没有错误处理和日志完整性
**文件**：ProjectServiceImpl.java:103-201
**问题**：
- 异步调用没有返回状态
- 缺少异常处理和重试机制
- Flask 端若失败，用户完全不知道

## 正确的流程应该是

```
uploadProject(file, name, description, companyId)
    ↓
    ├→ uploadFile(file)
    │   ├→ unzipAndSaveFile() → filePath
    │   ├→ 【关键】detectProjectType(filePath) → 真正检测语言
    │   └→ 返回 {filePath, detectedLanguage}
    │
    ├→ 【关键】createProject(..., detectedLanguage, ...)
    │   └→ 保存 detectedLanguage 到 Project.language
    │
    └→ 【关键】根据 detectedLanguage 触发异步解析
        ├→ if(java) → asyncParseJavaProject()
        ├→ if(c) → asyncParseCProject()
        └→ 返回已检测的语言给前端
```

## 待修复清单

- [ ] 使用 detectProjectType() 替换 calcLanguagePercentByFileSize()
- [ ] uploadFile() 返回 {filePath, language} 对象而不是单纯的路径
- [ ] 在 createProject 中使用检测到的语言，而非前端参数
- [ ] 在 uploadProject 接口中处理并返回检测结果
- [ ] 补充语言检测的异常处理和日志

