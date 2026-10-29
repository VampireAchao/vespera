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
    private static final int TIER_H = 9;
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
        // 基座：比塔基再宽四格，四周留出巡礼道
        k.pave(-BASE - 4, -BASE - 4, BASE + 4, BASE + 4, Palette.SANDSTONE, Palette.STONE_BRICK);
        for (int x = -BASE - 4; x <= BASE + 4; x++) {
            for (int z = -BASE - 4; z <= BASE + 4; z++) {
                int d = Math.max(Math.abs(x), Math.abs(z));
                if (d > BASE && d <= BASE + 4) {
                    k.set(x, -1, z, Palette.STONE_BRICK);
                    k.set(x, 0, z, Palette.STONE_CHISELED);
                }
            }
        }

        for (int t = 0; t < TIERS; t++) {
            int radius = BASE - t * INSET;
            int y0 = t * TIER_H;
            int next = t + 1 < TIERS ? radius - INSET : 0;

            // 外壳：三层厚，越往上越收
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    int d = Math.max(Math.abs(x), Math.abs(z));
                    if (d > radius || d <= radius - SHELL) {
                        continue;
                    }
                    for (int y = y0; y < y0 + TIER_H; y++) {
                        Block block = y == y0 ? Palette.STONE_CHISELED
                                : (d == radius ? Palette.SANDSTONE : Palette.STONE_BRICK);
                        // 竖缝：每四格一道凹槽，远看有阴影
                        if ((x + z) % 8 == 0 && y > y0 + 1) {
                            block = Palette.STONE_BRICK;
                        }
                        k.set(x, y, z, block);
                    }
                }
            }

            // 露台与本层女儿墙
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    int d = Math.max(Math.abs(x), Math.abs(z));
                    if (d <= next || d > radius) {
                        continue;
                    }
                    k.set(x, y0 + TIER_H, z, Palette.SANDSTONE);
                    if (d == radius || d == next + 1) {
                        k.set(x, y0 + TIER_H + 1, z, Palette.STONE_BRICK);
                    }
                }
            }

            // 内圈回廊：离外壳三格，宽六格，绕塔一周
            for (int x = -radius + SHELL; x <= radius - SHELL; x++) {
                for (int z = -radius + SHELL; z <= radius - SHELL; z++) {
                    int d = Math.max(Math.abs(x), Math.abs(z));
                    if (d > radius - SHELL || d <= radius - SHELL - 6) {
                        continue;
                    }
                    k.set(x, y0, z, Palette.SANDSTONE);
                    k.set(x, y0 + 7, z, Palette.STONE_CHISELED);
                }
            }

            // 南面的中央大台阶：从本层露台走到上一层，九步
            for (int i = 0; i < TIER_H; i++) {
                int walkY = y0 + i;
                int walkZ = radius - i;
                for (int x = -7; x <= 7; x++) {
                    k.set(x, walkY, walkZ, Block.stairs("minecraft:sandstone_stairs", "north"));
                    for (int up = 1; up <= 3; up++) {
                        k.set(x, walkY + up, walkZ, null);
                    }
                }
                for (int x : new int[] {-8, 8}) {
                    k.set(x, walkY, walkZ, Palette.STONE_CHISELED);
                    k.set(x, walkY + 1, walkZ, Palette.STONE_CHISELED);
                }
            }
            // 壁柱：每八格一道，把大面切成竖条
            for (int side = 0; side < 4; side++) {
                for (int along = -(radius - 6); along <= radius - 6; along += 8) {
                    for (int y = y0 + 1; y < y0 + TIER_H - 1; y++) {
                        int px = side < 2 ? along : (side == 2 ? -radius : radius);
                        int pz = side < 2 ? (side == 0 ? -radius : radius) : along;
                        k.set(px, y, pz, Palette.STONE_CHISELED);
                    }
                }
            }

            // 每层一道上行的梯：沿南侧内壁，九步一层
            int runZ = radius - SHELL - 7;
            for (int i = 0; i < TIER_H; i++) {
                for (int w = -2; w <= 2; w++) {
                    int x = w;
                    int z = runZ - i;
                    k.set(x, y0 + i, z, Block.stairs("minecraft:sandstone_stairs", "north"));
                    k.set(x, y0 + i + 1, z, null);
                    k.set(x, y0 + i + 2, z, null);
                    k.set(x, y0 + i + 3, z, null);
                }
            }

            // 底层：教会人员的居所——沿内壁一圈小室
            if (t == 0) {
                clergyRooms(k, radius);
            }
        }

        // 塔顶：祭坛
        altar(k);
        k.bump("圣座金字塔");
        k.bump("地标");
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
    private static void altar(BuildKit k) {
        int y = TIERS * TIER_H;
        int r = 18;
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                int d = Math.max(Math.abs(x), Math.abs(z));
                if (d <= r) {
                    k.set(x, y, z, (d <= 6) ? Palette.STONE_CHISELED : Palette.SANDSTONE);
                }
                if (d == r) {
                    k.set(x, y + 1, z, Palette.STONE_BRICK);
                    if ((x + z) % 4 == 0) {
                        k.set(x, y + 2, z, Palette.GOLD);
                    }
                }
            }
        }
        // 祭坛台：三级，金顶
        k.fill(-6, y + 1, -6, 6, y + 1, 6, Palette.STONE_CHISELED);
        k.fill(-4, y + 2, -4, 4, y + 2, 4, Palette.SANDSTONE);
        k.fill(-2, y + 3, -2, 2, y + 3, 2, Palette.GOLD);
        k.set(0, y + 4, 0, Palette.GOLD);
        // 四根柱与华盖
        for (int[] c : new int[][] {{-7, -7}, {7, -7}, {-7, 7}, {7, 7}}) {
            k.fill(c[0], y + 1, c[1], c[0], y + 6, c[1], Palette.STONE_CHISELED);
        }
        k.fill(-8, y + 7, -8, 8, y + 7, 8, Palette.PLASTER_CREAM);
        k.fill(-6, y + 8, -6, 6, y + 8, 6, Palette.SANDSTONE);
        k.fill(-4, y + 9, -4, 4, y + 9, 4, Palette.GOLD);
        k.fill(0, y + 10, 0, 0, y + 14, 0, Palette.GOLD);
        k.set(0, y + 15, 0, Palette.GOLD);
        // 火盆
        for (int[] c : new int[][] {{-14, -14}, {14, -14}, {-14, 14}, {14, 14}}) {
            k.fill(c[0], y + 1, c[1], c[0], y + 2, c[1], Palette.STONE_BRICK);
            k.set(c[0], y + 3, c[1], Palette.CAMPFIRE);
        }
        // 钟：挂在北侧
        k.fill(-2, y + 5, -14, 2, y + 5, -14, Palette.DARK_LOG);
        k.set(0, y + 4, -14, Palette.BELL);
    }

    // -- 王座大厅：正北，梯形向上 -------------------------------------------

    /**
     * 三级台地，一级比一级窄、一级比一级高，尽头是王座。
     *
     * <p>"梯形向上"不是造型，是仪式：来朝见的人必须一级一级走上去，
     * 每上一级，回头看一眼城就更小一点。等他站到王座前，已经没法平视了。
     */
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

        // 后部塔楼：王座的"钉子"
        int tx = 0;
        int tz = -272;
        k.fill(-9, base + 1, tz - 9, 9, top + 18, tz + 9, Palette.STONE_BRICK);
        k.carve(-8, base + 1, tz - 8, 8, top + 17, tz + 8);
        k.crenellate(-9, tz - 9, 9, tz + 9, top + 19, Palette.STONE_BRICK);
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

    // -- 角斗场：椭圆，四门 ------------------------------------------------

    /**
     * 罗马式角斗场：椭圆看台、四座门、沙地、地下关押区。
     *
     * <p>它被放进城墙里，是因为它本来就是城里的公共建筑——只不过在这里，
     * 沙地上被放逐的不是战俘，是"不该留在城里的人"。
     */
    static void colosseum(BuildKit k, int cx, int cz) {
        double rx = 82;
        double rz = 70;
        int rows = 18;
        int wallH = 24;

        // 外墙：三层盲拱
        for (int dx = -80; dx <= 80; dx++) {
            for (int dz = -68; dz <= 68; dz++) {
                double e = (dx / rx) * (dx / rx) + (dz / rz) * (dz / rz);
                if (e <= 1.0 || e > 1.09) {
                    continue;
                }
                int y = 0;
                boolean gate = Math.abs(dx) < 6 || Math.abs(dz) < 6;
                if (gate) {
                    continue;
                }
                for (y = 0; y < wallH; y++) {
                    boolean cornice = y == 7 || y == 15 || y == wallH - 1;
                    Block block = cornice ? Palette.STONE_CHISELED
                            : (((dx + dz) & 7) == 0 ? Palette.STONE_BRICK : Palette.SANDSTONE);
                    k.set(cx + dx, y, cz + dz, block);
                }
                // 拱窗：每三层开一个
                if ((dx + dz) % 5 == 0) {
                    k.set(cx + dx, 4, cz + dz, Palette.DARK_PLANK);
                    k.set(cx + dx, 12, cz + dz, Palette.DARK_PLANK);
                    k.set(cx + dx, 20, cz + dz, Palette.DARK_PLANK);
                }
            }
        }

        // 看台：一圈圈升起
        for (int row = 0; row < rows; row++) {
            double outer = 0.58 + row * 0.029;
            double inner = outer - 0.030;
            int y = 1 + row;
            for (int dx = -80; dx <= 80; dx++) {
                for (int dz = -68; dz <= 68; dz++) {
                    double e = (dx / rx) * (dx / rx) + (dz / rz) * (dz / rz);
                    if (e < inner * inner || e > outer * outer) {
                        continue;
                    }
                    boolean gate = Math.abs(dx) < 4 || Math.abs(dz) < 4;
                    if (gate) {
                        continue;
                    }
                    k.set(cx + dx, y, cz + dz, row % 2 == 0 ? Palette.STONE_BRICK
                            : Palette.ANDESITE_POLISHED);
                    k.set(cx + dx, y - 1, cz + dz, Palette.STONE_BRICK);
                }
            }
        }

        // 竞技场：沙地
        for (int dx = -46; dx <= 46; dx++) {
            for (int dz = -38; dz <= 38; dz++) {
                double e = (dx / 44.0) * (dx / 44.0) + (dz / 36.0) * (dz / 36.0);
                if (e <= 1.0) {
                    k.set(cx + dx, -1, cz + dz, ((dx + dz) & 1) == 0 ? Palette.SAND : Palette.COARSE);
                }
            }
        }
        // 地下关押区：地上留几道铁栅，说明沙地下还有一层
        for (int i = 0; i < 4; i++) {
            int x = cx - 24 + i * 16;
            k.set(x, -1, cz, Palette.IRON_BARS);
            k.set(x + 1, -1, cz, Palette.IRON_BARS);
        }

        // 四座门：拱廊
        for (int[] dir : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            int gx = cx + dir[0] * 62;
            int gz = cz + dir[1] * 52;
            for (int i = -5; i <= 5; i++) {
                for (int y = 0; y <= 8; y++) {
                    int x = dir[0] != 0 ? gx : cx + i;
                    int z = dir[0] != 0 ? cz + i : gz;
                    k.set(x, y, z, y == 8 ? Palette.STONE_CHISELED : Palette.STONE_BRICK);
                }
            }
        }

        // 檐口的桅杆：原先用来撑遮阳篷
        for (int i = 0; i < 12; i++) {
            double angle = i / 12.0 * Math.PI * 2;
            int x = cx + (int) Math.round(Math.cos(angle) * (rx + 2));
            int z = cz + (int) Math.round(Math.sin(angle) * (rz + 2));
            k.fill(x, wallH, z, x, wallH + 5, z, Palette.SPRUCE_FENCE);
        }
        // 场边的雕像与火盆
        for (int[] p : new int[][] {{-40, -30}, {40, -30}, {-40, 30}, {40, 30}}) {
            k.fill(cx + p[0], 0, cz + p[1], cx + p[0], 3, cz + p[1], Palette.STONE_CHISELED);
            k.set(cx + p[0], 4, cz + p[1], Palette.GOLD);
        }
        k.bump("角斗场");
        k.bump("地标");
    }
}
