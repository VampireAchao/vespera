package io.github.vampireachao.vespera.core.world;

import io.github.vampireachao.vespera.core.voxel.IsoRenderer;
import io.github.vampireachao.vespera.core.voxel.Scene;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * 出图：把城渲染成验收用的截图，外加一张规划平面图。
 *
 * <p>图与服务器里的方块同源——同一个 {@link Scene}，一个交给渲染器，一个交给 Bukkit。
 * 所以这张图不是"效果图"，是<b>施工记录</b>：图上有的，进服一定有；图上没有的，
 * 进服也不会有。
 */
public final class CityExport {

    private CityExport() {
    }

    private record Shot(String file, String title, int x1, int z1, int x2, int z2, int w, int h,
            double px) {
    }

    private static final List<Shot> SHOTS = List.of(
            new Shot("01-全城鸟瞰", "流明陨城 · 全景", -330, -330, 330, 330, 2200, 1450, 3.0),
            new Shot("02-南门与主街", "南门 · 铁闸与主街", -70, 210, 70, 335, 1700, 1150, 7.0),
            new Shot("03-圣座金字塔", "圣座 · 七层金字塔与顶坛", -95, -95, 95, 95, 1700, 1250, 8.0),
            new Shot("04-王座大厅", "王座大厅 · 三级台地", -100, -310, 100, -140, 1700, 1250, 6.0),
            new Shot("05-角斗场", "角斗场 · 看台与沙地", 90, 90, 290, 290, 1700, 1250, 6.0),
            new Shot("06-灰烬堡", "灰烬堡 · 主楼与幕墙", -300, -300, -140, -140, 1700, 1300, 7.0),
            new Shot("07-南市", "南市 · 面包房与酒坊", -60, 120, 60, 300, 1700, 1250, 7.0),
            new Shot("08-南港", "南港 · 仓库与驳船", -270, 230, -70, 420, 1700, 1150, 6.0));

    public static void main(String[] args) throws Exception {
        Path out = Path.of(args.length > 0 ? args[0] : "docs/设计图");
        Files.createDirectories(out);

        long started = System.currentTimeMillis();
        Scene scene = MedievalCity.build();
        System.out.println("场景：" + scene.size() + " 个方块，" + (System.currentTimeMillis() - started) + "ms");

        for (Shot shot : SHOTS) {
            BufferedImage image = IsoRenderer.render(scene, shot.x1(), shot.z1(), shot.x2(), shot.z2(),
                    shot.w(), shot.h(), shot.px(), shot.title());
            Path file = out.resolve(shot.file() + ".png");
            IsoRenderer.save(image, file);
            System.out.println("  " + file + "（" + Files.size(file) / 1024 + " KB）");
        }

        Path html = out.resolve("流明陨城-规划图.html");
        Files.writeString(html, plan(scene));
        System.out.println("  " + html + "（" + Files.size(html) / 1024 + " KB）");
    }

    // -- 平面图 -------------------------------------------------------------

    private static final int MAP = 1000;
    private static final int PAD = 46;
    private static final double SPAN = 1120;

    private static double px(double x) {
        return PAD + (x + SPAN / 2) / SPAN * MAP;
    }

    private static String plan(Scene scene) {
        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 1240 1200\" ")
                .append("font-family=\"Songti SC, Palatino, serif\">\n");
        svg.append("<rect width=\"1240\" height=\"1200\" fill=\"#f6efdd\"/>\n");
        svg.append("<text x=\"46\" y=\"46\" font-size=\"22\" fill=\"#2a231b\">流明陨城 · 规划图</text>\n");
        svg.append("<text x=\"46\" y=\"68\" font-size=\"12\" fill=\"#8a7a63\" letter-spacing=\"2\">")
                .append("LUMENFALL · 600×600 城墙 · 15 个分区</text>\n");

        // 城外地形与道路
        svg.append("<rect x=\"").append(px(-560)).append("\" y=\"").append(px(-560))
                .append("\" width=\"").append(MAP).append("\" height=\"").append(MAP)
                .append("\" fill=\"#e8e6c9\"/>\n");
        svg.append(road(-8, -560, 8, -MedievalCity.HALF, 16, "#cfc39a"));
        svg.append(road(-8, MedievalCity.HALF, 8, 560, 16, "#cfc39a"));
        svg.append(road(MedievalCity.HALF, -8, 560, 8, 16, "#cfc39a"));
        svg.append(road(-560, -8, -MedievalCity.HALF, 8, 16, "#cfc39a"));

        // 运河
        svg.append("<rect x=\"").append(px(-250)).append("\" y=\"").append(px(306))
                .append("\" width=\"").append(158 * MAP / SPAN).append("\" height=\"")
                .append(98 * MAP / SPAN).append("\" fill=\"#9dc0d4\"/>\n");

        // 农田
        svg.append("<rect x=\"").append(px(-500)).append("\" y=\"").append(px(40))
                .append("\" width=\"").append(108 * MAP / SPAN).append("\" height=\"")
                .append(204 * MAP / SPAN).append("\" fill=\"#cdd6a0\" stroke=\"#a9b47c\"/>\n");

        // 城墙
        svg.append("<rect x=\"").append(px(-MedievalCity.HALF)).append("\" y=\"").append(px(-MedievalCity.HALF))
                .append("\" width=\"").append(600 * MAP / SPAN).append("\" height=\"")
                .append(600 * MAP / SPAN)
                .append("\" fill=\"#efe6cf\" stroke=\"#5d5145\" stroke-width=\"7\"/>\n");

        // 街道
        for (MedievalCity.Street street : MedievalCity.STREETS) {
            svg.append(road(street.x1(), street.z1(), street.x2(), street.z2(), street.width(),
                    street.main() ? "#dcc79e" : "#e3d5b4"));
        }

        // 分区
        for (MedievalCity.District d : MedievalCity.DISTRICTS) {
            svg.append("<rect x=\"").append(px(d.x1())).append("\" y=\"").append(px(d.z1()))
                    .append("\" width=\"").append(d.width() * MAP / SPAN).append("\" height=\"")
                    .append(d.depth() * MAP / SPAN).append("\" fill=\"").append(fill(d.id()))
                    .append("\" fill-opacity=\".55\" stroke=\"#7a6b58\" stroke-width=\"1\"/>\n");
        }

        // 地标
        marker(svg, 0, 0, "圣座金字塔");
        marker(svg, 0, -230, "王座大厅");
        marker(svg, 185, 185, "角斗场");
        marker(svg, -220, -220, "灰烬堡");
        marker(svg, -166, -40, "市政厅");
        marker(svg, 160, -34, "行会");
        marker(svg, -170, 350, "南港");
        marker(svg, -420, 100, "风车");
        marker(svg, 370, -370, "北村");

        // 城门
        for (int[] gate : new int[][] {{0, -MedievalCity.HALF}, {0, MedievalCity.HALF}, {-MedievalCity.HALF, 0}, {MedievalCity.HALF, 0}}) {
            svg.append("<rect x=\"").append(px(gate[0]) - 6).append("\" y=\"")
                    .append(px(gate[1]) - 6).append("\" width=\"12\" height=\"12\" fill=\"#a8562f\"/>\n");
        }

        // 指北针
        svg.append("<g transform=\"translate(").append(PAD + MAP - 26).append(",120)\">\n")
                .append("<path d=\"M0,-20 L8,6 L0,0 L-8,6 Z\" fill=\"#9d2f24\"/>\n")
                .append("<text x=\"0\" y=\"22\" font-size=\"10\" fill=\"#7a6b58\" text-anchor=\"middle\">北</text>\n")
                .append("</g>\n");

        // 比例尺
        svg.append("<g transform=\"translate(").append(PAD).append(',').append(PAD + MAP + 30).append(")\">\n")
                .append("<line x1=\"0\" y1=\"0\" x2=\"").append(100 * MAP / SPAN)
                .append("\" y2=\"0\" stroke=\"#5d5145\" stroke-width=\"2\"/>\n")
                .append("<text x=\"").append(100 * MAP / SPAN + 8)
                .append("\" y=\"4\" font-size=\"11\" fill=\"#7a6b58\">100 方块</text>\n")
                .append("</g>\n");

        svg.append("</svg>");

        return "<!DOCTYPE html>\n<html lang=\"zh-CN\">\n<head>\n<meta charset=\"utf-8\">\n"
                + "<title>流明陨城 · 规划图</title>\n<style>\n"
                + "  body{margin:0;background:#efe6d4;font-family:'Songti SC',Palatino,serif;color:#2a231b}\n"
                + "  .wrap{max-width:1240px;margin:0 auto;padding:24px 20px 56px;display:grid;"
                + "grid-template-columns:minmax(0,1fr) 300px;gap:28px;align-items:start}\n"
                + "  @media(max-width:980px){.wrap{grid-template-columns:1fr}}\n"
                + "  svg{width:100%;height:auto;display:block;border:1px solid #cbb894;background:#f6efdd}\n"
                + "  aside{font-size:13px;line-height:1.9;color:#4a4034}\n"
                + "  h2{font-size:15px;margin:22px 0 8px;letter-spacing:1px}\n"
                + "  table{width:100%;border-collapse:collapse;font-size:12px}\n"
                + "  td{padding:3px 0;border-bottom:1px dotted #cbb894;vertical-align:top}\n"
                + "  td:first-child{color:#7a6b58;white-space:nowrap;padding-right:8px}\n"
                + "  .k{display:inline-block;width:10px;height:10px;margin-right:6px;border:1px solid #7a6b58;vertical-align:-1px}\n"
                + "  .note{font-size:12px;color:#7a6b58;margin-top:16px}\n"
                + "</style>\n</head>\n<body>\n<div class=\"wrap\">\n<div>\n"
                + svg
                + "</div>\n<aside>\n"
                + "<h2>这座城怎么读</h2>\n"
                + "<p>十字大街从四座城门通到中央市集。四座地标各占一个象限：教堂在东北，"
                + "城堡在西北，市政厅在西，行会在东。剩下的地方是街区——临街是房，院里是井。</p>\n"
                + "<h2>分区</h2>\n<table>\n"
                + districtRows()
                + "</table>\n"
                + "<h2>规模</h2>\n<table>\n"
                + "<tr><td>城墙</td><td>600 × 600，四门、十二座墙塔</td></tr>\n"
                + "<tr><td>方块</td><td>" + String.format(Locale.ROOT, "%,d", scene.size()) + "（含墙外）</td></tr>\n"
                + "<tr><td>最高点</td><td>王座塔 74 格 / 圣座顶坛 63 格</td></tr>\n"
                + "<tr><td>街区</td><td>8 × 8 网格，含 6 处小广场</td></tr>\n"
                + "</table>\n"
                + "<p class=\"note\">这张图与服务器里的方块同源：都从 <code>MedievalCity</code> 生成。"
                + "图上有的，进服一定有。</p>\n"
                + "</aside>\n</div>\n</body>\n</html>\n";
    }

    private static String districtRows() {
        StringBuilder rows = new StringBuilder();
        for (MedievalCity.District d : MedievalCity.DISTRICTS) {
            rows.append("<tr><td><span class=\"k\" style=\"background:").append(fill(d.id()))
                    .append("\"></span>").append(d.name()).append("</td><td>").append(d.note())
                    .append("</td></tr>\n");
        }
        return rows.toString();
    }

    private static String road(int x1, int z1, int x2, int z2, int width, String color) {
        double w = Math.max(2, width * MAP / SPAN);
        return "<line x1=\"" + px(x1) + "\" y1=\"" + px(z1) + "\" x2=\"" + px(x2) + "\" y2=\""
                + px(z2) + "\" stroke=\"" + color + "\" stroke-width=\"" + w
                + "\" stroke-linecap=\"square\"/>\n";
    }

    private static void marker(StringBuilder svg, int x, int z, String label) {
        double sx = px(x);
        double sy = px(z);
        svg.append("<circle cx=\"").append(sx).append("\" cy=\"").append(sy)
                .append("\" r=\"4\" fill=\"#9d2f24\"/>\n");
        svg.append("<text x=\"").append(sx + 7).append("\" y=\"").append(sy + 4)
                .append("\" font-size=\"11\" fill=\"#3a3227\">").append(label).append("</text>\n");
    }

    private static String fill(String id) {
        return switch (id) {
            case "sacred" -> "#e8d3a0";
            case "plaza" -> "#e6cf9a";
            case "throne" -> "#cbb9a8";
            case "colosseum" -> "#d8c7a8";
            case "cathedral", "temple-green" -> "#d9b7a0";
            case "castle" -> "#b8b2a6";
            case "townhall", "guild" -> "#c6c3a0";
            case "crafts", "residential" -> "#d8c6a6";
            case "barracks" -> "#b9b7a2";
            case "graveyard" -> "#b6bfa8";
            case "docks" -> "#a9c6d6";
            case "arena" -> "#dcc9a4";
            case "farm" -> "#c9d69c";
            case "hamlet" -> "#d6c3a2";
            default -> "#d8c6a6";
        };
    }
}
