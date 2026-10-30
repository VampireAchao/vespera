package io.github.vampireachao.vespera.plugin;

import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;

/**
 * 把圣座顶坛登记成 mmSupernaturals 的一座"教堂"。
 *
 * <p>为什么需要这一层：那个插件判定祭司祭坛要同时满足两件事——右键的是
 * {@code DIAMOND_BLOCK}，而且这块钻石在<b>某个已注册教堂</b>的 ±10 格之内
 * （{@code SNConfigHandler.isChurch}）。钻石块我们能砌，教堂点却只能靠
 * {@code /sn setChurch} 手动设一次，而那次设置写在它自己的 config 里——
 * 换一张世界就作废，没人会记得重设。
 *
 * <p>所以改成在启动时自己登记：城建完 → 找到顶坛那块钻石 → 不在就 {@code addChurch}
 * 一次。{@code isChurch} 先问一遍是为了不重复写：{@code addChurch} 每次都会落盘。
 *
 * <p>仍然用反射，理由和 {@link RaceBridge} 一样：那个 jar 没有 Maven 坐标，
 * 编译期依赖它会让一次干净的 clone 编不过。
 */
public final class ChurchBridge {

    private static final String PLUGIN = "mmSupernaturals";
    private static final String CONFIG = "me.matterz.supernaturals.io.SNConfigHandler";

    /** 顶坛那块钻石相对场地基面的高度（场景 y=60）。找不到方块时按这个兜底。 */
    private static final int ALTAR_HEIGHT = 60;

    private final VesperaPlugin plugin;
    private boolean resolved;
    private Method isChurch;
    private Method addChurch;
    private Location altar;
    private String problem;

    public ChurchBridge(VesperaPlugin plugin) {
        this.plugin = plugin;
    }

    private void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        Plugin target = Bukkit.getPluginManager().getPlugin(PLUGIN);
        if (target == null) {
            problem = "没有装 " + PLUGIN;
            return;
        }
        try {
            ClassLoader loader = target.getClass().getClassLoader();
            Class<?> handler = Class.forName(CONFIG, true, loader);
            isChurch = handler.getMethod("isChurch", Location.class);
            addChurch = handler.getMethod("addChurch", Location.class);
        } catch (ReflectiveOperationException | LinkageError e) {
            problem = "读不到 " + CONFIG + "：" + e;
        }
    }

    /** 插件在，且两个方法都对得上。 */
    public boolean available() {
        resolve();
        return isChurch != null && addChurch != null;
    }

    /**
     * 登记（如果还没有）。返回这次到底做成了没有。
     *
     * <p>调用时机必须是<b>城已经铺完之后</b>：要先有那块钻石，才知道教堂点该落在哪。
     */
    public boolean ensure(World world, int ox, int oz, int ground) {
        resolve();
        if (!available() || world == null) {
            return false;
        }
        altar = findAltar(world, ox, oz, ground);
        try {
            if ((boolean) isChurch.invoke(null, altar)) {
                return true;
            }
            addChurch.invoke(null, altar);
            plugin.getLogger().info("  祭坛已登记为教堂：" + describe(altar));
            return true;
        } catch (ReflectiveOperationException e) {
            problem = "登记教堂失败：" + e;
            return false;
        }
    }

    /**
     * 顶坛那块钻石在哪。先扫方块、扫不到才用常量。
     *
     * <p>为什么扫：坐标只是意图，方块才是事实。祭台改过一次高度、也改过一次材料，
     * 写死的数会安静地指到空气上去。
     */
    public Location findAltar(World world, int ox, int oz, int ground) {
        for (int y = ground + ALTAR_HEIGHT + 8; y >= ground + 40; y--) {
            Block block = world.getBlockAt(ox, y, oz);
            if (block.getType() == Material.DIAMOND_BLOCK) {
                return block.getLocation();
            }
        }
        return new Location(world, ox, ground + ALTAR_HEIGHT, oz);
    }

    /** 这一块算不算"在教堂范围内"。命令与自检都用它，避免两处各写一遍判定。 */
    public boolean registered(Location at) {
        resolve();
        if (!available() || at == null) {
            return false;
        }
        try {
            return (boolean) isChurch.invoke(null, at);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    /** 上次登记的那块祭坛；没登记过是 null。 */
    public Location altar() {
        return altar;
    }

    /** 连不上时人话说明；连得上返回 null。 */
    public String problem() {
        resolve();
        return problem;
    }

    public static String describe(Location at) {
        return at.getWorld().getName() + " (" + at.getBlockX() + ", " + at.getBlockY() + ", "
                + at.getBlockZ() + ")";
    }
}
