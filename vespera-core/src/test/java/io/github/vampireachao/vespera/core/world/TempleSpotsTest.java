package io.github.vampireachao.vespera.core.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vampireachao.vespera.core.voxel.Scene;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 圣堂里的五处服务点与那座地牢，到底能不能用。
 *
 * <p>这些点同时被两拨代码用着：建城时在这儿砌一块箱子/讲经台，插件据此判断
 * "玩家点的是不是那个箱子"。所以这里查两件事——<b>东西在不在这个坐标上</b>，
 * 以及<b>人走不走得到</b>。少任何一样，玩家看到的就是"点了没反应"。
 */
class TempleSpotsTest {

    private static Scene scene() {
        return MedievalCity.build();
    }

    @Test
    @DisplayName("四处服务物件都在自己该在的坐标上")
    void markersAreWhereTheCodeSaysTheyAre() {
        Scene s = scene();
        assertEquals("minecraft:chest", s.get(TempleSpots.WAREHOUSE_CHEST.x(), TempleSpots.WAREHOUSE_CHEST.y(),
                TempleSpots.WAREHOUSE_CHEST.z()).id(), "仓库没有箱子");
        assertEquals("minecraft:lectern", s.get(TempleSpots.LIBRARY_LECTERN.x(), TempleSpots.LIBRARY_LECTERN.y(),
                TempleSpots.LIBRARY_LECTERN.z()).id(), "藏书阁没有讲经台");
        assertNotNull(s.get(TempleSpots.COUNCIL_SEAT.x(), TempleSpots.COUNCIL_SEAT.y(),
                TempleSpots.COUNCIL_SEAT.z()), "议事厅没有座位");
        assertEquals("minecraft:chest", s.get(TempleSpots.DUNGEON_CHEST.x(), TempleSpots.DUNGEON_CHEST.y(),
                TempleSpots.DUNGEON_CHEST.z()).id(), "地牢没有箱子");
    }

    @Test
    @DisplayName("从东门进塔，一路走到地牢；地牢里挖得到 8 个以上的红石")
    void theDungeonIsRealAndDiggable() {
        Scene s = scene();
        WalkAudit au = new WalkAudit(s, -80, -12, -80, 80, 62, 80);
        assertTrue(au.canStand(72, 0, 0), "塔外站不住");
        au.walk(72, 0, 0);

        // 沿梯下到地牢地面（y=-8）
        assertTrue(au.reached(-25, -8, 23), "下不到地牢梯底");
        int floor = TempleSpots.DUNGEON_FLOOR;
        int cx = (TempleSpots.DUNGEON_X1 + TempleSpots.DUNGEON_X2) / 2;
        int cz = (TempleSpots.DUNGEON_Z1 + TempleSpots.DUNGEON_Z2) / 2;
        assertTrue(au.reached(cx, floor + 1, cz), "走不到地牢中间");

        // 血石：一块红石块挖下来 9 个红石，转职要 8 个
        List<String> veins = new ArrayList<>();
        for (int x = TempleSpots.DUNGEON_X1 - 1; x <= TempleSpots.DUNGEON_X2 + 1; x++) {
            for (int z = TempleSpots.DUNGEON_Z1 - 1; z <= TempleSpots.DUNGEON_Z2 + 1; z++) {
                for (int y = floor; y <= floor + 5; y++) {
                    var b = s.get(x, y, z);
                    if (b != null && b.id().equals("minecraft:redstone_block")) {
                        veins.add(x + "," + y + "," + z);
                    }
                }
            }
        }
        assertTrue(veins.size() >= 8, "地牢里只有 " + veins.size() + " 块血石，不够 8 个红石");
    }

    @Test
    @DisplayName("五处服务点：人都站得下，玩家都走得到")
    void everyServicePointIsReachable() {
        Scene s = scene();
        WalkAudit au = new WalkAudit(s, -80, -12, -80, 80, 62, 80);
        au.walk(72, 0, 0);
        List<String> bad = new ArrayList<>();
        for (TempleSpots.Post spot : TempleSpots.POSTS) {
            assertTrue(au.canStand(spot.x(), spot.y(), spot.z()),
                    spot.where() + " 站不下人（那儿是实心的）");
            if (!au.reached(spot.x(), spot.y(), spot.z())) {
                bad.add(spot.where());
            }
        }
        assertTrue(bad.isEmpty(), "这几处走不到：" + String.join("；", bad));
    }
}
