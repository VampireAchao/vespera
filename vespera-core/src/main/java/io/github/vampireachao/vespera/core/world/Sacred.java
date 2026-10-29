package io.github.vampireachao.vespera.core.world;

import io.github.vampireachao.vespera.core.voxel.Block;
import io.github.vampireachao.vespera.core.voxel.Palette;

/**
 * 城心三件事：圣座金字塔、王座大厅、角斗场。
 *
 * <p>它们不是三栋"大房子"，是三种权力摆在一起的样子：
 * 教会把祭坛放在最高处，王把宝座放在最北端，民众在椭圆形的沙地上看人被放逐。
 * 三者的位置关系，就是这座城对"谁说了算"的回答。
 */
final class Sacred {

    private Sacred() {
    }

    // -- 圣座：阶梯金字塔 ---------------------------------------------------

    private static final int BASE = 66;
    private static final int TIERS = 7;
    private static final int TIER_H = 8;
    private static final int INSET = 8;
    private static final int SHELL = 3;

    /**
     * 七层阶梯金字塔：底边 133，顶面 37×37，高 63。
     *
     * <p>为什么是金字塔而不是尖塔：这里的教会不靠"向上够天"表达权威，
     * 它靠"一级一级走上去"——每上一层，能上去的人少一层，到顶只剩祭司。
     * 建筑把等级写进了台阶数。
     */
    static void pyramid(BuildKit k) {
        // 基座与巡礼道
        k.pave(-BASE - 6, -BASE - 6, BASE + 6, BASE + 6, Palette.SANDSTONE, Palette.STONE_BRICK);
        for (int x = -BASE - 5; x <= BASE + 5; x++) {
            for (int z = -BASE - 5; z <= BASE + 5; z++) {
                int d = Math.max(Math.abs(x), Math.abs(z));
                if (d > BASE && d <= BASE + 5) {
                    k.set(x, -1, z, Palette.STONE_BRICK);
                    k.set(x, 0, z, Palette.STONE_CHISELED);
                }
            }
        }

        for (int t = 0; t < TIERS; t++) {
            int r = BASE - t * INSET;
            int y0 = t * TIER_H;
            int next = t + 1 < TIERS ? r - INSET : 0;
            tier(k, r, y0, next, t);
        }

        // 基座前的水池：台阶两侧的水一路落下来，总得有个地方接住
        for (int x = -9; x <= 9; x++) {
            for (int z = BASE + 6; z <= BASE + 10; z++) {
                boolean rim = Math.abs(x) == 9 || z == BASE + 10;
                k.set(x, -1, z, rim ? Palette.STONE_CHISELED : Palette.WATER);
            }
        }

        clergyRooms(k, BASE);
        altar(k);
        k.bump("圣座金字塔");
        k.bump("地标");
    }

    /**
     * 一层塔身：壳体、壁柱、露台、大台阶。
     *
     * <p>这一版是按"空中花园"那类作品的路子重排的：每层都有看得见的基脚与压顶、
     * 壁柱按四格一根的节奏、露台外沿用半砖压顶（能看出去、也能看出层次）、
     * 种植从栏板溢出到下层、水落到该落的地方。
     *
     * <p>台阶原来只有八格深却要爬九格高，多出来的一格只能凿穿露台——于是每爬一层
     * 都会掉进一个三格深的坑，顶端还顶在上一层墙上。现在层高改成八，正好一格一阶。
     */
    private static void tier(BuildKit k, int r, int y0, int next, int index) {
        // 1) 壳体：三层厚。基脚一圈凿制石，檐口一圈砂岩
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                int d = Math.max(Math.abs(x), Math.abs(z));
                if (d <= r - SHELL) {
                    continue;
                }
                for (int y = y0; y < y0 + TIER_H; y++) {
                    k.set(x, y, z, y == y0 ? Palette.STONE_CHISELED : Palette.SANDSTONE);
                }
            }
        }

        // 2) 壁柱：每四格一根，柱头换凿制石。节奏比花纹更能让人读出"这是一栋建筑"
        for (int side = 0; side < 4; side++) {
            for (int along = -(r - 2); along <= r - 2; along += 4) {
                int px = side < 2 ? along : (side == 2 ? -r : r);
                int pz = side < 2 ? (side == 0 ? -r : r) : along;
                for (int y = y0 + 1; y < y0 + TIER_H; y++) {
                    k.set(px, y, pz, y == y0 + TIER_H - 1 ? Palette.STONE_CHISELED : Palette.STONE_BRICK);
                }
            }
        }

        int topY = y0 + TIER_H;
        if (next <= 0) {
            return;
        }

        // 3) 露台：外沿半砖压顶 → 两格种植 → 走道 → 内圈种植
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                int d = Math.max(Math.abs(x), Math.abs(z));
                if (d <= next || d > r) {
                    continue;
                }
                if (d == r) {
                    k.set(x, topY, z, Block.slab("minecraft:smooth_sandstone_slab", "bottom"));
                } else if (d >= r - 2 || d <= next + 2) {
                    k.set(x, topY, z, Palette.GRASS);
                    if (Math.floorMod(x * 7 + z * 11, 9) == 0) {
                        k.set(x, topY + 1, z, Math.floorMod(x + z, 3) == 0
                                ? Palette.FLOWER_RED : Palette.FLOWER_YELLOW);
                    } else if (Math.floorMod(x * 3 + z * 5, 7) == 0) {
                        k.set(x, topY + 1, z, Palette.GRASS_TUFT);
                    }
                } else {
                    k.set(x, topY, z, Palette.SANDSTONE);
                }
                // 种植溢出到下层：每六格一挂藤，从压顶垂下去
                if (d == r && Math.floorMod(x * 5 + z * 3, 6) == 0) {
                    k.set(x, topY - 1, z + 1, Palette.VINE);
                    k.set(x, topY - 2, z + 1, Palette.VINE);
                }
            }
        }

        // 3.5) 台地家具：外沿每十二格一个花坛，走道边摆长凳
        for (int along = -r + 6; along <= r - 6; along += 12) {
            for (int[] p : new int[][] {{along, r - 1}, {along, -r + 1}, {r - 1, along}, {-r + 1, along}}) {
                if (Math.abs(p[0]) <= next + 3 || Math.abs(p[1]) <= next + 3) {
                    continue;
                }
                k.set(p[0], topY, p[1], Palette.STONE_BRICK);
                k.set(p[0], topY + 1, p[1], Palette.GRASS);
                k.set(p[0], topY + 2, p[1], Math.floorMod(along, 24) == 0
                        ? Palette.FLOWER_RED : Palette.OAK_LEAVES);
            }
        }
        for (int along = -r + 10; along <= r - 10; along += 16) {
            for (int[] p : new int[][] {{along, r - 4}, {r - 4, along}}) {
                k.set(p[0], topY + 1, p[1], Palette.OAK_PLANK);
                if (Math.abs(p[0]) > next + 2 && Math.abs(p[1]) > next + 2) {
                    k.set(p[0], topY + 1, p[1], Palette.OAK_PLANK);
                }
            }
        }

        // 4) 走道上的灯：每八格一盏，夜里能看出"这里有一圈路"
        for (int along = -r + 4; along <= r - 4; along += 8) {
            int d = r - 4;
            for (int[] p : new int[][] {{along, -d}, {along, d}, {-d, along}, {d, along}}) {
                if (Math.abs(p[0]) <= next + 2 || Math.abs(p[1]) <= next + 2) {
                    continue;
                }
                k.set(p[0], topY + 1, p[1], Palette.LANTERN);
            }
        }

        // 5) 南面大台阶：九格宽，一阶一上，正好走完这八格。
        //    第一级再多铺一级落地台阶——广场比台阶低一格，不铺就得"跳"上去。
        if (index == 0) {
            for (int x = -4; x <= 4; x++) {
                k.set(x, 0, r + TIER_H + 1, Block.stairs("minecraft:sandstone_stairs", "north"));
            }
            for (int x : new int[] {-5, 5}) {
                k.set(x, 0, r + TIER_H + 1, Palette.SANDSTONE);
                k.set(x, 1, r + TIER_H + 1, Palette.STONE_CHISELED);
            }
        }
        for (int step = 0; step < TIER_H; step++) {
            int y = y0 + 1 + step;
            int z = r + TIER_H - step;
            for (int x = -4; x <= 4; x++) {
                k.set(x, y, z, Block.stairs("minecraft:sandstone_stairs", "north"));
                for (int up = 1; up <= 3; up++) {
                    k.set(x, y + up, z, null);
                }
            }
            // 两侧栏墙：一层石、一层凿制，顶端留出净空
            for (int x : new int[] {-5, 5}) {
                k.set(x, y, z, Palette.SANDSTONE);
                k.set(x, y + 1, z, Palette.STONE_CHISELED);
                for (int up = 2; up <= 3; up++) {
                    k.set(x, y + up, z, null);
                }
            }
            // 每两级在栏墙上摆一只火盆
            if (step % 2 == 0) {
                for (int x : new int[] {-5, 5}) {
                    k.set(x, y + 2, z, Palette.CAMPFIRE);
                    k.set(x, y + 3, z, null);
                }
            }
        }
        // 台阶两侧的水槽：从上一层的落水口一直流到底座的水池
        for (int step = 0; step < TIER_H; step++) {
            int y = y0 + step;
            int z = r + TIER_H - step;
            for (int x : new int[] {-6, 6}) {
                k.set(x, y, z, Palette.STONE_CHISELED);
                if (step < TIER_H - 1) {
                    k.set(x, y + 1, z, Palette.WATER);
                }
            }
        }
    }

    /** 底层的一排小室：修士的居所、缮写室、库房。门朝内开。 */
    private static void clergyRooms(BuildKit k, int radius) {
        int inner = radius - SHELL;
        for (int i = 0; i < 6; i++) {
            int x0 = -inner + 9 + i * 18;
            int x1 = x0 + 14;
            int z0 = inner - 6;
            int z1 = inner;
            k.walls(x0, 0, z0, x1, 6, z1, Palette.PLASTER);
            k.carve(x0 + 1, 0, z0 + 1, x1 - 1, 5, z1 - 1);
            k.carve(x0 + 6, 0, z0, x0 + 8, 3, z0);
            k.set(x0 + 2, 0, z1 - 2, Palette.WOOL_CREAM);
            k.set(x0 + 3, 0, z1 - 2, Palette.WOOL_CREAM);
            k.set(x0 + 10, 0, z1 - 2, Palette.LECTERN);
            k.set(x0 + 1, 3, z0 + 2, Palette.LANTERN);
        }
    }

    /** 塔顶祭坛：四面火盆，中间金座，顶上开天窗。 */
    /**
     * 塔顶祭坛：一座四面敞开的亭子，四角墩柱、过梁、铜顶、金顶针。
     *
     * <p>原来是四根独柱撑一块整板再加一摞金块——从远处看就是"一个盒子戴了顶帽子"。
     * 现在改成"墙变成柱、屋顶收成锥"：人站在亭子里能看见四面天，铜顶在一片砂岩色里
     * 是唯一一处冷色，正好把轮廓收住。
     */
    private static void altar(BuildKit k) {
        int y = TIERS * TIER_H;
        int r = 18;

        // 顶面：同心方环铺装，外沿半砖压顶（能看出去，也看得出层次）
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                int d = Math.max(Math.abs(x), Math.abs(z));
                Block block;
                if (d == r) {
                    block = Block.slab("minecraft:smooth_sandstone_slab", "bottom");
                } else if ((d / 3) % 2 == 0) {
                    block = Palette.SANDSTONE;
                } else {
                    block = Palette.STONE_CHISELED;
                }
                k.set(x, y, z, block);
            }
        }

        // 祭坛台：三级，金盘
        k.fill(-6, y + 1, -6, 6, y + 1, 6, Palette.STONE_CHISELED);
        k.fill(-4, y + 2, -4, 4, y + 2, 4, Palette.SANDSTONE);
        k.fill(-2, y + 3, -2, 2, y + 3, 2, Palette.GOLD);
        k.set(0, y + 4, 0, Palette.GOLD);

        // 亭子：四角三乘三的墩柱，柱头贴金；四面过梁
        int pr = 7;
        for (int[] c : new int[][] {{-pr, -pr}, {pr, -pr}, {-pr, pr}, {pr, pr}}) {
            k.fill(c[0] - 1, y + 1, c[1] - 1, c[0] + 1, y + 8, c[1] + 1, Palette.STONE_CHISELED);
            k.set(c[0], y + 9, c[1], Palette.GOLD);
        }
        for (int i = -pr - 1; i <= pr + 1; i++) {
            for (int j : new int[] {-pr - 1, pr + 1}) {
                k.set(i, y + 9, j, Palette.STONE_CHISELED);
                k.set(j, y + 9, i, Palette.STONE_CHISELED);
                k.set(i, y + 10, j, Palette.SANDSTONE);
                k.set(j, y + 10, i, Palette.SANDSTONE);
            }
        }

        // 铜顶：五层收分的方锥，顶上金针。这是全城唯一一处冷色
        for (int layer = 0; layer < 5; layer++) {
            int rr = 9 - layer * 2;
            // 最下一圈还是新铜，往上已经生锈：真实铜顶从来不是一次变绿的
            Block copper = layer == 0 ? Palette.COPPER : Palette.COPPER_OXIDIZED;
            k.fill(-rr, y + 11 + layer, -rr, rr, y + 11 + layer, rr, copper);
        }
        k.fill(0, y + 16, 0, 0, y + 17, 0, Palette.GOLD);

        // 四角火盆
        for (int[] c : new int[][] {{-14, -14}, {14, -14}, {-14, 14}, {14, 14}}) {
            k.fill(c[0], y + 1, c[1], c[0], y + 2, c[1], Palette.STONE_BRICK);
            k.set(c[0], y + 3, c[1], Palette.CAMPFIRE);
        }

        // 两侧的水池：水到了顶上，总得有个地方落
        for (int side = -1; side <= 1; side += 2) {
            for (int x = -12; x <= -8; x++) {
                for (int z = -2; z <= 2; z++) {
                    k.set(x * side, y, z, Palette.WATER);
                }
            }
            k.set(side * 12, y + 1, 0, Palette.STONE_CHISELED);
        }

        // 钟：挂在北侧
        k.fill(-2, y + 6, -14, 2, y + 6, -14, Palette.DARK_LOG);
        k.set(0, y + 5, -14, Palette.BELL);
    }

    static void throneHall(BuildKit k) {
        int[] zSteps = {-150, -192, -238, -298};
        int[] widths = {78, 64, 50};

        // 三级台地：一级比一级窄，一级比一级高
        for (int t = 0; t < 3; t++) {
            int y = t * 5;
            int x1 = -widths[t];
            int x2 = widths[t];
            int z1 = zSteps[t + 1];
            int z2 = zSteps[t];
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    for (int yy = Math.max(-1, y - 5); yy <= y; yy++) {
                        k.set(x, yy, z, yy == y ? Palette.STONE_BRICK : Palette.STONE_CHISELED);
                    }
                }
            }
            // 台面铺装：中央一条深色轴线，每十二格一道横带。整片灰石台面近看就是水泥地
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    if (Math.abs(x) <= 3) {
                        k.set(x, y, z, Palette.STONE_CHISELED);
                    } else if (Math.floorMod(z, 12) == 0) {
                        k.set(x, y, z, Palette.ANDESITE);
                    }
                }
            }

            // 挡土墙：每级一道，压顶带线脚
            for (int x = x1; x <= x2; x++) {
                for (int yy = y - 4; yy <= y; yy++) {
                    k.set(x, yy, z2, yy == y ? Palette.STONE_BRICK : Palette.STONE_CHISELED);
                }
            }
            // 中央大台阶：十六格宽，五级一跑
            for (int step = 1; step <= 5; step++) {
                for (int x = -8; x <= 8; x++) {
                    k.set(x, y - 5 + step, z2 + 6 - step, Palette.STONE_CHISELED);
                }
            }
            // 台地两侧的火盆
            for (int x : new int[] {x1 + 6, x2 - 6}) {
                k.fill(x, y + 1, z2 + 3, x, y + 2, z2 + 3, Palette.STONE_BRICK);
                k.set(x, y + 3, z2 + 3, Palette.CAMPFIRE);
            }
        }

        int base = 15;
        int x1 = -46;
        int x2 = 46;
        int z1 = -288;
        int z2 = -238;
        int top = base + 20;

        k.walls(x1, base + 1, z1, x2, top, z2, Palette.STONE_BRICK);
        k.carve(x1 + 1, base + 1, z1 + 1, x2 - 1, top - 1, z2 - 1);

        // 前廊：八根柱，撑一片出檐的平顶
        for (int i = 0; i < 8; i++) {
            int x = x1 + 6 + i * 11;
            k.fill(x, base + 1, z2, x, top, z2, Palette.STONE_CHISELED);
            k.set(x, top - 1, z2, Palette.GOLD);
        }
        for (int i = 0; i < 8; i++) {
            int x = x1 + 6 + i * 11;
            k.fill(x, top + 1, z2 - 2, x, top + 1, z2 + 6, Palette.STONE_BRICK);
            k.set(x, top + 2, z2 + 6, Palette.STONE_CHISELED);
        }
        k.fill(x1, top + 1, z2 - 2, x2, top + 1, z2 + 6, Palette.STONE_BRICK);
        k.fill(x1, top + 2, z2 - 2, x2, top + 2, z2 + 6, Palette.STONE_CHISELED);

        // 平顶 + 女儿墙
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                k.set(x, top + 1, z, Palette.STONE_BRICK);
            }
        }
        k.crenellate(x1, z1, x2, z2, top + 2, Palette.STONE_BRICK);
        k.crenellate(x1, z1, x2, z2, top + 3, Palette.STONE_BRICK);

        // 屋顶采光带：一道低矮的高侧窗，平屋顶才有起伏，里面也有光落下来
        for (int z = z1 + 12; z <= z2 - 12; z++) {
            for (int x = -8; x <= 8; x++) {
                boolean wall = Math.abs(x) == 8;
                for (int dy = 2; dy <= 4; dy++) {
                    k.set(x, top + dy, z, wall ? Palette.STONE_BRICK : null);
                }
            }
            if (Math.floorMod(z, 6) == 0) {
                k.set(-8, top + 3, z, Palette.GLASS_GRAY);
                k.set(8, top + 3, z, Palette.GLASS_GRAY);
            }
        }
        k.fill(-9, top + 5, z1 + 12, 9, top + 5, z2 - 12, Palette.SLATE);

        // 后部塔楼：王座的"钉子"
        int tx = 0;
        int tz = -272;
        k.fill(-9, base + 1, tz - 9, 9, top + 18, tz + 9, Palette.STONE_BRICK);
        k.carve(-8, base + 1, tz - 8, 8, top + 17, tz + 8);
        k.crenellate(-9, tz - 9, 9, tz + 9, top + 19, Palette.STONE_BRICK);
        // 角墩：光秃秃的方塔立不起来，四角加一道从底到顶的竖线
        for (int[] b : new int[][] {{-9, tz - 9}, {9, tz - 9}, {-9, tz + 9}, {9, tz + 9}}) {
            k.fill(b[0], base + 1, b[1], b[0], top + 18, b[1], Palette.STONE_CHISELED);
        }
        k.cone(tx, tz, 10, top + 20, 16, Palette.SLATE);
        k.set(tx, top + 37, tz, Palette.GOLD);

        // 王座：五级台阶、金座、背屏
        int seatZ = z1 + 10;
        for (int i = 0; i < 5; i++) {
            k.fill(-9 + i, base + 1 + i, seatZ + 8 - i, 9 - i, base + 1 + i, seatZ + 12 - i,
                    Palette.STONE_CHISELED);
        }
        k.fill(-2, base + 6, seatZ, 2, base + 7, seatZ + 2, Palette.GOLD);
        k.fill(-3, base + 8, seatZ, 3, base + 13, seatZ, Palette.STONE_CHISELED);

        // 两侧旗与长桌
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < 3; i++) {
                int x = side * (20 + i * 9);
                k.fill(x, base + 1, z1 + 6, x, base + 12, z1 + 6, Palette.SPRUCE_FENCE);
                k.fill(x, base + 9, z1 + 6, x, base + 11, z1 + 6, Palette.WOOL_RED);
            }
        }
        for (int i = 0; i < 3; i++) {
            int x = -28 + i * 28;
            k.fill(x, base + 1, -252, x + 8, base + 1, -246, Palette.DARK_PLANK);
            k.set(x + 4, base + 2, -249, Palette.LANTERN);
        }

        // 地面：一条红毯直通王座，两侧铺凿制石
        for (int x = x1 + 1; x <= x2 - 1; x++) {
            for (int z = z1 + 1; z <= z2 - 1; z++) {
                k.set(x, base, z, Math.abs(x) <= 3 ? Palette.CARPET_RED : Palette.STONE_CHISELED);
            }
        }

        // 内柱廊：两列，每八格一根，柱础放大、柱头贴金
        for (int x : new int[] {-26, 26}) {
            for (int z = z1 + 10; z <= z2 - 8; z += 8) {
                k.fill(x - 1, base + 1, z - 1, x + 1, base + 1, z + 1, Palette.STONE_CHISELED);
                k.fill(x, base + 2, z, x, top - 2, z, Palette.STONE_BRICK);
                k.set(x, top - 1, z, Palette.GOLD);
            }
        }

        // 天花：方格梁 + 中央一列吊灯。没有天花的大厅从里面看是"露天盒子"
        for (int x = x1 + 1; x <= x2 - 1; x++) {
            for (int z = z1 + 1; z <= z2 - 1; z++) {
                boolean beam = Math.floorMod(x, 8) == 0 || Math.floorMod(z, 8) == 0;
                k.set(x, top, z, beam ? Palette.DARK_PLANK : Palette.STONE_CHISELED);
            }
        }
        for (int z = z1 + 6; z <= z2 - 6; z += 8) {
            k.set(0, top - 1, z, Palette.LANTERN_HANGING);
        }

        // 壁面：每六格一道壁柱，柱间挂旗；东西两侧开高窗透光
        for (int z = z1 + 4; z <= z2 - 4; z += 6) {
            for (int x : new int[] {x1 + 1, x2 - 1}) {
                k.fill(x, base + 1, z, x, top - 2, z, Palette.STONE_CHISELED);
            }
            if (Math.floorMod(z - z1, 12) == 0) {
                k.fill(x1 + 1, base + 5, z, x1 + 1, base + 10, z, Palette.WOOL_RED);
                k.fill(x2 - 1, base + 5, z, x2 - 1, base + 10, z, Palette.WOOL_RED);
            }
        }
        for (int z = z1 + 9; z <= z2 - 9; z += 8) {
            for (int x : new int[] {x1, x2}) {
                k.fill(x, top - 5, z, x, top - 3, z, Palette.GLASS_GRAY);
            }
        }

        // 王座前的红毯与两侧火盆
        for (int z = seatZ + 13; z <= z2 - 6; z++) {
            k.set(0, base, z, Palette.CARPET_RED);
        }
        for (int x : new int[] {-6, 6}) {
            k.fill(x, base + 6, seatZ + 7, x, base + 7, seatZ + 7, Palette.STONE_BRICK);
            k.set(x, base + 8, seatZ + 7, Palette.CAMPFIRE);
        }

        // 外墙：每六格一道外凸壁柱、檐口一圈挑出，正面就不是一块平板了
        for (int z = z1; z <= z2; z += 6) {
            for (int x : new int[] {x1 - 1, x2 + 1}) {
                k.fill(x, base + 1, z, x, top - 1, z, Palette.STONE_CHISELED);
            }
        }
        for (int x = x1 - 1; x <= x2 + 1; x++) {
            k.set(x, top - 1, z1, Palette.STONE_CHISELED);
            k.set(x, top - 1, z2, Palette.STONE_CHISELED);
        }

        // 通往大厅的仪式道：两列雕像与灯
        for (int i = 0; i < 4; i++) {
            for (int side = -1; side <= 1; side += 2) {
                int x = side * 22;
                int z = -156 - i * 12;
                k.fill(x, 0, z, x, 1, z, Palette.STONE_BRICK);
                k.fill(x, 2, z, x, 5, z, Palette.STONE_CHISELED);
                k.set(x, 6, z, Palette.GOLD);
            }
            k.lamp(-14, -156 - i * 12);
            k.lamp(14, -156 - i * 12);
        }
        k.bump("王座大厅");
        k.bump("地标");
    }

    // -- 角斗场：椭圆，四门，八道台阶 ----------------------------------------

    /**
     * 看台的尺寸。原来这三件事各写一遍（看台一套、台阶一套、包厢一套），
     * 改一处忘一处——台阶就是那样一级一级长到看台上方的。
     */
    private static final double ARENA_RX = 82;
    private static final double ARENA_RZ = 70;
    private static final int ARENA_ROWS = 16;
    private static final double ARENA_LIN_IN = 0.55;
    private static final double ARENA_LIN_OUT = 0.90;
    private static final double ARENA_ROW_STEP = (ARENA_LIN_OUT - ARENA_LIN_IN) / ARENA_ROWS;
    /** 沙地边界与护墙带（都以归一化半径的平方 e 计）。 */
    private static final double ARENA_SAND = 0.288;
    private static final double ARENA_PODIUM = 0.3025;
    /** 回廊地面那一层方块的高度；人站在它上面一格。 */
    private static final int ARENA_TOP = 2 + ARENA_ROWS;
    /** 四座门朝向。 */
    private static final int[][] ARENA_GATES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    /** 台阶压在中轴两侧第几格：四门各两道，三格宽。 */
    private static final int ARENA_AISLE = 10;

    private static double eOf(int dx, int dz) {
        double u = dx / ARENA_RX;
        double v = dz / ARENA_RZ;
        return u * u + v * v;
    }

    /**
     * 这一列的地面高度——人脚踩的那一格。
     *
     * <p>沙地 0、护墙 2、第一排 3、往后每排抬一格、回廊 19。看台照着它砌，
     * 台阶照着它走，包厢照着它放。三者问同一个函数，就不会再各说各话。
     */
    static int arenaSurface(double e) {
        if (e <= ARENA_SAND) {
            return 0;
        }
        if (e <= ARENA_PODIUM) {
            return 2;
        }
        double lin = Math.sqrt(e);
        if (lin > ARENA_LIN_OUT) {
            return ARENA_TOP + 1;
        }
        int row = (int) Math.floor((lin - ARENA_LIN_IN) / ARENA_ROW_STEP);
        row = Math.max(0, Math.min(ARENA_ROWS - 1, row));
        return 3 + row;
    }

    /**
     * 罗马式角斗场：椭圆看台、四座门、八道台阶、沙地、地下关押区。
     *
     * <p>它被放进城墙里，是因为它本来就是城里的公共建筑——只不过在这里，
     * 沙地上被放逐的不是战俘，是"不该留在城里的人"。
     */
    static void colosseum(BuildKit k, int cx, int cz) {
        int wallH = 24;

        // 外墙：按角度参数化。
        // 用 dx/dz 包围盒去判椭圆，一定会漏掉四个极点——要求 |dx|>82，
        // 而循环只走到 80。东西两极整段消失，南北各缺 39 格。
        for (double deg = 0; deg < 360; deg += 0.5) {
            double t = Math.toRadians(deg);
            for (double s = 1.0; s <= 1.076; s += 0.025) {
                int x = cx + (int) Math.round(ARENA_RX * Math.sqrt(s) * Math.cos(t));
                int z = cz + (int) Math.round(ARENA_RZ * Math.sqrt(s) * Math.sin(t));
                boolean gate = Math.abs(x - cx) <= 3 || Math.abs(z - cz) <= 3;
                for (int y = 0; y < wallH; y++) {
                    if (gate && y <= 4) {
                        continue;
                    }
                    boolean cornice = y == 7 || y == 15 || y == wallH - 1;
                    Block block = cornice ? Palette.STONE_CHISELED
                            : (((x + z) & 7) == 0 ? Palette.STONE_BRICK : Palette.SANDSTONE);
                    k.set(x, y, z, block);
                }
                if (gate) {
                    // 门洞上方的拱
                    k.set(x, 5, z, Palette.STONE_CHISELED);
                } else if ((x + z) % 5 == 0) {
                    k.set(x, 4, z, Palette.DARK_PLANK);
                    k.set(x, 12, z, Palette.DARK_PLANK);
                    k.set(x, 20, z, Palette.DARK_PLANK);
                }
            }
        }

        // 壁柱：每六格一道，贴着墙外皮往外凸两格。
        // 二十四格高的墙如果只有一层砂岩，从远处看就是一圈灰带——没有竖线，
        // 眼睛量不出它有多高。柱子只加在墙外面，不动墙身，所以不会漏光。
        for (double deg = 0; deg < 360; deg += 0.5) {
            double t = Math.toRadians(deg);
            int x = cx + (int) Math.round(ARENA_RX * Math.cos(t));
            int z = cz + (int) Math.round(ARENA_RZ * Math.sin(t));
            if (Math.abs(x - cx) <= 4 || Math.abs(z - cz) <= 4) {
                continue;
            }
            if (Math.floorMod(x + z, 6) != 0) {
                continue;
            }
            for (double s = 1.0; s <= 1.05; s += 0.02) {
                int px = cx + (int) Math.round(ARENA_RX * Math.sqrt(s) * Math.cos(t));
                int pz = cz + (int) Math.round(ARENA_RZ * Math.sqrt(s) * Math.sin(t));
                for (int y = 0; y <= wallH - 2; y++) {
                    k.set(px, y, pz, y % 8 == 0 ? Palette.STONE_CHISELED : Palette.ANDESITE_POLISHED);
                }
            }
        }

        // 看台：实心台地。
        // 行距 (0.90-0.55)/16，最外一排恰好收在 e=0.81；再往外是回廊，
        // 回廊再往外是墙。旧写法铺到 e=1.0，一点走道都没给看台留。
        for (int dx = -85; dx <= 85; dx++) {
            for (int dz = -73; dz <= 73; dz++) {
                double e = eOf(dx, dz);
                if (e > 1.0) {
                    continue;
                }
                int x = cx + dx;
                int z = cz + dz;
                if (e <= ARENA_SAND) {
                    k.set(x, -1, z, ((dx + dz) & 1) == 0 ? Palette.SAND : Palette.COARSE);
                    continue;
                }
                boolean gate = Math.abs(dx) <= 3 || Math.abs(dz) <= 3;
                if (e <= ARENA_PODIUM) {
                    // 护墙：两格，刚好让第一排能看见沙地。没有它，观众一步就跨进场里
                    if (!gate) {
                        k.set(x, 0, z, Palette.STONE_BRICK);
                        k.set(x, 1, z, Palette.STONE_CHISELED);
                    }
                    continue;
                }
                int surface = arenaSurface(e);
                int top = surface - 1;
                // 门洞位置留成隧道：下五格空，上面回填，从外面看仍是完整的看台
                int from = gate ? 5 : 0;
                for (int y = from; y <= top; y++) {
                    Block block;
                    if (y != top) {
                        block = Palette.STONE_BRICK;
                    } else if (surface == ARENA_TOP + 1) {
                        // 回廊铺成棋盘：一大片单色石板，站上去分不清走道与看台
                        block = Math.floorMod(x + z, 4) == 0
                                ? Palette.ANDESITE_POLISHED : Palette.STONE_BRICK;
                    } else {
                        block = top % 2 == 0 ? Palette.STONE_BRICK : Palette.ANDESITE_POLISHED;
                    }
                    k.set(x, y, z, block);
                }
            }
        }

        // 四门：外门洞底与场外同高，隧道直通沙地。
        // 一直挖到墙外面（e ≤ 1.076）——只挖到 e=1.0 的话，门洞里那段没有地面，
        // 走到门口会掉下去，等于门是假的。
        for (int[] dir : ARENA_GATES) {
            for (int d = -3; d <= 3; d++) {
                for (int step = 0; step <= 90; step++) {
                    int x = dir[0] != 0 ? cx + dir[0] * step : cx + d;
                    int z = dir[0] != 0 ? cz + d : cz + dir[1] * step;
                    double u = (double) (x - cx) / ARENA_RX;
                    double v = (double) (z - cz) / ARENA_RZ;
                    if (u * u + v * v > 1.076) {
                        break;
                    }
                    if (u * u + v * v > ARENA_SAND) {
                        for (int y = 0; y <= 4; y++) {
                            k.set(x, y, z, null);
                        }
                    }
                    k.set(x, -1, z, Palette.COBBLE);
                }
            }
        }

        // 八道台阶：四门两侧各一道，从沙地走上回廊。
        // 每格最多抬一格，踏面就压在原来看台的顶面上——所以它踩得实。
        for (int[] dir : ARENA_GATES) {
            for (int side = -1; side <= 1; side += 2) {
                aisle(k, cx, cz, dir, side);
            }
        }

        // 檐口的桅杆：用来撑遮阳篷
        for (int i = 0; i < 12; i++) {
            double angle = i / 12.0 * Math.PI * 2;
            int x = cx + (int) Math.round(Math.cos(angle) * (ARENA_RX + 2));
            int z = cz + (int) Math.round(Math.sin(angle) * (ARENA_RZ + 2));
            k.fill(x, wallH, z, x, wallH + 5, z, Palette.SPRUCE_FENCE);
        }
        k.bump("角斗场");
        k.bump("地标");
    }

    /**
     * 一道台阶：从沙地沿半径走到回廊，三格宽。
     *
     * <p>每一步的踏面都落在看台的顶面上，只在需要抬高的那一格放一块楼梯。
     * 于是坡度天然等于看台的坡度（约两格一抬），人走上去不用跳。
     *
     * <p>旧版是"每格抬一格、连抬十八格"：走五格就已悬在看台上方三格，
     * 台阶下面全是空的，谁也走不上去。坡陡于看台，是那件事的全部原因。
     */
    private static void aisle(BuildKit k, int cx, int cz, int[] dir, int side) {
        String facing = dir[0] > 0 ? "east" : dir[0] < 0 ? "west" : dir[1] > 0 ? "south" : "north";
        for (int w = -1; w <= 1; w++) {
            int fixedX = dir[0] == 0 ? cx + side * ARENA_AISLE + w : 0;
            int fixedZ = dir[0] != 0 ? cz + side * ARENA_AISLE + w : 0;
            int[] xs = new int[50];
            int[] zs = new int[50];
            int[] want = new int[50];
            int n = 0;
            for (int p = 40; p <= 86; p++) {
                int x = dir[0] != 0 ? cx + dir[0] * p : fixedX;
                int z = dir[0] != 0 ? cz + dir[1] * p : fixedZ;
                double e = eOf(x - cx, z - cz);
                if (e > 1.0) {
                    break;
                }
                xs[n] = x;
                zs[n] = z;
                want[n] = arenaSurface(e);
                n++;
            }
            int h = 0;
            for (int i = 0; i < n; i++) {
                int x = xs[i];
                int z = zs[i];
                // 一次只抬一格：护墙那两格落差靠这里补平，不许凭空跳上去
                h = Math.min(want[i], h + 1);
                if (want[i] == 0) {
                    k.set(x, -1, z, Palette.STONE_CHISELED);
                    continue;
                }
                // 挖到与脚下齐平，上面留两格净空
                for (int y = h; y <= want[i] + 1; y++) {
                    k.set(x, y, z, null);
                }
                for (int y = h - 4; y <= h - 2; y++) {
                    k.set(x, y, z, Palette.STONE_BRICK);
                }
                boolean rise = i + 1 < n && want[i + 1] > h;
                k.set(x, h - 1, z, rise
                        ? Block.stairs("minecraft:stone_brick_stairs", facing)
                        : Palette.STONE_CHISELED);
            }
        }
    }

    /**
     * 角斗场的内部：包厢、场务用房、地下兽笼、放逐点。
     *
     * <p>单独一遍，是因为它要在外墙与看台都定下来之后才能挖——先挖再砌，
     * 挖出来的洞会被后面的回填盖掉。
     */
    static void colosseumInterior(BuildKit k, int cx, int cz) {
        // 贵宾席在东、主持台在西，都压在中轴线上、都在门洞正上方
        vipBox(k, cx, cz);
        hostStand(k, cx, cz);
        // 场务办公室与值更室：东侧看台体内，门开向门洞
        rooms(k, cx, cz);
        // 地下：兽笼与牢房，顶上留一格铁栅透月光
        undercroft(k, cx, cz);

        // 放逐点：沙地正中偏南，3×3 石台
        int ex = cx;
        int ez = cz + 7;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                k.set(ex + dx, -1, ez + dz, Palette.STONE_CHISELED);
                k.set(ex + dx, 0, ez + dz, Palette.STONE_CHISELED);
            }
        }
        for (int[] c : new int[][] {{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) {
            k.fill(ex + c[0], 1, ez + c[1], ex + c[0], 3, ez + c[1], Palette.IRON_BARS);
        }
        k.fill(ex, 1, ez, ex, 5, ez, Palette.DARK_LOG);
        k.set(ex, 6, ez, Palette.GOLD);
        // 净空圈：把周围十九格换成粗砂，一眼能看出这块地是"让出来的"
        for (int dx = -9; dx <= 9; dx++) {
            for (int dz = -9; dz <= 9; dz++) {
                if (k.scene.get(ex + dx, -1, ez + dz) != null) {
                    k.set(ex + dx, -1, ez + dz, Palette.COARSE);
                }
            }
        }
        k.bump("放逐点");
    }

    /**
     * 贵宾席：东侧圆环正中，东门正上方。
     *
     * <p>位置不是挑出来的：门在中轴上，包厢也在中轴上，从沙地抬头看，包厢就悬在
     * 那道门的拱顶上。西侧对称的位置给主持人——两座台子对着摆，中间那片看台
     * 才是"给普通人"的。
     *
     * <p>台基底下的东门隧道要留着（y 0..4 那七格），填实之前先跳过它。
     */
    private static void vipBox(BuildKit k, int cx, int cz) {
        int x1 = cx + 70;
        int x2 = cx + 80;
        int z1 = cz - 8;
        int z2 = cz + 8;
        int floor = ARENA_TOP;
        int eave = floor + 5;

        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                for (int y = 0; y < floor; y++) {
                    if (y <= 4 && Math.abs(z - cz) <= 3) {
                        continue;
                    }
                    k.set(x, y, z, Palette.STONE_BRICK);
                }
                k.set(x, floor, z, Palette.DARK_PLANK);
                for (int y = floor + 1; y < eave; y++) {
                    k.set(x, y, z, null);
                }
                k.set(x, eave, z, Palette.SPRUCE_PLANK);
            }
        }
        // 屋顶：不封平。平顶从外面看就是一块棕板，站在场里也认不出那是包厢
        k.roof(x1 - 1, z1 - 1, x2 + 1, z2 + 1, eave, true, Palette.TILE_STAIR,
                Palette.PLASTER_CREAM, 1);

        // 前沿：一道矮栏，坐着能看见沙地。再高就挡视线，再矮就危险
        for (int z = z1; z <= z2; z++) {
            k.set(x1, floor + 1, z, z % 3 == 0 ? Palette.STONE_BRICK : Palette.STONE_CHISELED);
        }
        // 三面墙：南墙开门，其余开窗
        for (int y = floor + 1; y < eave; y++) {
            for (int x = x1; x <= x2; x++) {
                k.set(x, y, z1, Palette.PLASTER_CREAM);
                k.set(x, y, z2, Palette.PLASTER_CREAM);
            }
            for (int z = z1; z <= z2; z++) {
                k.set(x2, y, z, Palette.PLASTER_CREAM);
            }
        }
        for (int z = cz - 4; z <= cz + 4; z += 4) {
            k.set(x2 - 0, floor + 3, z, Palette.GLASS_GRAY);
        }
        for (int x = x1 + 3; x <= x2 - 3; x += 4) {
            k.set(x, floor + 3, z1, Palette.GLASS_GRAY);
            k.set(x, floor + 3, z2, Palette.GLASS_GRAY);
        }
        // 四角立柱：把这座木头盒子撑起来
        for (int x : new int[] {x1, x2}) {
            for (int z : new int[] {z1, z2}) {
                k.fill(x, floor + 1, z, x, eave, z, Palette.DARK_LOG);
            }
        }
        // 门：南墙，正对回廊
        for (int x = cx + 76; x <= cx + 77; x++) {
            for (int y = floor + 1; y <= floor + 2; y++) {
                k.set(x, y, z2, null);
            }
        }

        // 里面：一条红毯、三排长凳、两盏吊灯、后墙的旗
        for (int z = z1 + 2; z <= z2 - 2; z++) {
            k.set(cx + 74, floor, z, Palette.CARPET_RED);
        }
        for (int i = 0; i < 3; i++) {
            int bx = x2 - 2 - i * 2;
            k.fill(bx, floor + 1, z1 + 1, bx, floor + 1, z2 - 1, Palette.OAK_PLANK);
            if (i > 0) {
                k.fill(bx, floor, z1 + 1, bx, floor, z2 - 1, Palette.OAK_PLANK);
                k.fill(bx, floor + 1, z1 + 1, bx, floor + 1, z2 - 1, Palette.OAK_PLANK);
            }
        }
        for (int z : new int[] {z1 + 3, z2 - 3}) {
            k.set(cx + 73, eave - 1, z, Palette.LANTERN_HANGING);
        }
        for (int y = floor + 2; y <= floor + 4; y++) {
            for (int z = cz - 2; z <= cz + 2; z++) {
                k.set(x2, y, z, y % 2 == 0 ? Palette.WOOL_RED : Palette.WOOL_BLACK);
            }
        }
        k.bump("贵宾席");
    }

    /**
     * 主持台：西侧，与贵宾席一东一西对着。
     *
     * <p>贵宾席是封起来的，这座是敞的——主持人和听差的本来就该站在风里，
     * 让沙地上的人看得见他们。
     */
    private static void hostStand(BuildKit k, int cx, int cz) {
        int x1 = cx - 80;
        int x2 = cx - 70;
        int z1 = cz - 8;
        int z2 = cz + 8;
        int floor = ARENA_TOP;
        int eave = floor + 5;

        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                for (int y = 0; y < floor; y++) {
                    if (y <= 4 && Math.abs(z - cz) <= 3) {
                        continue;
                    }
                    k.set(x, y, z, Palette.STONE_BRICK);
                }
                k.set(x, floor, z, Palette.OAK_PLANK);
            }
        }

        // 栏杆：沿前沿，以及南北两侧的里半边。外侧留给回廊，人能走进来
        for (int z = z1; z <= z2; z++) {
            k.set(x2, floor + 1, z, Palette.STONE_CHISELED);
        }
        for (int x = x1; x <= cx - 74; x++) {
            k.set(x, floor + 1, z1, Palette.STONE_CHISELED);
            k.set(x, floor + 1, z2, Palette.STONE_CHISELED);
        }
        // 四根柱子 + 条纹布棚
        for (int x : new int[] {x1 + 1, x2 - 1}) {
            for (int z : new int[] {z1 + 1, z2 - 1}) {
                k.fill(x, floor + 1, z, x, eave - 1, z, Palette.SPRUCE_LOG);
            }
        }
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                if (x == x1 || x == x2 || z == z1 || z == z2) {
                    k.set(x, eave, z, ((x + z) & 1) == 0 ? Palette.WOOL_RED : Palette.WOOL_CREAM);
                }
            }
        }
        // 正中一张讲台，台上一盏灯、一本册子
        k.fill(cx - 77, floor + 1, cz - 1, cx - 75, floor + 1, cz + 1, Palette.STONE_CHISELED);
        k.set(cx - 76, floor + 2, cz, Palette.LECTERN);
        k.set(cx - 76, floor + 2, cz - 2, Palette.LANTERN);
        k.set(cx - 76, floor + 2, cz + 2, Palette.LANTERN);
        // 工作人员的条凳：靠外墙那半边
        for (int z = z1 + 2; z <= z2 - 2; z += 3) {
            k.set(x1 + 2, floor + 1, z, Palette.OAK_PLANK);
            k.set(x1 + 3, floor + 1, z, Palette.OAK_PLANK);
        }
        k.bump("主持台");
    }

    /**
     * 场务办公室与值更室：东侧看台体内，门开向门洞隧道。
     *
     * <p>原来是往看台里开门的——门外是实心的石头，那间房谁也进不去。
     */
    private static void rooms(BuildKit k, int cx, int cz) {
        int x1 = cx + 52;
        int x2 = cx + 66;
        int z1 = cz - 14;
        int z2 = cz - 4;
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                k.set(x, 0, z, Palette.DARK_PLANK);
                for (int y = 1; y <= 4; y++) {
                    k.set(x, y, z, null);
                }
                k.set(x, 5, z, Palette.STONE_BRICK);
            }
        }
        for (int x = x1; x <= x2; x++) {
            for (int y = 1; y <= 4; y++) {
                k.set(x, y, z1, Palette.STONE_BRICK);
                k.set(x, y, z2, Palette.STONE_BRICK);
            }
        }
        for (int z = z1; z <= z2; z++) {
            for (int y = 1; y <= 4; y++) {
                k.set(x1, y, z, Palette.STONE_BRICK);
                k.set(x2, y, z, Palette.STONE_BRICK);
            }
        }
        // 门：南墙，正对门洞（隧道在 z=cz-3..cz+3，y 0..4）
        for (int x = cx + 56; x <= cx + 57; x++) {
            for (int y = 1; y <= 3; y++) {
                k.set(x, y, z2, null);
            }
        }
        k.fill(cx + 58, 0, z2 - 1, cx + 65, 0, z2 - 1, Palette.SPRUCE_PLANK);
        // 值更的那几格：床、桌、灯、牌、柜
        k.set(x2 - 2, 1, z1 + 2, Palette.WOOL_RED);
        k.set(x2 - 3, 1, z1 + 2, Palette.WOOL_RED);
        k.set(x2 - 2, 1, z1 + 4, Palette.OAK_PLANK);
        k.set(x2 - 2, 2, z1 + 4, Palette.LANTERN);
        k.set(x1 + 1, 1, z1 + 1, Palette.CHEST);
        k.set(x1 + 1, 1, z2 - 1, Palette.SIGN);
        k.set(x1 + 2, 1, z2 - 1, Palette.LECTERN);
        k.bump("场务室");
    }

    /**
     * 地下兽笼：在沙地下面，只有一层可用（y −3..−2）。
     *
     * <p>顶上那一格铁栅是有意的：月光从这里照进最东一间。白天关得住，
     * 夜里关不住——这是支线的物证，不是装饰。
     */
    private static void undercroft(BuildKit k, int cx, int cz) {
        int x1 = cx + 1;
        int x2 = cx + 21;
        int z1 = cz + 13;
        int z2 = cz + 33;
        // 挖空
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                for (int y = -3; y <= -2; y++) {
                    k.set(x, y, z, null);
                }
            }
        }
        // 地板与隔墙：三间笼子
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                k.set(x, -4, z, Palette.STONE_BRICK);
            }
        }
        for (int x = x1; x <= x2; x++) {
            for (int y = -3; y <= -2; y++) {
                k.set(x, y, z1, Palette.STONE_BRICK);
                k.set(x, y, z2, Palette.STONE_BRICK);
            }
        }
        int[] bars = {x1 + 6, x1 + 14};
        for (int bx : bars) {
            for (int z = z1 + 1; z <= z2 - 1; z++) {
                for (int y = -3; y <= -2; y++) {
                    k.set(bx, y, z, Palette.IRON_BARS);
                }
            }
        }
        for (int x = x1; x <= x2; x++) {
            for (int y = -3; y <= -2; y++) {
                k.set(x, y, z1, Palette.STONE_BRICK);
            }
        }
        // 草垫与桶：笼子里该有的
        for (int x = x1 + 2; x <= x1 + 4; x++) {
            k.set(x, -3, z2 - 2, Palette.HAY);
        }
        k.set(x1 + 10, -3, z2 - 2, Palette.BARREL);
        k.set(x1 + 18, -3, z2 - 2, Palette.CAULDRON);
        // 那一格天窗：沙地边缘一格铁栅，月光直下
        int sky = x2 - 1;
        k.set(sky, -1, z2 - 6, Palette.IRON_BARS);
        for (int y = -2; y <= -2; y++) {
            k.set(sky, y, z2 - 6, null);
        }
        // 入口：沙地南沿一道下行梯。
        // 先把这块地填实再挖——旧版只挖不填，台阶悬在沙地下面的空气上，
        // 每一级头上还顶着一格没挖掉的沙，走三步就卡死。
        k.fill(cx - 1, -5, z2, cx + 1, -1, z2 + 4, Palette.STONE_BRICK);
        for (int i = 0; i <= 3; i++) {
            int z = z2 + 4 - i;
            for (int w = -1; w <= 1; w++) {
                for (int y = -i; y <= 1 - i; y++) {
                    k.set(cx + w, y, z, null);
                }
            }
        }
        // 门：台阶末端进笼子
        for (int w = -1; w <= 1; w++) {
            for (int y = -3; y <= -2; y++) {
                k.set(cx + w, y, z2, null);
            }
        }
        k.bump("兽笼");
    }

    /**
     * 神职院落：金字塔四周的四座院子。
     *
     * <p>外角一律切 45°，不是为了好看——公共水井在 (±96,±96)，正好落在院子的
     * 外角上。切掉那个角，井就留在院外，谁都能打水。
     */
    static void clergyCourts(BuildKit k) {
        court(k, -99, -99, "cloister");
        court(k, 72, -99, "scriptorium");
        court(k, 72, 72, "treasury");
        court(k, -99, 72, "hospice");
    }

    private static void court(BuildKit k, int x1, int z1, String type) {
        int x2 = x1 + 27;
        int z2 = z1 + 27;
        k.claim("神职院", x1, z1, x2, z2);
        int sx = x1 < 0 ? -1 : 1;
        int sz = z1 < 0 ? -1 : 1;

        // 院子地面：十字小径 + 草地
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                if (sx * x + sz * z > 184) {
                    continue;
                }
                int cx = (x1 + x2) / 2;
                int cz = (z1 + z2) / 2;
                boolean path = Math.abs(x - cx) <= 1 || Math.abs(z - cz) <= 1;
                k.set(x, -1, z, path ? Palette.PATH : Palette.GRASS);
            }
        }

        // 院墙：五格高，切角处断开，另留一道门
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
                if (!edge || sx * x + sz * z > 184) {
                    continue;
                }
                // 朝金字塔那一面开门，门宽三格
                boolean gate = (x == x2 && Math.abs(z - (z1 + z2) / 2) <= 1)
                        || (x == x1 && Math.abs(z - (z1 + z2) / 2) <= 1)
                        || (z == z2 && Math.abs(x - (x1 + x2) / 2) <= 1)
                        || (z == z1 && Math.abs(x - (x1 + x2) / 2) <= 1);
                boolean inward = Math.abs(x) < Math.abs(z) ? x == (x1 < 0 ? x2 : x1) : z == (z1 < 0 ? z2 : z1);
                if (gate && inward) {
                    continue;
                }
                for (int y = 0; y <= 5; y++) {
                    k.set(x, y, z, y == 5 ? Palette.STONE_CHISELED : Palette.COBBLE);
                }
            }
        }

        // 院里的楼：四座院子四种用途，体量都压得住院墙
        int bx = x1 + 6;
        int bz = z1 + 6;
        switch (type) {
            case "cloister" -> {
                hall(k, bx, bz, 15, 10, 10, false, Palette.PLASTER);
                // 钟楼：四院里唯一的竖向
                tower(k, x2 - 10, z2 - 12, 5, 22);
            }
            case "scriptorium" -> {
                hall(k, bx, bz, 16, 11, 9, false, Palette.PLASTER);
                for (int x = bx + 2; x <= bx + 14; x += 4) {
                    k.fill(x, 2, bz, x + 1, 5, bz, Palette.GLASS);
                }
                k.fill(bx + 2, 0, bz + 4, bx + 13, 0, bz + 5, Palette.OAK_PLANK);
                k.set(bx + 4, 1, bz + 4, Palette.LECTERN);
                k.set(bx + 8, 1, bz + 4, Palette.LECTERN);
                k.set(bx + 12, 1, bz + 4, Palette.LECTERN);
            }
            case "treasury" -> {
                hall(k, bx + 1, bz + 1, 13, 9, 8, true, Palette.STONE_BRICK);
                for (int x = bx + 3; x <= bx + 12; x++) {
                    for (int z = bz + 3; z <= bz + 8; z++) {
                        k.set(x, 0, z, Palette.STONE_BRICK);
                    }
                }
                for (int i = 0; i < 5; i++) {
                    k.set(bx + 2, 1, bz + 2 + i, Palette.CHEST);
                }
            }
            default -> {
                hall(k, bx, bz, 16, 10, 9, false, Palette.PLASTER);
                // 粥棚：院门内侧一条长桌
                k.fill(bx - 2, 0, bz + 12, bx + 10, 0, bz + 13, Palette.OAK_PLANK);
                for (int i = 0; i < 4; i++) {
                    k.set(bx - 1 + i * 3, 0, bz + 16, Palette.BARREL);
                }
                for (int i = 0; i < 3; i++) {
                    k.fill(bx + 12, 0, bz + 4 + i * 4, bx + 12, 3, bz + 4 + i * 4, Palette.SPRUCE_FENCE);
                    k.set(bx + 12, 4, bz + 4 + i * 4, Palette.WOOL_CREAM);
                }
            }
        }
        k.bump("神职院");
    }

    /** 一座石砌小楼：墙、窗、门、坡顶。四个院子共用。 */
    private static void hall(BuildKit k, int x, int z, int w, int d, int h, boolean sealed, Block plaster) {
        k.fill(x - 1, -3, z - 1, x + w, -1, z + d, Palette.STONE_BRICK);
        k.walls(x, 0, z, x + w - 1, h, z + d - 1, Palette.STONE_BRICK);
        k.carve(x + 1, 0, z + 1, x + w - 2, h - 1, z + d - 2);
        if (!sealed) {
            for (int dx = 3; dx < w - 3; dx += 4) {
                k.fill(x + dx, 2, z + d - 1, x + dx + 1, 4, z + d - 1, Palette.GLASS);
            }
            k.fill(x + w / 2 - 1, 0, z, x + w / 2, 2, z, Palette.DARK_PLANK);
        } else {
            // 铁门：两格宽、三格高，门框包石
            k.fill(x + w / 2 - 1, 0, z, x + w / 2, 2, z, Palette.IRON);
            k.fill(x + w / 2 - 2, 0, z, x + w / 2 - 2, 3, z, Palette.STONE_CHISELED);
            k.fill(x + w / 2 + 1, 0, z, x + w / 2 + 1, 3, z, Palette.STONE_CHISELED);
        }
        k.roof(x - 2, z - 2, x + w + 1, z + d + 1, h + 1, true, Palette.SLATE_STAIR, plaster, 1);
        k.set(x + 2, h + 1, z + 2, Palette.CAMPFIRE);
    }

    private static void tower(BuildKit k, int x, int z, int r, int h) {
        for (int y = 0; y <= h; y++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    boolean edge = Math.abs(dx) == r || Math.abs(dz) == r;
                    if (edge) {
                        k.set(x + dx, y, z + dz, y % 6 == 0 ? Palette.STONE_CHISELED : Palette.STONE_BRICK);
                    }
                }
            }
        }
        k.fill(x - r, h, z - r, x + r, h, z + r, Palette.STONE_BRICK);
        k.cone(x, z, r + 1, h + 1, 7, Palette.SLATE);
        // 钟窗
        for (int dz = -1; dz <= 1; dz++) {
            k.set(x, h - 3, z + dz, Palette.BELL);
        }
    }

    static void colosseumInterior(BuildKit k) {
        colosseumInterior(k, 185, 193);
    }

    /**
     * 演讲台：金字塔与王座厅之间的中轴。
     *
     * <p>它是全城唯一一处"一个人对所有人说话"的地方。空着的时间比用着的时间长。
     */
    static void tribune(BuildKit k) {
        int x1 = -13;
        int x2 = 13;
        int z1 = -92;
        int z2 = -80;
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                for (int y = 0; y <= 3; y++) {
                    k.set(x, y, z, y == 3 ? Palette.STONE_CHISELED : Palette.STONE_BRICK);
                }
            }
        }
        // 三级踏步朝南（面向广场）
        for (int i = 0; i < 3; i++) {
            for (int x = -6; x <= 6; x++) {
                k.set(x, i, z2 + 1 + (2 - i), Palette.STONE_CHISELED);
            }
        }
        // 背屏与四柱
        k.fill(x1, 4, z1, x2, 7, z1, Palette.STONE_BRICK);
        for (int x : new int[] {x1, x1 + 8, x2 - 8, x2}) {
            k.fill(x, 4, z1, x, 9, z1, Palette.STONE_CHISELED);
        }
        // 华盖
        k.fill(x1 - 1, 10, z1 - 1, x2 + 1, 10, z2 - 3, Palette.STONE_BRICK);
        k.fill(x1 - 1, 11, z1 - 1, x2 + 1, 11, z2 - 3, Palette.STONE_CHISELED);
        // 台上的旗与火盆
        for (int x : new int[] {x1 + 2, x2 - 2}) {
            k.fill(x, 4, z2 - 2, x, 8, z2 - 2, Palette.SPRUCE_FENCE);
            k.fill(x, 7, z2 - 2, x, 8, z2 - 2, Palette.WOOL_RED);
        }
        k.set(x1 + 4, 4, z2 - 2, Palette.CAMPFIRE);
        k.set(x2 - 4, 4, z2 - 2, Palette.CAMPFIRE);
        k.bump("演讲台");
    }
}
