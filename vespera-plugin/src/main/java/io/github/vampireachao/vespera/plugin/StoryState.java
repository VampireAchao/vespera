package io.github.vampireachao.vespera.plugin;

import io.github.vampireachao.vespera.core.StoryVars;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * 玩家的剧情状态：旗标与计数。
 *
 * <p>存在玩家的 {@link PersistentDataContainer} 里，而不是记分板。原因有三条，都很实际：
 *
 * <ul>
 *   <li>设计书列了十几组变量，其中 {@code aff_<id>} 一组就有三十个。记分板能做，但每加一个
 *       NPC 就要手工建一条 objective，而 PDC 加一个键是零成本。</li>
 *   <li>PDC 跟玩家数据一起存，服务器崩了也不丢半截；记分板要自己负责落盘与迁移。</li>
 *   <li>读 PDC 不会惊动任何别的插件。剧情状态不该是全局广播。</li>
 * </ul>
 *
 * <p>代价是：记分板能被数据包、命令方块、别的插件直接读，PDC 不能。所以需要对外暴露的
 * 少数几个值（比如"当前教学日"）会**同时**镜像到记分板，作为只读副本。权威仍在 PDC。
 */
public final class StoryState {

    private final NamespacedKey flagKey;
    private final NamespacedKey numKey;

    public StoryState(VesperaPlugin plugin) {
        this.flagKey = new NamespacedKey(plugin, "flag");
        this.numKey = new NamespacedKey(plugin, "num");
    }

    /** 键名只允许小写字母、数字与下划线；剧本里写中文键会被规整掉，而不是抛异常。 */
    static String sanitize(String name) {
        return name.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]+", "_");
    }

    private NamespacedKey child(Player player, NamespacedKey root, String name) {
        // PDC 的键挂在玩家自己的容器上，用 "flag:xxx" 这种复合名区分不同条目。
        return new NamespacedKey(root.getNamespace(), root.getKey() + "." + sanitize(name));
    }

    public boolean flag(Player player, String name) {
        Byte value = container(player).get(child(player, flagKey, name), PersistentDataType.BYTE);
        return value != null && value == 1;
    }

    public void setFlag(Player player, String name, boolean on) {
        if (on) {
            container(player).set(child(player, flagKey, name), PersistentDataType.BYTE, (byte) 1);
        } else {
            container(player).remove(child(player, flagKey, name));
        }
    }

    public int num(Player player, String name) {
        Integer value = container(player).get(child(player, numKey, name), PersistentDataType.INTEGER);
        return value == null ? 0 : value;
    }

    public void add(Player player, String name, int delta) {
        container(player).set(child(player, numKey, name), PersistentDataType.INTEGER, num(player, name) + delta);
    }

    public void set(Player player, String name, int value) {
        container(player).set(child(player, numKey, name), PersistentDataType.INTEGER, value);
    }

    /** 交给对话图的只读视图。每打开一次对话框取一次，不缓存——状态是会变的。 */
    public StoryVars view(Player player, RaceBridge races) {
        return new StoryVars() {
            @Override
            public String race() {
                return races.race(player);
            }

            @Override
            public boolean flag(String name) {
                return StoryState.this.flag(player, name);
            }

            @Override
            public int num(String name, int fallback) {
                Integer value = container(player).get(child(player, numKey, name), PersistentDataType.INTEGER);
                return value == null ? fallback : value;
            }
        };
    }

    /** {@code /vespera state} 的输出：把这个玩家身上记着的所有东西列出来。 */
    public List<String> dump(Player player) {
        List<String> lines = new ArrayList<>();
        String prefix = flagKey.getKey() + ".";
        String numPrefix = numKey.getKey() + ".";
        for (NamespacedKey key : container(player).getKeys()) {
            if (!key.getNamespace().equals(flagKey.getNamespace())) {
                continue;
            }
            if (key.getKey().startsWith(prefix)) {
                lines.add("旗标 " + key.getKey().substring(prefix.length()));
            } else if (key.getKey().startsWith(numPrefix)) {
                lines.add("计数 " + key.getKey().substring(numPrefix.length()) + " = "
                        + num(player, key.getKey().substring(numPrefix.length())));
            }
        }
        lines.sort(String::compareTo);
        return lines;
    }

    private PersistentDataContainer container(Player player) {
        return player.getPersistentDataContainer();
    }
}
