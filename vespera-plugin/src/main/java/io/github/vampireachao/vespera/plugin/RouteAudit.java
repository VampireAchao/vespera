package io.github.vampireachao.vespera.plugin;

import io.github.vampireachao.vespera.core.world.TempleSpots;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * 在<b>真实世界</b>里走一遍，看那条路到底通不通。
 *
 * <p>为什么还要有这个东西：core 里的 {@code WalkAudit} 走的是 {@code Scene}——
 * 那是"我们打算砌的东西"。世界不等于场景：地形塑形会填、{@code fillGaps} 会补、
 * 上一版留下的方块还在、落地器跳过草地层。所以"图上能走"和"进服能走"是两件事，
 * 而玩家只会遇到后者。
 *
 * <p>规则和 WalkAudit 完全一致，否则两边的结论没法对照：
 * 脚下实心、身上两格空、一次最多抬一格、可以往下落四格、门算可通行。
 *
 * <p>它只在建城结束时跑一次，把结果打进日志。这不是给玩家看的，
 * 是给"这一版到底能不能走"一个当场可查的答案。
 */
public final class RouteAudit {

    /** 走路时需要空的格子（门算空：右键就开）。 */
    private static boolean open(Block block) {
        if (block.isEmpty() || block.isLiquid()) {
            return true;
        }
        return block.getType().name().endsWith("_DOOR");
    }

    private boolean stand(World world, int x, int y, int z) {
        if (y <= world.getMinHeight() + 1 || y >= world.getMaxHeight() - 2) {
            return false;
        }
        Block floor = world.getBlockAt(x, y - 1, z);
        if (floor.isEmpty() || !standable(floor)) {
            return false;
        }
        return open(world.getBlockAt(x, y, z)) && open(world.getBlockAt(x, y + 1, z));
    }

    /**
     * 从起点走一遍，返回走得到的那些点。
     *
     * @param stops 每个点是 {@code {x, y, z, 标签}}；y 传 {@code Integer.MIN_VALUE}
     *              表示"这一列随便哪一层踩得到都算"
     */
    public List<String> reach(World world, int[] start, List<Object[]> stops,
            int minX, int minZ, int maxX, int maxZ) {
        int sx = start[0];
        int sy = start[1];
        int sz = start[2];
        int minY = world.getMinHeight() + 1;
        int maxY = Math.min(world.getMaxHeight() - 3, sy + 120);

        java.util.Set<Long> seen = new java.util.HashSet<>();
        Queue<int[]> queue = new ArrayDeque<>();
        // 起点先就位：写死的高度只是"大概在这一带"，路面比场地基面高或低一格都正常
        int snapped = Integer.MIN_VALUE;
        for (int dy = 0; dy <= 4 && snapped == Integer.MIN_VALUE; dy++) {
            if (stand(world, sx, sy + dy, sz)) {
                snapped = sy + dy;
            } else if (stand(world, sx, sy - dy, sz)) {
                snapped = sy - dy;
            }
        }
        if (snapped == Integer.MIN_VALUE) {
            List<String> bad = new ArrayList<>();
            bad.add("起点站不住 (" + sx + "," + sy + "," + sz + ")");
            return bad;
        }
        sy = snapped;
        queue.add(new int[] {sx, sy, sz});
        seen.add(key(sx, sy, sz));

        int[] dx = {1, -1, 0, 0};
        int[] dz = {0, 0, 1, -1};
        while (!queue.isEmpty()) {
            int[] p = queue.poll();
            int x = p[0];
            int y = p[1];
            int z = p[2];
            for (int d = 0; d < 4; d++) {
                int nx = x + dx[d];
                int nz = z + dz[d];
                if (nx < minX || nx > maxX || nz < minZ || nz > maxZ) {
                    continue;
                }
                // 同层
                if (stand(world, nx, y, nz) && seen.add(key(nx, y, nz))) {
                    queue.add(new int[] {nx, y, nz});
                }
                // 上一级：要头顶再空一格
                if (stand(world, nx, y + 1, nz) && open(world.getBlockAt(nx, y + 2, nz))
                        && seen.add(key(nx, y + 1, nz))) {
                    queue.add(new int[] {nx, y + 1, nz});
                }
                // 往下落，最多四格
                for (int drop = 1; drop <= 4; drop++) {
                    int ny = y - drop;
                    if (ny < minY || ny > maxY) {
                        break;
                    }
                    if (stand(world, nx, ny, nz) && seen.add(key(nx, ny, nz))) {
                        queue.add(new int[] {nx, ny, nz});
                        break;
                    }
                    if (!open(world.getBlockAt(nx, ny, nz))) {
                        break;
                    }
                }
            }
        }

        List<String> bad = new ArrayList<>();
        for (Object[] stop : stops) {
            int x = (Integer) stop[0];
            int y = (Integer) stop[1];
            int z = (Integer) stop[2];
            String label = (String) stop[3];
            boolean ok = false;
            if (y == Integer.MIN_VALUE) {
                for (int yy = minY; yy <= maxY && !ok; yy++) {
                    ok = seen.contains(key(x, yy, z));
                }
            } else {
                // 允许上下两格：写死的点是"这一带"，不是"这一格"。
                // 台面高一格、地毯不实心，都会让严格比对冤枉一条好路。
                for (int dy = 0; dy <= 2 && !ok; dy++) {
                    ok = seen.contains(key(x, y + dy, z)) || seen.contains(key(x, y - dy, z));
                }
            }
            if (!ok) {
                bad.add(label + " (" + x + "," + y + "," + z + ")" + section(world, x, y, z));
            }
        }
        return bad;
    }

    /**
     * 这一块能不能踩。地毯、半砖、楼梯都不是"实心"，但人确实站得上去。
     *
     * <p>只认 {@code isSolid()} 会把顶坛的红毯判成空——那是全城最重要的一个位置。
     */
    private static boolean standable(Block block) {
        if (block.getType().isSolid()) {
            return true;
        }
        String id = block.getType().name();
        return id.contains("CARPET") || id.contains("SLAB") || id.contains("STAIRS")
                || id.contains("PATH") || id.contains("SNOW");
    }

    /** 走不到时把断面打出来：脚下是什么、身上是什么、头上是什么。 */
    private static String section(World world, int x, int y, int z) {
        return "｜脚=" + name(world, x, y - 1, z)
                + " 身=" + name(world, x, y, z)
                + " 头=" + name(world, x, y + 1, z);
    }

    private static String name(World world, int x, int y, int z) {
        Block b = world.getBlockAt(x, y, z);
        String id = b.getType().name().toLowerCase(java.util.Locale.ROOT);
        return id.replace("minecraft:", "") + (b.getType().isSolid() ? "" : "(非实心)");
    }

    private static long key(int x, int y, int z) {
        return ((long) (x + 8192) << 26) | ((long) (z + 8192) << 11) | (y + 256);
    }

    /**
     * 从南门内出发，把主城里该走到的十几处逐个点名。
     *
     * @return 走不到的那几处；全通返回空表
     */
    public List<String> audit(World world, int ox, int oz, int ground) {
        List<Object[]> stops = new ArrayList<>();
        // 主路
        stops.add(new Object[] {ox + 0, ground + 0, oz + 240, "主街中段"});
        stops.add(new Object[] {ox + 0, ground, oz - 130, "圣座南广场"});
        stops.add(new Object[] {ox + 72, ground, oz, "圣座东门外"});
        // 塔内：每层梯口
        stops.add(new Object[] {ox + 0, ground, oz + 30, "底层大厅"});
        for (int t = 1; t <= 6; t++) {
            stops.add(new Object[] {ox - 10, ground + t * 8 + 1, oz + 62 - t * 8,
                    "第 " + t + " 层梯口"});
        }
        // 地牢
        stops.add(new Object[] {ox - 25, ground - 8, oz + 23, "地牢梯底"});
        stops.add(new Object[] {ox - 25, ground - 8, oz + 5, "地牢中间"});
        // 五处服务点
        for (TempleSpots.Post post : TempleSpots.POSTS) {
            stops.add(new Object[] {ox + post.x(), ground + post.y(), oz + post.z(),
                    post.label()});
        }
        // 顶坛与王座厅
        stops.add(new Object[] {ox + 0, ground + 57, oz + 8, "顶坛祭坛前"});
        stops.add(new Object[] {ox + 0, ground + 12, oz - 262, "王座厅门内"});
        // 盒子要盖住"南门内 → 王座厅"整段，否则走不到的地方会被误判成走不通
        return reach(world, new int[] {ox, ground, oz + 440}, stops,
                ox - 90, oz - 320, ox + 90, oz + 460);
    }
}
