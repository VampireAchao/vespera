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
            k.roundTower(t[0], t[1], 2, platform + 1, 50, Palette.STONE_BRICK,
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
        // 马厩：三面墙 + 单坡顶
        k.walls(sx, platform + 1, sz, sx + 34, platform + 6, sz + 14, Palette.SPRUCE_PLANK);
        k.carve(sx + 1, platform + 1, sz + 1, sx + 33, platform + 5, sz + 13);
        k.carve(sx + 1, platform + 1, sz + 14, sx + 10, platform + 4, sz + 14);
        k.roof(sx - 1, sz - 1, sx + 35, sz + 15, platform + 7, false, Palette.SHINGLE_STAIR,
                Palette.SPRUCE_PLANK, 1);
        for (int i = 0; i < 4; i++) {
            k.set(sx + 4 + i * 7, platform + 1, sz + 4, Palette.HAY);
            k.set(sx + 4 + i * 7, platform + 2, sz + 4, Palette.BARREL);
        }
        for (int i = 0; i < 3; i++) {
            for (int y = platform + 1; y <= platform + 3; y++) {
                k.set(sx + 36 + i * 2, y, sz + 3, Palette.SPRUCE_FENCE);
            }
        }
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
        int x1 = -120;
        int z1 = -34;
        int x2 = -72;
        int z2 = 34;

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
        tower(k, x2 - 12, z1 + 2, 16, 16, 54, 14, Palette.STONE_BRICK);
        // 钟面，朝广场
        int cx = x2 - 4;
        int cy = 42;
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
        int x1 = 74;
        int z1 = -38;
        int x2 = 136;
        int z2 = 42;

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
}
