# VulSystem 后端接口完整测试计划

## 需要测试的主要接口

### 项目管理接口
1. POST /project/uploadProject - 项目上传（**关键，需重点测试**）
2. POST /project/create - 项目创建
3. POST /project/delete - 项目删除
4. POST /project/update - 项目更新
5. GET /project/list - 项目列表
6. GET /project/info - 项目信息
7. GET /project/statistics - 项目统计
8. GET /project/getVulnerabilities - 获取漏洞
9. GET /project/sbom - 获取SBOM文件

### 用户管理接口
1. POST /user/register - 用户注册
2. POST /user/login - 用户登录
3. POST /user/logout - 用户登出
4. GET /user/verify - 用户验证

### 公司管理接口
1. POST /company/create - 公司创建
2. GET /company/list - 公司列表
3. GET /company/info - 公司信息

## 测试优先级

**P0（关键 - 必须测试）**：
- POST /project/uploadProject - 新的语言检测接口
- GET /project/list - 验证项目列表显示正确的language
- GET /project/info - 验证项目详情中的language字段

**P1（重要）**：
- POST /project/create - 验证手动创建项目时language处理
- GET /project/statistics - 验证统计功能
- GET /project/getVulnerabilities - 验证漏洞获取

**P2（一般）**：
- POST /project/delete/update/sbom
- 用户认证相关接口
- 公司管理接口

## 测试环境检查

### 前置条件检查清单
- [ ] MySQL 服务运行中（端口 3306）
- [ ] Flask 服务运行中（端口 5000）
- [ ] Spring Boot 应用未启动（将使用新编译的版本）
- [ ] 测试项目文件已准备
  - [ ] Java 项目（pom.xml）
  - [ ] C/C++ 项目（makefile 或 .c/.cpp）
  - [ ] 其他语言项目（Rust, Python 等）

