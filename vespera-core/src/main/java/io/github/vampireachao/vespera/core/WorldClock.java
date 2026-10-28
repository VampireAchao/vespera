package io.github.vampireachao.vespera.core;

/**
 * 学院的一日：把原版 24000 刻切成六段，外加一个"夜"。
 *
 * <p>分界点不是随手定的。晚钟落在第 12400 刻，正是超自然种族插件判定入夜的位置
 * （{@code time % 24000 > 12400}）。学院把晚钟叫做"放学"，世界把它叫做"天黑"——
 * 二者同源，是这套设定里最省力的一处咬合：玩家以为自己在赶时间表，其实在赶刷怪。
 *
 * <p>这个类不认识 Bukkit，也不认识任何插件。它只认识刻。
 */
public final class WorldClock {

    /** 原版一昼夜。 */
    public static final int TICKS_PER_DAY = 24_000;

    /** 入夜阈值。与超自然种族插件的 {@code SuperNManager#worldTimeIsNight} 严格对齐。 */
    public static final int NIGHT_THRESHOLD = 12_400;

    /** 门禁。晚钟之后 600 刻，宿舍锁门，校舍开始刷怪。 */
    public static final int CURFEW = 13_000;

    private WorldClock() {
    }

    /** 当天第几刻，0–23999。 */
    public static int ofDay(long time) {
        return (int) Math.floorMod(time, TICKS_PER_DAY);
    }

    /**
     * 是否入夜。判定式与插件一字不差地一致——这是刻意的：世界观里的"第 12400 声"
     * 必须等于代码里的 {@code > 12400}，否则狼人在第 12400 刻伸不出爪子，
     * 而文案会坚持说它伸了出来。
     */
    public static boolean isNight(long time) {
        return ofDay(time) > NIGHT_THRESHOLD;
    }

    /** 一日的一段。顺序即时间顺序，{@link #NIGHT} 合法地跨过午夜。 */
    public enum Period {
        MORNING_BELL("晨钟"),
        ASSEMBLY("朝礼"),
        MORNING_CLASSES("上午两节"),
        LUNCH("午休"),
        AFTERNOON_CLASSES("下午两节"),
        CLUBS("社团"),
        NIGHT("夜");

        private final String label;

        Period(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /** 按刻取段。边界值归入后一段：1000 刻整已经是朝礼。 */
    public static Period periodOf(long time) {
        int tick = ofDay(time);
        if (tick < 1_000) {
            return Period.MORNING_BELL;
        }
        if (tick < 2_000) {
            return Period.ASSEMBLY;
        }
        if (tick < 6_000) {
            return Period.MORNING_CLASSES;
        }
        if (tick < 7_000) {
            return Period.LUNCH;
        }
        if (tick < 11_000) {
            return Period.AFTERNOON_CLASSES;
        }
        if (tick <= NIGHT_THRESHOLD) {
            return Period.CLUBS;
        }
        return Period.NIGHT;
    }

    /** 玩家看得懂的说法。世界层不报数字，所以这句话里只有声音。 */
    public static String describe(long time) {
        int tick = ofDay(time);
        if (tick <= NIGHT_THRESHOLD) {
            return "第 " + tick + " 刻 · " + periodOf(time).label();
        }
        if (tick <= CURFEW) {
            return "晚钟未散 · " + periodOf(time).label();
        }
        return "第 " + tick + " 刻 · 门已锁";
    }
}
