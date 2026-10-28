package io.github.vampireachao.vespera.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RegionBookTest {

    private static Region square(String id, int minX, int minZ, int size) {
        return new Region(id, id, "world", minX, 40, minZ, minX + size, 160, minZ + size,
                "minecraft:plains", "test", java.util.Set.of());
    }

    @Test
    @DisplayName("嵌套时取体积最小的那一块：站在学院里，你不是在城里")
    void nestedRegionWinsByVolume() {
        Region city = square("lumenfall", -200, -200, 400);
        Region college = square("college", 220, -160, 300);
        Region tower = square("tower", 260, -20, 20);
        RegionBook book = RegionBook.of(List.of(city, college, tower));

        assertEquals("college", book.at("world", 300, 70, 0).orElseThrow().id());
        assertEquals("tower", book.at("world", 265, 70, -15).orElseThrow().id());
        assertEquals("lumenfall", book.at("world", 0, 70, 0).orElseThrow().id());
        assertEquals("college", book.at("world", 221, 70, -159).orElseThrow().id());
    }

    @Test
    @DisplayName("不在任何区域里就是野外，不是报错")
    void outsideIsEmpty() {
        RegionBook book = RegionBook.of(List.of(square("lumenfall", -200, -200, 400)));
        assertTrue(book.at("world", 2000, 70, 2000).isEmpty());
        // 维度不对也不行——同一个坐标在下界是另一块地方
        assertTrue(book.at("world_nether", 0, 70, 0).isEmpty());
    }

    @Test
    @DisplayName("高度也算：山体的区域不会一路穿到基岩")
    void respectsHeight() {
        Region region = new Region("abbey", "修道院", "world", -900, 60, -700, -500, 220, -300,
                "minecraft:windswept_hills", "abbey", java.util.Set.of());
        assertTrue(region.contains("world", -700, 100, -500));
        assertFalse(region.contains("world", -700, 30, -500));
    }

    @Test
    @DisplayName("from/to 写反了也认，两个角就是两个角")
    void acceptsReversedCorners() {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("id", "moonring");
        raw.put("world", "world");
        raw.put("from", List.of(900, 140, 900));
        raw.put("to", List.of(300, 50, 300));
        raw.put("biome", "minecraft:dark_forest");
        raw.put("tags", List.of("night", "wolf"));

        Region region = Region.parse(raw);
        assertEquals(300, region.minX());
        assertEquals(900, region.maxX());
        assertTrue(region.contains("world", 500, 70, 500));
        assertTrue(region.has("wolf"));
    }

    @Test
    @DisplayName("一条坏记录不拖垮整张表")
    void brokenEntryDoesNotBreakTheBook() {
        Map<String, Object> good = new LinkedHashMap<>();
        good.put("id", "ok");
        good.put("world", "world");
        good.put("from", List.of(0, 0, 0));
        good.put("to", List.of(10, 10, 10));

        Map<String, Object> bad = new LinkedHashMap<>();
        bad.put("id", "bad");
        bad.put("world", "world");
        bad.put("from", "这里应该是一串数字");

        RegionBook book = RegionBook.parse(List.of(good, bad));
        assertEquals(2, book.all().size());
        assertEquals("ok", book.at("world", 5, 5, 5).orElseThrow().id());
        assertTrue(book.byId("bad").orElseThrow().has("broken"));
    }
}
