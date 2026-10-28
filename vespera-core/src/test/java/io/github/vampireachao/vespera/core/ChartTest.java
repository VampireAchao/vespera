package io.github.vampireachao.vespera.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChartTest {

    private static final Chart NINTH_BELL = new Chart(120, 90, List.of(
            new Chart.Note(4, "melody", 12),
            new Chart.Note(6, "bass", 0),
            new Chart.Note(8, "drums", 0)));

    @Test
    @DisplayName("120 BPM 即 500ms 一拍，第 8 拍落在第 4000ms")
    void beatMath() {
        assertEquals(500, NINTH_BELL.millisPerBeat());
        assertEquals(2_000, NINTH_BELL.expectedMillis(4));
        assertEquals(4_000, NINTH_BELL.expectedMillis(8));
    }

    @Test
    @DisplayName("窗口 60 / 140 毫秒，边界值算进宽的那一档")
    void judgmentWindows() {
        assertEquals(Chart.Judgement.PERFECT, Chart.judge(0, false));
        assertEquals(Chart.Judgement.PERFECT, Chart.judge(60, false));
        assertEquals(Chart.Judgement.GOOD, Chart.judge(61, false));
        assertEquals(Chart.Judgement.GOOD, Chart.judge(-140, false));
        assertEquals(Chart.Judgement.MISS, Chart.judge(141, false));
    }

    @Test
    @DisplayName("练习模式整体放宽 1.5 倍：80ms 的偏差在正式局掉到 Good，在练习里仍是 Perfect")
    void practiceIsForgiving() {
        assertEquals(Chart.Judgement.GOOD, Chart.judge(80, false));
        assertEquals(Chart.Judgement.PERFECT, Chart.judge(80, true));
    }

    @Test
    @DisplayName("延迟要减掉再判，否则高 ping 玩家永远在抢拍")
    void latencyIsCompensated() {
        // 应在第 4000ms 到，实际 4060ms 到，单程 60ms —— 其实一分不差
        assertEquals(Chart.Judgement.PERFECT, NINTH_BELL.judgeWithLatency(8, 4_060, 60, false));
        // 不补偿的话，60ms 已经贴着 Perfect 的上限；再抖 30ms 就掉档
        assertEquals(Chart.Judgement.GOOD, NINTH_BELL.judgeWithLatency(8, 4_090, 0, false));
    }

    @Test
    @DisplayName("气氛值：Miss 掉 3，断连再扣 8；评级按 90 / 75 / 55 切")
    void moodAndGrade() {
        assertEquals(52, Chart.mood(50, Chart.Judgement.PERFECT, false));
        assertEquals(47, Chart.mood(50, Chart.Judgement.MISS, false));
        assertEquals(39, Chart.mood(50, Chart.Judgement.MISS, true));
        assertEquals(0, Chart.mood(2, Chart.Judgement.MISS, true));
        assertEquals("S", Chart.grade(90));
        assertEquals("A", Chart.grade(89));
        assertEquals("B", Chart.grade(55));
        assertEquals("C", Chart.grade(54));
    }

    @Test
    @DisplayName("谱面能从 YAML 映射读出来")
    void parsesYamlShape() {
        Chart chart = Chart.parse(Map.of(
                "bpm", 120,
                "length", 90,
                "notes", List.of(
                        Map.of("beat", 4, "part", "melody", "pitch", 12),
                        Map.of("beat", 6, "part", "bass", "pitch", 0))));
        assertEquals(120, chart.bpm());
        assertEquals(2, chart.notes().size());
        assertEquals("bass", chart.notes().get(1).part());
    }
}
