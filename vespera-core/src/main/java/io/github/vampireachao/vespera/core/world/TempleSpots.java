package io.github.vampireachao.vespera.core.world;

import java.util.List;

/**
 * 圣堂里的五处"办事的地方"。
 *
 * <p>为什么要有这个类：同样的几个坐标会被两拨人用到——建城的时候要在这儿砌一块
 * 箱子/讲经台/座椅，插件要在这儿判断"玩家点的是不是那个箱子"。两边各写一遍，
 * 改一处忘一处，症状是"点了没反应"，而图上完全正常。
 *
 * <p>所以坐标只写一次，在场景坐标系里（原点 = 圣座中心，y=-1 是城外地表）。
 * 落地时由 {@code CityApplier} 加上城心与基面偏移。
 *
 * <p>这些点是 P1「朝圣备礼」的发源地，对应 mmSupernaturals 转职要的四样东西：
 * 30 面包、8 红石、4 金锭、4 荧石粉。红石不在这里——它是地牢墙上的血石，
 * 得玩家自己挖。
 */
public final class TempleSpots {

    /** 一件可以点的东西（箱子、讲经台、座椅）。 */
    public record Marker(String id, String label, int x, int y, int z) {

        public String where() {
            return label + " (" + x + "," + y + "," + z + ")";
        }
    }

    /** 一个站着的人。{@code npc} 是锚点 id，与配置里的一一对应。 */
    public record Post(String id, String label, int x, int y, int z, String npc) {

        public String where() {
            return label + " (" + x + "," + y + "," + z + ")";
        }
    }

    private TempleSpots() {
    }

    /**
     * 物件与人都要分开定位。
     *
     * <p>把它们写成同一个坐标，人就站在箱子里了——测试里量出来的第一种失败。
     */
    public static final Marker WAREHOUSE_CHEST = new Marker("warehouse", "圣堂仓库", -46, 9, -48);
    public static final Marker LIBRARY_LECTERN = new Marker("library", "圣堂藏书阁", -42, 17, -38);
    public static final Marker COUNCIL_SEAT = new Marker("council", "祭司议事厅", -35, 25, -35);
    public static final Marker DUNGEON_CHEST = new Marker("dungeon", "圣堂地牢", -18, -8, 16);

    public static final Post STEWARD = new Post("warehouse", "仓库执事", -47, 9, -49, "temple-steward");
    public static final Post LIBRARIAN = new Post("library", "藏书阁执事", -43, 17, -40, "temple-librarian");
    public static final Post HERALD = new Post("council", "议事司仪", -32, 25, -36, "temple-herald");
    public static final Post POPE = new Post("pope", "教皇", -8, 49, -11, "temple-pope");
    public static final Post WARDEN = new Post("dungeon", "地牢看守", -22, -8, 16, "temple-warden");

    /** 需要砌的四件物件。 */
    public static final List<Marker> MARKERS = List.of(WAREHOUSE_CHEST, LIBRARY_LECTERN,
            COUNCIL_SEAT, DUNGEON_CHEST);

    /** 站着人的五处（含教皇）。 */
    public static final List<Post> POSTS = List.of(STEWARD, LIBRARIAN, HERALD, POPE, WARDEN);

    /** 地牢房间的范围（含地面与天花），给测试与建造共用。 */
    public static final int DUNGEON_X1 = -40;
    public static final int DUNGEON_X2 = -10;
    public static final int DUNGEON_Z1 = -12;
    public static final int DUNGEON_Z2 = 22;
    /** 地牢地面那一层。 */
    public static final int DUNGEON_FLOOR = -9;

    /**
     * 按锚点 id 找一处。找不到返回 null——配置里写错名字时，调用方要能自己决定
     * 是报错还是忽略，而不是抛异常把整次交互打断。
     */
    public static Post byNpc(String npcId) {
        for (Post post : POSTS) {
            if (post.npc().equals(npcId)) {
                return post;
            }
        }
        return null;
    }

    /** 按物件 id 找一处（插件判定"点到的是不是它"用）。 */
    public static Marker byId(String id) {
        for (Marker marker : MARKERS) {
            if (marker.id().equals(id)) {
                return marker;
            }
        }
        return null;
    }
}
