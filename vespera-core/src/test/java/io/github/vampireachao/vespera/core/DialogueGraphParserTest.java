package io.github.vampireachao.vespera.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DialogueGraphParserTest {

    /** 钟感测试：四个回答，三种班。带一个只有吸血鬼看得见的选项，用来验条件筛选。 */
    private static Map<String, Object> bellTest() {
        Map<String, Object> intro = new LinkedHashMap<>();
        intro.put("lines", List.of("他不问你叫什么。他只敲三下。", "第一声，正午。第二声，日落。"));
        intro.put("options", List.of(
                option("第三声不对。", "c-class", Map.of("bell_test", 1), null, null),
                option("都對。", "b-class", Map.of("bell_test", 2), null, null),
                option("（回避）", null, Map.of(), List.of("avoided"), true),
                option("（你是吸血鬼，你听见的是心跳。）", "c-class", Map.of(), null, null)));

        Map<String, Object> cClass = new LinkedHashMap<>();
        cClass.put("lines", List.of("他多看了你一眼。"));
        Map<String, Object> bClass = new LinkedHashMap<>();
        bClass.put("lines", List.of("最安全，也最平庸。"));

        Map<String, Object> nodes = new LinkedHashMap<>();
        nodes.put("intro", intro);
        nodes.put("c-class", cClass);
        nodes.put("b-class", bClass);

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("id", "bell-test");
        root.put("speaker", "老修士");
        root.put("entry", "intro");
        root.put("nodes", nodes);
        return root;
    }

    private static Map<String, Object> option(String label, String target, Map<String, Integer> set,
            List<String> flags, Boolean close) {
        Map<String, Object> option = new LinkedHashMap<>();
        option.put("label", label);
        if (target != null) {
            option.put("goto", target);
        }
        if (!set.isEmpty()) {
            option.put("set", set);
        }
        if (flags != null) {
            option.put("flags", flags);
            option.put("when", Map.of("race", List.of("vampire")));
        }
        if (close != null) {
            option.put("close", close);
        }
        return option;
    }

    @Test
    @DisplayName("一张图能读出来：说话人沿用文件头，入口默认第一个节点")
    void parsesGraph() {
        DialogueGraph graph = DialogueGraphParser.parse("fallback", bellTest());
        assertEquals("bell-test", graph.id());
        assertEquals("老修士", graph.speaker());
        assertEquals("intro", graph.entryId());
        assertEquals(3, graph.nodes().size());
        // intro 两句、两个结局各一句
        assertEquals(4, graph.lineCount());
    }

    @Test
    @DisplayName("四层筛选的第一层：人类看不见吸血鬼那一句，吸血鬼看得见")
    void filtersByRace() {
        DialogueGraph graph = DialogueGraphParser.parse("bell-test", bellTest());
        DialogueNode intro = graph.entry();
        assertEquals(3, graph.visible(intro, FakeVars.human()).size());
        assertEquals(4, graph.visible(intro, FakeVars.of("vampire")).size());
    }

    @Test
    @DisplayName("计数按增量写，跳转按 id 走")
    void carriesCountersAndTargets() {
        DialogueGraph graph = DialogueGraphParser.parse("bell-test", bellTest());
        DialogueOption first = graph.entry().options().get(0);
        assertEquals(Map.of("bell_test", 1), first.set());
        assertEquals("c-class", first.target());
        assertEquals(1, graph.node("c-class").lines().size());
    }

    @Test
    @DisplayName("断掉的跳转会被自检抓到，而不是等玩家点出空界面")
    void reportsBrokenTarget() {
        Map<String, Object> root = bellTest();
        @SuppressWarnings("unchecked")
        Map<String, Object> nodes = (Map<String, Object>) root.get("nodes");
        @SuppressWarnings("unchecked")
        Map<String, Object> intro = (Map<String, Object>) nodes.get("intro");
        intro.put("options", List.of(option("去哪？", "nowhere", Map.of(), null, null)));

        DialogueGraph graph = DialogueGraphParser.parse("bell-test", root);
        assertTrue(graph.problems().stream().anyMatch(problem -> problem.contains("nowhere")));
        assertTrue(graph.problems().stream().anyMatch(problem -> problem.contains("走不到")));
    }

    @Test
    @DisplayName("空按钮是错误，不是风格")
    void rejectsBlankLabel() {
        Map<String, Object> root = bellTest();
        @SuppressWarnings("unchecked")
        Map<String, Object> nodes = (Map<String, Object>) root.get("nodes");
        @SuppressWarnings("unchecked")
        Map<String, Object> intro = (Map<String, Object>) nodes.get("intro");
        intro.put("options", List.of(option("   ", null, Map.of(), null, true)));
        assertThrows(IllegalArgumentException.class, () -> DialogueGraphParser.parse("bell-test", root));
    }

    @Test
    @DisplayName("回避按钮自带旗标：跳过不是失败，但要留下它跳过的证据")
    void avoidOptionLeavesTrace() {
        DialogueOption avoid = DialogueOption.avoid("你说你不舒服，先走了。");
        assertTrue(avoid.close());
        assertTrue(avoid.flags().contains("avoided"));
        assertEquals(null, avoid.target());
    }

    @Test
    @DisplayName("闸门分两级：整张图一个，单个选项一个，互不覆盖")
    void parsesGates() {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("lines", List.of("熄灯之后，她翻了个身。"));
        node.put("options", List.of(Map.of("label", "（问她桌上那张纸。）", "gate", "conflict", "close", true)));

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("id", "dorm-night");
        root.put("gate", "bonds");
        root.put("entry", "a");
        root.put("nodes", Map.of("a", node));

        DialogueGraph graph = DialogueGraphParser.parse("dorm-night", root);
        assertEquals("bonds", graph.gate());
        assertEquals("conflict", graph.entry().options().get(0).gate());
        // 没写 gate 的选项就是"不设闸"，不是"闸门名叫 null"
        assertEquals(null, DialogueOption.avoid("走开。").gate());
    }
}
