# VulSystem Docker 部署指南

## 📋 目录
- [系统要求](#系统要求)
- [快速开始](#快速开始)
- [配置说明](#配置说明)
- [常用命令](#常用命令)
- [故障排查](#故障排查)
- [生产环境建议](#生产环境建议)

## 系统要求

- Docker 20.10+
- Docker Compose 1.29+
- 至少 2GB 可用内存
- 至少 5GB 可用磁盘空间

## 快速开始

### 1. 克隆项目（如果尚未克隆）
```bash
git clone <your-repository-url>
cd VulSystem
```

### 2. 配置环境变量
```bash
# 复制环境变量模板
cp .env.example .env

# 编辑 .env 文件，修改数据库密码等敏感信息
# Windows: notepad .env
# Linux/Mac: nano .env
```

**重要**: 修改 `.env` 文件中的 `DB_PASSWORD` 为一个强密码！

### 3. 启动服务
```bash
# 构建并启动所有服务（后台运行）
docker-compose up -d

# 查看启动日志
docker-compose logs -f
```

### 4. 验证部署
等待约 1-2 分钟后，访问：
- 后端 API: http://localhost:8081
- 健康检查: http://localhost:8081/actuator/health

### 5. 首次使用
数据库会自动初始化测试数据（通过 `test_data_corrected.sql`）。

## 配置说明

### 环境变量 (.env)

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `DB_NAME` | kulin | 数据库名称 |
| `DB_USERNAME` | root | 数据库用户名 |
| `DB_PASSWORD` | - | **必须修改** 数据库密码 |
| `DB_PORT_EXTERNAL` | 3306 | 外部访问 MySQL 端口 |
| `BACKEND_PORT` | 8081 | 后端服务端口 |

### 数据持久化

Docker 使用命名卷（named volumes）存储数据：

- `vulsystem_mysql_data`: MySQL 数据库文件
- `vulsystem_uploads`: 用户上传的文件

数据会持久保存，即使容器删除也不会丢失（除非使用 `docker-compose down -v`）。

### 目录映射

| 容器路径 | 主机路径 | 说明 |
|----------|----------|------|
| `/app/uploads` | Docker Volume | 文件上传目录 |
| `/app/opensca` | `./data` | OpenSCA 工具目录（只读） |
| `/app/logs` | `./logs` | 应用日志 |
| `/var/lib/mysql` | Docker Volume | MySQL 数据 |

## 常用命令

### 启动和停止

```bash
# 启动服务
docker-compose up -d

# 停止服务（保留数据）
docker-compose down

# 停止服务并删除所有数据（谨慎使用！）
docker-compose down -v

# 重启服务
docker-compose restart

# 重启单个服务
docker-compose restart backend
docker-compose restart mysql
```

### 查看日志

```bash
# 查看所有服务日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f backend
docker-compose logs -f mysql

# 查看最近 100 行日志
docker-compose logs --tail=100 backend
```

### 进入容器

```bash
# 进入后端容器
docker-compose exec backend sh

# 进入 MySQL 容器
docker-compose exec mysql bash

# 连接 MySQL 数据库
docker-compose exec mysql mysql -uroot -p
```

### 重新构建

```bash
# 重新构建镜像（代码更新后）
docker-compose build

# 强制重新构建（不使用缓存）
docker-compose build --no-cache

# 重新构建并启动
docker-compose up -d --build
```

### 查看状态

```bash
# 查看运行中的容器
docker-compose ps

# 查看资源使用情况
docker stats
```

## 故障排查

### 问题 1: 后端无法连接数据库

**症状**: 后端日志显示数据库连接错误

**解决方法**:
```bash
# 检查 MySQL 是否健康
docker-compose ps

# 查看 MySQL 日志
docker-compose logs mysql

# 确保 MySQL 完全启动后再启动后端
docker-compose restart backend
```

### 问题 2: 端口被占用

**症状**: `Error: bind: address already in use`

**解决方法**:
```bash
# 修改 .env 文件中的端口
BACKEND_PORT=8082
DB_PORT_EXTERNAL=3307

# 重新启动
docker-compose up -d
```

### 问题 3: 文件上传失败

**症状**: 上传文件时返回错误

**解决方法**:
```bash
# 检查上传目录权限
docker-compose exec backend ls -la /app/uploads

# 如果需要，重新创建目录
docker-compose exec backend mkdir -p /app/uploads
```

### 问题 4: 数据丢失

**症状**: 重启后数据消失

**原因**: 可能使用了 `docker-compose down -v`

**解决方法**:
```bash
# 只停止容器，不删除卷
docker-compose down

# 查看现有卷
docker volume ls | grep vulsystem

# 备份重要数据
docker-compose exec mysql mysqldump -uroot -p kulin > backup.sql
```

### 问题 5: 构建失败

**症状**: Maven 构建错误

**解决方法**:
```bash
# 清理并重新构建
docker-compose down
docker-compose build --no-cache backend
docker-compose up -d
```

## 生产环境建议

### 1. 安全加固

```bash
# 使用强密码
DB_PASSWORD=$(openssl rand -base64 32)

# 限制数据库外部访问
# 在 docker-compose.yml 中注释掉 MySQL 的 ports 部分
```

### 2. 资源限制

在 `docker-compose.yml` 中为每个服务添加资源限制：

```yaml
services:
  backend:
    # ...
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 1G
        reservations:
          cpus: '0.5'
          memory: 512M
```

### 3. 日志管理

```yaml
services:
  backend:
    # ...
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
```

### 4. 备份策略

```bash
#!/bin/bash
# backup.sh - 自动备份脚本

BACKUP_DIR="./backups/$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP_DIR"

# 备份数据库
docker-compose exec -T mysql mysqldump -uroot -p$DB_PASSWORD kulin > "$BACKUP_DIR/database.sql"

# 备份上传文件
docker run --rm -v vulsystem_uploads:/data -v "$BACKUP_DIR":/backup alpine tar czf /backup/uploads.tar.gz -C /data .

echo "Backup completed: $BACKUP_DIR"
```

### 5. 健康监控

使用 Docker 健康检查和外部监控工具（如 Prometheus + Grafana）。

### 6. 使用反向代理

在生产环境中，建议使用 Nginx 或 Traefik 作为反向代理：

```yaml
# 添加到 docker-compose.yml
nginx:
  image: nginx:alpine
  ports:
    - "80:80"
    - "443:443"
  volumes:
    - ./nginx.conf:/etc/nginx/nginx.conf:ro
    - ./ssl:/etc/nginx/ssl:ro
  depends_on:
    - backend
```

### 7. 环境隔离

为不同环境创建不同的配置文件：
- `docker-compose.yml` - 开发环境
- `docker-compose.prod.yml` - 生产环境

```bash
# 使用生产配置启动
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

## 维护建议

### 定期更新

```bash
# 更新基础镜像
docker-compose pull

# 重新构建应用
docker-compose build

# 重启服务
docker-compose up -d
```

### 清理无用资源

```bash
# 清理未使用的镜像
docker image prune -a

# 清理未使用的卷（谨慎！）
docker volume prune

# 清理所有未使用资源
docker system prune -a
```

## 技术支持

如遇到问题，请：
1. 查看日志: `docker-compose logs -f`
2. 检查容器状态: `docker-compose ps`
3. 查阅官方文档
4. 提交 Issue

---

**更新时间**: 2025-10-10
**维护者**: VulSystem Team
