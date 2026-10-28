package io.github.vampireachao.vespera.plugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
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
    }
}
