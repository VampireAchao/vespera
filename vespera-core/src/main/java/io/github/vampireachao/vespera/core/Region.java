package io.github.vampireachao.vespera.core;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 地图上的一块地方：一个长方体，一个生物群系，几个标签。
 *
 * <p>它只回答一个问题——"玩家此刻在哪"。这个问题看起来简单，实际是这套地图方案的地基：
 * 区域权限、PvP 开关、下界门定向、BGM、刷怪规则、剧情触发，全都要先知道答案。
 *
 * <p>坐标用闭区间（含边界），因为人写配置时想的是"从 -150 到 150"，而不是"从 -150 到 149"。
 */
public record Region(
        String id,
        String name,
        String world,
        int minX,
        int minY,
        int minZ,
        int maxX,
        int maxY,
        int maxZ,
        String biome,
        String kind,
        Set<String> tags) {

    public Region {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("区域必须有 id");
        }
        // 反着写也是合法的：from/to 只表示两个角
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            int x1 = Math.min(minX, maxX);
            int x2 = Math.max(minX, maxX);
            int y1 = Math.min(minY, maxY);
            int y2 = Math.max(minY, maxY);
            int z1 = Math.min(minZ, maxZ);
            int z2 = Math.max(minZ, maxZ);
            minX = x1;
            maxX = x2;
            minY = y1;
            maxY = y2;
            minZ = z1;
            maxZ = z2;
        }
        tags = tags == null ? Set.of() : Set.copyOf(tags);
        name = name == null || name.isBlank() ? id : name;
        kind = kind == null ? "wild" : kind;
    }

    public boolean contains(String world, double x, double y, double z) {
        if (world == null || !world.equals(this.world)) {
            return false;
        }
        return x >= minX && x <= maxX + 1
                && y >= minY && y <= maxY + 1
                && z >= minZ && z <= maxZ + 1;
    }

    /** 体积。判定嵌套时用它选"最里面那一层"。 */
    public long volume() {
        return (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }

    public boolean has(String tag) {
        return tags.contains(tag);
    }

    /**
     * 从一份 YAML 映射读一个区域。
     *
     * <pre>
     * - id: lumenfall
     *   name: 流明陨城
     *   world: world
     *   from: [-150, 40, -150]
     *   to: [150, 140, 150]
     *   biome: plains
     *   kind: city
     *   tags: [safe, trade]
     * </pre>
     */
    public static Region parse(Map<String, ?> map) {
        List<?> from = asList(map.get("from"), "from");
        List<?> to = asList(map.get("to"), "to");
        return new Region(
                String.valueOf(map.get("id")),
                map.get("name") == null ? null : String.valueOf(map.get("name")),
                String.valueOf(map.get("world")),
                number(from.get(0)), number(from.get(1)), number(from.get(2)),
                number(to.get(0)), number(to.get(1)), number(to.get(2)),
                map.get("biome") == null ? null : String.valueOf(map.get("biome")),
                map.get("kind") == null ? null : String.valueOf(map.get("kind")),
                tags(map.get("tags")));
    }

    private static List<?> asList(Object raw, String where) {
        if (!(raw instanceof List<?> list) || list.size() < 3) {
            throw new IllegalArgumentException(where + " 必须是三个数字，收到 " + raw);
        }
        return list;
    }

    private static int number(Object raw) {
        if (raw instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(raw).trim());
    }

    private static Set<String> tags(Object raw) {
        if (raw == null) {
            return Set.of();
        }
        Set<String> tags = new LinkedHashSet<>();
        if (raw instanceof List<?> list) {
            list.forEach(each -> tags.add(String.valueOf(each)));
        } else {
            tags.add(String.valueOf(raw));
        }
        return tags;
    }
}
