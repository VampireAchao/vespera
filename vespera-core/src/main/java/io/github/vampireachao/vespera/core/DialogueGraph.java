package io.github.vampireachao.vespera.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 一张对话图：一个 NPC 的一次谈话，或者一次审问，或者一封信的四个分岔。
 *
 * <p>它是一张有向图，不是一棵树。允许两个节点指向同一个节点——"不管你怎么答，
 * 老修士都会多看你一眼"，这件事用图表达是一行，用树表达是两行重复的台词。
 */
public final class DialogueGraph {

    private final String id;
    private final String speaker;
    private final String entry;
    private final String gate;
    private final Map<String, DialogueNode> nodes;

    public DialogueGraph(String id, String speaker, String entry, Map<String, DialogueNode> nodes, String gate) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("对话必须有 id");
        }
        if (nodes == null || nodes.isEmpty()) {
            throw new IllegalArgumentException("对话 " + id + " 一个节点都没有");
        }
        this.id = id;
        this.speaker = speaker == null || speaker.isBlank() ? id : speaker;
        this.nodes = Map.copyOf(new LinkedHashMap<>(nodes));
        this.entry = entry == null || entry.isBlank() ? nodes.keySet().iterator().next() : entry;
        this.gate = gate;
    }

    public String id() {
        return id;
    }

    /**
     * 整张图的内容闸门。闸门关着，这整张图在游戏里不存在——不是隐藏，是不加载给玩家。
     *
     * <p>闸门放在图这一层，是因为有些线整段都不该被表面提及（一张图就是一场戏）。
     * 只遮某一句、留个"这里本来有点什么"的空洞，是最糟的做法：它反而把注意力引过去。
     */
    public String gate() {
        return gate;
    }

    public String speaker() {
        return speaker;
    }

    public String entryId() {
        return entry;
    }

    public DialogueNode entry() {
        DialogueNode node = nodes.get(entry);
        if (node == null) {
            throw new IllegalStateException("对话 " + id + " 的入口 " + entry + " 不存在");
        }
        return node;
    }

    public DialogueNode node(String nodeId) {
        return nodes.get(nodeId);
    }

    public Map<String, DialogueNode> nodes() {
        return nodes;
    }

    /** 这个玩家此刻能看见的选项。看不见的选项不渲染，也不占位——空按钮比没有更伤人。 */
    public List<DialogueOption> visible(DialogueNode node, StoryVars vars) {
        List<DialogueOption> visible = new ArrayList<>();
        for (DialogueOption option : node.options()) {
            if (option.visible(vars)) {
                visible.add(option);
            }
        }
        return visible;
    }

    /**
     * 剧本自检：断掉的跳转、走不到的节点、没有出口的死胡同。
     *
     * <p>这些错误必须在上线前被机器发现，而不是等玩家点到一个空界面。
     * {@code /vespera doctor} 会把这份清单打出来。
     */
    public List<String> problems() {
        List<String> problems = new ArrayList<>();
        if (!nodes.containsKey(entry)) {
            problems.add("入口 " + entry + " 不存在");
        }
        List<String> reachable = new ArrayList<>();
        collect(entry, reachable);
        for (DialogueNode node : nodes.values()) {
            if (!reachable.contains(node.id())) {
                problems.add("节点 " + node.id() + " 走不到");
            }
            for (DialogueOption option : node.options()) {
                if (option.target() != null && !nodes.containsKey(option.target())) {
                    problems.add("节点 " + node.id() + " 的选项「" + option.label() + "」指向不存在的 " + option.target());
                }
            }
        }
        return problems;
    }

    private void collect(String nodeId, List<String> visited) {
        if (nodeId == null || !nodes.containsKey(nodeId) || visited.contains(nodeId)) {
            return;
        }
        visited.add(nodeId);
        for (DialogueOption option : nodes.get(nodeId).options()) {
            collect(option.target(), visited);
        }
    }

    /** 台词总量，用来对账"这一册写了多少行"。 */
    public int lineCount() {
        return nodes.values().stream().mapToInt(node -> node.lines().size()).sum();
    }
}
