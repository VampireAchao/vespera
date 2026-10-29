package io.github.vampireachao.vespera.core.world;

import io.github.vampireachao.vespera.core.voxel.Block;
import io.github.vampireachao.vespera.core.voxel.Palette;

/**
 * 街上的大件：过街楼、石拱、饮水台、树池、抬高的平台、晾衣绳、转角塔。
 *
 * <p>{@code streetProps} 管的是零到两格高的小物——桶、柴、车、槽、牌。它们解决
 * "街上空"，但解决不了"一眼看穿"：站在街口顺着八格宽的路望出去，三百格之外
 * 还是那条路。走起来就没有"下一段"这回事。
 *
 * <p>所以这里补的是四到十二格的东西，作用只有一个：<b>把路切成一段一段</b>。
 * 穿过去（过街楼、石拱）、绕过去（转角塔、树池）、爬上去（平台）、抬头看（晾衣绳）——
 * 每走二十几格就有一件事发生，路才有节奏。
 *
 * <p>铁律：<b>不挡路</b>。所有构件要么在头顶（y ≥ 4），要么贴着街沿一两格，
 * 中间那几格必须留空。{@code CirculationTest} 会走一遍，挡了就红。
 */
final class StreetLife {

    private StreetLife() {
    }

    private static final int K_BRIDGE = 0;
    private static final int K_ARCH = 1;
    private static final int K_TROUGH = 2;
    private static final int K_PIT = 3;
    private static final int K_PLATFORM = 4;
    private static final int K_LAUNDRY = 5;
    private static final int K_TURRET = 6;

    /**
     * 派发表：一条街从头到尾按这张表轮着来。
     *
     * <p>不用随机数是有原因的——随机会连着抽中同一件，或者整条街上一次过街楼都没有。
     * 轮转保证"走一条街能见全"，再用街号错开起点，免得每条街都是同一个顺序。
     */
    private static final int[] MIX = {
            K_BRIDGE, K_ARCH, K_TROUGH, K_BRIDGE, K_PIT, K_TURRET,
            K_PLATFORM, K_BRIDGE, K_TROUGH, K_LAUNDRY, K_ARCH, K_BRIDGE};

    /** 街上的坐标：{@code t} 沿街，{@code off} 横街（0 是街心，正负是两侧）。 */
    private static int x(boolean horiz, int t, int mid, int off) {
        return horiz ? t : mid + off;
    }

    private static int z(boolean horiz, int t, int mid, int off) {
        return horiz ? mid + off : t;
    }

    static void furnish(BuildKit k) {
        int streetIndex = 0;
        for (MedievalCity.Street st : MedievalCity.STREETS) {
            int ax = Math.min(st.x1(), st.x2());
            int bx = Math.max(st.x1(), st.x2());
            int az = Math.min(st.z1(), st.z2());
            int bz = Math.max(st.z1(), st.z2());
            boolean horiz = (bx - ax) >= (bz - az);
            int from = horiz ? ax : az;
            int to = horiz ? bx : bz;
            int mid = horiz ? (az + bz) / 2 : (ax + bx) / 2;
            int half = (horiz ? (bz - az) : (bx - ax)) / 2;
            if (half < 3) {
                continue;
            }
            int side = 1;
            int seg = 0;
            for (int t = from + 18; t <= to - 18; t += 26) {
                side = -side;
                int off = side * half;
                int kind = MIX[Math.floorMod(seg++ + streetIndex * 5, MIX.length)];
                switch (kind) {
                    case K_BRIDGE -> bridge(k, horiz, t, mid, half, ax, bx, az, bz);
                    case K_ARCH -> arch(k, horiz, t, mid, off, side);
                    case K_TROUGH -> trough(k, horiz, t, mid, off);
                    case K_PIT -> pit(k, horiz, t, mid, off);
                    case K_PLATFORM -> platform(k, horiz, t, mid, off, side);
                    case K_LAUNDRY -> laundry(k, horiz, t, mid, half);
                    default -> turret(k, horiz, t, mid, off, side);
                }
            }
            streetIndex++;
        }
        portico(k);
    }

    /**
     * 过街楼：两栋房子之间架一间屋子，人从底下穿过去。
     *
     * <p>这是最管用的一件——它把一条街从"一条线"剪成"两段"，走过去像过了一道门。
     * 落地端必须压在房子的墙上；两头的墙都不在，就不架，免得悬在半空。
     */
    private static void bridge(BuildKit k, boolean horiz, int t, int mid, int half,
            int ax, int bx, int az, int bz) {
        // 两侧的临街墙并不在同一个距离上（南边贴路沿，北边留两格人行道），
        // 所以不按"街宽 ± 一格"去猜，而是从街沿往外找第一道真墙。
        int lo = landing(k, horiz, t, mid, half, -1);
        int hi = landing(k, horiz, t, mid, half, 1);
        if (lo == 0 || hi == 0) {
            return;
        }
        for (int off = lo; off <= hi; off++) {
            boolean end = off == lo || off == hi;
            for (int d = 0; d <= 2; d++) {
                int px = x(horiz, t + d, mid, off);
                int pz = z(horiz, t + d, mid, off);
                k.set(px, 6, pz, Palette.DARK_PLANK);
                k.set(px, 7, pz, end ? Palette.PLASTER_CREAM : (Block) null);
                k.set(px, 8, pz, end ? Palette.PLASTER_CREAM : (Block) null);
                k.set(px, 9, pz, end ? Palette.STONE_BRICK : (Block) Palette.TILE);
            }
        }
        // 檐下一盏灯，人从底下过能看见
        int cx = x(horiz, t + 1, mid, mid);
        int cz = z(horiz, t + 1, mid, mid);
        k.set(cx, 5, cz, Palette.LANTERN_HANGING);
        // 朝街的两面各开一扇窗
        for (int o = mid - 1; o <= mid + 1; o += 2) {
            int px = x(horiz, t, mid, o);
            int pz = z(horiz, t, mid, o);
            k.set(px, 8, pz, Palette.GLASS_GRAY);
        }
        k.bump("过街楼");
    }


    /**
     * 骑楼：沿主街两侧加一道柱廊，把街沿那一条变成有顶的走道。
     *
     * <p>这是整座城最划算的一笔。宏观上，环路与四条大道从此有了一圈"连续的建筑带"——
     * 原来那是两排参差的房子，从空中看是碎边；有了廊顶，边就连续了。街面上，
     * 人走在柱子与墙之间，头顶有顶、侧边有柱，光被切成一条一条——这是"在城里走"
     * 与"在空地上走"最直接的区别。
     *
     * <p>柱子只占街沿一格、每三格一根，路照走。
     */
    static void portico(BuildKit k) {
        for (MedievalCity.Street st : MedievalCity.STREETS) {
            if (!st.main()) {
                continue;
            }
            int ax = Math.min(st.x1(), st.x2());
            int bx = Math.max(st.x1(), st.x2());
            int az = Math.min(st.z1(), st.z2());
            int bz = Math.max(st.z1(), st.z2());
            boolean horiz = (bx - ax) >= (bz - az);
            int from = horiz ? ax : az;
            int to = horiz ? bx : bz;
            int mid = horiz ? (az + bz) / 2 : (ax + bx) / 2;
            int half = (horiz ? (bz - az) : (bx - ax)) / 2;
            if (half < 3) {
                continue;
            }
            for (int side = -1; side <= 1; side += 2) {
                int edge = side * half;
                for (int t = from + 2; t <= to - 2; t++) {
                    int px = x(horiz, t, mid, edge);
                    int pz = z(horiz, t, mid, edge);
                    if (k.scene.get(px, 2, pz) != null && k.scene.get(px, 0, pz) != null) {
                        continue;
                    }
                    int wall = landing(k, horiz, t, mid, half, side);
                    if (wall == 0) {
                        continue;
                    }
                    // 柱子在门口就是一根挡路的桩子——玩家推门出来迎面撞柱子
                    boolean post = Math.floorMod(t, 3) == 0 && !k.doorFront(px, pz);
                    if (post) {
                        for (int y = 0; y <= 3; y++) {
                            k.set(px, y, pz, y == 3 ? Palette.STONE_CHISELED : Palette.SPRUCE_LOG);
                        }
                    }
                    // 廊顶：从街沿一直铺到墙上
                    int o = edge;
                    while (o != wall) {
                        int qx = x(horiz, t, mid, o);
                        int qz = z(horiz, t, mid, o);
                        k.set(qx, 4, qz, Palette.DARK_PLANK);
                        o += side;
                    }
                    int qx = x(horiz, t, mid, wall);
                    int qz = z(horiz, t, mid, wall);
                    k.set(qx, 4, qz, Palette.DARK_PLANK);
                }
            }
            k.bump("骑楼");
        }
    }

    /** 从街沿往外找第一道墙（三列里有两列实心就算），返回它的偏移。 */
    private static int landing(BuildKit k, boolean horiz, int t, int mid, int half, int side) {
        for (int d = 1; d <= 6; d++) {
            int off = side * (half + d);
            int solid = 0;
            for (int i = 0; i <= 2; i++) {
                int px = x(horiz, t + i, mid, off);
                int pz = z(horiz, t + i, mid, off);
                if (k.scene.get(px, 2, pz) != null) {
                    solid++;
                }
            }
            if (solid >= 2) {
                return off;
            }
        }
        return 0;
    }

    /**
     * 石拱：一道一尺厚的拱门横在街上，两根墩子贴着街沿。
     *
     * <p>比过街楼轻，不依赖房子，所以哪儿都能立；它做的也是"剪断视线"这件事。
     */
    private static void arch(BuildKit k, boolean horiz, int t, int mid, int off, int side) {
        int pier = mid + off - side;
        for (int y = 0; y <= 5; y++) {
            int px = x(horiz, t, mid, pier);
            int pz = z(horiz, t, mid, pier);
            k.set(px, y, pz, y % 3 == 0 ? Palette.STONE_CHISELED : Palette.STONE_BRICK);
        }
        int opposite = mid - off + side;
        for (int y = 0; y <= 5; y++) {
            int px = x(horiz, t, mid, opposite);
            int pz = z(horiz, t, mid, opposite);
            k.set(px, y, pz, y % 3 == 0 ? Palette.STONE_CHISELED : Palette.STONE_BRICK);
        }
        for (int y = 6; y <= 7; y++) {
            for (int o = mid - Math.abs(off) - 1; o <= mid + Math.abs(off) + 1; o++) {
                int px = x(horiz, t, mid, o);
                int pz = z(horiz, t, mid, o);
                k.set(px, y, pz, y == 7 ? Palette.STONE_CHISELED : Palette.STONE_BRICK);
            }
        }
        // 拱心石
        int px = x(horiz, t, mid, mid);
        int pz = z(horiz, t, mid, mid);
        k.set(px, 6, pz, Palette.GOLD);
        k.set(px, 5, pz, Palette.STONE_CHISELED);
        k.bump("石拱");
    }

    /** 饮水台：三口石槽，中间盛水，一头一根出水石柱。 */
    private static void trough(BuildKit k, boolean horiz, int t, int mid, int off) {
        for (int i = -1; i <= 1; i++) {
            int px = x(horiz, t + i, mid, off);
            int pz = z(horiz, t + i, mid, off);
            if (k.taken(px, pz, px, pz)) {
                return;
            }
        }
        for (int i = -1; i <= 1; i++) {
            int px = x(horiz, t + i, mid, off);
            int pz = z(horiz, t + i, mid, off);
            k.set(px, 0, pz, i == 0 ? Palette.WATER : Palette.STONE_CHISELED);
            k.set(px, -1, pz, Palette.STONE_BRICK);
        }
        int px = x(horiz, t + 2, mid, off);
        int pz = z(horiz, t + 2, mid, off);
        k.fill(px, 0, pz, px, 1, pz, Palette.STONE_CHISELED);
        k.set(px, 2, pz, Palette.LANTERN);
        k.bump("饮水台");
    }

    /** 树池：两格见方的石框，一棵树。街道上一格绿，比十盏灯管用。 */
    private static void pit(BuildKit k, boolean horiz, int t, int mid, int off) {
        int cx = x(horiz, t, mid, off);
        int cz = z(horiz, t, mid, off);
        int ex = x(horiz, t + 1, mid, off);
        int ez = z(horiz, t + 1, mid, off);
        if (k.taken(cx, cz, ex, ez)) {
            return;
        }
        // 树池沿街排两格，不进街心：占一格街沿，人来人往绕着走
        k.set(cx, -1, cz, Palette.GRASS);
        k.set(ex, -1, ez, Palette.GRASS);
        k.set(cx, 0, cz, Palette.COBBLE);
        k.set(ex, 0, ez, Palette.COBBLE);
        k.tree(cx, cz, 5 + k.rnd.nextInt(3), k.rnd.nextBoolean());
        k.bump("树池");
    }

    /**
     * 街角的抬高平台：两级，边上两级台阶。
     *
     * <p>整座城都是平的，走一天脚不离地。这种一米高的台子什么都不用做，
     * 只是让人"想上去看一眼"——第一人称里，高度变化是最便宜的那种有趣。
     */
    private static void platform(BuildKit k, boolean horiz, int t, int mid, int off, int side) {
        int p0 = mid + off - side;
        for (int i = -2; i <= 2; i++) {
            for (int o = p0; o != p0 + side * 3; o += side) {
                int px = x(horiz, t + i, mid, o);
                int pz = z(horiz, t + i, mid, o);
                if (k.taken(px, pz, px, pz)) {
                    return;
                }
            }
        }
        for (int i = -2; i <= 2; i++) {
            for (int o = p0; o != p0 + side * 3; o += side) {
                int px = x(horiz, t + i, mid, o);
                int pz = z(horiz, t + i, mid, o);
                k.set(px, 0, pz, Palette.STONE_BRICK);
                k.set(px, -1, pz, Palette.STONE_CHISELED);
            }
        }
        // 上台的两级：开在街心那一侧，人从这里迈上去
        String face = horiz ? (side > 0 ? "south" : "north") : (side > 0 ? "east" : "west");
        for (int i = -1; i <= 1; i++) {
            int px = x(horiz, t + i, mid, p0 - side);
            int pz = z(horiz, t + i, mid, p0 - side);
            k.set(px, 0, pz, Block.stairs("minecraft:stone_brick_stairs", face));
        }
        // 台上：一张长凳、一盏灯
        int bx = x(horiz, t, mid, p0 + side);
        int bz = z(horiz, t, mid, p0 + side);
        k.set(bx, 1, bz, Palette.OAK_PLANK);
        int b2x = x(horiz, t + 1, mid, p0 + side);
        int b2z = z(horiz, t + 1, mid, p0 + side);
        k.set(b2x, 1, b2z, Palette.OAK_PLANK);
        int lx = x(horiz, t + 2, mid, p0 + side * 2);
        int lz = z(horiz, t + 2, mid, p0 + side * 2);
        k.set(lx, 1, lz, Palette.LANTERN);
        k.bump("街台");
    }

    /** 晾衣绳：两墙之间一根绳，四格高，挂几片布。抬头才看得见，抬头才像有人住。 */
    private static void laundry(BuildKit k, boolean horiz, int t, int mid, int half) {
        for (int off = mid - half; off <= mid + half; off++) {
            int px = x(horiz, t, mid, off);
            int pz = z(horiz, t, mid, off);
            k.set(px, 4, pz, Palette.SPRUCE_FENCE);
            if (Math.floorMod(off + t, 3) == 0) {
                k.set(px, 3, pz, Math.floorMod(off + t, 2) == 0 ? Palette.WOOL_CREAM : Palette.WOOL_RED);
            }
        }
        k.bump("晾衣绳");
    }

    /**
     * 转角塔：从街角那栋房子的墙角长出来的小塔。
     *
     * <p>不做新房子，只借用已经站在那儿的墙角——城里的塔本来就多半是"加建"出来的。
     */
    private static void turret(BuildKit k, boolean horiz, int t, int mid, int off, int side) {
        int half = Math.abs(off);
        // 塔心要落在临街墙上；两侧的墙不在同一个距离上，所以照样往外找
        int moff = landing(k, horiz, t, mid, half, side);
        if (moff == 0) {
            return;
        }
        int px = x(horiz, t, mid, moff);
        int pz = z(horiz, t, mid, moff);
        int h = 10 + k.rnd.nextInt(4);
        for (int y = 0; y <= h; y++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) == 2) {
                        continue;
                    }
                    k.set(px + dx, y, pz + dz,
                            y % 7 == 0 ? Palette.STONE_CHISELED : Palette.STONE_BRICK);
                }
            }
        }
        k.cone(px, pz, 2, h + 1, 3, Palette.SLATE);
        k.bump("转角塔");
    }
}
