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
 * 天平与钥匙：街上进得来、柜台前站得住、铁门后下得去、账房上得去。
 *
 * <p>银行是"办一件事"的地方，所以它只需要三条动线成立：<b>客户走到柜台</b>、
 * <b>职员穿过铁门下库</b>、<b>账房上二楼</b>。三条断一条，这栋楼就是个摆设。
 */
class BankTest {

    /** 正门在北墙，两格宽。 */
    private static final int DOOR_X = 43;
    private static final int Z1 = 123;
    private static final int DOOR_Z = 135;

    private static Scene scene;

    private static Scene scene() {
        if (scene == null) {
            scene = MedievalCity.build();
        }
        return scene;
    }

    private static WalkAudit fromStreet() {
        WalkAudit au = new WalkAudit(scene(), 10, -8, 108, 64, 14, 152);
        assertTrue(au.canStand(DOOR_X, 0, Z1 - 6), "银行门前的广场上站不住人");
        au.walk(DOOR_X, 0, Z1 - 6);
        return au;
    }

    @Test
    @DisplayName("正门是真的双开门，不是凿的洞")
    void theDoorIsADoor() {
        Scene s = scene();
        for (int x = DOOR_X; x <= DOOR_X + 1; x++) {
            var b = s.get(x, 0, Z1);
            assertNotNull(b, "门洞那一格是空的：" + x);
            assertTrue(b.id().endsWith("_door"), "正门那一格不是门扇：" + b.id());
        }
    }

    @Test
    @DisplayName("街上进来，一路走到柜台前；柜台上方有铁栅窗口")
    void youCanWalkToTheCounter() {
        Scene s = scene();
        WalkAudit au = fromStreet();
        List<String> bad = new ArrayList<>();
        if (!au.reached(43, 0, 130)) {
            bad.add("大厅中段");
        }
        if (!au.reached(54, 0, 130)) {
            bad.add("柜台前");
        }
        if (!au.reached(54, 0, 141)) {
            bad.add("典当那一头");
        }
        assertTrue(bad.isEmpty(), "进了门走不到：" + String.join("、", bad));
        // 柜台窗口：铁栅那一列要空出来
        int bars = 0;
        for (int z = 128; z <= 142; z++) {
            var b = s.get(55, 3, z);
            if (b != null && b.id().contains("iron_bars")) {
                bars++;
            }
        }
        assertTrue(bars >= 3, "柜台上一格铁栅都没有：" + bars);
    }

    @Test
    @DisplayName("柜台正中的铁门后面是向下的石梯，一直下到库房")
    void theVaultIsUnderTheCounter() {
        Scene s = scene();
        var gate = s.get(55, 0, DOOR_Z);
        assertNotNull(gate, "铁门不在");
        assertEquals("minecraft:iron_door", gate.id(), "库房门不是铁的：" + gate.id());
        WalkAudit au = fromStreet();
        assertTrue(au.reached(56, 0, DOOR_Z), "推不开铁门");
        assertTrue(au.reached(56, -5, 141), "石梯下不到底");
        assertTrue(au.reached(50, -5, 140), "库房里走不进去");
        // 库房真的是空的，不是"图上有洞、进服是实心"
        assertTrue(s.get(50, -3, 140) == null, "库房没挖开");
        assertNotNull(s.get(50, -6, 140), "库房没有地板");
    }

    @Test
    @DisplayName("西墙那道直跑上二楼，两间办公室都进得去")
    void upstairsIsReachable() {
        WalkAudit au = fromStreet();
        assertTrue(au.reached(25, 8, 125), "上不到二层");
        List<String> bad = new ArrayList<>();
        if (!au.reached(32, 8, 128)) {
            bad.add("总管室");
        }
        if (!au.reached(32, 8, 142)) {
            bad.add("首席银行家室");
        }
        assertTrue(bad.isEmpty(), "上去了进不了门：" + String.join("、", bad));
    }
}
