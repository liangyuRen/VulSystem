# IDE 编译错误解决指南

## 问题描述

IDE (如 IntelliJ IDEA) 显示编译错误：
```
java: 程序包com.nju.backend.repository.mapper不存在
java: 程序包com.nju.backend.repository.po不存在
java: 找不到符号
  符号: 类 VulnerabilityReportMapper
```

但实际上这些文件和类都存在。

## 根本原因

这是 IDE 缓存或 Maven 索引过期导致的问题，而不是真正的代码错误。

## 快速解决方案

### 方案 1: IntelliJ IDEA (推荐)

**Step 1: 清除 IDE 缓存**
```
File → Invalidate Caches → Invalidate and Restart
```

**Step 2: 重新加载 Maven 项目**
```
View → Tool Windows → Maven
右键 backend 项目 → Reimport
```

或者用快捷键:
```
Ctrl + Shift + A (Windows)
搜索: "Reload All Maven Projects"
```

### 方案 2: 清除 Maven 缓存

```bash
# 删除本地 Maven 缓存
rm -rf ~/.m2/repository

# 重新下载依赖
cd backend
mvn clean install -DskipTests
```

### 方案 3: 清除项目构建缓存

```bash
cd backend
# 删除构建输出目录
rm -rf target/
# 重新编译
mvn clean compile
```

### 方案 4: 手动刷新 IDE

**IntelliJ IDEA**:
```
1. 右键项目 → Open Module Settings
2. Project → Project Compiler Output → 点击刷新按钮
3. File → Sync with File System (Ctrl+Alt+Y)
4. 重新构建项目 (Build → Rebuild Project)
```

**Eclipse**:
```
1. 右键项目 → Clean
2. Project → Clean → Select All → OK
3. Project → Build Automatically (勾选)
```

**VS Code (with Java Extension Pack)**:
```
1. Ctrl+Shift+P
2. 搜索: "Java: Clean Language Server Workspace"
3. 重新打开项目
```

## 验证是否解决

### 检查 1: IDE 中没有红色波浪线错误

打开 VulnerabilityJobHandler.java，检查:
- ✅ 第 33 行的 `VulnerabilityReportMapper` 没有红色波浪线
- ✅ 第 36 行的 `CompanyMapper` 没有红色波浪线
- ✅ 所有 import 语句都有✓

### 检查 2: 能够构建项目

```bash
cd backend
mvn clean install -DskipTests
```

预期输出:
```
BUILD SUCCESS
Total time: XX.XXXs
```

### 检查 3: IDE 能够提示自动补完

在编辑器中输入:
```java
CompanyMapper c // 应该能够自动补完
VulnerabilityReport v // 应该能够自动补完
```

## 如果问题仍未解决

### 检查 1: 确认文件确实存在

```bash
# 检查 mapper 文件
ls backend/src/main/java/com/nju/backend/repository/mapper/

# 检查 po 文件
ls backend/src/main/java/com/nju/backend/repository/po/
```

预期输出包含所有这些文件:
```
CompanyMapper.java
ProjectMapper.java
ProjectVulnerabilityMapper.java
UserMapper.java
VulnerabilityMapper.java
VulnerabilityReportMapper.java
VulnerabilityReportVulnerabilityMapper.java

Company.java
Project.java
ProjectVulnerability.java
User.java
Vulnerability.java
VulnerabilityReport.java
VulnerabilityReportVulnerability.java
WhiteList.java
```

### 检查 2: 确认 pom.xml 正确配置

打开 `backend/pom.xml`，检查:
```xml
<sourceDirectory>src/main/java</sourceDirectory>
<testSourceDirectory>src/test/java</testSourceDirectory>
```

### 检查 3: 重新克隆或重新加载项目

如果上述方法都不行，可以尝试:

```bash
# 完全刷新项目
cd backend
mvn clean
rm -rf target/
rm -rf .idea/
rm *.iml

# 重新打开项目
# (用 IDE 重新打开 backend 文件夹)
```

## 问题排查流程

```
IDE 显示编译错误
  ↓
是否文件真的存在? → 不存在: 检查 git status
                  → 存在: 继续
  ↓
清除 IDE 缓存 (File → Invalidate Caches)
  ↓
重新加载 Maven 项目
  ↓
刷新 IDE (Ctrl+Alt+Y)
  ↓
尝试编译 (mvn clean compile)
  ↓
问题解决?
  → 是: 继续开发
  → 否: 尝试方案 2 (清除 Maven 缓存)
```

## 预防措施

为了避免此问题重复出现：

1. **定期清理缓存**:
   ```bash
   # 每周一次
   rm -rf ~/.m2/repository
   mvn clean
   ```

2. **配置 IDE 自动刷新**:
   - IntelliJ: Settings → Build, Execution, Deployment → Compiler → Recompile on every project open

3. **使用 Maven Wrapper**:
   ```bash
   # 项目中的 Maven 版本一致
   ./mvnw clean install
   ```

## FAQ

**Q: 为什么会出现这个问题？**

A: 通常在以下情况发生:
- 拉取新代码后，IDE 的索引未更新
- Maven 本地缓存过期
- IDE 的构建缓存损坏
- IDE 和 Maven 使用了不同的 Java 版本

**Q: 这会影响最终的构建和部署吗？**

A: 不会。这只是 IDE 的显示问题。在命令行编译 (`mvn clean install`) 会成功。

**Q: 清除缓存会删除我的代码吗？**

A: 不会。缓存是 IDE 和 Maven 生成的临时文件，清除它们只是重新生成这些文件。你的源代码不会被删除。

**Q: 我应该提交这些缓存文件吗？**

A: 不应该。`.idea/` 和 `target/` 目录应该在 `.gitignore` 中，不要提交到版本控制。

---

**快速参考**:
- 最快解决: `File → Invalidate Caches → Invalidate and Restart`
- 彻底解决: `rm -rf ~/.m2/repository && mvn clean install`
- IDE 刷新: `Ctrl+Alt+Y` (IntelliJ)

---

如果上述方案都不能解决问题，请检查:
1. Java 版本是否正确 (应该是 JDK 8+)
2. Maven 版本是否正确 (应该是 3.6+)
3. IDE 是否为最新版本
