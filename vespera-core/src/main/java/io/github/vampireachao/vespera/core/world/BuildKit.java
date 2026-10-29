package io.github.vampireachao.vespera.core.world;

import io.github.vampireachao.vespera.core.voxel.Block;
import io.github.vampireachao.vespera.core.voxel.Palette;
import io.github.vampireachao.vespera.core.voxel.Scene;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * 中世纪的瓦刀与墨线。
 *
 * <p>这里只放"怎么砌"，不放"砌什么"。城的布局在 {@link MedievalCity}，地标在
 * {@link Landmarks}。分开的理由很实际：调一栋房子的比例，不该让整张地图跟着重建。
 *
 * <p>所有方法都接受局部坐标（城的原点在中央广场正中，地面是 y=0）。落地器会统一加上
 * 世界里的基准高度——建筑本身不需要知道海平面在哪。
 */
public final class BuildKit {

    public static final int FLOOR = 4;

    public final Scene scene;
    public final Random rnd;
    public final Map<String, Integer> stats = new LinkedHashMap<>();

    /**
     * 建筑的占地记录。盖的时候顺手记一笔，事后可以问"有没有两栋压在一起"。
     *
     * <p>之所以要记：体素没有"结构"这个概念，重叠之后方块照写，从外面看只是两个屋顶
     * 长在一起；等到有人走进去，才发现门被另一堵墙堵住。占地表是唯一能在收工后
     * 复查这件事的东西。
     */
    public final java.util.List<Claim> claims = new java.util.ArrayList<>();

    /** 一块占地：谁的、从哪到哪（含地基，不含出檐）。 */
    public record Claim(String kind, int x1, int z1, int x2, int z2) {

        public boolean hits(Claim other) {
            return x1 <= other.x2 && x2 >= other.x1 && z1 <= other.z2 && z2 >= other.z1;
        }

        public int overlap(Claim other) {
            int dx = Math.min(x2, other.x2) - Math.max(x1, other.x1) + 1;
            int dz = Math.min(z2, other.z2) - Math.max(z1, other.z1) + 1;
            return (dx <= 0 || dz <= 0) ? 0 : dx * dz;
        }

        public String where() {
            return x1 + ".." + x2 + " × " + z1 + ".." + z2;
        }
    }

    public void claim(String kind, int x1, int z1, int x2, int z2) {
        claims.add(new Claim(kind, Math.min(x1, x2), Math.min(z1, z2),
                Math.max(x1, x2), Math.max(z1, z2)));
    }

    /**
     * 这块地是不是已经有人占了。
     *
     * <p>和 {@link #free} 的分工：{@code free} 问"半空中有没有东西"（铺地之后仍然可用），
     * {@code taken} 问"有没有一栋房子已经站在这儿"。自动排屋必须两个都问——只问前者，
     * 两排房子就会在街角交叉，屋顶叠在一起。
     */
    public boolean taken(int x1, int z1, int x2, int z2) {
        int ax = Math.min(x1, x2);
        int bx = Math.max(x1, x2);
        int az = Math.min(z1, z2);
        int bz = Math.max(z1, z2);
        for (Claim c : claims) {
            if (ax <= c.x2() && bx >= c.x1() && az <= c.z2() && bz >= c.z1()) {
                return true;
            }
        }
        return false;
    }

    public BuildKit(Scene scene, long seed) {
        this.scene = scene;
        this.rnd = new Random(seed);
    }

    // -- 基本操作 -----------------------------------------------------------

    public void set(int x, int y, int z, Block block) {
        scene.set(x, y, z, block);
    }

    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, Block block) {
        scene.fill(x1, y1, z1, x2, y2, z2, block);
    }

    public void carve(int x1, int y1, int z1, int x2, int y2, int z2) {
        scene.carve(x1, y1, z1, x2, y2, z2);
    }

    public boolean free(int x1, int z1, int x2, int z2) {
        return !scene.anyBlockIn(x1, 0, z1, x2, 3, z2);
    }

    private static boolean isPaved(String id) {
        return id.contains("stone") || id.contains("cobble") || id.contains("andesite")
                || id.contains("brick") || id.contains("tile") || id.contains("deepslate")
                || id.contains("sandstone") || id.contains("sand");
    }

    /** 地面以上是否什么都没有（不看铺地）。补空用这个。 */
    public boolean openAir(int x1, int z1, int x2, int z2) {
        return !scene.anyBlockIn(x1, 0, z1, x2, 3, z2);
    }

    /** 这块地是不是已经铺过（街道、广场）——补空时要避开。 */
    public boolean paved(int x1, int z1, int x2, int z2) {
        for (int x = x1; x <= x2; x += 2) {
            for (int z = z1; z <= z2; z += 2) {
                Block block = scene.get(x, -1, z);
                if (block != null && isPaved(block.id())) {
                    return true;
                }
            }
        }
        return false;
    }

    public int stat(String key) {
        return stats.getOrDefault(key, 0);
    }

    public void bump(String key) {
        stats.merge(key, 1, Integer::sum);
    }

    /** 铺地：主材 + 随机杂色，免得一整条街像塑料。 */
    public void pave(int x1, int z1, int x2, int z2, Block main, Block accent) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                int roll = rnd.nextInt(100);
                Block block = roll < 8 ? accent : (roll < 20 ? Palette.COBBLE : main);
                set(x, -1, z, block);
                // 地面以下补一层，免得从侧面看见空洞
                set(x, -2, z, Palette.DIRT);
            }
        }
    }

    /** 一道墙：范围内四条边。 */
    public void walls(int x1, int y1, int z1, int x2, int y2, int z2, Block block) {
        int ax = Math.min(x1, x2);
        int bx = Math.max(x1, x2);
        int az = Math.min(z1, z2);
        int bz = Math.max(z1, z2);
        for (int x = ax; x <= bx; x++) {
            for (int y = y1; y <= y2; y++) {
                set(x, y, az, block);
                set(x, y, bz, block);
            }
        }
        for (int z = az; z <= bz; z++) {
            for (int y = y1; y <= y2; y++) {
                set(ax, y, z, block);
                set(bx, y, z, block);
            }
        }
    }

    /** 屋面的坡：沿某条轴起脊，两侧铺楼梯。 */
    public void roof(int x1, int z1, int x2, int z2, int y, boolean ridgeAlongZ, String stairId,
            Block gableBlock, int overhang) {
        int ax = Math.min(x1, x2);
        int bx = Math.max(x1, x2);
        int az = Math.min(z1, z2);
        int bz = Math.max(z1, z2);
        int width = ridgeAlongZ ? (bx - ax + 1) : (bz - az + 1);
        int half = width / 2;
        Block body = Palette.roofBody(stairId);

        for (int i = 0; i <= half; i++) {
            int level = y + i;
            if (ridgeAlongZ) {
                for (int z = az - overhang; z <= bz + overhang; z++) {
                    set(ax + i, level, z, Block.stairs(stairId, "east"));
                    set(bx - i, level, z, Block.stairs(stairId, "west"));
                    if (i > 0) {
                        // 楼梯正下方垫实：不垫的话从侧面能看见屋里的光
                        set(ax + i, level - 1, z, body);
                        set(bx - i, level - 1, z, body);
                    }
                }
                if (ax + i >= bx - i) {
                    for (int z = az - overhang; z <= bz + overhang; z++) {
                        set((ax + bx) / 2, level, z, body);
                    }
                }
            } else {
                for (int x = ax - overhang; x <= bx + overhang; x++) {
                    set(x, level, az + i, Block.stairs(stairId, "south"));
                    set(x, level, bz - i, Block.stairs(stairId, "north"));
                    if (i > 0) {
                        set(x, level - 1, az + i, body);
                        set(x, level - 1, bz - i, body);
                    }
                }
                if (az + i >= bz - i) {
                    for (int x = ax - overhang; x <= bx + overhang; x++) {
                        set(x, level, (az + bz) / 2, body);
                    }
                }
            }
        }

        // 山墙：把三角形填实，否则从两头能看进屋顶里
        if (ridgeAlongZ) {
            for (int x = ax; x <= bx; x++) {
                int h = Math.min(Math.min(x - ax, bx - x), half);
                for (int y2 = 0; y2 <= h; y2++) {
                    set(x, y + y2 - 1, az, gableBlock);
                    set(x, y + y2 - 1, bz, gableBlock);
                }
            }
        } else {
            for (int z = az; z <= bz; z++) {
                int h = Math.min(Math.min(z - az, bz - z), half);
                for (int y2 = 0; y2 <= h; y2++) {
                    set(ax, y + y2 - 1, z, gableBlock);
                    set(bx, y + y2 - 1, z, gableBlock);
                }
            }
        }
    }

    /** 圆盘：塔的楼板、塔顶、水井都用它。 */
    public void disc(int cx, int cz, int radius, int y, Block block) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz <= radius * radius + radius) {
                    set(cx + dx, y, cz + dz, block);
                }
            }
        }
    }

    /** 圆锥顶：一层层收，收成尖。 */
    public void cone(int cx, int cz, int radius, int y, int height, Block block) {
        for (int i = 0; i < height; i++) {
            int r = Math.max(0, (int) Math.round(radius * (1 - i / (double) height)));
            disc(cx, cz, r, y + i, block);
        }
    }

    /**
     * 圆塔。墙是环，不是实心——留出可以走上去的楼梯井。
     */
    public void roundTower(int cx, int cz, int radius, int y0, int height, Block wall, Block trim,
            String roofStair, int roofMode) {
        for (int y = 0; y < height; y++) {
            double r = radius;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (d > r || d < r - 1.25) {
                        continue;
                    }
                    Block block = wall;
                    if (y % 9 == 4 && (dx + dz) % 3 == 0) {
                        block = Palette.GLASS_GRAY;
                    }
                    if (y < 2 || y == height - 1) {
                        block = trim;
                    }
                    set(cx + dx, y0 + y, cz + dz, block);
                }
            }
        }
        disc(cx, cz, radius, y0 + height, trim);
        if (roofMode == 0) {
            // 平台 + 雉堞
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dz * dz <= radius * radius + radius
                            && (Math.abs(dx) + Math.abs(dz)) % 2 == 0) {
                        set(cx + dx, y0 + height + 1, cz + dz, trim);
                    }
                }
            }
        } else {
            cone(cx, cz, radius, y0 + height + 1, radius + 2, Palette.SLATE);
        }
    }

    /** 雉堞：一段墙顶的齿。 */
    public void crenellate(int x1, int z1, int x2, int z2, int y, Block block) {
        int ax = Math.min(x1, x2);
        int bx = Math.max(x1, x2);
        int az = Math.min(z1, z2);
        int bz = Math.max(z1, z2);
        for (int x = ax; x <= bx; x++) {
            for (int z = az; z <= bz; z++) {
                boolean edge = x == ax || x == bx || z == az || z == bz;
                if (edge && ((x + z) & 1) == 0) {
                    set(x, y, z, block);
                }
            }
        }
    }

    /** 一盏街灯：木柱三格，顶上挂灯笼。夜里这就是路的形状。 */
    public void lamp(int x, int z) {
        set(x, 0, z, Palette.COBBLE);
        for (int y = 1; y <= 3; y++) {
            set(x, y, z, Palette.SPRUCE_FENCE);
        }
        set(x, 4, z, Palette.LANTERN);
        set(x, 5, z, Palette.SPRUCE_FENCE);
    }

    /** 树。城里只种在院子与墓地，街道不留树坑。 */
    public void tree(int x, int z, int height, boolean dark) {
        Block leaves = dark ? Palette.DARK_LEAVES : Palette.OAK_LEAVES;
        Block log = dark ? Palette.DARK_LOG : Palette.OAK_LOG;
        for (int y = 0; y < height; y++) {
            set(x, y, z, log);
        }
        int top = height;
        for (int dy = -2; dy <= 1; dy++) {
            int r = dy == 1 ? 1 : (dy == 0 ? 2 : 2);
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx == 0 && dz == 0 && dy < 1) {
                        continue;
                    }
                    if (Math.abs(dx) == r && Math.abs(dz) == r && r > 1) {
                        continue;
                    }
                    set(x + dx, top + dy, z + dz, leaves);
                }
            }
        }
    }

    /** 摊位：六根柱子、一块条纹布棚、一张柜台，柜上摆货。 */
    public void stall(int x, int z, Block clothA, Block clothB) {
        claim("摊位", x, z, x + 5, z + 3);
        for (int dx = 0; dx <= 5; dx++) {
            for (int dz = 0; dz <= 3; dz++) {
                boolean post = (dx == 0 || dx == 5) && (dz == 0 || dz == 3);
                if (post) {
                    for (int y = 0; y <= 2; y++) {
                        set(x + dx, y, z + dz, Palette.SPRUCE_FENCE);
                    }
                }
                set(x + dx, 3, z + dz, ((dx + dz) & 1) == 0 ? clothA : clothB);
                if (dx == 0 || dx == 5) {
                    set(x + dx, 4, z + dz, ((dx + dz) & 1) == 0 ? clothA : clothB);
                }
            }
        }
        set(x + 2, 4, z, clothA);
        set(x + 2, 4, z + 3, clothB);
        for (int dx = 1; dx <= 4; dx++) {
            set(x + dx, 1, z + 3, Palette.OAK_PLANK);
        }
        // 柜台上的货：桶、筐、南瓜、面包（用干草块代替）
        set(x + 1, 2, z + 3, Palette.BARREL);
        set(x + 2, 2, z + 3, Palette.CRATE);
        set(x + 4, 2, z + 3, Palette.HAY);
        set(x + 3, 0, z + 1, Palette.CRATE);
        set(x, 1, z + 1, Palette.BARREL);
    }

    /** 一栋半木结构民居。 */
    public void house(int x, int z, int w, int d, int floors, String front, boolean shop) {
        int y0 = 0;
        int ax = x;
        int az = z;
        int bx = x + w - 1;
        int bz = z + d - 1;

        // 占地只算墙体：地基外扩那一圈是石脚，与邻居共墙时本来就该连成一条
        claim("民居", ax, az, bx, bz);
        fill(ax - 1, y0 - 3, az - 1, bx + 1, y0 - 1, bz + 1, Palette.COBBLE);
        fill(ax, y0 - 1, az, bx, y0 - 1, bz, Palette.DARK_PLANK);

        String roofStair = switch (rnd.nextInt(10)) {
            case 0, 1, 2 -> Palette.SLATE_STAIR;
            case 3, 4 -> Palette.SHINGLE_STAIR;
            default -> Palette.TILE_STAIR;
        };
        // 墙面颜色：四种灰泥轮着来，一条街才不会像刷了同一桶漆
        Block plaster = switch (rnd.nextInt(4)) {
            case 0 -> Palette.PLASTER_CREAM;
            case 1 -> Palette.PLASTER_GRAY;
            case 2 -> Palette.PLASTER_OCHRE;
            default -> Palette.PLASTER;
        };
        Block gable = plaster;

        for (int f = 0; f < floors; f++) {
            int base = y0 + f * FLOOR;
            boolean jetty = f > 0;
            int jx1 = ax - (jetty && front.equals("west") ? 1 : 0);
            int jx2 = bx + (jetty && front.equals("east") ? 1 : 0);
            int jz1 = az - (jetty && front.equals("north") ? 1 : 0);
            int jz2 = bz + (jetty && front.equals("south") ? 1 : 0);

            for (int y = base; y < base + FLOOR; y++) {
                for (int xx = jx1; xx <= jx2; xx++) {
                    for (int zz = jz1; zz <= jz2; zz++) {
                        boolean edge = xx == jx1 || xx == jx2 || zz == jz1 || zz == jz2;
                        if (!edge) {
                            continue;
                        }
                        boolean beamRow = y == base || y == base + FLOOR - 1;
                        boolean post = (xx == jx1 || xx == jx2) && (zz == jz1 || zz == jz2);
                        boolean timber = beamRow || post || (Math.floorMod(xx + zz, 5) == 0 && f > 0);
                        Block block;
                        if (f == 0 && !shop) {
                            block = timber ? Palette.DARK_LOG : Palette.STONE_BRICK;
                        } else {
                            block = timber ? Palette.DARK_LOG : plaster;
                        }
                        set(xx, y, zz, block);
                    }
                }
            }

            // 窗：每层同一高度，间距 3–4 格
            int wy = base + 2;
            for (int xx = jx1 + 2; xx <= jx2 - 2; xx += 3) {
                if (front.equals("north") || front.equals("south")) {
                    set(xx, wy, jz1, Palette.GLASS);
                    set(xx, wy, jz2, Palette.GLASS);
                    set(xx, wy + 1, jz1, Block.stairs(Palette.SHINGLE_STAIR, "south"));
                    set(xx, wy + 1, jz2, Block.stairs(Palette.SHINGLE_STAIR, "north"));
                }
            }
            for (int zz = jz1 + 2; zz <= jz2 - 2; zz += 3) {
                if (front.equals("east") || front.equals("west")) {
                    set(jx1, wy, zz, Palette.GLASS);
                    set(jx2, wy, zz, Palette.GLASS);
                    set(jx1, wy + 1, zz, Block.stairs(Palette.SHINGLE_STAIR, "east"));
                    set(jx2, wy + 1, zz, Block.stairs(Palette.SHINGLE_STAIR, "west"));
                }
            }
        }

        // 门
        int cx = (ax + bx) / 2;
        int cz = (az + bz) / 2;
        switch (front) {
            case "south" -> {
                carve(cx - 1, 0, bz, cx, 2, bz);
                set(cx, 3, bz, Block.stairs(Palette.SHINGLE_STAIR, "north"));
                set(cx - 2, 2, bz, Palette.LANTERN);
            }
            case "north" -> {
                carve(cx - 1, 0, az, cx, 2, az);
                set(cx, 3, az, Block.stairs(Palette.SHINGLE_STAIR, "south"));
                set(cx - 2, 2, az, Palette.LANTERN);
            }
            case "east" -> {
                carve(bx, 0, cz - 1, bx, 2, cz);
                set(bx, 3, cz, Block.stairs(Palette.SHINGLE_STAIR, "west"));
                set(bx, 2, cz - 2, Palette.LANTERN);
            }
            default -> {
                carve(ax, 0, cz - 1, ax, 2, cz);
                set(ax, 3, cz, Block.stairs(Palette.SHINGLE_STAIR, "east"));
                set(ax, 2, cz - 2, Palette.LANTERN);
            }
        }

        // 店面：把底层正面拆开，加一条布棚
        if (shop) {
            Block awning = rnd.nextBoolean() ? Palette.WOOL_RED : Palette.WOOL_GREEN;
            if (front.equals("south") || front.equals("north")) {
                int zz = front.equals("south") ? bz : az;
                for (int xx = ax + 1; xx <= bx - 1; xx++) {
                    if (Math.abs(xx - cx) <= 2) {
                        continue;
                    }
                    carve(xx, 0, zz, xx, 2, zz);
                    set(xx, 3, zz, ((xx & 1) == 0) ? awning : Palette.WOOL_CREAM);
                }
            } else {
                int xx = front.equals("east") ? bx : ax;
                for (int zz = az + 1; zz <= bz - 1; zz++) {
                    if (Math.abs(zz - cz) <= 2) {
                        continue;
                    }
                    carve(xx, 0, zz, xx, 2, zz);
                    set(xx, 3, zz, ((zz & 1) == 0) ? awning : Palette.WOOL_CREAM);
                }
            }
        }

        int wallTop = y0 + floors * FLOOR;
        boolean ridgeAlongZ = front.equals("north") || front.equals("south");
        // 屋顶不出左右檐：排屋是共墙的，出檐会插进邻居的坡面，露出半截楼梯。
        // 只在临街那一侧留一格（overhang 沿屋脊方向，正好朝街与后院）。
        roof(ax, az, bx, bz, wallTop, ridgeAlongZ, roofStair, gable, 1);

        // 烟囱
        int chx = (front.equals("east") ? ax + 1 : bx - 1);
        int chz = (front.equals("south") ? az + 1 : bz - 1);
        int roofTop = wallTop + Math.min(w, d) / 2 + 1;
        for (int y = wallTop - 1; y <= roofTop; y++) {
            set(chx, y, chz, Palette.TILE);
        }
        set(chx, roofTop + 1, chz, Palette.CAMPFIRE);
        bump("民居");
    }
}
