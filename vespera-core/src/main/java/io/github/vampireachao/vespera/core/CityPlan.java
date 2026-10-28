package io.github.vampireachao.vespera.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 流明陨城的平面：主城，也是玩家第一眼看到的地方。
 *
 * <p>为什么先有它：出生点在这里，四十二条主线从这里出发，玩家对这个世界的第一印象是
 * 一条主街和一个钟楼，不是一间教室。学院是"你可以去过一段日子"的地方，主城是"你回来了"
 * 的地方——先有回来的地方，日子才有落点。
 *
 * <p>坐标原点 {@code (0,0)} 是中央广场正中，也是世界出生点的位置。城是 300×300，
 * 四面城墙各开一门，主街十字交叉穿过广场。
 *
 * <p>这是灰盒：体量、动线、天际线是真的，材质是近似。
 */
public final class CityPlan {

    public enum Kind {
        /** 城墙：两格厚，带雉堞。 */
        WALL,
        /** 城门：两座塔夹一道拱。 */
        GATE,
        /** 角塔。 */
        CORNER,
        /** 钟楼：全城最高的东西。 */
        TOWER,
        /** 第九时圣座：双塔，正立面朝广场。 */
        CATHEDRAL,
        /** 灰烬公会：行会厅，方形，带旗杆。 */
        GUILD,
        /** 市集：铺地 + 摊位，没有墙。 */
        MARKET,
        /** 广场：铺地与一口井。 */
        PLAZA,
        /** 民居：两层，坡顶。 */
        HOUSE
    }

    public record Lot(String id, String name, Kind kind, int x1, int z1, int x2, int z2, int height) {

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
    }

    /** 城墙半边长。城是 300×300。 */
    public static final int HALF = 150;

    /** 城门开口半宽。 */
    public static final int GATE_HALF = 6;

    /** 主街半宽。 */
    public static final int ROAD_HALF = 4;

    public static final List<Lot> LOTS = buildLots();

    private CityPlan() {
    }

    private static List<Lot> buildLots() {
        List<Lot> lots = new ArrayList<>();

        lots.add(new Lot("tower", "钟楼", Kind.TOWER, 30, -60, 46, -44, 54));
        // 教堂让开广场：广场到 z=32，教堂从 z=36 起。第一版它压进广场八格，
        // 等于在广场中央立了一堵墙。
        lots.add(new Lot("cathedral", "第九时圣座", Kind.CATHEDRAL, 10, 36, 70, 100, 30));
        lots.add(new Lot("guild", "灰烬公会", Kind.GUILD, -140, -120, -90, -80, 18));
        lots.add(new Lot("market", "市集", Kind.MARKET, 60, -80, 130, -10, 5));
        lots.add(new Lot("plaza", "中央广场", Kind.PLAZA, -32, -32, 32, 32, 1));

        // 四个城门与四个角塔
        lots.add(new Lot("gate-n", "北门", Kind.GATE, -12, -HALF - 4, 12, -HALF + 4, 14));
        lots.add(new Lot("gate-s", "南门", Kind.GATE, -12, HALF - 4, 12, HALF + 4, 14));
        lots.add(new Lot("gate-e", "东门", Kind.GATE, HALF - 4, -12, HALF + 4, 12, 14));
        lots.add(new Lot("gate-w", "西门", Kind.GATE, -HALF - 4, -12, -HALF + 4, 12, 14));
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                int x = sx * (HALF - 8);
                int z = sz * (HALF - 8);
                lots.add(new Lot("corner-" + (sx > 0 ? "e" : "w") + (sz > 0 ? "s" : "n"),
                        "角塔", Kind.CORNER, x - 8, z - 8, x + 8, z + 8, 20));
            }
        }

        // 西南的民居街区：五列三排，街巷留六格
        int index = 0;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 5; column++) {
                int x = -132 + column * 18;
                int z = 24 + row * 34;
                lots.add(new Lot("house-" + (++index), "民居", Kind.HOUSE, x, z, x + 14, z + 12, 11));
            }
        }
        return List.copyOf(lots);
    }

    public static Lot lot(String id) {
        return LOTS.stream().filter(lot -> lot.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("没有这一块：" + id));
    }

    public static List<Lot> houses() {
        return LOTS.stream().filter(lot -> lot.kind() == Kind.HOUSE).toList();
    }

    /** 出生点：南门里，主街正中。往前看是广场与钟楼。 */
    public static int[] spawn() {
        return new int[] {0, 100};
    }

    public static String summary() {
        return String.format(Locale.ROOT, "城墙 %d×%d · %d 处地物（含 %d 栋民居）",
                HALF * 2, HALF * 2, LOTS.size(), houses().size());
    }

    /** 俯视图。与 {@code CampusPlan.svg()} 同样的画法，坐标换成城的。 */
    public static String svg() {
        double scale = 2.0;
        int pad = 34;
        int minX = -HALF - 24;
        int minZ = -HALF - 24;
        int span = HALF * 2 + 48;
        int viewW = (int) (span * scale) + pad * 2;
        int viewH = (int) (span * scale) + pad * 2 + 40;

        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 ").append(viewW).append(' ')
                .append(viewH).append("\" font-family=\"Songti SC, Palatino, serif\">\n");
        svg.append("<rect width=\"100%\" height=\"100%\" fill=\"#fbf5e6\"/>\n");
        svg.append("<text x=\"").append(pad).append("\" y=\"24\" font-size=\"12\" fill=\"#8a7a63\" ")
                .append("letter-spacing=\"3\">流明陨城 · 平面灰盒　LUMENFALL</text>\n");
        svg.append("<g transform=\"translate(").append(pad).append(',').append(pad + 10).append(")\">\n");

        // 城墙
        svg.append("<rect x=\"").append(px(-HALF, minX, scale)).append("\" y=\"")
                .append(px(-HALF, minZ, scale)).append("\" width=\"").append(HALF * 2 * scale)
                .append("\" height=\"").append(HALF * 2 * scale)
                .append("\" fill=\"none\" stroke=\"#6b5b45\" stroke-width=\"3\"/>\n");

        // 主街
        svg.append("<line x1=\"").append(px(0, minX, scale)).append("\" y1=\"").append(px(-HALF, minZ, scale))
                .append("\" x2=\"").append(px(0, minX, scale)).append("\" y2=\"")
                .append(px(HALF, minZ, scale)).append("\" stroke=\"#d8bf90\" stroke-width=\"")
                .append(ROAD_HALF * 2 * scale).append("\"/>\n");
        svg.append("<line x1=\"").append(px(-HALF, minX, scale)).append("\" y1=\"").append(px(0, minZ, scale))
                .append("\" x2=\"").append(px(HALF, minX, scale)).append("\" y2=\"")
                .append(px(0, minZ, scale)).append("\" stroke=\"#d8bf90\" stroke-width=\"")
                .append(ROAD_HALF * 2 * scale).append("\"/>\n");

        for (Lot lot : LOTS) {
            boolean open = lot.kind() == Kind.PLAZA || lot.kind() == Kind.MARKET;
            svg.append("<rect x=\"").append(px(Math.min(lot.x1(), lot.x2()), minX, scale))
                    .append("\" y=\"").append(px(Math.min(lot.z1(), lot.z2()), minZ, scale))
                    .append("\" width=\"").append(lot.width() * scale)
                    .append("\" height=\"").append(lot.depth() * scale)
                    .append("\" fill=\"").append(fill(lot.kind()))
                    .append("\" stroke=\"#6b5b45\" stroke-width=\"1\"")
                    .append(open ? " opacity=\".55\"/>\n" : "/>\n");
            // 只标地标：民居与角塔太多字会糊成一片
            boolean landmark = switch (lot.kind()) {
                case TOWER, CATHEDRAL, GUILD, MARKET, PLAZA -> true;
                default -> false;
            };
            if (landmark) {
                svg.append("<text x=\"").append(px(lot.centerX(), minX, scale)).append("\" y=\"")
                        .append(px(lot.centerZ(), minZ, scale) + 4)
                        .append("\" font-size=\"10\" fill=\"#2a231b\" text-anchor=\"middle\">")
                        .append(lot.name()).append("</text>\n");
            }
        }

        svg.append("<g transform=\"translate(").append((int) (span * scale) - 18).append(", 40)\">\n")
                .append("<path d=\"M0,-18 L6,4 L0,0 L-6,4 Z\" fill=\"#9d2f24\"/>\n")
                .append("<text x=\"0\" y=\"17\" font-size=\"8\" fill=\"#7a6b58\" text-anchor=\"middle\">北</text>\n")
                .append("</g>\n");
        svg.append("</g>\n");
        svg.append("<text x=\"").append(pad).append("\" y=\"").append(viewH - 14)
                .append("\" font-size=\"9\" fill=\"#8a7a63\">比例 1 方块 = 2 像素　·　浅褐十字为主街　·　")
                .append("半透明块为露天场所（广场 / 市集）</text>\n");
        svg.append("</svg>\n");
        return svg.toString();
    }

    private static double px(int value, int origin, double scale) {
        return (value - origin) * scale;
    }

    private static String fill(Kind kind) {
        return switch (kind) {
            case WALL -> "#b9ae9d";
            case GATE -> "#b8873b";
            case CORNER -> "#a8987c";
            case TOWER -> "#d9c6a5";
            case CATHEDRAL -> "#cfa98f";
            case GUILD -> "#a9b79a";
            case MARKET -> "#e2cfa4";
            case PLAZA -> "#e8dcc0";
            case HOUSE -> "#cbb894";
        };
    }
}
