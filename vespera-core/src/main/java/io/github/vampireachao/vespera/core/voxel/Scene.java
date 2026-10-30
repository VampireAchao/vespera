package io.github.vampireachao.vespera.core.voxel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 一堆方块，没有世界、没有区块、没有 Bukkit。
 *
 * <p>它存在的理由很实际：<b>建筑要能被看见两次</b>——一次在游戏里，一次在验收的图上。
 * 如果两次各写一遍代码，图上一定比游戏里好看，然后你就会在进服的那一刻失望。
 * 所以几何只生成一次，写进这里；之后一边交给 Bukkit，一边交给渲染器。
 *
 * <p>用哈希表而不是三维数组：城是 640×640×120，数组要 4900 万格，而真正有方块的地方
 * 只有四十多万格。稀疏存储让"城外的一片空"不占任何内存。
 */
public final class Scene {

    private final Map<Long, Block> blocks = new HashMap<>(1 << 20);
    /**
     * 明确要求清空的格子。
     *
     * <p>为什么需要它：{@link #set} 传 null 只是"把这一格从场景里删掉"，而落地器只写
     * 场景里有的方块——删掉不等于在世界里挖开。地上的建筑从来不需要说这句话
     * （那里原本就是空的），但<b>往地下挖</b>必须说：地牢要真的在石头里凿出一个洞，
     * 而不是在图上画一个洞、进服一挖还是实心。
     */
    private final java.util.Set<Long> air = new java.util.HashSet<>();
    private int minX = Integer.MAX_VALUE;
    private int maxX = Integer.MIN_VALUE;
    private int minY = Integer.MAX_VALUE;
    private int maxY = Integer.MIN_VALUE;
    private int minZ = Integer.MAX_VALUE;
    private int maxZ = Integer.MIN_VALUE;

    /** 坐标系允许的范围：±8192，Y 到 1024。够放一座城和它周围的地。 */
    private static final int LIMIT = 8192;

    public void set(int x, int y, int z, Block block) {
        if (block == null || block.isAir()) {
            blocks.remove(key(x, y, z));
            return;
        }
        air.remove(key(x, y, z));
        blocks.put(key(x, y, z), block);
        if (x < minX) {
            minX = x;
        }
        if (x > maxX) {
            maxX = x;
        }
        if (y < minY) {
            minY = y;
        }
        if (y > maxY) {
            maxY = y;
        }
        if (z < minZ) {
            minZ = z;
        }
        if (z > maxZ) {
            maxZ = z;
        }
    }

    public void set(int x, int y, int z, String id) {
        set(x, y, z, Block.of(id));
    }

    public Block get(int x, int y, int z) {
        return blocks.get(key(x, y, z));
    }

    public boolean isAir(int x, int y, int z) {
        Block block = get(x, y, z);
        return block == null || block.isAir();
    }

    /**
     * 把这一格标成"要挖掉"。落地器会真的把它写成空气。
     *
     * <p>和 {@code set(..., null)} 的区别：那个只是不在场景里画这一格，
     * 世界里的石头还在。挖进地形必须用这个。
     */
    public void air(int x, int y, int z) {
        blocks.remove(key(x, y, z));
        air.add(key(x, y, z));
    }

    /** 这一格是不是被明确要求挖掉。 */
    public boolean isCleared(int x, int y, int z) {
        return air.contains(key(x, y, z));
    }

    /** 所有要挖掉的格子，给落地器用。 */
    public java.util.List<int[]> airCells() {
        java.util.List<int[]> out = new java.util.ArrayList<>(air.size());
        for (long k : air) {
            out.add(new int[] {xOf(k), yOf(k), zOf(k)});
        }
        return out;
    }

    /** 要挖掉的格子数。 */
    public int airCount() {
        return air.size();
    }

    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, Block block) {
        int ax = Math.min(x1, x2);
        int bx = Math.max(x1, x2);
        int ay = Math.min(y1, y2);
        int by = Math.max(y1, y2);
        int az = Math.min(z1, z2);
        int bz = Math.max(z1, z2);
        for (int x = ax; x <= bx; x++) {
            for (int y = ay; y <= by; y++) {
                for (int z = az; z <= bz; z++) {
                    set(x, y, z, block);
                }
            }
        }
    }

    public void carve(int x1, int y1, int z1, int x2, int y2, int z2) {
        fill(x1, y1, z1, x2, y2, z2, null);
    }

    /** 只要有方块就算，用于"这块地空不空"。 */
    public boolean anyBlockIn(int x1, int y1, int z1, int x2, int y2, int z2) {
        int ax = Math.min(x1, x2);
        int bx = Math.max(x1, x2);
        int ay = Math.min(y1, y2);
        int by = Math.max(y1, y2);
        int az = Math.min(z1, z2);
        int bz = Math.max(z1, z2);
        for (int x = ax; x <= bx; x++) {
            for (int y = ay; y <= by; y++) {
                for (int z = az; z <= bz; z++) {
                    if (!isAir(x, y, z)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public int size() {
        return blocks.size();
    }

    public boolean isEmpty() {
        return blocks.isEmpty();
    }

    public int minX() {
        return minX;
    }

    public int maxX() {
        return maxX;
    }

    public int minY() {
        return minY;
    }

    public int maxY() {
        return maxY;
    }

    public int minZ() {
        return minZ;
    }

    public int maxZ() {
        return maxZ;
    }

    /** 遍历用的条目。渲染器与落地器都只需要这个。 */
    public record Entry(int x, int y, int z, Block block) {
    }

    public List<Entry> entries() {
        List<Entry> list = new ArrayList<>(blocks.size());
        for (Map.Entry<Long, Block> entry : blocks.entrySet()) {
            long key = entry.getKey();
            int x = (int) (key >> 26) - 8192;
            int z = (int) ((key >> 11) & 0x7FFF) - 8192;
            int y = (int) (key & 0x7FF) - 256;
            list.add(new Entry(x, y, z, entry.getValue()));
        }
        return list;
    }

    /** 只保留"至少有一面朝着空气"的方块。渲染用，能省掉七成填充物。 */
    public List<Entry> surface() {
        List<Entry> list = new ArrayList<>(blocks.size() / 2);
        for (Map.Entry<Long, Block> entry : blocks.entrySet()) {
            long key = entry.getKey();
            int x = (int) (key >> 26) - 8192;
            int z = (int) ((key >> 11) & 0x7FFF) - 8192;
            int y = (int) (key & 0x7FF) - 256;
            Block block = entry.getValue();
            if (block.isAir()) {
                continue;
            }
            if (isAir(x, y + 1, z) || isAir(x + 1, y, z) || isAir(x, y, z + 1)
                    || isAir(x, y - 1, z) || isAir(x - 1, y, z) || isAir(x, y, z - 1)) {
                list.add(new Entry(x, y, z, block));
            }
        }
        return list;
    }

    /**
     * 转 90° 的整数倍，返回一个新的场景。
     *
     * <p>渲染器是固定的等轴视角，只有一个方向。要看背面、看侧面，就得把场景转过来。
     * 转的时候方块状态也要跟着转——楼梯的 {@code facing}、原木的 {@code axis}。
     * 只转坐标不转朝向的话，屋顶会变成一片横七竖八的半砖，比不转还糟。
     *
     * @param quarter 逆时针转几个 90°
     */
    public Scene rotated(int quarter) {
        int q = Math.floorMod(quarter, 4);
        if (q == 0) {
            return this;
        }
        Scene out = new Scene();
        for (Entry e : entries()) {
            int x = e.x();
            int z = e.z();
            for (int i = 0; i < q; i++) {
                int nx = z;
                int nz = -x;
                x = nx;
                z = nz;
            }
            out.set(x, e.y(), z, rotateBlock(e.block(), q));
        }
        return out;
    }

    private static Block rotateBlock(Block b, int q) {
        if (b.props().isEmpty()) {
            return b;
        }
        StringBuilder sb = new StringBuilder();
        for (String part : b.props().split(",")) {
            String[] kv = part.split("=", 2);
            if (kv.length != 2) {
                continue;
            }
            String k = kv[0].trim();
            String v = kv[1].trim();
            for (int i = 0; i < q; i++) {
                if (k.equals("facing")) {
                    v = switch (v) {
                        case "north" -> "west";
                        case "west" -> "south";
                        case "south" -> "east";
                        default -> "north";
                    };
                } else if (k.equals("axis")) {
                    v = v.equals("x") ? "z" : v.equals("z") ? "x" : v;
                }
            }
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(k).append('=').append(v);
        }
        return Block.of(b.id(), sb.toString());
    }

    public long key(int x, int y, int z) {
        if (x < -LIMIT || x > LIMIT || z < -LIMIT || z > LIMIT || y < -256 || y > 1791) {
            throw new IllegalArgumentException("坐标超出场景范围：" + x + "," + y + "," + z);
        }
        return ((long) (x + LIMIT) << 26) | ((long) (z + LIMIT) << 11) | (y + 256);
    }

    /** 从 {@link #key} 反解。位序必须与它一致：x 高、z 中（15 位）、y 低（11 位）。 */
    public static int xOf(long key) {
        return (int) (key >> 26) - LIMIT;
    }

    public static int zOf(long key) {
        return (int) ((key >> 11) & 0x7FFF) - LIMIT;
    }

    public static int yOf(long key) {
        return (int) (key & 0x7FF) - 256;
    }
}
