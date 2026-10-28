package io.github.vampireachao.vespera.core;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 一个选项。
 *
 * <p>选项不是"分支"，是"一句会被记住的话"。因此它带的不是权重，而是三样会落到世界里的东西：
 * 一句回话（{@link #tell}）、几个旗标（{@link #flags}）、几个计数（{@link #set}）。
 *
 * <p>没有"好感 +5"这种字段。设计书的铁律是剧情不许直接改写插件数值；好感这类计数
 * 属于本项目自己的状态，写在这里是合法的，而血势那一类永远不在这里出现。
 *
 * @param label  按钮上的字。第一人称，句末带句号或不带，取决于它是不是一句台词
 * @param tooltip 悬停的一句补充，可为 null
 * @param target 跳转到的节点 id；为 null 表示谈完了
 * @param tell    选完之后立刻说出的一句，可为 null
 * @param close   选完是否关闭界面。{@code target != null} 时忽略
 * @param set     计数增量（不是重置）：{@code aff_luka: 5} 意思是加 5
 * @param flags   置位的旗标
 * @param when    这个选项什么时候出现
 */
public record DialogueOption(
        String label,
        String tooltip,
        String target,
        String tell,
        boolean close,
        Map<String, Integer> set,
        Set<String> flags,
        Condition when) {

    public DialogueOption {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("选项必须有字。空按钮是界面的 bug，不是设计");
        }
        set = set == null ? Map.of() : Map.copyOf(set);
        flags = flags == null ? Set.of() : Set.copyOf(flags);
        when = when == null ? Condition.ALWAYS : when;
    }

    public boolean visible(StoryVars vars) {
        return when.test(vars);
    }

    /** 安全阀：任何标记为"可以走开"的选项，都是这一句话的变体。 */
    public static DialogueOption avoid(String tell) {
        return new DialogueOption("（回避）", "你不必看完这一场。", null, tell, true, Map.of(), Set.of("avoided"),
                Condition.ALWAYS);
    }

    public List<String> counters() {
        return when.counters();
    }
}
