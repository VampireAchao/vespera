#!/usr/bin/env bash
#
# 维斯佩拉 · 出部署包
#
#   ./deploy.sh              产出 dist/（插件 jar + 地图资源 + 合规文件）
#   ./deploy.sh /srv/vespera 直接放进目标目录的 plugins/ 与 vespera/
#
# 它和 dev.sh 的关系只有一条：**互不接触。**
#   - 不读写 server/dev/ 与 server/test/（那是本地调试用的）
#   - 不启动、不停止任何服务器
#   - 只从构建暂存区 server/build/plugins/ 取产物
#
# 正式服的 server.properties、白名单、世界文件都不在这个包里——那些属于那台机器，
# 不该被一次部署覆盖。这个包只回答一个问题："插件和地图资源，该是什么版本。"

set -euo pipefail
cd "$(dirname "$0")"

target="${1:-dist}"

echo "打包……"
mvn -B -q -DskipTests package

echo "组装 $target ……"
mkdir -p "$target"
# 只清自己产出的东西，绝不 rm -rf 一个可能是正式服的目录
rm -rf "$target/plugins" "$target/vespera"
mkdir -p "$target/plugins" "$target/vespera"

cp server/build/plugins/vespera-plugin.jar "$target/plugins/"
cp -R vespera-world/target/world-assets/. "$target/vespera/"
cp NOTICE LICENSE "$target/"

cat > "$target/放置说明.txt" <<'TXT'
维斯佩拉 · 部署包
=================

把这两样放到服务器根目录（覆盖同名目录即可）：

  plugins/vespera-plugin.jar   →  服务器/plugins/
  vespera/                     →  服务器/vespera/

不要覆盖的：

  server.properties   白名单、正版验证、端口属于那台机器，由运维决定
  world/              世界文件与玩家数据，绝不随包分发
  plugins/ 里其它 jar 第三方插件（mmSupernaturals、Citizens……）各自独立升级

启动前确认：

  1. Java 25，Paper 26.3
  2. 服务器根目录下存在 vespera/regions/regions.yml（本包已带）
  3. 启动后在控制台敲  vespera doctor
     —— 版本、软依赖、区域、闸门、剧本，一次看全

合规提醒：

  本包不含任何 Minecraft 游戏文件、服务端核心、第三方插件或素材。
  分发本包不等于获得 Minecraft 的运行授权；每个运行者需要自己持有正版。
TXT

echo
echo "完成：$target/"
find "$target" -maxdepth 2 -type f | sed 's|^|  |'
