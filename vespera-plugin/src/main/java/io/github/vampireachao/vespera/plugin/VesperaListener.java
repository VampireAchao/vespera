package io.github.vampireachao.vespera.plugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * 世界与插件之间的那层薄膜：右键一个人，跟他说句话。
 *
 * <p>只做两件事，都是"把手"：玩家点到锚点时把事件拦下来（村民不该真的开交易界面），
 * 玩家下线时把时钟的临时记录清掉。
 */
public final class VesperaListener implements Listener {

    private final VesperaPlugin plugin;

    public VesperaListener(VesperaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        String id = plugin.npcs().npcId(event.getRightClicked());
        if (id == null) {
            return;
        }
        // 拦下来：不拦的话原版村民会弹出交易界面，压在我们的对话上面。
        event.setCancelled(true);
        if (event.getPlayer().isSneaking()) {
            return;
        }
        plugin.npcs().talk(event.getPlayer(), id);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.clock().forget(event.getPlayer().getUniqueId());
        plugin.dialogues().forget(event.getPlayer().getUniqueId());
    }

    /**
     * 第一次进城：如果人站在城墙外很远的地方，把他放到南门里。
     *
     * <p>为什么需要这个：世界可能比城先存在。玩家早就进过这张图，存档里记着他当时的位置——
     * 那多半是一片空地。之后再建城，他登录还是在空地上，于是"进服什么都没看见"。
     *
     * <p>只做一次：走远是玩家的自由，第二次登录还把人拽回来就是骚扰了。
     * {@code city.pull-strays=false} 可以整个关掉。
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("city.pull-strays", true)) {
            return;
        }
        org.bukkit.entity.Player player = event.getPlayer();
        org.bukkit.World city = RegionService.resolve(plugin.getConfig().getString("city.world", "world"));
        if (city == null || !city.equals(player.getWorld())) {
            return;
        }
        org.bukkit.NamespacedKey seen = new org.bukkit.NamespacedKey(plugin, "city_seen");
        if (player.getPersistentDataContainer().has(seen, org.bukkit.persistence.PersistentDataType.BYTE)) {
            return;
        }
        int[] origin = CityBuilder.originFrom(plugin);
        double dx = player.getLocation().getX() - origin[0];
        double dz = player.getLocation().getZ() - origin[1];
        boolean inside = Math.abs(dx) <= io.github.vampireachao.vespera.core.CityPlan.HALF
                && Math.abs(dz) <= io.github.vampireachao.vespera.core.CityPlan.HALF;
        player.getPersistentDataContainer().set(seen, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
        if (inside) {
            return;
        }
        int[] spawn = io.github.vampireachao.vespera.core.CityPlan.spawn();
        int y = city.getHighestBlockYAt(origin[0] + spawn[0], origin[1] + spawn[1]) + 1;
        player.teleport(new org.bukkit.Location(city, origin[0] + spawn[0] + 0.5, y, origin[1] + spawn[1] + 0.5));
        player.sendMessage(net.kyori.adventure.text.Component
                .text("你在城外很远的地方登录过，先把你放回南门。")
                .color(net.kyori.adventure.text.format.NamedTextColor.GRAY));
    }
}
