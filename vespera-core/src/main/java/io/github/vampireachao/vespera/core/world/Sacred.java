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

        // 外墙：按角度参数化。
        // 用 dx/dz 包围盒去判椭圆，一定会漏掉四个极点——要求 |dx|>82，
        // 而循环只走到 80。东西两极整段消失，南北各缺 39 格。
        for (double deg = 0; deg < 360; deg += 0.5) {
            double t = Math.toRadians(deg);
            for (double s = 1.0; s <= 1.076; s += 0.025) {
                int x = cx + (int) Math.round(rx * Math.sqrt(s) * Math.cos(t));
                int z = cz + (int) Math.round(rz * Math.sqrt(s) * Math.sin(t));
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

        // 看台：实心台地。
        // 行距 0.55 + (row+1)*0.025，最外行恰好落在 e=1.000，贴住墙内沿；
        // 旧写法 0.58 + row*0.029 会让第 17 排长到 e=1.15，从墙外鼓出三圈石头。
        for (int dx = -85; dx <= 85; dx++) {
            for (int dz = -73; dz <= 73; dz++) {
                double u = (double) dx / rx;
                double v = (double) dz / rz;
                double e = u * u + v * v;
                if (e > 1.0) {
                    continue;
                }
                int x = cx + dx;
                int z = cz + dz;
                boolean gate = Math.abs(dx) <= 3 || Math.abs(dz) <= 3;

                if (e <= 0.288) {
                    k.set(x, -1, z, ((dx + dz) & 1) == 0 ? Palette.SAND : Palette.COARSE);
                    continue;
                }
                if (e <= 0.3025) {
                    // 护墙：把沙地和第一排隔开。没有它，观众一步就跨进场里
                    if (!gate) {
                        for (int y = 0; y <= 2; y++) {
                            k.set(x, y, z, y == 2 ? Palette.STONE_CHISELED : Palette.STONE_BRICK);
                        }
                    }
                    continue;
                }
                double lin = Math.sqrt(e);
                int row = (int) Math.floor((lin - 0.55) / 0.025);
                row = Math.max(0, Math.min(rows, row));
                int top = 1 + row;
                // 门洞位置留成隧道：下五格空，上面回填，从外面看仍是完整的看台
                int from = gate ? 5 : 0;
                for (int y = from; y <= top; y++) {
                    Block block = (y == top)
                            ? (row % 2 == 0 ? Palette.STONE_BRICK : Palette.ANDESITE_POLISHED)
                            : Palette.STONE_BRICK;
                    k.set(x, y, z, block);
                }
            }
        }

        // 四门：外门洞底与场外同高，隧道直通沙地
        for (int[] dir : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            for (int d = -3; d <= 3; d++) {
                for (int step = 0; step <= 84; step++) {
                    int x = dir[0] != 0 ? cx + dir[0] * step : cx + d;
                    int z = dir[0] != 0 ? cz + d : cz + dir[1] * step;
                    double u = (double) (x - cx) / rx;
                    double v = (double) (z - cz) / rz;
                    if (u * u + v * v > 1.0) {
                        break;
                    }
                    if (u * u + v * v > 0.288) {
                        for (int y = 0; y <= 4; y++) {
                            k.set(x, y, z, null);
                        }
                    }
                    k.set(x, -1, z, Palette.COBBLE);
                }
            }
        }

        // 四段登看台的台阶：从沙地直上顶层，宽三格
        for (int[] dir : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            for (int i = 0; i < rows; i++) {
                int along = 46 + i;
                for (int w = -1; w <= 1; w++) {
                    int x = dir[0] != 0 ? cx + dir[0] * along : cx + w;
                    int z = dir[0] != 0 ? cz + w : cz + dir[1] * along;
                    k.set(x, 1 + i, z, Block.stairs("minecraft:stone_brick_stairs",
                            dir[0] > 0 ? "east" : dir[0] < 0 ? "west" : dir[1] > 0 ? "south" : "north"));
                    k.set(x, i, z, Palette.STONE_BRICK);
                }
            }
        }

        // 檐口的桅杆：用来撑遮阳篷
        for (int i = 0; i < 12; i++) {
            double angle = i / 12.0 * Math.PI * 2;
            int x = cx + (int) Math.round(Math.cos(angle) * (rx + 2));
            int z = cz + (int) Math.round(Math.sin(angle) * (rz + 2));
            k.fill(x, wallH, z, x, wallH + 5, z, Palette.SPRUCE_FENCE);
        }
        k.bump("角斗场");
        k.bump("地标");
    }

    /**
     * 角斗场的内部：包厢、场务用房、地下兽笼、放逐点。
     *
     * <p>单独一遍，是因为它要在外墙与看台都定下来之后才能挖——先挖再砌，
     * 挖出来的洞会被后面的回填盖掉。
     */
    static void colosseumInterior(BuildKit k, int cx, int cz) {
        // 达官包厢：东侧，观演面朝西，独立石梯从场外直接上来
        carveBox(k, cx, cz);
        // 场务办公室与值更室：东侧看台体内
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

    /** 包厢：六间，前沿一道铁栏，遮阳篷挂在檐口桅杆上。 */
    private static void carveBox(BuildKit k, int cx, int cz) {
        int x1 = cx + 51;
        int x2 = cx + 73;
        int z1 = cz - 45;
        int z2 = cz - 17;
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                k.set(x, 12, z, Palette.DARK_PLANK);
                for (int y = 13; y <= 17; y++) {
                    k.set(x, y, z, null);
                }
                k.set(x, 18, z, Palette.SPRUCE_PLANK);
            }
        }
        // 看台一侧的窗：铁栏，坐着能看见沙地，站着够不着
        for (int z = z1; z <= z2; z++) {
            for (int y = 13; y <= 15; y++) {
                k.set(x1, y, z, Palette.IRON_BARS);
            }
            for (int y = 16; y <= 18; y++) {
                k.set(x1, y, z, Palette.STONE_BRICK);
            }
        }
        // 隔间：每五格一道
        for (int z = z1 + 5; z < z2; z += 5) {
            for (int x = x1 + 1; x <= x2; x++) {
                for (int y = 13; y <= 17; y++) {
                    k.set(x, y, z, Palette.DARK_PLANK);
                }
            }
        }
        // 每间一桌一椅
        for (int z = z1 + 2; z < z2; z += 5) {
            k.set(x2 - 2, 13, z, Palette.OAK_PLANK);
            k.set(x2 - 3, 13, z, Palette.BARREL);
        }
        // 独立入口：外墙开口 + 场外石梯
        for (int x = x2 + 1; x <= cx + 82; x++) {
            for (int z = cz - 15; z <= cz - 11; z++) {
                for (int y = 12; y <= 15; y++) {
                    k.set(x, y, z, null);
                }
            }
        }
        for (int i = 0; i <= 12; i++) {
            for (int w = 0; w <= 4; w++) {
                k.set(cx + 83 + w, i, cz - 15 + (i / 2), Palette.STONE_BRICK);
                k.set(cx + 83 + w, i + 1, cz - 15 + (i / 2), null);
            }
        }
        k.set(x2 - 4, 13, z1 + 2, Palette.LANTERN);
        k.bump("包厢");
    }

    /** 场务办公室与值更室。夜里该有人坐在这儿——支线会用到。 */
    private static void rooms(BuildKit k, int cx, int cz) {
        int x1 = cx + 57;
        int x2 = cx + 73;
        int z1 = cz - 11;
        int z2 = cz + 7;
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
        // 门：朝西门洞
        for (int y = 1; y <= 3; y++) {
            k.set(x1, y, z1 + 3, null);
            k.set(x1, y, z1 + 4, null);
        }
        // 值更的那八格：床、桌、灯、牌
        k.set(x2 - 2, 1, z2 - 2, Palette.WOOL_RED);
        k.set(x2 - 3, 1, z2 - 2, Palette.WOOL_RED);
        k.set(x2 - 2, 1, z1 + 2, Palette.OAK_PLANK);
        k.set(x2 - 2, 2, z1 + 2, Palette.LANTERN);
        k.set(x1 + 1, 1, z1 + 6, Palette.CHEST);
        k.set(x1 + 1, 2, z1 + 6, Palette.SIGN);
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
        // 入口：南侧一道下行梯，从沙地边缘下去
        for (int i = 0; i <= 3; i++) {
            for (int w = -1; w <= 1; w++) {
                k.set(cx + w, -1 - i, cz + 35 - i, null);
                k.set(cx + w, -2 - i, cz + 35 - i, Palette.STONE_BRICK);
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
