# VulSystem Backend

Java Spring Boot 后端服务，提供 REST API 接口。

## 📋 项目结构

```
vulsystem-backend/
├── src/
│   ├── main/
│   │   ├── java/         # Java 源代码
│   │   │   └── com/example/vulsystem/
│   │   │       ├── controller/   # REST 控制器
│   │   │       ├── service/      # 业务逻辑层
│   │   │       ├── dao/          # 数据访问层
│   │   │       ├── entity/       # 数据实体
│   │   │       ├── config/       # 配置类
│   │   │       └── util/         # 工具类
│   │   └── resources/    # 资源文件
│   │       ├── application.yml    # Spring Boot 配置
│   │       ├── application-prod.yml
│   │       └── application-dev.yml
│   └── test/             # 单元测试
├── pom.xml               # Maven 配置
├── Dockerfile            # Docker 镜像定义
└── README.md
```

## 🚀 本地开发

### 前置要求

- JDK 11+
- Maven 3.6+
- MySQL 8.0+

### 安装依赖

```bash
cd /root/vulsystem-backend
mvn clean install
```

### 配置数据库

编辑 `src/main/resources/application-dev.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/kulin
    username: root
    password: your_password
  jpa:
    hibernate:
      ddl-auto: update
```

### 本地运行

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

应用将在 `http://localhost:8081` 启动。

## 🐳 Docker 运行

### 构建镜像

```bash
cd /root/vulsystem-orchestration
docker compose build vulsystem-backend
```

### 运行容器

```bash
docker compose up -d vulsystem-backend
```

### 查看日志

```bash
docker compose logs -f vulsystem-backend
```

## 📡 API 接口

### 用户管理

- `GET /api/user/info` - 获取用户信息
- `POST /api/user/login` - 用户登录
- `POST /api/user/logout` - 用户登出

### 项目管理

- `GET /api/project/list` - 获取项目列表
- `GET /api/project/{id}` - 获取项目详情
- `POST /api/project` - 创建项目
- `PUT /api/project/{id}` - 更新项目
- `DELETE /api/project/{id}` - 删除项目

### 漏洞报告

- `GET /vulnerabilityReport/list` - 获取漏洞列表
- `GET /vulnerabilityReport/{id}` - 获取漏洞详情

### 公司策略

- `GET /company/getStrategy` - 获取公司策略

## 🔧 配置说明

### application.yml 主要配置

```yaml
spring:
  application:
    name: vulsystem-backend
  datasource:
    url: jdbc:mysql://vulsystem-mysql:3306/kulin
    username: root
    password: ${MYSQL_ROOT_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  mvc:
    servlet:
      path: /api

server:
  port: 8081
  servlet:
    context-path: /
```

## 🗄️ 数据库

### 数据库名
```
kulin
```

### 主要数据表

- `user` - 用户表
- `project` - 项目表
- `vulnerability` - 漏洞表
- `scan_report` - 扫描报告表
- `company` - 公司表
- `company_strategy` - 公司策略表

### 初始化数据库

```bash
# 进入 MySQL 容器
docker compose exec vulsystem-mysql mysql -u root -p${MYSQL_ROOT_PASSWORD}

# 创建数据库
CREATE DATABASE IF NOT EXISTS kulin CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 使用数据库
USE kulin;

# 导入 SQL 脚本（如果有）
source /path/to/init.sql;
```

## 🧪 测试

### 运行单元测试

```bash
mvn test
```

### 运行集成测试

```bash
mvn verify
```

### 测试覆盖率

```bash
mvn jacoco:report
```

## 🔐 安全配置

### CORS 配置

编辑 `config/CorsConfig.java`:

```java
@Configuration
public class CorsConfig {
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost", "http://localhost:80"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
        // ...
    }
}
```

### JWT 认证

后端使用 JWT 进行身份验证。Token 需要在请求头中携带：

```http
Authorization: Bearer <token>
```

## 📊 性能监控

### 内置健康检查

```bash
curl http://localhost:8081/actuator/health
```

### 指标监控

```bash
curl http://localhost:8081/actuator/metrics
```

## 🐛 常见问题

### 1. 数据库连接失败

**错误信息**: `com.mysql.jdbc.exceptions.jdbc4.CommunicationsException: Communications link failure`

**解决方案**:
```bash
# 检查 MySQL 容器状态
docker compose ps | grep mysql

# 检查数据库连接
docker compose exec vulsystem-mysql mysql -u root -p${MYSQL_ROOT_PASSWORD} -e "SELECT 1;"
```

### 2. 端口被占用

**错误信息**: `Address already in use (:8081)`

**解决方案**:
```bash
# 查找占用 8081 端口的进程
lsof -i :8081

# 或者修改端口
# 编辑 docker-compose.yml，修改 ports 配置
```

### 3. 内存不足

**错误信息**: `java.lang.OutOfMemoryError`

**解决方案**:
```bash
# 修改 docker-compose.yml 中的内存限制
environment:
  - JAVA_OPTS=-Xms512m -Xmx2g
```

## 📈 项目依赖

主要依赖项：

- Spring Boot 2.x
- Spring Data JPA
- MySQL Connector/J
- Lombok
- Jackson
- Commons Lang3
- Log4j2

查看完整依赖：`pom.xml`

## 🔄 CI/CD 集成

### Maven Build

```bash
mvn clean package -DskipTests
```

### Docker Image Build

```bash
docker build -t vulsystem-backend:latest .
```

## 🚀 部署建议

### 生产环境配置

1. **使用生产数据库**: 修改 `application-prod.yml`
2. **启用 HTTPS**: 在 nginx/ingress 层处理
3. **配置日志级别**: `logging.level.root=WARN`
4. **调整 JVM 参数**: `-Xms1g -Xmx4g`
5. **启用缓存**: 配置 Redis 缓存

## 📝 代码规范

- 遵循 Google Java Style Guide
- 使用 Lombok 减少样板代码
- API 返回统一的 Response 对象
- 异常统一处理

## 🔗 相关链接

- [Spring Boot 官方文档](https://spring.io/projects/spring-boot)
- [MySQL 官方文档](https://dev.mysql.com/)
- [Maven 官方文档](https://maven.apache.org/)

---

**最后更新**: 2025-11-07
**技术栈**: Java 11+, Spring Boot 2.x, MySQL 8.0
