package io.github.vampireachao.vespera.core.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vampireachao.vespera.core.voxel.Block;
import io.github.vampireachao.vespera.core.voxel.Scene;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 角斗场：贵宾席与主持台到底怎么上去。
 *
 * <p>这两座台子以前都在看台顶上，只能沿十八排台阶走上去，走上去还看不见门——
 * 所以"贵宾席怎么进"这个问题，图上永远回答不了。现在它们都降到场边，
 * 各有一道七级石梯从沙地直上，这里就是把那两道梯子走一遍。
 */
class ColosseumTest {

    private static final int CX = 185;
    private static final int CZ = 193;

    private static Scene scene;

    private static Scene scene() {
        if (scene == null) {
            scene = MedievalCity.build();
        }
        return scene;
    }

    /** 只审场内这一圈：从沙地到看台顶。 */
    private static WalkAudit fromArena(int x, int z) {
        WalkAudit au = new WalkAudit(scene(), CX - 90, -4, CZ - 80, CX + 90, 24, CZ + 80);
        assertTrue(au.canStand(x, 0, z), "沙地上站不住人：" + x + "," + z);
        au.walk(x, 0, z);
        return au;
    }

    @Test
    @DisplayName("贵宾席：从沙地沿北侧石梯直上，进得去包厢")
    void theVipBoxHasItsOwnStair() {
        Scene s = scene();
        // 梯子第一级就在沙地边上
        assertNotNull(s.get(CX + 40, 0, CZ + 9), "上座梯第一级不在");
        WalkAudit au = fromArena(CX + 38, CZ + 9);
        List<String> bad = new ArrayList<>();
        if (!au.reached(CX + 43, 4, CZ + 9)) {
            bad.add("梯子中段");
        }
        if (!au.reached(CX + 46, 7, CZ + 9)) {
            bad.add("梯顶");
        }
        if (!au.reached(CX + 50, 7, CZ)) {
            bad.add("包厢里");
        }
        assertTrue(bad.isEmpty(), "贵宾席上不去：" + String.join("、", bad));
    }

    @Test
    @DisplayName("主持台：同样从沙地上得去，且讲台朝东对着场子")
    void theHostStandFacesTheArena() {
        Scene s = scene();
        WalkAudit au = fromArena(CX - 38, CZ + 9);
        assertTrue(au.reached(CX - 46, 7, CZ + 9), "主持台的梯子上不去");
        assertTrue(au.reached(CX - 50, 7, CZ), "进不了主持台");
        Block lectern = s.get(CX - 46, 8, CZ);
        assertNotNull(lectern, "讲台不在");
        assertEquals("minecraft:lectern", lectern.id(), "那不是讲台");
        assertTrue(lectern.props().contains("facing=east"),
                "讲台方向不对：" + lectern.props() + "——他要对着场上喊，不是对着城墙");
    }

    @Test
    @DisplayName("包厢是石头台子不是瓦房：顶上没有坡面，前沿有金栏杆")
    void theBoxIsATribune() {
        Scene s = scene();
        // 平顶：顶面那一层是石头，不是楼梯（楼梯＝坡屋顶）
        Block roof = s.get(CX + 50, 13, CZ);
        assertNotNull(roof, "包厢没有顶");
        assertTrue(!roof.id().endsWith("_stairs"), "包厢盖成了坡屋顶：" + roof.id());
        // 前沿金栏杆
        boolean gold = false;
        for (int z = CZ - 8; z <= CZ + 8; z++) {
            Block b = s.get(CX + 40, 7, z);
            if (b != null && b.id().contains("gold_block")) {
                gold = true;
                break;
            }
        }
        assertTrue(gold, "前沿没有金栏杆");
    }

    @Test
    @DisplayName("外立面三层拱廊：券洞里是暗的，而且真的凹进去两格")
    void theArcadesAreHollow() {
        Scene s = scene();
        int backs = 0;
        int open = 0;
        // 东立面第二层扫一片：椭圆是弯的，不能拿一条直线去量
        for (int x = CX + 70; x <= CX + 90; x++) {
            for (int z = CZ - 25; z <= CZ + 25; z++) {
                Block back = s.get(x, 10, z);
                if (back == null || !back.id().contains("deepslate")) {
                    continue;
                }
                backs++;
                if (s.get(x + 2, 10, z) == null) {
                    open++;
                }
            }
        }
        assertTrue(backs > 20, "券洞里没有暗色背板，看不出深度：" + backs);
        assertTrue(open > 20, "券洞没有真的凿穿外墙：" + open);
    }
}
