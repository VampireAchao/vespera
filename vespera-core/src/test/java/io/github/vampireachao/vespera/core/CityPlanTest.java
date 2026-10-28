package io.github.vampireachao.vespera.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vampireachao.vespera.core.CityPlan.Kind;
import io.github.vampireachao.vespera.core.CityPlan.Lot;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CityPlanTest {

    /** 这些都是"有体积"的建筑，彼此不能压。广场与市集是地面，另算。 */
    private static final Set<Kind> SOLID = Set.of(Kind.TOWER, Kind.CATHEDRAL, Kind.GUILD, Kind.HOUSE);

    @Test
    @DisplayName("主城没有两栋实心建筑压在一起")
    void noTwoBuildingsOverlap() {
        List<Lot> lots = CityPlan.LOTS.stream().filter(lot -> SOLID.contains(lot.kind())).toList();
        for (int i = 0; i < lots.size(); i++) {
            for (int j = i + 1; j < lots.size(); j++) {
                assertFalse(overlap(lots.get(i), lots.get(j)),
                        lots.get(i).name() + " 与 " + lots.get(j).name() + " 重叠了");
            }
        }
    }

    @Test
    @DisplayName("建筑不侵占广场——广场是城的中心，不该盖房子")
    void buildingsKeepOffThePlaza() {
        Lot plaza = CityPlan.lot("plaza");
        for (Lot lot : CityPlan.LOTS) {
            if (!SOLID.contains(lot.kind())) {
                continue;
            }
            assertFalse(overlap(plaza, lot), lot.name() + " 压进了广场");
        }
    }

    @Test
    @DisplayName("一切都在城墙以内，城门除外（它们就在墙上）")
    void everythingInsideTheWall() {
        for (Lot lot : CityPlan.LOTS) {
            if (lot.kind() == Kind.GATE) {
                continue;
            }
            int minX = Math.min(lot.x1(), lot.x2());
            int maxX = Math.max(lot.x1(), lot.x2());
            int minZ = Math.min(lot.z1(), lot.z2());
            int maxZ = Math.max(lot.z1(), lot.z2());
            assertTrue(minX >= -CityPlan.HALF && maxX <= CityPlan.HALF, lot.name() + " 横向出墙");
            assertTrue(minZ >= -CityPlan.HALF && maxZ <= CityPlan.HALF, lot.name() + " 纵向出墙");
        }
    }

    @Test
    @DisplayName("十五栋民居，每栋都装得下人")
    void housesAreHabitable() {
        assertEquals(15, CityPlan.houses().size());
        for (Lot house : CityPlan.houses()) {
            assertTrue(house.width() >= 10 && house.depth() >= 8, house.id() + " 太小了");
            assertTrue(house.height() >= 9, house.id() + " 太矮了");
        }
    }

    @Test
    @DisplayName("出生点在南门里，落在主街上")
    void spawnIsOnTheMainStreet() {
        int[] spawn = CityPlan.spawn();
        assertTrue(Math.abs(spawn[0]) <= CityPlan.ROAD_HALF, "出生点不在主街上");
        assertTrue(spawn[1] > 0 && spawn[1] < CityPlan.HALF, "出生点应当在南半城、城墙以内");
    }

    @Test
    @DisplayName("平面图能画出来，且主要建筑都标了名字")
    void svgContainsLandmarks() {
        String svg = CityPlan.svg();
        assertTrue(svg.startsWith("<svg"));
        for (String name : List.of("钟楼", "第九时圣座", "灰烬公会", "中央广场")) {
            assertTrue(svg.contains(name), "图上找不到 " + name);
        }
    }

    private static void assertEquals(int expected, int actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }

    private static boolean overlap(Lot left, Lot right) {
        int x = Math.min(Math.max(left.x1(), left.x2()), Math.max(right.x1(), right.x2()))
                - Math.max(Math.min(left.x1(), left.x2()), Math.min(right.x1(), right.x2()));
        int z = Math.min(Math.max(left.z1(), left.z2()), Math.max(right.z1(), right.z2()))
                - Math.max(Math.min(left.z1(), left.z2()), Math.min(right.z1(), right.z2()));
        return x > 0 && z > 0;
    }
}
