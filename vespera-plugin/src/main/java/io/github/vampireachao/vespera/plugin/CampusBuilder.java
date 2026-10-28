package io.github.vampireachao.vespera.plugin;

import io.github.vampireachao.vespera.core.CampusPlan;
import io.github.vampireachao.vespera.core.CampusPlan.Corridor;
import io.github.vampireachao.vespera.core.CampusPlan.Lot;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * 把 {@link CampusPlan} 盖进世界里。
 *
 * <p>这是**灰盒**：体量、动线、开口位置是真的，材质是能看懂的近似。它要回答的问题不是
 * "好看吗"，而是"走进去这片空间对不对"——走廊够不够长、门开在不在人走的路上、
 * 钟楼能不能从校门口看见。
 *
 * <p>为什么用代码盖而不是画一个 `.schem`：一是现在还没有任何建筑文件，二是灰盒要能改。
 * 平面数据在 core 里，改一个数字，房子和图纸一起变；换成 schematic 就得重新导一次，
 * 而"重新导一次"通常意味着它再也不会被改。
 *
 * <p>真正的美术资产仍然是 `.schem`（`layouts/college.yml` 里那十一块）。灰盒是它的骨架，
 * 将来一块一块替换掉，替换的时候位置已经对了。
 */
public final class CampusBuilder {

    /** 每层多高。原版房间习惯：内空三格 + 一层楼板。 */
    private static final int FLOOR_HEIGHT = 7;

    private final VesperaPlugin plugin;
    private final Random random = new Random(20261028L);

    public CampusBuilder(VesperaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 盖整片校区。
     *
     * @param world 世界
     * @param ox    校区原点 x（平面坐标 0）
     * @param oz    校区原点 z
     * @return 放置了多少个方块
     */
    public int build(World world, int ox, int oz) {
        int oy = groundY(world, ox, oz);
        int placed = 0;
        long started = System.currentTimeMillis();

        for (Lot lot : CampusPlan.LOTS) {
            placed += switch (lot.kind()) {
                case GATE -> gate(world, lot, ox, oy, oz);
                case TOWER -> tower(world, lot, ox, oy, oz);
                case GYM -> gym(world, lot, ox, oy, oz);
                case DOME -> dome(world, lot, ox, oy, oz);
                case GREENHOUSE -> greenhouse(world, lot, ox, oy, oz);
                case RUIN -> ruin(world, lot, ox, oy, oz);
                case BOILER -> boiler(world, lot, ox, oy, oz);
                default -> block(world, lot, ox, oy, oz);
            };
        }

        for (Corridor corridor : CampusPlan.CORRIDORS) {
            placed += corridor(world, corridor, ox, oy, oz);
        }
        placed += perimeterWall(world, ox, oy, oz);

        // 出生点放在校门外，抬头就是门与钟楼——第一眼得是对的。
        world.setSpawnLocation(ox, oy + 1, oz + CampusPlan.WALL_MIN_Z - 12);

        long millis = System.currentTimeMillis() - started;
        plugin.getLogger().info("校区灰盒已就位：" + CampusPlan.summary() + "，"
                + placed + " 个方块，" + millis + "ms。");
        return placed;
    }

    // -- 各类建筑 -----------------------------------------------------------

    /** 普通楼：空壳 + 楼板 + 平屋顶 + 窗 + 门。 */
    private int block(World world, Lot lot, int ox, int oy, int oz) {
        int placed = clearAndShell(world, lot, ox, oy, oz);
        placed += floors(world, lot, ox, oy, oz);
        placed += flatRoof(world, lot, ox, oy, oz);
        return placed;
    }

    /** 校门：两根石柱 + 一道铁栅，中间空着让人走。 */
    private int gate(World world, Lot lot, int ox, int oy, int oz) {
        int placed = 0;
        for (int side = -1; side <= 1; side += 2) {
            int x = lot.centerX() + side * 7;
            for (int dy = 0; dy < 8; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        placed += set(world, ox + x + dx, oy + dy, oz + lot.centerZ() + dz,
                                dy >= 6 ? Material.CHISELED_STONE_BRICKS : Material.STONE_BRICKS);
                    }
                }
            }
        }
        // 横梁与铁栅
        for (int x = -7; x <= 7; x++) {
            placed += set(world, ox + lot.centerX() + x, oy + 7, oz + lot.centerZ(), Material.STONE_BRICKS);
        }
        for (int x = -6; x <= 6; x++) {
            if (Math.abs(x) <= 2) {
                continue; // 中间留门
            }
            placed += set(world, ox + lot.centerX() + x, oy + 4, oz + lot.centerZ(), Material.IRON_BARS);
            placed += set(world, ox + lot.centerX() + x, oy + 5, oz + lot.centerZ(), Material.IRON_BARS);
        }
        return placed;
    }

    /** 钟楼：方塔，四面一块金盘当场钟面。 */
    private int tower(World world, Lot lot, int ox, int oy, int oz) {
        int placed = clearAndShell(world, lot, ox, oy, oz);
        placed += floors(world, lot, ox, oy, oz);
        placed += flatRoof(world, lot, ox, oy, oz);
        // 钟面：四面各一块金盘，高度在 3/4 处
        int dialY = oy + (int) (lot.height() * 0.72);
        int minX = ox + Math.min(lot.x1(), lot.x2());
        int maxX = ox + Math.max(lot.x1(), lot.x2());
        int minZ = oz + Math.min(lot.z1(), lot.z2());
        int maxZ = oz + Math.max(lot.z1(), lot.z2());
        int cx = ox + lot.centerX();
        int cz = oz + lot.centerZ();
        for (int d = -2; d <= 2; d++) {
            for (int h = -2; h <= 2; h++) {
                boolean ring = Math.abs(d) == 2 || Math.abs(h) == 2;
                // 中心用黑白混凝土画指针。原版的"钟"是物品，不是方块——
                // 想放一个钟面，只能用方块搭出来。
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

    /** 体育馆：通高一个大空间，一条玻璃带，双坡屋顶。 */
    private int gym(World world, Lot lot, int ox, int oy, int oz) {
        int placed = clearAndShell(world, lot, ox, oy, oz);
        int minX = ox + Math.min(lot.x1(), lot.x2());
        int maxX = ox + Math.max(lot.x1(), lot.x2());
        int minZ = oz + Math.min(lot.z1(), lot.z2());
        int maxZ = oz + Math.max(lot.z1(), lot.z2());

        // 高窗：一圈玻璃，替天窗的采光
        for (int y = oy + 8; y <= oy + 11; y++) {
            for (int x = minX + 1; x < maxX; x++) {
                placed += set(world, x, y, minZ, Material.GLASS);
                placed += set(world, x, y, maxZ, Material.GLASS);
            }
            for (int z = minZ + 1; z < maxZ; z++) {
                placed += set(world, minX, y, z, Material.GLASS);
                placed += set(world, maxX, y, z, Material.GLASS);
            }
        }
        // 双坡
        int half = (maxZ - minZ) / 2;
        for (int step = 0; step <= half; step++) {
            int y = oy + lot.height() - half + step;
            for (int x = minX - 1; x <= maxX + 1; x++) {
                placed += set(world, x, y, minZ + step, Material.DEEPSLATE_TILES);
                placed += set(world, x, y, maxZ - step, Material.DEEPSLATE_TILES);
            }
        }
        // 场地：木地板
        for (int x = minX + 1; x < maxX; x++) {
            for (int z = minZ + 1; z < maxZ; z++) {
                placed += set(world, x, oy - 1, z, Material.SPRUCE_PLANKS);
            }
        }
        return placed;
    }

    /** 天文台：圆顶。用半球近似，不追求精确。 */
    private int dome(World world, Lot lot, int ox, int oy, int oz) {
        int placed = clearAndShell(world, lot, ox, oy, oz);
        placed += floors(world, lot, ox, oy + (int) (lot.height() * 0.6), oz);
        int cx = ox + lot.centerX();
        int cz = oz + lot.centerZ();
        int radius = Math.min(lot.width(), lot.depth()) / 2;
        int baseY = oy + (int) (lot.height() * 0.6) + 1;
        for (int dy = 0; dy <= radius; dy++) {
            int r = (int) Math.round(Math.sqrt(Math.max(0, radius * radius - dy * dy)));
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dz * dz > r * r) {
                        continue;
                    }
                    // 只铺壳，不填实心
                    if (dx * dx + dz * dz < (r - 1) * (r - 1)) {
                        continue;
                    }
                    placed += set(world, cx + dx, baseY + dy, cz + dz,
                            dy == radius ? Material.COPPER_BLOCK : Material.GLASS);
                }
            }
        }
        return placed;
    }

    /** 温室：玻璃墙 + 玻璃顶 + 木框。 */
    private int greenhouse(World world, Lot lot, int ox, int oy, int oz) {
        int placed = clearAndShell(world, lot, ox, oy, oz);
        int minX = ox + Math.min(lot.x1(), lot.x2());
        int maxX = ox + Math.max(lot.x1(), lot.x2());
        int minZ = oz + Math.min(lot.z1(), lot.z2());
        int maxZ = oz + Math.max(lot.z1(), lot.z2());
        int top = oy + lot.height();

        // 把石墙换成玻璃，只留四角与每四格的立柱
        for (int y = oy; y < top; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    boolean edge = x == minX || x == maxX || z == minZ || z == maxZ;
                    if (!edge) {
                        continue;
                    }
                    boolean post = (x - minX) % 4 == 0 && (z - minZ) % 4 == 0;
                    placed += set(world, x, y, z, post ? Material.STRIPPED_OAK_LOG : Material.GLASS);
                }
            }
        }
        // 玻璃顶
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                placed += set(world, x, top, z, Material.GLASS);
            }
        }
        // 花坛
        for (int x = minX + 3; x < maxX - 2; x += 5) {
            for (int z = minZ + 3; z < maxZ - 2; z += 5) {
                placed += set(world, x, oy - 1, z, Material.DIRT);
                placed += set(world, x, oy, z, Material.OAK_LEAVES);
            }
        }
        return placed;
    }

    /** 旧校舍：一半塌了。墙留着，屋顶没有。 */
    private int ruin(World world, Lot lot, int ox, int oy, int oz) {
        int minX = ox + Math.min(lot.x1(), lot.x2());
        int maxX = ox + Math.max(lot.x1(), lot.x2());
        int minZ = oz + Math.min(lot.z1(), lot.z2());
        int maxZ = oz + Math.max(lot.z1(), lot.z2());
        int placed = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                boolean edge = x == minX || x == maxX || z == minZ || z == maxZ;
                clear(world, x, oy, z, oy + lot.height(), x, z);
                if (!edge) {
                    continue;
                }
                int height = lot.height() - (random.nextInt(6));
                for (int y = oy; y < oy + Math.max(2, height); y++) {
                    placed += set(world, x, y, z,
                            random.nextInt(4) == 0 ? Material.MOSSY_STONE_BRICKS : Material.STONE_BRICKS);
                }
            }
        }
        // 塌下来的梁
        for (int i = 0; i < 6; i++) {
            int x = minX + random.nextInt(Math.max(1, maxX - minX));
            int z = minZ + random.nextInt(Math.max(1, maxZ - minZ));
            placed += set(world, x, oy + random.nextInt(3), z, Material.DARK_OAK_PLANKS);
        }
        return placed;
    }

    /** 锅炉房：向下挖的两层，中间一扇下界门。 */
    private int boiler(World world, Lot lot, int ox, int oy, int oz) {
        int minX = ox + Math.min(lot.x1(), lot.x2());
        int maxX = ox + Math.max(lot.x1(), lot.x2());
        int minZ = oz + Math.min(lot.z1(), lot.z2());
        int maxZ = oz + Math.max(lot.z1(), lot.z2());
        int placed = 0;
        // 别挖穿世界底。挖到基岩以下写不进去，而且会白白算一遍。
        int bottom = Math.max(world.getMinHeight() + 2, oy - lot.height());
        for (int y = oy - 2; y >= bottom; y--) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    boolean edge = x == minX || x == maxX || z == minZ || z == maxZ;
                    if (edge) {
                        placed += set(world, x, y, z, Material.DEEPSLATE_BRICKS);
                    } else {
                        clear(world, x, y, z, y, x, z);
                    }
                }
            }
        }
        // 地板与天花板
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                placed += set(world, x, bottom, z, Material.DEEPSLATE_BRICKS);
            }
        }
        // 下界门框（黑曜石），门洞留给火
        int px = minX + 6;
        int pz = oz + lot.centerZ();
        for (int dy = 0; dy < 5; dy++) {
            for (int dx = 0; dx < 4; dx++) {
                boolean frame = dy == 0 || dy == 4 || dx == 0 || dx == 3;
                if (frame) {
                    placed += set(world, px + dx, bottom + dy, pz, Material.OBSIDIAN);
                }
            }
        }
        // 楼梯：从地面通到地下二层
        for (int i = 0; i < lot.height(); i++) {
            int x = maxX - 1;
            int z = maxZ - 1 - i;
            if (z < minZ + 1) {
                break;
            }
            placed += set(world, x, oy - i, z, Material.DEEPSLATE_TILE_STAIRS);
            clear(world, x, oy - i + 1, z, oy - i + 2, x, z);
        }
        return placed;
    }

    // -- 零件 ---------------------------------------------------------------

    /** 清空体量，砌四面墙，按层开窗。 */
    private int clearAndShell(World world, Lot lot, int ox, int oy, int oz) {
        int minX = ox + Math.min(lot.x1(), lot.x2());
        int maxX = ox + Math.max(lot.x1(), lot.x2());
        int minZ = oz + Math.min(lot.z1(), lot.z2());
        int maxZ = oz + Math.max(lot.z1(), lot.z2());
        int top = oy + lot.height();

        // 清空内部与地基
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                clear(world, x, oy - 1, z, top, x, z);
            }
        }

        int placed = 0;
        for (int y = oy; y < top; y++) {
            boolean floorLevel = (y - oy) % FLOOR_HEIGHT == 0;
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    boolean edge = x == minX || x == maxX || z == minZ || z == maxZ;
                    if (!edge) {
                        continue;
                    }
                    boolean window = !floorLevel && (y - oy) % FLOOR_HEIGHT == 4
                            && (x + z) % 3 == 0 && !isCorner(x, z, minX, maxX, minZ, maxZ);
                    placed += set(world, x, y, z, window ? Material.GLASS : wallMaterial(x, y, z));
                }
            }
            // 勒脚
            if (y == oy) {
                for (int x = minX; x <= maxX; x++) {
                    placed += set(world, x, y, minZ, Material.STONE_BRICK_WALL);
                    placed += set(world, x, y, maxZ, Material.STONE_BRICK_WALL);
                }
            }
        }
        return placed;
    }

    /** 楼板：按 {@code floors} 逐层铺，每层留一个楼梯井。 */
    private int floors(World world, Lot lot, int ox, int oy, int oz) {
        int placed = slab(world, lot, ox, oy - 1, oz, Material.SPRUCE_PLANKS);
        for (int level = 1; level < lot.floors(); level++) {
            placed += slab(world, lot, ox, oy + level * FLOOR_HEIGHT, oz, Material.SMOOTH_STONE);
        }
        return placed;
    }

    private int slab(World world, Lot lot, int ox, int y, int oz, Material material) {
        int minX = ox + Math.min(lot.x1(), lot.x2());
        int maxX = ox + Math.max(lot.x1(), lot.x2());
        int minZ = oz + Math.min(lot.z1(), lot.z2());
        int maxZ = oz + Math.max(lot.z1(), lot.z2());
        int placed = 0;
        for (int x = minX + 1; x < maxX; x++) {
            for (int z = minZ + 1; z < maxZ; z++) {
                // 楼梯井：贴着一个角落，三乘三
                boolean well = x >= minX + 2 && x <= minX + 4 && z >= minZ + 2 && z <= minZ + 4;
                if (well) {
                    continue;
                }
                placed += set(world, x, y, z, material);
            }
        }
        return placed;
    }

    private int flatRoof(World world, Lot lot, int ox, int oy, int oz) {
        int minX = ox + Math.min(lot.x1(), lot.x2());
        int maxX = ox + Math.max(lot.x1(), lot.x2());
        int minZ = oz + Math.min(lot.z1(), lot.z2());
        int maxZ = oz + Math.max(lot.z1(), lot.z2());
        int placed = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                placed += set(world, x, oy + lot.height(), z, Material.DEEPSLATE_TILES);
            }
        }
        // 女儿墙
        for (int x = minX; x <= maxX; x++) {
            placed += set(world, x, oy + lot.height() + 1, minZ, Material.STONE_BRICK_WALL);
            placed += set(world, x, oy + lot.height() + 1, maxZ, Material.STONE_BRICK_WALL);
        }
        for (int z = minZ; z <= maxZ; z++) {
            placed += set(world, minX, oy + lot.height() + 1, z, Material.STONE_BRICK_WALL);
            placed += set(world, maxX, oy + lot.height() + 1, z, Material.STONE_BRICK_WALL);
        }
        return placed;
    }

    /** 一段有顶回廊：地砖、两侧柱子、平顶。走进去不露天。 */
    private int corridor(World world, Corridor corridor, int ox, int oy, int oz) {
        int minX = ox + Math.min(corridor.x1(), corridor.x2());
        int maxX = ox + Math.max(corridor.x1(), corridor.x2());
        int minZ = oz + Math.min(corridor.z1(), corridor.z2());
        int maxZ = oz + Math.max(corridor.z1(), corridor.z2());
        int placed = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                placed += set(world, x, oy - 1, z, Material.SMOOTH_STONE);
                clear(world, x, oy, z, oy + 4, x, z);
                placed += set(world, x, oy + 5, z, Material.SPRUCE_PLANKS);
            }
        }
        // 柱子：沿长边每四格一根
        boolean alongX = (maxX - minX) >= (maxZ - minZ);
        if (alongX) {
            for (int x = minX; x <= maxX; x += 4) {
                placed += set(world, x, oy, minZ, Material.STONE_BRICKS);
                placed += set(world, x, oy, maxZ, Material.STONE_BRICKS);
            }
        } else {
            for (int z = minZ; z <= maxZ; z += 4) {
                placed += set(world, minX, oy, z, Material.STONE_BRICKS);
                placed += set(world, maxX, oy, z, Material.STONE_BRICKS);
            }
        }
        return placed;
    }

    /** 围墙：腰高，校门处断开。 */
    private int perimeterWall(World world, int ox, int oy, int oz) {
        int minX = ox + CampusPlan.WALL_MIN_X;
        int maxX = ox + CampusPlan.WALL_MAX_X;
        int minZ = oz + CampusPlan.WALL_MIN_Z;
        int maxZ = oz + CampusPlan.WALL_MAX_Z;
        int placed = 0;
        for (int x = minX; x <= maxX; x++) {
            boolean gateGap = Math.abs(x - ox) <= CampusPlan.GATE_HALF;
            for (int z : new int[] {minZ, maxZ}) {
                if (gateGap && z == minZ) {
                    continue;
                }
                placed += set(world, x, oy - 1, z, Material.STONE_BRICKS);
                placed += set(world, x, oy, z, Material.STONE_BRICKS);
            }
        }
        for (int z = minZ; z <= maxZ; z++) {
            for (int x : new int[] {minX, maxX}) {
                placed += set(world, x, oy - 1, z, Material.STONE_BRICKS);
                placed += set(world, x, oy, z, Material.STONE_BRICKS);
            }
        }
        return placed;
    }

    // -- 底层操作 -----------------------------------------------------------

    /**
     * 校区的地面高度。
     *
     * <p>**不能拿校区中心去问"最高的方块在哪"**：第一次盖之前那儿是草地，盖完之后那儿是
     * 钟楼屋顶。同一句问话，在两次运行里得到相差四十格的答案，于是重建会把整片校区
     * 一层层往天上叠。
     *
     * <p>所以取一个永远不会被盖的角：围墙外二十格。那里只有地形，没有房子。
     */
    static int groundY(World world, int ox, int oz) {
        int sampleX = ox + CampusPlan.WALL_MIN_X - 20;
        int sampleZ = oz + CampusPlan.WALL_MIN_Z - 20;
        return world.getHighestBlockYAt(sampleX, sampleZ) + 1;
    }

    private int set(World world, int x, int y, int z, Material material) {
        // 防御：物品不是方块。"放一个钟"这种念头最容易踩——钟是物品，钟面得用方块搭。
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

    /** 老砖墙：八成是好砖，两成苔藓。 */
    private Material wallMaterial(int x, int y, int z) {
        int hash = Math.abs((x * 31 + y * 17 + z * 13) % 10);
        return hash < 2 ? Material.MOSSY_STONE_BRICKS : Material.STONE_BRICKS;
    }

    private boolean isCorner(int x, int z, int minX, int maxX, int minZ, int maxZ) {
        return (x == minX || x == maxX) && (z == minZ || z == maxZ);
    }

    /** 给 {@code /vespera build} 用：坐标从配置读，缺省落在学院区域中心。 */
    public static int[] originFrom(VesperaPlugin plugin) {
        int x = plugin.getConfig().getInt("campus.origin-x", 370);
        int z = plugin.getConfig().getInt("campus.origin-z", 0);
        return new int[] {x, z};
    }

    /**
     * 抽查几个位置，确认房子真的在那儿。
     *
     * <p>没有客户端也能验的办法就这一种：站在已知坐标上问一句"这块是不是石砖"。
     * 它验不出好看，但验得出"根本没盖"和"盖到别处去了"——这两件事光看日志看不出来。
     */
    public List<String> verify(World world, int ox, int oz) {
        int oy = groundY(world, ox, oz);
        List<String> failures = new ArrayList<>();

        Lot gate = CampusPlan.lot("gate");
        Lot hall = CampusPlan.lot("hall");
        Lot tower = CampusPlan.lot("tower");
        Lot greenhouse = CampusPlan.lot("greenhouse");
        Corridor corridor = CampusPlan.CORRIDORS.get(0);

        probe(world, failures, "围墙", ox + CampusPlan.WALL_MIN_X, oy, oz, Material.STONE_BRICKS);
        probe(world, failures, "校门柱", ox + gate.centerX() + 7, oy + 2, oz + gate.centerZ(),
                Material.STONE_BRICKS);
        // 老砖墙有两成是苔藓砖，探针得认这一种"合法的偏差"
        probe(world, failures, "教学楼外墙", ox + Math.min(hall.x1(), hall.x2()), oy + 2,
                oz + Math.min(hall.z1(), hall.z2()), Material.STONE_BRICKS, Material.MOSSY_STONE_BRICKS);
        probe(world, failures, "教学楼楼板", ox + hall.centerX(), oy + FLOOR_HEIGHT,
                oz + hall.centerZ(), Material.SMOOTH_STONE);
        probe(world, failures, "钟楼顶", ox + tower.centerX(), oy + tower.height(),
                oz + tower.centerZ(), Material.DEEPSLATE_TILES);
        probe(world, failures, "回廊顶", ox + (corridor.x1() + corridor.x2()) / 2, oy + 5,
                oz + corridor.z1(), Material.SPRUCE_PLANKS);
        probe(world, failures, "温室玻璃", ox + Math.min(greenhouse.x1(), greenhouse.x2()) + 1, oy + 3,
                oz + Math.min(greenhouse.z1(), greenhouse.z2()), Material.GLASS);
        // 地下：锅炉房应该真的在地下。取 -4 而不是更低——再往下就是基岩层了，
        // 那里挖不出房间，也不该挖。
        Lot boiler = CampusPlan.lot("boiler");
        probe(world, failures, "锅炉房墙", ox + Math.min(boiler.x1(), boiler.x2()), oy - 4,
                oz + Math.min(boiler.z1(), boiler.z2()) + 2, Material.DEEPSLATE_BRICKS);
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

    /** 调试用：把平面图的每一栋楼的名字与体量列出来。 */
    public static Map<String, String> outline() {
        return CampusPlan.LOTS.stream().collect(java.util.stream.Collectors.toMap(
                Lot::id,
                lot -> String.format(Locale.ROOT, "%s %d×%d h%d", lot.name(), lot.width(), lot.depth(),
                        lot.height()),
                (left, right) -> left,
                java.util.LinkedHashMap::new));
    }
}
