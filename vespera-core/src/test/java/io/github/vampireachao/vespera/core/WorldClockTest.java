package io.github.vampireachao.vespera.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WorldClockTest {

    @Test
    @DisplayName("入夜判定与插件一字不差：第 12400 刻还是白天，12401 才是夜")
    void nightThresholdMatchesPlugin() {
        assertFalse(WorldClock.isNight(12_400));
        assertTrue(WorldClock.isNight(12_401));
        assertTrue(WorldClock.isNight(18_000));
        assertFalse(WorldClock.isNight(0));
    }

    @Test
    @DisplayName("跨天不炸：第 2 天的 500 刻还是晨钟")
    void wrapsAcrossDays() {
        assertEquals(500, WorldClock.ofDay(24_000 + 500));
        assertEquals(WorldClock.Period.MORNING_BELL, WorldClock.periodOf(48_000 + 500));
    }

    @Test
    @DisplayName("六段的边界都归后一段，晚钟那一响正好是社团的最后一刻")
    void periodBoundaries() {
        assertEquals(WorldClock.Period.MORNING_BELL, WorldClock.periodOf(0));
        assertEquals(WorldClock.Period.ASSEMBLY, WorldClock.periodOf(1_000));
        assertEquals(WorldClock.Period.MORNING_CLASSES, WorldClock.periodOf(2_000));
        assertEquals(WorldClock.Period.LUNCH, WorldClock.periodOf(6_000));
        assertEquals(WorldClock.Period.AFTERNOON_CLASSES, WorldClock.periodOf(7_000));
        assertEquals(WorldClock.Period.CLUBS, WorldClock.periodOf(11_000));
        assertEquals(WorldClock.Period.CLUBS, WorldClock.periodOf(WorldClock.NIGHT_THRESHOLD));
        assertEquals(WorldClock.Period.NIGHT, WorldClock.periodOf(WorldClock.NIGHT_THRESHOLD + 1));
    }

    @Test
    @DisplayName("门禁之后换一种说法：不是几点，是门锁了")
    void curfewReadsDifferently() {
        assertTrue(WorldClock.describe(12_500).contains("晚钟未散"));
        assertTrue(WorldClock.describe(13_500).contains("门已锁"));
    }
}
