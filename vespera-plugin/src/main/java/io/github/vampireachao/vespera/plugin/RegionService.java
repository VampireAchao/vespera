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
        return book.at(location.getWorld().getName(), location.getX(), location.getY(), location.getZ());
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
     */
    public record PortalRule(String id, String fromWorld, String fromRegion, Location target) {

        static PortalRule parse(Map<?, ?> raw) {
            Object to = raw.get("to");
            if (!(to instanceof Map<?, ?> target)) {
                return null;
            }
            World world = Bukkit.getWorld(String.valueOf(target.get("world")));
            if (world == null) {
                return null;
            }
            String fromWorld = raw.get("from-world") == null ? null : String.valueOf(raw.get("from-world"));
            String fromRegion = raw.get("from-region") == null ? null : String.valueOf(raw.get("from-region"));
            double x = number(target.get("x"));
            double y = number(target.get("y"));
            double z = number(target.get("z"));
            return new PortalRule(String.valueOf(raw.get("id")), fromWorld, fromRegion,
                    new Location(world, x, y, z));
        }

        private static double number(Object raw) {
            if (raw instanceof Number value) {
                return value.doubleValue();
            }
            return raw == null ? 0 : Double.parseDouble(String.valueOf(raw).trim());
        }

        public boolean matches(Location from, Region region) {
            if (from == null || from.getWorld() == null) {
                return false;
            }
            if (fromWorld != null && !fromWorld.equals(from.getWorld().getName())) {
                return false;
            }
            if (fromRegion == null) {
                return true;
            }
            return region != null && fromRegion.equals(region.id());
        }
    }
}
