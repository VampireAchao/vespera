package io.github.vampireachao.vespera.core.world;

import io.github.vampireachao.vespera.core.voxel.Block;
import io.github.vampireachao.vespera.core.voxel.Palette;

/**
 * 城里"过日子"的部分：市集、酒馆、铁匠铺、澡堂、码头、粮仓、兵营、墓地、竞技场、
 * 风车与农田。
 *
 * <p>地标决定一座城好不好看，这些东西决定它像不像有人住。所以它们都不大，但必须
 * 都有在运转的痕迹：铁匠铺的炉子点着、码头的吊杆挂着货、田里有麦子、墓地的碑
 * 不是一样高。
 */
final class Civic {

    private Civic() {
    }

    // -- 市集广场 -----------------------------------------------------------

    static void market(BuildKit k) {
        for (int x = -70; x <= 70; x++) {
            for (int z = -70; z <= 70; z++) {
                double d = Math.sqrt(x * x + z * z);
                Block block;
                if (d < 9) {
                    block = Palette.STONE_CHISELED;
                } else if (d < 12) {
                    block = Palette.ANDESITE_POLISHED;
                } else if (((int) (d / 6)) % 2 == 0) {
                    block = Palette.STONE_BRICK;
                } else {
                    block = Palette.ANDESITE;
                }
                if (k.rnd.nextInt(100) < 6) {
                    block = Palette.STONE_CRACKED;
                }
                k.set(x, -1, z, block);
            }
        }

        // 喷泉：三级水盘
        k.disc(0, 0, 8, 0, Palette.STONE_BRICK);
        k.disc(0, 0, 6, 0, Palette.WATER);
        // 中央立柱与金顶。上层不摆水：悬空的水会流下来，把广场变成浅滩。
        k.disc(0, 0, 3, 1, Palette.STONE_CHISELED);
        k.disc(0, 0, 2, 2, Palette.STONE_CHISELED);
        k.set(0, 3, 0, Palette.STONE_CHISELED);
        k.set(0, 4, 0, Palette.GOLD);
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > 7 && d < 8.6) {
                    k.set(dx, 1, dz, Palette.STONE_BRICK);
                }
            }
        }

        // 市集十字：石座、柱、十字
        k.fill(-2, 0, 44, 2, 2, 48, Palette.STONE_CHISELED);
        k.fill(0, 3, 46, 0, 10, 46, Palette.STONE_BRICK);
        k.fill(-2, 8, 46, 2, 8, 46, Palette.STONE_BRICK);

        // 摊位：东西两列，各六个
        Block[][] cloth = {
                {Palette.WOOL_RED, Palette.WOOL_CREAM},
                {Palette.WOOL_BLUE, Palette.WOOL_CREAM},
                {Palette.WOOL_GREEN, Palette.WOOL_YELLOW},
                {Palette.WOOL_YELLOW, Palette.WOOL_RED}};
        int index = 0;
        for (int z = -60; z <= 52; z += 16) {
            for (int side = -1; side <= 1; side += 2) {
                int x = side * 36;
                if (Math.abs(z) < 10) {
                    continue;
                }
                Block[] pair = cloth[index++ % cloth.length];
                k.stall(x - 3, z, pair[0], pair[1]);
            }
        }
        // 内圈再摆一排小摊：市场要挤，才有市集的样子
        for (int z = -52; z <= 44; z += 20) {
            for (int side = -1; side <= 1; side += 2) {
                if (Math.abs(z) < 8) {
                    continue;
                }
                Block[] pair = cloth[index++ % cloth.length];
                k.stall(side * 20 - 3, z, pair[0], pair[1]);
            }
        }
        // 两侧的常设市场棚：石基、木柱、瓦顶，风雨也开市
        for (int side = -1; side <= 1; side += 2) {
            int x = side * 58;
            for (int i = 0; i < 3; i++) {
                int z = -44 + i * 34;
                k.fill(x - 5, -1, z, x + 5, -1, z + 22, Palette.STONE_BRICK);
                for (int dz = 0; dz <= 22; dz += 11) {
                    k.fill(x - 4, 0, z + dz, x - 4, 4, z + dz, Palette.OAK_LOG);
                    k.fill(x + 4, 0, z + dz, x + 4, 4, z + dz, Palette.OAK_LOG);
                }
                k.roof(x - 7, z - 2, x + 7, z + 24, 5, true, Palette.TILE_STAIR,
                        Palette.OAK_PLANK, 1);
                k.set(x, 1, z + 6, Palette.BARREL);
                k.set(x, 1, z + 16, Palette.CRATE);
            }
        }
        // 边缘的花坛与树
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < 5; i++) {
                int x = side * 66;
                int z = -50 + i * 25;
                k.tree(x, z, 5 + (i % 3), false);
            }
        }

        // 广场上的车、草料、货堆：没有它们，广场就只是一块铺得比较好的地
        for (int i = 0; i < 6; i++) {
            int cx = -58 + i * 23;
            for (int dx = 0; dx < 3; dx++) {
                k.set(cx + dx, 0, 24, Palette.OAK_PLANK);
                k.set(cx + dx, 1, 24, i % 2 == 0 ? Palette.HAY : Palette.BARREL);
            }
            k.set(cx, 0, 22, Palette.SPRUCE_FENCE);
            k.set(cx + 2, 0, 22, Palette.SPRUCE_FENCE);
        }
        for (int i = 0; i < 8; i++) {
            int bx = -50 + i * 14;
            k.set(bx, 0, -30, i % 3 == 0 ? Palette.BARREL : Palette.CRATE);
        }
        // 四角的旗杆
        for (int x : new int[] {-60, 60}) {
            for (int z : new int[] {-60, 60}) {
                k.fill(x, 0, z, x, 9, z, Palette.SPRUCE_FENCE);
                k.fill(x, 7, z, x, 9, z, Palette.WOOL_RED);
            }
        }

        // 边角的灯与货
        for (int x : new int[] {-64, 64}) {
            for (int z : new int[] {-64, 64}) {
                k.lamp(x, z);
            }
        }
        for (int i = 0; i < 10; i++) {
            int x = k.rnd.nextInt(120) - 60;
            int z = k.rnd.nextInt(120) - 60;
            if (Math.abs(x) < 14 && Math.abs(z) < 14) {
                continue;
            }
            k.set(x, 0, z, k.rnd.nextBoolean() ? Palette.BARREL : Palette.CRATE);
        }
        k.bump("市集");
    }

    /**
     * 一间临街铺子：底楼敞开、布棚遮阳，门外摆它这一行该有的东西。
     *
     * <p>城的"生活感"不来自房子数量，来自门口那几样东西——面包房门口是面粉袋，
     * 酒馆门口是酒桶，铁匠门口是水桶。走过一条街，看门口就知道住着谁。
     */
    static void shop(BuildKit k, int x, int z, String front, String type) {
        k.house(x, z, 16, 12, 2, front, true);
        int cx = x + 8;
        int cz = z + 6;
        int ox = 0;
        int oz = 0;
        switch (front) {
            case "west" -> ox = -18;
            case "east" -> ox = 18;
            case "north" -> oz = -14;
            default -> oz = 14;
        }
        int px = cx + ox;
        int pz = cz + oz;
        switch (type) {
            case "bakery" -> {
                k.set(px, 0, pz, Palette.FURNACE);
                k.set(px + 1, 0, pz, Palette.HAY);
                k.set(px + 2, 0, pz, Palette.HAY);
                k.set(px - 1, 0, pz, Palette.BARREL);
            }
            case "brewery" -> {
                for (int i = 0; i < 4; i++) {
                    k.set(px + i, 0, pz, Palette.BARREL);
                    k.set(px + i, 1, pz, i % 2 == 0 ? Palette.BARREL : null);
                }
            }
            case "butcher" -> {
                k.set(px, 0, pz, Palette.CAMPFIRE);
                k.set(px + 1, 0, pz, Palette.CAULDRON);
                k.fill(px + 3, 0, pz, px + 3, 2, pz, Palette.SPRUCE_FENCE);
                k.set(px + 3, 3, pz, Palette.WOOL_RED);
            }
            case "fish" -> {
                k.set(px, 0, pz, Palette.BARREL);
                k.set(px + 1, 0, pz, Palette.CAULDRON);
                k.set(px + 2, 0, pz, Palette.BARREL);
            }
            case "tailor" -> {
                k.set(px, 0, pz, Palette.WOOL_BLUE);
                k.set(px + 1, 0, pz, Palette.WOOL_RED);
                k.set(px + 2, 0, pz, Palette.WOOL_CREAM);
                k.set(px, 1, pz, Palette.WOOL_GREEN);
            }
            case "apothecary", "potter" -> {
                k.set(px, 0, pz, Palette.CAULDRON);
                k.set(px + 1, 0, pz, Palette.POT);
                k.set(px + 2, 0, pz, Palette.CLAY);
            }
            case "chandler" -> {
                k.fill(px, 0, pz, px, 2, pz, Palette.SPRUCE_FENCE);
                k.set(px, 3, pz, Palette.LANTERN);
                k.set(px + 2, 0, pz, Palette.BARREL);
            }
            case "weaver" -> {
                k.set(px, 0, pz, Palette.CRAFTING);
                k.set(px + 1, 0, pz, Palette.WOOL_CREAM);
            }
            default -> {
                k.set(px, 0, pz, Palette.CAULDRON);
                k.set(px + 1, 0, pz, Palette.WATER);
                k.set(px + 2, 0, pz, Palette.BARREL);
            }
        }
        // 店招
        k.fill(px, 3, pz, px, 5, pz, Palette.SPRUCE_FENCE);
        k.set(px, 4, pz, Palette.SIGN);
        k.bump("铺子");
    }

    // -- 酒馆 ---------------------------------------------------------------

    static void tavern(BuildKit k, int x, int z) {
        k.house(x, z, 44, 30, 3, "north", false);
        // 招牌
        k.fill(x + 2, 19, z - 1, x + 2, 19, z - 3, Palette.DARK_FENCE);
        k.fill(x + 1, 17, z - 3, x + 3, 19, z - 3, Palette.WOOL_RED);
        k.set(x + 2, 16, z - 3, Palette.LANTERN_HANGING);
        // 门前长凳与酒桶
        for (int i = 0; i < 4; i++) {
            k.set(x + 6 + i * 8, 0, z - 4, Palette.SPRUCE_FENCE);
            k.set(x + 6 + i * 8, 1, z - 4, Palette.OAK_PLANK);
        }
        k.set(x + 4, 0, z - 6, Palette.BARREL);
        k.set(x + 5, 0, z - 6, Palette.BARREL);
        k.set(x + 36, 0, z - 5, Palette.CAMPFIRE);
        k.bump("酒馆");
    }

    // -- 铁匠铺 -------------------------------------------------------------

    static void smithy(BuildKit k, int x, int z) {
        int w = 30;
        int d = 22;
        k.fill(x - 1, -3, z - 1, x + w, -1, z + d, Palette.COBBLE);
        // 三面墙，正面敞开
        k.walls(x, 0, z, x + w - 1, 7, z + d - 1, Palette.COBBLE);
        k.carve(x, 0, z, x + w - 1, 6, z + d - 1);
        k.carve(x + 2, 0, z + d - 1, x + w - 3, 4, z + d - 1);
        k.roof(x - 2, z - 2, x + w + 1, z + d + 1, 8, false, Palette.SLATE_STAIR,
                Palette.COBBLE, 1);
        // 炉子与烟囱
        k.fill(x + 5, 0, z + 3, x + 9, 3, z + 7, Palette.DEEPSLATE_BRICK);
        k.fill(x + 6, 1, z + 4, x + 8, 2, z + 6, Palette.CAMPFIRE);
        k.fill(x + 6, 8, z + 4, x + 8, 16, z + 6, Palette.COBBLE);
        // 铁砧、水桶、工作台
        k.set(x + 14, 0, z + 5, Palette.ANVIL);
        k.set(x + 16, 0, z + 5, Palette.CAULDRON);
        k.set(x + 19, 0, z + 4, Palette.SMITHING);
        k.set(x + 12, 0, z + 8, Palette.BARREL);
        // 门口挂铁器
        for (int i = 0; i < 3; i++) {
            k.set(x + 3 + i * 3, 4, z + d, Palette.CHAIN);
            k.set(x + 3 + i * 3, 3, z + d, Palette.IRON);
        }
        k.bump("铁匠铺");
    }

    // -- 澡堂 ---------------------------------------------------------------

    static void bathhouse(BuildKit k, int x, int z) {
        k.fill(x - 2, -3, z - 2, x + 42, -1, z + 32, Palette.STONE_BRICK);
        k.walls(x, 0, z, x + 40, 14, z + 30, Palette.STONE_BRICK);
        k.carve(x + 1, 0, z + 1, x + 39, 13, z + 29);
        // 水池
        k.fill(x + 8, -1, z + 8, x + 32, -1, z + 22, Palette.WATER);
        k.walls(x + 7, 0, z + 7, x + 33, 0, z + 23, Palette.STONE_CHISELED);
        // 柱廊
        for (int i = 0; i < 4; i++) {
            for (int dz : new int[] {z + 3, z + 27}) {
                k.fill(x + 5 + i * 10, 0, dz, x + 5 + i * 10, 10, dz, Palette.STONE_CHISELED);
            }
        }
        k.fill(x, 11, z, x + 40, 11, z + 30, Palette.STONE_BRICK);
        for (int dx = 4; dx < 40; dx += 8) {
            for (int dz = 4; dz < 30; dz += 8) {
                k.set(x + dx, 12, z + dz, Palette.GLASS);
            }
        }
        k.roof(x - 2, z - 2, x + 42, z + 32, 12, true, Palette.TILE_STAIR, Palette.STONE_BRICK, 1);
        k.set(x + 2, 3, z + 2, Palette.CAMPFIRE);
        k.bump("澡堂");
    }

    // -- 图书馆 -------------------------------------------------------------

    static void library(BuildKit k, int x, int z) {
        k.fill(x - 2, -3, z - 2, x + 46, -1, z + 32, Palette.STONE_BRICK);
        k.walls(x, 0, z, x + 44, 18, z + 30, Palette.STONE_BRICK);
        k.carve(x + 1, 0, z + 1, x + 43, 17, z + 29);
        // 高窗
        for (int dx = 5; dx < 44; dx += 8) {
            k.fill(x + dx, 4, z, x + dx + 2, 13, z, Palette.GLASS_GRAY);
            k.fill(x + dx, 4, z + 30, x + dx + 2, 13, z + 30, Palette.GLASS_GRAY);
        }
        // 书墙
        for (int dx = 2; dx < 44; dx += 3) {
            k.fill(x + dx, 1, z + 2, x + dx, 9, z + 2, Palette.BOOKSHELF);
            k.fill(x + dx, 1, z + 28, x + dx, 9, z + 28, Palette.BOOKSHELF);
        }
        k.roof(x - 2, z - 2, x + 46, z + 32, 19, false, Palette.SLATE_STAIR, Palette.STONE_BRICK, 1);
        // 门廊
        k.fill(x + 18, 0, z - 4, x + 26, 0, z - 1, Palette.STONE_BRICK);
        for (int dz = -4; dz < 0; dz++) {
            k.set(x + 18, 6, z + dz, Palette.STONE_CHISELED);
            k.set(x + 26, 6, z + dz, Palette.STONE_CHISELED);
        }
        k.fill(x + 18, 7, z - 4, x + 26, 7, z - 1, Palette.STONE_BRICK);
        k.bump("图书馆");
    }

    // -- 粮仓与兵营 ---------------------------------------------------------

    static void granary(BuildKit k, int x, int z) {
        k.fill(x, -1, z, x + 36, 0, z + 26, Palette.STONE_BRICK);
        // 架空的地板，防潮防鼠
        for (int dx = 2; dx < 36; dx += 6) {
            for (int dz = 2; dz < 26; dz += 6) {
                k.fill(x + dx, 0, z + dz, x + dx, 2, z + dz, Palette.SPRUCE_LOG);
            }
        }
        k.fill(x + 1, 3, z + 1, x + 35, 3, z + 25, Palette.SPRUCE_PLANK);
        k.walls(x + 1, 4, z + 1, x + 35, 16, z + 25, Palette.SPRUCE_PLANK);
        k.carve(x + 2, 4, z + 2, x + 34, 15, z + 24);
        for (int dx = 4; dx < 34; dx += 5) {
            k.fill(x + dx, 4, z + 25, x + dx, 9, z + 25, Palette.PLASTER);
        }
        k.roof(x - 1, z - 1, x + 37, z + 27, 17, true, Palette.SHINGLE_STAIR, Palette.SPRUCE_PLANK, 1);
        for (int i = 0; i < 8; i++) {
            k.set(x + 4 + (i % 4) * 7, 4, z + 4 + (i / 4) * 8, Palette.HAY);
        }
        k.bump("粮仓");
    }

    static void barracks(BuildKit k, int x, int z) {
        k.fill(x - 1, -3, z - 1, x + 41, -1, z + 21, Palette.COBBLE);
        k.walls(x, 0, z, x + 40, 9, z + 20, Palette.STONE_BRICK);
        k.carve(x + 1, 0, z + 1, x + 39, 8, z + 19);
        for (int dx = 3; dx < 38; dx += 5) {
            k.fill(x + dx, 3, z, x + dx, 5, z, Palette.GLASS_GRAY);
            k.fill(x + dx, 3, z + 20, x + dx, 5, z + 20, Palette.GLASS_GRAY);
        }
        k.roof(x - 2, z - 2, x + 42, z + 22, 10, true, Palette.TILE_STAIR, Palette.STONE_BRICK, 1);
        // 训练场：木桩与草人
        k.pave(x + 4, z + 26, x + 36, z + 42, Palette.COARSE, Palette.PATH);
        for (int dx = 6; dx < 36; dx += 8) {
            k.fill(x + dx, 0, z + 30, x + dx, 3, z + 30, Palette.SPRUCE_LOG);
            k.set(x + dx, 4, z + 30, Palette.HAY);
            k.set(x + dx, 5, z + 30, Palette.WOOL_YELLOW);
            k.set(x + dx, 6, z + 30, Palette.HAY);
        }
        k.fill(x + 8, 0, z + 38, x + 34, 0, z + 40, Palette.OAK_PLANK);
        k.bump("兵营");
    }

    // -- 码头 ---------------------------------------------------------------

    static void docks(BuildKit k) {
        // 运河：偏在西侧，南门的大路要能直着走出去
        for (int x = -250; x <= -94; x++) {
            for (int z = 306; z <= 404; z++) {
                k.fill(x, -5, z, x, -1, z, Palette.WATER);
            }
        }
        // 石砌岸壁
        for (int z = 304; z <= 406; z++) {
            for (int x : new int[] {-252, -92}) {
                k.fill(x, -5, z, x, 0, z, Palette.STONE_BRICK);
            }
        }
        k.pave(-252, 296, -92, 308, Palette.COBBLE, Palette.GRAVEL);
        // 从南门沿墙根过来的一条码头路
        k.pave(-96, 286, 0, 294, Palette.PATH, Palette.GRAVEL);

        // 仓库：三座，门朝码头
        for (int i = 0; i < 3; i++) {
            int x = -244 + i * 46;
            int z = 250;
            k.fill(x - 1, -3, z - 1, x + 41, -1, z + 36, Palette.COBBLE);
            k.walls(x, 0, z, x + 40, 14, z + 34, Palette.STONE_BRICK);
            k.carve(x + 1, 0, z + 1, x + 39, 13, z + 33);
            k.roof(x - 2, z - 2, x + 42, z + 36, 15, true, Palette.TILE_STAIR, Palette.STONE_BRICK, 2);
            k.carve(x + 16, 0, z + 34, x + 24, 9, z + 34);
            k.fill(x + 15, 10, z + 34, x + 25, 10, z + 34, Palette.DARK_PLANK);
            k.set(x + 20, 11, z + 34, Palette.CHAIN);
            for (int j = 0; j < 6; j++) {
                k.set(x + 4 + j * 6, 0, z + 30, j % 2 == 0 ? Palette.BARREL : Palette.CRATE);
            }
        }

        // 吊杆：两根木柱、一条横臂、一根链
        for (int i = 0; i < 3; i++) {
            int x = -220 + i * 60;
            int z = 302;
            k.fill(x, 0, z, x, 9, z, Palette.SPRUCE_LOG);
            k.fill(x, 9, z, x + 10, 9, z, Palette.SPRUCE_LOG);
            k.set(x + 10, 8, z, Palette.CHAIN);
            k.set(x + 10, 7, z, Palette.CHAIN);
            k.set(x + 10, 6, z, Palette.BARREL);
        }

        // 船：三条平底驳船
        for (int i = 0; i < 3; i++) {
            int bx = -210 + i * 48;
            int bz = 340 + (i * 13) % 40;
            k.fill(bx, -2, bz, bx + 16, -1, bz + 7, Palette.DARK_PLANK);
            k.fill(bx + 1, -1, bz + 1, bx + 15, -1, bz + 6, Palette.WATER);
            k.fill(bx, 0, bz, bx + 16, 0, bz, Palette.DARK_PLANK);
            k.fill(bx, 0, bz + 7, bx + 16, 0, bz + 7, Palette.DARK_PLANK);
            k.fill(bx, 0, bz, bx, 0, bz + 7, Palette.DARK_PLANK);
            k.fill(bx + 16, 0, bz, bx + 16, 0, bz + 7, Palette.DARK_PLANK);
            k.fill(bx + 7, 0, bz + 3, bx + 7, 8, bz + 3, Palette.SPRUCE_FENCE);
            k.fill(bx + 4, 8, bz + 3, bx + 10, 9, bz + 3, Palette.WOOL_CREAM);
        }
        k.bump("码头");
    }

    // -- 墓地 ---------------------------------------------------------------

    static void graveyard(BuildKit k, int x, int z, int w, int d) {
        k.pave(x, z, x + w, z + d, Palette.GRASS, Palette.PATH);
        // 铁栅栏
        for (int i = 0; i <= w; i++) {
            k.set(x + i, 0, z, Palette.IRON);
            k.set(x + i, 1, z, Palette.IRON);
        }
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 4; j++) {
                int sx = x + 6 + i * 8;
                int sz = z + 8 + j * 12;
                boolean tall = k.rnd.nextInt(3) == 0;
                k.fill(sx, 0, sz, sx + 1, tall ? 2 : 1, sz, Palette.STONE_CRACKED);
                k.set(sx, tall ? 3 : 2, sz, Palette.STONE_MOSSY);
            }
        }
        // 小礼拜堂
        int cx = x + w / 2 - 9;
        int cz = z + d - 22;
        k.walls(cx, 0, cz, cx + 18, 10, cz + 14, Palette.STONE_BRICK);
        k.carve(cx + 1, 0, cz + 1, cx + 17, 9, cz + 13);
        k.roof(cx - 2, cz - 2, cx + 20, cz + 16, 11, false, Palette.SLATE_STAIR,
                Palette.STONE_BRICK, 1);
        Landmarks.pointedWindow(k, cx + 9, 3, cz, 2, 4, "north");
        k.fill(cx + 8, 0, cz, cx + 10, 3, cz, Palette.DARK_PLANK);
        // 紫杉
        k.tree(x + 4, z + d - 6, 5, true);
        k.tree(x + w - 5, z + 6, 6, true);
        k.bump("墓地");
    }

    // -- 竞技场 -------------------------------------------------------------

    static void arena(BuildKit k, int cx, int cz, int radius) {
        for (int y = 0; y <= 4; y++) {
            k.pave(cx - radius, cz - radius, cx + radius, cz + radius, Palette.SAND, Palette.COARSE);
        }
        // 看台：一圈圈抬高的石阶
        for (int r = radius - 10; r <= radius; r += 3) {
            int y = (r - (radius - 10)) / 3;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (d <= r && d > r - 3) {
                        k.set(cx + dx, y, cz + dz, Palette.STONE_BRICK);
                        k.set(cx + dx, y + 1, cz + dz, Palette.ANDESITE_POLISHED);
                    }
                }
            }
        }
        // 外墙与门洞
        for (int dx = -radius - 2; dx <= radius + 2; dx++) {
            for (int dz = -radius - 2; dz <= radius + 2; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > radius && d <= radius + 2) {
                    boolean gate = Math.abs(dx) < 4 || Math.abs(dz) < 4;
                    if (!gate) {
                        k.fill(cx + dx, 0, cz + dz, cx + dx, 9, cz + dz, Palette.STONE_BRICK);
                    }
                }
            }
        }
        k.crenellate(cx - radius - 2, cz - radius - 2, cx + radius + 2, cz + radius + 2, 10,
                Palette.STONE_BRICK);
        // 四角旗
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                int px = cx + sx * (radius - 4);
                int pz = cz + sz * (radius - 4);
                k.fill(px, 5, pz, px, 16, pz, Palette.SPRUCE_FENCE);
                k.fill(px, 12, pz, px, 16, pz, Palette.WOOL_RED);
            }
        }
        k.bump("竞技场");
        k.bump("地标");
    }

    // -- 风车 ---------------------------------------------------------------

    static void windmill(BuildKit k, int cx, int cz) {
        for (int y = 0; y < 22; y++) {
            for (int dx = -7; dx <= 7; dx++) {
                for (int dz = -7; dz <= 7; dz++) {
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (d <= 7 && d > 5.6) {
                        k.set(cx + dx, y, cz + dz, y % 6 == 0 ? Palette.STONE_CHISELED : Palette.STONE_BRICK);
                    }
                }
            }
        }
        k.cone(cx, cz, 7, 22, 8, Palette.SLATE);
        k.fill(cx - 2, 0, cz + 7, cx - 2, 4, cz + 7, Palette.DARK_PLANK);
        // 四叶：木框 + 布帆
        for (int i = 3; i < 24; i++) {
            k.set(cx + i, 20, cz, Palette.SPRUCE_LOG);
            k.set(cx - i, 20, cz, Palette.SPRUCE_LOG);
            k.set(cx, 20 + i, cz, Palette.SPRUCE_LOG);
            k.set(cx, 20 - i, cz, Palette.SPRUCE_LOG);
            if (i > 5) {
                k.set(cx + i, 21, cz, Palette.WOOL_CREAM);
                k.set(cx - i, 21, cz, Palette.WOOL_CREAM);
                k.set(cx, 20 + i, cz + 1, Palette.WOOL_CREAM);
                k.set(cx, 20 - i, cz + 1, Palette.WOOL_CREAM);
            }
        }
        k.set(cx, 20, cz, Palette.DARK_LOG);
        k.bump("风车");
    }

    // -- 农田 ---------------------------------------------------------------

    static void farm(BuildKit k, int x, int z, int w, int d) {
        for (int dx = 0; dx < w; dx++) {
            for (int dz = 0; dz < d; dz++) {
                int worldX = x + dx;
                int worldZ = z + dz;
                if (dx % 9 == 0) {
                    k.set(worldX, -1, worldZ, Palette.WATER);
                    continue;
                }
                k.set(worldX, -1, worldZ, Palette.FARMLAND);
                Block crop = switch ((dx / 9) % 4) {
                    case 0 -> Palette.WHEAT;
                    case 1 -> Palette.CARROT;
                    case 2 -> Palette.POTATO;
                    default -> Palette.BEET;
                };
                if (dz % 7 != 0) {
                    k.set(worldX, 0, worldZ, crop);
                }
            }
        }
        // 篱笆
        for (int dx = -1; dx <= w; dx++) {
            k.set(x + dx, 0, z - 1, Palette.SPRUCE_FENCE);
            k.set(x + dx, 0, z + d, Palette.SPRUCE_FENCE);
        }
        for (int dz = -1; dz <= d; dz++) {
            k.set(x - 1, 0, z + dz, Palette.SPRUCE_FENCE);
            k.set(x + w, 0, z + dz, Palette.SPRUCE_FENCE);
        }
        // 稻草人
        int sx = x + w / 2;
        int sz = z + d / 2;
        k.fill(sx, 0, sz, sx, 2, sz, Palette.SPRUCE_FENCE);
        k.set(sx, 3, sz, Palette.WOOL_YELLOW);
        k.set(sx - 1, 2, sz, Palette.SPRUCE_FENCE);
        k.set(sx + 1, 2, sz, Palette.SPRUCE_FENCE);
        k.bump("农田");
    }

    static void barn(BuildKit k, int x, int z) {
        k.fill(x - 1, -1, z - 1, x + 27, 0, z + 19, Palette.COBBLE);
        k.walls(x, 1, z, x + 26, 10, z + 18, Palette.SPRUCE_PLANK);
        k.carve(x + 1, 1, z + 1, x + 25, 9, z + 17);
        k.carve(x + 10, 1, z + 18, x + 16, 6, z + 18);
        k.roof(x - 2, z - 2, x + 28, z + 20, 11, true, Palette.SHINGLE_STAIR,
                Palette.SPRUCE_PLANK, 1);
        for (int i = 0; i < 5; i++) {
            k.set(x + 3 + i * 5, 1, z + 2, Palette.HAY);
        }
        k.bump("谷仓");
    }

    // -- 村落 ---------------------------------------------------------------

    static void hamlet(BuildKit k, int x, int z) {
        k.pave(x - 12, z - 12, x + 52, z + 48, Palette.GRASS, Palette.PATH);
        well(k, x + 20, z + 18);
        int[][] spots = {{0, 0}, {26, -4}, {0, 26}, {30, 26}, {52, 4}, {4, 44}};
        for (int[] s : spots) {
            k.house(x + s[0], z + s[1], 18, 13, 2, k.rnd.nextBoolean() ? "south" : "east", false);
        }
        // 小教堂
        k.walls(x + 34, 0, z + 36, x + 48, 9, z + 48, Palette.COBBLE);
        k.carve(x + 35, 0, z + 37, x + 47, 8, z + 47);
        k.roof(x + 32, z + 34, x + 50, z + 50, 10, true, Palette.SLATE_STAIR,
                Palette.COBBLE, 1);
        k.fill(x + 40, 10, z + 42, x + 40, 15, z + 42, Palette.STONE_BRICK);
        k.bump("村落");
    }

    static void well(BuildKit k, int x, int z) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                boolean rim = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                k.set(x + dx, 0, z + dz, rim ? Palette.STONE_BRICK : Palette.WATER);
            }
        }
        k.fill(x - 2, 1, z - 2, x + 2, 1, z - 2, Palette.OAK_PLANK);
        k.fill(x - 2, 1, z + 2, x + 2, 1, z + 2, Palette.OAK_PLANK);
        k.fill(x - 2, 2, z - 2, x - 2, 5, z - 2, Palette.SPRUCE_LOG);
        k.fill(x + 2, 2, z - 2, x + 2, 5, z - 2, Palette.SPRUCE_LOG);
        k.fill(x - 2, 5, z - 2, x + 2, 5, z + 2, Palette.SPRUCE_PLANK);
        k.set(x, 3, z, Palette.BARREL);
    }

    // -- 断弦：猎巫人公会的一层酒馆 -------------------------------------------

    /**
     * 公会的门面是酒馆。
     *
     * <p>一层横着一条穿堂巷（x 156..164），巷子中间一道铁门——门里是内厅，门外是酒馆。
     * 这条巷本来就是城里的街，只是正好从会所底下穿过去；把它留着，比封死更像真的。
     */
    static void guildTavern(BuildKit k) {
        int x1 = 130;
        int z1 = -74;
        int x2 = 192;
        int z2 = -14;

        // 二层楼板：不然楼上是个空壳
        for (int x = x1 + 1; x < x2; x++) {
            for (int z = z1 + 1; z < z2; z++) {
                k.set(x, 12, z, Palette.DARK_PLANK);
            }
        }

        // 内厅（巷西）：二十三格见方，净空二十×十二，一扇能关的门
        k.walls(x1 + 2, 0, z1 + 2, x1 + 24, 8, z1 + 30, Palette.PLASTER);
        k.carve(x1 + 3, 0, z1 + 3, x1 + 23, 7, z1 + 29);
        for (int y = 0; y <= 4; y++) {
            k.set(x1 + 24, y, z1 + 14, null);
            k.set(x1 + 24, y, z1 + 15, null);
        }
        for (int y = 0; y <= 4; y++) {
            k.set(x1 + 24, y, z1 + 16, Palette.DARK_PLANK);
        }
        // 内厅的陈设：一张长桌、两张弓架、一面名单墙
        k.fill(x1 + 6, 0, z1 + 12, x1 + 18, 0, z1 + 13, Palette.DARK_PLANK);
        for (int i = 0; i < 5; i++) {
            k.set(x1 + 7 + i * 2, 0, z1 + 10, Palette.OAK_FENCE);
            k.set(x1 + 7 + i * 2, 0, z1 + 15, Palette.OAK_FENCE);
        }
        for (int i = 0; i < 3; i++) {
            k.set(x1 + 5, 2, z1 + 6 + i * 8, Palette.SIGN);
            k.set(x1 + 5, 3, z1 + 6 + i * 8, Palette.SIGN);
        }
        k.set(x1 + 10, 0, z1 + 20, Palette.CHEST);

        // 铁门：巷中 z=-30，三格宽，门旁就是挂牌的墙
        for (int x = 158; x <= 162; x++) {
            for (int y = 0; y <= 4; y++) {
                k.set(x, y, -30, Palette.IRON_BARS);
            }
        }
        k.fill(157, 0, -30, 157, 5, -30, Palette.STONE_CHISELED);
        k.fill(163, 0, -30, 163, 5, -30, Palette.STONE_CHISELED);
        k.fill(157, 6, -30, 163, 6, -30, Palette.STONE_CHISELED);
        // 挂牌位：门两格内，三块
        k.set(155, 2, -30, Palette.SIGN);
        k.set(155, 3, -30, Palette.SIGN);
        k.set(155, 2, -28, Palette.SIGN);

        // 酒馆（巷东）：吧台靠北墙、三排长桌、壁炉、酒桶
        for (int x = 166; x <= 190; x++) {
            for (int z = z1 + 2; z <= z2 - 4; z++) {
                k.set(x, 0, z, Palette.DARK_PLANK);
            }
        }
        k.fill(166, 0, z1 + 2, 190, 1, z1 + 3, Palette.SPRUCE_PLANK);
        k.fill(166, 2, z1 + 3, 190, 2, z1 + 3, Palette.OAK_PLANK);
        for (int i = 0; i < 8; i++) {
            k.set(168 + i * 3, 0, z1 + 4, i % 2 == 0 ? Palette.BARREL : Palette.CAULDRON);
        }
        for (int i = 0; i < 3; i++) {
            k.fill(168, 0, z1 + 12 + i * 8, 186, 0, z1 + 13 + i * 8, Palette.OAK_PLANK);
            for (int dz = 0; dz <= 1; dz++) {
                k.set(169, 0, z1 + 10 + i * 8 + dz, Palette.OAK_FENCE);
                k.set(185, 0, z1 + 10 + i * 8 + dz, Palette.OAK_FENCE);
            }
        }
        // 壁炉
        k.fill(176, 0, z2 - 3, 180, 0, z2 - 2, Palette.COBBLE);
        k.set(178, 0, z2 - 2, Palette.CAMPFIRE);
        k.fill(178, 1, z2 - 2, 178, 6, z2 - 2, Palette.COBBLE);
        // 悬赏板：东墙
        for (int y = 2; y <= 4; y++) {
            for (int z = -44; z <= -34; z++) {
                k.set(190, y, z, Palette.SPRUCE_PLANK);
            }
        }
        k.set(190, 3, -42, Palette.SIGN);
        k.set(190, 3, -39, Palette.SIGN);
        k.set(190, 3, -36, Palette.SIGN);
        // 后门通靶场
        for (int y = 0; y <= 3; y++) {
            k.set(176, y, z1, null);
            k.set(177, y, z1, null);
        }
        // 二层的名单墙与通铺
        for (int y = 13; y <= 16; y++) {
            for (int x = 168; x <= 176; x++) {
                k.set(x, y, z1 + 4, Palette.SPRUCE_PLANK);
            }
        }
        for (int i = 0; i < 6; i++) {
            k.set(168 + i * 4, 13, z1 + 16, Palette.HAY);
            k.set(168 + i * 4, 13, z1 + 46, Palette.HAY);
        }
        k.bump("公会酒馆");
    }

    /** 靶场：箭靶、草人、兵器架。公会真正练手的地方。 */
    static void guildYard(BuildKit k) {
        int x1 = 166;
        int z1 = -142;
        int x2 = 192;
        int z2 = -100;
        k.pave(x1, z1, x2, z2, Palette.COARSE, Palette.PATH);
        for (int x = x1; x <= x2; x++) {
            for (int y = 0; y <= 4; y++) {
                k.set(x, y, z1, Palette.COBBLE);
                k.set(x, y, z2, Palette.COBBLE);
            }
        }
        for (int z = z1; z <= z2; z++) {
            for (int y = 0; y <= 4; y++) {
                k.set(x1, y, z, Palette.COBBLE);
                k.set(x2, y, z, Palette.COBBLE);
            }
        }
        // 门在南墙
        for (int y = 0; y <= 3; y++) {
            k.set(x1 + 12, y, z2, null);
            k.set(x1 + 13, y, z2, null);
        }
        // 靶：草垛 + 靶心
        for (int i = 0; i < 4; i++) {
            int tx = x1 + 4 + i * 6;
            k.fill(tx, 0, z1 + 6, tx + 1, 2, z1 + 7, Palette.HAY);
            k.set(tx, 3, z1 + 6, Palette.WOOL_RED);
            k.set(tx + 1, 3, z1 + 6, Palette.WOOL_CREAM);
        }
        // 训练桩
        for (int i = 0; i < 5; i++) {
            int px = x1 + 3 + i * 5;
            k.fill(px, 0, z1 + 20, px, 3, z1 + 20, Palette.SPRUCE_LOG);
            k.set(px, 4, z1 + 20, Palette.HAY);
        }
        // 兵器架
        for (int i = 0; i < 3; i++) {
            k.fill(x1 + 2, 0, z1 + 30 + i * 2, x1 + 2, 3, z1 + 30 + i * 2, Palette.SPRUCE_FENCE);
            k.set(x1 + 2, 3, z1 + 30 + i * 2, Palette.IRON);
        }
        k.bump("靶场");
    }

    // -- 市政书记处、行会宿舍 ------------------------------------------------

    /** 市政厅的书记处与档案库：账簿在地下一层，墙上一排抽屉。 */
    static void townHallOffices(BuildKit k) {
        int x = -188;
        int z = 0;
        int w = 31;
        int d = 25;
        k.fill(x - 1, -3, z - 1, x + w, -1, z + d, Palette.STONE_BRICK);
        k.walls(x, 0, z, x + w - 1, 12, z + d - 1, Palette.STONE_BRICK);
        k.carve(x + 1, 0, z + 1, x + w - 2, 11, z + d - 2);
        for (int dx = 4; dx < w - 4; dx += 5) {
            k.fill(x + dx, 3, z, x + dx + 1, 6, z, Palette.GLASS);
        }
        for (int y = 0; y <= 4; y++) {
            k.set(x + w / 2 - 1, y, z + d - 1, null);
            k.set(x + w / 2, y, z + d - 1, null);
        }
        // 抽屉墙
        for (int dz = 3; dz < d - 3; dz += 3) {
            for (int y = 1; y <= 4; y++) {
                k.set(x + 2, y, z + dz, Palette.CHEST);
                k.set(x + w - 3, y, z + dz, Palette.CHEST);
            }
        }
        k.fill(x + 8, 0, z + 10, x + 22, 0, z + 11, Palette.DARK_PLANK);
        k.set(x + 14, 1, z + 10, Palette.LECTERN);
        k.set(x + 16, 1, z + 10, Palette.BOOKSHELF);
        // 地下档案库
        for (int dx = 6; dx <= 24; dx++) {
            for (int dz = 6; dz <= 18; dz++) {
                for (int y = -3; y <= -2; y++) {
                    k.set(x + dx, y, z + dz, null);
                }
                k.set(x + dx, -4, z + dz, Palette.STONE_BRICK);
                boolean edge = dx == 6 || dx == 24 || dz == 6 || dz == 18;
                if (edge) {
                    k.set(x + dx, -3, z + dz, Palette.STONE_BRICK);
                    k.set(x + dx, -2, z + dz, Palette.STONE_BRICK);
                }
            }
        }
        for (int i = 0; i < 4; i++) {
            k.set(x + 8 + i, -1 - i, z + 4 - i, Palette.STONE_BRICK);
        }
        for (int i = 0; i < 6; i++) {
            k.set(x + 8 + i * 3, -2, z + 16, Palette.BOOKSHELF);
        }
        k.roof(x - 2, z - 2, x + w + 1, z + d + 1, 13, true, Palette.SLATE_STAIR,
                Palette.STONE_BRICK, 1);
        k.bump("书记处");
    }

    /** 行会学徒宿舍：两栋，中间一道跨巷连桥。 */
    static void guildDorms(BuildKit k) {
        dorm(k, 130, -116);
        dorm(k, 168, -116);
        // 连桥：跨 x 156..164 那条巷
        for (int x = 153; x <= 167; x++) {
            for (int z = -104; z <= -96; z++) {
                k.set(x, 7, z, Palette.SPRUCE_PLANK);
                for (int y = 8; y <= 9; y++) {
                    k.set(x, y, z, null);
                }
                k.set(x, 10, z, Palette.SPRUCE_PLANK);
            }
        }
        k.bump("学徒宿舍");
    }

    private static void dorm(BuildKit k, int x, int z) {
        int w = 23;
        int d = 35;
        k.fill(x - 1, -3, z - 1, x + w, -1, z + d, Palette.COBBLE);
        k.walls(x, 0, z, x + w - 1, 14, z + d - 1, Palette.SPRUCE_PLANK);
        k.carve(x + 1, 0, z + 1, x + w - 2, 13, z + d - 2);
        for (int fy : new int[] {5, 10}) {
            for (int dx = 1; dx < w - 1; dx++) {
                for (int dz = 1; dz < d - 1; dz++) {
                    k.set(x + dx, fy, z + dz, Palette.DARK_PLANK);
                }
            }
        }
        // 通铺：上下两层
        for (int i = 0; i < 5; i++) {
            int bz = z + 4 + i * 6;
            k.fill(x + 2, 1, bz, x + 7, 1, bz + 1, Palette.HAY);
            k.fill(x + 15, 1, bz, x + 20, 1, bz + 1, Palette.HAY);
            k.fill(x + 2, 6, bz, x + 7, 6, bz + 1, Palette.HAY);
            k.fill(x + 15, 6, bz, x + 20, 6, bz + 1, Palette.HAY);
        }
        for (int y = 0; y <= 3; y++) {
            k.set(x + w / 2 - 1, y, z + d - 1, null);
            k.set(x + w / 2, y, z + d - 1, null);
        }
        k.roof(x - 2, z - 2, x + w + 1, z + d + 1, 15, true, Palette.SHINGLE_STAIR,
                Palette.SPRUCE_PLANK, 1);
        k.bump("宿舍");
    }

    // -- 贫民窟：城里最密的一块 -----------------------------------------------

    /**
     * 贫民窟：六格乘八格的联排，巷宽三格，没有院子。
     *
     * <p>密度在这里是阶级，不是风格。同一块地上，贵族住二十四乘三十带水池，
     * 这里住六乘八、共墙、阁楼、公用灶台。
     */
    static void slum(BuildKit k) {
        slumBlock(k, -190, 124, 3, 3);
        slumBlock(k, -190, 166, 3, 6);
        slumBlock(k, -154, 124, 3, 2);
        slumBlock(k, -154, 182, 3, 4);
        k.bump("贫民窟");
    }

    private static void slumBlock(BuildKit k, int x0, int z0, int cols, int rows) {
        // 巷子先铺：它们不在 STREETS 里，不铺就是三格宽的草地
        for (int c = 0; c <= cols; c++) {
            int ax = x0 + c * 9 - 1;
            k.pave(ax, z0 - 2, ax + 2, z0 + rows * 11, Palette.COBBLE, Palette.GRAVEL);
        }
        for (int r = 0; r <= rows; r++) {
            int az = z0 + r * 11 - 1;
            k.pave(x0 - 2, az, x0 + cols * 9, az + 2, Palette.COBBLE, Palette.GRAVEL);
        }
        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                slumHouse(k, x0 + c * 9, z0 + r * 11);
            }
        }
    }

    /** 一间：六乘八，两层八格高，共墙，屋顶朝街。 */
    private static void slumHouse(BuildKit k, int x, int z) {
        k.fill(x, -3, z, x + 5, -1, z + 7, Palette.COBBLE);
        k.walls(x, 0, z, x + 5, 8, z + 7, Palette.PLASTER_GRAY);
        k.carve(x + 1, 0, z + 1, x + 4, 7, z + 6);
        // 木梁：半木，但省到只剩四角与一道腰线
        for (int y : new int[] {0, 4, 8}) {
            for (int dx = 0; dx <= 5; dx++) {
                k.set(x + dx, y, z, Palette.DARK_LOG);
                k.set(x + dx, y, z + 7, Palette.DARK_LOG);
            }
            for (int dz = 0; dz <= 7; dz++) {
                k.set(x, y, z + dz, Palette.DARK_LOG);
                k.set(x + 5, y, z + dz, Palette.DARK_LOG);
            }
        }
        k.fill(x + 2, 0, z + 7, x + 3, 2, z + 7, null);
        k.set(x + 2, 2, z + 7, Palette.GLASS_GRAY);
        for (int y = 5; y <= 6; y++) {
            k.set(x + 1, y, z + 7, Palette.GLASS_GRAY);
            k.set(x + 4, y, z + 7, Palette.GLASS_GRAY);
        }
        // 楼板与阁楼
        for (int dx = 1; dx <= 4; dx++) {
            for (int dz = 1; dz <= 6; dz++) {
                k.set(x + dx, 4, z + dz, Palette.DARK_PLANK);
            }
        }
        k.roof(x - 1, z - 1, x + 6, z + 8, 9, true, Palette.SHINGLE_STAIR,
                Palette.PLASTER_GRAY, 1);
        k.set(x + 1, 0, z + 5, Palette.CAULDRON);
        k.set(x + 4, 0, z + 5, Palette.BARREL);
        // 巷里的公用灶台与晾衣杆
        k.set(x + 6, 0, z + 1, Palette.CAMPFIRE);
        k.fill(x + 6, 0, z + 5, x + 6, 3, z + 5, Palette.SPRUCE_FENCE);
        k.set(x + 6, 3, z + 5, Palette.WOOL_CREAM);
    }

    /** 中产区：十乘十二带六格院，巷宽四格。 */
    static void middleClass(BuildKit k) {
        for (int c = 0; c < 3; c++) {
            for (int r = 0; r < 5; r++) {
                int x = 170 + c * 20;
                int z = 26 + r * 16;
                k.fill(x - 1, -3, z - 1, x + 17, -1, z + 12, Palette.COBBLE);
                k.house(x, z, 10, 12, 2, "south", true);
                // 院子：六格宽，一口小井或晾衣杆
                k.pave(x + 11, z, x + 16, z + 11, Palette.GRASS, Palette.PATH);
                if ((c + r) % 2 == 0) {
                    k.set(x + 13, 0, z + 5, Palette.WATER);
                    k.set(x + 13, 1, z + 5, Palette.SPRUCE_FENCE);
                } else {
                    k.fill(x + 13, 0, z + 3, x + 13, 3, z + 3, Palette.SPRUCE_FENCE);
                    k.set(x + 13, 3, z + 3, Palette.WOOL_BLUE);
                }
            }
        }
        k.bump("中产区");
    }

    // -- 角斗场周边：摊贩街 ---------------------------------------------------

    /**
     * 场子外面比场子里面热闹。
     *
     * <p>卖的是当天：烤肉、赌签、护身符、名字被人写过的木牌。建筑一律压到两层，
     * 因为谁也不想在看台上被一栋楼挡住。
     */
    static void arenaVendors(BuildKit k) {
        // 东街与南街
        k.pave(268, 127, 278, 259, Palette.COBBLE, Palette.GRAVEL);
        k.pave(105, 263, 265, 275, Palette.COBBLE, Palette.GRAVEL);

        // 东侧矮铺面：六间，两层
        for (int i = 0; i < 6; i++) {
            int z = 128 + i * 20;
            if (i == 5) {
                z = 246;
            }
            lowShop(k, 280, z, 13, 19);
        }
        // 南侧矮铺面：四间，中间留给售票与闸口
        lowShop(k, 108, 277, 21, 15);
        lowShop(k, 132, 277, 21, 15);
        lowShop(k, 218, 277, 21, 15);
        lowShop(k, 242, 277, 21, 15);

        // 售票亭：两座，中间三个闸口
        ticket(k, 158, 265);
        ticket(k, 197, 265);
        for (int i = 0; i < 3; i++) {
            int gx = 178 + i * 5;
            for (int dx = 0; dx <= 1; dx++) {
                k.fill(gx + dx, 0, 265, gx + dx, 0, 273, Palette.IRON_BARS);
                k.set(gx + dx, 1, 266, Palette.IRON_BARS);
                k.set(gx + dx, 1, 272, Palette.IRON_BARS);
            }
        }
        // 排队区
        k.pave(150, 275, 220, 291, Palette.COBBLE, Palette.STONE);

        // 固定摊位：东十、南十二
        Block[][] cloth = {
                {Palette.WOOL_RED, Palette.WOOL_CREAM},
                {Palette.WOOL_YELLOW, Palette.WOOL_RED},
                {Palette.WOOL_GREEN, Palette.WOOL_CREAM},
                {Palette.WOOL_BLUE, Palette.WOOL_YELLOW}};
        for (int i = 0; i < 10; i++) {
            Block[] pair = cloth[i % cloth.length];
            k.stall(268, 130 + i * 12, pair[0], pair[1]);
        }
        for (int i = 0; i < 12; i++) {
            Block[] pair = cloth[i % cloth.length];
            k.stall(112 + i * 12, 263, pair[0], pair[1]);
        }
        k.bump("摊贩街");
    }

    private static void lowShop(BuildKit k, int x, int z, int w, int d) {
        k.fill(x - 1, -3, z - 1, x + w, -1, z + d, Palette.COBBLE);
        k.walls(x, 0, z, x + w - 1, 8, z + d - 1, Palette.PLASTER_CREAM);
        k.carve(x + 1, 0, z + 1, x + w - 2, 7, z + d - 2);
        for (int y : new int[] {0, 4, 8}) {
            for (int dx = 0; dx < w; dx++) {
                k.set(x + dx, y, z, Palette.DARK_LOG);
                k.set(x + dx, y, z + d - 1, Palette.DARK_LOG);
            }
        }
        // 临街的门面：敞开，一张柜台
        k.fill(x + 2, 0, z + d - 1, x + w - 3, 2, z + d - 1, null);
        k.fill(x + 2, 0, z + d - 2, x + w - 3, 1, z + d - 2, Palette.OAK_PLANK);
        k.set(x + 3, 2, z + d - 2, Palette.BARREL);
        k.set(x + w - 4, 2, z + d - 2, Palette.CRATE);
        k.set(x + w / 2, 3, z + d - 1, Palette.SIGN);
        k.roof(x - 1, z - 1, x + w, z + d, 9, true, Palette.SHINGLE_STAIR,
                Palette.PLASTER_CREAM, 1);
        k.bump("矮铺面");
    }

    private static void ticket(BuildKit k, int x, int z) {
        int w = 16;
        int d = 9;
        k.fill(x, -1, z, x + w - 1, 0, z + d - 1, Palette.STONE_BRICK);
        k.walls(x, 1, z, x + w - 1, 4, z + d - 1, Palette.SPRUCE_PLANK);
        k.carve(x + 1, 1, z + 1, x + w - 2, 3, z + d - 2);
        // 两个窗口朝北（对着排队的人）
        for (int i = 0; i < 2; i++) {
            int wx = x + 3 + i * 7;
            k.fill(wx, 1, z, wx + 2, 2, z, null);
            k.fill(wx, 1, z, wx + 2, 1, z, Palette.OAK_PLANK);
        }
        k.roof(x - 1, z - 1, x + w, z + d, 5, true, Palette.SHINGLE_STAIR,
                Palette.SPRUCE_PLANK, 1);
        k.set(x + w / 2, 3, z + d - 1, Palette.LANTERN);
        k.bump("售票亭");
    }
}
