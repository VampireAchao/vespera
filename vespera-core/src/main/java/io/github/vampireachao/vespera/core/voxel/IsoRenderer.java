package io.github.vampireachao.vespera.core.voxel;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * 等轴投影渲染器：把 {@link Scene} 画成一张图。
 *
 * <p>为什么自己写：验收要的是"游戏里长什么样"，不是"概念图长什么样"。用 AI 生图能出更
 * 漂亮的画面，但那张图与服务器里的方块没有关系——真正的风险是你在图上点头，进服后失望。
 * 所以这里只画真的会出现在世界里的方块，一个不多、一个不少。
 *
 * <p>画法是最朴素的画家算法：按 {@code x+y+z} 从远到近排一遍，每块画顶面与两个侧面。
 * 楼梯画成半砖加一条台阶，栏杆画成细柱，玻璃画成薄片——形状对了，眼睛才认得出那是城。
 */
public final class IsoRenderer {

    private static final double KX = 0.866;
    private static final double KY = 0.46;
    private static final double KYY = 0.94;

    /** 先按两倍画，再缩回去：方块边缘的锯齿就是这么消掉的。 */
    private static final int SUPERSAMPLE = 2;

    private IsoRenderer() {
    }

    /**
     * 画一块区域。
     *
     * @param x0     西边界
     * @param z0     北边界
     * @param x1     东边界
     * @param z1     南边界
     * @param width  输出宽（像素）
     * @param height 输出高（像素）
     * @param maxPx  每格最多几像素，防止特写时糊成马赛克
     */
    public static BufferedImage render(Scene scene, int x0, int z0, int x1, int z1,
            int width, int height, double maxPx, String caption) {
        int w = width * SUPERSAMPLE;
        int h = height * SUPERSAMPLE;

        List<Scene.Entry> visible = new ArrayList<>();
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (Scene.Entry entry : scene.surface()) {
            if (entry.x() < x0 || entry.x() > x1 || entry.z() < z0 || entry.z() > z1) {
                continue;
            }
            visible.add(entry);
            minY = Math.min(minY, entry.y());
            maxY = Math.max(maxY, entry.y());
        }
        if (visible.isEmpty()) {
            throw new IllegalStateException("这块区域里没有方块：" + x0 + "," + z0 + " → " + x1 + "," + z1);
        }
        visible.sort(Comparator.comparingInt(entry -> entry.x() + entry.y() + entry.z()));

        double cx = (x0 + x1) / 2.0;
        double cz = (z0 + z1) / 2.0;
        double cy = (minY + maxY) / 2.0;

        double spanXZ = (x1 - x0) + (z1 - z0);
        double needW = spanXZ * KX;
        double needH = spanXZ * KY + (maxY - minY) * KYY;
        double scale = Math.min(Math.min(w * 0.94 / needW, h * 0.90 / needH), maxPx * SUPERSAMPLE);

        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        // 天：一层很缓的竖向渐变。没有它，城会像浮在纸上。
        for (int y = 0; y < h; y++) {
            double t = y / (double) h;
            int r = (int) (196 + 40 * t);
            int gg = (int) (214 + 26 * t);
            int b = (int) (226 + 16 * t);
            g.setColor(new Color(r, gg, b));
            g.drawLine(0, y, w, y);
        }

        Projector p = new Projector(cx, cz, cy, scale, w / 2.0, h / 2.0);

        // 地：一块比城稍大的平面，让城有落脚的地方。
        ground(g, p, x0 - 70, z0 - 70, x1 + 70, z1 + 70, minY - 1.06, shade(0x4c7038, 0.80));
        ground(g, p, x0 - 60, z0 - 60, x1 + 60, z1 + 60, minY - 1, shade(0x6f9a4c, 0.92));

        for (Scene.Entry entry : visible) {
            drawBlock(g, p, entry.x(), entry.y(), entry.z(), entry.block());
        }

        g.dispose();

        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D og = out.createGraphics();
        og.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        og.drawImage(image, 0, 0, width, height, null);
        og.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        if (caption != null && !caption.isBlank()) {
            og.setFont(new Font("Songti SC", Font.PLAIN, 15));
            Color ink = new Color(0x2a231b);
            og.setColor(new Color(255, 250, 238, 190));
            og.fillRect(0, height - 34, width, 34);
            og.setColor(ink);
            og.drawString(caption, 18, height - 12);
            og.setFont(new Font("Songti SC", Font.PLAIN, 11));
            og.setColor(new Color(0x7a6b58));
            String tag = "维斯佩拉 · 与服务器同一份几何";
            int tw = og.getFontMetrics().stringWidth(tag);
            og.drawString(tag, width - tw - 18, height - 12);
        }
        og.dispose();
        return out;
    }

    public static void save(BufferedImage image, Path path) throws IOException {
        java.nio.file.Files.createDirectories(path.toAbsolutePath().getParent());
        ImageIO.write(image, "png", path.toFile());
    }

    // -- 投影与画法 ---------------------------------------------------------

    private record Projector(double cx, double cz, double cy, double scale, double ox, double oy) {

        int sx(double x, double z) {
            return (int) Math.round(((x - cx) - (z - cz)) * KX * scale + ox);
        }

        int sy(double x, double y, double z) {
            return (int) Math.round(((x - cx) + (z - cz)) * KY * scale - (y - cy) * KYY * scale + oy);
        }
    }

    private static void drawBlock(Graphics2D g, Projector p, int x, int y, int z, Block block) {
        String name = block.shortName();
        int base = Palette.colorOf(block.id());
        int hash = (x * 73856093) ^ (z * 19349663) ^ (y * 83492791);
        double tint = 1.0 + ((hash & 0xFF) - 128) / 128.0 * 0.035;

        if (name.endsWith("_stairs")) {
            box(g, p, x, y, z, x + 1, y + 0.5, z + 1, base, tint);
            String facing = property(block, "facing", "east");
            switch (facing) {
                case "west" -> box(g, p, x, y + 0.5, z, x + 0.5, y + 1, z + 1, base, tint);
                case "south" -> box(g, p, x, y + 0.5, z + 0.5, x + 1, y + 1, z + 1, base, tint);
                case "north" -> box(g, p, x, y + 0.5, z, x + 1, y + 1, z + 0.5, base, tint);
                default -> box(g, p, x + 0.5, y + 0.5, z, x + 1, y + 1, z + 1, base, tint);
            }
            return;
        }
        if (name.endsWith("_slab")) {
            boolean top = "top".equals(property(block, "type", "bottom"));
            if (top) {
                box(g, p, x, y + 0.5, z, x + 1, y + 1, z + 1, base, tint);
            } else {
                box(g, p, x, y, z, x + 1, y + 0.5, z + 1, base, tint);
            }
            return;
        }
        if (name.endsWith("_fence") || name.equals("chain") || name.endsWith("_wall")) {
            box(g, p, x + 0.34, y, z + 0.34, x + 0.66, y + 1, z + 0.66, base, tint);
            return;
        }
        if (name.endsWith("_bars")) {
            box(g, p, x, y, z + 0.2, x + 1, y + 1, z + 0.3, base, tint);
            box(g, p, x, y, z + 0.45, x + 1, y + 1, z + 0.55, base, tint);
            box(g, p, x, y, z + 0.7, x + 1, y + 1, z + 0.8, base, tint);
            return;
        }
        if (name.endsWith("_pane")) {
            box(g, p, x + 0.42, y, z + 0.02, x + 0.58, y + 1, z + 0.98, base, tint);
            return;
        }
        if (name.endsWith("_carpet") || name.endsWith("_trapdoor")) {
            box(g, p, x, y, z, x + 1, y + 0.15, z + 1, base, tint);
            return;
        }
        if (name.endsWith("_sign") || name.equals("torch") || name.equals("lantern")
                || name.equals("soul_lantern") || name.equals("flower_pot")) {
            box(g, p, x + 0.3, y, z + 0.3, x + 0.7, y + 0.85, z + 0.7, base, tint);
            return;
        }
        if (name.equals("water")) {
            box(g, p, x, y - 0.12, z, x + 1, y + 0.75, z + 1, base, tint * 0.98);
            return;
        }
        box(g, p, x, y, z, x + 1, y + 1, z + 1, base, tint);
    }

    /** 一个长方体的三个可见面。顶面朝上，两个侧面朝 +x 与 +z。 */
    private static void box(Graphics2D g, Projector p, double x0, double y0, double z0,
            double x1, double y1, double z1, int rgb, double tint) {
        // +z 面（南）
        fillQuad(g, p,
                x0, y0, z1,
                x1, y0, z1,
                x1, y1, z1,
                x0, y1, z1,
                shade(rgb, 0.64 * tint));
        // +x 面（东）
        fillQuad(g, p,
                x1, y0, z0,
                x1, y0, z1,
                x1, y1, z1,
                x1, y1, z0,
                shade(rgb, 0.82 * tint));
        // 顶面
        fillQuad(g, p,
                x0, y1, z0,
                x1, y1, z0,
                x1, y1, z1,
                x0, y1, z1,
                shade(rgb, 1.0 * tint));
    }

    /** 水平的一块地。等轴投影下它仍是个平行四边形，所以还是那四个顶点。 */
    private static void ground(Graphics2D g, Projector p, double x0, double z0, double x1, double z1,
            double y, Color color) {
        fillQuad(g, p, x0, y, z0, x1, y, z0, x1, y, z1, x0, y, z1, color);
    }

    private static void fillQuad(Graphics2D g, Projector p,
            double ax, double ay, double az,
            double bx, double by, double bz,
            double cx, double cy, double cz,
            double dx, double dy, double dz,
            Color color) {
        int[] xs = {p.sx(ax, az), p.sx(bx, bz), p.sx(cx, cz), p.sx(dx, dz)};
        int[] ys = {p.sy(ax, ay, az), p.sy(bx, by, bz), p.sy(cx, cy, cz), p.sy(dx, dy, dz)};
        g.setColor(color);
        g.fillPolygon(xs, ys, 4);
        // 细描边可以盖住相邻多边形之间的针孔；颜色只比填充深一点点。
        g.setColor(color.darker());
        g.setStroke(new BasicStroke(0.6f));
        g.drawPolygon(xs, ys, 4);
    }

    private static Color shade(int rgb, double factor) {
        int r = Math.min(255, (int) (((rgb >> 16) & 0xFF) * factor));
        int gg = Math.min(255, (int) (((rgb >> 8) & 0xFF) * factor));
        int b = Math.min(255, (int) ((rgb & 0xFF) * factor));
        return new Color(r, gg, b);
    }

    private static String property(Block block, String key, String fallback) {
        for (String part : block.props().split(",")) {
            String trimmed = part.trim();
            if (trimmed.startsWith(key + "=")) {
                return trimmed.substring(key.length() + 1);
            }
        }
        return fallback;
    }
}
