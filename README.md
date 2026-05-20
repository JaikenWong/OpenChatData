# OpenChat4U - 多租智能问数平台

基于 Spring Boot 3.2 + Vue3 的多租户智能问数平台，支持多数据库对接、向量检索、NL2SQL、数据脱敏、审计日志和 RBAC 权限控制。

## 功能特性

### 核心功能
- **多租户隔离**: 每租户独立数据源、向量集合
- **多数据库支持**: PostgreSQL, MySQL, Oracle, SQL Server
- **数据源管理**: 动态配置 JDBC 连接，支持连接池调优
- **表结构同步**: 将表结构向量化存储到 Qdrant (讯飞 Embedding)
- **自然语言问数**: 通过 DeepSeek LLM 实现 NL2SQL
- **智能检索**: 讯飞 Rerank 重排序，选出最相关表结构
- **字典管理**: 同义词、业务术语映射，自动增强问数问题
- **数据脱敏**: 5种脱敏策略 (Full, Partial, Hash, Replace, Regex)
- **历史记录**: 查询历史持久化，支持搜索和统计
- **审计日志**: 全量请求审计追踪，支持按操作类型和时间范围筛选
- **RBAC 权限**: 角色权限管理，细粒度权限控制
- **流式输出**: SSE 实时响应
- **JWT 认证**: 基于 Token 的安全认证

### 技术栈

**后端**
- Spring Boot 3.2
- Java 17
- PostgreSQL (元数据存储)
- Qdrant (向量数据库)
- DeepSeek API (LLM)
- 讯飞星火 API (Embedding + Rerank)

**前端**
- Vue 3
- Element Plus
- Vite
- Pinia

## 快速开始

### 1. 启动基础设施

```bash
cd docker
docker-compose up -d
```

访问 Qdrant Dashboard: http://localhost:6333/dashboard

### 2. 配置环境变量

创建 `backend\.env` 文件：

```
DEEPSEEK_API_KEY=sk-your-deepseek-key
EMBEDDING_API_KEY=your-embedding-key
JWT_SECRET=your-secret-key-change-in-production
```

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
```

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问 http://localhost:3000

## API 接口

| 接口 | 说明 |
|------|------|
| POST /api/auth/login | 登录 |
| GET /api/admin/tenants | 租户列表 |
| POST /api/admin/tenants | 创建租户 |
| GET /api/admin/tenants/db-types | 获取支持的数据库类型 |
| GET /api/schema/{tenantCode}/tables | 获取表列表 |
| POST /api/schema/{tenantCode}/sync | 同步表结构到 Qdrant |
| POST /api/query/ask | 问数 |
| GET /api/history/{tenantCode} | 查询历史 |
| GET /api/dictionary/{tenantCode} | 字典列表 |
| POST /api/dictionary | 创建字典 |
| GET /api/masking/{tenantCode} | 脱敏规则列表 |
| POST /api/masking | 创建脱敏规则 |
| GET /api/audit/{tenantCode} | 审计日志 |
| GET /api/rbac/roles | 角色列表 |
| POST /api/rbac/roles | 创建角色 |
| POST /api/stream/chat | SSE 流式输出 |

## 使用流程

1. 登录系统
2. 在租户管理中创建新租户，选择数据库类型并配置 JDBC 连接
3. 在表结构页面选择租户，同步表结构到 Qdrant
4. 在字典管理中配置同义词和业务术语（可选）
5. 在脱敏规则中配置数据脱敏策略（可选）
6. 在问数页面输入自然语言问题

## 支持的数据库类型

| 数据库 | JDBC URL 示例 |
|--------|--------------|
| PostgreSQL | jdbc:postgresql://host:port/db |
| MySQL | jdbc:mysql://host:port/db |
| Oracle | jdbc:oracle:thin:@host:port:db |
| SQL Server | jdbc:sqlserver://host:port;databaseName=db |

## 脱敏策略

| 策略 | 说明 | 示例 |
|------|------|------|
| FULL | 全量替换 | `13812345678` -> `***********` |
| PARTIAL | 部分显示 | `13812345678` -> `13******78` (pattern: 2-2) |
| HASH | 哈希处理 | `13812345678` -> `a1b2c3d4e5f6...` |
| REPLACE | 固定替换 | `13812345678` -> `***` |
| REGEX | 正则替换 | 自定义正则匹配替换 |

## 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                        前端 (Vue3)                          │
├─────────────────────────────────────────────────────────────┤
│                     API Gateway / Nginx                     │
├─────────────────────────────────────────────────────────────┤
│                    Spring Boot Backend                      │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐  │
│  │  认证    │ │  问数    │ │  字典    │ │   脱敏       │  │
│  │  JWT     │ │  NL2SQL  │ │  管理    │ │   规则       │  │
│  └──────────┘ └──────────┘ └──────────┘ └──────────────┘  │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐  │
│  │  历史    │ │  审计    │ │  RBAC    │ │   数据源     │  │
│  │  记录    │ │  日志    │ │  权限    │ │   注册       │  │
│  └──────────┘ └──────────┘ └──────────┘ └──────────────┘  │
├─────────────────────────────────────────────────────────────┤
│                    外部服务 & 存储                          │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐  │
│  │PostgreSQL│ │  Qdrant  │ │ DeepSeek │ │   讯飞       │  │
│  │(元数据)  │ │(向量库)  │ │  (LLM)   │ │(Embedding)   │  │
│  └──────────┘ └──────────┘ └──────────┘ └──────────────┘  │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐                   │
│  │ MySQL    │ │  Oracle  │ │ SQL Srv  │                   │
│  │(租户DB)  │ │(租户DB)  │ │(租户DB)  │                   │
│  └──────────┘ └──────────┘ └──────────┘                   │
└─────────────────────────────────────────────────────────────┘
```

## 配置说明

### application.yml

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/openchat4u
    username: postgres
    password: postgres

qdrant:
  host: localhost
  port: 6334

deepseek:
  api-key: ${DEEPSEEK_API_KEY}
  base-url: https://api.deepseek.com
  model: deepseek-chat

embedding:
  api-key: ${EMBEDDING_API_KEY}
  base-url: https://maas-api.cn-huabei-1.xf-yun.com/v2/embeddings
  model: xop3qwen8bembedding

rerank:
  api-key: ${EMBEDDING_API_KEY}
  base-url: https://maas-api.cn-huabei-1.xf-yun.com/v2/rerank
  model: xop3qwen8bembedding

jwt:
  secret: ${JWT_SECRET}
  expiration: 86400000
```

## 演进规划

### Phase 1: 核心功能补全 ✅
- [x] 多数据库支持 (MySQL, Oracle, SQL Server)
- [x] Qdrant 向量检索集成
- [x] 历史记录持久化
- [x] 字典管理（同义词、业务术语）
- [x] 数据脱敏配置
- [x] 查询审计日志

### Phase 2: 能力增强 ✅
- [x] 细粒度权限控制 (RBAC)
- [x] 流式输出 (SSE)
- [x] SQL 执行计划分析
- [x] 图表可视化 (ECharts)
- [x] 问答上下文记忆

### Phase 3: 企业级特性 ✅
- [x] 数据源连接池监控
- [x] 多 LLM 支持（可切换）
- [x] API 开放平台
- [x] 慢查询分析

### Phase 4: 智能化提升 ✅
- [x] 自动表关系发现
- [x] SQL 模板库
- [x] 问数准确率评估
- [x] 反馈学习机制
