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
        templeInterior(k);
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

        // 2) 外皮：最下两层做盲拱，上面做壁柱。
        // 一座宫殿的基座之所以像"城"，是因为挡土墙上有一整圈拱龛——远看是明暗相间的
        // 带子，把一面大白墙分成几百个小格子。三格宽的壁柱只是竖线，拱龛才有"洞"。
        if (index <= 1) {
            blindArcade(k, r, y0);
        } else {
            for (int side = 0; side < 4; side++) {
                for (int along = -(r - 2); along <= r - 2; along += 4) {
                    int px = side < 2 ? along : (side == 2 ? -r : r);
                    int pz = side < 2 ? (side == 0 ? -r : r) : along;
                    for (int y = y0 + 1; y < y0 + TIER_H; y++) {
                        k.set(px, y, pz,
                                y == y0 + TIER_H - 1 ? Palette.STONE_CHISELED : Palette.STONE_BRICK);
                    }
                }
            }
        }

        int topY = y0 + TIER_H;
        if (next <= 0) {
            return;
        }

        // 2.5) 台身实体：平台底下那一圈原来是空的。
        //
        // 外壳只有三格厚，平台却从内圈一直铺到外沿——中间那段悬在半空，
        // 远看是"一片片浮板叠上去"，近看能从缝里看进塔内。
        // 真正的金字塔是实心砌体：除了留给房间的内圈，其余都是石头。
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                int d = Math.max(Math.abs(x), Math.abs(z));
                if (d <= next || d > r - SHELL) {
                    continue;
                }
                for (int y = y0; y < topY; y++) {
                    k.set(x, y, z, Palette.SANDSTONE);
                }
            }
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
                // 垫实到本层台面为止。垫到地面就错了——第二层以上的台阶会一路
                // 落成一根实心柱子，把塔心整个填死（量出来 y=0 到 7 全是砂岩）
                for (int down = y - 1; down >= y0; down--) {
                    k.set(x, down, z, Palette.SANDSTONE);
                }
                for (int up = 1; up <= 3; up++) {
                    k.set(x, y + up, z, null);
                }
            }
            // 两侧栏墙：一层石、一层凿制，顶端留出净空
            for (int x : new int[] {-5, 5}) {
                k.set(x, y, z, Palette.SANDSTONE);
                k.set(x, y + 1, z, Palette.STONE_CHISELED);
                for (int down = y - 1; down >= y0; down--) {
                    k.set(x, down, z, Palette.SANDSTONE);
                }
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

    /**
     * 金字塔内部：一条中轴大梯，六级楼层，每层一种职能。
     *
     * <p>原来的塔是个空壳——七层台地只有外皮，里面从底到顶是空的。远看是一座山，
     * 走进去是一个洞。一座神权机构不可能没有内部：底层的信众前厅、往上的祭器库、
     * 档案、议事、神谕、大祭司居所，最后一级通到顶坛。
     *
     * <p>所以这一版做三件事：
     * <ol>
     *   <li><b>楼板</b>——每级台地顶面按内圈尺寸铺满，塔才有"层"；</li>
     *   <li><b>中轴大梯</b>——五格宽一道直梯从底层大厅直上顶坛前厅，穿过每层楼板的地方
     *       开洞，走上去不用拐弯、不用找门；</li>
     *   <li><b>每层四间房</b>——四个角上各一间石刻的房间，职能各不相同，门朝中庭开。</li>
     * </ol>
     */
    private static void templeInterior(BuildKit k) {
        // 底层大厅（y 0..7）：两列柱、铺地、南面一道门
        int hall = BASE - SHELL - 1;
        for (int x = -hall; x <= hall; x++) {
            for (int z = -hall; z <= hall; z++) {
                k.set(x, -1, z, Math.abs(x) <= 3
                        ? Palette.STONE_CHISELED : Palette.SANDSTONE);
            }
        }
        for (int z = -hall + 8; z <= hall - 10; z += 10) {
            for (int x : new int[] {-26, 26}) {
                k.fill(x - 1, 0, z - 1, x + 1, 0, z + 1, Palette.STONE_CHISELED);
                k.fill(x, 1, z, x, 7, z, Palette.STONE_BRICK);
                k.set(x, 7, z, Palette.GOLD);
            }
        }
        // 地牢与服务点都要在大厅铺好之后再做：地牢是从大厅往下挖的
        // （放在最后，顺序写反会把刚挖的洞重新铺上地板）

        // 正门开在<b>东面</b>，不开南面。
        //
        // 南面是那条直上顶坛的大台阶——台阶从广场一格一格爬到 y=8，
        // 底层的 y=0 正好被它整条压在下面。门开在南面，等于开在楼梯底下。
        // 朝圣者从东侧进底层大厅，仪式性的那条路留给祭典。
        int wallX = BASE - 1;
        for (int x = wallX - 2; x <= wallX + 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int yy = 0; yy <= 1; yy++) {
                    k.set(x, yy, z, null);
                }
                k.set(x, -1, z, Palette.STONE_CHISELED);
            }
        }
        k.door(wallX, 0, 0, "east", "dark_oak", "圣座正门");
        for (int z : new int[] {-2, 2}) {
            for (int yy = 0; yy <= 2; yy++) {
                k.set(wallX + 1, yy, z, Palette.STONE_CHISELED);
            }
        }
        // 门前三级踏跺：广场比门槛低一格
        for (int step = 0; step < 3; step++) {
            for (int z = -2; z <= 2; z++) {
                k.set(wallX + 2 + step, -1 + step / 2, z, Palette.STONE_CHISELED);
            }
        }

        // 每级一道楼板 + 四个角上的房间；楼板在梯子经过的地方开洞
        String[] use = {"祭器库", "档案室", "议事厅", "神谕室", "大祭司居所", "顶坛前厅"};
        for (int t = 1; t <= 6; t++) {
            int y = t * TIER_H;
            int h = BASE - INSET * t - SHELL - 1;
            if (h < 6) {
                break;
            }
            // 楼板
            for (int x = -h; x <= h; x++) {
                for (int z = -h; z <= h; z++) {
                    if (x >= -12 && x <= -8 && Math.abs(z - stairZ(t)) <= 3) {
                        continue;
                    }
                    boolean band = Math.floorMod(x + z, 8) == 0;
                    k.set(x, y, z, band ? Palette.STONE_CHISELED : Palette.SANDSTONE);
                }
            }
            // 楼梯口：楼梯在每层都比房间边缘再深入四格（那四格在台身里），
            // 所以每层要开一条五格宽的短廊，从房间通到梯口
            for (int x = -12; x <= -8; x++) {
                for (int z = h - 2; z <= stairZ(t) + 2; z++) {
                    for (int yy = y + 1; yy <= y + 3; yy++) {
                        k.set(x, yy, z, null);
                    }
                    k.set(x, y, z, Palette.SANDSTONE);
                }
            }

            // 四角房间：正方形，门朝中庭
            int c = h - 6;
            if (c >= 8) {
                int[][] corners = {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}};
                for (int[] q : corners) {
                    cell(k, q[0] * c, q[1] * c, y, use[Math.min(t - 1, use.length - 1)]);
                }
            }
        }

        // 中轴大梯：五格宽，一站一格，从底层大厅直上顶坛。
        //
        // 上一版从 y=9 起坡——那是二层楼板的高度，底层大厅到这道梯之间没有路，
        // 整座塔等于上不去。现在从大厅地面（y=1）起步，一路爬到 56。
        for (int z = 61; z >= 4; z--) {
            int y = 1 + (61 - z);
            if (y > TIERS * TIER_H) {
                break;
            }
            for (int x = -12; x <= -8; x++) {
                for (int up = 0; up <= 3; up++) {
                    k.set(x, y + up, z, null);
                }
                k.set(x, y - 1, z, Palette.SANDSTONE);
                k.set(x, y, z, x == -12 || x == -8 ? Palette.STONE_CHISELED
                        : Palette.STONE_BRICK);
            }
            // 每八格一站：一盏灯 + 两侧栏墙
            if (Math.floorMod(z, 8) == 0) {
                for (int x : new int[] {-13, -7}) {
                    k.set(x, y, z, Palette.STONE_CHISELED);
                    k.set(x, y + 1, z, Palette.LANTERN);
                }
            }
        }
        // 梯顶两级：顶坛在 y=56，梯子在 z=6 才爬到 56。z=4、5 补两块，
        // 人从梯顶一步迈上平台，而不是踩空掉回下一层
        for (int z = 4; z <= 6; z++) {
            for (int x = -12; x <= -8; x++) {
                k.set(x, TIERS * TIER_H, z, Palette.STONE_CHISELED);
            }
        }

        servicePoints(k);

        // 窗：每级台身四面各开几扇，光落在楼梯与房间里
        for (int t = 0; t < TIERS; t++) {
            int r = BASE - t * INSET;
            int y0 = t * TIER_H;
            for (int side = 0; side < 4; side++) {
                for (int along = -(r - 8); along <= r - 8; along += 12) {
                    if (Math.abs(along) < 8) {
                        continue;
                    }
                    int px = side < 2 ? along : (side == 2 ? -r : r);
                    int pz = side < 2 ? (side == 0 ? -r : r) : along;
                    for (int yy = y0 + 5; yy <= y0 + 6; yy++) {
                        k.set(px, yy, pz, Palette.GLASS_GRAY);
                    }
                }
            }
        }
    }

    /**
     * 圣堂地牢，以及五处"办事的地方"。
     *
     * <p>地牢是这一轮唯一一处<b>往地下挖</b>的建筑。它必须用 {@code excavate} 而不是
     * {@code carve}：carve 只是"场景里不画这一格"，落地器只写场景里有的方块，
     * 所以在图上挖了个洞、进服一挖还是实心石头。excavate 会把它记成"明确挖掉"。
     *
     * <p>红石用块不用矿：一块红石块挖下来 9 个红石，任何镐都挖得动。
     * 矿石要铁镐，一个刚进服、身上只有木镐的人会卡在这儿。
     */
    /**
     * 地牢要从"整座城都铺完"之后才挖，所以它由 {@link MedievalCity} 在最后调用。
     *
     * <p>这一条是被量出来的：地牢原本写在塔内构造里，结果 {@code fillGaps} 后来
     * 又把地面填缝填回去了——梯口那一格重新变成实心，人下不去，而图上完全正常。
     */
    static void undercroft(BuildKit k) {
        int x1 = TempleSpots.DUNGEON_X1;
        int x2 = TempleSpots.DUNGEON_X2;
        int z1 = TempleSpots.DUNGEON_Z1;
        int z2 = TempleSpots.DUNGEON_Z2;
        int f = TempleSpots.DUNGEON_FLOOR;

        // 先砌实一整块，再把当中挖空——比"四壁各砌一遍"少一半代码，也不会漏角
        k.fill(x1, f, z1, x2, f + 5, z2, Palette.STONE_BRICK);
        k.excavate(x1 + 1, f + 1, z1 + 1, x2 - 1, f + 4, z2 - 1);

        // 血石：嵌在北墙与东西两壁。一块 9 个红石，够 8 个的门槛
        for (int x = x1 + 4; x <= x2 - 4; x += 6) {
            k.set(x, f + 2, z1, Palette.REDSTONE_BLOCK);
            k.set(x, f + 3, z1, Palette.REDSTONE_BLOCK);
        }
        for (int z = z1 + 6; z <= z2 - 6; z += 8) {
            k.set(x1, f + 2, z, Palette.REDSTONE_BLOCK);
            k.set(x2, f + 2, z, Palette.REDSTONE_BLOCK);
        }

        // 灯：地牢里没有光就没有"往下走"的感觉
        for (int x = x1 + 6; x <= x2 - 6; x += 10) {
            for (int z = z1 + 6; z <= z2 - 6; z += 10) {
                k.set(x, f + 4, z, Palette.LANTERN);
            }
        }
        // 几间空牢房：铁栅一列，给"地牢"一个说法
        for (int z = z1 + 4; z <= z1 + 12; z++) {
            k.set(x1 + 6, f + 1, z, Palette.IRON_BARS);
            k.set(x1 + 6, f + 2, z, Palette.IRON_BARS);
        }
        k.set(x1 + 6, f + 1, z1 + 8, null);
        k.set(x1 + 6, f + 2, z1 + 8, null);

        // -- 下行梯与通道，必须放在石室砌完之后 --
        //
        // 顺序写反过一次：先把通道挖开、再砌石室，石室那面南墙又把通道封上了。
        // 结果就是"图上有梯、走过去掉不下去"，测试一量就红。
        for (int i = 0; i < 8; i++) {
            int y = -2 - i;
            int z = 30 - i;
            for (int x = -26; x <= -24; x++) {
                k.set(x, y, z, Block.stairs("minecraft:stone_brick_stairs", "north"));
                k.excavate(x, y + 1, z, x, y + 3, z);
            }
        }
        // 梯口：大厅地面在三格宽的地方开一个口
        k.excavate(-26, -1, 30, -24, -1, 31);
        // 梯底接石室：打通南墙（z=22），并把 z=23 这一段也留空。
        // 从 f+1 起：f 那一层是最后一级梯的踏面，挖掉就等于把人悬在梯口
        k.excavate(-26, f + 1, 22, -24, f + 4, 23);

        // 地牢里那只箱子必须放在挖空之后：放早了会被上面那一步 excavate 一起挖掉
        k.set(TempleSpots.DUNGEON_CHEST.x(), TempleSpots.DUNGEON_CHEST.y(),
                TempleSpots.DUNGEON_CHEST.z(), Palette.CHEST);
    }

    /**
     * 五处服务点的"物件"。箱子、讲经台、座椅各就各位，人由插件站在旁边。
     *
     * <p>坐标全部来自 {@link TempleSpots}——建造和插件判定必须用同一份数字，
     * 各写一遍的结果是"点了没反应"，而图上完全正常。
     */
    private static void servicePoints(BuildKit k) {
        k.set(TempleSpots.WAREHOUSE_CHEST.x(), TempleSpots.WAREHOUSE_CHEST.y(),
                TempleSpots.WAREHOUSE_CHEST.z(), Palette.CHEST);
        k.set(TempleSpots.LIBRARY_LECTERN.x(), TempleSpots.LIBRARY_LECTERN.y(),
                TempleSpots.LIBRARY_LECTERN.z(), Palette.LECTERN);
        k.set(TempleSpots.COUNCIL_SEAT.x(), TempleSpots.COUNCIL_SEAT.y(),
                TempleSpots.COUNCIL_SEAT.z(),
                Block.stairs("minecraft:stone_brick_stairs", "east"));
    }

    /** 这一层的楼梯在哪个 z 上（楼板要在这里开洞）。 */
    private static int stairZ(int t) {
        return 62 - TIER_H * t;
    }

    /** 一角上的房间：四面墙、一道门、一张桌一盏灯，按职能放东西。 */
    private static void cell(BuildKit k, int cx, int cz, int y, String use) {
        int half = 5;
        k.walls(cx - half, y + 1, cz - half, cx + half, y + 5, cz + half, Palette.STONE_BRICK);
        k.carve(cx - half + 1, y + 1, cz - half + 1, cx + half - 1, y + 4, cz + half - 1);
        // 门朝中庭：选离原点最近的那一面
        String side = Math.abs(cx) > Math.abs(cz)
                ? (cx > 0 ? "west" : "east") : (cz > 0 ? "north" : "south");
        String wood = "dark_oak";
        switch (side) {
            case "west" -> k.door(cx - half, y + 1, cz, "west", wood, use);
            case "east" -> k.door(cx + half, y + 1, cz, "east", wood, use);
            case "north" -> k.door(cx, y + 1, cz - half, "north", wood, use);
            default -> k.door(cx, y + 1, cz + half, "south", wood, use);
        }
        // 家什：每个职能一套，别让六间房长得一样
        switch (use) {
            case "祭器库" -> {
                for (int i = -2; i <= 2; i += 2) {
                    k.set(cx + i, y + 1, cz + 3, Palette.CHEST);
                    k.set(cx + i, y + 1, cz - 3, Palette.BARREL);
                }
                k.set(cx, y + 2, cz, Palette.GOLD);
            }
            case "档案室" -> {
                for (int i = -3; i <= 3; i++) {
                    k.fill(cx + i, y + 1, cz - 4, cx + i, y + 3, cz - 4, Palette.BOOKSHELF);
                }
                k.set(cx, y + 1, cz + 2, Palette.LECTERN);
                k.set(cx + 2, y + 1, cz + 2, Palette.LECTERN);
            }
            case "议事厅" -> {
                k.fill(cx - 3, y + 1, cz - 1, cx + 3, y + 1, cz + 1, Palette.DARK_PLANK);
                for (int[] p : new int[][] {{-3, -3}, {3, -3}, {-3, 3}, {3, 3}}) {
                    k.set(cx + p[0], y + 1, cz + p[1], Block.stairs("minecraft:oak_stairs", "east"));
                }
            }
            case "神谕室" -> {
                k.fill(cx - 2, y + 1, cz - 2, cx + 2, y + 1, cz + 2, Palette.STONE_CHISELED);
                k.set(cx, y + 2, cz, Palette.GOLD);
                for (int[] p : new int[][] {{-3, 0}, {3, 0}, {0, -3}, {0, 3}}) {
                    k.set(cx + p[0], y + 1, cz + p[1], Palette.CANDLE);
                }
            }
            case "大祭司居所" -> {
                k.fill(cx - 2, y + 1, cz - 1, cx + 2, y + 1, cz + 1, Palette.WOOL_RED);
                k.set(cx + 4, y + 1, cz - 4, Palette.CHEST);
                k.set(cx - 4, y + 1, cz + 4, Palette.LECTERN);
            }
            default -> {
                k.set(cx, y + 1, cz, Palette.STONE_CHISELED);
                k.set(cx, y + 2, cz, Palette.GOLD);
            }
        }
        k.set(cx, y + 5, cz, Palette.LANTERN_HANGING);
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
     * 台身外皮的盲拱：三格宽、四格高、凿进一格，顶上压一块楼梯当拱腹。
     *
     * <p>只在最下两层做。越往上拱越小越少，是真实的做法——为了省钱，
     * 也因为远看时上面本来就看不见几个洞。
     */
    private static void blindArcade(BuildKit k, int r, int y0) {
        String[] face = {"south", "north", "east", "west"};
        for (int side = 0; side < 4; side++) {
            for (int along = -(r - 4); along <= r - 4; along += 6) {
                // 正中间让开：那里是大台阶与壁柱
                if (Math.abs(along) < 10) {
                    continue;
                }
                for (int w = -1; w <= 1; w++) {
                    int px = side < 2 ? along + w : (side == 2 ? -r : r);
                    int pz = side < 2 ? (side == 0 ? -r : r) : along + w;
                    for (int h = 1; h <= 4; h++) {
                        k.set(px, y0 + h, pz, null);
                    }
                    k.set(px, y0 + 5, pz,
                            Block.stairs("minecraft:stone_brick_stairs", face[side]));
                }
            }
        }
    }

    /**
     * 塔顶祭坛：一座四面敞开的圆亭，十二根柱子，一顶金穹隆。
     *
     * <p>原来是"四根方柱 + 五层收分的方锥铜顶"。方的东西在轮廓上立不住——从远处看，
     * 一座城的顶点必须是一个<b>曲面</b>，眼睛才认得出"这是顶"。所以换成圆亭加真穹隆，
     * 顶上再插一根金针：全城唯一的暖金就在这儿，和下面七层砂岩台地拉开。
     */
    /**
     * 顶冠：圆亭 + 金穹顶 + 金针，外加台角一座鼓形副塔。
     *
     * <p>这三件是照着"一个宽台基、中段碎、顶上一点金"那个轮廓配的：
     * 圆亭给曲面，穹顶给高光，金针把视线往上带，副塔让顶点旁边还有一个次高，
     * 不至于孤零零一根。少了副塔，顶上那点金会被七层台地的宽度压住。
     */
    private static void crown(BuildKit k, int y) {
        int ring = 12;
        // 台座：只做两圈窄沿。
        //
        // 原来是两块实心圆盘（半径 14 与 13），把中心整个铺平在 y=57、58 上——
        // 祭坛的头两级正好埋在这两层里，走上去只看见一块圆地。窄沿把中心让出来，
        // 柱脚落在沿上，祭坛立在地面上。
        for (int x = -ring - 2; x <= ring + 2; x++) {
            for (int z = -ring - 2; z <= ring + 2; z++) {
                double r = Math.sqrt(x * x + z * z);
                if (r >= ring + 1 && r <= ring + 2) {
                    k.set(x, y + 1, z, Palette.STONE_CHISELED);
                } else if (r >= ring && r < ring + 1) {
                    k.set(x, y + 2, z, Palette.SANDSTONE);
                }
            }
        }
        // 十二根柱子，柱头贴金
        for (int i = 0; i < 12; i++) {
            double a = i / 12.0 * Math.PI * 2;
            int px = (int) Math.round(Math.cos(a) * ring);
            int pz = (int) Math.round(Math.sin(a) * ring);
            k.fill(px, y + 3, pz, px, y + 10, pz, Palette.STONE_CHISELED);
            k.set(px, y + 11, pz, Palette.GOLD);
            k.set(px, y + 1, pz, Palette.STONE_BRICK);
            k.set(px, y + 2, pz, Palette.SANDSTONE);
        }
        // 环梁：柱头之上一圈砂岩，亭子才有"檐"
        for (int i = 0; i < 96; i++) {
            double a = i / 96.0 * Math.PI * 2;
            int px = (int) Math.round(Math.cos(a) * ring);
            int pz = (int) Math.round(Math.sin(a) * ring);
            k.set(px, y + 11, pz, Palette.SANDSTONE);
            k.set(px, y + 12, pz, Palette.SANDSTONE);
        }
        // 穹顶：十一层半球，整体贴金，只有最下两圈是铜绿——
        // 雨水从顶上流下来，先锈的是檐口那一圈，不是顶心。而且从空中看，
        // 看见的是顶心：顶心要是绿的，这顶冠就白做了
        for (int i = 0; i <= 10; i++) {
            double t = i / 10.0;
            int rr = (int) Math.round((ring + 2) * Math.sqrt(Math.max(0, 1 - t * t)));
            k.disc(0, 0, rr, y + 13 + i, i <= 1 ? Palette.COPPER_OXIDIZED : Palette.GOLD);
        }
        // 金针
        k.fill(0, y + 25, 0, 0, y + 27, 0, Palette.GOLD);
        k.set(0, y + 28, 0, Palette.GOLD);

        // 副塔：台角一座鼓形塔，铜绿小穹顶
        int tx = 15;
        int tz = 15;
        for (int yy = y + 1; yy <= y + 14; yy++) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (d <= 3.4 && d > 2.2) {
                        k.set(tx + dx, yy, tz + dz,
                                yy % 6 == 0 ? Palette.STONE_CHISELED : Palette.STONE_BRICK);
                    }
                }
            }
        }
        k.disc(tx, tz, 3, y + 15, Palette.SANDSTONE);
        for (int i = 0; i <= 4; i++) {
            k.disc(tx, tz, Math.max(0, 4 - i), y + 16 + i, Palette.COPPER_OXIDIZED);
        }
        k.set(tx, y + 21, tz, Palette.GOLD);
    }

    /**
     * 塔顶祭坛：一座圆亭底下的圣所。
     *
     * <p>原来是"三级台 + 一块金 + 四只火盆"——从空中看是座祭坛，站上去是一块空地。
     * 祭坛是这座城里唯一"人神交界"的地方，它必须有仪式上说得通的层次：
     * 从台阶上来先过一道栏杆，走过一段毯子，上三级台阶，才到祭台；
     * 祭台四周八根小柱撑一圈华盖，华盖底下只放得下一块金。
     */
    private static void altar(BuildKit k) {
        int y = TIERS * TIER_H;
        int r = 18;

        // 顶面：同心圆环铺装，外沿半砖压顶
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                // 大梯上来的那一段要留空——不然爬到顶上一头撞进地板
                if (x >= -12 && x <= -8 && z >= 4 && z <= 9) {
                    continue;
                }
                int d = Math.max(Math.abs(x), Math.abs(z));
                int ring = (int) Math.round(Math.sqrt(x * x + z * z));
                Block block;
                if (d == r) {
                    block = Block.slab("minecraft:smooth_sandstone_slab", "bottom");
                } else if (ring % 4 == 0) {
                    block = Palette.STONE_CHISELED;
                } else {
                    block = Palette.SANDSTONE;
                }
                k.set(x, y, z, block);
            }
        }

        // 仪式道：从大梯口一直铺到祭台，红线毯 + 两侧铜钉
        for (int z = 4; z <= 15; z++) {
            k.set(0, y, z, Palette.CARPET_RED);
            for (int x : new int[] {-1, 1}) {
                k.set(x, y, z, Palette.STONE_CHISELED);
            }
        }

        // 栏杆：把"圣所"围一圈，只在南面留一道口
        for (int i = 0; i < 72; i++) {
            double a = i / 72.0 * Math.PI * 2;
            int px = (int) Math.round(Math.cos(a) * 11);
            int pz = (int) Math.round(Math.sin(a) * 11);
            // 南面留两处口：正中一处给红毯，西侧一处给大梯上来的人
            if (pz > 0 && (Math.abs(px) <= 1 || (px >= -13 && px <= -7))) {
                continue;
            }
            k.set(px, y + 1, pz, Palette.STONE_CHISELED);
            k.set(px, y + 2, pz, Palette.GOLD);
        }

        // 祭台：四级，顶上一圈金、正中一格钻石块
        //
        // 正中那块是**转职祭坛的触发块**，材料不能随便换：mmSupernaturals 只认
        // DIAMOND_BLOCK（Priest.Church.AltarMaterial）。原来这里放的是金块，而金块
        // 恰好是吸血鬼祭坛的触发块——放错料，右键就永远没有反应，图上也看不出来。
        //
        // 上面原本还压着一支蜡烛，右键顶面会命中蜡烛而不是钻石块，一并去掉。
        k.fill(-4, y + 1, -4, 4, y + 1, 4, Palette.STONE_CHISELED);
        k.fill(-3, y + 2, -3, 3, y + 2, 3, Palette.SANDSTONE);
        k.fill(-2, y + 3, -2, 2, y + 3, 2, Palette.STONE_CHISELED);
        k.fill(-1, y + 4, -1, 1, y + 4, 1, Palette.GOLD);
        k.set(0, y + 4, 0, Palette.DIAMOND);
        // 四角刻纹：凿制的竖槽，让台身不是一块整板
        for (int[] c : new int[][] {{-4, -4}, {4, -4}, {-4, 4}, {4, 4}}) {
            k.fill(c[0], y + 1, c[1], c[0], y + 1, c[1], Palette.STONE_BRICK);
        }
        // 上台的三级踏跺（南面正中）
        for (int step = 0; step < 3; step++) {
            k.fill(-2, y + 1, 5 + step, 2, y + 1 + step, 5 + step, Palette.STONE_CHISELED);
        }

        // 华盖：八根细柱，柱头贴金，围住祭台
        for (int i = 0; i < 8; i++) {
            double a = i / 8.0 * Math.PI * 2;
            int px = (int) Math.round(Math.cos(a) * 6);
            int pz = (int) Math.round(Math.sin(a) * 6);
            k.fill(px, y + 1, pz, px, y + 7, pz, Palette.STONE_CHISELED);
            k.set(px, y + 8, pz, Palette.GOLD);
        }
        // 华盖顶：一圈砂岩环梁 + 中心一盏吊灯
        for (int i = 0; i < 64; i++) {
            double a = i / 64.0 * Math.PI * 2;
            int px = (int) Math.round(Math.cos(a) * 6);
            int pz = (int) Math.round(Math.sin(a) * 6);
            k.set(px, y + 9, pz, Palette.SANDSTONE);
        }
        k.fill(-2, y + 9, -2, 2, y + 9, 2, Palette.SANDSTONE);
        k.set(0, y + 8, 0, Palette.LANTERN_HANGING);

        // 供桌：四张，靠栏杆，各摆一份
        for (int[] p : new int[][] {{-7, 6}, {7, 6}, {-7, -6}, {7, -6}}) {
            k.fill(p[0] - 1, y + 1, p[1], p[0] + 1, y + 1, p[1], Palette.DARK_PLANK);
            k.set(p[0], y + 2, p[1], Math.floorMod(p[0] + p[1], 2) == 0
                    ? Palette.CHEST : Palette.CANDLE);
        }

        // 顶冠：圆亭 + 金穹顶 + 金针
        crown(k, y);

        // 四角火盆：立在台座上的铜盆，不是地上的一堆火
        for (int[] c : new int[][] {{-14, -14}, {14, -14}, {-14, 14}, {14, 14}}) {
            k.fill(c[0], y + 1, c[1], c[0], y + 1, c[1], Palette.STONE_BRICK);
            k.fill(c[0], y + 2, c[1], c[0], y + 3, c[1], Palette.STONE_CHISELED);
            k.set(c[0], y + 4, c[1], Palette.CAMPFIRE);
        }

        // 两侧的水池：挪到柱廊外面，不然十二根柱子会泡在水里
        for (int side = -1; side <= 1; side += 2) {
            for (int x = 13; x <= 16; x++) {
                for (int z = -2; z <= 2; z++) {
                    k.set(x * side, y, z, Palette.WATER);
                }
            }
            k.set(side * 12, y + 1, 0, Palette.STONE_CHISELED);
            k.set(side * 17, y, 0, Palette.STONE_CHISELED);
        }

        // 钟：挂在北侧
        k.fill(-2, y + 6, -14, 2, y + 6, -14, Palette.DARK_LOG);
        k.set(0, y + 5, -14, Palette.BELL);

        // 大梯出口：最后清一条五格宽的通道。
        //
        // 亭子的台座窄沿、栏杆、祭坛前的地面，都是按"静态好看"排的；
        // 大梯从西侧上来，出口正好落在它们的中间。这一条在最后清，
        // 因为前面任何一道工序都可能在这里再放一块石头——上一版就是这么堵死的。
        for (int x = -12; x <= -8; x++) {
            for (int z = 2; z <= 14; z++) {
                for (int yy = y + 1; yy <= y + 2; yy++) {
                    k.set(x, yy, z, null);
                }
            }
            // z=9 这一排不能垫：楼梯在 z=9 是第 53 级，人站在 54，
            // 垫一块 55 就把自己的头顶堵了（这条反复踩了三次，最后靠量方块才看出来）
        }
        // 通道口铺上毯子，人一上来就知道该往哪走。
        // 只铺在平台那一段（z≥10）——z=4..9 下面还是梯级，铺了就把楼梯堵死
        for (int z = 10; z <= 12; z++) {
            k.set(-10, y, z, Palette.CARPET_RED);
        }
    }

    /**
     * 王座大厅：三级台地之上的一座殿堂。
     *
     * <p>上一版有三个硬伤，都是照着渲染图能一眼看出来的：
     * <ol>
     *   <li><b>断层</b>——台地最高一级在 y=10，而大厅的地板却摆在 y=15。中间五格是空的，
     *       整座建筑悬在台地上，从侧面能看见一条贯穿全长的黑影。</li>
     *   <li><b>像个盒子</b>——九十三格长、五十一格深、一个平屋顶加一圈雉堞，
     *       远看就是一只盖了盖的箱子。没有门廊、没有两翼、没有坡顶。</li>
     *   <li><b>台地是空场</b>——三级台面只有一片灰石与几道横带，没有栏杆、没有壁面，
     *       走上去像停车场。</li>
     * </ol>
     *
     * <p>这一版按"台基 + 主体 + 两翼 + 门廊 + 坡顶 + 后塔"六件重新排：
     * 地板落在台面上（y=11），主体收窄到六十九格、加一道双坡顶；
     * 两侧各接一座两层的矮翼，正面挑出一座八柱门廊与山花；
     * 台地本身加挡土墙盲拱、栏杆与十七格宽的大台阶。
     */
    static void throneHall(BuildKit k) {
        int[] zSteps = {-150, -192, -238, -298};
        int[] widths = {78, 64, 50};

        // -- 1) 三级台地 ---------------------------------------------------
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
            // 台面铺装：中轴一条深色带，两侧每十二格一道横缝
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    if (Math.abs(x) <= 3) {
                        k.set(x, y, z, Palette.STONE_CHISELED);
                    } else if (Math.floorMod(z, 12) == 0) {
                        k.set(x, y, z, Palette.ANDESITE);
                    }
                }
            }
            // 挡土墙：盲拱一圈。一面大白墙分成几百个小格子，远看才是"城"，不是"台"
            for (int x = x1 + 4; x <= x2 - 4; x += 8) {
                if (Math.abs(x) < 10) {
                    continue;
                }
                for (int w = 0; w <= 3; w++) {
                    for (int yy = y - 3; yy <= y - 1; yy++) {
                        k.set(x + w, yy, z2, null);
                    }
                    k.set(x + w, y - 4, z2, Palette.STONE_BRICK);
                }
            }
            // 栏杆：台沿一圈，每八格一根金顶望柱
            for (int x = x1; x <= x2; x++) {
                for (int z : new int[] {z1, z2}) {
                    if (Math.abs(x) <= 9) {
                        continue;
                    }
                    k.set(x, y + 1, z, Palette.STONE_CHISELED);
                    if (Math.floorMod(x, 8) == 0) {
                        k.set(x, y + 2, z, Palette.STONE_BRICK);
                        k.set(x, y + 3, z, Palette.GOLD);
                    }
                }
            }
            for (int z = z1; z <= z2; z++) {
                for (int x : new int[] {x1, x2}) {
                    k.set(x, y + 1, z, Palette.STONE_CHISELED);
                    if (Math.floorMod(z, 8) == 0) {
                        k.set(x, y + 2, z, Palette.STONE_BRICK);
                        k.set(x, y + 3, z, Palette.GOLD);
                    }
                }
            }
            // 十七格宽的大台阶：两侧有栏墙与火盆
            for (int step = 1; step <= 5; step++) {
                for (int x = -8; x <= 8; x++) {
                    k.set(x, y - 5 + step, z2 + 6 - step, Palette.STONE_CHISELED);
                    for (int up = 1; up <= 3; up++) {
                        k.set(x, y - 5 + step + up, z2 + 6 - step, null);
                    }
                }
                for (int x : new int[] {-9, 9}) {
                    k.set(x, y - 5 + step, z2 + 6 - step, Palette.STONE_BRICK);
                    k.set(x, y - 4 + step, z2 + 6 - step, Palette.STONE_CHISELED);
                }
            }
            for (int x : new int[] {x1 + 6, x2 - 6}) {
                k.fill(x, y + 1, z2 + 3, x, y + 2, z2 + 3, Palette.STONE_BRICK);
                k.set(x, y + 3, z2 + 3, Palette.CAMPFIRE);
            }
        }

        // -- 2) 主体：地板落在最高一级台地上（断层就是这里补掉的）-------------
        int floor = 11;
        int wallTop = 30;
        int hx = 34;
        int z1 = -286;
        int z2 = -244;

        // 台基：地板往下砌满，与台面接上
        k.fill(-hx - 2, 10, z1 - 2, hx + 2, 10, z2 + 2, Palette.STONE_CHISELED);
        k.fill(-hx, floor, z1, hx, floor, z2, Palette.STONE_CHISELED);

        k.walls(-hx, floor + 1, z1, hx, wallTop, z2, Palette.STONE_BRICK);
        k.carve(-hx + 1, floor + 1, z1 + 1, hx - 1, wallTop - 1, z2 - 1);

        // -- 3) 两翼：两层矮楼，比主体低八格 ---------------------------------
        for (int side = -1; side <= 1; side += 2) {
            int wx1 = side > 0 ? hx + 2 : -hx - 16;
            int wx2 = side > 0 ? hx + 16 : -hx - 2;
            int wz1 = z1 + 4;
            int wz2 = z2 - 6;
            k.walls(wx1, floor + 1, wz1, wx2, floor + 11, wz2, Palette.STONE_BRICK);
            k.carve(wx1 + 1, floor + 1, wz1 + 1, wx2 - 1, floor + 10, wz2 - 1);
            // 翼楼的门朝中庭（朝主体那一侧）
            int dx = side > 0 ? wx1 : wx2;
            k.door(dx, floor + 1, (wz1 + wz2) / 2, side > 0 ? "west" : "east",
                    "dark_oak", "王座厅侧翼");
            // 翼楼正面的窗：两层各三扇
            for (int i = 0; i < 3; i++) {
                int z = wz1 + 6 + i * 8;
                for (int yy : new int[] {floor + 3, floor + 7}) {
                    k.set(side > 0 ? wx2 : wx1, yy, z, Palette.GLASS_GRAY);
                    k.set(side > 0 ? wx2 : wx1, yy + 1, z, Palette.GLASS_GRAY);
                }
            }
            // 翼楼坡顶
            k.hipRoof(wx1, wz1, wx2, wz2, floor + 12, 5, Palette.SLATE_STAIR,
                    Palette.SLATE, 1);
            // 与主体之间的连廊
            k.fill(side > 0 ? hx : -hx - 1, floor + 1, (wz1 + wz2) / 2 - 2,
                    side > 0 ? hx + 1 : -hx, floor + 4, (wz1 + wz2) / 2 + 2, null);
        }

        // -- 4) 正门廊：八柱、额枋、山花 -------------------------------------
        //
        // 门廊只有六格深，原因很实在：它再往南伸，就盖到最高一级台地的大台阶头上了。
        // 台阶从 y=5 一格一格爬到 y=10，头顶正好是门廊的地板——人爬到第八级就撞头。
        // 所以门廊退到台面标高（y=10），台阶爬到台面就接上，中间不再有悬空的地板。
        int pz1 = z2 + 1;
        int pz2 = z2 + 6;
        // 门廊地面：与最高一级台面同高，只是换了铺装
        for (int z = pz1; z <= pz2; z++) {
            for (int x = -15; x <= 15; x++) {
                k.set(x, floor - 1, z, Math.abs(x) <= 3 ? Palette.CARPET_RED
                        : Palette.STONE_CHISELED);
            }
        }
        // 八根柱：站在台面上，柱头贴金
        for (int i = 0; i < 8; i++) {
            int x = -14 + i * 4;
            k.fill(x - 1, floor, pz2 - 1, x + 1, floor, pz2, Palette.STONE_CHISELED);
            k.fill(x, floor + 1, pz2 - 1, x, floor + 14, pz2, Palette.STONE_CHISELED);
            k.set(x, floor + 15, pz2 - 1, Palette.GOLD);
            k.set(x, floor + 15, pz2, Palette.GOLD);
        }
        // 额枋与檐口
        k.fill(-16, floor + 16, pz1, 16, floor + 16, pz2 + 1, Palette.STONE_BRICK);
        k.fill(-16, floor + 17, pz1, 16, floor + 17, pz2 + 1, Palette.STONE_CHISELED);
        k.fill(-16, floor + 18, pz1, 16, floor + 18, pz2, Palette.STONE_BRICK);
        // 山花：实心三角，起十二格收成尖
        int pedRise = 12;
        int pedHalf = 16;
        for (int i = 0; i <= pedRise; i++) {
            int xa = -pedHalf + i;
            int xb = pedHalf - i;
            for (int x = xa; x <= xb; x++) {
                for (int z = pz1; z <= pz2; z++) {
                    k.set(x, floor + 19 + i, z, Palette.STONE_BRICK);
                }
            }
            for (int z = pz1; z <= pz2; z++) {
                k.set(xa, floor + 19 + i, z, Palette.STONE_CHISELED);
                k.set(xb, floor + 19 + i, z, Palette.STONE_CHISELED);
            }
        }
        for (int x = -2; x <= 2; x++) {
            for (int yy = floor + 21; yy <= floor + 24; yy++) {
                k.set(x, yy, pz2, Palette.GLASS_GRAY);
            }
        }
        k.set(0, floor + 25, pz2, Palette.GOLD);
        k.fill(-2, floor + 19 + pedRise, pz1, 2, floor + 19 + pedRise, pz2,
                Palette.STONE_CHISELED);
        k.set(0, floor + 20 + pedRise, pz2, Palette.GOLD);
        // 正门：两格高，装深色橡木门扇
        k.door(0, floor + 1, z2, "south", "dark_oak", "王座厅正门");
        for (int x : new int[] {-2, 2}) {
            for (int yy = floor + 1; yy <= floor + 3; yy++) {
                k.set(x, yy, z2, Palette.STONE_CHISELED);
            }
        }
        k.set(0, floor + 3, z2, Palette.STONE_CHISELED);

        // -- 5) 坡顶：双坡，脊上一条金线 -------------------------------------
        // 坡高九格：七十一格宽的屋面若按"进深一半"起坡，会高出后塔一大截
        k.hipRoof(-hx, z1, hx, z2, wallTop + 1, 9, Palette.SLATE_STAIR,
                Palette.SLATE, 2);
        for (int z = z1; z <= z2; z++) {
            k.set(0, wallTop + 10, z, Palette.GOLD);
        }
        // 采光亭：屋脊上一排小窗，白天有光落进大厅
        for (int z = z1 + 8; z <= z2 - 8; z += 8) {
            k.set(-1, wallTop + 9, z, Palette.GLASS_GRAY);
            k.set(1, wallTop + 9, z, Palette.GLASS_GRAY);
        }

        // -- 6) 后塔 ----------------------------------------------------------
        int tz = -272;
        k.fill(-9, floor + 1, tz - 9, 9, wallTop + 6, tz + 9, Palette.STONE_BRICK);
        k.carve(-8, floor + 1, tz - 8, 8, wallTop + 5, tz + 8);
        // 塔的南面开一道大券。塔不是"立在厅里的一根柱子"——它是王座的龛：
        // 原来四面封死，五级台阶与金座都砌在里头，从大厅看不见也走不进。
        k.carve(-6, floor + 1, tz + 9, 6, floor + 9, tz + 9);
        k.carve(-5, floor + 1, tz + 10, 5, floor + 8, tz + 10);
        k.crenellate(-9, tz - 9, 9, tz + 9, wallTop + 7, Palette.STONE_BRICK);
        for (int[] b : new int[][] {{-9, tz - 9}, {9, tz - 9}, {-9, tz + 9}, {9, tz + 9}}) {
            k.fill(b[0], floor + 1, b[1], b[0], wallTop + 6, b[1], Palette.STONE_CHISELED);
        }
        k.cone(0, tz, 10, wallTop + 8, 10, Palette.SLATE);
        k.set(0, wallTop + 18, tz, Palette.GOLD);

        // -- 7) 内装 ----------------------------------------------------------
        // 地面：红毯直通王座
        for (int x = -hx + 1; x <= hx - 1; x++) {
            for (int z = z1 + 1; z <= z2 - 1; z++) {
                k.set(x, floor, z, Math.abs(x) <= 3 ? Palette.CARPET_RED : Palette.STONE_CHISELED);
            }
        }
        // 内柱廊：两列，每八格一根，柱础放大、柱头贴金
        for (int x : new int[] {-22, 22}) {
            for (int z = z1 + 10; z <= z2 - 10; z += 8) {
                k.fill(x - 1, floor + 1, z - 1, x + 1, floor + 1, z + 1, Palette.STONE_CHISELED);
                k.fill(x, floor + 2, z, x, wallTop - 2, z, Palette.STONE_BRICK);
                k.set(x, wallTop - 1, z, Palette.GOLD);
            }
        }
        // 二层跑马廊：两侧各一条，栏板在外、柱在内，楼梯在南端
        for (int side = -1; side <= 1; side += 2) {
            int gx1 = side > 0 ? 28 : -hx + 1;
            int gx2 = side > 0 ? hx - 1 : -28;
            k.fill(gx1, floor + 9, z1 + 1, gx2, floor + 9, z2 - 1, Palette.DARK_PLANK);
            for (int z = z1 + 1; z <= z2 - 1; z++) {
                k.set(side > 0 ? gx1 : gx2, floor + 10, z, Palette.STONE_CHISELED);
            }
            // 楼梯：贴墙，从地面斜上跑马廊
            for (int i = 0; i < 8; i++) {
                int z = z2 - 4 - i;
                int yy = floor + 1 + i;
                int cx2 = side > 0 ? hx - 3 : -hx + 3;
                k.set(cx2, yy, z, Block.stairs("minecraft:stone_brick_stairs",
                        side > 0 ? "east" : "west"));
                k.set(cx2, yy - 1, z, Palette.STONE_BRICK);
                k.set(cx2, floor + 9, z, null);
            }
        }
        // 天花：方格梁
        for (int x = -hx + 1; x <= hx - 1; x++) {
            for (int z = z1 + 1; z <= z2 - 1; z++) {
                boolean beam = Math.floorMod(x, 8) == 0 || Math.floorMod(z, 8) == 0;
                k.set(x, wallTop, z, beam ? Palette.DARK_PLANK : Palette.STONE_CHISELED);
            }
        }
        for (int z = z1 + 6; z <= z2 - 6; z += 8) {
            k.set(0, wallTop - 1, z, Palette.LANTERN_HANGING);
        }
        // 侧墙高窗 + 壁柱
        for (int z = z1 + 4; z <= z2 - 4; z += 6) {
            for (int x : new int[] {-hx + 1, hx - 1}) {
                k.fill(x, floor + 1, z, x, wallTop - 2, z, Palette.STONE_CHISELED);
            }
            if (Math.floorMod(z - z1, 12) == 0) {
                k.fill(-hx + 1, floor + 4, z, -hx + 1, floor + 8, z, Palette.WOOL_RED);
                k.fill(hx - 1, floor + 4, z, hx - 1, floor + 8, z, Palette.WOOL_RED);
            }
        }
        for (int z = z1 + 9; z <= z2 - 9; z += 8) {
            for (int x : new int[] {-hx, hx}) {
                k.fill(x, wallTop - 6, z, x, wallTop - 4, z, Palette.GLASS_GRAY);
            }
        }

        // -- 8) 王座 ----------------------------------------------------------
        // 王座龛：五级台从大厅地面一级一级抬到金座。
        //
        // 原来是五块各自五格深的实心台，从 z=-266 铺到 -262，而金座摆在 -274——
        // 座在半空，台阶又堵在门口，人走进券洞就上不去。现在改成"一级一格、
        // 越往上越窄"，每一级都从龛门口一路铺到座前。
        int seatZ = -274;
        for (int i = 0; i < 5; i++) {
            int y = floor + 1 + i;
            int zs = -262 - 3 * i;
            k.fill(-9 + i, y, zs - 2, 9 - i, y, zs, Palette.STONE_CHISELED);
        }
        // 金座与背屏
        k.fill(-2, 17, seatZ, 2, 18, seatZ + 2, Palette.GOLD);
        k.fill(-3, 20, seatZ - 2, 3, 26, seatZ - 2, Palette.STONE_CHISELED);
        for (int x : new int[] {-6, 6}) {
            k.fill(x, floor + 6, seatZ + 7, x, floor + 7, seatZ + 7, Palette.STONE_BRICK);
            k.set(x, floor + 8, seatZ + 7, Palette.CAMPFIRE);
        }
        // 王座两侧的旗
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < 3; i++) {
                int x = side * (12 + i * 6);
                k.fill(x, floor + 1, z1 + 4, x, floor + 12, z1 + 4, Palette.SPRUCE_FENCE);
                k.fill(x, floor + 9, z1 + 4, x, floor + 11, z1 + 4, Palette.WOOL_RED);
            }
        }

        // -- 9) 仪式道：两列雕像与灯 ------------------------------------------
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
