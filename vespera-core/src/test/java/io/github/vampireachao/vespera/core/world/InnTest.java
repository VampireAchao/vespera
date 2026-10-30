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
 * 客栈能不能"住人"。
 *
 * <p>一座车马店有三条必须成立的动线：<b>街上进门能坐下</b>、<b>能上楼进房间</b>、
 * <b>车马从拱门进院、马能进厩</b>。这三条哪一条断了，图上看不出来——图上一样有门、
 * 一样有楼梯，只有把脚放上去走一遍才知道。
 *
 * <p>所以这里不验"好不好看"，只验"走不走得到"。
 */
class InnTest {

    /** 用地：x −76..−26、z 246..282。审计盒比它大一圈，好让街面也在里面。 */
    private static final int X1 = -82;
    private static final int X2 = -22;
    private static final int Y1 = -4;
    private static final int Y2 = 16;
    private static final int Z1 = 242;
    private static final int Z2 = 290;

    private static Scene scene;

    private static Scene scene() {
        if (scene == null) {
            scene = MedievalCity.build();
        }
        return scene;
    }

    private static WalkAudit fromStreet() {
        WalkAudit au = new WalkAudit(scene(), X1, Y1, Z1, X2, Y2, Z2);
        assertTrue(au.canStand(-28, 0, 262), "客栈门口站不住人");
        au.walk(-28, 0, 262);
        return au;
    }

    @Test
    @DisplayName("街上推门进大堂：柜台、长桌、壁炉都在同一层")
    void theStreetDoorOpensIntoTheHall() {
        Scene s = scene();
        assertEquals("minecraft:dark_oak_door", s.get(-30, 0, 262).id(), "正门不是门扇");
        assertTrue(s.get(-30, 0, 262).id().endsWith("_door"), "正门那一格不是门");
        WalkAudit au = fromStreet();
        List<String> bad = new ArrayList<>();
        if (!au.reached(-36, 0, 258)) {
            bad.add("大堂中段");
        }
        if (!au.reached(-33, 0, 251)) {
            bad.add("柜台后面");
        }
        if (!au.reached(-36, 0, 274)) {
            bad.add("南侧私厅（壁炉前）");
        }
        assertTrue(bad.isEmpty(), "进了门走不到：" + String.join("、", bad));
    }

    @Test
    @DisplayName("楼梯通到三楼：二层走廊、每间客房、三层阁楼都走得到")
    void upstairsIsActuallyUpstairs() {
        WalkAudit au = fromStreet();
        assertTrue(au.reached(-40, 6, 267), "上不到二层走廊");
        List<String> bad = new ArrayList<>();
        int[] doorZ = {253, 260, 267, 274};
        for (int z : doorZ) {
            if (!au.reached(-35, 6, z)) {
                bad.add("客房 z=" + z);
            }
        }
        assertTrue(bad.isEmpty(), "这几位客人进不了房：" + String.join("、", bad));
        assertTrue(au.reached(-40, 11, 270), "上不到三层");
        assertTrue(au.reached(-36, 11, 257), "三层第一间进不去");
        assertTrue(au.reached(-36, 11, 275), "三层通铺进不去");
    }

    @Test
    @DisplayName("拱门通车：从街上一直走到院子、后厨、马厩")
    void theArchwayCarriesYouToTheYard() {
        Scene s = scene();
        // 拱门三格宽、四格高，中间那一列要通到顶
        for (int y = 0; y <= 2; y++) {
            for (int z = 268; z <= 270; z++) {
                assertTrue(s.get(-30, y, z) == null, "东墙拱门没凿开：" + y + "," + z);
            }
        }
        WalkAudit au = fromStreet();
        assertTrue(au.reached(-55, 0, 272), "穿不过院子");
        assertTrue(au.reached(-54, 0, 256), "进不了后厨");
        assertTrue(au.reached(-70, 0, 265), "进不了马厩");
    }

    @Test
    @DisplayName("二层走廊那道门通到院子回廊：回廊是客房的走廊，不是装饰")
    void theGalleryIsWalkable() {
        WalkAudit au = fromStreet();
        assertTrue(au.reached(-45, 6, 265), "走廊门出去踩不到回廊");
        assertTrue(au.reached(-45, 6, 277), "回廊南段走不到");
        assertTrue(au.reached(-55, 6, 263), "回廊北段走不到");
        // 回廊有上有下：院里那道梯子能走上来
        assertTrue(au.reached(-52, 0, 276), "院里的上廊梯底下站不住");
    }

    @Test
    @DisplayName("马厩真的分出了栏位：至少五间，每间有草有水")
    void theStableHasStalls() {
        Scene s = scene();
        int fences = 0;
        for (int z = 252; z <= 280; z++) {
            if (s.get(-74, 1, z) != null && s.get(-74, 1, z).id().endsWith("fence")) {
                fences++;
            }
        }
        assertTrue(fences >= 4, "马厩只有 " + fences + " 道分栏");
        assertEquals("minecraft:water", s.get(-69, 0, 262).id(), "水槽里没有水");
    }
}
