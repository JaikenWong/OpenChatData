#!/usr/bin/env bash
# 通用测试库：断言、HTTP、登录、MySQL 客户端。
# Source 进调用方脚本。

set -euo pipefail

# === 颜色 ===
if [ -t 1 ]; then
  C_OK=$'\e[32m'; C_FAIL=$'\e[31m'; C_INFO=$'\e[36m'; C_DIM=$'\e[2m'; C_RESET=$'\e[0m'
else
  C_OK=''; C_FAIL=''; C_INFO=''; C_DIM=''; C_RESET=''
fi

# === 计数器 ===
_PASS=0; _FAIL=0; _FAIL_NAMES=()

step() { echo; echo "${C_INFO}==> $*${C_RESET}"; }

pass() {
  _PASS=$((_PASS + 1))
  echo "  ${C_OK}✓${C_RESET} $1"
}

fail() {
  _FAIL=$((_FAIL + 1))
  _FAIL_NAMES+=("$1")
  echo "  ${C_FAIL}✗${C_RESET} $1"
  [ $# -ge 2 ] && echo "    ${C_DIM}$2${C_RESET}"
}

# 不退出，仅记录失败 — 让脚本继续跑完拿全貌。
# 致命错误用 die。
die() { echo "${C_FAIL}FATAL: $*${C_RESET}" >&2; exit 1; }

# === 断言 ===
# assert_eq <name> <expected> <actual>
assert_eq() {
  local name="$1" expected="$2" actual="$3"
  if [ "$expected" = "$actual" ]; then
    pass "$name (=$expected)"
  else
    fail "$name" "expected='$expected' actual='$actual'"
  fi
}

# assert_contains <name> <needle> <haystack>
assert_contains() {
  local name="$1" needle="$2" hay="$3"
  if echo "$hay" | grep -qi -- "$needle"; then
    pass "$name (contains '$needle')"
  else
    fail "$name" "haystack missing '$needle': $(echo "$hay" | head -c 200)"
  fi
}

# assert_not_empty <name> <value>
assert_not_empty() {
  if [ -n "${2:-}" ] && [ "${2:-}" != "null" ]; then
    pass "$1"
  else
    fail "$1" "value empty or null"
  fi
}

# assert_ge <name> <actual> <min>
assert_ge() {
  local name="$1" actual="$2" min="$3"
  if [ "$actual" -ge "$min" ] 2>/dev/null; then
    pass "$name ($actual >= $min)"
  else
    fail "$name" "actual=$actual min=$min"
  fi
}

# === HTTP ===
# http <method> <url> <auth?> <body?>
# stdout: body (用于 $(...) 捕获)
# 副作用: 把状态码写入文件，调用方用 assert_http_ok 读 (子 shell 全局变量丢失，用文件兜底)
HTTP_STATUS_FILE="${TMPDIR:-/tmp}/.openchat_http.status"

http() {
  local method="$1" url="$2" auth="${3:-}" body="${4:-}"
  local hdrs=(-H "Content-Type: application/json")
  [ -n "$auth" ] && hdrs+=(-H "Authorization: Bearer $auth")
  local tmp; tmp=$(mktemp)
  local status
  status=$(curl -sS -o "$tmp" -w "%{http_code}" -X "$method" "$url" "${hdrs[@]}" \
    ${body:+--data-binary "$body"})
  echo "$status" > "$HTTP_STATUS_FILE"
  cat "$tmp"
  rm -f "$tmp"
}

# assert_http_ok <name>
assert_http_ok() {
  local name="$1"
  local status
  status=$(cat "$HTTP_STATUS_FILE" 2>/dev/null || echo "0")
  if [ "$status" -ge 200 ] 2>/dev/null && [ "$status" -lt 300 ]; then
    pass "$name (HTTP $status)"
  else
    fail "$name" "HTTP $status"
  fi
}

# === Login ===
# login <api-base> <user> <pass> <tenant>  → echoes token; empty on fail
login() {
  local resp; resp=$(http POST "$1/api/auth/login" "" \
    "{\"username\":\"$2\",\"password\":\"$3\",\"tenantCode\":\"$4\"}")
  echo "$resp" | jq -r '.token // empty'
}

# === 等待后端 ===
wait_for_backend() {
  local url="$1" tries="${2:-30}"
  for _ in $(seq 1 "$tries"); do
    if curl -fsS "$url/api/health" >/dev/null 2>&1; then return 0; fi
    sleep 1
  done
  return 1
}

# === MySQL ===
# mysql_exec <container> <root-pw> <db?> -- runs query on stdin, stderr surfaced
mysql_exec() {
  local container="$1" pw="$2" db="${3:-}"
  if [ -n "$db" ]; then
    docker exec -i "$container" mysql --default-character-set=utf8mb4 -uroot -p"$pw" "$db"
  else
    docker exec -i "$container" mysql --default-character-set=utf8mb4 -uroot -p"$pw"
  fi
}

# wait_for_mysql <container> <root-pw> <tries>
wait_for_mysql() {
  local container="$1" pw="$2" tries="${3:-60}"
  for _ in $(seq 1 "$tries"); do
    if docker exec "$container" mysqladmin ping -uroot -p"$pw" --silent >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done
  return 1
}

# === 终态汇总 ===
print_summary() {
  echo
  echo "================================================"
  echo "  PASS: $_PASS    FAIL: $_FAIL"
  if [ "$_FAIL" -gt 0 ]; then
    echo "${C_FAIL}Failed assertions:${C_RESET}"
    for n in "${_FAIL_NAMES[@]}"; do echo "  - $n"; done
    echo "================================================"
    exit 1
  fi
  echo "${C_OK}全部通过${C_RESET}"
  echo "================================================"
}

# === 失败陷阱 ===
on_err() {
  local code=$?
  echo "${C_FAIL}[trap] 脚本在第 ${BASH_LINENO[0]} 行非预期退出 code=$code${C_RESET}" >&2
  print_summary || true
  exit $code
}
trap on_err ERR

require_jq() { command -v jq >/dev/null || die "需要 jq, 请先 brew install jq"; }
require_docker() { command -v docker >/dev/null || die "需要 docker"; }
