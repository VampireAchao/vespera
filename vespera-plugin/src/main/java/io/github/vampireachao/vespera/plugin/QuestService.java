package io.github.vampireachao.vespera.plugin;

import io.github.vampireachao.vespera.core.world.TempleSpots;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * 朝圣备礼：转职要的四样东西，在圣堂里各有各的来路。
 *
 * <p>这一段不判定"能不能转职"——那是 mmSupernaturals 的事，它只看玩家背包。
 * 这里只做两件：<b>给东西</b>与<b>报进度</b>。所以它永远是"引导"，不是"门禁"：
 * 玩家完全可以不理这些人，自己去挖矿、去下界，凑齐了照样转职。
 *
 * <p>四样东西的出处（与 {@link TempleSpots} 一一对应）：
 *
 * <table border="1">
 *   <caption>备礼来源</caption>
 *   <tr><th>东西</th><th>数量</th><th>从哪儿来</th></tr>
 *   <tr><td>面包</td><td>30</td><td>祭器库的仓库箱，领一次</td></tr>
 *   <tr><td>红石</td><td>8</td><td>地牢墙上的血石，自己挖（一块出 9 个）</td></tr>
 *   <tr><td>金锭</td><td>4</td><td>三件事：查档 1、旁听会议 1、交回 8 个红石 2</td></tr>
 *   <tr><td>荧石粉</td><td>4</td><td>教皇、藏书阁执事、议事司仪、地牢看守，一人一份</td></tr>
 * </table>
 *
 * <p>每样都只给一次，靠玩家身上的旗标记；重复右键只会得到一句"你已经领过了"。
 * 不记数量、不记进度条——背包才是权威，多一套账本只会多一处对不上的地方。
 */
public final class QuestService {

    /** 转职配方，与上游 {@code Priest.Church.Recipe} 的默认值一致。 */
    public static final int NEED_BREAD = 30;
    public static final int NEED_REDSTONE = 8;
    public static final int NEED_GOLD = 4;
    public static final int NEED_GLOWSTONE = 4;

    private final VesperaPlugin plugin;

    public QuestService(VesperaPlugin plugin) {
        this.plugin = plugin;
    }

    // -- 物件 ---------------------------------------------------------------

    /**
     * 玩家点了一个方块：是不是我们认识的那几件东西。
     *
     * @return 认领了这次点击就返回 true（调用方要 cancel，别让箱子真的打开）
     */
    public boolean click(Player player, Block block) {
        TempleSpots.Marker marker = markerAt(block);
        if (marker == null) {
            return false;
        }
        switch (marker.id()) {
            case "warehouse" -> grant(player, "pil_bread", new ItemStack(Material.BREAD, NEED_BREAD),
                    "仓库执事从架上搬下一只麻袋：三十个面包。", "面包你已经领过了。");
            case "dungeon" -> grant(player, "pil_kit",
                    new ItemStack(Material.WOODEN_PICKAXE),
                    "看守把一把木镐和几支火把推过来：「墙上的血石，自己凿。」",
                    "工具你已经拿过了。");
            case "library" -> {
                if (!grantOnce(player, "pil_book")) {
                    player.sendMessage(text("你已经查过那一段了。"));
                    return true;
                }
                grant(player, "pil_book", new ItemStack(Material.GOLD_INGOT),
                        "你翻到一页讲「祝圣」的旧记载。执事看过，付了你一锭金。", "");
            }
            case "council" -> {
                if (!grantOnce(player, "pil_council")) {
                    player.sendMessage(text("会已经散了。"));
                    return true;
                }
                grant(player, "pil_council", new ItemStack(Material.GOLD_INGOT),
                        "你在末座听完了整场议事。司仪把一锭金放进你手里。", "");
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    private void grant(Player player, String flag, ItemStack item, String first, String again) {
        if (!grantOnce(player, flag)) {
            if (!again.isEmpty()) {
                player.sendMessage(text(again));
            }
            return;
        }
        give(player, item);
        player.sendMessage(text(first));
        progress(player);
    }

    /** 第一次做这件事返回 true，之后一直 false。 */
    private boolean grantOnce(Player player, String flag) {
        if (plugin.state().flag(player, flag)) {
            return false;
        }
        plugin.state().setFlag(player, flag, true);
        return true;
    }

    private void give(Player player, ItemStack item) {
        if (!player.getInventory().addItem(item).isEmpty()) {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        }
    }

    // -- 人 -----------------------------------------------------------------

    /**
     * 玩家跟一个锚点说话。第一句之前先把该给的给了——道具永远附在第一次见面，
     * 不挂在剧本的某个选项上：那样玩家可以读完就关，东西永远拿不到。
     */
    public void onTalk(Player player, String npcId) {
        switch (npcId) {
            case "temple-pope" -> firstGift(player, "pil_g_pope",
                    "教皇在你掌心放下一撮荧石粉：「祭坛要的不是金子，是光。」");
            case "temple-librarian" -> firstGift(player, "pil_g_lib",
                    "执事从抽屉里取出一点荧石粉：「抄经的人手上总会剩这个。」");
            case "temple-herald" -> firstGift(player, "pil_g_herald",
                    "司仪把荧石粉倒进你手里：「灯是要有人添的。」");
            case "temple-warden" -> {
                firstGift(player, "pil_g_warden",
                        "看守掂了掂一小袋荧石粉：「地底下待久了，这个比钱好用。」");
                // 交回红石：只认背包，不记账——挖多挖少都是他自己的事
                if (!plugin.state().flag(player, "pil_red")
                        && count(player, Material.REDSTONE) >= NEED_REDSTONE) {
                    plugin.state().setFlag(player, "pil_red", true);
                    give(player, new ItemStack(Material.GOLD_INGOT, 2));
                    player.sendMessage(text("看守数过那八颗红石，点了点头，给了你两锭金。"));
                    progress(player);
                }
            }
            default -> {
            }
        }
    }

    private void firstGift(Player player, String flag, String line) {
        if (!grantOnce(player, flag)) {
            return;
        }
        give(player, new ItemStack(Material.GLOWSTONE_DUST));
        player.sendMessage(text(line));
        player.sendMessage(text("（荧石粉 1 份。备礼共需 4 份，分别来自四个人。）"));
    }

    // -- 进度 ---------------------------------------------------------------

    /** 还差什么。玩家问"我要带什么去"时，答案就在这儿。 */
    public List<String> status(Player player) {
        List<String> lines = new ArrayList<>();
        lines.add("朝圣备礼（转职要带的四样）");
        lines.add(row("面包", count(player, Material.BREAD), NEED_BREAD));
        lines.add(row("红石", count(player, Material.REDSTONE), NEED_REDSTONE));
        lines.add(row("金锭", count(player, Material.GOLD_INGOT), NEED_GOLD));
        lines.add(row("荧石粉", count(player, Material.GLOWSTONE_DUST), NEED_GLOWSTONE));
        lines.add("  来源：仓库领面包｜地牢挖血石｜查档·旁听·交红石换金锭｜四人各给一份荧石粉");
        lines.add("  也可以不进圣堂，自己挖、自己去下界凑——那扇门不查你来路。");
        return lines;
    }

    private static String row(String label, int have, int need) {
        String mark = have >= need ? "✓" : "·";
        return String.format(Locale.ROOT, "  %s %s %d/%d", mark, label, Math.min(have, need), need);
    }

    /** 一次性把四样念给玩家听（站在祭坛上、材料不够时最有用）。 */
    public void progress(Player player) {
        List<String> lines = status(player);
        player.sendMessage(text(lines.get(0)));
        for (int i = 1; i < lines.size() - 2; i++) {
            player.sendMessage(text(lines.get(i)));
        }
    }

    public static int count(Player player, Material material) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == material) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    // -- 坐标 ---------------------------------------------------------------

    /**
     * 这个方块是不是某处服务点。
     *
     * <p>反查而不是"记下位置"：城可能重建、坐标可能平移，唯一稳定的做法是每次
     * 用当前城心把场景坐标换算一次。换算错了的表现是"点了没反应"，所以
     * {@code /vespera quest} 会把换算结果打出来。
     */
    public TempleSpots.Marker markerAt(Block block) {
        World world = block.getWorld();
        Integer ground = groundOf(world);
        if (ground == null) {
            return null;
        }
        int ox = plugin.cityX();
        int oz = plugin.cityZ();
        for (TempleSpots.Marker marker : TempleSpots.MARKERS) {
            if (block.getX() == ox + marker.x()
                    && block.getY() == ground + marker.y()
                    && block.getZ() == oz + marker.z()) {
                return marker;
            }
        }
        return null;
    }

    /** 某处服务点在世界里的实际位置，给命令与自检用。 */
    public Location where(World world, TempleSpots.Marker marker) {
        Integer ground = groundOf(world);
        if (ground == null) {
            return null;
        }
        return new Location(world, plugin.cityX() + marker.x(), ground + marker.y(),
                plugin.cityZ() + marker.z());
    }

    private Integer groundOf(World world) {
        if (world == null || plugin.city() == null) {
            return null;
        }
        World city = RegionService.resolve(plugin.getConfig().getString("city.world", "world"));
        if (city == null || !city.equals(world)) {
            return null;
        }
        return plugin.city().groundLevel(city, plugin.cityX(), plugin.cityZ());
    }

    private static net.kyori.adventure.text.Component text(String line) {
        return net.kyori.adventure.text.Component.text(line)
                .color(net.kyori.adventure.text.format.NamedTextColor.GRAY);
    }
}
