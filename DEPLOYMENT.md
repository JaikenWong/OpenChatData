# OpenChat4U 部署与使用文档

## 目录
- [系统要求](#系统要求)
- [快速部署](#快速部署)
- [配置说明](#配置说明)
- [使用指南](#使用指南)
- [API 文档](#api 文档)
- [常见问题](#常见问题)

---

## 系统要求

### 最低配置
- **CPU**: 2 核
- **内存**: 4GB
- **磁盘**: 10GB
- **JDK**: 17+
- **Node.js**: 18+

### 推荐配置
- **CPU**: 4 核+
- **内存**: 8GB+
- **磁盘**: 20GB+
- **数据库**: PostgreSQL 15+
- **向量库**: Qdrant 1.7+

---

## 快速部署

### 1. 克隆项目
```bash
git clone <repository-url>
cd OpenChat4U
```

### 2. 启动基础设施 (PostgreSQL + Qdrant)
```bash
cd docker
docker-compose up -d
```

验证服务启动:
```bash
docker-compose ps
# 应显示 postgres 和 qdrant 状态为 Up
```

### 3. 配置环境变量

创建后端环境变量文件 `backend/.env`:
```bash
# JWT 密钥 (必须修改！生成方式：openssl rand -base64 32)
JWT_SECRET=your-secure-random-key-at-least-32-characters

# 加密密钥 (用于加密数据库密码)
ENCRYPTION_KEY=$(openssl rand -base64 32)

# DeepSeek API 密钥
DEEPSEEK_API_KEY=sk-your-deepseek-api-key

# 讯飞 Embedding API 密钥
EMBEDDING_API_KEY=your-embedding-api-key
```

**重要安全提示**:
- ⚠️ **必须**修改 `JWT_SECRET`，使用随机生成的 32 位以上字符串
- ⚠️ **建议**配置 `ENCRYPTION_KEY` 加密存储数据库密码
- ⚠️ 不要将 `.env` 文件提交到版本控制

### 4. 启动后端服务

```bash
cd backend

# 方式 1: 使用 Maven
mvn spring-boot:run

# 方式 2: 打包后运行
mvn clean package -DskipTests
java -jar target/openchat4u-backend-1.0.0.jar
```

验证后端启动:
```bash
curl http://localhost:8080/api/health
# 应返回 200 OK
```

### 5. 启动前端服务

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 或打包生产版本
npm run build
```

访问前端: http://localhost:3000

---

## 配置说明

### application.yml 配置项

```yaml
# 服务器配置
server:
  port: 8080

# 数据库配置 (元数据存储)
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/openchat4u
    username: postgres
    password: postgres

# Qdrant 向量库配置
qdrant:
  host: localhost
  port: 6334

# LLM 配置
deepseek:
  api-key: ${DEEPSEEK_API_KEY}
  base-url: https://api.deepseek.com
  model: deepseek-chat

# Embedding 配置
embedding:
  api-key: ${EMBEDDING_API_KEY}
  base-url: https://maas-api.cn-huabei-1.xf-yun.com/v2/embeddings
  model: xop3qwen8bembedding

# JWT 安全配置
jwt:
  secret: ${JWT_SECRET}  # 必须通过环境变量设置
  expiration: 86400000   # 24 小时

# 加密配置
encryption:
  key: ${ENCRYPTION_KEY}  # 可选，用于加密数据库密码
```

### 生产环境部署建议

1. **使用环境变量**:
```bash
export JWT_SECRET=$(openssl rand -base64 32)
export ENCRYPTION_KEY=$(openssl rand -base64 32)
export DEEPSEEK_API_KEY=your-key
```

2. **使用 Docker 部署**:
```dockerfile
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/openchat4u-backend-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

3. **使用 Nginx 反向代理**:
```nginx
server {
    listen 80;
    server_name your-domain.com;

    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location / {
        proxy_pass http://localhost:3000;
    }
}
```

---

## 使用指南

### 1. 首次使用流程

#### Step 1: 登录系统
- 访问 http://localhost:3000
- 使用默认管理员账号登录 (需先在数据库创建)

#### Step 2: 创建租户
1. 进入 **租户管理** 页面
2. 点击 **新增租户**
3. 填写信息:
   - 名称：示例企业
   - 代码：example
   - 数据库类型：选择 PostgreSQL/MySQL/Oracle/SQL Server
   - JDBC URL: `jdbc:postgresql://host:port/db`
   - 用户名/密码：数据库连接凭证
4. 点击 **确定**

#### Step 3: 同步表结构
1. 进入 **表结构** 页面
2. 选择刚创建的租户
3. 点击 **同步表结构**，将表结构向量化存储到 Qdrant

#### Step 4: 配置字典 (可选)
1. 进入 **字典管理** 页面
2. 添加业务术语和同义词
   - 类型：SYNONYM / BUSINESS_TERM
   - 术语：销售额
   - 同义词：营收，收入
3. 这样用户问"营收"时会自动转换为"销售额"

#### Step 5: 开始问数
1. 进入 **问数** 页面
2. 输入自然语言问题，例如:
   - "上个月销售额是多少？"
   - "各部门利润对比"
   - "查看销售趋势"
3. 系统自动生成 SQL 并返回结果和图表

### 2. 高级功能使用

#### 数据脱敏配置
1. 进入 **字典管理** -> **脱敏规则** 标签
2. 添加脱敏规则:
   - 表名：users
   - 列名：phone
   - 脱敏类型：PARTIAL (部分显示)
   - 模式：3-4 (显示前 3 位和后 4 位)

#### API 开放平台
1. 进入 **LLM 配置** 页面
2. 创建 API Key:
   - 租户代码：example
   - 描述：第三方系统对接
   - 速率限制：100 次/分钟
3. 外部系统使用 API Key 调用问数接口:
```bash
curl -X POST http://localhost:8080/api/open/ask \
  -H "X-API-Key: oc4u_your-api-key" \
  -H "Content-Type: application/json" \
  -d '{"question": "上个月销售额"}'
```

#### 监控中心
1. 进入 **监控中心** 页面
2. 查看:
   - 数据源连接池状态
   - 活跃连接数
   - 连接使用率
   - 健康检查状态

---

## API 文档

### 认证接口

#### 登录
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "password",
  "tenantCode": "example"
}
```

响应:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "admin",
  "tenantId": 1,
  "tenantCode": "example",
  "tenantName": "示例企业"
}
```

### 问数接口

#### 自然语言问数
```http
POST /api/query/ask
Authorization: Bearer {token}
Content-Type: application/json

{
  "question": "上个月销售额是多少？",
  "tables": ["sales"]  // 可选，指定表名
}
```

响应:
```json
{
  "answer": "上个月销售额为 100 万元",
  "sql": "SELECT SUM(amount) FROM sales WHERE date >= '2024-01-01'",
  "data": [...],
  "chart": {
    "chartType": "bar",
    "categories": ["1 月", "2 月", "3 月"],
    "series": [{"name": "销售额", "data": [30, 35, 35]}]
  }
}
```

### 租户管理接口

#### 创建租户
```http
POST /api/admin/tenants
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "示例企业",
  "code": "example",
  "dbType": "POSTGRESQL",
  "jdbcUrl": "jdbc:postgresql://localhost:5432/sales_db",
  "username": "db_user",
  "password": "db_password",
  "maxConnections": 10,
  "connectionTimeout": 10000
}
```

### 字典管理接口

#### 创建字典
```http
POST /api/dictionary
Authorization: Bearer {token}
Content-Type: application/json

{
  "tenantCode": "example",
  "type": "SYNONYM",
  "term": "销售额",
  "synonyms": "营收，收入，销售收入",
  "description": "企业销售金额"
}
```

### API 开放接口

#### 使用 API Key 问数
```http
POST /api/open/ask
X-API-Key: oc4u_your-api-key
Content-Type: application/json

{
  "question": "查询销售数据"
}
```

---

## 常见问题

### 1. JWT 密钥错误
**错误**: `JWT secret is not configured!`

**解决**:
```bash
# 生成随机密钥
export JWT_SECRET=$(openssl rand -base64 32)

# 添加到 backend/.env 文件
echo "JWT_SECRET=$JWT_SECRET" >> backend/.env

# 重启后端服务
```

### 2. 数据库连接失败
**错误**: `Failed to register DataSource`

**检查**:
1. 确认 JDBC URL 格式正确
2. 确认数据库用户有只读权限
3. 确认防火墙允许连接
4. 检查数据库驱动是否已加载

### 3. Qdrant 连接失败
**错误**: `Failed to sync schema to Qdrant`

**检查**:
```bash
# 检查 Qdrant 容器状态
docker-compose ps

# 查看 Qdrant 日志
docker-compose logs qdrant

# 测试 Qdrant 连接
curl http://localhost:6333/
```

### 4. LLM API 调用失败
**错误**: `Failed to generate SQL`

**检查**:
1. 确认 API Key 配置正确
2. 检查网络连接
3. 查看 API 余额是否充足
4. 检查日志获取详细错误信息

### 5. 登录账户被锁定
**错误**: `Account is locked due to too many failed login attempts`

**解决**:
- 等待 15 分钟自动解锁
- 或联系管理员在数据库清空 `login_attempts` 表

### 6. 前端无法连接后端
**错误**: `Network Error`

**检查**:
1. 确认后端服务已启动
2. 检查前端代理配置 (`frontend/vite.config.js`)
3. 确认 CORS 配置正确
4. 检查浏览器控制台错误信息

---

## 技术支持

- **GitHub Issues**: 提交 Bug 报告和功能请求
- **邮箱**: support@openchat4u.com
- **文档**: https://docs.openchat4u.com

---

## 版本信息

- **当前版本**: 1.0.0
- **发布日期**: 2024-05-20
- **JDK 版本**: 17+
- **Spring Boot**: 3.2.5
- **Vue**: 3.4.21
