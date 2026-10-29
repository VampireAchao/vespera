package io.github.vampireachao.vespera.core.world;

import io.github.vampireachao.vespera.core.voxel.Scene;
import java.util.BitSet;

/**
 * 走一遍：从某一点出发，按玩家真实的走法能到哪儿。
 *
 * <p>为什么要写这个：看台"看起来"有台阶，和"踩着能上去"是两件事。渲染图里
 * 一块楼梯趴在半空的看台上，和旁边那块一模一样——只有把脚放上去才知道踩不着。
 * 这个类不认建筑，只认三条：脚下是实的、身上两格是空的、一次最多抬一格。
 *
 * <p>它只在一个盒子里走。城太大，三维数组存不下，而需要核对动线的地方
 * （角斗场、王座厅）本来就是局部。
 */
public final class WalkAudit {

    private final Scene scene;
    private final int x1;
    private final int y1;
    private final int z1;
    private final int width;
    private final int depth;
    private final int height;
    private final BitSet seen;

    public WalkAudit(Scene scene, int x1, int y1, int z1, int x2, int y2, int z2) {
        this.scene = scene;
        this.x1 = x1;
        this.y1 = y1;
        this.z1 = z1;
        this.width = x2 - x1 + 1;
        this.depth = z2 - z1 + 1;
        this.height = y2 - y1 + 1;
        this.seen = new BitSet(width * depth * height);
    }

    /**
     * 这一格拦不拦人。
     *
     * <p>门算"不拦"：门是关着的，但玩家右键就开。审动线时如果按实心算，
     * 每一栋房子都会判成"进不去"——而我们想知道的恰恰是"推开门走不走得进去"。
     * 所以只有门在这个类里享有通行权，玻璃、栅栏、牌子都没有。
     */
    private boolean open(int x, int y, int z) {
        var b = scene.get(x, y, z);
        if (b == null || b.isAir()) {
            return true;
        }
        return b.id().endsWith("_door");
    }

    /** 脚下实、身上两格空——玩家能站在这一格。 */
    public boolean canStand(int x, int y, int z) {
        if (!inside(x, y, z)) {
            return false;
        }
        if (scene.isAir(x, y - 1, z) || !open(x, y, z) || !open(x, y + 1, z)) {
            return false;
        }
        return true;
    }

    private boolean inside(int x, int y, int z) {
        return x >= x1 && x < x1 + width && z >= z1 && z < z1 + depth
                && y >= y1 && y < y1 + height;
    }

    private int index(int x, int y, int z) {
        return ((y - y1) * depth + (z - z1)) * width + (x - x1);
    }

    /**
     * 从 (x,z) 这一列从高往低找第一块能站的地面。
     *
     * @param fromY 从哪一层开始往下找
     * @return 站人的那一格；找不到返回 Integer.MIN_VALUE
     */
    public int standY(int x, int z, int fromY) {
        for (int y = Math.min(fromY, y1 + height - 1); y >= y1; y--) {
            if (canStand(x, y, z)) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    /** 从 (sx,sy,sz) 出发走一遍。走不到起点就什么也没发生。 */
    public void walk(int sx, int sy, int sz) {
        if (!canStand(sx, sy, sz)) {
            return;
        }
        int[] queue = new int[width * depth * height];
        int head = 0;
        int tail = 0;
        queue[tail++] = index(sx, sy, sz);
        seen.set(index(sx, sy, sz));
        int[] dx = {1, -1, 0, 0};
        int[] dz = {0, 0, 1, -1};
        while (head < tail) {
            int at = queue[head++];
            int y = at / (width * depth) + y1;
            int rest = at % (width * depth);
            int z = rest / width + z1;
            int x = rest % width + x1;
            for (int d = 0; d < 4; d++) {
                for (int dy = 1; dy >= -4; dy--) {
                    int nx = x + dx[d];
                    int nz = z + dz[d];
                    int ny = y + dy;
                    if (!canStand(nx, ny, nz)) {
                        continue;
                    }
                    // 往上抬一格时，头顶那一格也要是空的（钻石边不算）
                    if (dy == 1 && !open(x, y + 2, z)) {
                        continue;
                    }
                    int ni = index(nx, ny, nz);
                    if (!seen.get(ni)) {
                        seen.set(ni);
                        queue[tail++] = ni;
                    }
                    break;
                }
            }
        }
    }

    public boolean reached(int x, int y, int z) {
        return inside(x, y, z) && seen.get(index(x, y, z));
    }

    /** 这一列最近的地面走到了没有。 */
    public boolean reachedColumn(int x, int z, int fromY) {
        int y = standY(x, z, fromY);
        return y != Integer.MIN_VALUE && reached(x, y, z);
    }
}
