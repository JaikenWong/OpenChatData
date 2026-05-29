#!/usr/bin/env bash
#
# 跑全部脚本端到端测试，最终汇总。
# 用法: bash backend/scripts/run-all-tests.sh
#

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

PASS=0; FAIL=0; FAILED=()

run() {
  local name="$1" path="$2"
  echo
  echo "########################################"
  echo "# $name"
  echo "########################################"
  if bash "$path"; then
    PASS=$((PASS + 1))
    echo "[$name] ✓ PASS"
  else
    FAIL=$((FAIL + 1))
    FAILED+=("$name")
    echo "[$name] ✗ FAIL"
  fi
}

run "MySQL onboarding" "$SCRIPT_DIR/test-mysql.sh"
run "EAV + MVCC"       "$SCRIPT_DIR/test-eav-mvcc.sh"
run "Windchill PLM"    "$SCRIPT_DIR/test-windchill.sh"
run "Windchill 进阶"   "$SCRIPT_DIR/test-windchill-advanced.sh"

echo
echo "================================================"
echo "  SUITE PASS: $PASS  FAIL: $FAIL"
if [ "$FAIL" -gt 0 ]; then
  for n in "${FAILED[@]}"; do echo "  - $n"; done
  exit 1
fi
echo "全部脚本测试通过"
echo "================================================"
