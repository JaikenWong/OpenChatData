#!/usr/bin/env bash
#
# EAV (Entity-Attribute-Value) 多表 + MVCC 快照隔离测试。
#
# 模型:
#   products          主表       (id, name, category)
#   attribute_defs    属性定义   (id, attr_name, attr_type, unit)
#   attribute_values  属性值     (id, product_id, attr_def_id, value)  <- EAV 核心
#
# Usage:
#   bash backend/scripts/test-eav-mvcc.sh
#
set -euo pipefail

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

step() { echo; echo "==> $*"; }
mysql_run() { docker exec -i "$MYSQL_CONTAINER" mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" "$@" 2>/dev/null; }

command -v jq >/dev/null || { echo "需要 jq"; exit 1; }

step "1. 建 EAV 库 + 表 + 数据"
docker exec -i "$MYSQL_CONTAINER" mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" 2>/dev/null <<SQL
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

-- iPhone 15 (product 1)
INSERT INTO attribute_values(product_id,attr_def_id,value) VALUES
(1,1,'黑色'),(1,2,'256'),(1,3,'171'),(1,4,'6.1');
-- MacBook Pro (product 2)
INSERT INTO attribute_values(product_id,attr_def_id,value) VALUES
(2,1,'银色'),(2,2,'512'),(2,3,'1600'),(2,4,'14');
-- AirPods Pro (product 3, 无屏幕)
INSERT INTO attribute_values(product_id,attr_def_id,value) VALUES
(3,1,'白色'),(3,3,'56');
SQL
echo "数据就绪:"
mysql_run "$MYSQL_DB" -e "SELECT p.name, d.attr_name, v.value, d.unit FROM products p JOIN attribute_values v ON p.id=v.product_id JOIN attribute_defs d ON v.attr_def_id=d.id ORDER BY p.id, d.id;"

step "2. 登录 admin"
TOKEN=$(curl -sS -X POST "$API_BASE/api/auth/login" -H "Content-Type: application/json" \
  -d "{\"username\":\"$ADMIN_USER\",\"password\":\"$ADMIN_PASS\",\"tenantCode\":\"$ADMIN_TENANT\"}" | jq -r .token)
[ -n "$TOKEN" ] && [ "$TOKEN" != "null" ] || { echo "登录失败"; exit 1; }

step "3. 创建/重建租户 $TENANT_CODE"
EXIST_ID=$(curl -sS "$API_BASE/api/admin/tenants" -H "Authorization: Bearer $TOKEN" \
  | jq -r ".[] | select(.code==\"$TENANT_CODE\") | .id" | head -1)
if [ -n "$EXIST_ID" ]; then
  curl -sS -X DELETE "$API_BASE/api/admin/tenants/$EXIST_ID" -H "Authorization: Bearer $TOKEN" >/dev/null
fi
curl -sS -X POST "$API_BASE/api/admin/tenants" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d "$(cat <<JSON
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
)" | jq '{id,code,dbType}'

step "4. 同步表结构"
curl -sS -X POST "$API_BASE/api/schema/$TENANT_CODE/sync" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '["products","attribute_defs","attribute_values"]' | jq .

step "5. 给 $TENANT_CODE 种用户"
docker exec "$PG_CONTAINER" psql -U postgres -d openchat4u -c \
  "INSERT INTO users(username,password_hash,tenant_code,status,login_attempts,created_at,updated_at)
   VALUES('admin','\$2a\$10\$iRy9I1nsTTY3ta5gMP35MebXJDBRKuAb8cROzNK1OTlclfXfQkMgC','$TENANT_CODE','ACTIVE',0,NOW(),NOW())
   ON CONFLICT (username, tenant_code) DO NOTHING;" >/dev/null && echo "user ready"

T2=$(curl -sS -X POST "$API_BASE/api/auth/login" -H "Content-Type: application/json" \
  -d "{\"username\":\"$ADMIN_USER\",\"password\":\"$ADMIN_PASS\",\"tenantCode\":\"$TENANT_CODE\"}" | jq -r .token)

ask() {
  echo
  echo "---- Q: $1"
  curl -sS -X POST "$API_BASE/api/query/ask" \
    -H "Authorization: Bearer $T2" -H "Content-Type: application/json" \
    -d "{\"question\":\"$1\"}" | jq '{sql, answer, data}'
}

step "6. EAV 多表 JOIN 问数"
ask "iPhone 15 的所有属性和对应的值是什么"
ask "颜色是黑色的商品有哪些"
ask "列出每个商品的名称、分类以及它的内存大小"

step "7. MVCC 快照隔离测试"
echo "后台开事务: 改 iPhone 15 内存 256->1024, 不提交, 持有 15 秒后才 COMMIT"
docker exec -i "$MYSQL_CONTAINER" mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DB" 2>/dev/null <<SQL &
START TRANSACTION;
UPDATE attribute_values v JOIN attribute_defs d ON v.attr_def_id=d.id
  SET v.value='1024'
  WHERE v.product_id=1 AND d.attr_name='内存';
SELECT SLEEP(15);
COMMIT;
SQL
TX_PID=$!

sleep 3
echo
echo ">>> [提交前] 查 iPhone 内存 — 期望仍是旧值 256 (MVCC: 未提交事务对其他会话不可见)"
ask "iPhone 15 的内存是多少"

echo ">>> 等事务提交..."
wait $TX_PID
sleep 1
echo
echo ">>> [提交后] 查 iPhone 内存 — 期望新值 1024"
ask "iPhone 15 的内存是多少"

step "完成"
echo "若提交前=256 提交后=1024, 说明读路径走 MySQL MVCC 快照, 只读已提交数据。"
