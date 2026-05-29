#!/usr/bin/env bash
#
# End-to-end MySQL onboarding for OpenChat4U (带断言版本).
#
# Usage:
#   bash backend/scripts/test-mysql.sh [question]
#
# Prereqs:
#   * docker, jq
#   * backend running at http://localhost:8080 (mvn spring-boot:run)
#   * admin user seeded (默认 admin/admin123, tenant demo)
#

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib.sh
source "$SCRIPT_DIR/lib.sh"

API_BASE="${API_BASE:-http://localhost:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASS="${ADMIN_PASS:-admin123}"
ADMIN_TENANT="${ADMIN_TENANT:-demo}"

MYSQL_CONTAINER="${MYSQL_CONTAINER:-demo-mysql}"
MYSQL_PORT="${MYSQL_PORT:-3307}"
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-root}"
MYSQL_DB="${MYSQL_DB:-salesdb}"
MYSQL_HOST_FOR_BACKEND="${MYSQL_HOST_FOR_BACKEND:-localhost}"
PG_CONTAINER="${PG_CONTAINER:-openchat4u-postgres}"

TENANT_NAME="${TENANT_NAME:-销售示例}"
TENANT_CODE="${TENANT_CODE:-sales}"
QUESTION="${1:-统计每个客户的订单总金额,按金额降序}"

require_jq
require_docker

step "0. 后端可达性"
if wait_for_backend "$API_BASE" 5; then
  pass "backend up at $API_BASE"
else
  die "backend 未启动: $API_BASE/api/health"
fi

step "1. 起 MySQL ($MYSQL_CONTAINER 端口 $MYSQL_PORT)"
if docker ps -a --format '{{.Names}}' | grep -q "^${MYSQL_CONTAINER}$"; then
  docker start "$MYSQL_CONTAINER" >/dev/null
  pass "已存在容器已启动"
else
  docker run -d --name "$MYSQL_CONTAINER" \
    -e MYSQL_ROOT_PASSWORD="$MYSQL_ROOT_PASSWORD" \
    -e MYSQL_DATABASE="$MYSQL_DB" \
    -p "${MYSQL_PORT}:3306" \
    mysql:8 >/dev/null
  pass "新容器已创建"
fi

step "2. 等 MySQL 就绪"
if wait_for_mysql "$MYSQL_CONTAINER" "$MYSQL_ROOT_PASSWORD" 60; then
  pass "MySQL ready"
else
  die "MySQL 60 次重试仍未就绪"
fi

step "3. 灌测试数据"
mysql_exec "$MYSQL_CONTAINER" "$MYSQL_ROOT_PASSWORD" "$MYSQL_DB" <<'SQL'
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

ROWS=$(docker exec -i "$MYSQL_CONTAINER" mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -s -e \
  "SELECT (SELECT COUNT(*) FROM $MYSQL_DB.customers) + (SELECT COUNT(*) FROM $MYSQL_DB.orders);" 2>/dev/null)
assert_eq "种子数据行数 (customers + orders)" "8" "$ROWS"

step "4. admin 登录拿 token"
TOKEN=$(login "$API_BASE" "$ADMIN_USER" "$ADMIN_PASS" "$ADMIN_TENANT")
assert_not_empty "admin token" "$TOKEN"
[ -n "$TOKEN" ] || die "登录失败 — 检查 admin 用户是否种子化"
echo "  token: ${TOKEN:0:40}..."

step "5. 创建租户 $TENANT_CODE (如已存在则跳过)"
LIST=$(http GET "$API_BASE/api/admin/tenants" "$TOKEN")
assert_http_ok "GET /api/admin/tenants"
EXISTING=$(echo "$LIST" | jq "[.[] | select(.code==\"$TENANT_CODE\")] | length")
if [ "${EXISTING:-0}" = "0" ]; then
  BODY=$(cat <<JSON
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
)
  RESP=$(http POST "$API_BASE/api/admin/tenants" "$TOKEN" "$BODY")
  assert_http_ok "POST /api/admin/tenants"
  CREATED_CODE=$(echo "$RESP" | jq -r '.code // empty')
  assert_eq "返回的 tenant code" "$TENANT_CODE" "$CREATED_CODE"
else
  pass "租户已存在, 跳过创建"
fi

step "6. 列表表结构"
TABLES=$(http GET "$API_BASE/api/schema/$TENANT_CODE/tables" "$TOKEN")
assert_http_ok "GET /api/schema/$TENANT_CODE/tables"
N=$(echo "$TABLES" | jq 'length')
assert_ge "表数量" "${N:-0}" "2"
assert_contains "表列表含 customers" "customers" "$TABLES"
assert_contains "表列表含 orders" "orders" "$TABLES"

step "7. 同步到 Qdrant"
SYNC=$(http POST "$API_BASE/api/schema/$TENANT_CODE/sync" "$TOKEN" '["customers","orders"]')
assert_http_ok "POST /api/schema/$TENANT_CODE/sync"
SYNCED=$(echo "$SYNC" | jq -r '.synced // .count // "0"')
echo "  synced response: $(echo "$SYNC" | head -c 200)"

step "8. 给 $TENANT_CODE 种 admin/admin123"
docker exec "$PG_CONTAINER" psql -U postgres -d openchat4u -c \
  "INSERT INTO users(username,password_hash,tenant_code,status,login_attempts,created_at,updated_at)
   VALUES('admin','\$2a\$10\$iRy9I1nsTTY3ta5gMP35MebXJDBRKuAb8cROzNK1OTlclfXfQkMgC','$TENANT_CODE','ACTIVE',0,NOW(),NOW())
   ON CONFLICT (username, tenant_code) DO NOTHING;" >/dev/null
pass "user seeded"

step "9. 切到 $TENANT_CODE 问数"
TOKEN_SALES=$(login "$API_BASE" "$ADMIN_USER" "$ADMIN_PASS" "$TENANT_CODE")
assert_not_empty "tenant token" "$TOKEN_SALES"
[ -n "$TOKEN_SALES" ] || die "$TENANT_CODE 登录失败"

ASK=$(http POST "$API_BASE/api/query/ask" "$TOKEN_SALES" "{\"question\":\"$QUESTION\"}")
assert_http_ok "POST /api/query/ask"
SQL=$(echo "$ASK" | jq -r '.sql // empty')
assert_not_empty "生成 SQL 非空" "$SQL"
echo "  生成 SQL: $SQL"
# 关键字断言：query 含 SUM + 客户 + 订单
assert_contains "SQL 含 SUM" "sum" "$SQL"
assert_contains "SQL 含 customers" "customers" "$SQL"
assert_contains "SQL 含 orders" "orders" "$SQL"
DATA_LEN=$(echo "$ASK" | jq '.data | length // 0')
assert_ge "返回数据行数" "${DATA_LEN:-0}" "1"

print_summary
