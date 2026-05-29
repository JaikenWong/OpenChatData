#!/usr/bin/env bash
#
# EAV 多表 + MVCC 快照隔离测试 (带断言版本).
#
# 模型:
#   products          (id, name, category)
#   attribute_defs    (id, attr_name, attr_type, unit)
#   attribute_values  (id, product_id, attr_def_id, value)  <- EAV 核心
#
# Usage:
#   bash backend/scripts/test-eav-mvcc.sh
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
MYSQL_DB="${MYSQL_DB:-eavdb}"
MYSQL_HOST_FOR_BACKEND="${MYSQL_HOST_FOR_BACKEND:-localhost}"
PG_CONTAINER="${PG_CONTAINER:-openchat4u-postgres}"

TENANT_CODE="${TENANT_CODE:-eav}"
TX_SLEEP="${TX_SLEEP:-20}"     # 背景事务持续秒数，要 >> 查询耗时
QUERY_DELAY="${QUERY_DELAY:-4}" # 启动 tx 后等多久再查 (确保 tx 已发出 UPDATE)

require_jq
require_docker

step "0. 后端可达性"
wait_for_backend "$API_BASE" 5 || die "backend 未启动: $API_BASE/api/health"
pass "backend up"

step "1. 建 EAV 库 + 表 + 数据"
mysql_exec "$MYSQL_CONTAINER" "$MYSQL_ROOT_PASSWORD" <<SQL
CREATE DATABASE IF NOT EXISTS $MYSQL_DB CHARACTER SET utf8mb4;
USE $MYSQL_DB;
DROP TABLE IF EXISTS attribute_values;
DROP TABLE IF EXISTS attribute_defs;
DROP TABLE IF EXISTS products;

CREATE TABLE products (
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) COMMENT '商品名称',
  category VARCHAR(50) COMMENT '商品分类',
  created_at DATETIME
) COMMENT='商品主表';

CREATE TABLE attribute_defs (
  id INT PRIMARY KEY AUTO_INCREMENT,
  attr_name VARCHAR(50) COMMENT '属性名称',
  attr_type VARCHAR(20) COMMENT '属性数据类型: string/number',
  unit VARCHAR(20) COMMENT '单位'
) COMMENT='属性定义表';

CREATE TABLE attribute_values (
  id INT PRIMARY KEY AUTO_INCREMENT,
  product_id INT COMMENT '商品ID, 关联 products.id',
  attr_def_id INT COMMENT '属性定义ID, 关联 attribute_defs.id',
  value VARCHAR(200) COMMENT '属性具体值'
) COMMENT='属性值表(EAV)';

INSERT INTO products(name,category,created_at) VALUES
('iPhone 15','手机','2026-01-10'),
('MacBook Pro','笔记本','2026-02-15'),
('AirPods Pro','耳机','2026-03-20');

INSERT INTO attribute_defs(attr_name,attr_type,unit) VALUES
('颜色','string',NULL),
('内存','number','GB'),
('重量','number','g'),
('屏幕尺寸','number','英寸');

INSERT INTO attribute_values(product_id,attr_def_id,value) VALUES
(1,1,'黑色'),(1,2,'256'),(1,3,'171'),(1,4,'6.1'),
(2,1,'银色'),(2,2,'512'),(2,3,'1600'),(2,4,'14'),
(3,1,'白色'),(3,3,'56');
SQL

PROD_COUNT=$(docker exec -i "$MYSQL_CONTAINER" mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -s -e \
  "SELECT COUNT(*) FROM $MYSQL_DB.products;" 2>/dev/null)
assert_eq "products 行数" "3" "$PROD_COUNT"
AV_COUNT=$(docker exec -i "$MYSQL_CONTAINER" mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -s -e \
  "SELECT COUNT(*) FROM $MYSQL_DB.attribute_values;" 2>/dev/null)
assert_eq "attribute_values 行数" "10" "$AV_COUNT"

step "2. admin 登录"
TOKEN=$(login "$API_BASE" "$ADMIN_USER" "$ADMIN_PASS" "$ADMIN_TENANT")
assert_not_empty "admin token" "$TOKEN"
[ -n "$TOKEN" ] || die "登录失败"

step "3. 创建/重建租户 $TENANT_CODE"
LIST=$(http GET "$API_BASE/api/admin/tenants" "$TOKEN")
EXIST_ID=$(echo "$LIST" | jq -r ".[] | select(.code==\"$TENANT_CODE\") | .id" | head -1)
if [ -n "$EXIST_ID" ]; then
  http DELETE "$API_BASE/api/admin/tenants/$EXIST_ID" "$TOKEN" >/dev/null
  assert_http_ok "删除旧租户"
fi
BODY=$(cat <<JSON
{
  "name": "EAV示例",
  "code": "$TENANT_CODE",
  "dbType": "MYSQL",
  "jdbcUrl": "jdbc:mysql://${MYSQL_HOST_FOR_BACKEND}:${MYSQL_PORT}/${MYSQL_DB}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true",
  "username": "root",
  "password": "$MYSQL_ROOT_PASSWORD",
  "maxConnections": 5,
  "connectionTimeout": 10000
}
JSON
)
http POST "$API_BASE/api/admin/tenants" "$TOKEN" "$BODY" >/dev/null
assert_http_ok "POST /api/admin/tenants"

step "4. 同步表结构"
http POST "$API_BASE/api/schema/$TENANT_CODE/sync" "$TOKEN" \
  '["products","attribute_defs","attribute_values"]' >/dev/null
assert_http_ok "POST /api/schema/$TENANT_CODE/sync"

step "5. 给 $TENANT_CODE 种用户"
docker exec "$PG_CONTAINER" psql -U postgres -d openchat4u -c \
  "INSERT INTO users(username,password_hash,tenant_code,status,login_attempts,created_at,updated_at)
   VALUES('admin','\$2a\$10\$iRy9I1nsTTY3ta5gMP35MebXJDBRKuAb8cROzNK1OTlclfXfQkMgC','$TENANT_CODE','ACTIVE',0,NOW(),NOW())
   ON CONFLICT (username, tenant_code) DO NOTHING;" >/dev/null
T2=$(login "$API_BASE" "$ADMIN_USER" "$ADMIN_PASS" "$TENANT_CODE")
assert_not_empty "$TENANT_CODE token" "$T2"
[ -n "$T2" ] || die "$TENANT_CODE 登录失败"

# Returns: json {sql, answer, data}
ask_json() {
  http POST "$API_BASE/api/query/ask" "$T2" "{\"question\":\"$1\"}"
}

step "6. EAV 多表 JOIN 问数"

# Q1: 全属性列出
R=$(ask_json "iPhone 15 的所有属性和对应的值是什么")
assert_http_ok "Q1 HTTP"
SQL1=$(echo "$R" | jq -r '.sql // empty')
assert_not_empty "Q1 sql" "$SQL1"
echo "  Q1 sql: $SQL1"
if echo "$SQL1" | grep -qiE 'attribute_values|v_products'; then
  pass "Q1 SQL 走 EAV 或 pivot 视图"
else
  fail "Q1 SQL 路径不对" "$SQL1"
fi
LEN1=$(echo "$R" | jq '.data | length // 0')
assert_ge "Q1 返回行数" "${LEN1:-0}" "1"

# Q2: 颜色过滤 — LLM 可能走原始 EAV 三表 (attribute_defs) 也可能走 v_products 视图，都可接受
R=$(ask_json "颜色是黑色的商品有哪些")
SQL2=$(echo "$R" | jq -r '.sql // empty')
echo "  Q2 sql: $SQL2"
if echo "$SQL2" | grep -qiE 'attribute_defs|v_products'; then
  pass "Q2 SQL 走 EAV 三表或 pivot 视图"
else
  fail "Q2 SQL 没走 EAV 或视图" "$SQL2"
fi
assert_contains "Q2 SQL 含 '黑色'" "黑色" "$SQL2"
DATA2=$(echo "$R" | jq -c '.data')
assert_contains "Q2 数据含 iPhone 15" "iPhone 15" "$DATA2"

# Q3: 双属性
R=$(ask_json "列出每个商品的名称、分类以及它的内存大小")
SQL3=$(echo "$R" | jq -r '.sql // empty')
echo "  Q3 sql: $SQL3"
if echo "$SQL3" | grep -qiE 'products'; then
  pass "Q3 SQL 含 products / v_products"
else
  fail "Q3 SQL 不含 products" "$SQL3"
fi

step "7. MVCC 快照隔离"
echo "  背景事务: UPDATE iPhone 内存 256→1024, SLEEP $TX_SLEEP, 然后 COMMIT"
TX_LOG=$(mktemp)
(
  mysql_exec "$MYSQL_CONTAINER" "$MYSQL_ROOT_PASSWORD" "$MYSQL_DB" <<SQL > "$TX_LOG" 2>&1
START TRANSACTION;
UPDATE attribute_values v JOIN attribute_defs d ON v.attr_def_id=d.id
  SET v.value='1024'
  WHERE v.product_id=1 AND d.attr_name='内存';
SELECT SLEEP($TX_SLEEP);
COMMIT;
SQL
) &
TX_PID=$!

# 失败时确保杀掉 tx 进程，防 mysql 容器锁住
cleanup_tx() {
  if kill -0 $TX_PID 2>/dev/null; then
    echo "[cleanup] kill background tx $TX_PID"
    kill $TX_PID 2>/dev/null || true
  fi
  rm -f "$TX_LOG" 2>/dev/null || true
}
trap cleanup_tx EXIT

sleep "$QUERY_DELAY"
echo
echo "  >>> [提交前 t+${QUERY_DELAY}s] 查 iPhone 内存 — 期望旧值 256"
R=$(ask_json "iPhone 15 的内存是多少")
SQL_PRE=$(echo "$R" | jq -r '.sql // empty')
DATA_PRE=$(echo "$R" | jq -c '.data')
echo "  pre sql:  $SQL_PRE"
echo "  pre data: $DATA_PRE"
assert_contains "MVCC 提交前快照 = 256" "256" "$DATA_PRE"

# 进一步确认未泄露 1024 (防 dirty read)
if echo "$DATA_PRE" | grep -q "1024"; then
  fail "MVCC dirty read 检测" "提交前看到了未提交值 1024"
else
  pass "MVCC 未泄露未提交值 1024"
fi

echo "  >>> 等事务提交..."
wait $TX_PID || true
sleep 1

# 直接走 MySQL 兜底确认提交真发生
COMMITTED_VAL=$(docker exec -i "$MYSQL_CONTAINER" mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" -N -s -e \
  "SELECT v.value FROM $MYSQL_DB.attribute_values v JOIN $MYSQL_DB.attribute_defs d ON v.attr_def_id=d.id
   WHERE v.product_id=1 AND d.attr_name='内存';" 2>/dev/null)
assert_eq "MySQL 直查提交后值" "1024" "$COMMITTED_VAL"

echo
echo "  >>> [提交后] 查 iPhone 内存 — 期望新值 1024"
R=$(ask_json "iPhone 15 的内存是多少")
DATA_POST=$(echo "$R" | jq -c '.data')
echo "  post data: $DATA_POST"
assert_contains "MVCC 提交后快照 = 1024" "1024" "$DATA_POST"

print_summary
