package io.github.vampireachao.vespera.plugin;

import io.github.vampireachao.vespera.core.Region;
import io.github.vampireachao.vespera.core.RegionBook;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * 区域表：地图上每一块地方叫什么、属于哪个群系、能不能打人。
 *
 * <p>它读的是 {@code vespera-world} 产出的 {@code regions/regions.yml}——同一份文件，
 * 地图组按它建造，插件按它判定。**只有一份坐标，所以不会漂。**
 *
 * <p>顺带管下界门的定向。这件事本来该是"传送门"插件的活，但我们的需求只有一条：
 * 城里的门都通到熔炉之喉。为此装一个插件不值得，二十行事件监听够用。
 */
public final class RegionService {

    private final VesperaPlugin plugin;
    private final List<PortalRule> portals = new ArrayList<>();
    private RegionBook book = RegionBook.EMPTY;
    private File file;

    public RegionService(VesperaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        book = RegionBook.EMPTY;
        portals.clear();

        file = locate();
        if (file == null || !file.isFile()) {
            plugin.getLogger().warning("没有找到区域表（预期在 server/vespera/regions/regions.yml）。"
                    + "区域判定与下界门定向都会关闭，其余功能不受影响。");
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        List<Map<String, ?>> raw = new ArrayList<>();
        for (Map<?, ?> entry : yaml.getMapList("regions")) {
            Map<String, Object> typed = new LinkedHashMap<>();
            entry.forEach((key, value) -> typed.put(String.valueOf(key), value));
            raw.add(typed);
        }
        book = RegionBook.parse(raw);
        for (Region region : book.all()) {
            if (region.has("broken")) {
                plugin.getLogger().warning("区域表里有一条读不出来，已跳过：" + region.id());
            }
        }

        for (Map<?, ?> entry : yaml.getMapList("portals")) {
            PortalRule rule = PortalRule.parse(entry);
            if (rule != null) {
                portals.add(rule);
            }
        }
        plugin.getLogger().info("区域表已载入：" + book.all().size() + " 块地方，" + portals.size() + " 条门规则。");
    }

    /**
     * 找到区域表。优先用世界容器下的资产目录（`vespera-world` 同步过来的那份），
     * 找不到再看插件自己的数据目录——开发时两处都可能存在，先认地图那一份。
     */
    private File locate() {
        String assets = plugin.getConfig().getString("world.assets", "vespera");
        File staged = new File(Bukkit.getWorldContainer(), assets + "/regions/regions.yml");
        if (staged.isFile()) {
            return staged;
        }
        File local = new File(plugin.getDataFolder(), "regions.yml");
        return local.isFile() ? local : staged;
    }

    public Optional<Region> at(Location location) {
        if (location == null || location.getWorld() == null) {
            return Optional.empty();
        }
        return book.at(canonical(location.getWorld()), location.getX(), location.getY(), location.getZ());
    }

    /**
     * 把 Bukkit 的世界名换成区域表里的标准名。
     *
     * <p>区域表写的是 {@code world / world_nether / world_the_end}，那是维度的名字；
     * 而 Bukkit 的世界名带 {@code level-name} 前缀（平世界叫 {@code campus} 时，
     * 下界就是 {@code campus_nether}）。不做这一层换算，换个 level-name 就会
     * "所有区域都匹配不上、所有门规则都失效"，而且日志里一声不响。
     */
    public static String canonical(World world) {
        if (world == null) {
            return "";
        }
        return switch (world.getEnvironment()) {
            case NETHER -> "world_nether";
            case THE_END -> "world_the_end";
            default -> "world";
        };
    }

    /** 按标准名找世界；找不到再按维度找。找不到返回 null，调用方自己决定怎么退化。 */
    public static World resolve(String name) {
        World direct = Bukkit.getWorld(name == null ? "" : name);
        if (direct != null) {
            return direct;
        }
        World.Environment environment = switch (name == null ? "" : name) {
            case "world_nether" -> World.Environment.NETHER;
            case "world_the_end" -> World.Environment.THE_END;
            default -> World.Environment.NORMAL;
        };
        return Bukkit.getWorlds().stream()
                .filter(world -> world.getEnvironment() == environment)
                .findFirst()
                .orElse(null);
    }

    /** 给 {@code /vespera where} 的一句话。 */
    public String describe(Location location) {
        if (location == null || location.getWorld() == null) {
            return "不在任何世界里。";
        }
        Optional<Region> region = at(location);
        String where = region
                .map(found -> found.name() + "（" + found.kind() + " · " + found.biome() + "）"
                        + (found.tags().isEmpty() ? "" : "　" + String.join("/", found.tags())))
                .orElse("野外（未登记）");
        World world = location.getWorld();
        return String.format("%s　%s　%.0f, %.0f, %.0f　群系 %s",
                where, world.getName(), location.getX(), location.getY(), location.getZ(),
                world.getBiome(location.getBlockX(), location.getBlockY(), location.getBlockZ()).getKey());
    }

    public RegionBook book() {
        return book;
    }

    public List<PortalRule> portals() {
        return List.copyOf(portals);
    }

    /**
     * 一条门规则：从哪儿进，落到哪儿。
     *
     * <p>只匹配"从哪个区域进"和"从哪个维度进"两件事，不匹配具体方块——因为玩家会在自家
     * 地下室搭门，位置根本不可预测。按区域匹配，才能做到"城里的门都通到熔炉之喉"，
     * 而"荒野里搭的门仍然通荒野"。
     *
     * <p>目标世界**不在解析时解析**。维度可能比插件晚加载，而且 {@code level-name} 一改
     * （平世界叫 {@code campus}），{@code world_nether} 就变成了 {@code campus_nether}。
     * 早解析的结果是"规则静默消失"，那是最难查的一类 bug。
     */
    public record PortalRule(String id, String fromWorld, String fromRegion,
            String targetWorld, double x, double y, double z) {

        static PortalRule parse(Map<?, ?> raw) {
            Object to = raw.get("to");
            if (!(to instanceof Map<?, ?> target)) {
                return null;
            }
            String fromWorld = raw.get("from-world") == null ? null : String.valueOf(raw.get("from-world"));
            String fromRegion = raw.get("from-region") == null ? null : String.valueOf(raw.get("from-region"));
            return new PortalRule(String.valueOf(raw.get("id")), fromWorld, fromRegion,
                    String.valueOf(target.get("world")),
                    number(target.get("x")), number(target.get("y")), number(target.get("z")));
        }

        private static double number(Object raw) {
            if (raw instanceof Number value) {
                return value.doubleValue();
            }
            return raw == null ? 0 : Double.parseDouble(String.valueOf(raw).trim());
        }

        /** 解析后的落点；那个维度还没加载就是 null（这一次传送按原版处理）。 */
        public Location target() {
            World world = RegionService.resolve(targetWorld);
            return world == null ? null : new Location(world, x, y, z);
        }

        public boolean matches(Location from, Region region) {
            if (from == null || from.getWorld() == null) {
                return false;
            }
            if (fromWorld != null && !fromWorld.equals(RegionService.canonical(from.getWorld()))) {
                return false;
            }
            if (fromRegion == null) {
                return true;
            }
            return region != null && fromRegion.equals(region.id());
        }
    }
}
