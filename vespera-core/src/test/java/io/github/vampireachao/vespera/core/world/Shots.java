package io.github.vampireachao.vespera.core.world;

import io.github.vampireachao.vespera.core.voxel.IsoRenderer;
import io.github.vampireachao.vespera.core.voxel.Scene;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * 出图工具：把城里的某一块，从几个机位拍下来。
 *
 * <p>为什么不是"跑一遍服务器截图"：截图要先起服、要有人站那儿、还要挑天气。而这里要的
 * 是<b>可复现</b>——同一份几何，今天拍和下周拍必须一模一样，否则"这处改了没改好"永远
 * 说不清。所以图直接由 {@link Scene} 画，方块一个不多一个不少。
 *
 * <p>机位只有四个真机位。等轴渲染器是固定视角（从东南方往下看），要看其余三面就得
 * 把场景转过来：{@code rotated(1)} 看到的是原先的西南面，{@code rotated(2)} 是西北，
 * {@code rotated(3)} 是东北。所以"五机位"= 四向立面 + 一张局部特写。
 *
 * <p>平时不跑：只有在命令行给 {@code -Dshots=inn} 时才干活。城市塑形加建造要四十秒，
 * 挂在每次 {@code mvn test} 上不合适。
 */
@EnabledIfSystemProperty(named = "shots", matches = ".+")
class Shots {

    /** surefire 的工作目录是模块根，图的落点是仓库里的 docs/切图。 */
    private static final Path OUT = Path.of("..", "docs", "切图");

    private static Scene city;

    private static Scene city() {
        if (city == null) {
            city = MedievalCity.build();
        }
        return city;
    }

    @Test
    void inn() throws IOException {
        String filter = System.getProperty("shots", "");
        if (!filter.contains("inn")) {
            return;
        }
        Scene s = city();
        List<String> made = new ArrayList<>();

        // 地里那一块：x −76..−30、z 246..281，外扩一点好让街面入画
        int x0 = -79;
        int z0 = 246;
        int x1 = -27;
        int z1 = 281;
        made.addAll(five("客栈", s, x0, z0, x1, z1, 1280, 940, 40));
        // 第五机位：东立面入口。正门与拱门是这栋楼唯一的功能入口，值得一张特写
        made.add(shotNamed("客栈-5入口（东立面）", s, 0, -47, 248, -27, 281, 1080, 860, 40));

        for (String p : made) {
            System.out.println("SHOT " + p);
        }
    }

    @Test
    void colosseum() throws IOException {
        if (!System.getProperty("shots", "").contains("colosseum")) {
            return;
        }
        Scene s = city();
        List<String> made = new ArrayList<>();
        int x0 = 96;
        int z0 = 114;
        int x1 = 274;
        int z1 = 272;
        made.addAll(five("角斗场", s, x0, z0, x1, z1, 1400, 1000, 40));
        made.add(shotNamed("角斗场-5东门与贵宾席", s, 0, 205, 150, 285, 240, 1100, 880, 40));
        for (String p : made) {
            System.out.println("SHOT " + p);
        }
    }

    @Test
    void colosseumDebug() throws IOException {
        if (!System.getProperty("shots", "").contains("arenadebug")) {
            return;
        }
        Scene s = city();
        System.out.println("SHOT " + shotNamed("调试-场心", s, 0, 130, 140, 240, 250, 1100, 900, 40,
                cut(s, 9)));
        System.out.println("SHOT " + shotNamed("调试-东立面", s, 0, 255, 150, 290, 240, 900, 900, 40));
        System.out.println("SHOT " + shotNamed("调试-主持台", s, 0, 132, 176, 156, 214, 900, 760, 40));
        System.out.println("SHOT " + shotNamed("调试-包厢内", s, 0, 218, 178, 250, 210, 900, 760, 40));
    }

    @Test
    void innDebug() throws IOException {
        if (!System.getProperty("shots", "").contains("inndebug")) {
            return;
        }
        Scene s = city();
        System.out.println("SHOT " + shotNamed("调试-剖y6", s, 0, -80, 244, -26, 286, 1280, 940, 40,
                cut(s, 6)));
        System.out.println("SHOT " + shotNamed("调试-院", s, 0, -68, 256, -42, 284, 1000, 800, 40,
                cut(s, 9)));
        System.out.println("SHOT " + shotNamed("调试-大堂", s, 0, -46, 248, -28, 282, 1000, 800, 40,
                cut(s, 4)));
    }

    // -- 机位 ---------------------------------------------------------------

    /** 四向立面：东南、西南、西北、东北。 */
    private static List<String> five(String name, Scene s, int x0, int z0, int x1, int z1,
            int w, int h, double maxPx) throws IOException {
        String[] dir = {"东南", "西南", "西北", "东北"};
        List<String> out = new ArrayList<>();
        for (int q = 0; q < 4; q++) {
            String file = name + "-" + (q + 1) + dir[q] + ".png";
            shot(s, q, x0, z0, x1, z1, w, h, maxPx, file);
            out.add(file);
        }
        return out;
    }

    private static String closeup(String name, Scene s, int x0, int z0, int x1, int z1,
            int w, int h, double maxPx) throws IOException {
        String file = name + ".png";
        shot(s, 0, x0, z0, x1, z1, w, h, maxPx, file);
        return file;
    }

    /** 剖面：切掉 {@code top} 以上，看屋子里的结构。 */
    private static String section(String name, Scene s, int x0, int z0, int x1, int z1, int top,
            int w, int h, double maxPx) throws IOException {
        String file = name + ".png";
        shot(cut(s, top), 0, x0, z0, x1, z1, w, h, maxPx, file);
        return file;
    }

    /**
     * 只留 y ≤ top 的方块。
     *
     * <p>不是"把屋顶变透明"——是真的不画。所以切完之后，露出来的那一层会被当成地表
     * 重新描一遍顶面，屋里什么样子就看得见了。
     */
    static Scene cut(Scene s, int top) {
        Scene low = new Scene();
        for (Scene.Entry e : s.entries()) {
            if (e.y() <= top) {
                low.set(e.x(), e.y(), e.z(), e.block());
            }
        }
        return low;
    }

    /** 指定文件名拍一张。 */
    static String shotNamed(String name, Scene s, int q, int x0, int z0, int x1, int z1,
            int w, int h, double maxPx) throws IOException {
        return shotNamed(name, s, q, x0, z0, x1, z1, w, h, maxPx, s);
    }

    /** 同上，但换成另一份场景（用来拍剖面）。 */
    static String shotNamed(String name, Scene s, int q, int x0, int z0, int x1, int z1,
            int w, int h, double maxPx, Scene take) throws IOException {
        String file = name + ".png";
        shot(take, q, x0, z0, x1, z1, w, h, maxPx, file);
        return file;
    }

    /** 拍一张。{@code q} 是逆时针转几个 90°——转的是场景，视角不动。 */
    static void shot(Scene s, int q, int x0, int z0, int x1, int z1, int w, int h, double maxPx,
            String file) throws IOException {
        Scene rot = s.rotated(q);
        int rx0;
        int rx1;
        int rz0;
        int rz1;
        switch (Math.floorMod(q, 4)) {
            case 1 -> {
                rx0 = z0;
                rx1 = z1;
                rz0 = -x1;
                rz1 = -x0;
            }
            case 2 -> {
                rx0 = -x1;
                rx1 = -x0;
                rz0 = -z1;
                rz1 = -z0;
            }
            case 3 -> {
                rx0 = -z1;
                rx1 = -z0;
                rz0 = x0;
                rz1 = x1;
            }
            default -> {
                rx0 = x0;
                rx1 = x1;
                rz0 = z0;
                rz1 = z1;
            }
        }
        BufferedImage img = IsoRenderer.render(rot, rx0, rz0, rx1, rz1, w, h, maxPx, caption(file));
        IsoRenderer.save(img, OUT.resolve(file));
    }

    private static String caption(String file) {
        return file.replace(".png", "");
    }
}
