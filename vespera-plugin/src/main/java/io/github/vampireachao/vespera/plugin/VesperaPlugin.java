package io.github.vampireachao.vespera.plugin;

import io.github.vampireachao.vespera.core.CampusPlan;
import io.github.vampireachao.vespera.core.world.MedievalCity;
import java.io.File;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 维斯佩拉的插件入口。
 *
 * <p>它刻意很小。所有真正的工作在四个服务里，每个只负责一件事：时钟报时、对话开合、
 * 状态读写、锚点站立。这样一个人的服务器也不会因为一块坏掉就全坏——对话读不出来，
 * 钟还在响；种族插件没装，剧本照样能谈。
 *
 * <p>启动日志只打一行总结加几行警告。安静的插件是健康的插件：需要你知道的事都在
 * {@code /vespera doctor} 里，不在刷屏里。
 */
public final class VesperaPlugin extends JavaPlugin {

    /** 首次启动写进 {@code plugins/Vespera/} 的默认文件。 */
    private static final String[] BUNDLED_FILES = {
            "content.yml",
            "dialogues/bell-test.yml",
            "dialogues/altar-warning.yml",
            "dialogues/dorm-night.yml",
            "dialogues/patrol.yml"
    };

    private StoryState state;
    private RaceBridge races;
    private ContentPolicy policy;
    private RegionService regions;
    private DialogueService dialogues;
    private ClockService clock;
    private NpcService npcs;
    private CityApplier city;
    private CampusBuilder campus;
    private int cityX;
    private int cityZ;

    /** 已建成的版本标记。改了平面就把它加一，下次启动重建。 */
    public static final String CITY_VERSION = "4";
    public static final String CAMPUS_VERSION = "1";

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveBundledFiles();

        state = new StoryState(this);
        races = new RaceBridge(this);
        policy = new ContentPolicy(this);
        policy.load();
        regions = new RegionService(this);
        regions.load();
        dialogues = new DialogueService(this, state, races, policy);
        clock = new ClockService(this);
        npcs = new NpcService(this);

        int loaded = dialogues.load();
        npcs.spawnAll();
        city = new CityApplier(this);
        campus = new CampusBuilder(this);
        // 先城后校：城定出生点。反过来会让玩家站在学院门口，看不见主城。
        buildCity();
        buildIfNeeded("campus", CAMPUS_VERSION, getConfig().getBoolean("campus.auto", false),
                CampusBuilder::originFrom, (world, ox, oz) -> campus.build(world, ox, oz));
        grantAdmins();
        getServer().getPluginManager().registerEvents(new VesperaListener(this), this);
        getServer().getPluginManager().registerEvents(new PortalGate(regions), this);

        PluginCommand command = getCommand("vespera");
        if (command != null) {
            VesperaCommand executor = new VesperaCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        } else {
            getLogger().severe("plugin.yml 里没有 vespera 这条命令，管理命令全部不可用。");
        }

        clock.start();
        getLogger().info("已就绪：" + loaded + " 张对话图，" + npcs.count() + " 个锚点"
                + (races.available() ? "，已接上 mmSupernaturals。" : "，未接上 mmSupernaturals（种族按 human 处理）。"));
    }

    @Override
    public void onDisable() {
        if (clock != null) {
            clock.stop();
        }
        if (npcs != null) {
            // 锚点下线即撤：世界文件里不留一堆站着的村民，重开时按配置重建。
            npcs.clear();
        }
    }

    /**
     * 世界刚加载时把灰盒盖出来。
     *
     * <p>同步盖、不延后：玩家进服第一眼看到的东西必须已经在那儿。代价是启动多几秒，
     * 换来的是"进服就站在主街上"而不是"站在一片空地上等它长出来"。
     *
     * <p>用世界身上的 PDC 记版本，不记在配置里——世界和建筑是一体的，
     * 换一张世界就该重建一次。
     */
    private void buildIfNeeded(String name, String version, boolean enabled,
            java.util.function.Function<VesperaPlugin, int[]> originOf, BuildTask task) {
        if (!enabled) {
            return;
        }
        World world = RegionService.resolve(getConfig().getString(name + ".world", "world"));
        if (world == null) {
            getLogger().warning("找不到可建造的世界（配置 " + name + ".world="
                    + getConfig().getString(name + ".world") + "，已加载："
                    + Bukkit.getWorlds().stream().map(World::getName).toList() + "），跳过。");
            return;
        }
        NamespacedKey key = new NamespacedKey(this, name);
        if (version.equals(world.getPersistentDataContainer().get(key, PersistentDataType.STRING))) {
            return;
        }
        int[] origin = originOf.apply(this);
        getLogger().info("首次启动，开始盖" + ("city".equals(name) ? "主城" : "校区") + "灰盒……");
        int placed = task.build(world, origin[0], origin[1]);
        world.getPersistentDataContainer().set(key, PersistentDataType.STRING, version);
        getLogger().info("  已放置 " + placed + " 个方块。");
    }

    /**
     * 建城。选址、整平、铺城三步都在 {@link CityApplier} 里，这里只负责"要不要建"。
     *
     * <p>自动建城的判断不看配置里的坐标，而看世界身上记着的版本——世界和城是一体的，
     * 换一张世界就该重建一次。
     */
    private void buildCity() {
        World world = RegionService.resolve(getConfig().getString("city.world", "world"));
        if (world == null) {
            getLogger().warning("找不到主城世界（city.world=" + getConfig().getString("city.world")
                    + "，已加载：" + Bukkit.getWorlds().stream().map(World::getName).toList() + "），跳过建城。");
            return;
        }
        int wantedX = getConfig().getInt("city.origin-x", 0);
        int wantedZ = getConfig().getInt("city.origin-z", 0);
        int[] site = city.site(world, wantedX, wantedZ);
        cityX = site[0];
        cityZ = site[1];

        NamespacedKey key = new NamespacedKey(this, "city");
        String built = world.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (CITY_VERSION.equals(built) && getConfig().getBoolean("city.auto", true)) {
            getLogger().info("城已在位：" + MedievalCity.summary() + "，中心 " + cityX + "," + cityZ);
            return;
        }
        if (!getConfig().getBoolean("city.auto", true)) {
            getLogger().info("city.auto=false，跳过建城（/vespera build city 可手动建）。");
            return;
        }
        city.build(world, cityX, cityZ);
        world.getPersistentDataContainer().set(key, PersistentDataType.STRING, CITY_VERSION);
    }

    /** 名单里的人进服即给 OP。私人服里这是最省事的一步。 */
    private void grantAdmins() {
        for (String name : getConfig().getStringList("admins")) {
            org.bukkit.OfflinePlayer player = Bukkit.getOfflinePlayerIfCached(name);
            if (player != null && player.isOnline() && !player.isOp()) {
                player.setOp(true);
            }
        }
    }

    /** 建造任务。两个灰盒的签名一致，用一个函数式接口把它们统一起来。 */
    @FunctionalInterface
    private interface BuildTask {
        int build(World world, int ox, int oz);
    }

    public CityApplier city() {
        return city;
    }

    public int cityX() {
        return cityX;
    }

    public int cityZ() {
        return cityZ;
    }

    public CampusBuilder campus() {
        return campus;
    }

    /** 重载配置、剧本与锚点。玩家状态不动——那是玩家身上的东西，不该被 reload 抹掉。 */
    public void reload() {
        reloadConfig();
        policy.load();
        regions.load();
        dialogues.load();
        npcs.spawnAll();
        clock.stop();
        clock.start();
    }

    private void saveBundledFiles() {
        for (String resource : BUNDLED_FILES) {
            File target = new File(getDataFolder(), resource);
            if (!target.isFile()) {
                try {
                    saveResource(resource, false);
                } catch (IllegalArgumentException e) {
                    getLogger().log(Level.WARNING, "自带文件 " + resource + " 没能写出来", e);
                }
            }
        }
    }

    public ContentPolicy policy() {
        return policy;
    }

    public RegionService regions() {
        return regions;
    }

    public StoryState state() {
        return state;
    }

    public RaceBridge races() {
        return races;
    }

    public DialogueService dialogues() {
        return dialogues;
    }

    public ClockService clock() {
        return clock;
    }

    public NpcService npcs() {
        return npcs;
    }
}
