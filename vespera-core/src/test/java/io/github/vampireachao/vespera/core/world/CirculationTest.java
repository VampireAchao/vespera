package io.github.vampireachao.vespera.core.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vampireachao.vespera.core.voxel.Block;
import io.github.vampireachao.vespera.core.voxel.Scene;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 动线：脚能不能到。
 *
 * <p>这个测试不看图、不看日志，只做一件事——把玩家能站的格子连成一张网，
 * 然后问"从这儿能不能走到那儿"。台阶悬空、门后是实心石头、回廊被包厢堵死，
 * 都只有这一问能看见。
 */
class CirculationTest {

    private static final int CX = 185;
    private static final int CZ = 193;

    private static Scene scene;

    private static Scene scene() {
        if (scene == null) {
            scene = MedievalCity.build();
        }
        return scene;
    }

    private static WalkAudit arena(int pad) {
        return new WalkAudit(scene(), CX - 82 - pad, -8, CZ - 70 - pad,
                CX + 82 + pad, 32, CZ + 70 + pad);
    }

    @Test
    @DisplayName("角斗场的台阶真能走：沙地四个角都上得去回廊")
    void arenaStairsClimb() {
        WalkAudit w = arena(20);
        assertTrue(w.canStand(CX, 0, CZ + 20), "沙地上站不住");
        w.walk(CX, 0, CZ + 20);
        // 回廊在 e>0.81 的环带上。取四个斜角，避开东西两侧的贵宾席与主持台
        for (int[] dir : new int[][] {{1, 1}, {-1, 1}, {1, -1}, {-1, -1}}) {
            int ax = CX + dir[0] * 54;
            int az = CZ + dir[1] * 47;
            int y = w.standY(ax, az, 21);
            assertEquals(19, y, "回廊高度不对：" + ax + "," + az + " → " + y);
            assertTrue(w.reached(ax, y, az), "从沙地上不去回廊：" + ax + "," + az);
        }
    }

    @Test
    @DisplayName("四座门都是通的：从门洞外面走进沙地")
    void gatesLeadIntoSand() {
        for (int[] dir : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            WalkAudit w = arena(6);
            int ox = CX + dir[0] * (dir[0] != 0 ? 84 : 0);
            int oz = CZ + dir[1] * (dir[1] != 0 ? 71 : 0);
            int y = w.standY(ox, oz, 8);
            assertEquals(0, y, "门洞里没有落脚的地方：" + ox + "," + oz);
            w.walk(ox, y, oz);
            assertTrue(w.reached(CX, 0, CZ + 20), "从门洞走不进场里：" + ox + "," + oz);
        }
    }

    @Test
    @DisplayName("贵宾席与主持台都上得去：各自从沙地一道石梯直上")
    void boxesAreReachable() {
        WalkAudit w = arena(6);
        w.walk(CX, 0, CZ + 20);
        // 贵宾席在东、主持台在西，都降到场边（台面 y=6，人站 7）
        int boxY = w.standY(CX + 50, CZ, 12);
        assertEquals(7, boxY, "贵宾席地板高度不对");
        assertTrue(w.reached(CX + 50, boxY, CZ), "贵宾席进不去");
        int standY = w.standY(CX - 50, CZ + 4, 12);
        assertEquals(7, standY, "主持台地板高度不对");
        assertTrue(w.reached(CX - 50, standY, CZ + 4), "主持台上不去");
    }

    @Test
    @DisplayName("场务室与兽笼都进得去：一个开门洞，一个开在沙地上")
    void serviceRoomsAreReachable() {
        WalkAudit w = arena(6);
        w.walk(CX, 0, CZ + 20);
        // 场务室往东挪了八格，避让贵宾席的台基
        int roomY = w.standY(CX + 68, CZ - 9, 8);
        assertEquals(1, roomY, "场务室地板高度不对：" + roomY);
        assertTrue(w.reached(CX + 68, roomY, CZ - 9), "场务室进不去（门开在看台里）");
        int cageY = w.standY(CX + 5, CZ + 25, -2);
        assertEquals(-3, cageY, "兽笼地板高度不对：" + cageY);
        assertTrue(w.reached(CX + 5, cageY, CZ + 25), "兽笼下不去");
    }


    @Test
    @DisplayName("民居里能上楼：楼板、梯井与二楼都是通的")
    void housesHaveUsableUpstairs() {
        BuildKit k = MedievalCity.kit(20261028L);
        Scene s = k.scene;
        int tested = 0;
        for (BuildKit.Claim c : k.claims) {
            if (!c.kind().equals("民居")) {
                continue;
            }
            if (c.x2() - c.x1() < 9 || c.z2() - c.z1() < 9) {
                continue;
            }
            // 单层房没有楼板，不该要求"上得去"
            boolean hasSlab = false;
            for (int x = c.x1() + 1; x < c.x2() && !hasSlab; x++) {
                for (int z = c.z1() + 1; z < c.z2() && !hasSlab; z++) {
                    Block b = s.get(x, 3, z);
                    hasSlab = b != null && b.shortName().equals("dark_oak_planks");
                }
            }
            if (!hasSlab) {
                continue;
            }
            WalkAudit au = new WalkAudit(s, c.x1() - 1, -1, c.z1() - 1, c.x2() + 1, 10, c.z2() + 1);
            int sx = 0;
            int sz = 0;
            boolean found = false;
            for (int x = c.x1() + 1; x < c.x2() && !found; x++) {
                for (int z = c.z1() + 1; z < c.z2() && !found; z++) {
                    if (au.canStand(x, 0, z)) {
                        sx = x;
                        sz = z;
                        found = true;
                    }
                }
            }
            if (!found) {
                continue;
            }
            au.walk(sx, 0, sz);
            boolean up = false;
            for (int x = c.x1(); x <= c.x2() && !up; x++) {
                for (int z = c.z1(); z <= c.z2() && !up; z++) {
                    up = au.reached(x, 4, z);
                }
            }
            assertTrue(up, "这栋房子的二楼上去不了：" + c.where());
            if (++tested >= 6) {
                break;
            }
        }
        assertTrue(tested >= 4, "能测的民居太少：" + tested);
    }

    @Test
    @DisplayName("街道连成一片：每一条街都能从中央广场走到")
    void streetsAreConnected() {
        Scene s = scene();
        int w0 = 1100;
        boolean[][] road = new boolean[w0][w0];
        for (int i = 0; i < w0; i++) {
            for (int j = 0; j < w0; j++) {
                road[i][j] = isRoad(s, i - 550, j - 550);
            }
        }
        // 把路归成连通块，最大的一块就是"城里能走到的那些路"
        int[][] comp = new int[w0][w0];
        int[] dx = {1, -1, 0, 0};
        int[] dz = {0, 0, 1, -1};
        int best = 0;
        int bestSize = 0;
        int cid = 0;
        for (int i = 0; i < w0; i++) {
            for (int j = 0; j < w0; j++) {
                if (!road[i][j] || comp[i][j] != 0) {
                    continue;
                }
                cid++;
                int size = 0;
                Queue<int[]> queue = new ArrayDeque<>();
                comp[i][j] = cid;
                queue.add(new int[] {i, j});
                while (!queue.isEmpty()) {
                    int[] p = queue.poll();
                    size++;
                    for (int d = 0; d < 4; d++) {
                        int nx = p[0] + dx[d];
                        int nz = p[1] + dz[d];
                        if (nx < 0 || nz < 0 || nx >= w0 || nz >= w0 || !road[nx][nz]
                                || comp[nx][nz] != 0) {
                            continue;
                        }
                        comp[nx][nz] = cid;
                        queue.add(new int[] {nx, nz});
                    }
                }
                if (size > bestSize) {
                    bestSize = size;
                    best = cid;
                }
            }
        }

        List<String> cut = new ArrayList<>();
        for (MedievalCity.Street street : MedievalCity.STREETS) {
            int ax = Math.min(street.x1(), street.x2());
            int bx = Math.max(street.x1(), street.x2());
            int az = Math.min(street.z1(), street.z2());
            int bz = Math.max(street.z1(), street.z2());
            int total = 0;
            int lost = 0;
            int lx1 = Integer.MAX_VALUE;
            int lz1 = Integer.MAX_VALUE;
            int lx2 = Integer.MIN_VALUE;
            int lz2 = Integer.MIN_VALUE;
            for (int x = ax; x <= bx; x++) {
                for (int z = az; z <= bz; z++) {
                    if (!isRoad(s, x, z)) {
                        continue;
                    }
                    total++;
                    if (comp[x + 550][z + 550] != best) {
                        lost++;
                        lx1 = Math.min(lx1, x);
                        lx2 = Math.max(lx2, x);
                        lz1 = Math.min(lz1, z);
                        lz2 = Math.max(lz2, z);
                    }
                }
            }
            // 容许一成二：一条街上被码头、衙署、城门占掉几格是正常的。
            // 真断头（整段没了）会接近 100%，照样红
            if (total > 0 && lost * 100 > total * 12) {
                cut.add("(" + ax + "," + az + ")-(" + bx + "," + bz + ") 丢 " + lost + "/" + total
                        + " 在 (" + lx1 + "," + lz1 + ")-(" + lx2 + "," + lz2 + ")");
            }
        }
        assertTrue(cut.isEmpty(), "这些街段从广场走不过去：" + cut);
    }

    /** 街面：y=-1 铺的是石，头顶两格是空的。土路、院子、房屋地板都不算。 */
    private static boolean isRoad(Scene s, int x, int z) {
        Block block = s.get(x, -1, z);
        if (block == null) {
            return false;
        }
        String id = block.id();
        boolean paved = id.contains("cobblestone") || id.contains("stone_bricks")
                || id.contains("andesite") || id.contains("sandstone")
                || id.contains("deepslate_tiles") || id.contains("bricks");
        return paved && s.isAir(x, 0, z) && s.isAir(x, 1, z);
    }
}
