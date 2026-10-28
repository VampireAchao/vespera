package io.github.vampireachao.vespera.plugin;

import io.github.vampireachao.vespera.core.DialogueGraph;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * {@code /vespera}：一个人的运维台。
 *
 * <p>开发期真正会被反复敲的只有 {@code doctor}。它存在的理由是：这套系统的失败方式
 * 大多数是"看起来没事"——剧本少一个跳转、种族插件没加载、报时的世界改名了，服务器
 * 照样启动，玩家照样能走，只是某一句台词永远不会出现。doctor 把这些沉默的错一次说完。
 */
public final class VesperaCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS =
            List.of("doctor", "where", "talk", "dialogues", "state", "clock", "npc", "build", "verify",
                    "reload");

    private final VesperaPlugin plugin;

    public VesperaCommand(VesperaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            String[] args) {
        String sub = args.length == 0 ? "doctor" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "doctor" -> doctor(sender);
            case "where" -> where(sender);
            case "talk" -> talk(sender, args);
            case "dialogues" -> dialogues(sender);
            case "state" -> state(sender, args);
            case "clock" -> sender.sendMessage(Component.text(plugin.clock().describe()));
            case "npc" -> {
                if (!sender.hasPermission("vespera.admin")) {
                    deny(sender);
                    return true;
                }
                plugin.npcs().spawnAll();
                sender.sendMessage(Component.text("锚点已重立：" + plugin.npcs().count() + " 个。"));
            }
            case "build" -> {
                if (!sender.hasPermission("vespera.admin")) {
                    deny(sender);
                    return true;
                }
                build(sender, args.length > 1 ? args[1] : "all");
            }
            case "verify" -> verify(sender, args.length > 1 ? args[1] : "city");
            case "reload" -> {
                if (!sender.hasPermission("vespera.admin")) {
                    deny(sender);
                    return true;
                }
                plugin.reload();
                sender.sendMessage(Component.text("已重载：" + plugin.dialogues().graphs().size() + " 张对话图。"));
            }
            default -> sender.sendMessage(Component.text("用法：/vespera <" + String.join("|", SUBCOMMANDS) + ">"));
        }
        return true;
    }

    private void doctor(CommandSender sender) {
        List<String> lines = new ArrayList<>();
        lines.add("维斯佩拉 · 自检");
        lines.add("  服务端    " + Bukkit.getVersion());
        lines.add("  Minecraft " + Bukkit.getMinecraftVersion());
        lines.add("  插件 API  " + plugin.getPluginMeta().getAPIVersion());
        lines.add("  插件版本  " + plugin.getPluginMeta().getVersion());
        lines.add("  Java      " + System.getProperty("java.version"));
        lines.add("  超自然    " + (plugin.races().available()
                ? "已连接 mmSupernaturals"
                : "未安装（种族一律按 human 处理，其余功能不受影响）"));
        lines.add("  剧本      " + plugin.dialogues().graphs().size() + " 张，"
                + plugin.dialogues().graphs().stream().mapToInt(DialogueGraph::lineCount).sum() + " 行");
        int problems = plugin.dialogues().graphs().stream().mapToInt(graph -> graph.problems().size()).sum();
        lines.add("  剧本问题  " + (problems == 0 ? "无" : problems + " 处（详见启动日志）"));
        List<String> renderProblems = plugin.dialogues().lint();
        lines.add("  界面构建  " + (renderProblems.isEmpty()
                ? "全部节点可渲染"
                : renderProblems.size() + " 处失败：" + renderProblems.get(0)));
        lines.add("  NPC 锚点  " + plugin.npcs().count() + " 个");
        lines.add("  区域      " + (plugin.regions().book().isEmpty()
                ? "未载入（server/vespera/regions/regions.yml）"
                : plugin.regions().book().all().size() + " 块 · 门规则 " + plugin.regions().portals().size() + " 条"));
        lines.add("  时钟      " + plugin.clock().describe());
        lines.add("  软依赖    " + softDependencies());
        lines.add("  内容闸门  " + plugin.policy().describe());
        if (sender instanceof Player player) {
            lines.add("  你        " + plugin.races().race(player)
                    + "，血势 " + plugin.races().power(player)
                    + "，super=" + plugin.races().superNatural(player));
        }
        for (String line : lines) {
            sender.sendMessage(Component.text(line).color(NamedTextColor.GRAY));
        }
    }

    private String softDependencies() {
        StringBuilder text = new StringBuilder();
        for (String name : List.of("mmSupernaturals", "Citizens", "WorldEdit", "WorldGuard", "LuckPerms")) {
            Plugin present = Bukkit.getPluginManager().getPlugin(name);
            if (text.length() > 0) {
                text.append(" · ");
            }
            text.append(present == null ? "—" : name);
        }
        return text.toString();
    }

    /**
     * {@code /vespera where}：我在哪一块地方。
     *
     * <p>看起来是个调试命令，实际上是建造时最常用的一个：站在现场，一行就知道这块地
     * 在区域表里叫什么、绑的是哪个群系、带哪些标签。地图组没人愿意翻 YAML 找坐标。
     */
    private void where(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("只有站在世界里的人才有「在哪儿」。"));
            return;
        }
        player.sendMessage(Component.text(plugin.regions().describe(player.getLocation()))
                .color(NamedTextColor.GRAY));
    }

    /**
     * {@code /vespera build [city|campus|all]}：重建灰盒。
     *
     * <p>改了平面数据之后用它，比"删世界重来"体面得多。它会先把体量清空再盖，
     * 所以反复执行是安全的。
     */
    private void build(CommandSender sender, String target) {
        long started = System.currentTimeMillis();
        int total = 0;
        boolean city = target.equals("all") || target.equals("city");
        boolean campus = target.equals("all") || target.equals("campus");
        if (!city && !campus) {
            sender.sendMessage(Component.text("用法：/vespera build [city|campus|all]"));
            return;
        }
        if (city) {
            org.bukkit.World world = RegionService.resolve(plugin.getConfig().getString("city.world", "world"));
            if (world == null) {
                sender.sendMessage(Component.text("主城所在的世界不存在。"));
                return;
            }
            total += plugin.city().build(world, plugin.cityX(), plugin.cityZ());
        }
        if (campus) {
            org.bukkit.World world = RegionService.resolve(plugin.getConfig().getString("campus.world", "world"));
            if (world == null) {
                sender.sendMessage(Component.text("校区所在的世界不存在。"));
                return;
            }
            int[] origin = CampusBuilder.originFrom(plugin);
            total += plugin.campus().build(world, origin[0], origin[1]);
        }
        sender.sendMessage(Component.text("灰盒已重建：" + total + " 个方块，"
                + (System.currentTimeMillis() - started) + "ms。"));
    }

    /**
     * {@code /vespera verify [city|campus]}：抽查灰盒是否真的在那儿。
     *
     * <p>八处探针，每处问一句"这块该是石砖吧"。验不出好看，但验得出"根本没盖"
     * 和"盖到别处去了"——这两种情况光看日志是看不出来的。
     */
    private void verify(CommandSender sender, String target) {
        List<String> failures = new java.util.ArrayList<>();
        if (!target.equals("campus")) {
            org.bukkit.World world = RegionService.resolve(plugin.getConfig().getString("city.world", "world"));
            if (world == null) {
                sender.sendMessage(Component.text("找不到主城世界。"));
                return;
            }
            failures.addAll(plugin.city().verify(world, plugin.cityX(), plugin.cityZ()));
        }
        if (!target.equals("city")) {
            org.bukkit.World world = RegionService.resolve(plugin.getConfig().getString("campus.world", "world"));
            if (world == null) {
                sender.sendMessage(Component.text("找不到校区世界。"));
                return;
            }
            int[] origin = CampusBuilder.originFrom(plugin);
            failures.addAll(plugin.campus().verify(world, origin[0], origin[1]));
        }
        if (failures.isEmpty()) {
            sender.sendMessage(Component.text("灰盒校验：全部探针通过。"));
            return;
        }
        sender.sendMessage(Component.text("灰盒校验：" + failures.size() + " 处不符："));
        failures.forEach(failure -> sender.sendMessage(Component.text("  " + failure).color(NamedTextColor.GRAY)));
    }

    private void talk(CommandSender sender, String[] args) {        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("只有玩家能开口说话。"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(Component.text("用法：/vespera talk <对话 id>"));
            return;
        }
        if (!plugin.dialogues().open(player, args[1])) {
            player.sendMessage(Component.text("没有这张对话图：" + args[1]));
        }
    }

    private void dialogues(CommandSender sender) {
        for (DialogueGraph graph : plugin.dialogues().graphs()) {
            sender.sendMessage(Component.text("  " + graph.id() + "　" + graph.speaker()
                    + "　" + graph.nodes().size() + " 节点 / " + graph.lineCount() + " 行")
                    .color(NamedTextColor.GRAY));
        }
    }

    private void state(CommandSender sender, String[] args) {
        Player target;
        if (args.length >= 2) {
            if (!sender.hasPermission("vespera.admin")) {
                deny(sender);
                return;
            }
            target = Bukkit.getPlayerExact(args[1]);
        } else {
            target = sender instanceof Player player ? player : null;
        }
        if (target == null) {
            sender.sendMessage(Component.text("找不到这个玩家。"));
            return;
        }
        List<String> dump = plugin.state().dump(target);
        sender.sendMessage(Component.text(target.getName() + " 身上记着 " + dump.size() + " 条："));
        dump.forEach(line -> sender.sendMessage(Component.text("  " + line).color(NamedTextColor.GRAY)));
    }

    private void deny(CommandSender sender) {
        sender.sendMessage(Component.text("这事得管理员来。"));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            String[] args) {
        if (args.length == 1) {
            return SUBCOMMANDS.stream().filter(sub -> sub.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("talk")) {
            return plugin.dialogues().graphs().stream().map(DialogueGraph::id).toList();
        }
        return List.of();
    }
}
