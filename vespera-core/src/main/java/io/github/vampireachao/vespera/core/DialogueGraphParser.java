package io.github.vampireachao.vespera.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 把一份 YAML 映射读成 {@link DialogueGraph}。
 *
 * <p>刻意只吃 {@code Map}，不吃文件：读文件是 Bukkit 的事，解析是这里的事。分开之后，
 * 这一层可以在没有服务器的机器上跑单元测试——剧本的正确性不该依赖能不能启动一个 Paper。
 *
 * <p>格式见 {@code vespera-plugin/src/main/resources/dialogues/bell-test.yml}。
 */
public final class DialogueGraphParser {

    private DialogueGraphParser() {
    }

    public static DialogueGraph parse(String fallbackId, Map<String, ?> root) {
        if (root == null) {
            throw new IllegalArgumentException("对话 " + fallbackId + " 内容是空的");
        }
        String id = text(root.get("id"), fallbackId);
        String speaker = text(root.get("speaker"), null);

        Map<String, ?> rawNodes = asMap(root.get("nodes"), "nodes");
        if (rawNodes == null || rawNodes.isEmpty()) {
            throw new IllegalArgumentException("对话 " + id + " 没有 nodes");
        }

        Map<String, DialogueNode> nodes = new LinkedHashMap<>();
        for (Map.Entry<String, ?> entry : rawNodes.entrySet()) {
            String nodeId = entry.getKey();
            Map<String, ?> body = asMap(entry.getValue(), "nodes." + nodeId);
            if (body == null) {
                throw new IllegalArgumentException("节点 " + nodeId + " 是空的");
            }
            nodes.put(nodeId, new DialogueNode(
                    nodeId,
                    text(body.get("speaker"), null),
                    lines(body.get("lines"), nodeId),
                    options(body.get("options"), nodeId)));
        }
        return new DialogueGraph(id, speaker, text(root.get("entry"), null), nodes, text(root.get("gate"), null));
    }

    private static List<String> lines(Object raw, String nodeId) {
        if (raw == null) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        if (raw instanceof Collection<?> list) {
            for (Object each : list) {
                if (each != null) {
                    lines.add(String.valueOf(each));
                }
            }
        } else {
            lines.add(String.valueOf(raw));
        }
        if (lines.isEmpty() && !nodeId.isEmpty()) {
            throw new IllegalArgumentException("节点 " + nodeId + " 的 lines 是空的");
        }
        return lines;
    }

    private static List<DialogueOption> options(Object raw, String nodeId) {
        if (raw == null) {
            return List.of();
        }
        if (!(raw instanceof Collection<?> list)) {
            throw new IllegalArgumentException("节点 " + nodeId + " 的 options 必须是列表");
        }
        List<DialogueOption> options = new ArrayList<>();
        for (Object each : list) {
            Map<String, ?> body = asMap(each, "options");
            if (body == null) {
                throw new IllegalArgumentException("节点 " + nodeId + " 有一个空选项");
            }
            options.add(new DialogueOption(
                    text(body.get("label"), null),
                    text(body.get("tooltip"), null),
                    text(body.get("goto"), null),
                    text(body.get("tell"), null),
                    truth(body.get("close")),
                    counters(body.get("set"), nodeId),
                    flags(body.get("flags")),
                    Condition.parse(body.get("when")),
                    text(body.get("gate"), null)));
        }
        return options;
    }

    private static Map<String, Integer> counters(Object raw, String nodeId) {
        Map<String, ?> map = asMap(raw, "set");
        if (map == null || map.isEmpty()) {
            return Map.of();
        }
        Map<String, Integer> values = new LinkedHashMap<>();
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (!(value instanceof Number number)) {
                throw new IllegalArgumentException(
                        "节点 " + nodeId + " 的 set." + entry.getKey() + " 必须是整数，收到 " + value);
            }
            values.put(entry.getKey(), number.intValue());
        }
        return values;
    }

    private static Set<String> flags(Object raw) {
        if (raw == null) {
            return Set.of();
        }
        Set<String> flags = new LinkedHashSet<>();
        if (raw instanceof Collection<?> list) {
            for (Object each : list) {
                flags.add(String.valueOf(each));
            }
        } else {
            flags.add(String.valueOf(raw));
        }
        return flags;
    }

    private static String text(Object raw, String fallback) {
        if (raw == null) {
            return fallback;
        }
        String value = String.valueOf(raw).trim();
        return value.isEmpty() ? fallback : value;
    }

    private static boolean truth(Object raw) {
        if (raw instanceof Boolean value) {
            return value;
        }
        return raw != null && Boolean.parseBoolean(String.valueOf(raw).trim());
    }

    private static Map<String, ?> asMap(Object raw, String where) {
        if (raw == null) {
            return null;
        }
        if (!(raw instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(where + " 必须是映射，收到 " + raw.getClass());
        }
        Map<String, Object> typed = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            typed.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return typed;
    }
}
