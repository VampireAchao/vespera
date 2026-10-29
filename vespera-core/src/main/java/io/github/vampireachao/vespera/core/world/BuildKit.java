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

    /**
     * 一道门。
     *
     * <p>为什么要把"门"单独记下来：体素里"墙上有洞"和"有一扇门"长得一模一样，
     * 都是两格空。可玩家进去的第一件事是右键——洞是打不开的，门能开。
     * 所以每装一扇就记一笔，收工后可以逐扇问：它是不是两格高、是不是成对、
     * 从外面走不走得到。
     */
    public record Door(String kind, int x, int y, int z, String facing, String wood) {

        public String where() {
            return kind + " (" + x + "," + y + "," + z + ") 朝" + facing;
        }

        /** 门外那一格。 */
        public int outX() {
            return x + (facing.equals("east") ? 1 : facing.equals("west") ? -1 : 0);
        }

        public int outZ() {
            return z + (facing.equals("south") ? 1 : facing.equals("north") ? -1 : 0);
        }
    }

    public final java.util.List<Door> doors = new java.util.ArrayList<>();

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

    /**
     * 开一道真门：宽一格、高两格，装上下两半门扇，门槛压一块石。
     *
     * <p>两格高不是随便定的——玩家身高正好两格。开三格是"洞"，一眼能看到天花；
     * 开一格是"猫洞"，人过不去。所以门洞只挖两格，第三格留作过梁。
     *
     * <p>门扇用 {@code open=false}：玩家右键就开。这是"门"与"洞"唯一的区别。
     */
    public void door(int x, int y, int z, String facing, String wood, String kind) {
        carve(x, y, z, x, y + 1, z);
        scene.set(x, y, z, Palette.door(wood, facing, false));
        scene.set(x, y + 1, z, Palette.door(wood, facing, true));
        scene.set(x, y - 1, z, Palette.STONE_BRICK);
        doors.add(new Door(kind, x, y, z, facing, wood));
    }

    /**
     * 双开大门：宽两格、高两格。马厩、仓房、城门内侧那种能进车的门。
     *
     * <p>两扇各自带上下半，合成一扇对开的门——所以它是"两扇门"，不是"一个洞"。
     */
    public void gate(int x, int y, int z, String facing, boolean alongX, String wood,
            String kind) {
        for (int i = 0; i <= 1; i++) {
            int px = alongX ? x + i : x;
            int pz = alongX ? z : z + i;
            carve(px, y, pz, px, y + 1, pz);
            scene.set(px, y - 1, pz, Palette.COBBLE);
            scene.set(px, y, pz, Palette.door(wood, facing, false));
            scene.set(px, y + 1, pz, Palette.door(wood, facing, true));
            doors.add(new Door(kind, px, y, pz, facing, wood));
        }
    }

    public void carve(int x1, int y1, int z1, int x2, int y2, int z2) {
        scene.carve(x1, y1, z1, x2, y2, z2);
    }

    /**
     * 收尾：把所有门重新钉一遍，并把门口那一格腾出来。
     *
     * <p>城是一样一样堆上去的，后堆的会盖住先堆的。墙根种花的循环、指路牌、
     * 摊位木架，都会恰好落在门口那一格——图上完全看不出来，进服一推门才发现
     * 门外立着一截栅栏。门是最后一件事，所以最后再钉一遍。
     *
     * <p>只清"软"东西（花、栅栏、牌子、桶、草垛）。清到石头就说明这扇门
     * 确实被另一栋建筑压住了，那种情况要人去看，不能在这里硬凿。
     */
    public int sealDoors() {
        int fixed = 0;
        for (Door d : doors) {
            carve(d.x(), d.y(), d.z(), d.x(), d.y() + 1, d.z());
            set(d.x(), d.y(), d.z(), Palette.door(d.wood(), d.facing(), false));
            set(d.x(), d.y() + 1, d.z(), Palette.door(d.wood(), d.facing(), true));
            // 过梁：门上方那一格必须是实的，两格是门、三格就是洞。
            // 少数房子的墙在这一格被后盖的东西咬掉了，这里补回去，材料沿用旁边的墙
            Block over = scene.get(d.x(), d.y() + 2, d.z());
            if (over == null || over.isAir()) {
                Block near = wallMaterial(d);
                set(d.x(), d.y() + 2, d.z(), near);
                fixed++;
            }
            // 门口两格：软装饰清掉，石头留着
            for (int dy = 0; dy <= 1; dy++) {
                Block out = scene.get(d.outX(), d.y() + dy, d.outZ());
                if (out != null && soft(out.id())) {
                    carve(d.outX(), d.y() + dy, d.outZ(), d.outX(), d.y() + dy, d.outZ());
                    fixed++;
                }
            }
            // 屋里正对门那一格：家什要清掉，否则推门进去迎面一只桶
            int inX = d.x() - (d.outX() - d.x());
            int inZ = d.z() - (d.outZ() - d.z());
            for (int dy = 0; dy <= 1; dy++) {
                Block in = scene.get(inX, d.y() + dy, inZ);
                if (in != null && soft(in.id())) {
                    carve(inX, d.y() + dy, inZ, inX, d.y() + dy, inZ);
                    fixed++;
                }
            }
            // 门口那一格底下不能是空的——否则推门出去先掉一格。
            // 这种地方多半是街道边缘没铺到，补一块石阶就成
            Block under = scene.get(d.outX(), d.y() - 1, d.outZ());
            if (under == null || under.isAir()) {
                set(d.outX(), d.y() - 1, d.outZ(), Palette.COBBLE);
                fixed++;
            }
        }
        return fixed;
    }

    /** 这一面出去能不能站人：脚下有地、身上两格空。 */
    private boolean canExit(int ax, int az, int bx, int bz, String side) {
        int x = side.equals("east") ? bx + 1 : side.equals("west") ? ax - 1 : (ax + bx) / 2;
        int z = side.equals("south") ? bz + 1 : side.equals("north") ? az - 1 : (az + bz) / 2;
        Block under = scene.get(x, -1, z);
        if (under == null || under.isAir()) {
            return false;
        }
        for (int dy = 0; dy <= 1; dy++) {
            Block b = scene.get(x, dy, z);
            if (b != null && !b.isAir()) {
                return false;
            }
        }
        return true;
    }

    /** 门开哪一面：指定的那一面出不去就换对面；两个都不行就用原来的。 */
    private String exitSide(int ax, int az, int bx, int bz, String front) {
        if (canExit(ax, az, bx, bz, front)) {
            return front;
        }
        String back = switch (front) {
            case "south" -> "north";
            case "north" -> "south";
            case "east" -> "west";
            default -> "east";
        };
        return canExit(ax, az, bx, bz, back) ? back : front;
    }

    /** 这一格是不是某扇门的门口——柱廊立柱子之前必须先问一句。 */
    public boolean doorFront(int x, int z) {
        for (Door d : doors) {
            if (d.outX() == x && d.outZ() == z) {
                return true;
            }
        }
        return false;
    }

    /** 门旁边那堵墙用什么材料，过梁就跟着用什么。 */
    private Block wallMaterial(Door d) {
        int[] dx = {1, -1, 0, 0};
        int[] dz = {0, 0, 1, -1};
        for (int i = 0; i < 4; i++) {
            Block b = scene.get(d.x() + dx[i], d.y() + 2, d.z() + dz[i]);
            if (b != null && !b.isAir()) {
                return b;
            }
            Block c = scene.get(d.x() + dx[i], d.y() + 1, d.z() + dz[i]);
            if (c != null && !c.isAir() && !c.id().endsWith("_door")) {
                return c;
            }
        }
        return Palette.STONE_BRICK;
    }

    /** 花、栅栏、牌子、桶、草垛这些"摆上去的"，不是"砌上去的"。 */
    private static boolean soft(String id) {
        return id.contains("fence") || id.contains("sign") || id.contains("lantern")
                || id.contains("flower") || id.contains("poppy") || id.contains("dandelion")
                || id.contains("pot") || id.contains("barrel") || id.contains("crate")
                || id.contains("hay") || id.contains("torch") || id.contains("grass")
                || id.contains("wheat") || id.contains("carpet") || id.contains("sapling")
                || id.contains("lantern") || id.contains("chain") || id.contains("ladder")
                || id.contains("trapdoor") || id.contains("candle");
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
        // 旧瓦：整片一个色的大屋面在近景里像一块塑料板。
        // 按坐标散点换一点深色瓦，人眼看得出"这是一片一片铺的"。
        String wornId = stairId.equals("minecraft:deepslate_tile_stairs")
                ? "minecraft:deepslate_tile_stairs" : "minecraft:spruce_stairs";

        for (int i = 0; i <= half; i++) {
            int level = y + i;
            if (ridgeAlongZ) {
                for (int z = az - overhang; z <= bz + overhang; z++) {
                    boolean worn = Math.floorMod(z * 13 + level * 7, 23) == 0;
                    set(ax + i, level, z, Block.stairs(worn ? wornId : stairId, "east"));
                    set(bx - i, level, z, Block.stairs(worn ? wornId : stairId, "west"));
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
                    boolean worn = Math.floorMod(x * 13 + level * 7, 23) == 0;
                    set(x, level, az + i, Block.stairs(worn ? wornId : stairId, "south"));
                    set(x, level, bz - i, Block.stairs(worn ? wornId : stairId, "north"));
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
        house(x, z, w, d, floors, front, shop, 0);
    }

    /**
     * 同上，但按"料"来：0 民居、1 工匠、2 市集、3 贵族。
     *
     * <p>料不是装饰。一片城要能用眼睛分区，靠的就是墙色与瓦色——
     * 走进去先看见白墙板岩，那就是贵族区；看见陶土与烟囱，就是工匠街。
     */
    public void house(int x, int z, int w, int d, int floors, String front, boolean shop,
            int style) {
        int y0 = 0;
        int ax = x;
        int az = z;
        int bx = x + w - 1;
        int bz = z + d - 1;

        // 占地只算墙体：地基外扩那一圈是石脚，与邻居共墙时本来就该连成一条
        claim("民居", ax, az, bx, bz);
        fill(ax - 1, y0 - 3, az - 1, bx + 1, y0 - 1, bz + 1, Palette.COBBLE);
        fill(ax, y0 - 1, az, bx, y0 - 1, bz, Palette.DARK_PLANK);

        String roofStair = switch (style) {
            case 1 -> Palette.SHINGLE_STAIR;
            case 3 -> Palette.SLATE_STAIR;
            // 民居的瓦偏冷：一大片青灰压下去，圣座那点白金才浮得出来。
            // 原来七成是红瓦，整座城偏暖，宫殿和民居混在一起，远看没有主次
            default -> switch (rnd.nextInt(10)) {
                case 0, 1, 2, 3, 4, 5, 6 -> Palette.SLATE_STAIR;
                case 7, 8 -> Palette.TILE_STAIR;
                default -> Palette.SHINGLE_STAIR;
            };
        };
        // 墙面颜色：同一条街用同一种料，只有深浅的微差
        Block plaster = switch (style) {
            case 1 -> rnd.nextInt(3) == 0 ? Palette.TILE : Palette.PLASTER_OCHRE;
            case 2 -> rnd.nextBoolean() ? Palette.PLASTER_CREAM : Palette.SANDSTONE;
            case 3 -> rnd.nextBoolean() ? Palette.PLASTER : Palette.PLASTER_CREAM;
            default -> switch (rnd.nextInt(4)) {
                case 0 -> Palette.PLASTER_CREAM;
                case 1 -> Palette.PLASTER_GRAY;
                case 2 -> Palette.PLASTER_OCHRE;
                default -> Palette.PLASTER;
            };
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
                        boolean corner = (xx == jx1 || xx == jx2) && (zz == jz1 || zz == jz2);
                        // 立柱沿墙每四格一根。原来用的是 (x+z)%5 的斜纹——远处看是"有纹理"，
                        // 走到墙根前才发现那是一道道斜杠，既不是柱子也不承重，整面墙因此发飘。
                        int along = (zz == jz1 || zz == jz2) ? xx : zz;
                        boolean stud = Math.floorMod(along, 4) == 0;
                        boolean timber = beamRow || corner || (stud && f > 0);
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
                    set(xx, wy - 1, jz1, Block.slab("minecraft:stone_brick_slab", "bottom"));
                    set(xx, wy - 1, jz2, Block.slab("minecraft:stone_brick_slab", "bottom"));
                    set(xx, wy, jz1, Palette.GLASS);
                    set(xx, wy, jz2, Palette.GLASS);
                    set(xx - 1, wy, jz1, Palette.OAK_TRAPDOOR);
                    set(xx + 1, wy, jz1, Palette.OAK_TRAPDOOR);
                    set(xx - 1, wy, jz2, Palette.OAK_TRAPDOOR);
                    set(xx + 1, wy, jz2, Palette.OAK_TRAPDOOR);
                    set(xx, wy + 1, jz1, Block.stairs(Palette.SHINGLE_STAIR, "south"));
                    set(xx, wy + 1, jz2, Block.stairs(Palette.SHINGLE_STAIR, "north"));
                }
            }
            for (int zz = jz1 + 2; zz <= jz2 - 2; zz += 3) {
                if (front.equals("east") || front.equals("west")) {
                    set(jx1, wy - 1, zz, Block.slab("minecraft:stone_brick_slab", "bottom"));
                    set(jx2, wy - 1, zz, Block.slab("minecraft:stone_brick_slab", "bottom"));
                    set(jx1, wy, zz, Palette.GLASS);
                    set(jx2, wy, zz, Palette.GLASS);
                    set(jx1, wy, zz - 1, Palette.OAK_TRAPDOOR);
                    set(jx1, wy, zz + 1, Palette.OAK_TRAPDOOR);
                    set(jx2, wy, zz - 1, Palette.OAK_TRAPDOOR);
                    set(jx2, wy, zz + 1, Palette.OAK_TRAPDOOR);
                    set(jx1, wy + 1, zz, Block.stairs(Palette.SHINGLE_STAIR, "east"));
                    set(jx2, wy + 1, zz, Block.stairs(Palette.SHINGLE_STAIR, "west"));
                }
            }
        }

        // 门：宽一格、高两格，装真门扇。
        //
        // 原来是宽两格、高三格的"洞"——能走进去，但右键没反应，抬头能看见屋架。
        // 玩家的身高正好两格，所以门就是两格；第三格留成过梁，门上方才是墙。
        int cx = (ax + bx) / 2;
        int cz = (az + bz) / 2;
        String wood = switch (style) {
            case 1 -> "spruce";
            case 2 -> "birch";
            case 3 -> "dark_oak";
            default -> "oak";
        };
        // 门开在哪一面：先看指定的那一面，出不去就换对面。
        //
        // 街区是网格排的，但网格线不全是街——两条街之间还夹着后院与巷道。
        // 门正好开在"后院那一面"时，门外就是邻居的墙，这一栋等于没有门。
        // 与其事后封死，不如开的时候就换一面。
        String side = exitSide(ax, az, bx, bz, front);
        String hood = side.equals("south") ? "north"
                : side.equals("north") ? "south"
                : side.equals("east") ? "west" : "east";
        if (side.equals("south")) {
            if (!shop) {
                door(cx, 0, bz, "south", wood, "民居");
            } else {
                carve(cx - 1, 0, bz, cx, 2, bz);
            }
            set(cx, 2, bz, Block.stairs(Palette.SHINGLE_STAIR, hood));
            set(cx - 2, 2, bz, Palette.LANTERN);
        } else if (side.equals("north")) {
            if (!shop) {
                door(cx, 0, az, "north", wood, "民居");
            } else {
                carve(cx - 1, 0, az, cx, 2, az);
            }
            set(cx, 2, az, Block.stairs(Palette.SHINGLE_STAIR, hood));
            set(cx - 2, 2, az, Palette.LANTERN);
        } else if (side.equals("east")) {
            if (!shop) {
                door(bx, 0, cz, "east", wood, "民居");
            } else {
                carve(bx, 0, cz - 1, bx, 2, cz);
            }
            set(bx, 2, cz, Block.stairs(Palette.SHINGLE_STAIR, hood));
            set(bx, 2, cz - 2, Palette.LANTERN);
        } else {
            if (!shop) {
                door(ax, 0, cz, "west", wood, "民居");
            } else {
                carve(ax, 0, cz - 1, ax, 2, cz);
            }
            set(ax, 2, cz, Block.stairs(Palette.SHINGLE_STAIR, hood));
            set(ax, 2, cz - 2, Palette.LANTERN);
        }

        // 门口：门槛石与门外的一件家什。门洞本身是空的，人一眼看进去什么都没有；
        // 门口有块石头、有个桶，这栋房子才像有人在住。
        switch (front) {
            case "south" -> {
                set(cx - 1, y0 - 1, bz, Palette.STONE_BRICK);
                set(cx, y0 - 1, bz, Palette.STONE_BRICK);
                Block prop = (rnd.nextBoolean() ? Palette.BARREL : Palette.CRATE);
                set(cx + 2, y0, bz, prop);
            }
            case "north" -> {
                set(cx - 1, y0 - 1, az, Palette.STONE_BRICK);
                set(cx, y0 - 1, az, Palette.STONE_BRICK);
                set(cx - 3, y0, az, Palette.CRATE);
            }
            case "east" -> {
                set(bx, y0 - 1, cz - 1, Palette.STONE_BRICK);
                set(bx, y0 - 1, cz, Palette.STONE_BRICK);
                set(bx, y0, cz + 2, Palette.BARREL);
            }
            default -> {
                set(ax, y0 - 1, cz - 1, Palette.STONE_BRICK);
                set(ax, y0 - 1, cz, Palette.STONE_BRICK);
                set(ax, y0, cz - 3, Palette.BARREL);
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
        // 内装：楼板、楼梯、家什。原来楼上是一整个空腔，进门抬头能看见椽子，
        // 二楼却既没有地板也没有上去的路——走过十栋，十栋都一样空。
        interior(ax, az, bx, bz, floors, front, shop);
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

    /**
     * 一栋房子里面该有的东西。
     *
     * <p>体素房子的门本来就是通的，"能进去"早成立；问题是进去以后什么都没有：
     * 四面墙、一层地板、头顶一个通到屋架的空腔，二楼既没楼板也没上去的路。
     * 走十栋十栋一个样——这是"闲逛没意思"最直接的那个原因。
     *
     * <p>所以补三样：层间楼板、一道真能走上去的楼梯、一层二层的家什。
     * 楼梯一律贴着与门相对的那面墙，四步上一级，楼板在梯井处开两个格。
     * 家具一律用半砖（可跨过去），不挡路——房子里也不该有要跳过去的桌子。
     */
    private void interior(int ax, int az, int bx, int bz, int floors, String front, boolean shop) {
        int ix1 = ax + 1;
        int ix2 = bx - 1;
        int iz1 = az + 1;
        int iz2 = bz - 1;
        if (ix2 - ix1 < 3 || iz2 - iz1 < 3) {
            return;
        }
        // 楼板：每层一道；最上一层留着当阁楼，头顶就是屋架
        for (int f = 1; f < floors; f++) {
            int y = f * FLOOR - 1;
            fill(ix1, y, iz1, ix2, y, iz2, Palette.DARK_PLANK);
        }
        // 楼梯：贴背墙，四步上一级。单层房子没有"上一级"，就别在屋里摆一截孤零零的梯子
        if (floors >= 2) {
            int sz = front.equals("south") ? iz1 : iz2;
            int dir = front.equals("south") || front.equals("west") ? 1 : -1;
            int bx0 = dir > 0 ? ix1 + 1 : ix2 - 1;
            String face = dir > 0 ? "east" : "west";
            for (int i = 0; i < 4; i++) {
                int x = bx0 + dir * i;
                // 梯井：整个梯段上方的楼板都要掏空。只掏一两格的话，人爬到一半
                // 头顶就顶着上一层的楼板——看着有楼梯，实际上一级也上不去
                if (i <= 2) {
                    set(x, FLOOR - 1, sz, null);
                }
                for (int yy = 0; yy < i; yy++) {
                    set(x, yy, sz, Palette.DARK_PLANK);
                }
                set(x, i, sz, Block.stairs("minecraft:dark_oak_stairs", face));
            }
        }

        // 一层：靠墙一张矮桌、两只凳、箱与桶、一盏吊灯
        int mx = (ix1 + ix2) / 2;
        int mz = (iz1 + iz2) / 2;
        Block slab = Block.slab("minecraft:oak_slab", "bottom");
        set(mx, 0, mz, slab);
        if (mx + 1 <= ix2) {
            set(mx + 1, 0, mz, slab);
        }
        set(mx - 1, 0, mz, Palette.OAK_FENCE);
        set(mx + 2, 0, mz, Palette.OAK_FENCE);
        set(ix1, 0, iz2, rnd.nextBoolean() ? Palette.CHEST : Palette.BARREL);
        set(ix2, 0, iz1, rnd.nextBoolean() ? Palette.BARREL : Palette.CAULDRON);
        set(mx, floors >= 2 ? 2 : 1, mz, floors >= 2
                ? Palette.LANTERN_HANGING : Palette.LANTERN);
        if (shop) {
            // 铺面：一条柜台，柜上摆货，后墙一排架子
            int w = ix2 - ix1;
            for (int i = 1; i < w; i++) {
                set(ix1 + i, 0, mz + 1, slab);
            }
            set(ix1 + 1, 0, mz + 1, Palette.BARREL);
            set(ix1 + 3, 0, mz + 1, Palette.CRATE);
            set(ix2, 0, mz, Palette.BOOKSHELF);
            set(ix2, 1, mz, Palette.BOOKSHELF);
        }

        // 二层：床、箱、窗边小桌
        if (floors >= 2) {
            int uy = FLOOR - 1;
            set(ix1, uy + 1, iz1, Palette.WOOL_RED);
            set(ix1 + 1, uy + 1, iz1, Palette.WOOL_RED);
            set(ix1, uy + 1, iz1 + 1, Palette.DARK_PLANK);
            set(ix1, uy + 2, iz1 + 1, Palette.LANTERN);
            set(ix2, uy + 1, iz2, rnd.nextBoolean() ? Palette.CHEST : Palette.BARREL);
            if (ix2 - ix1 > 5) {
                set(ix2 - 1, uy + 1, iz2, slab);
                set(ix2 - 2, uy + 1, iz2, Palette.OAK_FENCE);
            }
        }
    }
}
