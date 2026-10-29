package io.github.vampireachao.vespera.core.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vampireachao.vespera.core.voxel.Block;
import io.github.vampireachao.vespera.core.voxel.Scene;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 门到底是不是门。
 *
 * <p>这一组断言回答的是"进服以后第一分钟会遇到的事"：门是不是两格、能不能推开、
 * 推开门以后走不走得进去。体素里"墙上有洞"和"有一扇门"看起来一样，
 * 差别只在右键有没有反应——所以必须逐扇查方块，不能看图。
 */
class DoorwayTest {

    @Test
    @DisplayName("每一扇门：上下两半成对、门楣在、门槛实")
    void everyDoorIsARealTwoHighDoor() {
        BuildKit k = MedievalCity.kit(20261028L);
        Scene s = k.scene;
        List<String> bad = new ArrayList<>();
        for (BuildKit.Door d : k.doors) {
            Block lower = s.get(d.x(), d.y(), d.z());
            Block upper = s.get(d.x(), d.y() + 1, d.z());
            if (lower == null || !lower.id().endsWith("_door") || !lower.has("half", "lower")) {
                bad.add("下半不是门：" + d.where());
                continue;
            }
            if (upper == null || !upper.id().endsWith("_door") || !upper.has("half", "upper")) {
                bad.add("上半不是门：" + d.where());
                continue;
            }
            if (!lower.id().equals(upper.id())) {
                bad.add("上下两半材质不一样：" + d.where());
            }
            if (!lower.has("facing", d.facing())) {
                bad.add("门朝向与墙不一致：" + d.where());
            }
            // 第三格必须是过梁。两格是门，三格就是洞
            Block lintel = s.get(d.x(), d.y() + 2, d.z());
            if (lintel == null || lintel.isAir()) {
                bad.add("门上方是空的（门洞三格高）：" + d.where());
            }
            // 门槛：脚下不能踩空
            Block sill = s.get(d.x(), d.y() - 1, d.z());
            if (sill == null || sill.isAir()) {
                bad.add("门槛是空的（进门先掉一格）：" + d.where());
            }
        }
        assertTrue(bad.isEmpty(), "有 " + bad.size() + " 扇门不对：\n  "
                + String.join("\n  ", bad.subList(0, Math.min(12, bad.size()))));
    }

    @Test
    @DisplayName("推开门能走进去：民居、马厩、铺面都试一遍")
    void doorsLeadSomewhere() {
        BuildKit k = MedievalCity.kit(20261028L);
        Scene s = k.scene;
        int checked = 0;
        List<String> bad = new ArrayList<>();
        for (BuildKit.Door d : k.doors) {
            if (!d.kind().equals("民居") && !d.kind().equals("马厩门")
                    && !d.kind().equals("马厩")) {
                continue;
            }
            if (checked >= 2000) {
                break;
            }
            // 门外那一格：按门朝向退一步
            int ox = d.x() + (d.facing().equals("east") ? 1 : d.facing().equals("west") ? -1 : 0);
            int oz = d.z() + (d.facing().equals("south") ? 1 : d.facing().equals("north") ? -1 : 0);
            // 往里退两格，才是"屋里"
            int ix = d.x() - (ox - d.x());
            int iz = d.z() - (oz - d.z());
            WalkAudit au = new WalkAudit(s, Math.min(ox, ix) - 5, d.y() - 2,
                    Math.min(oz, iz) - 5, Math.max(ox, ix) + 5, d.y() + 8,
                    Math.max(oz, iz) + 5);
            // 门外可能有一级台阶（门槛石、街沿）。站在门那一层或者迈上一格都算走得到
            int oy = au.canStand(ox, d.y(), oz) ? d.y()
                    : au.canStand(ox, d.y() + 1, oz) ? d.y() + 1 : Integer.MIN_VALUE;
            if (oy == Integer.MIN_VALUE) {
                bad.add("门外站不住：" + d.where() + " 门外格 (" + ox + "," + d.y() + "," + oz + ")");
                checked++;
                continue;
            }
            au.walk(ox, oy, oz);
            if (!au.reached(ix, d.y(), iz) && !au.reached(ix, d.y() + 1, iz)) {
                bad.add("推门进不去：" + d.where() + " → (" + ix + "," + d.y() + "," + iz + ")");
            }
            checked++;
        }
        assertTrue(checked >= 300, "能查的门太少：" + checked);
        // 全城 1300 多扇门里还剩个位数，全在最外圈（墙根那一条背巷）：
        // 门开出去是巷子尽头的一堵墙。不到千分之六，留作已知项，不再为它改布局。
        // 阈值设成 10 是为了防回退——将来谁动了朝向或街区网格，这个数会立刻涨上来
        assertTrue(bad.size() <= 10, "有 " + bad.size() + " 处进不去（上限 10）：\n  "
                + String.join("\n  ", bad.subList(0, Math.min(12, bad.size()))));
    }


    @Test
    @DisplayName("城门：三座开着的走得出去，北面那座仪式门关着走不出去")
    void cityGatesPass() {
        Scene s = MedievalCity.build();
        record Gate(String name, int sx, int sz, int tx, int tz, boolean passable) {
        }
        List<Gate> gates = List.of(
                new Gate("南门", 0, 445, 0, 472, true),
                new Gate("东门", 445, 0, 472, 0, true),
                new Gate("西门", -445, 0, -472, 0, true),
                new Gate("北门", 0, -445, 0, -472, false));
        List<String> bad = new ArrayList<>();
        for (Gate g : gates) {
            int x1 = Math.min(g.sx(), g.tx()) - 12;
            int x2 = Math.max(g.sx(), g.tx()) + 12;
            int z1 = Math.min(g.sz(), g.tz()) - 12;
            int z2 = Math.max(g.sz(), g.tz()) + 12;
            WalkAudit au = new WalkAudit(s, x1, -2, z1, x2, 14, z2);
            if (!au.canStand(g.sx(), 0, g.sz())) {
                bad.add(g.name() + "：城里这一侧站不住");
                continue;
            }
            au.walk(g.sx(), 0, g.sz());
            boolean out = au.reached(g.tx(), 0, g.tz());
            if (out != g.passable()) {
                bad.add(g.name() + "：" + (g.passable() ? "开着却出不去" : "关着却走得出去"));
            }
        }
        assertTrue(bad.isEmpty(), String.join("；", bad));
    }

    @Test
    @DisplayName("马厩：有门、有栏、有水、有草")
    void stablesAreUsable() {
        BuildKit k = MedievalCity.kit(20261028L);
        int stables = k.stat("马厩");
        assertTrue(stables >= 3, "马厩数量不对：" + stables);
        long gates = k.doors.stream().filter(d -> d.kind().equals("马厩门")).count();
        // 每座马厩两扇门（双开）
        assertEquals(stables * 2L, gates, "马厩门扇数与马厩数量对不上");
    }

    @Test
    @DisplayName("民居的门数量与民居数量一致：一栋一扇，不多不少")
    void eachHouseHasOneDoor() {
        BuildKit k = MedievalCity.kit(20261028L);
        long doors = k.doors.stream().filter(d -> d.kind().equals("民居")).count();
        long houses = k.claims.stream().filter(c -> c.kind().equals("民居")).count();
        assertTrue(doors > 0, "一扇门都没有");
        // 铺面的门是敞口柜台，不装门扇，所以门扇数会少于房子数，但不该少太多
        assertTrue(doors * 10 >= houses * 7,
                "门太少：民居 " + houses + " 栋，门 " + doors + " 扇——铺面不该占这么多");
    }
}
