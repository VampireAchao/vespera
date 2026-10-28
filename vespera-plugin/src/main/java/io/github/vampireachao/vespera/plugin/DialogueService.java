package io.github.vampireachao.vespera.plugin;

import io.github.vampireachao.vespera.core.DialogueGraph;
import io.github.vampireachao.vespera.core.DialogueGraphParser;
import io.github.vampireachao.vespera.core.DialogueNode;
import io.github.vampireachao.vespera.core.DialogueOption;
import io.github.vampireachao.vespera.core.StoryVars;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

/**
 * 对话：把一张 {@link DialogueGraph} 变成玩家屏幕上的选项框，并把选择写回世界。
 *
 * <p>用原版 Dialog 而不是箱子 GUI，理由值得写清楚，因为这决定了整个项目的交互手感：
 *
 * <ul>
 *   <li>它是真界面，不是聊天栏里的一串字。有标题、正文、按钮、悬停提示，字体和换行由客户端排版。</li>
 *   <li>不需要资源包。玩家用原版客户端就能看见——这是本项目的硬约束，不是偏好。</li>
 *   <li>按钮能绑定服务端回调（{@code DialogAction.customClick}），点下去发生什么由我们说了算，
 *       不需要玩家真的执行一条命令。</li>
 *   <li>它也能收输入：文本框、滑块、勾选、单选。要玩家"报上名字"时不用打字猜命令。</li>
 * </ul>
 *
 * <p>代价同样要说清楚：Dialog 是一块盖住屏幕的界面，玩家打开它就不能走了。所以
 * <b>边走边聊不用它</b>——那种场景用 {@code tellraw} 的可点击文本，聊天栏里的选项不挡路。
 * 两种用法各管一段，见 {@code docs/03-对话与交互.md}。
 */
public final class DialogueService {

    private final VesperaPlugin plugin;
    private final StoryState state;
    private final RaceBridge races;
    private final ContentPolicy policy;
    private final Map<String, DialogueGraph> graphs = new LinkedHashMap<>();
    private final Map<String, Style> styles = new LinkedHashMap<>();
    /** 每个玩家这一轮用哪种渲染器。开口时定下，谈完清掉。 */
    private final Map<java.util.UUID, Style> sessions = new java.util.HashMap<>();

    /**
     * 两种渲染器，同一张剧本。
     *
     * <p>这不是"两套对话系统"，是同一棵对话树的两副皮：界面（Dialog）挡路、看得清；
     * 聊天栏（tellraw）不挡路、读得快。选哪个是**场景决定**，不是技术限制。
     */
    public enum Style {
        /** 原生 Dialog 界面。用于有分量的对话、需要排版与输入的地方。 */
        DIALOG,
        /** 聊天栏可点击文本。用于路边一句话、不想打断走路的时候。 */
        CHAT;

        static Style of(Object raw) {
            return raw != null && "chat".equalsIgnoreCase(String.valueOf(raw).trim()) ? CHAT : DIALOG;
        }
    }

    public DialogueService(VesperaPlugin plugin, StoryState state, RaceBridge races, ContentPolicy policy) {
        this.plugin = plugin;
        this.state = state;
        this.races = races;
        this.policy = policy;
    }

    /** 读盘。返回加载成功的张数；有问题的图仍然加载，只是把问题记进日志。 */
    public int load() {
        graphs.clear();
        File directory = new File(plugin.getDataFolder(),
                plugin.getConfig().getString("dialogues.directory", "dialogues"));
        if (!directory.isDirectory() && !directory.mkdirs()) {
            plugin.getLogger().warning("建不出剧本目录：" + directory);
            return 0;
        }
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return 0;
        }
        for (File file : files) {
            String id = file.getName().substring(0, file.getName().length() - ".yml".length());
            try {
                Map<String, Object> plain = plainMap(YamlConfiguration.loadConfiguration(file));
                DialogueGraph graph = DialogueGraphParser.parse(id, plain);
                graphs.put(graph.id(), graph);
                styles.put(graph.id(), Style.of(plain.get("style")));
                for (String problem : graph.problems()) {
                    plugin.getLogger().warning("剧本 " + graph.id() + "：" + problem);
                }
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.WARNING, "剧本 " + id + " 读不了", e);
            }
        }
        return graphs.size();
    }

    /**
     * 把 Bukkit 的配置树摊成纯 Map / List。
     *
     * <p>必须这么做，因为 {@code YamlConfiguration#getValues(false)} 的"值"并不是
     * {@code Map}，而是 {@code MemorySection}——一个长得像 Map 但不是 Map 的东西。
     * core 那一层刻意不认识 Bukkit，所以这一步转换只能发生在这里，不能下沉。
     */
    private static Map<String, Object> plainMap(ConfigurationSection section) {
        Map<String, Object> plain = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            plain.put(key, plainValue(section.get(key)));
        }
        return plain;
    }

    private static Object plainValue(Object value) {
        if (value instanceof ConfigurationSection section) {
            return plainMap(section);
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> plain = new LinkedHashMap<>();
            map.forEach((key, each) -> plain.put(String.valueOf(key), plainValue(each)));
            return plain;
        }
        if (value instanceof List<?> list) {
            List<Object> plain = new ArrayList<>(list.size());
            list.forEach(each -> plain.add(plainValue(each)));
            return plain;
        }
        return value;
    }

    public Collection<DialogueGraph> graphs() {
        return graphs.values();
    }

    public DialogueGraph graph(String id) {
        return graphs.get(id);
    }

    /** 从入口开始谈。玩家不在线、图不存在、闸门关着，都安静返回——NPC 不该因为剧本写错而报错给玩家看。 */
    public boolean open(Player player, String graphId) {
        DialogueGraph graph = graphs.get(graphId);
        if (graph == null) {
            plugin.getLogger().warning("没有这张对话图：" + graphId);
            return false;
        }
        if (!policy.allows(graph.gate())) {
            // 不解释、不提示"这里本来有什么"。关着的线就是不发生。
            // 给一句世界里的话，是因为沉默比拒绝更让人起疑。
            player.sendMessage(Component.text("「今天没什么可说的。」").color(NamedTextColor.GRAY)
                    .decorate(TextDecoration.ITALIC));
            return false;
        }
        // 每次"开口"都重定渲染器：同一个人上一段是聊天栏，这一段可能是界面。
        // 用 put 而不是 computeIfAbsent——沿用上一段的皮肤，是这里最容易犯的错。
        sessions.put(player.getUniqueId(), styleOf(graph.id()));
        openNode(player, graph, graph.entryId());
        return true;
    }

    /** 这个玩家这一轮会看到哪种界面。外部（比如右键 NPC）想知道时问这里。 */
    public Style styleOf(String graphId) {
        return styles.getOrDefault(graphId, Style.DIALOG);
    }

    public void openNode(Player player, DialogueGraph graph, String nodeId) {
        DialogueNode node = graph.node(nodeId);
        if (node == null) {
            plugin.getLogger().warning("剧本 " + graph.id() + " 没有节点 " + nodeId);
            return;
        }
        Style style = sessions.computeIfAbsent(player.getUniqueId(), key -> styleOf(graph.id()));
        if (style == Style.CHAT) {
            renderChat(player, graph, node);
        } else {
            player.showDialog(build(state.view(player, races), graph, node));
        }
    }

    /**
     * 聊天栏渲染：一段话，加一行可点的方括号。
     *
     * <p>三条刻意的选择：
     *
     * <ul>
     *   <li>台词逐行发，不做成一段——聊天栏换行难对齐，一行一句反而像说话。</li>
     *   <li>选项用方括号，不用"请回复 1 2 3"。玩家不需要记数字，也不需要打字。</li>
     *   <li>点完不重绘历史，只续上新的一句。聊天记录本身就是演出的时间轴。</li>
     * </ul>
     */
    private void renderChat(Player player, DialogueGraph graph, DialogueNode node) {
        String speaker = node.speaker() == null ? graph.speaker() : node.speaker();
        player.sendMessage(Component.text("　" + speaker).color(NamedTextColor.DARK_GRAY));
        for (String line : node.lines()) {
            player.sendMessage(Component.text(line).color(NamedTextColor.GRAY));
        }
        List<DialogueOption> options = graph.visible(node, state.view(player, races)).stream()
                .filter(option -> policy.allows(option.gate()))
                .toList();
        if (options.isEmpty()) {
            sessions.remove(player.getUniqueId());
            return;
        }
        Component row = Component.text("　");
        for (DialogueOption option : options) {
            row = row.append(Component.text("[" + option.label() + "]")
                    .color(NamedTextColor.AQUA)
                    .clickEvent(net.kyori.adventure.text.event.ClickEvent.callback(audience -> {
                        if (audience instanceof Player clicker) {
                            choose(clicker, graph, option);
                        }
                    }))
                    .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                            Component.text(option.tooltip() == null ? "点一下" : option.tooltip())
                                    .color(NamedTextColor.DARK_GRAY))))
                    .append(Component.space());
        }
        player.sendMessage(row);
    }

    /**
     * 把每一张图的每一个节点都试着构造一遍，只构造、不展示。
     *
     * <p>这是这套系统里唯一一处"不用客户端也能验对话"的办法。它挡不住排版难看，
     * 但挡得住真正致命的那一类：某个节点少写了说话人、按钮宽度非法、动作绑定失败——
     * 这些错误如果只在玩家右键的那一刻才暴露，就等于没有暴露。
     */
    public List<String> lint() {
        List<String> problems = new ArrayList<>();
        for (DialogueGraph graph : graphs.values()) {
            for (DialogueNode node : graph.nodes().values()) {
                try {
                    build(StoryVars.empty(), graph, node);
                } catch (RuntimeException e) {
                    problems.add(graph.id() + "/" + node.id() + " 构不出界面：" + e);
                }
            }
        }
        return problems;
    }

    private Dialog build(StoryVars vars, DialogueGraph graph, DialogueNode node) {
        List<DialogBody> body = new ArrayList<>();
        for (String line : node.lines()) {
            body.add(DialogBody.plainMessage(Component.text(line).color(NamedTextColor.GRAY)));
        }

        List<ActionButton> buttons = new ArrayList<>();
        for (DialogueOption option : graph.visible(node, vars)) {
            // 闸门在渲染前判：关着的选项根本不进界面。留一个灰按钮说"未解锁"，
            // 等于在告诉玩家"这里有东西"，那正是最不该做的事。
            if (!policy.allows(option.gate())) {
                continue;
            }
            ActionButton.Builder button = ActionButton.builder(Component.text(option.label())).width(220);
            if (option.tooltip() != null) {
                button = button.tooltip(Component.text(option.tooltip()).color(NamedTextColor.DARK_GRAY));
            }
            buttons.add(button.action(DialogAction.customClick(
                    (response, audience) -> {
                        if (audience instanceof Player clicker) {
                            choose(clicker, graph, option);
                        }
                    },
                    ClickCallback.Options.builder().build())).build());
        }

        DialogBase base = DialogBase.builder(Component.text(node.speaker() == null ? graph.speaker() : node.speaker()))
                .body(body)
                .canCloseWithEscape(true)
                // 不暂停：世界该走的还在走。对话框是界面，不是时停。
                .pause(false)
                .build();

        DialogType type = buttons.isEmpty() ? DialogType.notice() : DialogType.multiAction(buttons).columns(1).build();
        return Dialog.create(factory -> factory.empty().base(base).type(type));
    }

    /** 点下去之后：先记账，再说话，最后决定是翻页还是合上。 */
    private void choose(Player player, DialogueGraph graph, DialogueOption option) {
        for (String flag : option.flags()) {
            state.setFlag(player, flag, true);
        }
        option.set().forEach((key, delta) -> state.add(player, key, delta));
        if (option.tell() != null) {
            player.sendMessage(Component.text(option.tell()).color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC));
        }
        if (option.target() != null) {
            // 下一帧再开：在回调里直接换界面，客户端会先收到关闭再收到打开，容易闪一下。
            Bukkit.getScheduler().runTask(plugin, () -> openNode(player, graph, option.target()));
        } else {
            player.closeDialog();
            sessions.remove(player.getUniqueId());
        }
    }

    /** 玩家下线时清掉他的渲染器记录，免得下次开口时继承了上一轮的界面。 */
    public void forget(java.util.UUID player) {
        sessions.remove(player);
    }
}
