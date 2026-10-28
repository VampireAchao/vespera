package io.github.vampireachao.vespera.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vampireachao.vespera.core.CampusPlan.Kind;
import io.github.vampireachao.vespera.core.CampusPlan.Lot;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CampusPlanTest {

    @Test
    @DisplayName("没有两栋楼压在一起——钟楼贴着教学楼不算压")
    void noTwoBuildingsOverlap() {
        List<Lot> lots = CampusPlan.LOTS;
        for (int i = 0; i < lots.size(); i++) {
            for (int j = i + 1; j < lots.size(); j++) {
                Lot left = lots.get(i);
                Lot right = lots.get(j);
                // 锅炉房在地下，和地上的楼共用一个平面是应该的
                if (left.kind() == Kind.BOILER || right.kind() == Kind.BOILER) {
                    continue;
                }
                assertFalse(overlap(left, right),
                        left.name() + " 与 " + right.name() + " 重叠了");
            }
        }
    }

    @Test
    @DisplayName("所有楼都在围墙以内，校门除外（它就在墙上）")
    void everythingInsideTheWall() {
        for (Lot lot : CampusPlan.LOTS) {
            int minX = Math.min(lot.x1(), lot.x2());
            int maxX = Math.max(lot.x1(), lot.x2());
            int minZ = Math.min(lot.z1(), lot.z2());
            int maxZ = Math.max(lot.z1(), lot.z2());
            assertTrue(minX >= CampusPlan.WALL_MIN_X && maxX <= CampusPlan.WALL_MAX_X,
                    lot.name() + " 横向出了围墙");
            assertTrue(minZ >= CampusPlan.WALL_MIN_Z && maxZ <= CampusPlan.WALL_MAX_Z,
                    lot.name() + " 纵向出了围墙");
        }
    }

    @Test
    @DisplayName("每栋楼都有名字、有体量，且高度是正数（锅炉房除外，它往下挖）")
    void everyLotIsBuildable() {
        for (Lot lot : CampusPlan.LOTS) {
            assertFalse(lot.name().isBlank(), lot.id() + " 没有名字");
            assertTrue(lot.width() > 4 && lot.depth() > 4, lot.name() + " 太小了");
            if (lot.kind() == Kind.BOILER) {
                assertTrue(lot.height() > 0, "锅炉房的 height 记的是深度，也应当是正数");
            } else {
                assertTrue(lot.height() >= 8, lot.name() + " 太矮了");
            }
        }
    }

    @Test
    @DisplayName("平面图能画出来，且每栋楼都在图上留了名字")
    void svgContainsEveryName() {
        String svg = CampusPlan.svg();
        assertTrue(svg.startsWith("<svg"));
        for (Lot lot : CampusPlan.LOTS) {
            assertTrue(svg.contains(lot.name()), "图上找不到 " + lot.name());
        }
    }

    /** 只算真正的交叠：共用一条边不算。 */
    private static boolean overlap(Lot left, Lot right) {
        int x = Math.min(Math.max(left.x1(), left.x2()), Math.max(right.x1(), right.x2()))
                - Math.max(Math.min(left.x1(), left.x2()), Math.min(right.x1(), right.x2()));
        int z = Math.min(Math.max(left.z1(), left.z2()), Math.max(right.z1(), right.z2()))
                - Math.max(Math.min(left.z1(), left.z2()), Math.min(right.z1(), right.z2()));
        return x > 0 && z > 0;
    }
}
