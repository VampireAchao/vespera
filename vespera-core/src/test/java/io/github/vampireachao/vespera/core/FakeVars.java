package io.github.vampireachao.vespera.core;

import java.util.List;
import java.util.Map;

/**
 * 测试用的世界状态：一张固定的小表，不启动服务器。
 */
final class FakeVars implements StoryVars {

    private final String race;
    private final Map<String, Integer> counters;
    private final List<String> flags;

    FakeVars(String race, Map<String, Integer> counters, String... flags) {
        this.race = race;
        this.counters = counters;
        this.flags = List.of(flags);
    }

    static FakeVars human() {
        return new FakeVars("human", Map.of());
    }

    static FakeVars of(String race) {
        return new FakeVars(race, Map.of());
    }

    @Override
    public String race() {
        return race;
    }

    @Override
    public boolean flag(String name) {
        return flags.contains(name);
    }

    @Override
    public int num(String name, int fallback) {
        return counters.getOrDefault(name, fallback);
    }
}
