#!/usr/bin/env bash
#
# 维斯佩拉 · 一条命令的入口。
#
#   ./dev.sh          起开发服务器（第一次会下 Paper，约 50 MB）
#   ./dev.sh smoke    起、验、退；退出码 0 表示插件真的加载并答了话
#   ./dev.sh debug    起服务器并挂上 JDWP，插件可以打断点
#
# 它只做两件事：先构建，再把参数交给 DevServer。真正的逻辑在
# vespera-dev/src/main/java/.../DevServer.java 里——在版本库里，不在 IDE 里。

set -euo pipefail
cd "$(dirname "$0")"

mode="${1:-dev}"

case "$mode" in
  dev|smoke|debug) ;;
  *)
    echo "用法：./dev.sh [dev|smoke|debug]" >&2
    exit 2
    ;;
esac

echo "构建中……（core → world → plugin → dev）"
mvn -B -q -DskipTests package

echo "启动……"
exec mvn -B -q -pl vespera-dev exec:java -Dexec.args="$mode"
