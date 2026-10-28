package io.github.vampireachao.vespera.plugin;

import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * 读玩家的种族，从超自然种族插件那里。
 *
 * <p>为什么用反射而不是编译期依赖：那个插件的产物不在任何 Maven 仓库里（它走 GitHub
 * Release 分发），把它当 {@code provided} 依赖会让别人的一次干净 clone 编不过。反射的
 * 代价是方法名写错要到运行时才知道——所以这里只认三个方法，且在 {@code doctor} 里
 * 明说自己有没有连上。
 *
 * <p>正确的长期解法是让上游暴露一个服务（{@code ServicesManager}）。上游就在同一个人的
 * 仓库里，加一个 {@code SuperNApi} 接口是几十行的事，那时这一段可以整块删掉。
 */
public final class RaceBridge {

    private static final String MANAGER = "me.matterz.supernaturals.manager.SuperNManager";
    private static final String PLUGIN = "mmSupernaturals";

    private final VesperaPlugin plugin;
    private boolean resolved;
    private Method getByPlayer;
    private Method getType;
    private Method getPower;
    private Method isSuper;

    public RaceBridge(VesperaPlugin plugin) {
        this.plugin = plugin;
    }

    private void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        Plugin target = Bukkit.getPluginManager().getPlugin(PLUGIN);
        if (target == null) {
            return;
        }
        try {
            ClassLoader loader = target.getClass().getClassLoader();
            Class<?> manager = Class.forName(MANAGER, true, loader);
            getByPlayer = manager.getMethod("get", Player.class);
            Class<?> player = Class.forName("me.matterz.supernaturals.SuperNPlayer", true, loader);
            getType = player.getMethod("getType");
            getPower = player.getMethod("getPower");
            isSuper = player.getMethod("isSuper");
        } catch (ReflectiveOperationException | LinkageError e) {
            plugin.getLogger().warning("找到了 mmSupernaturals，但读不到它的玩家对象：" + e
                    + "（种族判定会一律返回 human）");
            getByPlayer = null;
        }
    }

    /** 插件在，且三处方法都对得上。 */
    public boolean available() {
        resolve();
        return getByPlayer != null;
    }

    /** 小写的种族名。连不上时一律 human——这比抛异常好，至少服务器还能跑。 */
    public String race(Player player) {
        Object sn = call(player);
        if (sn == null) {
            return "human";
        }
        try {
            return String.valueOf(getType.invoke(sn)).toLowerCase(java.util.Locale.ROOT);
        } catch (ReflectiveOperationException e) {
            return "human";
        }
    }

    /** 血势。只读——设计书铁律一：剧情不得改写数值，所以这里没有 setter。 */
    public int power(Player player) {
        Object sn = call(player);
        if (sn == null) {
            return 0;
        }
        try {
            return (int) Math.round((Double) getPower.invoke(sn));
        } catch (ReflectiveOperationException e) {
            return 0;
        }
    }

    /** {@code isSuper()}：五血为真，人类与三职为假。全篇所有判定的分叉点。 */
    public boolean superNatural(Player player) {
        Object sn = call(player);
        if (sn == null) {
            return false;
        }
        try {
            return (boolean) isSuper.invoke(sn);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private Object call(Player player) {
        resolve();
        if (getByPlayer == null || player == null) {
            return null;
        }
        try {
            return getByPlayer.invoke(null, player);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
