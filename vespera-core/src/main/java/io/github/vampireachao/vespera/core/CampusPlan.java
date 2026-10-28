package io.github.vampireachao.vespera.core;

import java.util.List;
import java.util.Locale;

/**
 * 钟塔学院的平面：一份数据，两个用途——盖房子，和画出来给人看。
 *
 * <p>之前这里是空的：只有坐标表和建筑名，没有一栋楼。现在把"长什么样"写成数据，
 * 让建造器和图纸从同一个源出发生成，改布局只改一处。
 *
 * <p>坐标原点 {@code (0, 0)} 是校区中心，x 向东，z 向南。真实落位时整体平移到
 * {@code regions/regions.yml} 里 {@code college} 的中心，y 取地面高度。
 *
 * <p>这是**灰盒**（greybox）：体量、动线、开口位置是真的，材质是能看懂的近似。
 * 它的作用是让人走进去判断"这片空间对不对"，不是终稿。
 */
public final class CampusPlan {

    public enum Kind {
        /** 校门：两根石柱 + 一道铁栅。 */
        GATE,
        /** 教学楼：三层，教室沿走廊排开。 */
        HALL,
        /** 钟楼：全校最高的东西，从任何地方都看得见。 */
        TOWER,
        /** 宿舍：两层，每间一扇窗。 */
        DORM,
        /** 体育馆：唯一有完整屋顶的大场馆，无天窗。 */
        GYM,
        /** 校医室：双门，一门通走廊，一门通后山。 */
        CLINIC,
        /** 礼堂：舞台 + 吊灯。 */
        STAGE,
        /** 温室：玻璃顶。 */
        GREENHOUSE,
        /** 旧校舍：塌了一半。 */
        RUIN,
        /** 天文台：圆顶。 */
        DOME,
        /** 锅炉房：在地下，有一扇常开的下界门。 */
        BOILER
    }

    /**
     * 一栋楼。
     *
     * @param height 从地面算起的高度；{@link Kind#BOILER} 是向下挖的深度（用负数表示）
     * @param floors 楼层数，一层就是空壳
     */
    public record Lot(String id, String name, Kind kind, int x1, int z1, int x2, int z2, int height, int floors) {

        public int width() {
            return Math.abs(x2 - x1);
        }

        public int depth() {
            return Math.abs(z2 - z1);
        }

        public int centerX() {
            return (x1 + x2) / 2;
        }

        public int centerZ() {
            return (z1 + z2) / 2;
        }

        public boolean contains(int x, int z) {
            return x >= Math.min(x1, x2) && x <= Math.max(x1, x2)
                    && z >= Math.min(z1, z2) && z <= Math.max(z1, z2);
        }
    }

    /** 一段有顶的回廊：轴对齐，宽度固定四格。 */
    public record Corridor(int x1, int z1, int x2, int z2) {
    }

    public static final List<Lot> LOTS = List.of(
            new Lot("gate", "校门", Kind.GATE, -8, -46, 8, -38, 8, 1),
            new Lot("hall", "教学楼", Kind.HALL, -32, -22, 32, 14, 21, 3),
            // 钟楼贴着教学楼的东墙，不独立占地——它是这栋楼的一部分
            new Lot("tower", "钟楼", Kind.TOWER, 32, -22, 42, -10, 46, 5),
            new Lot("dorm", "宿舍楼", Kind.DORM, -84, -18, -44, 14, 15, 2),
            new Lot("gym", "体育馆", Kind.GYM, 48, -18, 108, 22, 17, 1),
            new Lot("clinic", "校医室", Kind.CLINIC, -30, 20, -8, 36, 9, 1),
            new Lot("stage", "礼堂", Kind.STAGE, 2, 26, 38, 52, 15, 1),
            new Lot("greenhouse", "温室", Kind.GREENHOUSE, -84, 26, -58, 48, 11, 1),
            new Lot("oldwing", "旧校舍", Kind.RUIN, 60, 40, 90, 72, 13, 2),
            new Lot("observatory", "天文台", Kind.DOME, 96, 44, 118, 66, 20, 1),
            new Lot("boiler", "锅炉房", Kind.BOILER, 58, 4, 78, 24, 12, 1));

    /**
     * 回廊：只连相邻的两栋楼，不横穿任何一栋。
     *
     * <p>第一版把主廊画成一条横贯全校的大道，结果它从教学楼正中间穿了过去——
     * 图纸上看着很像"中轴线"，走进去会发现楼被劈成两半。
     */
    public static final List<Corridor> CORRIDORS = List.of(
            new Corridor(-2, -38, 2, -22),     // 校门 → 教学楼北门
            new Corridor(-44, -4, -32, 0),     // 宿舍东门 → 教学楼西门
            new Corridor(32, -4, 48, 0),       // 教学楼东门 → 体育馆西门
            new Corridor(-20, 14, -16, 20),    // 教学楼南 → 校医室
            new Corridor(18, 14, 22, 26),      // 教学楼南 → 礼堂
            new Corridor(-72, 14, -68, 26),    // 宿舍南 → 温室
            new Corridor(64, 22, 72, 30));     // 体育馆南 → 锅炉房楼梯口

    /** 围墙范围，只到腰高——看得见外面，出不去而已。 */
    public static final int WALL_MIN_X = -92;
    public static final int WALL_MIN_Z = -52;
    public static final int WALL_MAX_X = 124;
    public static final int WALL_MAX_Z = 78;

    /** 校门的开口半宽。围墙到这里断开。 */
    public static final int GATE_HALF = 8;

    private CampusPlan() {
    }

    public static Lot lot(String id) {
        return LOTS.stream().filter(lot -> lot.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("没有这栋楼：" + id));
    }

    /** 整片校区的包围盒，建造前用来决定要不要清地。 */
    public static int[] bounds() {
        return new int[] {WALL_MIN_X, WALL_MIN_Z, WALL_MAX_X, WALL_MAX_Z};
    }

    /**
     * 画一张俯视平面图（SVG）。
     *
     * <p>这张图就是给建造用的同一份数据：改了 {@link #LOTS}，图和房子一起变。
     * 图纸和实物不同步，是这类项目最常见的谎。
     */
    public static String svg() {
        double scale = 3.0;
        int pad = 40;
        int minX = WALL_MIN_X - 20;
        int minZ = WALL_MIN_Z - 20;
        int width = (int) ((WALL_MAX_X - WALL_MIN_X + 40) * scale);
        int height = (int) ((WALL_MAX_Z - WALL_MIN_Z + 40) * scale);
        int viewW = width + pad * 2;
        int viewH = height + pad * 2 + 46;

        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 ").append(viewW).append(' ')
                .append(viewH).append("\" font-family=\"Songti SC, Palatino, serif\">\n");
        svg.append("<rect width=\"100%\" height=\"100%\" fill=\"#fbf5e6\"/>\n");
        svg.append("<text x=\"").append(pad).append("\" y=\"26\" font-size=\"13\" fill=\"#8a7a63\" letter-spacing=\"3\">")
                .append("钟塔学院 · 平面灰盒　CAMPUS PLAN</text>\n");

        svg.append("<g transform=\"translate(").append(pad).append(',').append(pad + 12).append(")\">\n");
        // 地面
        svg.append("<rect x=\"").append(px(WALL_MIN_X, minX, scale)).append("\" y=\"")
                .append(px(WALL_MIN_Z, minZ, scale)).append("\" width=\"")
                .append((WALL_MAX_X - WALL_MIN_X) * scale).append("\" height=\"")
                .append((WALL_MAX_Z - WALL_MIN_Z) * scale)
                .append("\" fill=\"#f4ead6\" stroke=\"#cbb894\" stroke-dasharray=\"6 5\"/>\n");

        // 回廊
        for (Corridor corridor : CORRIDORS) {
            svg.append("<line x1=\"").append(px(corridor.x1(), minX, scale)).append("\" y1=\"")
                    .append(px(corridor.z1(), minZ, scale)).append("\" x2=\"")
                    .append(px(corridor.x2(), minX, scale)).append("\" y2=\"")
                    .append(px(corridor.z2(), minZ, scale))
                    .append("\" stroke=\"#d8bf90\" stroke-width=\"").append(4 * scale)
                    .append("\" stroke-linecap=\"square\"/>\n");
        }

        // 楼
        for (Lot lot : LOTS) {
            boolean underground = lot.kind() == Kind.BOILER;
            String fill = underground ? "#bfb3a0" : kindFill(lot.kind());
            svg.append("<rect x=\"").append(px(Math.min(lot.x1(), lot.x2()), minX, scale))
                    .append("\" y=\"").append(px(Math.min(lot.z1(), lot.z2()), minZ, scale))
                    .append("\" width=\"").append(lot.width() * scale)
                    .append("\" height=\"").append(lot.depth() * scale)
                    .append("\" fill=\"").append(fill)
                    .append("\" stroke=\"#6b5b45\" stroke-width=\"1.2\"")
                    .append(underground ? " stroke-dasharray=\"4 3\" opacity=\".75\"/>\n" : "/>\n");
            svg.append("<text x=\"").append(px(lot.centerX(), minX, scale)).append("\" y=\"")
                    .append(px(lot.centerZ(), minZ, scale) + 4)
                    .append("\" font-size=\"11\" fill=\"#2a231b\" text-anchor=\"middle\">")
                    .append(lot.name()).append("</text>\n");
            svg.append("<text x=\"").append(px(lot.centerX(), minX, scale)).append("\" y=\"")
                    .append(px(lot.centerZ(), minZ, scale) + 16)
                    .append("\" font-size=\"8\" fill=\"#7a6b58\" text-anchor=\"middle\" letter-spacing=\"1\">")
                    .append(lot.width()).append('×').append(lot.depth()).append("  h").append(lot.height())
                    .append("</text>\n");
        }

        // 北向
        svg.append("<g transform=\"translate(").append(width - 26).append(", 46)\">\n")
                .append("<path d=\"M0,-22 L7,6 L0,0 L-7,6 Z\" fill=\"#9d2f24\"/>\n")
                .append("<text x=\"0\" y=\"20\" font-size=\"9\" fill=\"#7a6b58\" text-anchor=\"middle\">北</text>\n</g>\n");
        svg.append("</g>\n");

        svg.append("<text x=\"").append(pad).append("\" y=\"").append(viewH - 16)
                .append("\" font-size=\"9\" fill=\"#8a7a63\">")
                .append("比例 1 方块 = 3 像素　·　虚线方框为围墙（腰高）　·　虚线楼为地下锅炉房　·　")
                .append("浅褐色带状为有顶回廊</text>\n");
        svg.append("</svg>\n");
        return svg.toString();
    }

    private static double px(int value, int origin, double scale) {
        return (value - origin) * scale;
    }

    private static String kindFill(Kind kind) {
        return switch (kind) {
            case GATE -> "#b8873b";
            case HALL, TOWER -> "#d9c6a5";
            case DORM -> "#cbb894";
            case GYM -> "#a9b79a";
            case CLINIC -> "#c8b6b0";
            case STAGE -> "#b7a8c4";
            case GREENHOUSE -> "#a8c6bd";
            case RUIN -> "#b0a89a";
            case DOME -> "#9fb3c4";
            default -> "#cccccc";
        };
    }

    /** 一句话摘要，进日志用。 */
    public static String summary() {
        return String.format(Locale.ROOT, "%d 栋楼 + %d 段回廊，围墙 %d×%d",
                LOTS.size(), CORRIDORS.size(),
                WALL_MAX_X - WALL_MIN_X, WALL_MAX_Z - WALL_MIN_Z);
    }
}
