# OpenChat4U 快速部署指南

## 🚀 一键部署 (推荐)

### 1. 克隆项目
```bash
git clone <repository-url>
cd OpenChat4U
```

### 2. 配置环境变量
```bash
# 复制环境变量模板
cp .env.example .env

# 编辑 .env 文件，填入你的配置
# ⚠️ 必须修改 JWT_SECRET 和 API Keys
vim .env
```

### 3. 生成安全密钥
```bash
# 生成 JWT 密钥
export JWT_SECRET=$(openssl rand -base64 32)

# 生成加密密钥
export ENCRYPTION_KEY=$(openssl rand -base64 32)

# 更新到 .env 文件
echo "JWT_SECRET=$JWT_SECRET" >> .env
echo "ENCRYPTION_KEY=$ENCRYPTION_KEY" >> .env
```

### 4. 启动所有服务
```bash
# 一键启动 (PostgreSQL + Qdrant + Backend + Frontend)
docker-compose up -d

# 查看日志
docker-compose logs -f

# 查看服务状态
docker-compose ps
```

### 5. 访问系统
- **前端**: http://localhost
- **后端 API**: http://localhost:8080
- **Qdrant Dashboard**: http://localhost:6333/dashboard
- **PostgreSQL**: localhost:5432

---

## 🔧 分步部署

### 只启动基础设施
```bash
# 只启动 PostgreSQL 和 Qdrant
docker-compose up -d postgres qdrant
```

### 只启动后端
```bash
# 使用 Docker
docker-compose up -d backend

# 或者本地运行
cd backend
mvn spring-boot:run
```

### 只启动前端
```bash
# 使用 Docker
docker-compose up -d frontend

# 或者本地运行
cd frontend
npm install
npm run dev
```

---

## 📦 构建镜像

### 手动构建镜像
```bash
# 构建后端镜像
docker build -t openchat4u-backend:latest ./backend

# 构建前端镜像
docker build -t openchat4u-frontend:latest ./frontend
```

### 重新构建并重启
```bash
# 后端
docker-compose build backend
docker-compose up -d backend

# 前端
docker-compose build frontend
docker-compose up -d frontend
```

---

## 🔍 运维命令

### 查看日志
```bash
# 查看所有服务日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f backend
docker-compose logs -f frontend
```

### 进入容器
```bash
# 进入后端容器
docker-compose exec backend sh

# 进入前端容器
docker-compose exec frontend sh

# 进入数据库容器
docker-compose exec postgres psql -U postgres
```

### 重启服务
```bash
# 重启所有服务
docker-compose restart

# 重启特定服务
docker-compose restart backend
docker-compose restart frontend
```

### 停止服务
```bash
# 停止所有服务
docker-compose down

# 停止并删除数据卷 (⚠️ 会删除所有数据！)
docker-compose down -v
```

---

## 🛠️ 故障排查

### 后端启动失败
```bash
# 查看详细日志
docker-compose logs backend

# 检查环境变量
docker-compose exec backend env | grep JWT

# 测试数据库连接
docker-compose exec backend wget --spider postgres:5432
```

### 前端无法访问
```bash
# 检查 nginx 配置
docker-compose exec frontend nginx -t

# 查看前端日志
docker-compose logs frontend
```

### 数据库连接问题
```bash
# 检查 PostgreSQL 状态
docker-compose ps postgres

# 测试数据库连接
docker-compose exec postgres pg_isready -U postgres
```

---

## 📊 生产环境部署

### 使用 Docker Swarm
```bash
# 初始化 swarm
docker swarm init

# 部署服务
docker stack deploy -c docker-compose.yml openchat4u
```

### 使用 Kubernetes
```bash
# 创建命名空间
kubectl create ns openchat4u

# 部署应用
kubectl apply -f k8s/ -n openchat4u
```

### 环境变量管理
```bash
# 使用 Docker secrets
echo "your-secret" | docker secret create jwt_secret -

# 在 docker-compose.yml 中引用
secrets:
  - jwt_secret
```

---

## 🎯 性能优化

### 调整 JVM 参数
```bash
# 在 .env 文件中设置
JAVA_OPTS="-Xms2g -Xmx4g -XX:+UseG1GC"
```

### 增加连接池大小
```bash
# 在创建租户时设置
maxConnections: 20
connectionTimeout: 30000
```

### Nginx 优化
已在 `frontend/nginx.conf` 中配置:
- Gzip 压缩
- 静态文件缓存
- 超时配置

---

## 📝 检查清单

部署前确认:
- [ ] 修改了 `JWT_SECRET`
- [ ] 配置了 `ENCRYPTION_KEY` (推荐)
- [ ] 填写了 `DEEPSEEK_API_KEY`
- [ ] 填写了 `EMBEDDING_API_KEY`
- [ ] 修改了数据库密码 (生产环境)
- [ ] 配置了防火墙规则
- [ ] 备份了数据卷

---

## 🆘 常见问题

**Q: 后端一直重启怎么办？**
```bash
# 查看日志
docker-compose logs backend

# 检查 JWT_SECRET 是否配置
docker-compose exec backend env | grep JWT

# 如果 JWT_SECRET 未配置，服务会拒绝启动
```

**Q: 前端访问后端 404？**
- 确认后端服务正常运行：`docker-compose ps backend`
- 检查 nginx 配置：`docker-compose exec frontend nginx -t`
- 查看网络配置：`docker network inspect openchat4u-network`

**Q: 如何备份数据？**
```bash
# 备份 PostgreSQL 数据
docker-compose exec postgres pg_dump -U postgres openchat4u > backup.sql

# 备份 Qdrant 数据
docker run --rm \
  -v openchat4u_qdrant_data:/qdrant_storage \
  -v $(pwd):/backup \
  alpine tar czf /backup/qdrant-backup.tar.gz /qdrant_storage
```

---

## 📞 技术支持

- **GitHub Issues**: 提交 Bug 和功能请求
- **文档**: 查看 DEPLOYMENT.md 获取详细使用指南
- **邮箱**: support@openchat4u.com
