package io.github.vampireachao.vespera.core.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vampireachao.vespera.core.voxel.Block;
import io.github.vampireachao.vespera.core.voxel.Scene;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 城的结构自检：只验"该在的东西在不在"，不验好不好看。
 *
 * <p>好看只能靠眼睛；但"城墙根本没盖"和"教堂盖到别出去"这两种失败，光看日志看不出来，
 * 却完全可以用坐标问出来。这几条断言就是替眼睛先看一遍。
 */
class MedievalCityTest {

    private static Scene scene;

    private static Scene scene() {
        if (scene == null) {
            scene = MedievalCity.build();
        }
        return scene;
    }

    @Test
    @DisplayName("城真的盖出来了：超过一百万个方块，且高度不出世界")
    void cityIsBig() {
        Scene s = scene();
        assertTrue(s.size() > 1_000_000, "方块太少，城没盖完：" + s.size());
        assertTrue(s.maxY() < 140, "有方块高过 140 格，说明某个尖顶失控了：" + s.maxY());
        assertTrue(s.minX() < -MedievalCity.HALF, "场景没有延伸到西墙外");
        assertTrue(s.maxZ() > MedievalCity.HALF, "场景没有延伸到南墙外（港区）");
    }

    @Test
    @DisplayName("四面城墙在：南墙、东墙、北墙、西墙各取一点")
    void wallsExist() {
        int h = MedievalCity.HALF;
        assertWall(40, 5, h);
        assertWall(h, 5, 40);
        assertWall(40, 5, -h);
        assertWall(-h, 5, 40);
    }

    private void assertWall(int x, int y, int z) {
        Block block = scene().get(x, y, z);
        assertNotNull(block, "城墙位置是空的：" + x + "," + z);
        String id = block.id();
        assertTrue(id.contains("stone_brick") || id.contains("mossy"), "城墙材质不对：" + id);
    }

    @Test
    @DisplayName("四座地标各就各位：喷泉、圣座塔、城堡主楼、市政厅钟塔")
    void landmarksExist() {
        assertNotNull(scene().get(66, 5, 0), "金字塔塔身没有立起来");
        assertNotNull(scene().get(0, 64, 0), "金字塔顶的祭坛没有立起来");
        assertNotNull(scene().get(-46, 20, -260), "王座大厅没有立起来");
        assertNotNull(scene().get(-268, 30, -268), "城堡主楼没有立起来");
        assertNotNull(scene().get(-154, 30, -72), "市政厅钟塔没有立起来");
    }

    @Test
    @DisplayName("城外也有东西：南港的水、竞技场的沙地")
    void outskirtsExist() {
        assertEquals("minecraft:water", scene().get(-170, -1, 350).id(), "运河没有水");
        assertNotNull(scene().get(185, -1, 185), "角斗场沙地没有方块");
        assertNotNull(scene().get(-420, 20, 100), "风车没有立起来");
    }

    @Test
    @DisplayName("街道表能读：主街十六条格宽，其余按环递减")
    void streetsAreDeclared() {
        assertTrue(MedievalCity.STREETS.size() >= 12);
        assertTrue(MedievalCity.STREETS.stream().anyMatch(street -> street.width() == 16),
                "没有一条十六格宽的主街");
    }

    @Test
    @DisplayName("分区表覆盖全部有意设计的地方，没有重复 id")
    void districtsAreUnique() {
        long unique = MedievalCity.DISTRICTS.stream().map(MedievalCity.District::id).distinct().count();
        assertEquals(MedievalCity.DISTRICTS.size(), unique, "分区 id 有重复");
        assertTrue(MedievalCity.DISTRICTS.size() >= 14);
    }
}
