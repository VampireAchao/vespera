package io.github.vampireachao.vespera.core.world;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vampireachao.vespera.core.voxel.Scene;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 金字塔里面到底能不能走。
 *
 * <p>塔是"分层"的，分层这件事只有把脚放上去才算数：楼板要连成一片、大梯要从
 * 地面上起步、每层的房门要推得开。所以这一条测试从塔外一路走到顶坛。
 */
class TempleTest {

    @Test
    @DisplayName("从塔外走进底层大厅，再沿中轴大梯一路爬到顶坛")
    void theTempleIsClimbable() {
        Scene s = MedievalCity.build();
        WalkAudit au = new WalkAudit(s, -80, -2, -80, 80, 62, 80);
        // 东门外（南面是上顶坛的大台阶，门开在东侧）
        assertTrue(au.canStand(72, 0, 0), "塔外站不住人");
        au.walk(72, 0, 0);
        assertTrue(au.reached(58, 0, 0), "从东门进不去底层大厅");
        assertTrue(au.reached(0, 0, 40), "底层大厅走不深");
        // 大梯：每上一层都在
        for (int t = 1; t <= 6; t++) {
            int y = t * 8;
            int z = 62 - 8 * t;
            assertTrue(au.reached(-10, y + 1, z), "第 " + t + " 层没有走上去（y=" + y + " z=" + z + "）");
        }
        // 顶坛：站在祭台前
        assertTrue(au.reached(-10, 57, 6), "上去以后没站到顶坛上");
        assertTrue(au.reached(0, 57, 8), "从梯口走不到祭坛前");
    }
}
