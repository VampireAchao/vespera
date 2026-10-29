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
}
