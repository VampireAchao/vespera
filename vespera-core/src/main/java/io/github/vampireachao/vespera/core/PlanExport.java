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
        Path svg = Path.of(args.length > 0 ? args[0] : "docs/设计图/学院平面图.svg");
        Files.createDirectories(svg.toAbsolutePath().getParent());
        String drawing = CampusPlan.svg();
        Files.writeString(svg, drawing);

        // 同目录再写一个 HTML：SVG 单独打开不一定有预览器，包一层就一定能看。
        Path html = svg.resolveSibling("学院平面图.html");
        Files.writeString(html, page(drawing));

        System.out.println("已写出 " + svg + "（" + Files.size(svg) + " 字节）");
        System.out.println("已写出 " + html + "（" + Files.size(html) + " 字节）");
        System.out.println(CampusPlan.summary());
    }

    private static String page(String drawing) {
        return "<!DOCTYPE html>\n<html lang=\"zh-CN\">\n<head>\n<meta charset=\"utf-8\">\n"
                + "<title>钟塔学院 · 平面灰盒</title>\n<style>\n"
                + "  body{margin:0;background:#efe6d4;font-family:'Songti SC',Palatino,serif;color:#2a231b}\n"
                + "  .wrap{max-width:900px;margin:0 auto;padding:28px 20px 48px}\n"
                + "  svg{width:100%;height:auto;display:block;background:#fbf5e6;border:1px solid #cbb894}\n"
                + "  .note{margin-top:18px;font-size:13px;line-height:1.9;color:#5d5145}\n"
                + "  .note b{color:#2a231b}\n"
                + "  .tag{display:inline-block;padding:1px 8px;border:1px solid #cbb894;border-radius:2px;"
                + "font-size:12px;color:#8a7a63;margin-right:8px}\n"
                + "</style>\n</head>\n<body>\n<div class=\"wrap\">\n"
                + drawing
                + "<div class=\"note\">\n"
                + "  <p><span class=\"tag\">怎么读</span>这张图与游戏里的房子<b>同源</b>："
                + "都从 <code>CampusPlan</code> 生成。改一个数字，图纸和建筑一起变。</p>\n"
                + "  <p><span class=\"tag\">灰盒</span>体量、动线、开口是真的，材质是近似。"
                + "它要回答的不是「好看吗」，是「走进去这片空间对不对」。</p>\n"
                + "  <p><span class=\"tag\">怎么进去</span>运行 <code>./dev.sh</code>，"
                + "服务器会自己盖一遍，出生点就落在校门外十二格。</p>\n"
                + "</div>\n</div>\n</body>\n</html>\n";
    }
}
