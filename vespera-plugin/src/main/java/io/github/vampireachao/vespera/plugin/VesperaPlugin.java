package io.github.vampireachao.vespera.plugin;

import java.io.File;
import java.util.logging.Level;
import org.bukkit.command.PluginCommand;
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

    /** 首次启动写进 {@code plugins/Vespera/dialogues/} 的两份示例。 */
    private static final String[] BUNDLED_DIALOGUES = {
            "dialogues/bell-test.yml",
            "dialogues/altar-warning.yml"
    };

    private StoryState state;
    private RaceBridge races;
    private DialogueService dialogues;
    private ClockService clock;
    private NpcService npcs;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveBundledDialogues();

        state = new StoryState(this);
        races = new RaceBridge(this);
        dialogues = new DialogueService(this, state, races);
        clock = new ClockService(this);
        npcs = new NpcService(this);

        int loaded = dialogues.load();
        npcs.spawnAll();
        getServer().getPluginManager().registerEvents(new VesperaListener(this), this);

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

    /** 重载配置、剧本与锚点。玩家状态不动——那是玩家身上的东西，不该被 reload 抹掉。 */
    public void reload() {
        reloadConfig();
        dialogues.load();
        npcs.spawnAll();
        clock.stop();
        clock.start();
    }

    private void saveBundledDialogues() {
        for (String resource : BUNDLED_DIALOGUES) {
            File target = new File(getDataFolder(), resource);
            if (!target.isFile()) {
                try {
                    saveResource(resource, false);
                } catch (IllegalArgumentException e) {
                    getLogger().log(Level.WARNING, "自带剧本 " + resource + " 没能写出来", e);
                }
            }
        }
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
