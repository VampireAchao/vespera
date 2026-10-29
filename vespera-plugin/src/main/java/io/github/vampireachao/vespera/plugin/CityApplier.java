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

    /** 场地平整半径（比城墙多留一圈，放得下城外那几处）。 */
    private static final int LEVEL_RADIUS = MedievalCity.HALF + 30;

    private final VesperaPlugin plugin;
    private final Map<Block, BlockData> dataCache = new HashMap<>();

    public CityApplier(VesperaPlugin plugin) {
        this.plugin = plugin;
    }

    // -- 入口 ---------------------------------------------------------------

    public int build(World world, int ox, int oz) {
        long started = System.currentTimeMillis();
        int ground = groundOf(world, ox, oz);
        plugin.getLogger().info("城市基面 y=" + ground + "，开始整平（半径 " + LEVEL_RADIUS + "）……");
        level(world, ox, oz, ground, LEVEL_RADIUS);
        levelOuterPads(world, ox, oz, ground);
        carveWater(world, ox, oz, ground);

        long leveled = System.currentTimeMillis();
        plugin.getLogger().info("  整平完成，用时 " + (leveled - started) + "ms，开始铺城……");

        Scene scene = MedievalCity.build();
        int placed = apply(world, scene, ox, oz, ground);

        populate(world, ox, oz, ground);

        int[] spawn = MedievalCity.spawn();
        world.setSpawnLocation(ox + spawn[0], ground + 1, oz + spawn[1]);
        long done = System.currentTimeMillis();
        plugin.getLogger().info("流明陨城已就位：" + scene.size() + " 个方块（写入 " + placed + "），"
                + "整平 " + (leveled - started) + "ms + 建造 " + (done - leveled) + "ms。");
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

    /** 削高填低。整平之后每个格子顶面都等于 ground。 */
    private void level(World world, int ox, int oz, int ground, int radius) {
        int cleared = 0;
        int filled = 0;
        for (int x = ox - radius; x <= ox + radius; x++) {
            for (int z = oz - radius; z <= oz + radius; z++) {
                if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                    world.getChunkAt(x >> 4, z >> 4);
                }
                int top = world.getHighestBlockYAt(x, z, HeightMap.WORLD_SURFACE);
                // 城外三十格缓降三格：城墙不会在草原上切出一道直角
                int r = Math.max(Math.abs(x - ox), Math.abs(z - oz));
                int target = r <= MedievalCity.HALF ? ground
                        : ground - Math.min(3, (r - MedievalCity.HALF) / 10);
                if (top > target) {
                    for (int y = target + 1; y <= top; y++) {
                        if (!world.getBlockAt(x, y, z).isEmpty()) {
                            world.getBlockAt(x, y, z).setType(Material.AIR, false);
                            cleared++;
                        }
                    }
                } else if (top < target) {
                    for (int y = top + 1; y <= target; y++) {
                        world.getBlockAt(x, y, z)
                                .setType(y == target ? Material.GRASS_BLOCK : Material.DIRT, false);
                        filled++;
                    }
                }
                world.getBlockAt(x, target, z).setType(Material.GRASS_BLOCK, false);
            }
        }
        plugin.getLogger().info("  削掉 " + cleared + " 格，填上 " + filled + " 格。");
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
                pads.add(new int[] {-512, 28, -344, 256});  // 农田与风车
        pads.add(new int[] {310, -430, 430, -310}); // 北村
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
                for (int y = ground - 5; y <= ground; y++) {
                    world.getBlockAt(x, y, z).setType(
                            y <= ground - 1 ? Material.WATER : Material.AIR, false);
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
            int x = ox - 470 + (i % 4) * 28;
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
        probe(world, failures, "金字塔祭坛", ox, ground + 64, oz,
                Material.CHISELED_STONE_BRICKS, Material.GOLD_BLOCK);
        probe(world, failures, "王座大厅", ox - 46, ground + 20, oz - 260, Material.STONE_BRICKS);
        probe(world, failures, "角斗场沙地", ox + 185, ground - 1, oz + 185,
                Material.SAND, Material.COARSE_DIRT);
        probe(world, failures, "城堡主楼", ox - 268, ground + 30, oz - 268, Material.STONE_BRICKS);
        probe(world, failures, "码头水面", ox - 170, ground - 1, oz + 350, Material.WATER);
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

        int[] spawn = MedievalCity.spawn();
        player.teleport(new org.bukkit.Location(world, ox + spawn[0] + 0.5, ground + 1.2,
                oz + spawn[1] + 0.5));
        player.sendMessage(net.kyori.adventure.text.Component
                .text("你上一次登录在城外很远的地方。城已经建好了，从这里开始。"));
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
