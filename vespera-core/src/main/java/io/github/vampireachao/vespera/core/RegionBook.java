package io.github.vampireachao.vespera.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 全部区域，以及"某个坐标落在哪一块"的答案。
 *
 * <p>规则只有一条：**体积最小的那个胜出。** 因为区域是嵌套的——学院校区在流明陨城里面，
 * 钟塔又在校区里面。走到钟楼底层时，玩家同时在三个区域里，而他真正需要的是最里面那一个
 * （只有它知道"这里是静音区"）。
 *
 * <p>不做"后定义的覆盖先定义的"：那种规则在配置里看不出来，改一行会静默地影响别处。
 * 体积是坐标的固有属性，谁都能算，谁都能验。
 */
public final class RegionBook {

    public static final RegionBook EMPTY = new RegionBook(List.of());

    private final List<Region> regions;

    private RegionBook(List<Region> regions) {
        this.regions = List.copyOf(regions);
    }

    public static RegionBook of(List<Region> regions) {
        return regions == null || regions.isEmpty() ? EMPTY : new RegionBook(regions);
    }

    /** 从 {@code regions:} 列表读。坏掉一条不影响其余的——一张地图不该因为一个坐标写错就整块失效。 */
    public static RegionBook parse(List<Map<String, ?>> maps) {
        List<Region> parsed = new ArrayList<>();
        for (Map<String, ?> map : maps) {
            try {
                parsed.add(Region.parse(map));
            } catch (RuntimeException e) {
                // 留一条带 broken 标记的占位：名字还在，坐标全是 0，永远不会被匹配到。
                // 这样日志里能点名是哪一条坏了，而其余区域照常可用。
                String id = map.get("id") == null ? "（无名）" : String.valueOf(map.get("id"));
                parsed.add(new Region(id, "（读不出来）", String.valueOf(map.get("world")),
                        0, 0, 0, 0, 0, 0, null, "broken", java.util.Set.of("broken")));
            }
        }
        return new RegionBook(parsed);
    }

    public Optional<Region> at(String world, double x, double y, double z) {
        return regions.stream()
                .filter(region -> !region.has("broken") && region.contains(world, x, y, z))
                .min(Comparator.comparingLong(Region::volume));
    }

    public Optional<Region> byId(String id) {
        return regions.stream().filter(region -> region.id().equals(id)).findFirst();
    }

    public List<Region> all() {
        return regions;
    }

    public boolean isEmpty() {
        return regions.isEmpty();
    }

    /** 某个维度里有几块。给 doctor 用。 */
    public long countIn(String world) {
        return regions.stream().filter(region -> region.world().equals(world)).count();
    }
}
