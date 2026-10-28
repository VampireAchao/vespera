package io.github.vampireachao.vespera.plugin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataType;

/**
 * NPC 锚点：一个站着不动、有名字、能右键说话的人。
 *
 * <p>这是本方案里最容易被低估的一块。它只做一件事，但把这件事做对，能让整个项目
 * 不依赖任何第三方 NPC 插件就立起来：
 *
 * <ul>
 *   <li><b>没有客户端模组。</b>它就是一个原版村民。玩家不需要装任何东西。</li>
 *   <li><b>没有前置依赖。</b>缺 Citizens 也能跑，所以"一键起服"永远能成功。</li>
 *   <li><b>身份写在实体身上。</b>PDC 里存 {@code vespera:npc = bell-monk}，
 *       重启、换区块、被 /kill 之后都能认回来，不靠坐标记忆。</li>
 * </ul>
 *
 * <p>它的上限也很清楚：没有皮肤、不会走、不会看人。这些正是 Citizens 的价值，
 * 所以升级路径是"锚点还在，渲染换成 Citizens"——{@code npcId} 这个身份不变，
 * 换的是站在那个坐标上的东西。见 {@code docs/02-NPC与地图.md}。
 */
public final class NpcService {

    private final VesperaPlugin plugin;
    private final NamespacedKey key;
    private final Map<String, String> dialogueOf = new LinkedHashMap<>();
    private int spawned;

    public NpcService(VesperaPlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "npc");
    }

    public void spawnAll() {
        clear();
        List<Map<?, ?>> entries = plugin.getConfig().getMapList("npcs");
        for (Map<?, ?> entry : entries) {
            // 没写 enabled 就算开：配置里少一个键，不该让一个 NPC 消失。
            Object enabled = entry.get("enabled");
            if (enabled != null && !Boolean.parseBoolean(String.valueOf(enabled))) {
                continue;
            }
            try {
                spawn(entry);
            } catch (RuntimeException e) {
                plugin.getLogger().warning("NPC 锚点 " + entry.get("id") + " 没立起来：" + e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void spawn(Map<?, ?> entry) {
        String id = text(entry.get("id"));
        String name = text(entry.get("name"));
        // 用标准名解析世界：平世界叫 campus 时，配置里写的 world 也要能找到。
        World world = RegionService.resolve(text(entry.get("world")));
        if (id == null || world == null) {
            plugin.getLogger().warning("NPC 锚点缺少 id 或世界不存在：" + entry);
            return;
        }
        Location where = location(world, text(entry.get("at")), text(entry.get("offset")));
        world.spawn(where, Villager.class, villager -> {
            villager.customName(Component.text(name == null ? id : name));
            villager.setCustomNameVisible(true);
            villager.setSilent(true);
            villager.setInvulnerable(true);
            villager.setPersistent(true);
            villager.setRemoveWhenFarAway(false);
            // 不 tick AI：站着不动的 NPC 不该占算力，也不该被僵尸推着走。
            villager.setAware(false);
            villager.setCollidable(false);
            villager.getPersistentDataContainer().set(key, PersistentDataType.STRING, id);
        });
        dialogueOf.put(id, text(entry.get("dialogue")));
        spawned++;
    }

    /** 把所有锚点撤掉。只认自己打的标记，不动玩家养的村民。 */
    public void clear() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                    entity.remove();
                }
            }
        }
        dialogueOf.clear();
        spawned = 0;
    }

    /** 这个实体是不是锚点；是的话返回它的 id。 */
    public String npcId(Entity entity) {
        if (entity == null) {
            return null;
        }
        return entity.getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    public String dialogueOf(String npcId) {
        return dialogueOf.get(npcId);
    }

    public int count() {
        return spawned;
    }

    /**
     * {@code at} 支持三种写法：
     * {@code world-spawn}（跟着世界出生点走）、{@code campus-gate}（站在校门外，跟着校区走）、
     * 或一串坐标 {@code "8.5 65 8.5"}。{@code offset} 是相对位移。
     *
     * <p>{@code campus-gate} 是后加的：校区建在远离出生点的地方，写死坐标的话，
     * 一改校区原点 NPC 就跑到野地里去了。
     */
    private Location location(World world, String at, String offset) {
        Location base;
        if (at == null || at.equals("world-spawn")) {
            base = world.getSpawnLocation();
        } else if (at.equals("campus-gate")) {
            int ox = plugin.getConfig().getInt("campus.origin-x", 370);
            int oz = plugin.getConfig().getInt("campus.origin-z", 0)
                    + io.github.vampireachao.vespera.core.CampusPlan.WALL_MIN_Z - 6;
            base = new Location(world, ox + 0.5, world.getHighestBlockYAt(ox, oz) + 1, oz + 0.5);
        } else {
            String[] parts = at.trim().split("\\s+");
            base = new Location(world,
                    Double.parseDouble(parts[0]),
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]));
        }
        if (offset != null) {
            String[] parts = offset.trim().split("\\s+");
            base = base.clone().add(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]));
        }
        // 让锚点站在方块上面，而不是陷进去半格。
        return base.getBlock().getLocation().add(0.5, 0, 0.5);
    }

    private static String text(Object raw) {
        if (raw == null) {
            return null;
        }
        String value = String.valueOf(raw).trim();
        return value.isEmpty() ? null : value;
    }

    /** 让某个玩家跟锚点说话。听众由事件决定，不由坐标决定。 */
    public void talk(Player player, String npcId) {
        String dialogue = dialogueOf.get(npcId);
        if (dialogue != null) {
            plugin.dialogues().open(player, dialogue);
        }
    }
}
