package io.github.vampireachao.vespera.core.world;

import io.github.vampireachao.vespera.core.voxel.Block;
import io.github.vampireachao.vespera.core.voxel.Palette;

/**
 * 地标：一座城里真正撑起天际线的几样东西。
 *
 * <p>写它们的时候守着一条规矩：<b>每个形体都要有结构上的理由</b>。扶壁是因为高墙会倒，
 * 尖顶是因为雪要滑下来，吊桥是因为门最容易被攻。中世纪的人不装饰，他们解决问题，
 * 然后问题解决得好看。照这个顺序做，比往墙上贴花纹更容易"像"。
 */
final class Landmarks {

    private Landmarks() {
    }

    // -- 第九时圣座 ---------------------------------------------------------

    static void cathedral(BuildKit k) {
        int zFront = -96;
        int zBack = -272;
        int x1 = 132;
        int x2 = 188;
        int wallTop = 34;

        // 台基与门前广场
        k.pave(100, zBack - 8, 220, zFront + 10, Palette.STONE_BRICK, Palette.ANDESITE);
        for (int i = 0; i < 4; i++) {
            k.fill(112 + i, -1, zFront + 2 + i, 208 - i, 0, zFront + 4 + i, Palette.STONE_BRICK);
        }

        // 中殿
        for (int x = x1; x <= x2; x++) {
            for (int z = zBack; z <= zFront; z++) {
                boolean edge = x == x1 || x == x2 || z == zBack || z == zFront;
                if (!edge) {
                    continue;
                }
                int band = (x + z) % 17;
                Block block = band == 0 ? Palette.STONE_MOSSY
                        : (band == 5 ? Palette.STONE_CRACKED : Palette.STONE_BRICK);
                k.fill(x, 0, z, x, wallTop, z, block);
            }
        }
        k.carve(x1 + 1, 0, zBack + 1, x2 - 1, wallTop + 18, zFront - 1);

        // 高侧窗：尖拱，彩色玻璃，每 8 格一扇
        for (int z = zBack + 8; z < zFront - 6; z += 9) {
            pointedWindow(k, x1, 0, z, 3, 10, "west");
            pointedWindow(k, x2, 0, z, 3, 10, "east");
        }

        // 扶壁：每 8 格一道，探出两格，顶上有小尖塔
        for (int z = zBack + 4; z <= zFront - 4; z += 9) {
            for (int side = -1; side <= 1; side += 2) {
                int bx = side < 0 ? x1 : x2;
                k.fill(bx + side, 0, z - 1, bx + side * 2, 22, z + 1, Palette.STONE_BRICK);
                k.fill(bx + side, 23, z - 1, bx + side * 2, 26, z + 1, Palette.STONE_CHISELED);
                k.set(bx + side * 2, 27, z, Palette.STONE_BRICK);
                k.set(bx + side * 2, 28, z, Palette.STONE_BRICK);
                k.set(bx + side * 2, 29, z, Palette.STONE_CHISELED);
                // 飞扶壁：从外侧斜撑到高窗下
                for (int t = 0; t <= 8; t++) {
                    int fx = bx + side * (2 - t * 0 / 8);
                    int fy = 22 + t;
                    if (side < 0) {
                        k.set(bx - 2 + t, fy, z, Palette.STONE_BRICK);
                    } else {
                        k.set(bx + 2 - t, fy, z, Palette.STONE_BRICK);
                    }
                }
            }
        }

        // 横厅
        int tz1 = -216;
        int tz2 = -184;
        k.walls(104, 0, tz1, 216, 26, tz2, Palette.STONE_BRICK);
        k.carve(105, 0, tz1 + 1, 215, 25, tz2 - 1);
        k.roof(102, tz1 - 1, 218, tz2 + 1, 26, false, Palette.SLATE_STAIR, Palette.STONE_BRICK, 1);
        pointedWindow(k, 104, 0, tz1 + 8, 4, 10, "west");
        pointedWindow(k, 216, 0, tz1 + 8, 4, 10, "east");

        // 屋顶：板岩，脊上一排装饰
        k.roof(x1 - 3, zBack - 1, x2 + 3, zFront + 1, wallTop, true, Palette.SLATE_STAIR,
                Palette.STONE_BRICK, 2);
        for (int z = zBack; z <= zFront; z += 6) {
            k.set((x1 + x2) / 2, wallTop + 30, z, Palette.STONE_CHISELED);
        }
        // 十字架
        for (int y = 1; y <= 6; y++) {
            k.set((x1 + x2) / 2, wallTop + 30 + y, zBack + 30, Palette.DARK_LOG);
        }
        k.fill((x1 + x2) / 2 - 2, wallTop + 34, zBack + 30, (x1 + x2) / 2 + 2, wallTop + 34, zBack + 30,
                Palette.DARK_LOG);

        // 西立面双塔
        for (int side = -1; side <= 1; side += 2) {
            int tx = side < 0 ? 106 : 190;
            tower(k, tx, -118, 22, 22, 84, 26, Palette.STONE_BRICK);
        }

        // 交叉处的尖塔：哥特教堂的屋脊上必须有一个东西把视线钉住
        int fx = (x1 + x2) / 2;
        int fz = (tz1 + tz2) / 2;
        k.roundTower(fx, fz, 3, wallTop + 26, 6, Palette.STONE_BRICK, Palette.STONE_CHISELED,
                Palette.SLATE_STAIR, 0);
        k.cone(fx, fz, 4, wallTop + 32, 14, Palette.SLATE);
        k.set(fx, wallTop + 47, fz, Palette.GOLD);

        // 后殿：半圆，五面，一圈高窗
        int apseCz = zBack - 10;
        for (int y = 0; y <= 26; y++) {
            for (int dx = -18; dx <= 18; dx++) {
                for (int dz = -18; dz <= 0; dz++) {
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (d <= 18 && d > 16.4) {
                        Block block = (y % 11 == 1) ? Palette.STONE_CHISELED : Palette.STONE_BRICK;
                        k.set(fx + dx, y, apseCz + dz, block);
                    }
                }
            }
        }
        for (int i = 0; i < 5; i++) {
            int az = apseCz - 16 + i * 4;
            k.fill(fx - 12 + i * 2, 6, az, fx - 12 + i * 2, 18, az, Palette.GLASS_BLUE);
        }
        k.cone(fx, apseCz, 18, 27, 12, Palette.SLATE);

        // 玫瑰窗
        int cx = (x1 + x2) / 2;
        int cy = 22;
        for (int dx = -10; dx <= 10; dx++) {
            for (int dy = -10; dy <= 10; dy++) {
                double d = Math.sqrt(dx * dx + dy * dy);
                if (d > 10) {
                    continue;
                }
                if (d > 8.4) {
                    k.set(cx + dx, cy + dy, zFront, Palette.STONE_CHISELED);
                } else if (d > 7.4) {
                    k.set(cx + dx, cy + dy, zFront, Palette.GOLD);
                } else if ((Math.abs(dx) < 2 || Math.abs(dy) < 2
                        || Math.abs(Math.abs(dx) - Math.abs(dy)) < 1)) {
                    k.set(cx + dx, cy + dy, zFront, Palette.STONE_CHISELED);
                } else {
                    Block glass = (Math.abs(dx) + Math.abs(dy)) % 2 == 0
                            ? Palette.GLASS_ROSE : Palette.GLASS_BLUE;
                    k.set(cx + dx, cy + dy, zFront, glass);
                }
            }
        }

        // 三门
        portal(k, cx, zFront, 9, 9);
        portal(k, cx - 18, zFront, 6, 7);
        portal(k, cx + 18, zFront, 6, 7);

        k.bump("大教堂");
        k.bump("地标");
    }

    /** 一扇尖拱窗：下部玻璃，上部石券收成尖。 */
    static void pointedWindow(BuildKit k, int x, int y, int z, int width, int height, String facing) {
        boolean alongX = facing.equals("north") || facing.equals("south");
        for (int i = 0; i < width; i++) {
            for (int h = 0; h < height; h++) {
                if (!alongX) {
                    k.carve(x, y + h, z + i, x, y + h, z + i);
                } else {
                    k.carve(x + i, y + h, z, x + i, y + h, z);
                }
            }
        }
        int step = 0;
        for (int h = height; h < height + width / 2 + 1; h++) {
            int inset = step++;
            for (int i = inset; i < width - inset; i++) {
                if (!alongX) {
                    k.carve(x, y + h, z + i, x, y + h, z + i);
                } else {
                    k.carve(x + i, y + h, z, x + i, y + h, z);
                }
            }
        }
        if (!alongX) {
            k.fill(x, y, z, x, y + height - 1, z + width - 1, Palette.GLASS_BLUE);
        } else {
            k.fill(x, y, z, x + width - 1, y + height - 1, z, Palette.GLASS_BLUE);
        }
    }

    /** 门洞：两级台阶、一圈券、一扇木门。 */
    static void portal(BuildKit k, int cx, int z, int width, int height) {
        k.carve(cx - width / 2, 0, z - 1, cx + width / 2, height, z + 1);
        for (int i = 0; i <= width / 2; i++) {
            for (int h = 0; h < height; h++) {
                k.set(cx - width / 2 - 1, h, z, Palette.STONE_CHISELED);
                k.set(cx + width / 2 + 1, h, z, Palette.STONE_CHISELED);
                k.set(cx - width / 2 - 1, h, z - 1, Palette.STONE_CHISELED);
                k.set(cx + width / 2 + 1, h, z - 1, Palette.STONE_CHISELED);
            }
        }
        for (int i = 0; i < width; i++) {
            k.set(cx - width / 2 + i, height + Math.abs(i - width / 2), z, Palette.STONE_CHISELED);
        }
        for (int i = 0; i < width; i += 3) {
            k.set(cx - width / 2 + i, 0, z, Palette.DARK_PLANK);
            k.set(cx - width / 2 + i, 1, z, Palette.DARK_PLANK);
        }
    }

    /** 方形塔：身、腰线、钟窗、雉堞，最后加尖顶。 */
    static void tower(BuildKit k, int x, int z, int w, int d, int height, int spire, Block wall) {
        int x2 = x + w - 1;
        int z2 = z + d - 1;
        for (int y = 0; y <= height; y++) {
            for (int xx = x; xx <= x2; xx++) {
                for (int zz = z; zz <= z2; zz++) {
                    boolean edge = xx == x || xx == x2 || zz == z || zz == z2;
                    if (!edge) {
                        continue;
                    }
                    boolean setback = y >= height - 6 && (xx == x || xx == x2) && (zz == z || zz == z2);
                    Block block = (y % 12 == 0) ? Palette.STONE_CHISELED : wall;
                    if (y > 4 && y % 12 == 7 && ((xx + zz) % 5 == 0)) {
                        block = Palette.GLASS_GRAY;
                    }
                    k.set(xx, y, zz, block);
                }
            }
        }
        k.carve(x + 1, 0, z + 1, x2 - 1, height - 1, z2 - 1);
        // 钟窗：四面各一个高开口
        k.carve(x + w / 2 - 2, height - 10, z, x + w / 2 + 2, height - 3, z);
        k.carve(x + w / 2 - 2, height - 10, z2, x + w / 2 + 2, height - 3, z2);
        k.carve(x, height - 10, z + d / 2 - 2, x, height - 3, z + d / 2 + 2);
        k.carve(x2, height - 10, z + d / 2 - 2, x2, height - 3, z + d / 2 + 2);
        k.crenellate(x, z, x2, z2, height, wall);
        k.crenellate(x, z, x2, z2, height - 1, wall);
        if (spire > 0) {
            int cx = (x + x2) / 2;
            int cz = (z + z2) / 2;
            k.cone(cx, cz, Math.min(w, d) / 2 - 1, height + 1, spire, Palette.SLATE);
            k.set(cx, height + spire + 1, cz, Palette.GOLD);
        }
    }

    // -- 城堡 ---------------------------------------------------------------

    /**
     * 灰烬堡：一座有台地、幕墙、主楼、门楼与大院长屋的城堡。
     *
     * <p>它建在比城高两格的石台上——这不是装饰，是防御：台地让攻方必须仰攻，
     * 也让它在城里的天际线上压得住教堂。
     */
    static void castle(BuildKit k) {
        int x1 = -288;
        int z1 = -288;
        int x2 = -152;
        int z2 = -152;
        int platform = 2;

        // 台地：两层石，边缘就是挡土墙
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                for (int y = -1; y <= platform; y++) {
                    k.set(x, y, z, y == platform ? Palette.COBBLE : Palette.STONE_BRICK);
                }
            }
        }
        k.walls(x1, -1, z1, x2, platform, z2, Palette.STONE_BRICK);
        k.crenellate(x1, z1, x2, z2, platform + 1, Palette.STONE_BRICK);

        // 幕墙
        int wx1 = x1 + 10;
        int wz1 = z1 + 10;
        int wx2 = x2 - 8;
        int wz2 = z2 - 8;
        int wallTop = platform + 14;
        k.walls(wx1, platform + 1, wz1, wx2, wallTop, wz2, Palette.STONE_BRICK);
        for (int x = wx1; x <= wx2; x++) {
            for (int z = wz1; z <= wz2; z++) {
                boolean edge = x == wx1 || x == wx2 || z == wz1 || z == wz2;
                if (!edge) {
                    continue;
                }
                k.set(x, wallTop + 1, z, Palette.STONE_BRICK);
                if (((x + z) & 1) == 0) {
                    k.set(x, wallTop + 2, z, Palette.STONE_BRICK);
                }
            }
        }
        // 墙上的箭孔
        for (int x = wx1 + 6; x < wx2 - 4; x += 10) {
            k.set(x, platform + 8, wz1, Palette.STONE_CHISELED);
            k.set(x, platform + 8, wz2, Palette.STONE_CHISELED);
        }
        for (int z = wz1 + 6; z < wz2 - 4; z += 10) {
            k.set(wx1, platform + 8, z, Palette.STONE_CHISELED);
            k.set(wx2, platform + 8, z, Palette.STONE_CHISELED);
        }

        // 四角圆塔
        int[][] corners = {{wx1, wz1}, {wx2, wz1}, {wx1, wz2}, {wx2, wz2}};
        for (int[] c : corners) {
            k.roundTower(c[0], c[1], 7, platform + 1, 22, Palette.STONE_BRICK,
                    Palette.STONE_CHISELED, Palette.SLATE_STAIR, 1);
        }

        // 主楼：方形、平顶、四角小塔，中央一座楼梯塔
        int kx1 = wx1 + 10;
        int kz1 = wz1 + 10;
        int kx2 = kx1 + 36;
        int kz2 = kz1 + 30;
        int keepTop = platform + 40;
        k.walls(kx1, platform + 1, kz1, kx2, keepTop, kz2, Palette.STONE_BRICK);
        k.carve(kx1 + 1, platform + 1, kz1 + 1, kx2 - 1, keepTop - 1, kz2 - 1);
        k.crenellate(kx1, kz1, kx2, kz2, keepTop + 1, Palette.STONE_BRICK);
        k.crenellate(kx1, kz1, kx2, kz2, keepTop + 2, Palette.STONE_BRICK);
        // 窗：成对的小窗，越往上越少
        for (int y = platform + 8; y < keepTop - 8; y += 9) {
            k.fill(kx1 + 8, y, kz2, kx1 + 9, y + 2, kz2, Palette.GLASS_GRAY);
            k.fill(kx1 + 26, y, kz2, kx1 + 27, y + 2, kz2, Palette.GLASS_GRAY);
            k.fill(kx2, y, kz1 + 10, kx2, y + 2, kz1 + 11, Palette.GLASS_GRAY);
        }
        for (int[] t : new int[][] {{kx1, kz1}, {kx2, kz1}, {kx1, kz2}, {kx2, kz2}}) {
            k.roundTower(t[0], t[1], 2, platform + 1, 42, Palette.STONE_BRICK,
                    Palette.STONE_CHISELED, Palette.SLATE_STAIR, 1);
        }
        k.roundTower((kx1 + kx2) / 2, (kz1 + kz2) / 2, 3, keepTop, 8, Palette.STONE_BRICK,
                Palette.STONE_CHISELED, Palette.SLATE_STAIR, 1);
        // 主楼入口：石阶
        for (int i = 0; i < 4; i++) {
            k.fill(kx1 + 15, platform + i, kz2 + 4 - i, kx1 + 21, platform + i, kz2 + 4 - i,
                    Palette.STONE_BRICK);
        }
        portal(k, kx1 + 18, kz2, 6, 6);

        // 大院长屋
        int hx1 = kx2 + 8;
        int hz1 = wz1 + 12;
        int hx2 = wx2 - 8;
        int hz2 = hz1 + 20;
        k.walls(hx1, platform + 1, hz1, hx2, platform + 14, hz2, Palette.STONE_BRICK);
        k.carve(hx1 + 1, platform + 1, hz1 + 1, hx2 - 1, platform + 13, hz2 - 1);
        k.roof(hx1 - 2, hz1 - 2, hx2 + 2, hz2 + 2, platform + 15, true, Palette.SLATE_STAIR,
                Palette.STONE_BRICK, 1);
        for (int z = hz1 + 4; z < hz2 - 2; z += 5) {
            k.fill(hx1, platform + 7, z, hx1, platform + 10, z, Palette.GLASS_GRAY);
        }

        // 门楼：南墙正中
        int gx = (wx1 + wx2) / 2;
        int gz = wz2;
        for (int side = -1; side <= 1; side += 2) {
            k.roundTower(gx + side * 11, gz, 6, platform + 1, 22, Palette.STONE_BRICK,
                    Palette.STONE_CHISELED, Palette.SLATE_STAIR, 1);
        }
        k.fill(gx - 8, platform + 1, gz - 4, gx + 8, wallTop + 4, gz + 4, Palette.STONE_BRICK);
        k.carve(gx - 4, platform + 1, gz - 4, gx + 4, platform + 8, gz + 4);
        for (int i = 0; i <= 4; i++) {
            for (int y = platform + 9 + i; y <= platform + 12; y++) {
                k.set(gx - 4 + i, y, gz, Palette.STONE_CHISELED);
                k.set(gx + 4 - i, y, gz, Palette.STONE_CHISELED);
            }
        }
        // 吊桥与门扇
        for (int i = 1; i <= 8; i++) {
            k.fill(gx - 4, platform, gz + i, gx + 4, platform, gz + i, Palette.DARK_PLANK);
            k.set(gx - 4, platform + 1, gz + i, Palette.CHAIN);
            k.set(gx + 4, platform + 1, gz + i, Palette.CHAIN);
        }
        for (int dx = -4; dx <= 4; dx++) {
            for (int y = platform + 1; y <= platform + 7; y++) {
                if ((dx + y) % 2 == 0) {
                    k.set(gx + dx, y, gz - 2, Palette.DARK_PLANK);
                }
            }
        }

        // 坡道：吊桥不能悬在半空
        for (int i = 0; i < 6; i++) {
            int y = platform - i;
            if (y < 0) {
                break;
            }
            k.fill(gx - 4, y, gz + 9 + i, gx + 4, y, gz + 9 + i, Palette.COBBLE);
        }

        // 院子：马厩、营房、训练场、井
        int sx = wx1 + 8;
        int sz = wz2 - 34;
        // 马厩：同样的做法。原来这间四面封死、门口只是把墙凿开十格宽——马进不去人也不进
        stalls(k, sx, sz, 35, 15, platform);
        k.roof(sx - 1, sz - 1, sx + 35, sz + 15, platform + 7, false, Palette.SHINGLE_STAIR,
                Palette.SPRUCE_PLANK, 1);
        // 营房
        int bx = wx2 - 34;
        int bz = wz2 - 26;
        k.walls(bx, platform + 1, bz, bx + 30, platform + 8, bz + 16, Palette.STONE_BRICK);
        k.carve(bx + 1, platform + 1, bz + 1, bx + 29, platform + 7, bz + 15);
        k.roof(bx - 2, bz - 2, bx + 32, bz + 18, platform + 9, true, Palette.TILE_STAIR,
                Palette.STONE_BRICK, 1);
        k.fill(bx + 4, platform + 1, bz + 16, bx + 7, platform + 3, bz + 16, Palette.DARK_PLANK);
        // 训练桩
        for (int i = 0; i < 4; i++) {
            k.fill(sx + 6 + i * 7, platform + 1, sz + 20, sx + 6 + i * 7, platform + 4, sz + 20,
                    Palette.SPRUCE_LOG);
            k.set(sx + 6 + i * 7, platform + 5, sz + 20, Palette.HAY);
        }
        // 院内主路：门楼 → 主楼台阶，再到长屋。铺装是院子的骨架。
        for (int z = kz2; z <= gz; z++) {
            for (int dx = -2; dx <= 2; dx++) {
                k.set(gx + dx, platform, z, Palette.STONE_BRICK);
            }
        }
        for (int x = gx; x <= hx1 + 2; x++) {
            for (int dz = -2; dz <= 2; dz++) {
                k.set(x, platform, hz2 + 6 + dz, Palette.STONE_BRICK);
            }
        }
        for (int i = 0; i < 5; i++) {
            k.set(wx1 + 20 + i, platform, wz2 - 30, Palette.BARREL);
        }

        Civic.well(k, wx1 + 26, wz2 - 12);
        k.fill(wx1 + 4, platform + 1, wz1 + 30, wx1 + 14, platform + 1, wz1 + 40, Palette.COARSE);

        k.bump("城堡");
        k.bump("地标");
    }

    // -- 市政厅 -------------------------------------------------------------

    static void townHall(BuildKit k) {
        // 往北让两格：原来的南墙压在出城大道上
        townHall(k, -190, -76);
    }

    static void townHall(BuildKit k, int x1, int z1) {
        int x2 = x1 + 48;
        int z2 = z1 + 68;

        k.fill(x1 - 2, -3, z1 - 2, x2 + 2, -1, z2 + 2, Palette.STONE_BRICK);
        k.walls(x1, 0, z1, x2, 20, z2, Palette.STONE_BRICK);
        k.carve(x1 + 1, 0, z1 + 1, x2 - 1, 19, z2 - 1);

        // 底层拱廊：朝东（广场）
        for (int z = z1 + 4; z < z2 - 3; z += 8) {
            for (int i = 0; i < 3; i++) {
                k.carve(x2, 0, z + i, x2, 5, z + i + 1);
            }
            k.set(x2, 6, z, Palette.STONE_CHISELED);
            k.set(x2, 6, z + 2, Palette.STONE_CHISELED);
        }
        // 三层窗
        for (int floor = 1; floor <= 2; floor++) {
            int y = floor * 7 + 1;
            for (int z = z1 + 4; z < z2 - 2; z += 5) {
                k.fill(x2, y, z, x2, y + 2, z, Palette.GLASS);
                k.set(x2, y + 3, z, Palette.STONE_CHISELED);
            }
        }

        // 阶梯山墙：朝东，一级一级收上去
        for (int i = 0; i <= 8; i++) {
            int y = 21 + i;
            int inset = i * 2;
            k.fill(x1 + inset, y, z1 + inset, x2 - inset, y, z2 - inset, Palette.STONE_BRICK);
            if (i % 2 == 1) {
                k.crenellate(x1 + inset, z1 + inset, x2 - inset, z2 - inset, y + 1, Palette.STONE_BRICK);
            }
        }
        k.roof(x1 + 18, z1 + 18, x2 - 18, z2 - 18, 29, false, Palette.SLATE_STAIR, Palette.SLATE, 1);

        // 钟塔
        // 钟塔从 69 压到 44：原来是全城第三高，和金字塔、王座厅挤在一个高度上，
        // 天际线就成了一排平顶。市政厅本来就该低于王座、高于街屋。
        tower(k, x2 - 12, z1 + 2, 16, 16, 36, 8, Palette.STONE_BRICK);
        // 钟面，朝广场
        int cx = x2 - 4;
        int cy = 24;
        for (int dy = -4; dy <= 4; dy++) {
            for (int dz = -4; dz <= 4; dz++) {
                double d = Math.sqrt(dy * dy + dz * dz);
                if (d <= 4) {
                    Block face = d > 3 ? Palette.GOLD
                            : (Math.abs(dz) < 1 || Math.abs(dy) < 1 ? Palette.WOOL_BLACK : Palette.WOOL_CREAM);
                    k.set(cx, cy + dy, z1 + 2, face);
                }
            }
        }
        // 门
        portal(k, x2, (z1 + z2) / 2, 7, 7);
        k.bump("市政厅");
        k.bump("地标");
    }

    // -- 行会厅 -------------------------------------------------------------

    static void guildHall(BuildKit k) {
        guildHall(k, 130, -74);
    }

    static void guildHall(BuildKit k, int x1, int z1) {
        int x2 = x1 + 62;
        // 南界从 +80 收到 +60：再往南就压住东大道（z -8..8），出城的路会断在这里
        int z2 = z1 + 60;

        k.fill(x1 - 2, -3, z1 - 2, x2 + 2, -1, z2 + 2, Palette.STONE_BRICK);
        k.walls(x1, 0, z1, x2, 12, z2, Palette.STONE_BRICK);
        k.carve(x1 + 1, 0, z1 + 1, x2 - 1, 11, z2 - 1);

        // 二层半木，向外出挑一格
        k.walls(x1 - 1, 12, z1 - 1, x2 + 1, 24, z2 + 1, Palette.PLASTER);
        for (int y = 12; y <= 24; y++) {
            boolean beam = y == 12 || y == 24 || y % 4 == 0;
            for (int x = x1 - 1; x <= x2 + 1; x++) {
                if (beam || Math.floorMod(x, 5) == 0) {
                    k.set(x, y, z1 - 1, Palette.DARK_LOG);
                    k.set(x, y, z2 + 1, Palette.DARK_LOG);
                }
            }
            for (int z = z1 - 1; z <= z2 + 1; z++) {
                if (beam || Math.floorMod(z, 5) == 0) {
                    k.set(x1 - 1, y, z, Palette.DARK_LOG);
                    k.set(x2 + 1, y, z, Palette.DARK_LOG);
                }
            }
        }
        for (int x = x1 + 2; x < x2 - 1; x += 4) {
            k.fill(x, 16, z1 - 1, x, 19, z1 - 1, Palette.GLASS);
            k.fill(x, 16, z2 + 1, x, 19, z2 + 1, Palette.GLASS);
        }
        k.roof(x1 - 4, z1 - 4, x2 + 4, z2 + 4, 25, true, Palette.TILE_STAIR, Palette.PLASTER, 2);

        // 底层拱门与旗帜
        for (int i = 0; i < 5; i++) {
            k.carve(x1 + 2 + i * 12, 0, z1, x1 + 7 + i * 12, 7, z1);
            k.set(x1 + 2 + i * 12, 8, z1, Palette.STONE_CHISELED);
            k.set(x1 + 7 + i * 12, 8, z1, Palette.STONE_CHISELED);
        }
        for (int i = 0; i < 3; i++) {
            k.set(x1 + 6 + i * 20, 12, z1 - 2, Palette.WOOL_RED);
            k.fill(x1 + 6 + i * 20, 13, z1 - 2, x1 + 6 + i * 20, 16, z1 - 2, Palette.WOOL_RED);
        }
        k.bump("行会厅");
        k.bump("地标");
    }

    // -- 王室内苑 -------------------------------------------------------------

    /**
     * 台地两翼：西边国库与王室居所，东边学堂。
     *
     * <p>这三栋都得贴着台地盖，因为台地本身就是地形的最高处——它们存在的前提
     * 是"离权力一墙之隔"。所以西墙距挡土墙只留两格，那是极限。
     */
    static void palaceWings(BuildKit k) {
        // 服务街：把国库与居所之间那条十一格的缝做成路
        k.pave(-103, -294, -93, -252, Palette.COBBLE, Palette.STONE);
        treasury(k, -92, -290, 41, 29);
        royalHouse(k, -141, -292, 38, 37);
        royalSchool(k, 54, -290, 41, 29);
        k.bump("王室内苑");
        k.bump("地标");
    }

    /** 国库：无窗、单铁门、三层门廊。教会账簿的实物落点。 */
    private static void treasury(BuildKit k, int x, int z, int w, int d) {
        k.fill(x - 1, -3, z - 1, x + w, -1, z + d, Palette.STONE_BRICK);
        k.walls(x, 0, z, x + w - 1, 9, z + d - 1, Palette.STONE_BRICK);
        k.carve(x + 1, 0, z + 1, x + w - 2, 8, z + d - 2);
        // 三道门：门口、门厅、库房。前两道是铁栅，最后一道是整块铁
        int c = x + w / 2;
        for (int dx = -2; dx <= 2; dx++) {
            for (int y = 0; y <= 5; y++) {
                k.set(c + dx, y, z, Palette.IRON);
                k.set(c + dx, y, z + 4, Palette.IRON_BARS);
                k.set(c + dx, y, z + 8, Palette.IRON_BARS);
            }
        }
        for (int gate : new int[] {z, z + 4, z + 8}) {
            k.fill(c - 3, 0, gate, c - 3, 5, gate, Palette.STONE_CHISELED);
            k.fill(c + 3, 0, gate, c + 3, 5, gate, Palette.STONE_CHISELED);
            k.fill(c - 3, 6, gate, c + 3, 6, gate, Palette.STONE_CHISELED);
        }
        // 库架：靠墙六组，箱与桶
        for (int i = 0; i < 6; i++) {
            int bx = x + 4 + i * 6;
            k.set(bx, 0, z + d - 3, Palette.CHEST);
            k.set(bx + 1, 0, z + d - 3, i % 2 == 0 ? Palette.BARREL : Palette.CRATE);
        }
        // 平顶 + 雉堞：它看着就该比居所难进
        k.fill(x, 10, z, x + w - 1, 10, z + d - 1, Palette.STONE_BRICK);
        k.crenellate(x, z, x + w - 1, z + d - 1, 11, Palette.STONE_BRICK);
        k.set(x + 3, 0, z + 2, Palette.LANTERN);
        k.bump("国库");
    }

    /** 王室居所：三层、角塔、朝东的正门，接那条十一格服务街。 */
    private static void royalHouse(BuildKit k, int x, int z, int w, int d) {
        k.fill(x - 1, -3, z - 1, x + w, -1, z + d, Palette.STONE_BRICK);
        k.walls(x, 0, z, x + w - 1, 17, z + d - 1, Palette.STONE_BRICK);
        k.carve(x + 1, 0, z + 1, x + w - 2, 16, z + d - 2);
        // 楼层
        for (int fy : new int[] {6, 12}) {
            for (int dx = 1; dx < w - 1; dx++) {
                for (int dz = 1; dz < d - 1; dz++) {
                    k.set(x + dx, fy, z + dz, Palette.DARK_PLANK);
                }
            }
        }
        // 窗：南面与东面各一排
        for (int dx = 4; dx < w - 4; dx += 5) {
            pointedWindow(k, x + dx, 2, z + d - 1, 2, 3, "south");
            pointedWindow(k, x + dx, 8, z + d - 1, 2, 3, "south");
        }
        for (int dz = 5; dz < d - 5; dz += 7) {
            pointedWindow(k, x + w - 1, 2, z + dz, 2, 3, "east");
            pointedWindow(k, x + w - 1, 8, z + dz, 2, 3, "east");
        }
        // 正门朝东
        for (int y = 0; y <= 4; y++) {
            k.fill(x + w - 1, y, z + d / 2 - 1, x + w - 1, y, z + d / 2 + 1,
                    y == 4 ? Palette.STONE_CHISELED : null);
        }
        for (int i = 0; i < 3; i++) {
            k.fill(x + w, i, z + d / 2 - 2, x + w, i, z + d / 2 + 2, Palette.STONE_CHISELED);
        }
        k.roof(x - 3, z - 3, x + w + 2, z + d + 2, 18, true, Palette.SLATE_STAIR,
                Palette.STONE_BRICK, 2);
        // 角塔
        k.roundTower(x + w - 3, z + 3, 4, 0, 24, Palette.STONE_BRICK,
                Palette.STONE_CHISELED, Palette.SLATE_STAIR, 1);
        k.bump("王室居所");
    }

    /** 王室学堂：内院十二格见方，回廊一圈。 */
    private static void royalSchool(BuildKit k, int x, int z, int w, int d) {
        k.fill(x - 1, -3, z - 1, x + w, -1, z + d, Palette.STONE_BRICK);
        k.walls(x, 0, z, x + w - 1, 11, z + d - 1, Palette.STONE_BRICK);
        // 内院：十二格见方，四面回廊
        int cx = x + w / 2;
        int cz = z + d / 2;
        k.carve(cx - 6, 0, cz - 6, cx + 6, 10, cz + 6);
        for (int i = -6; i <= 6; i++) {
            for (int y = 0; y <= 10; y++) {
                k.set(cx + i, y, cz - 7, y <= 9 ? null : Palette.STONE_BRICK);
                k.set(cx + i, y, cz + 7, y <= 9 ? null : Palette.STONE_BRICK);
                k.set(cx - 7, y, cz + i, y <= 9 ? null : Palette.STONE_BRICK);
                k.set(cx + 7, y, cz + i, y <= 9 ? null : Palette.STONE_BRICK);
            }
        }
        k.pave(cx - 6, cz - 6, cx + 6, cz + 6, Palette.PATH, Palette.GRASS);
        k.set(cx, 0, cz, Palette.LECTERN);
        for (int y = 0; y <= 5; y++) {
            k.set(x + w / 2, y, z + d - 1, y == 5 ? Palette.STONE_CHISELED : null);
        }
        for (int dx = 4; dx < w - 4; dx += 6) {
            k.fill(x + dx, 3, z, x + dx + 1, 6, z, Palette.GLASS);
        }
        k.roof(x - 2, z - 2, x + w + 1, z + d + 1, 12, true, Palette.SLATE_STAIR,
                Palette.STONE_BRICK, 1);
        // 内院上方必须露天：把屋顶挖开十二格见方的洞，否则院子被封死
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -7; dz <= 7; dz++) {
                for (int y = 12; y <= 20; y++) {
                    k.set(cx + dx, y, cz + dz, null);
                }
            }
        }
        k.bump("王室学堂");
    }

    // -- 贵族府邸 -------------------------------------------------------------

    /** 六座府邸 + 两座马厩院。密度在这里必须掉下来：院子是身份的计量单位。 */
    static void nobleQuarter(BuildKit k) {
        mansion(k, 104, -288, false);
        mansion(k, 170, -288, false);
        mansion(k, 104, -228, false);
        mansion(k, 170, -228, false);
        mansion(k, 104, -196, true);
        mansion(k, 170, -196, false);
        stable(k, 104, -152, 49);
        stable(k, 170, -152, 35);
        k.bump("贵族区");
    }

    /**
     * 一座府邸：主楼 24×30、三层、前院带水池与地窖，侧门开在私巷。
     *
     * <p>池子不深，一格水一 格石——它表达的是"这人家里有水"，不是游泳池。
     */
    private static void mansion(BuildKit k, int x, int z, boolean tall) {
        int w = 24;
        int d = 30;
        int h = tall ? 20 : 18;
        k.claim("府邸", x - 1, z - 1, x + w, z + d);
        k.fill(x - 1, -3, z - 1, x + w, -1, z + d, Palette.STONE_BRICK);
        k.walls(x, 0, z, x + w - 1, h, z + d - 1, Palette.STONE_BRICK);
        k.carve(x + 1, 0, z + 1, x + w - 2, h - 1, z + d - 2);
        for (int fy : new int[] {6, 12}) {
            for (int dx = 1; dx < w - 1; dx++) {
                for (int dz = 1; dz < d - 1; dz++) {
                    k.set(x + dx, fy, z + dz, Palette.DARK_PLANK);
                }
            }
        }
        // 南立面：门与窗，规矩要统一——这条街上房子不能各说各话
        k.fill(x + w / 2 - 1, 0, z + d - 1, x + w / 2, 3, z + d - 1, Palette.DARK_PLANK);
        k.fill(x + w / 2 - 2, 0, z + d - 1, x + w / 2 - 2, 4, z + d - 1, Palette.STONE_CHISELED);
        k.fill(x + w / 2 + 1, 0, z + d - 1, x + w / 2 + 1, 4, z + d - 1, Palette.STONE_CHISELED);
        for (int dx = 4; dx < w - 4; dx += 5) {
            k.fill(x + dx, 2, z + d - 1, x + dx + 1, 4, z + d - 1, Palette.GLASS);
            k.fill(x + dx, 8, z + d - 1, x + dx + 1, 10, z + d - 1, Palette.GLASS);
        }
        for (int dz = 6; dz < d - 4; dz += 6) {
            k.fill(x + w - 1, 2, z + dz, x + w - 1, 4, z + dz + 1, Palette.GLASS);
        }
        k.roof(x - 3, z - 3, x + w + 2, z + d + 2, h + 1, true, Palette.SLATE_STAIR,
                Palette.STONE_BRICK, 2);
        if (tall) {
            k.roundTower(x + 3, z + 3, 3, 0, h + 8, Palette.STONE_BRICK,
                    Palette.STONE_CHISELED, Palette.SLATE_STAIR, 1);
        }
        // 前院：院墙、门、水池、地窖口
        int yx1 = x + w + 1;
        int yx2 = yx1 + 9;
        k.pave(yx1, z, yx2, z + d - 1, Palette.GRASS, Palette.PATH);
        for (int i = 0; i <= 9; i++) {
            for (int y = 0; y <= 3; y++) {
                k.set(yx1 + i, y, z, Palette.COBBLE);
                k.set(yx1 + i, y, z + d - 1, Palette.COBBLE);
            }
        }
        for (int dz = 1; dz < d - 1; dz++) {
            for (int y = 0; y <= 3; y++) {
                k.set(yx2, y, z + dz, Palette.COBBLE);
            }
        }
        for (int y = 0; y <= 3; y++) {
            k.set(yx1, y, z + d / 2 - 1, null);
            k.set(yx1, y, z + d / 2, null);
        }
        // 水池：三格乘五格，石沿
        for (int dx = 0; dx <= 4; dx++) {
            for (int dz = 0; dz <= 2; dz++) {
                k.set(yx1 + 3 + dx, -1, z + 6 + dz, Palette.WATER);
            }
        }
        for (int dx = -1; dx <= 5; dx++) {
            k.set(yx1 + 3 + dx, 0, z + 5, Palette.STONE_CHISELED);
            k.set(yx1 + 3 + dx, 0, z + 9, Palette.STONE_CHISELED);
        }
        // 地窖口：下去一层，两格见方，放酒
        for (int i = 0; i < 3; i++) {
            k.set(yx1 + 2, -1 - i, z + 14 + i, Palette.STONE_BRICK);
        }
        for (int y = -3; y <= -2; y++) {
            k.set(yx1 + 2, y, z + 17, null);
        }
        for (int dx = 0; dx <= 3; dx++) {
            for (int dz = 0; dz <= 3; dz++) {
                k.set(yx1 + 1 + dx, -3, z + 18 + dz, Palette.COBBLE);
            }
        }
        k.set(yx1 + 1, -2, z + 18, Palette.BARREL);
        k.set(yx1 + 2, -1, z + 16, Palette.OAK_TRAPDOOR);
        // 侧门：开向私巷，不走正街
        for (int y = 0; y <= 2; y++) {
            k.set(yx2, y, z + d - 3, null);
        }
        k.set(yx2, 3, z + d - 3, Palette.DARK_PLANK);
        k.bump("府邸");
    }

    private static void stable(BuildKit k, int x, int z, int w) {
        k.claim("马厩", x - 1, z - 1, x + w, z + 11);
        stalls(k, x, z, w, 11, 0);
        // 屋面压在墙顶：原来是 8 而墙只到 6，中间留了一圈一格的缝，
        // 站近了能看进去，雨天漏雨
        k.roof(x - 2, z - 2, x + w + 1, z + 11, 7, true, Palette.SHINGLE_STAIR,
                Palette.SPRUCE_PLANK, 1);
    }

    /**
     * 一间能用的马厩：双开大门、分栏、草料、水槽、门外的拴马桩。
     *
     * <p>原来的写法是"三面墙 + 掏空 + 一排干草"——从外面看是马厩，走进去才发现
     * 四面都是墙，一扇门也没有，而且栏是拦在<b>墙外面</b>的。马厩的第一功能是
     * "马进得去、人进得去"，所以这一版先解决门，再解决一间一间的栏与水草。
     *
     * @param base 地面那一层（地板顶面）
     */
    private static void stalls(BuildKit k, int x, int z, int w, int d, int base) {
        k.fill(x - 1, base - 1, z - 1, x + w, base, z + d, Palette.COBBLE);
        k.walls(x, base + 1, z, x + w - 1, base + 6, z + d - 1, Palette.SPRUCE_PLANK);
        k.carve(x + 1, base + 1, z + 1, x + w - 2, base + 5, z + d - 2);
        // 大门：朝街那面墙的正中，双开、两格高——马能过，人能推
        int gx = x + (w - 2) / 2;
        k.gate(gx, base + 1, z, "north", true, "spruce", "马厩门");
        // 门两侧挂灯，夜里认得出来这是马厩不是柴房
        k.set(gx - 1, base + 2, z - 1, Palette.LANTERN);
        k.set(gx + 2, base + 2, z - 1, Palette.LANTERN);
        // 分栏：每六格一间。栏到胸口，人看得见马，马跨不出来
        for (int i = 6; i < w - 3; i += 6) {
            k.fill(x + i, base + 1, z + 1, x + i, base + 3, z + d - 3, Palette.SPRUCE_FENCE);
        }
        // 每间：靠后墙一垛草、一只桶
        for (int i = 0; i * 6 + 3 < w - 2; i++) {
            int sx = x + i * 6 + 3;
            if (sx >= x + w - 2) {
                break;
            }
            k.set(sx, base + 1, z + d - 3, Palette.HAY);
            k.set(sx, base + 2, z + d - 3, Palette.HAY);
            k.set(sx + 1, base + 1, z + d - 3, Palette.BARREL);
        }
        // 水槽：石沿围一格水，就在走廊边上——马出门前先喝水
        int tx = x + 2;
        int tz = z + d - 6;
        k.fill(tx - 1, base + 1, tz - 1, tx + 2, base + 1, tz + 1, Palette.STONE_CHISELED);
        k.fill(tx, base + 1, tz, tx + 1, base + 1, tz, Palette.WATER);
        // 门外：拴马桩与草料车
        for (int i = 0; i < 3; i++) {
            k.fill(x + 2 + i * 4, base + 1, z - 2, x + 2 + i * 4, base + 3, z - 2,
                    Palette.SPRUCE_FENCE);
        }
        k.set(x + w - 3, base + 1, z - 2, Palette.HAY);
        k.bump("马厩");
    }

    // -- 天平与钥匙（银行 + 典当行） -------------------------------------------

    /**
     * 银行与典当行二合一：一座两层的账房，一层对外的客户厅，二层账房，地下是库。
     *
     * <p>形制照意大利的 counting house 来。这一行的全部家当本来只是一张长凳——
     * 「banca」就是柜台，银行家的钱赔光了，公会当街把他的长凳砸断，那叫 bancarotta，
     * 也就是破产。所以这座楼的第一件事不是"气派"，是<b>柜台</b>：进门穿过大厅，
     * 正对一道横贯整面墙的长柜台，柜台上有铁栅窗口，柜台后面是职员与账架。
     *
     * <p>第二件事是<b>那扇铁门</b>。柜台正中断开两格，装一扇铁门，门后就是向下的
     * 石梯。玩家的记忆点全在这儿：钱在柜台后面，柜台后面有一扇锁着的门。
     *
     * <p>第三件事是<b>典当窗口挂红布</b>。中世纪佛罗伦萨的当铺在门拱上挂一块红布，
     * 那是"公开的高利贷者"的标记——不需要识字的招牌。
     *
     * <p>旧版是一间三十七乘二十三的盒子：西墙凿一个两格宽的洞当门，屋顶按"进深一半"
     * 起坡，二十格高，把整栋楼压成一口倒扣的锅；地下库房的斜道没有垫实也没有梯子，
     * 人根本下不去。这里整栋重砌。
     */
    static void bankAndPawn(BuildKit k, int x, int z) {
        int x1 = x - 1;
        int x2 = x + 37;
        int z1 = z - 1;
        int z2 = z + 23;
        int deck = 7;
        int eave = 12;
        k.claim("银行", x1, z1, x2, z2);
        // 正门在北墙。棚子是后建的、柱距三格，门要落在两根柱子之间
        int doorX = x + 19;

        // 地面与墙。地基只铺一层：再往下要挖库房，填实了就得再凿开
        k.fill(x1 - 1, -1, z1 - 1, x2 + 1, -1, z2 + 1, Palette.STONE_BRICK);
        k.walls(x1, 0, z1, x2, eave, z2, Palette.STONE_BRICK);
        k.carve(x1 + 1, 0, z1 + 1, x2 - 1, deck - 1, z2 - 1);
        k.carve(x1 + 1, deck + 1, z1 + 1, x2 - 1, eave - 1, z2 - 1);

        // 立面：整石的 palazzo。壁柱每五格一根、腰线一道、檐口一道
        for (int y = 0; y <= eave; y++) {
            for (int xx = x1; xx <= x2; xx++) {
                for (int zz = z1; zz <= z2; zz++) {
                    boolean edge = xx == x1 || xx == x2 || zz == z1 || zz == z2;
                    if (!edge) {
                        continue;
                    }
                    boolean band = y == deck || y == eave;
                    int along = (zz == z1 || zz == z2) ? xx : zz;
                    if (band || Math.floorMod(along, 5) == 0) {
                        k.set(xx, y, zz, Palette.STONE_CHISELED);
                    }
                }
            }
        }
        // 壁柱往外凸一格：没有这条竖线，三十九格宽的石墙是一整片灰
        for (int y = 0; y <= eave; y++) {
            for (int xx = x1; xx <= x2; xx++) {
                if (Math.floorMod(xx, 5) != 0) {
                    continue;
                }
                k.set(xx, y, z1 - 1, Palette.STONE_CHISELED);
                k.set(xx, y, z2 + 1, Palette.STONE_CHISELED);
            }
            for (int zz = z1; zz <= z2; zz++) {
                if (Math.floorMod(zz, 5) != 0) {
                    continue;
                }
                k.set(x1 - 1, y, zz, Palette.STONE_CHISELED);
                k.set(x2 + 1, y, zz, Palette.STONE_CHISELED);
            }
        }

        // 勒脚：底下三格既换料、又往外长一格。
        //
        // 只换颜色不够——同一面墙上深浅差一档，远看还是平的。真正让 palazzo 站住的是
        // 底座比墙宽：眼睛先看见一道台阶，才知道上面那堵墙是"立"着的。
        for (int y = 0; y <= 2; y++) {
            for (int xx = x1; xx <= x2; xx++) {
                for (int zz = z1; zz <= z2; zz++) {
                    boolean edge = xx == x1 || xx == x2 || zz == z1 || zz == z2;
                    if (edge) {
                        Block now = k.scene.get(xx, y, zz);
                        if (now != null && now.id().contains("stone_bricks")) {
                            k.set(xx, y, zz, Palette.ANDESITE);
                        }
                    }
                }
            }
            // 外扩那一圈。西面门口三格留空，让出一小块门斗
            for (int xx = x1 - 1; xx <= x2 + 1; xx++) {
                // 门口四格让开：台基比墙宽一圈，不留口子的话门就被自家勒脚堵死
                if (xx < doorX - 1 || xx > doorX + 2) {
                    k.set(xx, y, z1 - 1, Palette.ANDESITE);
                }
                k.set(xx, y, z2 + 1, Palette.ANDESITE);
            }
            for (int zz = z1 - 1; zz <= z2 + 1; zz++) {
                k.set(x2 + 1, y, zz, Palette.ANDESITE);
                if (zz >= z + 10 && zz <= z + 13) {
                    continue;
                }
                k.set(x1 - 1, y, zz, Palette.ANDESITE);
            }
        }
        // 勒脚上沿一条线脚，把台基和墙分开
        for (int xx = x1 - 1; xx <= x2 + 1; xx++) {
            k.set(xx, 3, z1 - 1, Palette.STONE_CHISELED);
            k.set(xx, 3, z2 + 1, Palette.STONE_CHISELED);
        }
        for (int zz = z1 - 1; zz <= z2 + 1; zz++) {
            k.set(x2 + 1, 3, zz, Palette.STONE_CHISELED);
            if (zz < z + 10 || zz > z + 13) {
                k.set(x1 - 1, 3, zz, Palette.STONE_CHISELED);
            }
        }
        // 檐口：整圈挑出一格。二十四格高的墙要有压顶，否则屋顶像扣上去的盖子
        for (int xx = x1 - 1; xx <= x2 + 1; xx++) {
            k.set(xx, eave, z1 - 1, Palette.STONE_CHISELED);
            k.set(xx, eave, z2 + 1, Palette.STONE_CHISELED);
        }
        for (int zz = z1 - 1; zz <= z2 + 1; zz++) {
            k.set(x1 - 1, eave, zz, Palette.STONE_CHISELED);
            k.set(x2 + 1, eave, zz, Palette.STONE_CHISELED);
        }

        // 正门朝北。
        //
        // 原来朝西——那是打算面向"南大道"，可西边早就被一排铺面封死了：
        // 出门三格就是一堵两格高的邻居墙，门外那块地永远走不进来。图上看着有门，
        // 进服推门只能对着墙。北面是环市集退下来的空地，那才是真有人走的一面。
        k.gate(doorX, 0, z1, "north", true, "dark_oak", "银行正门");
        // 券顶与横匾
        for (int y = 2; y <= 3; y++) {
            for (int dx = -2; dx <= 3; dx++) {
                k.set(doorX + dx, y, z1, Palette.STONE_CHISELED);
            }
        }
        for (int dx = -1; dx <= 2; dx++) {
            k.set(doorX + dx, 4, z1, Palette.SIGN);
        }
        // 门前檐：外挑一格，门就有了"框"
        for (int dx = -2; dx <= 3; dx++) {
            k.set(doorX + dx, 5, z1 - 1, Palette.STONE_CHISELED);
        }
        k.set(doorX, 4, z1 - 1, Palette.LANTERN_HANGING);
        k.set(doorX - 2, 3, z1 - 1, Palette.GOLD);
        k.set(doorX + 3, 3, z1 - 1, Palette.GOLD);

        // 门前不用另铺广场：这面墙外面就是环城路（RING=120），城里最热闹的一条。
        // 我一开始在这儿铺了块"银行广场"，把马路挖开重铺——那是把街当院子用。
        // 银行本来就该开在大街上，门口那排柱子是环路的柱廊，门落在两根柱子之间就行

        // 一层：客户厅。地面铺成棋盘，柱子撑住楼板
        for (int xx = x1 + 1; xx <= x + 30; xx++) {
            for (int zz = z1 + 1; zz <= z2 - 1; zz++) {
                k.set(xx, -1, zz, Math.floorMod(xx + zz, 2) == 0
                        ? Palette.STONE_CHISELED : Palette.ANDESITE_POLISHED);
            }
        }
        for (int xx : new int[] {x1 + 11, x1 + 21}) {
            for (int zz = z1 + 3; zz <= z2 - 3; zz += 6) {
                k.fill(xx, 1, zz, xx, deck - 1, zz, Palette.STONE_BRICK);
                k.set(xx, 1, zz, Palette.STONE_CHISELED);
                k.set(xx, deck - 1, zz, Palette.STONE_CHISELED);
            }
        }
        // 靠门一侧的长凳：坐着等号的人。银行里最多的就是等待
        for (int zz = z1 + 3; zz <= z1 + 8; zz++) {
            k.set(x1 + 2, 0, zz, Block.stairs(Palette.SLATE_STAIR, "east"));
        }
        for (int zz = z2 - 8; zz <= z2 - 3; zz++) {
            k.set(x1 + 2, 0, zz, Block.stairs(Palette.SLATE_STAIR, "east"));
        }

        // 门前的小广场。银行这门手艺本来就在广场上——一张长凳、一架天平，
        // 当街换钱。后来才搬进屋子，但"门口要有一块能站人的地"这条没变。
        // 西边原本是一片长草的空地，门开在那儿等于开在野地里
        for (int xx = x1 - 12; xx <= x1 - 2; xx++) {
            for (int zz = z1 + 2; zz <= z2 - 2; zz++) {
                boolean belt = xx <= x1 - 10;
                k.set(xx, -1, zz, belt ? Palette.STONE_BRICK
                        : (Math.floorMod(xx + zz, 3) == 0 ? Palette.ANDESITE : Palette.COBBLE));
            }
        }
        // 广场中央立一根兑换牌：石柱顶一块牌子，旁边挂一串钱
        int px = x1 - 7;
        int pz = z + 11;
        k.fill(px, 0, pz, px, 3, pz, Palette.STONE_CHISELED);
        k.set(px, 4, pz, Palette.SIGN);
        k.set(px - 1, 3, pz, Palette.GOLD);
        k.set(px + 1, 3, pz, Palette.LANTERN);
        // 两张当街的长凳：等着开门的人
        for (int zz : new int[] {z + 4, z + 18}) {
            k.fill(x1 - 5, 0, zz, x1 - 4, 0, zz, Palette.OAK_PLANK);
        }

        bankCounter(k, x, z, x1, x2, z1, z2);
        bankVault(k, x, z, x1, x2, z1, z2);
        bankUpstairs(k, x, z, x1, x2, z1, z2, deck, eave);

        // 屋面：坡高六格。旧版按"进深一半"起坡，二十格高
        k.hipRoof(x1, z1, x2, z2, eave + 1, 3, Palette.SLATE_STAIR, Palette.SLATE, 1);
        // 女儿墙：屋顶收成缓坡，再压一圈矮墙。这样从街上抬头，看到的是一条水平线，
        // 不是一片斜坡——公共建筑靠水平线立威
        for (int xx = x1 - 1; xx <= x2 + 1; xx++) {
            k.set(xx, eave + 1, z1 - 1, Palette.STONE_CHISELED);
            k.set(xx, eave + 1, z2 + 1, Palette.STONE_CHISELED);
        }
        for (int zz = z1 - 1; zz <= z2 + 1; zz++) {
            k.set(x1 - 1, eave + 1, zz, Palette.STONE_CHISELED);
            k.set(x2 + 1, eave + 1, zz, Palette.STONE_CHISELED);
        }
        k.bump("银行");
    }

    /** 柜台、铁门、上二层的梯子。银行的全部体面都在这三样上。 */
    private static void bankCounter(BuildKit k, int x, int z, int x1, int x2, int z1, int z2) {
        int bx = x + 31;
        int doorZ = z + 11;
        // 柜台横贯东半面：从 z1+5 一直到 z2-5，正中断开两格做铁门
        for (int zz = z1 + 5; zz <= z2 - 5; zz++) {
            if (Math.abs(zz - doorZ) <= 1) {
                continue;
            }
            k.fill(bx, 0, zz, bx + 1, 0, zz, Palette.STONE_BRICK);
            k.fill(bx, 1, zz, bx + 1, 1, zz, Palette.OAK_PLANK);
            // 铁栅：每五格留三格窗口。窗口是钱过的地方，栅栏是人不许过的地方
            if (Math.floorMod(zz - z1, 5) >= 3) {
                for (int y = 2; y <= 4; y++) {
                    k.set(bx, y, zz, Palette.IRON_BARS);
                }
            }
            // 典当那半边挂红布：佛罗伦萨的当铺就是这么挂牌的
            if (zz >= z2 - 8) {
                k.set(bx, 5, zz, Palette.WOOL_RED);
            } else {
                k.set(bx, 5, zz, Palette.STONE_CHISELED);
            }
        }
        // 柜台上：金堆、天平（用铁砧代）、账册、烛台
        for (int i = 0; i < 4; i++) {
            k.set(bx + 1, 2, z1 + 7 + i * 4, Palette.GOLD);
        }
        k.set(bx + 1, 2, z1 + 6, Palette.ANVIL);
        k.set(bx + 1, 2, z2 - 6, Palette.LECTERN);
        k.set(bx + 1, 3, z2 - 6, Palette.CANDLE);
        // 柜台后的账架：一墙的皮面账本
        for (int zz = z1 + 6; zz <= z2 - 6; zz++) {
            if (Math.abs(zz - doorZ) <= 2) {
                continue;
            }
            k.set(x2 - 1, 2, zz, Palette.BOOKSHELF);
            k.set(x2 - 1, 4, zz, Palette.BOOKSHELF);
        }
        k.set(x2 - 1, 1, z1 + 8, Palette.CHEST);
        k.set(x2 - 1, 1, z2 - 8, Palette.CHEST);

        // 铁门：柜台正中，两格宽。门后是一道向下的石梯
        k.carve(bx, 0, doorZ - 1, bx + 1, 3, doorZ + 1);
        k.fill(bx, 4, doorZ - 1, bx + 1, 4, doorZ + 1, Palette.STONE_CHISELED);
        k.gate(bx, 0, doorZ, "west", false, "iron", "库房门");
        // 门框：整石收边，铁门嵌在里面
        for (int dz : new int[] {-2, 2}) {
            k.fill(bx, 0, doorZ + dz, bx, 3, doorZ + dz, Palette.STONE_CHISELED);
        }

        // 上二层：西墙根一道直跑，七级到楼板
        int sx = x1 + 1;
        for (int i = 0; i <= 6; i++) {
            int zz = z1 + 9 - i;
            for (int w = 0; w <= 1; w++) {
                k.set(sx + w, i, zz, Block.stairs(Palette.SLATE_STAIR, "south"));
                k.set(sx + w, Math.max(0, i - 1), zz, Palette.STONE_BRICK);
            }
        }
    }

    /** 地下库房：一道石梯下来，四壁整石，中间一座铁库。 */
    private static void bankVault(BuildKit k, int x, int z, int x1, int x2, int z1, int z2) {
        int vx1 = x + 16;
        int vx2 = x + 36;
        int vz1 = z + 6;
        int vz2 = z + 20;
        int floor = -6;
        int top = -2;

        // 真的把土挖掉。只 carve 不 excavate 的话，图上有洞、进服是实心
        k.excavate(vx1, floor + 1, vz1, vx2, top, vz2);
        k.fill(vx1, floor, vz1, vx2, floor, vz2, Palette.STONE_BRICK);
        k.walls(vx1, floor + 1, vz1, vx2, top, vz2, Palette.STONE_BRICK);
        // 天花板：一层的地面就是它的顶，再补一层，免得从上面看下去一眼到底
        for (int xx = vx1; xx <= vx2; xx++) {
            for (int zz = vz1; zz <= vz2; zz++) {
                k.set(xx, -1, zz, Math.floorMod(xx + zz, 3) == 0
                        ? Palette.STONE_CRACKED : Palette.STONE_BRICK);
            }
        }

        // 下行石梯：从铁门进来，五级落到库房
        int doorZ = z + 11;
        int sx = x + 32;
        for (int i = 0; i <= 5; i++) {
            k.set(sx, -1 - i, doorZ + 1 + i, Block.stairs(Palette.SLATE_STAIR, "north"));
            k.set(sx, Math.max(floor, -2 - i), doorZ + 1 + i, Palette.STONE_BRICK);
        }
        // 梯子两侧各留一格净空。
        // 从第二级开始清：第一级旁边那一格正好是铁门的门槛，清了推门就掉下去
        for (int i = 1; i <= 5; i++) {
            k.set(sx - 1, -1 - i, doorZ + 1 + i, null);
            k.set(sx + 1, -1 - i, doorZ + 1 + i, null);
        }

        // 库中库：一道铁栅隔开的内库，放最要紧的东西
        int ix = vx2 - 8;
        k.fill(ix, floor + 1, vz1 + 2, ix, top - 1, vz2 - 2, Palette.IRON_BARS);
        k.carve(ix, floor + 1, vz1 + 6, ix, floor + 2, vz1 + 7);
        k.set(ix, floor + 3, vz1 + 6, Palette.STONE_CHISELED);
        k.set(ix, floor + 3, vz1 + 7, Palette.STONE_CHISELED);
        // 铁栅后面：一排锁箱、一只金柜
        for (int zz = vz1 + 3; zz <= vz2 - 3; zz += 2) {
            k.set(vx2 - 2, floor + 1, zz, Palette.CHEST);
        }
        k.fill(vx2 - 5, floor + 1, vz1 + 8, vx2 - 4, floor + 2, vz1 + 9, Palette.IRON);
        k.set(vx2 - 4, floor + 3, vz1 + 8, Palette.GOLD);
        // 外间：钱袋、账簿、一只验金的砧
        for (int i = 0; i < 5; i++) {
            k.set(vx1 + 2 + i * 2, floor + 1, vz2 - 3, Palette.CHEST);
        }
        k.set(vx1 + 3, floor + 1, vz1 + 3, Palette.ANVIL);
        k.set(vx1 + 5, floor + 1, vz1 + 3, Palette.BARREL);
        for (int zz = vz1 + 4; zz <= vz2 - 4; zz += 5) {
            k.set(vx1 + 8, top - 1, zz, Palette.LANTERN_HANGING);
        }
        // 天窗：一格铁栅，正午一道光打到库房中间
        k.set(vx1 + 9, -1, vz1 + 7, Palette.IRON_BARS);
        k.set(vx1 + 9, -2, vz1 + 7, null);
        k.bump("银行库房");
    }

    /** 二层：一条走廊、两间办公室、一间档案室。 */
    private static void bankUpstairs(BuildKit k, int x, int z, int x1, int x2, int z1, int z2,
            int deck, int eave) {
        // 楼板，楼梯井那一段留空
        for (int xx = x1 + 1; xx <= x2 - 1; xx++) {
            for (int zz = z1 + 1; zz <= z2 - 1; zz++) {
                boolean well = xx <= x1 + 2 && zz >= z1 + 3 && zz <= z1 + 9;
                if (!well) {
                    k.set(xx, deck, zz, Palette.DARK_PLANK);
                }
            }
        }
        // 走廊贴西墙，两间办公室沿东侧排开，东端是档案室
        int wall = x1 + 5;
        k.fill(wall, deck + 1, z1 + 1, wall, eave - 1, z2 - 1, Palette.PLASTER_CREAM);
        k.fill(wall, deck + 1, z + 11, x + 20, eave - 1, z + 11, Palette.PLASTER_CREAM);
        k.door(wall, deck + 1, z1 + 5, "west", "spruce", "总管室门");
        k.door(wall, deck + 1, z2 - 6, "west", "spruce", "首席室门");
        // 办公室：一张长桌、两把椅子、一墙账本、一扇窗
        for (int side = 0; side < 2; side++) {
            int tz = side == 0 ? z1 + 5 : z2 - 6;
            k.fill(wall + 3, deck + 1, tz - 2, wall + 7, deck + 1, tz - 2, Palette.DARK_PLANK);
            k.set(wall + 4, deck + 1, tz - 1, Block.stairs(Palette.SHINGLE_STAIR, "north"));
            k.set(wall + 6, deck + 1, tz - 1, Block.stairs(Palette.SHINGLE_STAIR, "north"));
            k.set(wall + 6, deck + 2, tz - 2, Palette.LECTERN);
            k.set(wall + 4, deck + 2, tz - 2, Palette.CANDLE);
            k.set(wall + 8, deck + 1, tz, Palette.CHEST);
            for (int zz = tz - 4; zz <= tz + 4; zz += 2) {
                k.set(x2 - 1, deck + 1, zz, Palette.BOOKSHELF);
            }
        }
        // 档案室：一排一排的账架，中间只留走道
        for (int xx = x + 22; xx <= x2 - 3; xx += 3) {
            for (int zz = z1 + 2; zz <= z2 - 2; zz++) {
                k.set(xx, deck + 1, zz, Palette.BOOKSHELF);
                k.set(xx, deck + 2, zz, Palette.BOOKSHELF);
            }
        }
        // 窗：四面都开，一格宽两格高不算窗，得有三格、有窗台、有过梁。
        // 一层朝街的窗一律带铁栅——钱庄的窗是防人的，不是观景的
        for (int xx = x1 + 5; xx <= x2 - 6; xx += 7) {
            bankWindow(k, xx, z1, true, deck);
            bankWindow(k, xx, z2, true, deck);
        }
        for (int zz = z1 + 5; zz <= z2 - 6; zz += 7) {
            if (Math.abs(zz - (z1 + 11)) <= 2) {
                continue;
            }
            bankWindow(k, x1, zz, false, deck);
            bankWindow(k, x2, zz, false, deck);
        }
        k.bump("银行账房");
    }

    /**
     * 一扇临街的窗：窗台一块石、三格玻璃、上面一道过梁。
     *
     * <p>一层带铁栅、二层不带——这一条不是装饰，是这栋楼的态度：底下的钱有人惦记，
     * 楼上的账本没人抢。
     */
    private static void bankWindow(BuildKit k, int x, int z, boolean alongX, int deck) {
        for (int i = 0; i <= 1; i++) {
            int px = alongX ? x + i : x;
            int pz = alongX ? z : z + i;
            // 一层：y4..5，带铁栅。窗放高一点，人够不着，栅栏才有用
            k.set(px, 3, pz, Palette.STONE_CHISELED);
            for (int y = 4; y <= 5; y++) {
                k.set(px, y, pz, Palette.IRON_BARS);
            }
            k.set(px, 6, pz, Palette.STONE_CHISELED);
            // 二层：y9..10，玻璃
            k.set(px, deck + 1, pz, Palette.STONE_CHISELED);
            for (int y = deck + 2; y <= deck + 3; y++) {
                k.set(px, y, pz, Palette.GLASS);
            }
            k.set(px, deck + 4, pz, Palette.STONE_CHISELED);
        }
    }

    /**
     * 南门里的客栈：一座"车马店"（coaching inn），不是一座大房子。
     *
     * <p>形制照现实里的车马店来：临街主楼 + 穿堂拱门 + 三面回廊的内院 + 后院马厩。
     * 这三样缺一样就散架——拱门是车马进来的路，回廊就是楼上客房的走廊，马厩必须在
     * 院子里而不在街对面。照搬《Skyrim》烛炉厅那种"一栋楼塞满人"的做法在这儿不成立：
     * 那在城中心，靠脚来；这里在城门口，人带着马和车来。
     *
     * <p>进门要一眼读得出三件事：柜台在哪、桌在哪、楼梯在哪。所以正门落在南半，
     * 柜台在右手边贴北墙，楼梯在左手边贴西墙，长桌走中轴——三条视线互不遮挡。
     *
     * <p>用地由 {@code MedievalCity.RESERVED} 给死：x −76..−26、z 246..282。
     */
    static void inn(BuildKit k, int x, int z) {
        int ax = x - 4;
        int az = z;
        innMain(k, ax, az);
        innKitchen(k, ax, az);
        innStable(k, ax, az);
        innYard(k, ax, az);
    }

    /** 临街主楼：一层大堂与私厅，二层四间客房，三层两间阁楼房。 */
    private static void innMain(BuildKit k, int ax, int az) {
        int x1 = ax + 32;
        int x2 = ax + 46;
        int z1 = az + 4;
        int z2 = az + 34;
        int top = 13;
        int f2 = 5;
        int f3 = 10;
        k.claim("客栈主楼", x1, z1, x2, z2);

        k.fill(x1 - 1, -3, z1 - 1, x2 + 1, -1, z2 + 1, Palette.STONE_BRICK);
        k.fill(x1, -1, z1, x2, -1, z2, Palette.OAK_PLANK);
        // 底下三格石砌（车马蹭得起），上面抹灰挂木架
        k.walls(x1, 0, z1, x2, 2, z2, Palette.STONE_BRICK);
        k.walls(x1, 3, z1, x2, top, z2, Palette.PLASTER_CREAM);

        // 木构架：角柱、腰梁、每四格一根立柱。没有它，十五格宽的抹灰面是一块白板
        for (int y = 3; y <= top; y++) {
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    if (x != x1 && x != x2 && z != z1 && z != z2) {
                        continue;
                    }
                    int along = (z == z1 || z == z2) ? x : z;
                    boolean corner = (x == x1 || x == x2) && (z == z1 || z == z2);
                    boolean beam = y == f2 || y == f3 || y == top;
                    if (corner || beam || Math.floorMod(along, 4) == 0) {
                        k.set(x, y, z, Palette.DARK_LOG);
                    }
                }
            }
        }

        // 两层楼板。楼梯井那两段留空——不掏空就没有"上得去"这回事
        for (int x = x1 + 1; x <= x2 - 1; x++) {
            for (int z = z1 + 1; z <= z2 - 1; z++) {
                boolean well1 = x <= x1 + 2 && z >= z1 + 2 && z <= z1 + 6;
                boolean well2 = x >= x1 + 3 && x <= x1 + 4 && z >= z2 - 6 && z <= z2 - 2;
                if (!well1) {
                    k.set(x, f2, z, Palette.DARK_PLANK);
                }
                if (!well2) {
                    k.set(x, f3, z, Palette.DARK_PLANK);
                }
            }
        }

        // 东立面：一道人行正门，一道车马拱门。两道分开，是车马店的规矩
        k.door(x2, 0, az + 16, "east", "dark_oak", "客栈正门");
        // 门套：整石的门框。深色门扇嵌在浅色石框里，从街上一眼就认得出那是门
        for (int dz = -1; dz <= 1; dz += 2) {
            k.fill(x2, 0, az + 16 + dz, x2, 2, az + 16 + dz, Palette.STONE_CHISELED);
        }
        k.set(x2, 2, az + 16, Palette.STONE_CHISELED);
        k.set(x2, 3, az + 16, Palette.SIGN);
        // 招牌：铁架挑出去三格，挂灯、挂木牌。中世纪的客人多数不识字，靠图认店
        k.fill(x2 + 1, 4, az + 19, x2 + 3, 4, az + 19, Palette.DARK_FENCE);
        k.set(x2 + 3, 3, az + 19, Palette.LANTERN_HANGING);
        k.set(x2 + 2, 3, az + 19, Palette.OAK_TRAPDOOR);
        k.set(x2 + 2, 2, az + 19, Palette.OAK_TRAPDOOR);
        k.set(x2 + 1, 2, az + 16, Palette.LANTERN);

        // 门廊：三级石台、两根柱子、一片雨棚、两盏挂灯。
        //
        // 客栈在街上要一眼认出来。靠的不是体量——十五格宽的三层楼在南门里并不稀奇——
        // 而是"门口这一团"：有台阶、有柱子、有灯、有招牌。人从街上走过来，先看见的是
        // 这一团光，然后才看见楼。
        k.fill(x2 + 1, -1, az + 13, x2 + 3, -1, az + 19, Palette.STONE_CHISELED);
        k.fill(x2 + 1, -1, az + 15, x2 + 3, -1, az + 17, Palette.STONE_BRICK);
        for (int z : new int[] {az + 13, az + 19}) {
            k.fill(x2 + 3, 0, z, x2 + 3, 3, z, Palette.SPRUCE_LOG);
        }
        k.fill(x2 + 1, 4, az + 13, x2 + 3, 4, az + 19,
                Block.stairs(Palette.TILE_STAIR, "east"));
        k.set(x2 + 3, 3, az + 13, Palette.LANTERN_HANGING);
        k.set(x2 + 3, 3, az + 19, Palette.LANTERN_HANGING);

        // 拱门：三格宽四格高，穿堂过楼到院子。券顶收成一格，才像拱而不像洞
        k.carve(x1, 0, az + 22, x2, 3, az + 24);
        for (int o = 0; o <= 3; o++) {
            k.set(x1, o, az + 22, null);
            k.set(x1, o, az + 24, null);
            k.set(x2, o, az + 22, null);
            k.set(x2, o, az + 24, null);
        }
        k.set(x1, 3, az + 23, Palette.STONE_CHISELED);
        k.set(x2, 3, az + 23, Palette.STONE_CHISELED);
        // 券石：拱门两侧的墙根用整石收边
        for (int o = 0; o <= 3; o++) {
            k.set(x1, o, az + 21, Palette.STONE_CHISELED);
            k.set(x1, o, az + 25, Palette.STONE_CHISELED);
            k.set(x2, o, az + 21, Palette.STONE_CHISELED);
            k.set(x2, o, az + 25, Palette.STONE_CHISELED);
        }
        k.set(x1, 4, az + 23, Palette.STONE_CHISELED);
        k.set(x2, 4, az + 23, Palette.STONE_CHISELED);
        // 车辙：拱门底下铺一条石道
        for (int x = x1; x <= x2; x++) {
            k.set(x, -1, az + 23, Palette.STONE_CHISELED);
        }

        // 一层：柜台在北墙前，L 形，短边朝门。
        //
        // 柜台不能贴着墙：贴着墙就没有"掌柜站的那一边"，吧台成了墙上的一块凸起。
        // 所以柜台退一格，墙根那一条留给掌柜与酒架。
        k.fill(x1 + 8, 0, z1 + 2, x2 - 1, 0, z1 + 2, Palette.DARK_PLANK);
        k.fill(x1 + 8, 1, z1 + 2, x2 - 1, 1, z1 + 2, Palette.OAK_PLANK);
        k.fill(x1 + 8, 0, z1 + 2, x1 + 8, 1, z1 + 5, Palette.OAK_PLANK);
        for (int x = x1 + 9; x <= x2 - 2; x += 2) {
            k.set(x, 2, z1, Palette.BARREL);
        }
        k.set(x1 + 11, 3, z1 + 1, Palette.LANTERN);
        k.set(x1 + 15, 3, z1 + 1, Palette.LANTERN);

        // 长桌：中轴两侧各一列，中间留两格通道——说书人站中间。
        //
        // 一桌两凳，加起来四格，两条这样的"桌子带"之后还剩两格通道。原来的排法把
        // 十三格填满到只剩一格，而且凳子用的是栅栏——那是一堵墙，不是凳子。
        for (int i = 0; i < 2; i++) {
            int bench = x1 + 3 + i * 6;
            int tx = bench + 1;
            k.fill(tx, 0, z1 + 8, tx + 1, 0, z1 + 16, Palette.OAK_PLANK);
            for (int z = z1 + 8; z <= z1 + 16; z++) {
                k.set(bench, 0, z, Block.stairs(Palette.SHINGLE_STAIR, "east"));
                k.set(tx + 2, 0, z, Block.stairs(Palette.SHINGLE_STAIR, "west"));
            }
            k.set(tx, 1, z1 + 10, Palette.CAULDRON);
            k.set(tx + 1, 1, z1 + 13, Palette.BARREL);
        }

        // 壁炉：南侧私厅的南墙。整栋楼只有一个火源，所有人围着它坐
        int fx = x1 + 6;
        k.fill(fx - 1, 0, z2 - 1, fx + 3, 0, z2 - 1, Palette.STONE_BRICK);
        k.fill(fx, 0, z2 - 1, fx + 2, 0, z2 - 1, Palette.CAMPFIRE);
        k.fill(fx, 1, z2 - 1, fx + 2, 4, z2 - 1, Palette.STONE_BRICK);
        k.set(fx + 1, 0, z2 - 2, Palette.CARPET_RED);
        k.set(fx - 1, 3, z2 - 1, Palette.LANTERN);
        k.set(fx + 3, 3, z2 - 1, Palette.LANTERN);

        // 悬灯：沿中轴挂四盏，从门口望进去，一路有光到底
        for (int z = z1 + 5; z <= z1 + 26; z += 7) {
            k.set(x1 + 7, 4, z, Palette.CHAIN);
            k.set(x1 + 7, 3, z, Palette.LANTERN_HANGING);
        }
        // 拱门里两盏：穿堂不能是黑洞
        k.set(x1 + 1, 3, az + 22, Palette.LANTERN);
        k.set(x1 + 1, 3, az + 24, Palette.LANTERN);

        // 私厅：拱门南边那半间。一张小桌、两把椅子——付得起钱的人不坐大堂
        k.fill(x1 + 2, 0, az + 27, x1 + 3, 0, az + 28, Palette.DARK_PLANK);
        k.set(x1 + 2, 1, az + 27, Palette.CANDLE);
        k.set(x1 + 4, 0, az + 27, Palette.OAK_FENCE);
        k.set(x1 + 1, 0, az + 28, Palette.OAK_FENCE);
        k.set(x2 - 2, 0, z2 - 3, Palette.CHEST);

        // 两道直跑楼梯：一层→二层在北端，二层→三层在南端
        flight(k, x1 + 1, z1 + 6, 0, 5, "north", 2);
        flight(k, x1 + 3, z2 - 2, 5, 5, "north", 2);

        // -- 二层：四间客房 ------------------------------------------------
        // 走廊三格宽贴西侧，房间沿东侧一列排开；走廊尽头一道门通院子回廊
        // 隔墙只砌到二层顶（f3−1）。砌到屋檐的话，这些墙会穿到三层，
        // 把阁楼切成一条条走不进去的窄缝——图上什么也看不出来
        k.fill(x1 + 6, f2 + 1, z1 + 1, x1 + 6, f3 - 1, z2 - 1, Palette.SPRUCE_PLANK);
        for (int z : new int[] {z1 + 7, z1 + 14, z1 + 21}) {
            k.fill(x1 + 6, f2 + 1, z, x2 - 1, f3 - 1, z, Palette.SPRUCE_PLANK);
        }
        k.door(x1, 6, az + 19, "west", "oak", "客栈回廊门");
        int[][] rooms = {
                {z1 + 1, z1 + 6},
                {z1 + 8, z1 + 13},
                {z1 + 15, z1 + 20},
                {z1 + 22, z2 - 1}};
        for (int[] r : rooms) {
            k.door(x1 + 6, f2 + 1, r[0] + 2, "west", "spruce", "客房门");
            k.set(x1 + 10, f2 + 1, r[0] + 1, Palette.WOOL_RED);
            k.set(x1 + 11, f2 + 1, r[0] + 1, Palette.WOOL_RED);
            k.set(x1 + 10, f2 + 1, r[0] + 2, Palette.WOOL_CREAM);
            k.set(x1 + 11, f2 + 1, r[0] + 2, Palette.WOOL_CREAM);
            k.set(x1 + 12, f2 + 1, r[1] - 1, Palette.CHEST);
            k.set(x1 + 7, f2 + 3, r[1] - 1, Palette.LANTERN);
            k.set(x2 - 1, f2 + 2, r[0] + 2, Palette.WOOL_BLUE);
            window(k, x2, f2 + 3, r[0] + 2, "west");
        }

        // -- 三层：两间阁楼房 ----------------------------------------------
        k.fill(x1 + 6, f3 + 1, z1 + 1, x1 + 6, top - 1, z2 - 1, Palette.SPRUCE_PLANK);
        k.fill(x1 + 6, f3 + 1, z1 + 13, x2 - 1, top - 1, z1 + 13, Palette.SPRUCE_PLANK);
        k.door(x1 + 6, f3 + 1, az + 8, "west", "spruce", "阁楼房");
        k.door(x1 + 6, f3 + 1, az + 28, "west", "spruce", "通铺门");
        for (int z = z1 + 3; z <= z1 + 8; z += 2) {
            k.set(x1 + 9, f3 + 1, z, Palette.WOOL_RED);
            k.set(x1 + 10, f3 + 1, z, Palette.WOOL_RED);
            k.set(x1 + 11, f3 + 1, z, Palette.WOOL_CREAM);
        }
        // 通铺：干草铺地，一条被子都没有
        for (int z = z1 + 16; z <= z2 - 3; z += 2) {
            k.set(x1 + 9, f3 + 1, z, Palette.HAY);
            k.set(x1 + 11, f3 + 1, z, Palette.HAY);
        }
        k.set(x1 + 8, f3 + 3, az + 22, Palette.LANTERN);
        k.set(x1 + 8, f3 + 3, az + 30, Palette.LANTERN);
        // 背街两面也要有窗。只有临街一面开窗的房子，从院子里看是一堵白墙
        for (int x : new int[] {x1 + 4, x1 + 8, x1 + 12}) {
            window(k, x, f2 + 3, z1, "south");
            window(k, x, f3 + 2, z1, "south");
        }
        for (int z : new int[] {az + 26, az + 31}) {
            window(k, x1, f2 + 3, z, "east");
            window(k, x1, f3 + 2, z, "east");
        }
        window(k, x2, f3 + 2, az + 20, "west");
        window(k, x2, f3 + 2, az + 28, "west");
        for (int wz : new int[] {az + 11, az + 27}) {
            window(k, x2, 2, wz, "west");
            window(k, x2, 2, wz + 1, "west");
        }

        // 屋面：坡高七格。旧版按"进深一半"起坡，四十三格进深就是二十三米高的一个尖，
        // 整栋楼成了一口倒扣的锅
        k.hipRoof(x1, z1, x2, z2, top + 1, 7, Palette.TILE_STAIR, Palette.TILE, 1);
        // 烟囱：壁炉的烟要出去，它同时也是这条街的天际线
        k.fill(fx, 5, z2 - 1, fx + 2, 20, z2 - 1, Palette.STONE_BRICK);
        k.set(fx + 1, 21, z2 - 1, Palette.STONE_CHISELED);
        k.bump("客栈");
    }

    /** 后厨北楼：一层灶、二层存货与伙计的床。 */
    private static void innKitchen(BuildKit k, int ax, int az) {
        int x1 = ax + 12;
        int x2 = ax + 31;
        int z1 = az + 4;
        int z2 = az + 16;
        int top = 8;
        k.claim("客栈后厨", x1, z1, x2, z2);

        k.fill(x1 - 1, -3, z1 - 1, x2 + 1, -1, z2 + 1, Palette.COBBLE);
        k.fill(x1, -1, z1, x2, -1, z2, Palette.STONE_BRICK);
        k.walls(x1, 0, z1, x2, 1, z2, Palette.STONE_BRICK);
        k.walls(x1, 2, z1, x2, top, z2, Palette.PLASTER_CREAM);
        for (int x = x1 + 1; x <= x2 - 1; x++) {
            for (int z = z1 + 1; z <= z2 - 1; z++) {
                boolean well = x <= x1 + 2 && z >= z1 + 2 && z <= z1 + 6;
                if (!well) {
                    k.set(x, 5, z, Palette.DARK_PLANK);
                }
            }
        }
        // 门朝院子（南），另一道双开门进货
        k.door(x1 + 10, 0, z2, "south", "spruce", "后厨门");
        k.gate(x1 + 18, 0, z2, "south", true, "spruce", "后厨货门");
        window(k, x1 + 6, 2, z2, "north");
        window(k, x1 + 14, 2, z2, "north");

        // 灶：三口灶眼一字排开，中间一口大锅。厨房的墙是黑的，因为烟一直在这儿
        k.fill(x1 + 2, 0, z1 + 1, x1 + 7, 0, z1 + 2, Palette.STONE_BRICK);
        k.set(x1 + 2, 1, z1 + 1, Palette.FURNACE);
        k.set(x1 + 4, 1, z1 + 1, Palette.CAULDRON);
        k.set(x1 + 6, 1, z1 + 1, Palette.SMOKER);
        k.fill(x1 + 2, 2, z1 + 1, x1 + 7, 3, z1 + 1, Palette.STONE_BRICK);
        // 长案：和面、切肉、摆盘都在这张板上
        k.fill(x1 + 10, 0, z1 + 3, x1 + 17, 0, z1 + 4, Palette.OAK_PLANK);
        k.set(x1 + 11, 1, z1 + 3, Palette.CRAFTING);
        k.set(x1 + 15, 1, z1 + 3, Palette.BARREL);
        for (int i = 0; i < 4; i++) {
            k.set(x1 + 3 + i * 2, 0, z2 - 3, Palette.BARREL);
        }
        k.set(x1 + 2, 0, z2 - 5, Palette.CHEST);
        for (int x = x1 + 11; x <= x1 + 16; x += 2) {
            k.set(x, 3, z1 + 6, Palette.CHAIN);
            k.set(x, 2, z1 + 6, Palette.LANTERN);
        }
        flight(k, x1 + 1, z1 + 6, 0, 5, "north", 2);
        for (int z = z1 + 3; z <= z1 + 9; z += 3) {
            k.set(x1 + 8, 6, z, Palette.HAY);
            k.set(x1 + 10, 6, z, Palette.HAY);
        }
        for (int i = 0; i < 5; i++) {
            k.set(x2 - 2, 6, z1 + 7 + i, i % 2 == 0 ? Palette.BARREL : Palette.CRATE);
        }
        k.set(x1 + 12, 7, z1 + 3, Palette.LANTERN);

        k.hipRoof(x1, z1, x2, z2, top + 1, 5, Palette.TILE_STAIR, Palette.TILE, 1);
        k.fill(x1 + 4, 9, z1 + 1, x1 + 5, 15, z1 + 1, Palette.STONE_BRICK);
        k.bump("客栈后厨");
    }

    /** 后院马厩：六栏、草料、水槽，门朝着院子。 */
    private static void innStable(BuildKit k, int ax, int az) {
        int x1 = ax;
        int x2 = ax + 11;
        int z1 = az + 6;
        int z2 = az + 34;
        k.claim("客栈马厩", x1, z1, x2, z2);

        k.fill(x1 - 1, -3, z1 - 1, x2 + 1, -1, z2 + 1, Palette.COBBLE);
        k.fill(x1, -1, z1, x2, -1, z2, Palette.COARSE);
        k.walls(x1, 0, z1, x2, 5, z2, Palette.SPRUCE_PLANK);
        k.carve(x1 + 1, 0, z1 + 1, x2 - 1, 4, z2 - 1);
        // 院墙那面（东）开双开大门，正对回廊
        k.gate(x2, 0, az + 18, "east", false, "spruce", "马厩门");
        k.set(x2, 3, az + 17, Palette.LANTERN);
        k.set(x2, 3, az + 21, Palette.LANTERN);

        // 分栏：每五格一间，栏到胸口——人看得见马，马跨不出来
        int boxes = 0;
        for (int z = z1 + 5; z <= z2 - 5; z += 5) {
            k.fill(x1 + 1, 1, z, x2 - 3, 3, z, Palette.SPRUCE_FENCE);
            boxes++;
        }
        for (int z = z1 + 2; z <= z2 - 3; z += 5) {
            k.set(x1 + 1, 1, z, Palette.HAY);
            k.set(x1 + 1, 2, z, Palette.HAY);
            k.set(x1 + 2, 1, z, Palette.BARREL);
        }
        // 水槽：进门右手，石沿围一格水
        k.fill(x2 - 5, 0, az + 16, x2 - 4, 0, az + 16, Palette.STONE_CHISELED);
        k.set(x2 - 4, 0, az + 16, Palette.WATER);
        k.fill(x1 + 1, 4, z1 + 1, x2 - 3, 4, z1 + 4, Palette.HAY);

        k.hipRoof(x1, z1, x2, z2, 6, 4, Palette.SHINGLE_STAIR, Palette.SPRUCE_PLANK, 1);
        k.bump("马厩");
        if (boxes < 5) {
            k.bump("马厩栏位不足");
        }
    }

    /** 内院：车辙、草料、马车，两面二层回廊。回廊接着主楼二层的门。 */
    private static void innYard(BuildKit k, int ax, int az) {
        int x1 = ax + 12;
        int x2 = ax + 31;
        int z1 = az + 17;
        int z2 = az + 34;

        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                boolean rut = z == az + 23 || z == az + 24;
                k.set(x, -1, z, rut ? Palette.STONE_CHISELED
                        : (k.rnd.nextInt(100) < 22 ? Palette.GRAVEL : Palette.COBBLE));
            }
        }
        // 南院墙：实墙，不开门。
        //
        // 本来在这儿留了一道小门"给货走"，墙外却是隔壁车马店的墙——门开出来正对着
        // 别人的立面，推门就撞墙。车马店本来也只有一道拱门：车和马都从那儿进。
        k.fill(x1, 0, z2 + 1, x2, 2, z2 + 1, Palette.STONE_BRICK);

        gallery(k, x1, z1, x2, z2, 5);

        k.fill(x1 + 1, 0, z2 - 3, x1 + 2, 2, z2 - 2, Palette.HAY);
        k.set(x1 + 4, -1, z2 - 5, Palette.CART);
        k.set(x1 + 7, 0, z2 - 5, Palette.BARREL);
        k.set(x1 + 7, 0, z2 - 6, Palette.BARREL);
        k.set(x2 - 2, 0, z1 + 3, Palette.CHEST);
        k.set(x1 + 1, 4, z1, Palette.LANTERN_HANGING);
        k.set(x2 - 1, 4, z1, Palette.LANTERN_HANGING);
        k.bump("客栈内院");
    }

    /**
     * 一道两层高的木回廊：柱子落地、板铺在五格高、栏杆到腰。
     *
     * <p>它不是装饰。车马店的回廊就是楼上客房的走廊——客人从二楼的门出来，
     * 沿着廊走到院子另一头那间房。所以廊必须<b>上得去</b>。
     *
     * <p>廊要两格深：一格走人、一格立栏杆。栏杆跟走道同格的话，那条廊一步都迈不动——
     * 栏杆在体素里是一整块，人站不进去。
     */
    private static void gallery(BuildKit k, int x1, int z1, int x2, int z2, int deckY) {
        int rail = deckY + 1;
        // 北廊：走道在 z1，栏杆在 z1+1（往下看的是院子）
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z1 + 1; z++) {
                k.set(x, deckY, z, Palette.OAK_PLANK);
            }
            // 东北角不留栏杆：两条廊要在这里转弯，栏杆一立就把弯堵死了
            if (x != x2) {
                k.set(x, rail, z1 + 1, Palette.OAK_FENCE);
            }
            if (Math.floorMod(x, 4) == 0) {
                k.fill(x, 1, z1, x, deckY - 1, z1, Palette.SPRUCE_LOG);
                k.fill(x, 1, z1 + 1, x, deckY - 1, z1 + 1, Palette.OAK_PLANK);
            }
        }
        // 东廊：走道在 x2（贴主楼），栏杆在 x2-1
        int stairZ = z2 - 4;
        for (int z = z1 + 2; z <= z2; z++) {
            k.set(x2 - 1, deckY, z, Palette.OAK_PLANK);
            k.set(x2, deckY, z, Palette.OAK_PLANK);
            if (z != stairZ) {
                k.set(x2 - 1, rail, z, Palette.OAK_FENCE);
            }
            if (Math.floorMod(z, 4) == 0) {
                k.fill(x2 - 1, 1, z, x2 - 1, deckY - 1, z, Palette.SPRUCE_LOG);
            }
        }
        // 院里的上廊梯：五级，正对栏杆断开的那一格
        for (int o = 0; o < 5; o++) {
            k.set(x2 - 2 - o, o, stairZ, Block.stairs(Palette.SHINGLE_STAIR, "east"));
        }
    }

    /** 墙上的窗：玻璃嵌进墙里，下面一块石台，上面一道挑檐。 */
    private static void window(BuildKit k, int x, int y, int z, String hood) {
        k.set(x, y, z, Palette.GLASS);
        k.set(x, y - 1, z, Block.slab("minecraft:stone_brick_slab", "bottom"));
        k.set(x, y + 1, z, Block.stairs(Palette.TILE_STAIR, hood));
    }

    /**
     * 一道直跑楼梯，往 {@code dir} 走 {@code steps} 级，{@code w} 格宽。
     *
     * <p>一级一格。玩家一次抬一格，所以一格一级就是"走得上去"；做成两格一跳，
     * 图上好看，脚上过不去。
     */
    private static void flight(BuildKit k, int x, int z, int y, int steps, String dir, int w) {
        String face = switch (dir) {
            case "north" -> "south";
            case "south" -> "north";
            case "west" -> "east";
            default -> "west";
        };
        for (int i = 0; i < steps; i++) {
            int cx = x + (dir.equals("east") ? i : dir.equals("west") ? -i : 0);
            int cz = z + (dir.equals("south") ? i : dir.equals("north") ? -i : 0);
            for (int j = 0; j < w; j++) {
                int px = cx + (dir.equals("north") || dir.equals("south") ? j : 0);
                int pz = cz + (dir.equals("east") || dir.equals("west") ? j : 0);
                k.set(px, y + i, pz, Block.stairs(Palette.SHINGLE_STAIR, face));
                if (y + i - 1 >= 0) {
                    k.set(px, y + i - 1, pz, Palette.DARK_PLANK);
                }
            }
        }
    }
}
