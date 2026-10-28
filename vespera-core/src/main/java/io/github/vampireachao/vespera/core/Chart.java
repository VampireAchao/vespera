package io.github.vampireachao.vespera.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 音游谱面：一张表，服务端拿它算节拍，客户端什么都不知道。
 *
 * <p>为什么是服务端判定：原版客户端不会告诉你"玩家此刻在不在拍子上"。可行的做法只有
 * 让玩家做一个动作（手持音叉右键），服务端拿收到的时间减掉预期节拍时间，再决定这一拍
 * 算不算。代价是延迟会被算进判定里——所以窗口比街机宽，且延迟高时整体放宽。
 *
 * <p>这条路的天花板写在明处：{@link #judgeWithLatency} 里的 {@code latencyMillis}
 * 只是把玩家的 ping 从判定里减掉，减不掉网络抖动。真要做到手感扎实，要么承认它是
 * "节奏提示"而不是"节奏游戏"，要么把判定搬到客户端（那就需要模组，与本项目的
 * "原版客户端可加入"冲突）。本方案选前者。
 */
public record Chart(int bpm, int lengthBeats, List<Note> notes) {

    /** 一拍多少毫秒。120 BPM = 500ms = 10 红石刻。 */
    public static final long BASE_BEAT_MILLIS = 500;

    public enum Judgement {
        PERFECT, GOOD, MISS
    }

    public Chart {
        if (bpm <= 0) {
            throw new IllegalArgumentException("bpm 必须为正");
        }
        notes = notes == null ? List.of() : List.copyOf(notes);
    }

    public record Note(int beat, String part, int pitch) {
    }

    public long millisPerBeat() {
        return Math.round(60_000.0 / bpm);
    }

    /** 第 {@code beat} 拍应当落在这一首的第几毫秒。 */
    public long expectedMillis(int beat) {
        return Math.round(beat * (double) millisPerBeat());
    }

    /**
     * 判定。窗口 60 / 140 毫秒，练习模式整体 ×1.5。
     *
     * <p>这两个数字不是手感调出来的，是算出来的：原版一个 tick 是 50ms，服务端能分辨的
     * 最小粒度就是它，所以 Perfect 窗口必须大于 1 tick，取 60；Good 取 140，约等于
     * 两个半 tick，够覆盖一次正常的网络抖动。
     */
    public static Judgement judge(long deltaMillis, boolean practice) {
        double scale = practice ? 1.5 : 1.0;
        long delta = Math.abs(deltaMillis);
        if (delta <= Math.round(60 * scale)) {
            return Judgement.PERFECT;
        }
        if (delta <= Math.round(140 * scale)) {
            return Judgement.GOOD;
        }
        return Judgement.MISS;
    }

    /**
     * 把玩家的单程延迟从判定里减掉再判。{@code latencyMillis} 取 ping 的一半。
     *
     * <p>不这么做的话，所有高 ping 玩家都会被系统性地判成"抢拍"——这是把网络问题
     * 伪装成操作问题，是最不体面的一种不公平。
     */
    public Judgement judgeWithLatency(int beat, long arrivedAtMillis, int latencyMillis, boolean practice) {
        return judge(arrivedAtMillis - latencyMillis - expectedMillis(beat), practice);
    }

    /** 气氛值结算：Perfect +2、Good +1、Miss −3、断连 −8，从 50 起算。 */
    public static int mood(int previous, Judgement judgement, boolean brokeCombo) {
        int delta = switch (judgement) {
            case PERFECT -> 2;
            case GOOD -> 1;
            case MISS -> -3;
        };
        if (brokeCombo) {
            delta -= 8;
        }
        return Math.max(0, Math.min(100, previous + delta));
    }

    /** S ≥90 / A ≥75 / B ≥55 / C <55。演出不会因为一个人停下，评级只是记分。 */
    public static String grade(int mood) {
        if (mood >= 90) {
            return "S";
        }
        if (mood >= 75) {
            return "A";
        }
        if (mood >= 55) {
            return "B";
        }
        return "C";
    }

    /** 解析 YAML：{@code bpm / length / notes[{beat, part, pitch}]}。 */
    public static Chart parse(Map<String, ?> root) {
        int bpm = asInt(root.get("bpm"), 120);
        int length = asInt(root.get("length"), 0);
        List<Note> notes = new ArrayList<>();
        if (root.get("notes") instanceof Collection<?> list) {
            for (Object each : list) {
                if (!(each instanceof Map<?, ?> map)) {
                    continue;
                }
                notes.add(new Note(
                        asInt(map.get("beat"), 0),
                        map.get("part") == null ? "melody" : String.valueOf(map.get("part")),
                        asInt(map.get("pitch"), 0)));
            }
        }
        return new Chart(bpm, length, notes);
    }

    private static int asInt(Object raw, int fallback) {
        if (raw instanceof Number number) {
            return number.intValue();
        }
        if (raw == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(String.valueOf(raw).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
