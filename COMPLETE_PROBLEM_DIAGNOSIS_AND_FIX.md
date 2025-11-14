# 完整问题诊断和修复方案

**主要问题**: 漏洞数据显示为0，涉及多个层面的问题

---

## 问题1: 漏洞数据显示为0

### 根本原因分析

#### 1.1 数据库连接问题

**症状**: API 返回 500 错误
```
Unsupported character encoding 'utf8mb4'
```

**原因**: MySQL JDBC 驱动与数据库字符编码不匹配

**解决方案**:

修改 `application.properties`:
```properties
# 原来:
spring.datasource.url=...&characterEncoding=utf8mb4&connectionCollation=utf8mb4_unicode_ci&...

# 改为:
spring.datasource.url=...&characterEncoding=utf8&...
```

#### 1.2 编译问题

**症状**: `NoClassDefFoundError: ProjectServiceImpl$2`

**原因**: 空匿名内部类 `TypeReference<Map<String, String>>() { }` 导致编译失败

**解决方案**:
✅ 已完成 - 替换为 `Map.class`

#### 1.3 漏洞数据为0的根本原因

即使 API 成功，漏洞数据仍然为0，可能是:

**原因A: 没有漏洞报告数据**
```sql
SELECT COUNT(*) FROM vulnerability_report;
```
如果为 0，需要先爬取漏洞数据

**原因B: 没有项目和依赖数据**
```sql
SELECT COUNT(*) FROM white_list;
SELECT COUNT(*) FROM project;
```
如果为 0，需要先上传项目并进行依赖扫描

**原因C: 公司和项目的关联不正确**
```sql
SELECT * FROM company WHERE id = 1;
-- 检查 project_id 字段是否为有效的 JSON
```

**原因D: 漏洞检测没有运行**
```sql
SELECT COUNT(*) FROM vulnerability;
SELECT COUNT(*) FROM project_vulnerability;
```
如果为 0，需要手动触发漏洞检测

---

## 问题2: 前端和后端连接问题

### 2.1 检查前端 API 调用

**前端应该调用的 API 端点**:

```javascript
// 获取项目列表
GET /project/list?companyId=1&page=1&size=10

// 获取项目统计信息
GET /project/statistics?companyId=1

// 获取项目漏洞
GET /project/getVulnerabilities?id=1
```

### 2.2 测试后端 API

```bash
# 测试1: 获取项目列表
curl "http://localhost:8081/project/list?companyId=1&page=1&size=10"

# 测试2: 获取项目统计信息
curl "http://localhost:8081/project/statistics?companyId=1"

# 测试3: 获取项目漏洞
curl "http://localhost:8081/project/getVulnerabilities?id=1"
```

### 2.3 前端网络请求调试

在浏览器开发者工具 (F12) 中检查:

1. **Network 选项卡**
   - [ ] 请求 URL 是否正确
   - [ ] 请求方法是否为 GET
   - [ ] 响应状态码是否为 200
   - [ ] 响应 Content-Type 是否为 application/json

2. **Console 选项卡**
   - [ ] 是否有 CORS 错误
   - [ ] 是否有 TypeError 或其他 JS 错误
   - [ ] 是否有 `common.getProjectListFailed` 错误

---

## 问题3: 编译问题

### 3.1 已修复的问题

✅ **TypeReference 匿名内部类问题**
- 位置: `ProjectServiceImpl.java` 第 326 行和 383 行
- 修复: 替换为 `Map.class`
- 状态: 已完成

### 3.2 重新编译步骤

```bash
# 1. 设置 JDK17 环境变量
export JAVA_HOME="C:/Program Files/Java/jdk-17.0.1"

# 2. 进入 backend 目录
cd backend

# 3. 清理并编译
mvn clean compile

# 4. 检查编译是否成功
echo $?  # 应该输出 0 表示成功
```

### 3.3 重启后端服务

```bash
# 方式1: 使用 IDE
# 在 IntelliJ IDEA 中重新运行应用

# 方式2: 使用 Maven
cd backend
mvn spring-boot:run

# 方式3: 使用 nohup （后台运行）
cd backend
nohup mvn spring-boot:run > backend.log 2>&1 &

# 方式4: 使用已编译的 JAR
java -jar backend/target/backend-0.0.1-SNAPSHOT.jar
```

---

## 完整修复清单

### Phase 1: 修复编译问题 ✅ 已完成

- [x] 修改 `getProjectList()` 方法
- [x] 修改 `getProjectStatistics()` 方法
- [x] 删除 `TypeReference` 导入
- [x] 重新编译代码

### Phase 2: 修复数据库连接问题

- [ ] 1. 修改 `application.properties` 中的字符编码:
  ```properties
  # 改这一行：
  spring.datasource.url=...&characterEncoding=utf8&...
  ```

- [ ] 2. 重新编译并重启后端:
  ```bash
  mvn clean compile
  mvn spring-boot:run
  ```

- [ ] 3. 验证数据库连接:
  ```bash
  curl "http://localhost:8081/project/list?companyId=1&page=1&size=10"
  ```

### Phase 3: 确保有足够的数据

- [ ] 1. 检查是否有漏洞报告数据:
  ```bash
  curl "http://localhost:8081/vulnerabilityReport/list"
  ```
  如果为空，需要爬取漏洞数据。

- [ ] 2. 检查是否有项目和依赖数据:
  ```bash
  curl "http://localhost:8081/project/list?companyId=1&page=1&size=10"
  ```
  如果为空，需要上传项目。

- [ ] 3. 运行漏洞检测:
  ```bash
  curl -X POST "http://localhost:8081/vulnerability/detect/all"
  ```

### Phase 4: 验证前端连接

- [ ] 1. 清空浏览器缓存 (Ctrl+Shift+Delete)
- [ ] 2. 刷新页面 (F5)
- [ ] 3. 打开开发者工具 (F12)
- [ ] 4. 查看 Network 选项卡中的 API 请求
- [ ] 5. 验证响应数据是否正确显示

### Phase 5: 测试完整流程

- [ ] 1. 登录系统
- [ ] 2. 导航到项目列表
- [ ] 3. 查看项目列表是否有数据
- [ ] 4. 查看项目统计信息
- [ ] 5. 点击项目查看漏洞
- [ ] 6. 验证漏洞数据是否不为 0

---

## 诊断脚本

### 快速诊断所有问题

```bash
#!/bin/bash

echo "=== 漏洞检测系统诊断开始 ==="

echo ""
echo "1. 检查数据库连接..."
curl -s "http://localhost:8081/project/list?companyId=1&page=1&size=10" | head -50

echo ""
echo "2. 检查漏洞报告数据..."
curl -s "http://localhost:8081/vulnerabilityReport/list" | head -20

echo ""
echo "3. 检查项目数据..."
curl -s "http://localhost:8081/project/list?companyId=1&page=1&size=10" | head -50

echo ""
echo "4. 检查项目统计..."
curl -s "http://localhost:8081/project/statistics?companyId=1" | head -50

echo ""
echo "=== 诊断完成 ==="
```

---

## 项目中的其他常见报错

### 报错1: CORS 跨域错误

**症状**: 前端无法调用后端 API，浏览器控制台出现:
```
Access to XMLHttpRequest from origin 'http://localhost:3000' has been blocked
```

**解决方案**:
检查 `CorsConfig.java` 配置:
```java
@Configuration
public class CorsConfig {
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(
            Arrays.asList("http://localhost", "http://localhost:80", "http://localhost:3000", "http://localhost:8080")
        );
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        // ...
    }
}
```

### 报错2: 401 Unauthorized

**症状**: API 返回 401 错误

**原因**: JWT Token 过期或不存在

**解决方案**:
1. 确保已登录
2. 检查 `Authorization` 请求头是否包含 Token
3. Token 格式应为: `Bearer <token>`

### 报错3: 404 Not Found

**症状**: API 端点返回 404

**原因**: API 路径错误

**解决方案**:
检查请求的 URL 是否与后端路由匹配:
- `/project/list` ✓ 正确
- `/projects/list` ✗ 错误 (加了 s)
- `/api/project/list` ✗ 错误 (多了 /api)

### 报错4: 500 Internal Server Error

**症状**: 后端返回 500 错误

**解决方案**:
1. 查看后端日志 (`nohup.out` 或 IDE 控制台)
2. 检查异常信息
3. 检查数据库连接
4. 检查数据库中是否有相应数据

---

## 一键修复脚本

### 对于 Windows (PowerShell)

```powershell
# 设置 JDK
$env:JAVA_HOME = "C:/Program Files/Java/jdk-17.0.1"

# 进入后端目录
cd backend

# 清理并编译
mvn clean compile

# 检查编译结果
if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ 编译成功" -ForegroundColor Green
    Write-Host "请手动重启后端服务"
} else {
    Write-Host "✗ 编译失败" -ForegroundColor Red
}
```

### 对于 Linux/Mac (Bash)

```bash
#!/bin/bash

# 设置 JDK
export JAVA_HOME="/opt/java/jdk-17.0.1"

# 进入后端目录
cd backend

# 清理并编译
mvn clean compile

# 检查编译结果
if [ $? -eq 0 ]; then
    echo "✓ 编译成功"
    echo "请手动重启后端服务"
else
    echo "✗ 编译失败"
fi
```

---

## 预期结果

### ✓ 修复完成后的表现

1. **前端**:
   - 项目列表正常显示
   - 漏洞数据不为0
   - 没有 `common.getProjectListFailed` 错误

2. **后端**:
   - `/project/list` API 返回 200
   - 数据库连接正常
   - 没有 `NoClassDefFoundError`

3. **数据库**:
   - 字符编码正确
   - 有足够的测试数据
   - 漏洞检测已运行

---

## 总结

| 问题 | 原因 | 解决方案 | 状态 |
|------|------|--------|------|
| 漏洞数据为0 | 数据库、连接、检测未运行 | 多步检查和修复 | 进行中 |
| 编译错误 | TypeReference 匿名类 | 替换为 Map.class | ✅ 完成 |
| 数据库连接失败 | UTF8MB4 不支持 | 改为 UTF8 | 待做 |
| 前端显示错误 | API 调用失败 | 验证 API 和网络 | 待做 |
| 其他报错 | 各种原因 | 参考诊断脚本 | 参考 |

---

**下一步**:
1. ✅ 完成编译修复
2. 修改数据库字符编码配置
3. 重启后端服务
4. 验证 API 连接
5. 确保有充足的测试数据
6. 运行漏洞检测
7. 验证前端显示

