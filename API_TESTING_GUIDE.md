# 后端API完整测试指南

## 一、系统概览

### 可用的测试数据

**用户** (9 个):
- ID 1: rly (role: boss) - Email: 3514737887@qq.com
- ID 2: testUser (role: role) - Email: test@test.com
- ID 3: rly (role: user) - Email: 211850116@smail.nju.edu.cn
- ID 9,11,14: test (各种邮箱)
- ID 15,16,17: testuser_* (自动生成的测试用户)

**项目** (1 个):
- ID 22: mall (language: java)

**公司** (1 个):
- ID 1: company

**漏洞报告** (1504 个)

---

## 二、API 端点总览

### 用户管理 (3 个端点)
1. **GET /user/login** - 用户登录
2. **POST /user/register** - 用户注册
3. **GET /user/info** - 获取用户信息

### 项目管理 (10 个端点)
1. **POST /project/uploadFile** - 上传ZIP文件
2. **POST /project/uploadProject** - 上传文件并创建项目
3. **POST /project/create** - 创建项目
4. **POST /project/delete** - 删除项目
5. **POST /project/update** - 更新项目
6. **GET /project/list** - 获取项目列表
7. **GET /project/info** - 获取项目信息
8. **GET /project/getVulnerabilities** - 获取项目的漏洞
9. **GET /project/statistics** - 获取项目统计
10. **GET /project/sbom** - 生成SBOM文件

### 公司管理 (2 个端点)
1. **POST /company/updateStrategy** - 更新公司策略
2. **GET /company/getStrategy** - 获取公司策略

### 漏洞报告 (3 个端点)
1. **GET /vulnerabilityReport/list** - 获取漏洞报告列表
2. **GET /vulnerabilityReport/search** - 搜索漏洞报告
3. **GET /vulnerabilityReport/filter** - 过滤漏洞报告

---

## 三、关键问题诊断

### 文件上传功能 (用户报告有问题)

**上传流程:**
1. 接收 MultipartFile
2. 验证ZIP格式 (通过魔术字节检查)
3. 保存到 D:\kuling\upload\{UUID}\
4. 创建项目记录
5. 异步触发依赖解析 (Java 项目调用 http://localhost:5000/parse/pom_parse)

**常见问题:**
1. **ZIP 格式验证失败** - 文件不是有效的 ZIP
2. **依赖解析服务未运行** - localhost:5000 无法连接
3. **上传目录权限问题** - D:\kuling\upload\ 目录不存在或无写权限
4. **APIFox 配置问题** - Content-Type 或多部分编码问题

---

## 四、详细的API测试用例

### A. 用户管理测试

#### 1. 登录测试
```
请求: GET /user/login?username=rly&password=rly
预期: 成功登录, code = 200, 返回用户信息
实际: (待测试)
```

#### 2. 注册新用户
```
请求: POST /user/register
参数:
  - username: newuser_{timestamp}
  - email: newuser_{timestamp}@test.com
  - password: TestPass123
  - phone: 13800000001

预期: code = 200, 消息 "注册成功，请使用用户名和密码登录"
实际: (待测试)
```

#### 3. 获取用户信息
```
请求: GET /user/info?username=rly
预期: code = 200, 返回用户详细信息
实际: (待测试)
```

---

### B. 项目管理测试

#### 1. 上传 ZIP 文件
```
请求: POST /project/uploadFile
  Content-Type: multipart/form-data
  文件: test.zip (有效的ZIP文件)

预期:
  - code = 200
  - 返回文件路径
  - 服务器创建 UUID 目录
  - 根据文件类型启动异步解析

实际: (待测试)

常见问题诊断:
  - 如果返回 "检测到7z格式"，说明文件不是ZIP
  - 如果返回 "文件格式不正确"，说明文件头异常
  - 如果返回超时，说明依赖解析服务未运行
```

#### 2. 上传文件并创建项目 (推荐)
```
请求: POST /project/uploadProject
  Content-Type: multipart/form-data

参数:
  - file: test.zip (必需)
  - name: myproject
  - description: Test Project
  - language: java (可选,默认java)
  - risk_threshold: 50 (可选)
  - companyId: 1 (必需)

预期:
  - code = 200
  - message = "项目上传成功，正在分析..."
  - status = "pending"

实际: (待测试)

注意: 支持 risk_threshold 和 riskThreshold 两种参数名
```

#### 3. 创建项目 (不上传文件)
```
请求: POST /project/create
参数:
  - name: manual-project
  - description: Manually created project
  - language: java
  - risk_threshold: 50
  - companyId: 1
  - filePath: D:\kuling\upload\{existing-uuid}

预期: code = 200
实际: (待测试)
```

#### 4. 获取项目列表
```
请求: GET /project/list?companyId=1&page=1&size=10
预期: code = 200, 返回项目列表和分页信息
实际: (待测试)
```

#### 5. 获取项目信息
```
请求: GET /project/info?id=22
预期: code = 200, 返回项目详细信息
实际: (待测试)
```

#### 6. 获取项目漏洞
```
请求: GET /project/getVulnerabilities?id=22
预期: code = 200, 返回与项目关联的漏洞列表
实际: (待测试)
```

#### 7. 获取项目统计
```
请求: GET /project/statistics?companyId=1
预期: code = 200, 返回统计信息 (项目数, 漏洞数等)
实际: (待测试)
```

#### 8. 更新项目
```
请求: POST /project/update
参数:
  - id: 22
  - name: mall-updated
  - description: Updated description
  - risk_threshold: 60
  - filePath: D:\kuling\upload\{uuid}

预期: code = 200
实际: (待测试)
```

#### 9. 删除项目
```
请求: POST /project/delete
参数: id=22

预期: code = 200
实际: (待测试)
```

#### 10. 获取SBOM
```
请求: GET /project/sbom?id=22&format=json&outFileName=sbom.json

格式选项: json, xml, spdx
预期: code = 200, 返回 SBOM 文件

注意: 这个端点可能失败，原因:
  - OpenSCA 工具未安装 (D:\kuling\opensca\)
  - 项目的依赖解析尚未完成
  - 数据库中没有该项目的依赖信息

实际: (待测试)
```

---

### C. 漏洞报告测试

#### 1. 获取漏洞报告列表
```
请求: GET /vulnerabilityReport/list?page=1&size=20
预期: code = 200, 返回1504个漏洞报告 (分页)
实际: (待测试)
```

#### 2. 搜索漏洞报告
```
请求: GET /vulnerabilityReport/search?keyword={关键词}&page=1&size=20
预期: code = 200, 返回匹配的漏洞报告
实际: (待测试)
```

#### 3. 过滤漏洞报告
```
请求: GET /vulnerabilityReport/filter?status={状态}&severity={严重性}&page=1&size=20
预期: code = 200, 返回过滤后的漏洞报告
实际: (待测试)
```

---

### D. 公司管理测试

#### 1. 更新策略
```
请求: POST /company/updateStrategy
参数:
  - companyId: 1
  - strategy: {JSON 策略对象}

预期: code = 200
实际: (待测试)
```

#### 2. 获取策略
```
请求: GET /company/getStrategy?companyId=1
预期: code = 200, 返回公司策略
实际: (待测试)
```

---

## 五、使用 APIFox 测试

### 第一步: 导入 API 集合

在 APIFox 中创建新的 API 集合，添加所有端点。

### 第二步: 配置环境变量

创建一个环境变量:
```
BASE_URL = http://localhost:8081
COMPANY_ID = 1
PROJECT_ID = 22
TEST_USER = rly
TEST_PASSWORD = rly
```

### 第三步: 使用前置脚本 (Pre-request Script)

对于需要登录的请求:
```javascript
// 如果还未获得 token，先进行登录
if (!pm.environment.get("token")) {
    // 调用登录接口
    // 保存返回的 token
}

// 添加认证信息
pm.request.headers.add({
    key: "Authorization",
    value: "Bearer " + pm.environment.get("token")
});
```

### 第四步: 文件上传特殊处理

在 APIFox 中测试 `/project/uploadFile` 或 `/project/uploadProject`:

1. 选择 **Body** 标签
2. 选择 **form-data**
3. 对于 "file" 字段:
   - 类型: **File**
   - 选择一个有效的 ZIP 文件
4. 对于其他参数 (如 name, description):
   - 类型: **Text**
   - 输入相应的值

**重要**: 确保:
- Content-Type 自动设置为 multipart/form-data
- 所有必需参数都已填写
- 文件是有效的 ZIP 格式

---

## 六、故障排除指南

### 问题 1: 文件上传返回 "检测到7z/RAR格式"

**原因**: 上传的不是 ZIP 格式

**解决方案**:
1. 检查文件是否真的是 ZIP 格式
2. 如果是 7z 或 RAR，需要重新压缩为 ZIP
3. 在 Windows 中: 右键 → 发送到 → 压缩文件夹

### 问题 2: 文件上传超时或无响应

**原因**: 可能是依赖解析服务 (localhost:5000) 未运行

**解决方案**:
1. 检查 Flask 服务是否运行: `curl http://localhost:5000/health`
2. 如果未运行，启动 Flask 服务
3. 查看后端日志中的错误信息

### 问题 3: SBOM 生成失败

**原因**: OpenSCA 工具未安装或配置不正确

**解决方案**:
1. 检查 `D:\kuling\opensca\` 目录是否存在
2. 验证 `opensca-cli-3.0.8-installer.exe` 或 `opensca-cli.exe` 是否存在
3. 如果不存在，需要下载并安装 OpenSCA 工具

### 问题 4: 在 APIFox 中看不到响应

**可能原因**:
1. 后端服务未启动
2. 端口错误 (应该是 8081)
3. CORS 配置问题
4. 防火墙阻止

**解决方案**:
1. 验证后端运行: `curl http://localhost:8081/user/login?username=rly&password=rly`
2. 检查 SecurityConfig.java 中的 CORS 配置
3. 查看后端日志

---

## 七、预期的成功响应格式

### 成功响应 (code = 200)
```json
{
  "code": 200,
  "message": "SUCCESS",
  "obj": {
    // 数据内容取决于端点
  }
}
```

### 失败响应 (code = 500)
```json
{
  "code": 500,
  "message": "服务端异常",
  "obj": "具体错误信息"
}
```

---

## 八、测试检查清单

- [ ] 用户登录测试
- [ ] 用户注册测试
- [ ] 用户信息获取
- [ ] 项目列表查询
- [ ] 项目信息查询
- [ ] 项目漏洞查询
- [ ] 文件上传 (小文件)
- [ ] 文件上传 (大文件, 接近100MB限制)
- [ ] 文件上传 + 项目创建
- [ ] 项目更新
- [ ] 项目删除
- [ ] SBOM 生成
- [ ] 漏洞报告列表
- [ ] 漏洞报告搜索
- [ ] 漏洞报告过滤
- [ ] 公司策略更新
- [ ] 公司策略获取

---

**文档更新**: 2025-11-13
**测试环境**: Windows + Spring Boot 2.6.13
**数据库**: MySQL (kulin)
**后端端口**: 8081

