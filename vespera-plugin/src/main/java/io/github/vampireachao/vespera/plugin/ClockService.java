package io.github.vampireachao.vespera.plugin;

import io.github.vampireachao.vespera.core.WorldClock;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * 校历时钟：在每一段的第一刻说一句话，并在晚钟那一响把音敲出来。
 *
 * <p>它不做别的事——不锁门、不传人、不推进剧情。这些留给剧本，因为"什么时候该发生什么"
 * 是叙事决定，不是时钟决定。时钟只负责让世界有一个共同的时间：玩家听见钟，就知道
 * 别人也听见了。
 *
 * <p>报时用 actionbar 而不是聊天栏：聊天栏属于玩家之间的说话，一条系统消息插进去就脏了；
 * actionbar 在视线下方，看一眼就没了，正合适。
 */
public final class ClockService {

    private final VesperaPlugin plugin;
    private final Map<UUID, WorldClock.Period> lastPeriod = new HashMap<>();
    private BukkitTask task;

    public ClockService(VesperaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        if (!plugin.getConfig().getBoolean("clock.enabled", true)) {
            return;
        }
        // 一秒一次。原版时间每 tick 走一格，一秒 20 格，一秒采样不会漏掉任何一段的边界。
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        lastPeriod.clear();
    }

    private void tick() {
        World world = Bukkit.getWorld(plugin.getConfig().getString("clock.world", "world"));
        if (world == null) {
            return;
        }
        WorldClock.Period period = WorldClock.periodOf(world.getTime());
        for (Player player : world.getPlayers()) {
            WorldClock.Period previous = lastPeriod.put(player.getUniqueId(), period);
            if (previous == null || previous == period) {
                // 刚进服的人不补报历史；同一段里不重复说话。
                continue;
            }
            announce(player, period);
        }
    }

    private void announce(Player player, WorldClock.Period period) {
        if (period == WorldClock.Period.NIGHT) {
            String sound = plugin.getConfig().getString("clock.sound", "minecraft:block.bell.use");
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        }
        String message = plugin.getConfig().getString("clock.messages." + period.name());
        if (message != null && !message.isBlank()) {
            player.sendActionBar(Component.text(message).color(NamedTextColor.GRAY));
        }
    }

    /** 给 {@code /vespera clock} 用的一句话。 */
    public String describe() {
        World world = Bukkit.getWorld(plugin.getConfig().getString("clock.world", "world"));
        if (world == null) {
            return "报时的世界不存在：" + plugin.getConfig().getString("clock.world");
        }
        return WorldClock.describe(world.getTime())
                + "　入夜判定：" + (WorldClock.isNight(world.getTime()) ? "已入夜" : "白天")
                + "（阈值 " + WorldClock.NIGHT_THRESHOLD + "）";
    }

    /** 玩家进来时清掉旧的段记录，免得换世界之后补报一次不相干的时。 */
    public void forget(UUID player) {
        lastPeriod.remove(player);
    }
}
