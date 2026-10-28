package io.github.vampireachao.vespera.core;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 把平面图导出成 SVG。
 *
 * <p>图与房子同源：都从 {@link CampusPlan} 出。所以"图纸和实物不一致"这件事，
 * 在这套结构里是做不到的——想不一致，得改两处，而没人会为了撒谎特意改两处。
 *
 * <pre>
 *   java -cp vespera-core/target/classes io.github.vampireachao.vespera.core.PlanExport
 * </pre>
 */
public final class PlanExport {

    private PlanExport() {
    }

    public static void main(String[] args) throws Exception {
        Path dir = Path.of(args.length > 0 ? args[0] : "docs/设计图");
        Files.createDirectories(dir.toAbsolutePath());

        // 主城优先：它是出生点，也是玩家第一眼看到的东西
        write(dir.resolve("流明陨城平面图"), CityPlan.svg(), "流明陨城 · 平面灰盒",
                "出生点在南门里，往北是广场与钟楼。城墙 300×300，四面各开一门。",
                CityPlan.summary());
        write(dir.resolve("学院平面图"), CampusPlan.svg(), "钟塔学院 · 平面灰盒",
                "主城以东约 370 格，隔一条河。十一栋楼，七段有顶回廊。",
                CampusPlan.summary());
    }

    private static void write(Path base, String drawing, String title, String note, String summary)
            throws Exception {
        Path svg = base.resolveSibling(base.getFileName() + ".svg");
        Files.writeString(svg, drawing);
        Path html = base.resolveSibling(base.getFileName() + ".html");
        Files.writeString(html, page(drawing, title, note));
        System.out.println("已写出 " + svg.getFileName() + " / " + html.getFileName() + "　" + summary);
    }

    private static String page(String drawing, String title, String note) {
        return "<!DOCTYPE html>\n<html lang=\"zh-CN\">\n<head>\n<meta charset=\"utf-8\">\n"
                + "<title>" + title + "</title>\n<style>\n"
                + "  body{margin:0;background:#efe6d4;font-family:'Songti SC',Palatino,serif;color:#2a231b}\n"
                + "  .wrap{max-width:900px;margin:0 auto;padding:28px 20px 48px}\n"
                + "  svg{width:100%;height:auto;display:block;background:#fbf5e6;border:1px solid #cbb894}\n"
                + "  .note{margin-top:18px;font-size:13px;line-height:1.9;color:#5d5145}\n"
                + "  .tag{display:inline-block;padding:1px 8px;border:1px solid #cbb894;border-radius:2px;"
                + "font-size:12px;color:#8a7a63;margin-right:8px}\n"
                + "</style>\n</head>\n<body>\n<div class=\"wrap\">\n"
                + drawing
                + "<div class=\"note\">\n"
                + "  <p><span class=\"tag\">怎么读</span>这张图与游戏里的房子<b>同源</b>："
                + "都从平面数据生成。改一个数字，图纸和建筑一起变。</p>\n"
                + "  <p><span class=\"tag\">位置</span>" + note + "</p>\n"
                + "  <p><span class=\"tag\">怎么进去</span>IDEA 运行配置只有一个"
                + "「维斯佩拉 · 本地服务器」，进服就在出生点。</p>\n"
                + "</div>\n</div>\n</body>\n</html>\n";
    }
}
