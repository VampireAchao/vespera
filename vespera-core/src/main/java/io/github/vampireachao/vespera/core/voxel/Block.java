package io.github.vampireachao.vespera.core.voxel;

import java.util.Locale;

/**
 * 一个方块：材质 id，加上可选的方块状态。
 *
 * <p>把状态写进字符串，是为了让几何数据不必依赖 Bukkit。{@code facing=east,half=bottom}
 * 这样的写法可以直接交给 {@code Bukkit.createBlockData}，也可以在渲染器里被解析成
 * 一个斜面——同一份数据，游戏里是楼梯，图上也是楼梯。
 *
 * @param id    {@code minecraft:spruce_stairs}
 * @param props 形如 {@code facing=east,half=bottom}，可为空串
 */
public record Block(String id, String props) {

    public static Block of(String id) {
        return new Block(id, "");
    }

    public static Block of(String id, String props) {
        return new Block(id, props == null ? "" : props);
    }

    public boolean isAir() {
        return id.equals("minecraft:air") || id.equals("minecraft:cave_air");
    }

    /** 交给 Bukkit 的那一串。 */
    public String bukkit() {
        return props.isEmpty() ? id : id + "[" + props + "]";
    }

    public String shortName() {
        int colon = id.indexOf(':');
        return colon < 0 ? id : id.substring(colon + 1);
    }

    public boolean has(String key, String value) {
        String needle = key + "=" + value;
        for (String part : props.split(",")) {
            if (part.trim().equalsIgnoreCase(needle)) {
                return true;
            }
        }
        return false;
    }

    // -- 常用形状的构造：让生成器读起来像在砌墙，而不是在拼字符串 --------------

    public static Block stairs(String id, String facing) {
        return of(id, "facing=" + facing.toLowerCase(Locale.ROOT) + ",half=bottom");
    }

    public static Block slab(String id, String type) {
        return of(id, "type=" + type.toLowerCase(Locale.ROOT));
    }

    public static Block log(String id, String axis) {
        return of(id, "axis=" + axis.toLowerCase(Locale.ROOT));
    }

    public static Block pane(String id) {
        return of(id, "waterlogged=false");
    }

    @Override
    public String toString() {
        return bukkit();
    }
}
