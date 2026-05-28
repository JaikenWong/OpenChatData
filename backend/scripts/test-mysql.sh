#!/usr/bin/env bash
#
# End-to-end MySQL onboarding for OpenChat4U.
#
# Usage:
#   bash backend/scripts/test-mysql.sh [question]
#
# Prereqs:
#   * docker available locally
#   * backend running at http://localhost:8080 (mvn spring-boot:run)
#   * admin user seeded (default: admin / admin123, tenant demo)
#

set -euo pipefail

API_BASE="${API_BASE:-http://localhost:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASS="${ADMIN_PASS:-admin123}"
ADMIN_TENANT="${ADMIN_TENANT:-demo}"

MYSQL_CONTAINER="${MYSQL_CONTAINER:-demo-mysql}"
MYSQL_PORT="${MYSQL_PORT:-3307}"
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-root}"
MYSQL_DB="${MYSQL_DB:-salesdb}"
# Host the BACKEND uses to reach MySQL.
#   backend on host (mvn spring-boot:run)  -> localhost
#   backend in docker (docker-compose)     -> host.docker.internal
MYSQL_HOST_FOR_BACKEND="${MYSQL_HOST_FOR_BACKEND:-localhost}"

TENANT_NAME="${TENANT_NAME:-销售示例}"
TENANT_CODE="${TENANT_CODE:-sales}"
QUESTION="${1:-统计每个客户的订单总金额,按金额降序}"

step() { echo; echo "==> $*"; }

require_jq() {
  if ! command -v jq >/dev/null 2>&1; then
    echo "需要 jq, 请先 brew install jq" >&2
    exit 1
  fi
}

require_jq

step "1. 起 MySQL ($MYSQL_CONTAINER 端口 $MYSQL_PORT)"
if docker ps -a --format '{{.Names}}' | grep -q "^${MYSQL_CONTAINER}$"; then
  docker start "$MYSQL_CONTAINER" >/dev/null
else
  docker run -d --name "$MYSQL_CONTAINER" \
    -e MYSQL_ROOT_PASSWORD="$MYSQL_ROOT_PASSWORD" \
    -e MYSQL_DATABASE="$MYSQL_DB" \
    -p "${MYSQL_PORT}:3306" \
    mysql:8 >/dev/null
fi

step "2. 等 MySQL 就绪"
for i in $(seq 1 60); do
  if docker exec "$MYSQL_CONTAINER" mysqladmin ping -uroot -p"$MYSQL_ROOT_PASSWORD" --silent >/dev/null 2>&1; then
    echo "ready"
    break
  fi
  sleep 2
done

step "3. 灌测试数据"
docker exec -i "$MYSQL_CONTAINER" mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DB" <<'SQL'
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS customers;

CREATE TABLE customers (
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) COMMENT '客户姓名',
  region VARCHAR(50) COMMENT '所在地区',
  created_at DATETIME
) COMMENT='客户表';

CREATE TABLE orders (
  id INT PRIMARY KEY AUTO_INCREMENT,
  customer_id INT COMMENT '客户ID',
  product VARCHAR(100) COMMENT '商品名',
  amount DECIMAL(10,2) COMMENT '金额',
  created_at DATETIME COMMENT '下单时间'
) COMMENT='订单表';

INSERT INTO customers(name,region,created_at) VALUES
('张三','华北','2026-01-15'),('李四','华东','2026-02-03'),('王五','华南','2026-03-20');

INSERT INTO orders(customer_id,product,amount,created_at) VALUES
(1,'iPhone',8000,'2026-04-01'),(1,'iPad',5000,'2026-04-05'),
(2,'MacBook',15000,'2026-04-10'),(3,'AirPods',1500,'2026-04-12'),
(2,'iPhone',8000,'2026-04-20');
SQL

step "4. 登录 admin 拿 token"
TOKEN=$(curl -sS -X POST "$API_BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$ADMIN_USER\",\"password\":\"$ADMIN_PASS\",\"tenantCode\":\"$ADMIN_TENANT\"}" \
  | jq -r .token)
[ "$TOKEN" != "null" ] && [ -n "$TOKEN" ] || { echo "登录失败"; exit 1; }
echo "token: ${TOKEN:0:40}..."

step "5. 创建租户 $TENANT_CODE (如已存在, 跳过)"
EXISTING=$(curl -sS "$API_BASE/api/admin/tenants" -H "Authorization: Bearer $TOKEN" \
  | jq "[.[] | select(.code==\"$TENANT_CODE\")] | length")
if [ "$EXISTING" -eq 0 ]; then
  curl -sS -X POST "$API_BASE/api/admin/tenants" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "$(cat <<JSON
{
  "name": "$TENANT_NAME",
  "code": "$TENANT_CODE",
  "description": "MySQL 销售测试库",
  "dbType": "MYSQL",
  "jdbcUrl": "jdbc:mysql://${MYSQL_HOST_FOR_BACKEND}:${MYSQL_PORT}/${MYSQL_DB}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true",
  "username": "root",
  "password": "$MYSQL_ROOT_PASSWORD",
  "maxConnections": 5,
  "connectionTimeout": 10000
}
JSON
)" | jq .
else
  echo "租户已存在"
fi

step "6. 验证连通: 列表"
curl -sS "$API_BASE/api/schema/$TENANT_CODE/tables" -H "Authorization: Bearer $TOKEN" | jq .

step "7. 同步表结构到 Qdrant"
curl -sS -X POST "$API_BASE/api/schema/$TENANT_CODE/sync" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '["customers","orders"]' | jq .

step "8. 给 $TENANT_CODE 租户也种一个 admin/admin123 用户 (postgres 容器内执行)"
PG_CONTAINER="${PG_CONTAINER:-openchat4u-postgres}"
docker exec "$PG_CONTAINER" psql -U postgres -d openchat4u -c \
  "INSERT INTO users(username,password_hash,tenant_code,status,login_attempts,created_at,updated_at)
   VALUES('admin','\$2a\$10\$iRy9I1nsTTY3ta5gMP35MebXJDBRKuAb8cROzNK1OTlclfXfQkMgC','$TENANT_CODE','ACTIVE',0,NOW(),NOW())
   ON CONFLICT (username, tenant_code) DO NOTHING;"

step "9. 切到 $TENANT_CODE 租户登录并问数"
TOKEN_SALES=$(curl -sS -X POST "$API_BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$ADMIN_USER\",\"password\":\"$ADMIN_PASS\",\"tenantCode\":\"$TENANT_CODE\"}" \
  | jq -r .token)

if [ "$TOKEN_SALES" = "null" ] || [ -z "$TOKEN_SALES" ]; then
  echo "提示: $TENANT_CODE 租户下无用户, 跳过问数。先按 step 8 建用户后重跑。"
  exit 0
fi

curl -sS -X POST "$API_BASE/api/query/ask" \
  -H "Authorization: Bearer $TOKEN_SALES" \
  -H "Content-Type: application/json" \
  -d "{\"question\":\"$QUESTION\"}" | jq .
