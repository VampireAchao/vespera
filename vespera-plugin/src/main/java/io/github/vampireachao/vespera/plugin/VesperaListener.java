package io.github.vampireachao.vespera.plugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
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
        // 先给东西再说话：道具挂在"第一次见面"上，不挂在剧本的某个选项上——
        // 那样玩家读完就关，东西永远拿不到
        plugin.quest().onTalk(event.getPlayer(), id);
        plugin.npcs().talk(event.getPlayer(), id);
    }

    /**
     * 右键箱子/讲经台/座椅：圣堂里的四处服务点。
     *
     * <p>按坐标认，不按"箱子就是仓库"认——全城有上千只箱子。认错了的表现是
     * "随手开个箱子，多了三十个面包"，比点了没反应更难查。
     */
    @EventHandler
    public void onInteractBlock(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        if (plugin.quest().click(event.getPlayer(), event.getClickedBlock())) {
            // 认领了就把事件拦下来：别让箱子真的打开、别让讲经台翻页
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.clock().forget(event.getPlayer().getUniqueId());
        plugin.dialogues().forget(event.getPlayer().getUniqueId());
    }

    /**
     * 进服：给名单里的人 OP；第一次登录若在城墙外很远，把人放回南门。
     *
     * <p>为什么需要后者：世界可能比城先存在，玩家存档里记着他当时的位置——那多半是
     * 一片空地。之后再建城，他登录还是在空地上，于是"进服什么都没看见"。
     *
     * <p>只做一次：走远是玩家的自由，第二次登录还把人拽回来就是骚扰了。
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        org.bukkit.entity.Player player = event.getPlayer();
        if (plugin.getConfig().getStringList("admins").contains(player.getName()) && !player.isOp()) {
            player.setOp(true);
            player.sendMessage(net.kyori.adventure.text.Component
                    .text("已把你加为管理员（OP）。")
                    .color(net.kyori.adventure.text.format.NamedTextColor.GRAY));
        }
        // 出生点落在方块里时把人挪出来。这种事只该发生一次，但不该让人自己去挖。
        plugin.city().unstick(player);
        if (!plugin.getConfig().getBoolean("city.pull-strays", true)) {
            return;
        }
        org.bukkit.World city = RegionService.resolve(plugin.getConfig().getString("city.world", "world"));
        if (city == null) {
            return;
        }
        plugin.city().pullStray(player, city, plugin.cityX(), plugin.cityZ());
    }
}
