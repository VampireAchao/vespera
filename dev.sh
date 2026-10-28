#!/usr/bin/env bash
#
# 维斯佩拉 · 本地启动器（只管开发和测试，不管部署）
#
#   ./dev.sh          开发服：server/dev/，端口 25565，世界保留
#   ./dev.sh debug    开发服 + 挂调试器（服务端 JVM 开 5005）
#   ./dev.sh test     测试服：server/test/，端口 25599，世界保留
#   ./dev.sh smoke    验收：server/test/，每次重建世界，起→验→退，退出码即结论
#
# 三套环境是分开的：开发服、测试服各有一个目录和一个端口，谁也不动谁。
# 部署是另一件事，见 deploy.sh —— 那个脚本不碰这两个目录。

set -euo pipefail
cd "$(dirname "$0")"

mode="${1:-dev}"

case "$mode" in
  dev|debug|test|smoke) ;;
  *)
    echo "用法：./dev.sh [dev|debug|test|smoke]" >&2
    exit 2
    ;;
esac

echo "构建中……（core → world → plugin → dev）"
mvn -B -q -DskipTests package

echo "启动……"
exec mvn -B -q -pl vespera-dev exec:java \
  -Dexec.args="$mode" \
  -Dvespera.projectDir="$PWD"
