#!/usr/bin/env bash
#
# 维斯佩拉 · 本地启动器（只管开发和测试，不管部署）
#
#   ./dev.sh          开发服：server/dev/，端口 25565（离线验证，主城在出生点）
#   ./dev.sh debug    同上，并给服务端开 5005 调试端口
#   ./dev.sh test     测试服：server/test/，端口 25599
#   ./dev.sh smoke    验收：起→自检→灰盒探针→退出，退出码即结论
#   ./dev.sh stop     停掉正在跑的本地服务端（清端口与目录锁）
#
# 部署是另一件事，见 deploy.sh —— 那个脚本不碰这些目录。

set -euo pipefail
cd "$(dirname "$0")"

mode="${1:-dev}"

# -- 停 ------------------------------------------------------------------
#
# IDEA 的 Stop（尤其是 Force Stop）会把启动器杀掉，而服务端是它的子进程，可能活下来。
# 之后每次启动都看到"端口被占用"，却找不到是谁——所以给一个明确的停法。

stop_one() {
  local dir="$1"
  local lock="$dir/.vespera.lock"
  [ -f "$lock" ] || return 1
  local pid port
  pid="$(cut -d' ' -f1 "$lock")"
  port="$(cut -d' ' -f2 "$lock")"
  echo "停止 ${dir}（launcher pid ${pid}，端口 ${port}）……"
  kill -TERM "$pid" 2>/dev/null || true
  for _ in $(seq 1 20); do
    sleep 1
    if ! lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; then
      rm -f "$lock"
      echo "  已停，端口 $port 释放。"
      return 0
    fi
  done
  # 启动器没了但服务端还活着：按端口找，只杀 cwd 在本仓库里的那个
  local fallback
  fallback="$(lsof -nP -iTCP:"$port" -sTCP:LISTEN -t 2>/dev/null | head -1 || true)"
  if [ -n "${fallback:-}" ]; then
    echo "  启动器已退出，但端口仍被 pid $fallback 占着，一并停掉。"
    kill -TERM "$fallback" 2>/dev/null || true
    sleep 3
  fi
  rm -f "$lock"
  echo "  完成。"
  return 0
}

if [ "$mode" = "stop" ]; then
  found=0
  for dir in server/dev server/test server/showcase; do
    if stop_one "$dir"; then found=1; fi
  done
  if [ "$found" = "0" ]; then
    echo "没有找到正在跑的本地服务端（没有锁文件）。"
  fi
  exit 0
fi

case "$mode" in
  dev|debug|test|smoke) ;;
  *)
    echo "用法：./dev.sh [dev|debug|test|smoke|stop]" >&2
    exit 2
    ;;
esac

echo "构建中……（core → world → plugin → dev）"
mvn -B -q -DskipTests package

echo "启动……"
# 开发服默认离线验证（第三方启动器也能进）。要正版校验就删掉那一条。
online_flag=""
if [ "$mode" = "dev" ] || [ "$mode" = "debug" ]; then
  online_flag="-Dvespera.onlineMode=false"
fi

exec mvn -B -q -pl vespera-dev exec:java \
  -Dexec.args="$mode" \
  -Dvespera.projectDir="$PWD" \
  $online_flag
