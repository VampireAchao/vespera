package io.github.vampireachao.vespera.core;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 一条台词、一个选项、一扇门，什么时候成立。
 *
 * <p>写法只允许五种，多一种都会被审校打回来——因为剧本是给人读的，不是给判定引擎读的：
 *
 * <pre>
 * when:
 *   race: [vampire, ghoul]      # 你是这几种之一
 *   flag: [met_bernard]         # 这些旗标都在
 *   not: [burned_at_noon]       # 这些旗标都不在
 *   min: { aff_luka: 20 }       # 计数下限
 *   max: { exposure: 40 }       # 计数上限
 * </pre>
 *
 * <p>五个键之间是"且"。没有一个键写"或"——"或"用两条选项表达，读起来更像人话。
 */
public final class Condition {

    /** 恒真。没写 {@code when} 的选项都是这一条。 */
    public static final Condition ALWAYS = new Condition(Set.of(), Set.of(), Set.of(), Map.of(), Map.of());

    private final Set<String> races;
    private final Set<String> flags;
    private final Set<String> notFlags;
    private final Map<String, Integer> min;
    private final Map<String, Integer> max;

    private Condition(Set<String> races, Set<String> flags, Set<String> notFlags,
            Map<String, Integer> min, Map<String, Integer> max) {
        this.races = races;
        this.flags = flags;
        this.notFlags = notFlags;
        this.min = min;
        this.max = max;
    }

    /**
     * 解析 YAML 里的 {@code when}。
     *
     * <p>也接受一个裸字符串或一个字符串列表，意思是"只按种族筛"——招新摊位那句
     * "只有吸血鬼听得见"的写法，不该逼人写三行。
     */
    public static Condition parse(Object raw) {
        if (raw == null) {
            return ALWAYS;
        }
        if (raw instanceof String race) {
            return new Condition(Set.of(lower(race)), Set.of(), Set.of(), Map.of(), Map.of());
        }
        if (raw instanceof Collection<?> list) {
            Set<String> races = new LinkedHashSet<>();
            for (Object each : list) {
                races.add(lower(String.valueOf(each)));
            }
            return new Condition(races, Set.of(), Set.of(), Map.of(), Map.of());
        }
        if (!(raw instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("when 只认字符串、列表或映射，收到的是 " + raw.getClass());
        }
        return new Condition(
                strings(map.get("race")),
                strings(map.get("flag")),
                strings(map.get("not")),
                numbers(map.get("min")),
                numbers(map.get("max")));
    }

    public boolean test(StoryVars vars) {
        if (!races.isEmpty() && !races.contains(lower(vars.race()))) {
            return false;
        }
        for (String flag : flags) {
            if (!vars.flag(flag)) {
                return false;
            }
        }
        for (String flag : notFlags) {
            if (vars.flag(flag)) {
                return false;
            }
        }
        for (Map.Entry<String, Integer> entry : min.entrySet()) {
            if (vars.num(entry.getKey(), Integer.MIN_VALUE) < entry.getValue()) {
                return false;
            }
        }
        for (Map.Entry<String, Integer> entry : max.entrySet()) {
            if (vars.num(entry.getKey(), Integer.MAX_VALUE) > entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    public boolean isEmpty() {
        return races.isEmpty() && flags.isEmpty() && notFlags.isEmpty() && min.isEmpty() && max.isEmpty();
    }

    /** 剧本核对用的一句话摘要，进 {@code /vespera doctor} 的输出。 */
    public String describe() {
        if (isEmpty()) {
            return "总是";
        }
        StringBuilder text = new StringBuilder();
        if (!races.isEmpty()) {
            text.append("族=").append(races);
        }
        if (!flags.isEmpty()) {
            text.append(text.isEmpty() ? "" : " ").append("有=").append(flags);
        }
        if (!notFlags.isEmpty()) {
            text.append(text.isEmpty() ? "" : " ").append("无=").append(notFlags);
        }
        if (!min.isEmpty()) {
            text.append(text.isEmpty() ? "" : " ").append("≥").append(min);
        }
        if (!max.isEmpty()) {
            text.append(text.isEmpty() ? "" : " ").append("≤").append(max);
        }
        return text.toString();
    }

    private static Set<String> strings(Object raw) {
        if (raw == null) {
            return Set.of();
        }
        Set<String> values = new LinkedHashSet<>();
        if (raw instanceof Collection<?> list) {
            for (Object each : list) {
                values.add(String.valueOf(each));
            }
        } else {
            values.add(String.valueOf(raw));
        }
        return values;
    }

    private static Map<String, Integer> numbers(Object raw) {
        if (raw == null) {
            return Map.of();
        }
        if (!(raw instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("min / max 必须是映射，收到的是 " + raw.getClass());
        }
        Map<String, Integer> values = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            values.put(String.valueOf(entry.getKey()), asInt(entry.getValue()));
        }
        return values;
    }

    private static int asInt(Object raw) {
        if (raw instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(raw).trim());
    }

    private static String lower(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    /** 只给测试与分析用：列出所有被引用到的计数键，剧本对账时会求这个集合。 */
    public List<String> counters() {
        return java.util.stream.Stream.concat(min.keySet().stream(), max.keySet().stream()).distinct().toList();
    }
}
