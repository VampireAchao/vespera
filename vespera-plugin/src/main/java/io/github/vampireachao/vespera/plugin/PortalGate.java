package io.github.vampireachao.vespera.plugin;

import io.github.vampireachao.vespera.core.Region;
import java.util.Optional;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

/**
 * 下界门的定向：从主城走进门，落到熔炉之喉的入口平台。
 *
 * <p>为什么需要这个：原版下界门只按 1:8 换算坐标，不认"这是哪一扇门"。所以要实现
 * "走特定传送门能到熔炉之喉，自己搭门也能在荒野里探索"，就必须在传送发生的那一刻
 * 改写落点——不是拦住玩家，而是替他决定同一扇门通向哪儿。
 *
 * <p>只处理 {@link TeleportCause#NETHER_PORTAL}：末地折跃门是原版机制，落点由种子决定，
 * 那边不需要我们插手（珍珠荒原在外岛，本来就是要玩家自己走过去）。
 */
public final class PortalGate implements Listener {

    private final RegionService regions;

    public PortalGate(RegionService regions) {
        this.regions = regions;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPortal(PlayerPortalEvent event) {
        if (event.getCause() != TeleportCause.NETHER_PORTAL) {
            return;
        }
        Location from = event.getFrom();
        if (from == null || from.getWorld() == null) {
            return;
        }
        Optional<Region> region = regions.at(from);
        for (RegionService.PortalRule rule : regions.portals()) {
            if (!rule.matches(from, region.orElse(null))) {
                continue;
            }
            Location target = rule.target();
            if (target == null) {
                // 目标维度还没加载：这次按原版走，不拦。
                return;
            }
            event.setTo(target);
            // 不要顺手在对面的落点再生成一扇新门——那会让"入口平台"长出一堆门。
            event.setCanCreatePortal(false);
            return;
        }
    }
}
