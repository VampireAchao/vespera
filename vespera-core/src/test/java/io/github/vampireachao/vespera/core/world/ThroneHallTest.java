package io.github.vampireachao.vespera.core.world;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vampireachao.vespera.core.voxel.Scene;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 从圣座走到王座厅。
 *
 * <p>这一路上有三道台地、三道台阶、一道门廊。图上看着都在，脚放上去才知道
 * 有没有一级被门廊的地板压住——上一版就是那样：前五级能走，第六级一头撞在
 * 门廊底下，整条路等于断在最后三格。
 */
class ThroneHallTest {

    @Test
    @DisplayName("从圣座北侧一路走到王座厅，三道台地都能上，最后进得了大厅")
    void walkFromPyramidToThrone() {
        Scene s = MedievalCity.build();
        WalkAudit au = new WalkAudit(s, -60, -2, -300, 60, 26, -60);
        assertTrue(au.canStand(0, 0, -76), "圣座北侧站不住人");
        au.walk(0, 0, -76);

        // 三道台地：第一级台面 y=0，第二级 y=5，第三级 y=10
        assertTrue(au.reached(0, 1, -160), "上不了第一道台地");
        assertTrue(au.reached(0, 6, -200), "上不了第二道台地");
        assertTrue(au.reached(0, 11, -240), "上不了第三道台地");

        // 门廊与大厅：站在门廊地面（y=10 之上）与大厅地面（y=11 之上）
        assertTrue(au.reached(0, 11, -242), "走不到门廊");
        assertTrue(au.reached(0, 12, -250), "推门进不了大厅");
        assertTrue(au.reached(0, 13, -263), "走不上王座龛的五级台");
        assertNotNull(s.get(0, 17, -274), "金座不在龛里");
    }
}
