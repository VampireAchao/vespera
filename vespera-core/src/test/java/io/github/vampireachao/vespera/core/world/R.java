package io.github.vampireachao.vespera.core.world;

import io.github.vampireachao.vespera.core.voxel.IsoRenderer;
import io.github.vampireachao.vespera.core.voxel.Scene;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class R {

    /** 把一个机位转到第 quarter 象限，并按转过的坐标重新算框。 */
    private void shot(Scene s, String file, int quarter, int x1, int z1, int x2, int z2,
            int w, int h, double scale, String caption) throws Exception {
        int[] a = rot(x1, z1, quarter);
        int[] b = rot(x2, z2, quarter);
        int[] c = rot(x1, z2, quarter);
        int[] d = rot(x2, z1, quarter);
        int minX = Math.min(Math.min(a[0], b[0]), Math.min(c[0], d[0]));
        int maxX = Math.max(Math.max(a[0], b[0]), Math.max(c[0], d[0]));
        int minZ = Math.min(Math.min(a[1], b[1]), Math.min(c[1], d[1]));
        int maxZ = Math.max(Math.max(a[1], b[1]), Math.max(c[1], d[1]));
        IsoRenderer.save(IsoRenderer.render(s.rotated(quarter), minX, minZ, maxX, maxZ, w, h,
                scale, caption), Path.of(file));
    }

    private int[] rot(int x, int z, int q) {
        for (int i = 0; i < Math.floorMod(q, 4); i++) {
            int nx = z;
            int nz = -x;
            x = nx;
            z = nz;
        }
        return new int[] {x, z};
    }

    @Test
    void r() throws Exception {
        Scene s = MedievalCity.build();
        shot(s, "/tmp/w_th_0.png", 0, -110, -320, 110, -140, 1700, 1150, 7.0, "王座厅 · 东南面");
        shot(s, "/tmp/w_th_1.png", 1, -110, -320, 110, -140, 1700, 1150, 7.0, "王座厅 · 东北面");
        shot(s, "/tmp/w_th_2.png", 2, -110, -320, 110, -140, 1700, 1150, 7.0, "王座厅 · 西北面");
        shot(s, "/tmp/w_th_3.png", 3, -110, -320, 110, -140, 1700, 1150, 7.0, "王座厅 · 西南面");
        shot(s, "/tmp/w_th_stair.png", 0, -30, -250, 30, -200, 1500, 1000, 12.0, "王座厅 · 大台阶到门廊");
        shot(s, "/tmp/v_pyr_stair.png", 0, -20, -80, 20, -60, 1400, 900, 14.0, "圣座北面 · 去王座厅的路");
    }
}
