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
        assertNotNull(scene().get(-154, 30, -74), "市政厅钟塔没有立起来");
    }

    @Test
    @DisplayName("城外也有东西：南港的水、竞技场的沙地")
    void outskirtsExist() {
        // 水面比岸低两格：与岸齐平的水看着像泳池
        assertEquals("minecraft:water", scene().get(-170, -2, 350).id(), "运河没有水");
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
    @DisplayName("建筑占地互不重叠：摊贩不会插进售票亭，排屋不会在街角穿成一团")
    void claimsNeverOverlap() {
        BuildKit kit = MedievalCity.kit(20261028L);
        List<String> hits = new ArrayList<>();
        List<BuildKit.Claim> claims = kit.claims;
        for (int i = 0; i < claims.size(); i++) {
            for (int j = i + 1; j < claims.size(); j++) {
                BuildKit.Claim a = claims.get(i);
                BuildKit.Claim b = claims.get(j);
                if (a.overlap(b) > 0) {
                    hits.add(a.kind() + " " + a.where() + " × " + b.kind() + " " + b.where()
                            + " 重叠 " + a.overlap(b) + " 格");
                }
            }
        }
        assertTrue(hits.isEmpty(), "建筑互相压住了：\n  " + String.join("\n  ", hits));
    }

    @Test
    @DisplayName("城里以夯土碎石为主，草只留在院子与墓地")
    void cityGroundIsEarth() {
        int grass = 0;
        int earth = 0;
        for (int x = -MedievalCity.HALF; x <= MedievalCity.HALF; x += 7) {
            for (int z = -MedievalCity.HALF; z <= MedievalCity.HALF; z += 7) {
                Block block = scene().get(x, -1, z);
                if (block == null) {
                    continue;
                }
                String id = block.id();
                if (id.contains("grass_block")) {
                    grass++;
                } else if (id.contains("coarse_dirt") || id.contains("dirt_path")
                        || id.contains("gravel") || id.contains("dirt")) {
                    earth++;
                }
            }
        }
        assertTrue(earth > 1000, "土质地面太少，这条测试没意义：" + earth);
        assertTrue(earth > grass * 5,
                "城里还是草地：土 " + earth + " 格，草 " + grass + " 格（草应只出现在院子与墓地）");
    }

    @Test
    @DisplayName("分区表覆盖全部有意设计的地方，没有重复 id")
    void districtsAreUnique() {
        long unique = MedievalCity.DISTRICTS.stream().map(MedievalCity.District::id).distinct().count();
        assertEquals(MedievalCity.DISTRICTS.size(), unique, "分区 id 有重复");
        assertTrue(MedievalCity.DISTRICTS.size() >= 26, "分区表还没扩到 26 条");
    }

    @Test
    @DisplayName("预留表不跨街：跨一条街，整条路就会被 blocked() 吞掉")
    void reservedNeverCrossesStreet() {
        for (int[] area : MedievalCity.reservedAreas()) {
            for (MedievalCity.Street street : MedievalCity.STREETS) {
                int ax = Math.min(street.x1(), street.x2());
                int bx = Math.max(street.x1(), street.x2());
                int az = Math.min(street.z1(), street.z2());
                int bz = Math.max(street.z1(), street.z2());
                boolean overlaps = area[0] <= bx && area[2] >= ax && area[1] <= bz && area[3] >= az;
                assertTrue(!overlaps, "预留矩形 " + area[0] + "," + area[1] + " → " + area[2] + ","
                        + area[3] + " 压住了街 " + ax + "," + az + " → " + bx + "," + bz);
            }
        }
    }

    @Test
    @DisplayName("环路与东大道全程有铺装：这两处曾经整段断掉")
    void ringAndAvenueArePaved() {
        int ring = MedievalCity.RING;
        for (int i = -4; i <= 4; i++) {
            assertPaved(ring - 4, (i * ring) / 4);
            assertPaved(-ring + 4, (i * ring) / 4);
            assertPaved((i * ring) / 4, ring - 4);
            assertPaved((i * ring) / 4, -ring + 4);
        }
        for (int x = 124; x <= 292; x += 21) {
            assertPaved(x, 0);
        }
    }

    private void assertPaved(int x, int z) {
        Block block = scene().get(x, -1, z);
        assertNotNull(block, "路上没有方块：" + x + "," + z);
        String id = block.id();
        assertTrue(id.contains("stone") || id.contains("cobble") || id.contains("andesite")
                || id.contains("path"), "路口没铺装：" + x + "," + z + " 是 " + id);
    }

    @Test
    @DisplayName("角斗场外墙闭合：沿一圈采样三百六十点，没有一处是空")
    void colosseumWallIsClosed() {
        int cx = 185;
        int cz = 193;
        int holes = 0;
        for (int deg = 0; deg < 360; deg++) {
            double t = Math.toRadians(deg);
            int x = cx + (int) Math.round(82.0 * Math.cos(t));
            int z = cz + (int) Math.round(70.0 * Math.sin(t));
            // 四个门洞是开着的，跳过它们
            if (Math.abs(x - cx) <= 3 || Math.abs(z - cz) <= 3) {
                continue;
            }
            Block block = scene().get(x, 20, z);
            if (block == null) {
                holes++;
            }
        }
        assertEquals(0, holes, "外墙上还有 " + holes + " 处空洞");
    }

    @Test
    @DisplayName("看台不外溢：最外一圈之外不应再有实心看台块")
    void seatingStaysInsideWall() {
        int cx = 185;
        int cz = 193;
        for (int deg = 0; deg < 360; deg += 15) {
            double t = Math.toRadians(deg);
            int x = cx + (int) Math.round(82.0 * 1.12 * Math.cos(t));
            int z = cz + (int) Math.round(70.0 * 1.12 * Math.sin(t));
            Block block = scene().get(x, 10, z);
            // 场子外面还有民居与摊子，只判"看台材质"有没有越界
            boolean seating = block != null && (block.id().contains("stone_bricks")
                    || block.id().contains("andesite_polished"));
            assertTrue(!seating, "看台鼓出外墙了：" + x + "," + z + " 是 " + block);
        }
    }

    @Test
    @DisplayName("七处新地方都在：演讲台、国库、客栈、银行、神职院、府邸水池、公会铁门")
    void newPlacesExist() {
        assertNotNull(scene().get(0, 3, -86), "演讲台没有台面");
        assertEquals("minecraft:iron_block", scene().get(-72, 1, -290).id(), "国库铁门不是铁");
        assertNotNull(scene().get(-40, 5, 265), "客栈二层没有楼板");
        assertNotNull(scene().get(40, 2, 130), "银行没有柜台");
        assertNotNull(scene().get(85, 2, 99), "圣器库院墙没有立起来");
        assertEquals("minecraft:water", scene().get(134, -1, -282).id(), "府邸水池没有水");
        assertEquals("minecraft:iron_bars", scene().get(160, 2, -30).id(), "公会铁门不在巷中");
    }

    @Test
    @DisplayName("出生点站在祭坛上：脚下一格实心，身上两格是空气")
    void spawnStandsOnAltar() {
        int[] spawn = MedievalCity.spawn();
        int y = MedievalCity.spawnHeight();
        assertNotNull(scene().get(spawn[0], y - 1, spawn[1]), "出生点脚下没有台面");
        assertTrue(scene().get(spawn[0], y, spawn[1]) == null, "出生点那一格被方块占了");
        assertTrue(scene().get(spawn[0], y + 1, spawn[1]) == null, "出生点头顶被占了");
    }

    @Test
    @DisplayName("兽笼真的在地下，且顶上留着那一格天窗")
    void undercroftIsUnderground() {
        assertTrue(scene().get(190, -2, 215) == null, "兽笼没有被挖开");
        assertNotNull(scene().get(190, -4, 215), "兽笼没有地板");
        assertEquals("minecraft:iron_bars", scene().get(205, -1, 220).id(), "天窗那格不是铁栅");
    }
}
