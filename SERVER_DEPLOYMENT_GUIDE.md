# 服务器部署指南

## 📋 目录
- [前置准备](#前置准备)
- [方法一：使用 Git 部署（推荐）](#方法一使用-git-部署推荐)
- [方法二：使用 SCP/SFTP 上传](#方法二使用-scpsftp-上传)
- [方法三：使用 Docker Hub](#方法三使用-docker-hub)
- [服务器环境配置](#服务器环境配置)
- [部署步骤](#部署步骤)
- [生产环境优化](#生产环境优化)

---

## 前置准备

### 1. 服务器要求
- **操作系统**: Linux (推荐 Ubuntu 20.04+ / CentOS 7+)
- **内存**: 至少 2GB RAM
- **磁盘空间**: 至少 10GB 可用空间
- **网络**: 开放端口 8081 (或你自定义的端口)

### 2. 本地准备
- 服务器 IP 地址
- SSH 访问权限（用户名和密码或密钥）
- 确保已提交所有代码更改

### 3. 检查服务器连接
```bash
# Windows PowerShell 或 CMD
ssh username@your-server-ip

# 示例
ssh root@192.168.1.100
```

---

## 方法一：使用 Git 部署（推荐）

### 优点
- ✅ 最简单、最快捷
- ✅ 便于版本控制和更新
- ✅ 可以轻松回滚

### 步骤

#### 1. 将代码推送到 Git 仓库

```bash
# 在本地项目目录执行

# 如果还没有 Git 仓库，先初始化
cd C:\Users\任良玉\Desktop\kuling\VulSystem
git init

# 添加远程仓库（GitHub/GitLab/Gitee）
git remote add origin https://github.com/yourusername/VulSystem.git

# 或使用 Gitee（国内速度更快）
git remote add origin https://gitee.com/yourusername/VulSystem.git

# 提交所有文件
git add .
git commit -m "Initial Docker deployment setup"

# 推送到远程仓库
git push -u origin master
```

**⚠️ 重要：不要提交 .env 文件**（已在 .gitignore 中配置）

#### 2. 在服务器上克隆项目

```bash
# SSH 登录服务器
ssh username@your-server-ip

# 克隆项目
cd /opt  # 或其他你喜欢的目录
git clone https://github.com/yourusername/VulSystem.git
cd VulSystem

# 如果仓库是私有的，需要先配置 Git 凭证
git config --global credential.helper store
```

#### 3. 后续更新非常简单

```bash
# 在服务器上
cd /opt/VulSystem
git pull origin master
docker-compose down
docker-compose up -d --build
```

---

## 方法二：使用 SCP/SFTP 上传

### 优点
- ✅ 不需要 Git 仓库
- ✅ 直接传输文件

### 缺点
- ❌ 更新麻烦
- ❌ 需要传输大量文件

### 使用 SCP（命令行）

```bash
# Windows PowerShell
# 先打包项目（排除不必要的文件）
cd C:\Users\任良玉\Desktop\kuling

# 创建压缩包
tar -czf VulSystem.tar.gz VulSystem/ --exclude=VulSystem/backend/target --exclude=VulSystem/.git --exclude=VulSystem/logs

# 上传到服务器
scp VulSystem.tar.gz username@your-server-ip:/opt/

# 登录服务器解压
ssh username@your-server-ip
cd /opt
tar -xzf VulSystem.tar.gz
cd VulSystem
```

### 使用 WinSCP（图形界面）- Windows 推荐

1. **下载安装 WinSCP**: https://winscp.net/
2. 打开 WinSCP，输入服务器信息：
   - 主机名: `your-server-ip`
   - 用户名: `root` 或其他用户
   - 密码: 你的密码
3. 连接后，将项目文件夹直接拖拽到服务器目录（如 `/opt/`）

### 使用 FileZilla（跨平台）

1. **下载安装 FileZilla**: https://filezilla-project.org/
2. 使用 SFTP 协议连接服务器
3. 拖拽上传项目文件

---

## 方法三：使用 Docker Hub

### 优点
- ✅ 镜像预构建，部署更快
- ✅ 适合多服务器部署

### 步骤

#### 1. 本地构建并推送镜像

```bash
# 登录 Docker Hub
docker login

# 构建镜像
cd C:\Users\任良玉\Desktop\kuling\VulSystem
docker build -t yourusername/vulsystem-backend:latest ./backend

# 推送到 Docker Hub
docker push yourusername/vulsystem-backend:latest
```

#### 2. 服务器上拉取运行

修改服务器上的 `docker-compose.yml`：

```yaml
services:
  backend:
    image: yourusername/vulsystem-backend:latest  # 使用远程镜像
    # 删除 build 部分
```

```bash
# 服务器上执行
docker-compose pull
docker-compose up -d
```

---

## 服务器环境配置

### 1. 安装 Docker

#### Ubuntu/Debian
```bash
# 更新包索引
sudo apt update

# 安装依赖
sudo apt install -y apt-transport-https ca-certificates curl software-properties-common

# 添加 Docker 官方 GPG 密钥
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg

# 添加 Docker 仓库
echo "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# 安装 Docker
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io

# 启动 Docker
sudo systemctl start docker
sudo systemctl enable docker

# 验证安装
sudo docker --version
```

#### CentOS/RHEL
```bash
# 安装依赖
sudo yum install -y yum-utils

# 添加 Docker 仓库
sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo

# 安装 Docker
sudo yum install -y docker-ce docker-ce-cli containerd.io

# 启动 Docker
sudo systemctl start docker
sudo systemctl enable docker

# 验证安装
sudo docker --version
```

### 2. 安装 Docker Compose

```bash
# 下载 Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/download/v2.20.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose

# 添加执行权限
sudo chmod +x /usr/local/bin/docker-compose

# 验证安装
docker-compose --version
```

### 3. 配置防火墙

#### Ubuntu (UFW)
```bash
# 开放应用端口
sudo ufw allow 8081/tcp
sudo ufw allow 22/tcp  # SSH

# 如果需要外部访问 MySQL
sudo ufw allow 3306/tcp

# 启用防火墙
sudo ufw enable
sudo ufw status
```

#### CentOS (firewalld)
```bash
# 开放端口
sudo firewall-cmd --permanent --add-port=8081/tcp
sudo firewall-cmd --permanent --add-port=22/tcp

# 重载防火墙
sudo firewall-cmd --reload
sudo firewall-cmd --list-ports
```

---

## 部署步骤

### 1. 上传项目（选择上述方法之一）

### 2. 配置环境变量

```bash
# 进入项目目录
cd /opt/VulSystem

# 复制环境变量模板
cp .env.example .env

# 编辑环境变量
nano .env  # 或使用 vim .env
```

修改 `.env` 文件：
```env
DB_NAME=kulin
DB_USERNAME=root
DB_PASSWORD=your_super_secure_password_here  # ⚠️ 必须修改
DB_PORT_EXTERNAL=3306
BACKEND_PORT=8081
```

**安全建议**：生成强密码
```bash
openssl rand -base64 32
```

### 3. 准备数据目录

```bash
# 创建日志目录
mkdir -p /opt/VulSystem/logs

# 确保 data 目录存在（OpenSCA 工具）
ls -la /opt/VulSystem/data
```

### 4. 启动服务

```bash
# 构建并启动
sudo docker-compose up -d

# 查看启动日志
sudo docker-compose logs -f

# 等待服务完全启动（约 1-2 分钟）
# 按 Ctrl+C 退出日志查看
```

### 5. 验证部署

```bash
# 检查容器状态
sudo docker-compose ps

# 测试健康检查
curl http://localhost:8081/actuator/health

# 查看后端日志
sudo docker-compose logs backend

# 查看数据库日志
sudo docker-compose logs mysql
```

### 6. 从外部访问

在浏览器中访问：
```
http://your-server-ip:8081
```

如果无法访问，检查：
- 防火墙是否开放端口
- 云服务器安全组是否开放端口（阿里云/腾讯云/AWS）
- 服务是否正常启动

---

## 生产环境优化

### 1. 配置反向代理（Nginx）

#### 安装 Nginx
```bash
sudo apt install nginx -y  # Ubuntu
sudo yum install nginx -y  # CentOS
```

#### 配置 Nginx
```bash
sudo nano /etc/nginx/sites-available/vulsystem
```

添加以下内容：
```nginx
server {
    listen 80;
    server_name your-domain.com;  # 或服务器 IP

    # 日志
    access_log /var/log/nginx/vulsystem_access.log;
    error_log /var/log/nginx/vulsystem_error.log;

    # 代理到后端
    location / {
        proxy_pass http://localhost:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # WebSocket 支持
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";

        # 超时设置
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # 文件上传大小限制
    client_max_body_size 100M;
}
```

启用配置：
```bash
# 创建软链接
sudo ln -s /etc/nginx/sites-available/vulsystem /etc/nginx/sites-enabled/

# 测试配置
sudo nginx -t

# 重启 Nginx
sudo systemctl restart nginx
sudo systemctl enable nginx
```

现在可以通过 `http://your-server-ip` (80 端口) 访问应用。

### 2. 配置 HTTPS (可选但推荐)

使用 Let's Encrypt 免费 SSL 证书：

```bash
# 安装 Certbot
sudo apt install certbot python3-certbot-nginx -y

# 获取证书并自动配置 Nginx
sudo certbot --nginx -d your-domain.com

# 自动续期
sudo certbot renew --dry-run
```

### 3. 设置自动备份

创建备份脚本：
```bash
sudo nano /opt/scripts/backup-vulsystem.sh
```

```bash
#!/bin/bash
BACKUP_DIR="/opt/backups/vulsystem"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_PATH="$BACKUP_DIR/$DATE"

mkdir -p "$BACKUP_PATH"

# 备份数据库
cd /opt/VulSystem
docker-compose exec -T mysql mysqldump -uroot -p${DB_PASSWORD} kulin > "$BACKUP_PATH/database.sql"

# 备份上传文件
docker run --rm -v vulsystem_uploads:/data -v "$BACKUP_PATH":/backup alpine tar czf /backup/uploads.tar.gz -C /data .

# 删除 7 天前的备份
find "$BACKUP_DIR" -type d -mtime +7 -exec rm -rf {} +

echo "Backup completed: $BACKUP_PATH"
```

添加定时任务：
```bash
sudo chmod +x /opt/scripts/backup-vulsystem.sh
sudo crontab -e

# 添加以下行（每天凌晨 2 点备份）
0 2 * * * /opt/scripts/backup-vulsystem.sh >> /var/log/vulsystem-backup.log 2>&1
```

### 4. 配置日志轮转

```bash
sudo nano /etc/logrotate.d/vulsystem
```

```
/opt/VulSystem/logs/*.log {
    daily
    rotate 7
    compress
    delaycompress
    notifempty
    missingok
    create 0644 root root
}
```

### 5. 设置系统服务（开机自启）

Docker Compose 已经配置了 `restart: unless-stopped`，容器会自动重启。

确保 Docker 开机自启：
```bash
sudo systemctl enable docker
```

### 6. 监控和告警（可选）

安装基础监控工具：
```bash
# 安装 htop (查看系统资源)
sudo apt install htop -y

# 安装 ctop (查看容器资源)
sudo wget https://github.com/bcicen/ctop/releases/download/v0.7.7/ctop-0.7.7-linux-amd64 -O /usr/local/bin/ctop
sudo chmod +x /usr/local/bin/ctop
```

---

## 常见问题

### 1. 端口已被占用
```bash
# 查看端口占用
sudo netstat -tulpn | grep 8081

# 停止占用端口的进程
sudo kill -9 <PID>
```

### 2. 内存不足
```bash
# 检查内存
free -h

# 清理 Docker 缓存
sudo docker system prune -a
```

### 3. 数据库连接失败
```bash
# 检查 MySQL 容器
sudo docker-compose logs mysql

# 重启 MySQL
sudo docker-compose restart mysql

# 等待 30 秒后重启后端
sudo docker-compose restart backend
```

### 4. 云服务器无法访问

- **阿里云/腾讯云**: 在控制台的"安全组"中开放 8081 端口
- **AWS**: 在 EC2 的 Security Groups 中添加入站规则
- **Azure**: 在网络安全组中添加入站规则

---

## 快速参考命令

```bash
# 查看服务状态
sudo docker-compose ps

# 查看日志
sudo docker-compose logs -f backend

# 重启服务
sudo docker-compose restart

# 停止服务
sudo docker-compose down

# 更新代码（Git 方式）
git pull origin master
sudo docker-compose up -d --build

# 备份数据库
docker-compose exec mysql mysqldump -uroot -p kulin > backup.sql

# 恢复数据库
docker-compose exec -T mysql mysql -uroot -p kulin < backup.sql

# 查看容器资源使用
sudo docker stats
```

---

## 总结推荐方案

**最佳实践组合**：
1. 使用 **Git** 部署代码（方便更新）
2. 配置 **Nginx** 反向代理（生产环境标配）
3. 启用 **HTTPS**（安全性）
4. 设置 **自动备份**（数据安全）
5. 配置 **防火墙**（网络安全）

---

**需要帮助？** 根据你的服务器类型（阿里云/腾讯云/AWS等）和需求，我可以提供更详细的指导！
