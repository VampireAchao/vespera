package io.github.vampireachao.vespera.plugin;

import io.github.vampireachao.vespera.core.CityPlan;
import io.github.vampireachao.vespera.core.CityPlan.Kind;
import io.github.vampireachao.vespera.core.CityPlan.Lot;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * 把 {@link CityPlan} 盖进世界：流明陨城。
 *
 * <p>灰盒的标准和学院一样：体量与动线是真的，材质是近似。但它要多做一件事——
 * <b>立起天际线</b>。玩家从南门进来，第一眼应该看见钟楼压在一排屋顶上，
 * 而不是一堆等高的方块。所以钟楼 54 格、教堂双塔 38 格、民居 11 格，高差是设计过的。
 *
 * <p>与 {@link CampusBuilder} 有若干相似的辅助方法（砌墙、铺地、坡顶）。暂时各留一份，
 * 等主城的形确定下来再抽成公共的——现在抽，会把两处都改坏。
 */
public final class CityBuilder {

    /** 城墙高。够高到翻不过去，够低到看得见里面。 */
    private static final int WALL_HEIGHT = 8;

    private final VesperaPlugin plugin;
    private final Random random = new Random(20261028L);

    public CityBuilder(VesperaPlugin plugin) {
        this.plugin = plugin;
    }

    public int build(World world, int ox, int oz) {
        int oy = groundY(world, ox, oz);
        int placed = 0;
        long started = System.currentTimeMillis();

        placed += wallRing(world, ox, oy, oz);
        placed += roads(world, ox, oy, oz);
        for (Lot lot : CityPlan.LOTS) {
            placed += switch (lot.kind()) {
                case PLAZA -> plaza(world, lot, ox, oy, oz);
                case MARKET -> market(world, lot, ox, oy, oz);
                case GATE -> gate(world, lot, ox, oy, oz);
                case CORNER -> cornerTower(world, lot, ox, oy, oz);
                case TOWER -> clockTower(world, lot, ox, oy, oz);
                case CATHEDRAL -> cathedral(world, lot, ox, oy, oz);
                case GUILD -> guild(world, lot, ox, oy, oz);
                case HOUSE -> house(world, lot, ox, oy, oz);
                case WALL -> 0;
            };
        }

        int[] spawn = CityPlan.spawn();
        world.setSpawnLocation(ox + spawn[0], oy + 1, oz + spawn[1]);

        long millis = System.currentTimeMillis() - started;
        plugin.getLogger().info("主城灰盒已就位：" + CityPlan.summary() + "，" + placed
                + " 个方块，" + millis + "ms。出生点在主街南段。");
        return placed;
    }

    // -- 城的骨架 -----------------------------------------------------------

    /** 四面城墙 + 雉堞。城门处留口。 */
    private int wallRing(World world, int ox, int oy, int oz) {
        int half = CityPlan.HALF;
        int placed = 0;
        for (int x = -half; x <= half; x++) {
            for (int z : new int[] {-half, half}) {
                for (int thickness = 0; thickness < 2; thickness++) {
                    int zz = z + (z > 0 ? thickness : -thickness);
                    // 城门开口：位于各边中点，宽 2*GATE_HALF
                    boolean gap = Math.abs(x - ox) <= CityPlan.GATE_HALF;
                    for (int y = 0; y < WALL_HEIGHT; y++) {
                        if (gap && y < 7) {
                            continue;
                        }
                        placed += set(world, ox + x, oy + y, oz + zz, Material.STONE_BRICKS);
                    }
                    // 雉堞：外沿每隔两格起一块
                    if (!gap && (x & 1) == 0) {
                        placed += set(world, ox + x, oy + WALL_HEIGHT, oz + zz, Material.STONE_BRICK_WALL);
                    }
                }
            }
        }
        for (int z = -half; z <= half; z++) {
            for (int x : new int[] {-half, half}) {
                for (int thickness = 0; thickness < 2; thickness++) {
                    int xx = x + (x > 0 ? thickness : -thickness);
                    boolean gap = Math.abs(z - oz) <= CityPlan.GATE_HALF;
                    for (int y = 0; y < WALL_HEIGHT; y++) {
                        if (gap && y < 7) {
                            continue;
                        }
                        placed += set(world, ox + xx, oy + y, oz + z, Material.STONE_BRICKS);
                    }
                    if (!gap && (z & 1) == 0) {
                        placed += set(world, ox + xx, oy + WALL_HEIGHT, oz + z, Material.STONE_BRICK_WALL);
                    }
                }
            }
        }
        return placed;
    }

    /** 十字主街：石砖铺地，两侧镶一条深色边。 */
    private int roads(World world, int ox, int oy, int oz) {
        int half = CityPlan.HALF;
        int r = CityPlan.ROAD_HALF;
        int placed = 0;
        for (int x = -r; x <= r; x++) {
            for (int z = -half; z <= half; z++) {
                Material material = Math.abs(x) == r ? Material.SMOOTH_STONE : Material.STONE_BRICKS;
                placed += set(world, ox + x, oy - 1, oz + z, material);
            }
        }
        for (int z = -r; z <= r; z++) {
            for (int x = -half; x <= half; x++) {
                Material material = Math.abs(z) == r ? Material.SMOOTH_STONE : Material.STONE_BRICKS;
                placed += set(world, ox + x, oy - 1, oz + z, material);
            }
        }
        return placed;
    }

    /** 广场：铺地 + 一口井。 */
    private int plaza(World world, Lot lot, int ox, int oy, int oz) {
        int minX = ox + Math.min(lot.x1(), lot.x2());
        int maxX = ox + Math.max(lot.x1(), lot.x2());
        int minZ = oz + Math.min(lot.z1(), lot.z2());
        int maxZ = oz + Math.max(lot.z1(), lot.z2());
        int placed = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                Material material = (x + z) % 4 == 0 ? Material.POLISHED_ANDESITE : Material.STONE_BRICKS;
                placed += set(world, x, oy - 1, z, material);
            }
        }
        // 井：石圈 + 水面
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                boolean rim = Math.abs(dx) == 3 || Math.abs(dz) == 3;
                placed += set(world, ox + dx, oy, oz + dz, rim ? Material.STONE_BRICK_WALL : Material.WATER);
            }
        }
        return placed;
    }

    /** 市集：铺地 + 十二个摊位（四根柱子 + 一块布顶）。 */
    private int market(World world, Lot lot, int ox, int oy, int oz) {
        int minX = ox + Math.min(lot.x1(), lot.x2());
        int maxX = ox + Math.max(lot.x1(), lot.x2());
        int minZ = oz + Math.min(lot.z1(), lot.z2());
        int maxZ = oz + Math.max(lot.z1(), lot.z2());
        int placed = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                placed += set(world, x, oy - 1, z, Material.COARSE_DIRT);
            }
        }
        Material[] cloth = {Material.RED_WOOL, Material.YELLOW_WOOL, Material.BLUE_WOOL, Material.GREEN_WOOL};
        int index = 0;
        for (int x = minX + 4; x <= maxX - 6; x += 12) {
            for (int z = minZ + 4; z <= maxZ - 6; z += 14) {
                Material canopy = cloth[index++ % cloth.length];
                for (int dx = 0; dx < 5; dx++) {
                    for (int dz = 0; dz < 5; dz++) {
                        boolean post = (dx == 0 || dx == 4) && (dz == 0 || dz == 4);
                        if (post) {
                            for (int y = 0; y < 3; y++) {
                                placed += set(world, x + dx, oy + y, z + dz, Material.OAK_FENCE);
                            }
                        }
                        placed += set(world, x + dx, oy + 3, z + dz, canopy);
                    }
                }
                placed += set(world, x + 2, oy, z + 2, Material.BARREL);
            }
        }
        return placed;
    }

    // -- 四个门与四个角 -----------------------------------------------------

    /** 城门：两座塔夹一道通行口，上面一条石梁。 */
    private int gate(World world, Lot lot, int ox, int oy, int oz) {
        int placed = 0;
        boolean alongX = lot.width() > lot.depth();
        int half = CityPlan.GATE_HALF;
        int tower = 8;
        int height = lot.height();

        for (int side = -1; side <= 1; side += 2) {
            for (int a = 0; a < tower; a++) {
                for (int b = 0; b < tower; b++) {
                    for (int y = 0; y < height; y++) {
                        int x;
                        int z;
                        if (alongX) {
                            x = ox + lot.centerX() + side * (half + a);
                            z = oz + lot.centerZ() - tower / 2 + b;
                        } else {
                            x = ox + lot.centerX() - tower / 2 + b;
                            z = oz + lot.centerZ() + side * (half + a);
                        }
                        // 中空：只砌外壳
                        boolean shell = a == 0 || a == tower - 1 || b == 0 || b == tower - 1;
                        if (!shell) {
                            clear(world, x, oy, z, oy + height, x, z);
                            continue;
                        }
                        placed += set(world, x, oy + y, z,
                                y >= height - 1 ? Material.STONE_BRICK_WALL : Material.STONE_BRICKS);
                    }
                }
            }
        }
        // 门楣
        for (int d = -half; d <= half; d++) {
            for (int y = 7; y <= 8; y++) {
                int x = alongX ? ox + lot.centerX() + d : ox + lot.centerX();
                int z = alongX ? oz + lot.centerZ() : oz + lot.centerZ() + d;
                placed += set(world, x, oy + y, z, Material.STONE_BRICKS);
            }
        }
        return placed;
    }

    /** 角塔：实心台基 + 一圈雉堞。 */
    private int cornerTower(World world, Lot lot, int ox, int oy, int oz) {
        int minX = ox + Math.min(lot.x1(), lot.x2());
        int maxX = ox + Math.max(lot.x1(), lot.x2());
        int minZ = oz + Math.min(lot.z1(), lot.z2());
        int maxZ = oz + Math.max(lot.z1(), lot.z2());
        int placed = 0;
        for (int y = 0; y < lot.height(); y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    boolean shell = x == minX || x == maxX || z == minZ || z == maxZ;
                    if (!shell) {
                        continue;
                    }
                    boolean window = y == lot.height() / 2 && (x + z) % 4 == 0;
                    placed += set(world, x, oy + y, z, window ? Material.GLASS : Material.STONE_BRICKS);
                }
            }
        }
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                boolean edge = x == minX || x == maxX || z == minZ || z == maxZ;
                if (edge && ((x + z) & 1) == 0) {
                    placed += set(world, x, oy + lot.height(), z, Material.STONE_BRICK_WALL);
                } else {
                    placed += set(world, x, oy + lot.height(), z, Material.STONE_BRICKS);
                }
            }
        }
        return placed;
    }

    // -- 三座主要建筑 -------------------------------------------------------

    /** 钟楼：全城最高。四面金盘，从四个门都能看见。 */
    private int clockTower(World world, Lot lot, int ox, int oy, int oz) {
        int placed = shell(world, lot, ox, oy, oz, Material.STONE_BRICKS, Material.STONE_BRICKS);
        int minX = ox + Math.min(lot.x1(), lot.x2());
        int maxX = ox + Math.max(lot.x1(), lot.x2());
        int minZ = oz + Math.min(lot.z1(), lot.z2());
        int maxZ = oz + Math.max(lot.z1(), lot.z2());
        int cx = ox + lot.centerX();
        int cz = oz + lot.centerZ();

        // 四层楼板
        for (int level = 1; level <= 3; level++) {
            int y = oy + level * 12;
            for (int x = minX + 1; x < maxX; x++) {
                for (int z = minZ + 1; z < maxZ; z++) {
                    placed += set(world, x, y, z, Material.SMOOTH_STONE);
                }
            }
        }
        // 顶层平台与雉堞
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                placed += set(world, x, oy + lot.height(), z, Material.DEEPSLATE_TILES);
                boolean edge = x == minX || x == maxX || z == minZ || z == maxZ;
                if (edge && ((x + z) & 1) == 0) {
                    placed += set(world, x, oy + lot.height() + 1, z, Material.STONE_BRICK_WALL);
                }
            }
        }
        // 四面钟盘
        int dialY = oy + (int) (lot.height() * 0.72);
        for (int d = -3; d <= 3; d++) {
            for (int h = -3; h <= 3; h++) {
                boolean ring = Math.abs(d) == 3 || Math.abs(h) == 3;
                Material face = ring ? Material.GOLD_BLOCK
                        : (d == 0 || h == 0 ? Material.WHITE_CONCRETE : Material.BLACK_CONCRETE);
                placed += set(world, cx + d, dialY + h, minZ, face);
                placed += set(world, cx + d, dialY + h, maxZ, face);
                placed += set(world, minX, dialY + h, cz + d, face);
                placed += set(world, maxX, dialY + h, cz + d, face);
            }
        }
        return placed;
    }

    /** 大教堂：中殿 + 双塔 + 坡顶 + 玫瑰窗。 */
    private int cathedral(World world, Lot lot, int ox, int oy, int oz) {
        int placed = shell(world, lot, ox, oy, oz, Material.STONE_BRICKS, Material.CHISELED_STONE_BRICKS);
        int minX = ox + Math.min(lot.x1(), lot.x2());
        int maxX = ox + Math.max(lot.x1(), lot.x2());
        int minZ = oz + Math.min(lot.z1(), lot.z2());
        int maxZ = oz + Math.max(lot.z1(), lot.z2());
        int cx = ox + lot.centerX();

        // 双塔：正立面朝广场（北侧 z = minZ）
        for (int side = -1; side <= 1; side += 2) {
            int tx = cx + side * 24;
            for (int dx = -6; dx <= 6; dx++) {
                for (int dz = -6; dz <= 6; dz++) {
                    for (int y = 0; y < 38; y++) {
                        boolean shell = Math.abs(dx) == 6 || Math.abs(dz) == 6;
                        if (!shell) {
                            clear(world, tx + dx, oy, minZ + 6 + dz, oy + 38, tx + dx, minZ + 6 + dz);
                            continue;
                        }
                        boolean window = y % 9 == 5 && (dx + dz) % 3 == 0;
                        placed += set(world, tx + dx, oy + y, minZ + 6 + dz,
                                window ? Material.GLASS : Material.STONE_BRICKS);
                    }
                }
            }
        }
        // 中殿坡顶：沿 z 方向起脊
        int ridge = oy + lot.height() + 8;
        for (int z = minZ + 10; z <= maxZ; z++) {
            int span = Math.min(z - minZ - 10, maxZ - z);
            for (int x = minX; x <= maxX; x++) {
                placed += set(world, x, ridge - Math.max(0, 8 - span), z, Material.DEEPSLATE_TILES);
            }
        }
        // 玫瑰窗：北墙一面圆玻璃
        int cy = oy + 18;
        for (int dx = -6; dx <= 6; dx++) {
            for (int dy = -6; dy <= 6; dy++) {
                if (dx * dx + dy * dy <= 36) {
                    placed += set(world, cx + dx, cy + dy, minZ, Material.LIGHT_BLUE_STAINED_GLASS);
                }
            }
        }
        return placed;
    }

    /** 行会厅：方盒子，平顶，四下插旗。 */
    private int guild(World world, Lot lot, int ox, int oy, int oz) {
        int placed = shell(world, lot, ox, oy, oz, Material.MOSSY_STONE_BRICKS, Material.DEEPSLATE_TILES);
        int minX = ox + Math.min(lot.x1(), lot.x2());
        int maxX = ox + Math.max(lot.x1(), lot.x2());
        int minZ = oz + Math.min(lot.z1(), lot.z2());
        int maxZ = oz + Math.max(lot.z1(), lot.z2());
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                placed += set(world, x, oy + lot.height(), z, Material.DEEPSLATE_TILES);
                boolean edge = x == minX || x == maxX || z == minZ || z == maxZ;
                if (edge) {
                    placed += set(world, x, oy + lot.height() + 1, z, Material.STONE_BRICK_WALL);
                }
            }
        }
        // 旗：四角一根杆，杆顶一块羊毛
        for (int x : new int[] {minX + 1, maxX - 1}) {
            for (int z : new int[] {minZ + 1, maxZ - 1}) {
                for (int y = 1; y <= 6; y++) {
                    placed += set(world, x, oy + lot.height() + y, z, Material.DARK_OAK_FENCE);
                }
                placed += set(world, x, oy + lot.height() + 7, z, Material.RED_WOOL);
            }
        }
        return placed;
    }

    /** 民居：两层，坡顶，一扇门两扇窗。 */
    private int house(World world, Lot lot, int ox, int oy, int oz) {
        int placed = shell(world, lot, ox, oy, oz, Material.OAK_PLANKS, Material.SPRUCE_PLANKS);
        int minX = ox + Math.min(lot.x1(), lot.x2());
        int maxX = ox + Math.max(lot.x1(), lot.x2());
        int minZ = oz + Math.min(lot.z1(), lot.z2());
        int maxZ = oz + Math.max(lot.z1(), lot.z2());
        // 楼板
        for (int x = minX + 1; x < maxX; x++) {
            for (int z = minZ + 1; z < maxZ; z++) {
                placed += set(world, x, oy + 5, z, Material.SPRUCE_PLANKS);
            }
        }
        // 坡顶：沿 x 起脊
        int half = (maxZ - minZ) / 2;
        for (int step = 0; step <= half; step++) {
            int y = oy + lot.height() + step;
            for (int x = minX - 1; x <= maxX + 1; x++) {
                placed += set(world, x, y, minZ + step, Material.DEEPSLATE_TILES);
                placed += set(world, x, y, maxZ - step, Material.DEEPSLATE_TILES);
            }
        }
        // 门：朝南（面向街巷）
        for (int dx = -1; dx <= 1; dx++) {
            for (int y = 0; y <= 2; y++) {
                int x = ox + lot.centerX() + dx;
                clear(world, x, oy + y, maxZ, oy + y, x, maxZ);
            }
        }
        placed += set(world, ox + lot.centerX(), oy + 3, maxZ, Material.OAK_PLANKS);
        return placed;
    }

    // -- 零件 ---------------------------------------------------------------

    /** 空壳：清体量，砌外墙，按层开窗，最后铺顶由调用者决定。 */
    private int shell(World world, Lot lot, int ox, int oy, int oz, Material wall, Material top) {
        int minX = ox + Math.min(lot.x1(), lot.x2());
        int maxX = ox + Math.max(lot.x1(), lot.x2());
        int minZ = oz + Math.min(lot.z1(), lot.z2());
        int maxZ = oz + Math.max(lot.z1(), lot.z2());
        int topY = oy + lot.height();
        int placed = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                clear(world, x, oy, z, topY, x, z);
            }
        }
        for (int y = oy; y < topY; y++) {
            boolean floorLine = (y - oy) % 6 == 5;
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    boolean edge = x == minX || x == maxX || z == minZ || z == maxZ;
                    if (!edge) {
                        continue;
                    }
                    boolean window = !floorLine && (y - oy) % 6 == 3 && (x + z) % 4 == 0;
                    placed += set(world, x, y, z, window ? Material.GLASS : (floorLine ? top : wall));
                }
            }
        }
        return placed;
    }

    private int set(World world, int x, int y, int z, Material material) {
        if (material == null || !material.isBlock()) {
            return 0;
        }
        Block block = world.getBlockAt(x, y, z);
        if (block.getType() == material) {
            return 0;
        }
        block.setType(material, false);
        return 1;
    }

    private void clear(World world, int x1, int y1, int z1, int y2, int x2, int z2) {
        for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
            for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (!block.getType().isAir()) {
                        block.setType(Material.AIR, false);
                    }
                }
            }
        }
    }

    /** 主城原点：默认就是世界出生点 (0,0)。 */
    public static int[] originFrom(VesperaPlugin plugin) {
        return new int[] {
                plugin.getConfig().getInt("city.origin-x", 0),
                plugin.getConfig().getInt("city.origin-z", 0)};
    }

    /**
     * 地面高度取城墙外二十格——和校区同一个道理：城里盖满房子之后，
     * 再问"中心最高的方块"只会得到钟楼顶。
     */
    static int groundY(World world, int ox, int oz) {
        return world.getHighestBlockYAt(ox - CityPlan.HALF - 20, oz - CityPlan.HALF - 20) + 1;
    }

    /** 抽查：城真的立起来了吗。 */
    public List<String> verify(World world, int ox, int oz) {
        int oy = groundY(world, ox, oz);
        List<String> failures = new ArrayList<>();
        int half = CityPlan.HALF;
        Lot tower = CityPlan.lot("tower");
        Lot cathedral = CityPlan.lot("cathedral");
        Lot house = CityPlan.lot("house-1");
        Lot guild = CityPlan.lot("guild");

        // 城墙探针避开城门开口，否则量到的是门洞
        probe(world, failures, "南城墙", ox + 40, oy + 2, oz + half, Material.STONE_BRICKS);
        probe(world, failures, "主街铺地", ox, oy - 1, oz + 40, Material.STONE_BRICKS);
        probe(world, failures, "钟楼顶", ox + tower.centerX(), oy + tower.height(),
                oz + tower.centerZ(), Material.DEEPSLATE_TILES);
        // 塔身是空壳，探针要打在壳上（离轴线六格），打在中心只会量到空气
        probe(world, failures, "大教堂塔", ox + cathedral.centerX() - 24 - 6, oy + 20,
                oz + Math.min(cathedral.z1(), cathedral.z2()) + 6, Material.STONE_BRICKS);
        probe(world, failures, "民居墙", ox + Math.min(house.x1(), house.x2()), oy + 2,
                oz + Math.min(house.z1(), house.z2()), Material.OAK_PLANKS);
        probe(world, failures, "行会厅顶", ox + guild.centerX(), oy + guild.height(),
                oz + guild.centerZ(), Material.DEEPSLATE_TILES);
        probe(world, failures, "广场井沿", ox + 3, oy, oz, Material.STONE_BRICK_WALL);
        probe(world, failures, "市集摊位", ox + 60 + 4, oy + 3, oz - 80 + 4, Material.RED_WOOL);
        return failures;
    }

    private void probe(World world, List<String> failures, String what, int x, int y, int z,
            Material... expected) {
        Material actual = world.getBlockAt(x, y, z).getType();
        for (Material option : expected) {
            if (actual == option) {
                return;
            }
        }
        failures.add(what + " 应为 " + java.util.Arrays.toString(expected) + "，实际 " + actual
                + "（" + x + "," + y + "," + z + "）");
    }
}
