package io.github.vampireachao.vespera.plugin;

import io.github.vampireachao.vespera.core.voxel.Block;
import io.github.vampireachao.vespera.core.voxel.Scene;
import io.github.vampireachao.vespera.core.world.MedievalCity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.persistence.PersistentDataType;

/**
 * 把 {@link Scene} 写进世界：选址、整平、铺城。
 *
 * <p>这一步是"好看"与"能玩"之间的桥。设计数据是干净的：地面在 y=0，城是 600×600。
 * 真实地形不是——它可能在海上，可能在山坡上。所以顺序必须是：
 *
 * <ol>
 *   <li><b>选址</b>：优先原地，若是水面就沿网格找一块够平的陆地，记在世界里，下次不再找。</li>
 *   <li><b>定基</b>：取场地一圈的地表高度中位数。城基一旦定了，全城都跟着它。</li>
 *   <li><b>整平</b>：把 640×640 的场地削平；城外几个点（竞技场、农田、村落）各整一小块。</li>
 *   <li><b>铺城</b>：方块逐块写入，方块状态原样保留——楼梯还是楼梯。</li>
 * </ol>
 *
 * <p>为什么要整平而不是"顺着地形盖"：中世纪的城本来就建在平台上。顺着坡盖的房子，
 * 门会开在半空，墙脚会露出来。平台加挡土墙，既是真事，也最省事。
 */
public final class CityApplier {

    /** 塑造半径：城墙外一百四十格之内都归我们塑形，再往外交给原版地形。 */
    private static final int SCULPT_RADIUS = MedievalCity.HALF + 140;

    /** 地形采样步长（粗网格 + 双线性插值，比逐列问一遍快两个数量级）。 */
    private static final int SCULPT_STEP = 8;

    /** 台地边缘到坡脚：这一段是从"城的平面"过渡到"野地的坡"。 */
    private static final int FOOT = MedievalCity.HALF + 28;

    private final VesperaPlugin plugin;
    private final Map<Block, BlockData> dataCache = new HashMap<>();

    public CityApplier(VesperaPlugin plugin) {
        this.plugin = plugin;
    }

    // -- 入口 ---------------------------------------------------------------

    public int build(World world, int ox, int oz) {
        long started = System.currentTimeMillis();
        int ground = groundOf(world, ox, oz);
        plugin.getLogger().info("城市基面 y=" + ground + "，开始塑形（半径 " + SCULPT_RADIUS + "）……");
        sculpt(world, ox, oz, ground);
        levelOuterPads(world, ox, oz, ground);
        carveWater(world, ox, oz, ground);

        long leveled = System.currentTimeMillis();
        plugin.getLogger().info("  整平完成，用时 " + (leveled - started) + "ms，开始铺城……");

        Scene scene = MedievalCity.build();
        int placed = apply(world, scene, ox, oz, ground);

        populate(world, ox, oz, ground);

        // 出生点在圣座顶坛。但"顶坛"是个坐标，不是保证——所以要扫一遍确认脚下有地、身上有空
        org.bukkit.Location spawn = safeSpawn(world, ox, oz, ground);
        world.setSpawnLocation(spawn);
        plugin.getLogger().info("  出生点 " + spawn.getBlockX() + "," + spawn.getBlockY()
                + "," + spawn.getBlockZ() + "（圣座顶坛，祭坛前一步）");
        long done = System.currentTimeMillis();
        plugin.getLogger().info("流明陨城已就位：" + scene.size() + " 个方块（写入 " + placed + "），"
                + "塑形 " + (leveled - started) + "ms + 建造 " + (done - leveled) + "ms。");
        return placed;
    }

    /** 只重建，不整平——改平面之后用这个，省掉几十秒。 */
    public int rebuild(World world, int ox, int oz) {
        int ground = groundOf(world, ox, oz);
        Scene scene = MedievalCity.build();
        return apply(world, scene, ox, oz, ground);
    }

    // -- 选址与整平 ---------------------------------------------------------

    /**
     * 场地基面。取场地外圈一圈采样（避开城中心将来的建筑），取中位数。
     * 中位数比平均数稳：一个山头不会把整座城抬高十格。
     */
    public int groundOf(World world, int ox, int oz) {
        List<Integer> samples = new ArrayList<>();
        int radius = MedievalCity.HALF + 20;
        for (int i = 0; i < 24; i++) {
            double angle = i / 24.0 * Math.PI * 2;
            int x = ox + (int) Math.round(Math.cos(angle) * radius);
            int z = oz + (int) Math.round(Math.sin(angle) * radius);
            if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                world.getChunkAt(x >> 4, z >> 4);
            }
            samples.add(world.getHighestBlockYAt(x, z, HeightMap.WORLD_SURFACE));
        }
        samples.sort(Integer::compareTo);
        int median = samples.get(samples.size() / 2);
        return Math.max(world.getMinHeight() + 8, Math.min(180, median));
    }

    /** 找一块够干的平地。原地就是陆地就不动；是海就沿网格找，并把结果记进世界。 */
    public int[] site(World world, int preferredX, int preferredZ) {
        NamespacedKey key = new NamespacedKey(plugin, "city-site");
        String saved = world.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (saved != null && saved.contains(",")) {
            String[] parts = saved.split(",");
            return new int[] {Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim())};
        }
        if (isDryEnough(world, preferredX, preferredZ)) {
            remember(world, key, preferredX, preferredZ);
            return new int[] {preferredX, preferredZ};
        }
        int best = -1;
        int bestScore = Integer.MIN_VALUE;
        int[] bestPoint = {preferredX, preferredZ};
        for (int ring = 1; ring <= 4; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                        continue;
                    }
                    int x = preferredX + dx * 512;
                    int z = preferredZ + dz * 512;
                    int score = dryness(world, x, z);
                    if (score > bestScore) {
                        bestScore = score;
                        bestPoint = new int[] {x, z};
                        best = score;
                    }
                }
            }
            if (bestScore >= 14) {
                break;
            }
        }
        plugin.getLogger().info("原地不适合建城（水或坡），改用 " + bestPoint[0] + "," + bestPoint[1]
                + "（干燥度 " + best + "/16）。");
        remember(world, key, bestPoint[0], bestPoint[1]);
        return bestPoint;
    }

    private void remember(World world, NamespacedKey key, int x, int z) {
        world.getPersistentDataContainer().set(key, PersistentDataType.STRING, x + "," + z);
    }

    private boolean isDryEnough(World world, int ox, int oz) {
        return dryness(world, ox, oz) >= 13;
    }

    /** 一格网格 5×5 采样里有多少格在地面以上（不是水）。 */
    private int dryness(World world, int ox, int oz) {
        int dry = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int x = ox + dx * 110;
                int z = oz + dz * 110;
                if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                    world.getChunkAt(x >> 4, z >> 4);
                }
                int y = world.getHighestBlockYAt(x, z, HeightMap.WORLD_SURFACE);
                Material type = world.getBlockAt(x, y, z).getType();
                if (y >= world.getSeaLevel() && type != Material.WATER && type != Material.KELP
                        && type != Material.SEAGRASS) {
                    dry++;
                }
            }
        }
        return dry;
    }

    /**
     * 塑形：城内是平台，城外是一段真的坡。
     *
     * <p>原来的做法是"半径内削平、半径外不管"，于是城墙外三十格有一条笔直的断崖，
     * 再往外一步就跳回原始地形——从空中看像一块被刀切下来的蛋糕。现在改成三段：
     *
     * <ol>
     *   <li><b>台地</b>（墙外两格以内）：与城内同高，是城墙的基脚；</li>
     *   <li><b>陡坎</b>（墙外四格）：落两格。这是挡土墙的高度，也是攻城时要爬的那一段；</li>
     *   <li><b>坡脚到野地</b>（墙外四格到一百四十格）：按自然高度做平滑过渡，
     *       中段叠一点低频起伏，让它像山坡而不像抹平的水泥。</li>
     * </ol>
     *
     * <p>关键是"自然高度"要提前采：一边削一边读，读到的就是自己刚削出来的高度，
     * 过渡带会越算越平。所以先按八格网格把原始地表采样存下来，再插值。
     */
    private void sculpt(World world, int ox, int oz, int ground) {
        int pad = SCULPT_STEP * 2;
        int[][] natural = sampleTerrain(world, ox, oz, SCULPT_RADIUS + pad);
        int cleared = 0;
        int filled = 0;
        for (int x = ox - SCULPT_RADIUS; x <= ox + SCULPT_RADIUS; x++) {
            for (int z = oz - SCULPT_RADIUS; z <= oz + SCULPT_RADIUS; z++) {
                int r = Math.max(Math.abs(x - ox), Math.abs(z - oz));
                int naturalY = naturalAt(natural, ox, oz, SCULPT_RADIUS + pad, x, z);
                if (r > MedievalCity.HALF + 6 && r > FOOT) {
                    // 过渡带外侧：目标高度与原地形一致时不用动土（能省掉几十万次空写）
                    double t0 = smooth((r - FOOT) / (double) (SCULPT_RADIUS - FOOT));
                    if (Math.abs(t0 - 1.0) < 0.02) {
                        continue;
                    }
                }

                // 台地与陡坎：与场景里那圈裙边逐格对齐（y = -1 - drop）
                int drop = r <= MedievalCity.HALF + 2 ? 0 : (r <= MedievalCity.HALF + 4 ? 1 : 2);
                int edge = ground - drop;
                int target;
                if (r <= FOOT) {
                    target = edge;
                } else {
                    double t = smooth((r - FOOT) / (double) (SCULPT_RADIUS - FOOT));
                    // 起伏在两端归零：靠城的一侧不能有坑，靠野地的一侧不能有坎
                    double wobble = fbm(x, z) * 4.0 * (4 * t * (1 - t));
                    target = (int) Math.round(edge * (1 - t) + naturalY * t + wobble);
                }

                Material inside = r <= MedievalCity.HALF + 6 ? Material.COARSE_DIRT : null;
                int changed = carveTo(world, x, z, target, inside, naturalY);
                if (changed > 0) {
                    if (target < naturalY) {
                        cleared += changed;
                    } else {
                        filled += changed;
                    }
                }
            }
        }
        plugin.getLogger().info("  削掉 " + cleared + " 格，填上 " + filled + " 格；"
                + "城墙外一百四十格内已按自然高度接上。");
        reportProfile(world, ox, oz, ground, natural, SCULPT_RADIUS + pad);
    }

    /**
     * 打一条剖面：城东一排采样点的"原高度 → 现高度"。
     *
     * <p>塑形最怕的不是不好看，是接缝。接缝在图上、在游戏里都不容易一眼看出来，
     * 但可以在日志里排成一行数字：原高度与现高度越到外侧越接近，说明接上了；
     * 若某处突然拉开十几格，那就是一条断崖。
     */
    private void reportProfile(World world, int ox, int oz, int ground, int[][] natural, int radius) {
        StringBuilder line = new StringBuilder();
        for (int d : new int[] {300, 306, 316, 340, 380, 420, 440, 460}) {
            int x = ox + d;
            int now = world.getHighestBlockYAt(x, oz, HeightMap.WORLD_SURFACE);
            int before = naturalAt(natural, ox, oz, radius, x, oz);
            line.append("  r=").append(d).append(' ').append(before).append("→").append(now);
        }
        plugin.getLogger().info("  东向剖面（原→现）：" + line);
    }

    /** 粗网格采样原始地表。{@code step} 越小越准，也越慢；八格对坡地足够。 */
    private int[][] sampleTerrain(World world, int ox, int oz, int radius) {
        int n = (radius * 2) / SCULPT_STEP + 1;
        int[][] heights = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                int x = ox - radius + i * SCULPT_STEP;
                int z = oz - radius + j * SCULPT_STEP;
                if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                    world.getChunkAt(x >> 4, z >> 4);
                }
                heights[i][j] = world.getHighestBlockYAt(x, z, HeightMap.WORLD_SURFACE);
            }
        }
        return heights;
    }

    private int naturalAt(int[][] heights, int ox, int oz, int radius, int x, int z) {
        double fi = (x - (ox - radius)) / (double) SCULPT_STEP;
        double fj = (z - (oz - radius)) / (double) SCULPT_STEP;
        int i = Math.max(0, Math.min(heights.length - 2, (int) fi));
        int j = Math.max(0, Math.min(heights.length - 2, (int) fj));
        double ti = Math.min(1, Math.max(0, fi - i));
        double tj = Math.min(1, Math.max(0, fj - j));
        double a = heights[i][j] * (1 - ti) + heights[i + 1][j] * ti;
        double b = heights[i][j + 1] * (1 - ti) + heights[i + 1][j + 1] * ti;
        return (int) Math.round(a * (1 - tj) + b * tj);
    }

    /**
     * 把一列削到（或填到）目标高度。
     *
     * @param inside 城内用的表层方块；null 表示按"削了多深"自己选（浅了是土，深了是石）
     * @return 改动的方块数
     */
    private int carveTo(World world, int x, int z, int target, Material inside, int naturalY) {
        int top = world.getHighestBlockYAt(x, z, HeightMap.WORLD_SURFACE);
        int changed = 0;
        if (top > target) {
            for (int y = target + 1; y <= top; y++) {
                if (!world.getBlockAt(x, y, z).isEmpty()) {
                    world.getBlockAt(x, y, z).setType(Material.AIR, false);
                    changed++;
                }
            }
        } else if (top < target) {
            for (int y = top + 1; y <= target; y++) {
                world.getBlockAt(x, y, z).setType(y == target ? Material.DIRT : Material.STONE, false);
                changed++;
            }
        }
        int cut = naturalY - target;
        Material surface = inside != null ? inside
                : (cut > 5 ? Material.STONE : (cut > 1 ? Material.DIRT : Material.GRASS_BLOCK));
        world.getBlockAt(x, target, z).setType(surface, false);
        return changed;
    }

    /** 平滑插值：两端斜率为零，接缝才不会有一道折线。 */
    private static double smooth(double t) {
        double c = Math.max(0, Math.min(1, t));
        return c * c * (3 - 2 * c);
    }

    /** 低频起伏。不用 Perlin，两个错相的正弦已经够让坡面不像塑料。 */
    private static double fbm(int x, int z) {
        return Math.sin(x * 0.031 + Math.sin(z * 0.019) * 1.9) * 0.55
                + Math.sin(z * 0.047 + Math.sin(x * 0.011) * 2.3) * 0.45;
    }

    /** 城外那几个点各整一小块，让竞技场、农田、村落有地方站。 */
    private void levelOuterPads(World world, int ox, int oz, int ground) {
        for (int[] pad : outerPads()) {
            int x1 = ox + pad[0];
            int z1 = oz + pad[1];
            int x2 = ox + pad[2];
            int z2 = oz + pad[3];
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                        world.getChunkAt(x >> 4, z >> 4);
                    }
                    int top = world.getHighestBlockYAt(x, z, HeightMap.WORLD_SURFACE);
                    if (top > ground) {
                        for (int y = ground + 1; y <= top; y++) {
                            world.getBlockAt(x, y, z).setType(Material.AIR, false);
                        }
                    } else if (top < ground) {
                        for (int y = top + 1; y <= ground; y++) {
                            world.getBlockAt(x, y, z).setType(Material.DIRT, false);
                        }
                    }
                    world.getBlockAt(x, ground, z).setType(Material.GRASS_BLOCK, false);
                }
            }
        }
    }

    /** 城外要整平的小块：竞技场、两块田、村落、码头堆场。 */
    private List<int[]> outerPads() {
        List<int[]> pads = new ArrayList<>();
        // 坐标跟着 core 里那几条预留走：城扩到 ±460 之后，农田和北村都挪到了墙外
        pads.add(new int[] {-590, 30, -462, 270});  // 农田与风车
        pads.add(new int[] {294, -606, 386, -518}); // 北村
        pads.add(new int[] {-262, 240, -84, 420});  // 码头
        return pads;
    }

    /** 运河：先把地形挖开，再灌水。否则水会被埋在地里。 */
    private void carveWater(World world, int ox, int oz, int ground) {
        int x1 = ox - 252;
        int x2 = ox - 92;
        int z1 = oz + 304;
        int z2 = oz + 406;
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                for (int y = ground - 6; y <= ground; y++) {
                    world.getBlockAt(x, y, z).setType(
                            y <= ground - 2 ? Material.WATER : Material.AIR, false);
                }
            }
        }
    }

    // -- 铺城 ---------------------------------------------------------------

    private int apply(World world, Scene scene, int ox, int oz, int ground) {
        int placed = 0;
        for (Scene.Entry entry : scene.entries()) {
            // 这一层地面落地器已经整平过，不必再写一遍（能省三十万次 setBlockData）。
            if (entry.y() == -1 && entry.block().id().endsWith("grass_block")) {
                continue;
            }
            BlockData data = data(entry.block());
            if (data == null) {
                continue;
            }
            int x = ox + entry.x();
            int y = ground + entry.y();
            int z = oz + entry.z();
            if (y < world.getMinHeight() || y >= world.getMaxHeight()) {
                continue;
            }
            world.getBlockAt(x, y, z).setBlockData(data, false);
            placed++;
        }
        return placed;
    }

    /**
     * 放几头牲口在城外的田边。
     *
     * <p>半生存的一半是"有东西可养"。田里长着麦子却一头牛也没有，那块地就只是布景。
     * 用一个世界级的标记防重复：重建城不该再多出一倍的羊。
     */
    private void populate(World world, int ox, int oz, int ground) {
        NamespacedKey key = new NamespacedKey(plugin, "livestock");
        if (world.getPersistentDataContainer().has(key, PersistentDataType.BYTE)) {
            return;
        }
        org.bukkit.entity.EntityType[] kinds = {
                org.bukkit.entity.EntityType.COW, org.bukkit.entity.EntityType.SHEEP,
                org.bukkit.entity.EntityType.CHICKEN, org.bukkit.entity.EntityType.PIG};
        int spawned = 0;
        for (int i = 0; i < 16; i++) {
            int x = ox - 580 + (i % 4) * 28;
            int z = oz + 70 + (i / 4) * 30;
            int y = world.getHighestBlockYAt(x, z) + 1;
            if (world.getBlockAt(x, y - 1, z).getType() != Material.GRASS_BLOCK) {
                continue;
            }
            world.spawnEntity(new org.bukkit.Location(world, x + 0.5, y, z + 0.5),
                    kinds[i % kinds.length]);
            spawned++;
        }
        world.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        plugin.getLogger().info("  田边放了 " + spawned + " 头牲口。");
    }

    private BlockData data(Block block) {
        return dataCache.computeIfAbsent(block, key -> {
            try {
                return Bukkit.createBlockData(key.bukkit());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("方块状态不认识，已跳过：" + key.bukkit());
                return null;
            }
        });
    }

    // -- 自检 ---------------------------------------------------------------

    /**
     * 抽查：房子真的在那儿吗。
     *
     * <p>只验"结构存在"，不验"好不好看"。好看只能靠眼睛，存在可以靠坐标——
     * 而"根本没盖"和"盖到别处去了"正是最难从日志里看出来的两种失败。
     */
    public List<String> verify(World world, int ox, int oz) {
        int ground = groundOf(world, ox, oz);
        List<String> failures = new ArrayList<>();
        probe(world, failures, "南城墙", ox + 40, ground + 4, oz + MedievalCity.HALF,
                Material.STONE_BRICKS, Material.MOSSY_STONE_BRICKS);
        probe(world, failures, "主街铺地", ox, ground - 1, oz + 200,
                Material.STONE_BRICKS, Material.ANDESITE, Material.COBBLESTONE);
        probe(world, failures, "环市集", ox, ground - 1, oz + 100,
                Material.STONE_BRICKS, Material.ANDESITE, Material.CRACKED_STONE_BRICKS);
        probe(world, failures, "金字塔塔身", ox + 66, ground + 5, oz,
                Material.SANDSTONE, Material.STONE_BRICKS, Material.CHISELED_STONE_BRICKS);
        probe(world, failures, "金字塔祭坛", ox, ground + 57, oz,
                Material.CHISELED_STONE_BRICKS, Material.GOLD_BLOCK);
        probe(world, failures, "出生点台面", ox, ground + 59, oz + 1, Material.GOLD_BLOCK);
        probe(world, failures, "演讲台", ox, ground + 3, oz - 86,
                Material.CHISELED_STONE_BRICKS, Material.STONE_BRICKS);
        probe(world, failures, "王座大厅", ox - 46, ground + 20, oz - 260, Material.STONE_BRICKS);
        probe(world, failures, "国库铁门", ox - 72, ground + 1, oz - 290, Material.IRON_BLOCK);
        probe(world, failures, "角斗场沙地", ox + 200, ground - 1, oz + 200,
                Material.SAND, Material.COARSE_DIRT);
        probe(world, failures, "角斗场包厢", ox + 245, ground + 12, oz + 160,
                Material.DARK_OAK_PLANKS);
        probe(world, failures, "城堡主楼", ox - 268, ground + 30, oz - 268, Material.STONE_BRICKS);
        probe(world, failures, "公会铁门", ox + 160, ground + 2, oz - 30, Material.IRON_BARS);
        probe(world, failures, "银行库房", ox + 42, ground - 2, oz + 144, Material.IRON_BARS);
        probe(world, failures, "客栈楼板", ox - 40, ground + 5, oz + 265, Material.DARK_OAK_PLANKS);
        probe(world, failures, "府邸水池", ox + 134, ground - 1, oz - 282, Material.WATER);
        probe(world, failures, "贫民窟巷", ox - 188, ground - 1, oz + 140, Material.COBBLESTONE);
        probe(world, failures, "码头水面", ox - 170, ground - 2, oz + 350, Material.WATER);
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
        failures.add(String.format(Locale.ROOT, "%s 应为 %s，实际 %s（%d,%d,%d）", what,
                java.util.Arrays.toString(expected), actual, x, y, z));
    }

    /** 调试用：把场景里有多少种方块打出来。 */
    public String describeScene() {
        Scene scene = MedievalCity.build();
        return "场景 " + scene.size() + " 方块，范围 x " + scene.minX() + ".." + scene.maxX()
                + "，y " + scene.minY() + ".." + scene.maxY()
                + "，z " + scene.minZ() + ".." + scene.maxZ();
    }

    /**
     * 出生点若在城墙外很远，第一次登录时把人放回南门。
     *
     * <p>世界可能比城先存在，玩家存档里记着旧位置——那是一片空地。
     * 这种情况只能靠"把他放回去"解决，而且只做一次。
     */
    public void pullStray(org.bukkit.entity.Player player, World world, int ox, int oz) {
        NamespacedKey key = new NamespacedKey(plugin, "pulled-to-city");
        if (player.getPersistentDataContainer().has(key, PersistentDataType.BYTE)) {
            return;
        }
        player.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        if (!player.getWorld().equals(world)) {
            return;
        }
        double dx = Math.abs(player.getLocation().getX() - ox);
        double dz = Math.abs(player.getLocation().getZ() - oz);
        if (dx < MedievalCity.HALF && dz < MedievalCity.HALF) {
            return;
        }
        int ground = groundOf(world, ox, oz);
        populate(world, ox, oz, ground);

        player.teleport(safeSpawn(world, ox, oz, ground));
        player.sendMessage(net.kyori.adventure.text.Component
                .text("你上一次登录在城外很远的地方。城已经建好了，从这里开始。"));
    }

    /**
     * 出生点：从顶坛的预期高度往下扫，找第一个"脚下实心、身上两格空气"的位置。
     *
     * <p>写死高度是不行的——世界可能比城先存在，也可能建过一半就停了。坐标只是意图，
     * 方块才是事实。扫不到就退到城中心的地面，总比让人卡在石头里强。
     */
    public static org.bukkit.Location safeSpawn(World world, int ox, int oz, int ground) {
        int[] spawn = MedievalCity.spawn();
        int x = ox + spawn[0];
        int z = oz + spawn[1];
        // 只从"预期高度往下"扫。往上扫会先撞到祭坛的华盖顶——那不是祭坛，是它的屋顶
        int from = ground + MedievalCity.spawnHeight();
        int to = Math.max(world.getMinHeight() + 2, ground + MedievalCity.spawnHeight() - 12);
        for (int y = from; y >= to; y--) {
            if (!world.getBlockAt(x, y - 1, z).getType().isSolid()) {
                continue;
            }
            if (world.getBlockAt(x, y, z).isEmpty() && world.getBlockAt(x, y + 1, z).isEmpty()) {
                return new org.bukkit.Location(world, x + 0.5, y, z + 0.5);
            }
        }
        int y = world.getHighestBlockYAt(x, z) + 1;
        return new org.bukkit.Location(world, x + 0.5, y, z + 0.5);
    }

    /**
     * 把世界出生点校正一遍。城已经建过、只是出生点歪了的时候用这个，不必重建。
     *
     * @return 校正后的出生点；世界不对时返回 null
     */
    public org.bukkit.Location ensureSpawn(World world, int ox, int oz) {
        if (world == null) {
            return null;
        }
        org.bukkit.Location now = world.getSpawnLocation();
        boolean bad = now.getBlock().getType().isSolid()
                || now.clone().add(0, 1, 0).getBlock().getType().isSolid()
                || !now.clone().subtract(0, 1, 0).getBlock().getType().isSolid();
        if (!bad) {
            return now;
        }
        org.bukkit.Location fixed = safeSpawn(world, ox, oz, groundOf(world, ox, oz));
        world.setSpawnLocation(fixed);
        plugin.getLogger().info("  出生点原来卡在方块里（" + now.getBlockX() + "," + now.getBlockY()
                + "," + now.getBlockZ() + "），已改到 " + fixed.getBlockX() + ","
                + fixed.getBlockY() + "," + fixed.getBlockZ() + "。");
        return fixed;
    }

    /**
     * 把卡在方块里的人弄出来：向上找第一个能站的地方，找不到就送到出生点。
     *
     * <p>这不是给"玩家挖矿掉进岩浆"用的，是给出生成点落在石头里这种事兜底的。
     */
    public boolean unstick(org.bukkit.entity.Player player) {
        org.bukkit.Location loc = player.getLocation();
        org.bukkit.block.Block feet = loc.getBlock();
        boolean stuck = feet.getType().isSolid()
                || feet.getRelative(org.bukkit.block.BlockFace.UP).getType().isSolid();
        if (!stuck) {
            return false;
        }
        for (int y = loc.getBlockY(); y <= Math.min(player.getWorld().getMaxHeight() - 2,
                loc.getBlockY() + 96); y++) {
            if (!player.getWorld().getBlockAt(loc.getBlockX(), y - 1, loc.getBlockZ()).getType().isSolid()) {
                continue;
            }
            if (player.getWorld().getBlockAt(loc.getBlockX(), y, loc.getBlockZ()).isEmpty()
                    && player.getWorld().getBlockAt(loc.getBlockX(), y + 1, loc.getBlockZ()).isEmpty()) {
                // Location#setY 是 void，不能链式写
                org.bukkit.Location target = loc.clone();
                target.setY(y);
                player.teleport(target);
                player.sendMessage(net.kyori.adventure.text.Component
                        .text("你刚才卡在方块里，已经把你挪到上面了。")
                        .color(net.kyori.adventure.text.format.NamedTextColor.GRAY));
                return true;
            }
        }
        if (loc.getWorld() != null) {
            player.teleport(loc.getWorld().getSpawnLocation());
            return true;
        }
        return false;
    }

    /** 给 doctor 用：城是否已经盖过。 */
    public static String versionKey() {
        return "3";
    }

    /** 保存状态用的方块快照接口，留给将来的"局部重建"。 */
    public interface Snapshot {
        BlockState state();
    }
}
