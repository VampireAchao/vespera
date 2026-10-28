package io.github.vampireachao.vespera.core;

import java.util.List;

/**
 * 对话图上的一个节点：一段话，加它下面能点的几个选项。
 *
 * <p>{@code speaker} 为 null 表示沿用整张图的说话人——同一个 NPC 说十段话，
 * 只该在文件头上写一次名字。
 */
public record DialogueNode(String id, String speaker, List<String> lines, List<DialogueOption> options) {

    public DialogueNode {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("节点必须有 id");
        }
        lines = lines == null ? List.of() : List.copyOf(lines);
        options = options == null ? List.of() : List.copyOf(options);
        if (lines.isEmpty() && options.isEmpty()) {
            throw new IllegalArgumentException("节点 " + id + " 既没有说话也没有选项，是一处死路");
        }
    }

    /** 这个节点谈完之后去哪：全部选项都指向 null，就是这条线的尽头。 */
    public boolean terminal() {
        return options.stream().allMatch(option -> option.target() == null);
    }
}
