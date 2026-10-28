package io.github.vampireachao.vespera.core.world;

import io.github.vampireachao.vespera.core.voxel.Block;
import io.github.vampireachao.vespera.core.voxel.Palette;
import io.github.vampireachao.vespera.core.voxel.Scene;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 流明陨城：一座 600×600 的中世纪城市，加上墙外的港区、农田、竞技场与村落。
 *
 * <p>布局不是画出来的，是<b>排出来的</b>。先定四件事，剩下的都从它们推：
 *
 * <ol>
 *   <li><b>十字大街</b>从四个城门通到中央广场——所有商业都在它两侧。</li>
 *   <li><b>四座地标</b>各占一个象限：教堂（东北）、城堡（西北）、市政厅（西）、行会（东）。</li>
 *   <li><b>环路</b>把内城与外墙之间那圈住人的地方分成街区。</li>
 *   <li><b>城墙</b>不是边界，是取景框：站在南门往里看，钟楼、教堂尖顶、城堡塔楼要叠成三层。</li>
 * </ol>
 *
 * <p>房子沿街排，院子藏在里面——这是中世纪城市真实的生长方式：临街的地皮贵，所以窄而高；
 * 里面不值钱，所以堆货、种菜、挖井。
 */
public final class MedievalCity {

    /** 城墙中线。城是 600×600。 */
    public static final int HALF = 300;

    /** 城墙厚。 */
    public static final int WALL_THICK = 4;

    /** 城墙高到走道。 */
    public static final int WALL_HEIGHT = 12;

    /** 主街半宽。 */
    public static final int AVENUE = 8;

    public record District(String id, String name, int x1, int z1, int x2, int z2, String note) {

        public int width() {
            return x2 - x1;
        }

        public int depth() {
            return z2 - z1;
        }
    }

    /** 分区表：图纸、文档、区域判定都读这一份。 */
    public static final List<District> DISTRICTS = List.of(
            new District("market", "中央市集", -72, -72, 72, 72, "喷泉、十字碑、二十四个摊位"),
            new District("cathedral", "第九时圣座", 96, -300, 222, -86, "双塔 92 格，飞扶壁，玫瑰窗"),
            new District("castle", "灰烬堡", -298, -298, -144, -144, "土台、幕墙、主楼、吊桥"),
            new District("townhall", "市政厅", -124, -38, -68, 38, "阶梯山墙，钟塔 68 格"),
            new District("guild", "行会厅", 68, -42, 140, 46, "石砌拱廊，半木二层"),
            new District("crafts", "工匠街", 84, 84, 236, 236, "铁匠、制革、织工、陶坊"),
            new District("residential", "民居带", -236, 84, -84, 236, "半木联排，内院与水井"),
            new District("barracks", "兵营", -236, -120, -190, -80, "长屋与训练场"),
            new District("temple-green", "圣座前庭", 96, -84, 222, -20, "柱廊与朝圣者宿处"),
            new District("graveyard", "安息园", 226, -292, 298, -160, "铁栅、碑列、紫杉"),
            new District("docks", "南港", -256, 236, -90, 412, "运河、仓库、吊杆、驳船"),
            new District("arena", "东竞技场", 414, 14, 526, 126, "圆形看台，四门"),
            new District("farm", "西田", -520, 60, -340, 260, "条田、水渠、谷仓、风车"),
            new District("hamlet", "北村", 320, -420, 420, -320, "六户人家、水井、小堂"));

    private static final int[] STREET_X = {-240, -160, -84, 0, 84, 160, 240};
    private static final int[] STREET_Z = {-240, -160, -84, 0, 84, 160, 240};
    private static final int[] STREET_W = {7, 9, 7, 16, 7, 9, 7};

    /** 一条街：起止与宽度。图纸与铺地都读这一份。 */
    public record Street(int x1, int z1, int x2, int z2, int width, boolean main) {
    }

    /** 全部街道。主街宽 16，环路 9，其余 7。 */
    public static final List<Street> STREETS = buildStreets();

    private static List<Street> buildStreets() {
        List<Street> list = new ArrayList<>();
        list.add(new Street(-AVENUE, -HALF, AVENUE, HALF, AVENUE * 2, true));
        list.add(new Street(-HALF, -AVENUE, HALF, AVENUE, AVENUE * 2, true));
        for (int i = 0; i < STREET_X.length; i++) {
            int x = STREET_X[i];
            if (x == 0) {
                continue;
            }
            list.add(new Street(x - STREET_W[i] / 2, -HALF, x + STREET_W[i] / 2, HALF, STREET_W[i], false));
        }
        for (int i = 0; i < STREET_Z.length; i++) {
            int z = STREET_Z[i];
            if (z == 0) {
                continue;
            }
            list.add(new Street(-HALF, z - STREET_W[i] / 2, HALF, z + STREET_W[i] / 2, STREET_W[i], false));
        }
        return List.copyOf(list);
    }

    private MedievalCity() {
    }

    public static Scene build() {
        return build(20261028L);
    }

    public static Scene build(long seed) {
        Scene scene = new Scene();
        BuildKit k = new BuildKit(scene, seed);

        streets(k);
        walls(k);
        Landmarks.cathedral(k);
        Landmarks.castle(k);
        Landmarks.townHall(k);
        Landmarks.guildHall(k);
        Civic.market(k);
        Civic.tavern(k, -196, 76);
        Civic.smithy(k, 100, 100);
        Civic.bathhouse(k, -276, 156);
        Civic.library(k, 150, 60);
        Civic.granary(k, -172, 196);
        Civic.barracks(k, -236, -120);
        Civic.graveyard(k, 226, -292, 72, 132);
        Civic.docks(k);
        Civic.arena(k, 470, 70, 56);
        Civic.windmill(k, -420, 100);
        Civic.farm(k, -500, 160, 108, 84);
        Civic.farm(k, -500, 40, 108, 84);
        Civic.barn(k, -360, 250);
        Civic.hamlet(k, 320, -420);

        houses(k);
        fillGaps(k);
        outskirts(k);
        plazaDetails(k);

        return scene;
    }

    // -- 街道 ---------------------------------------------------------------

    private static void streets(BuildKit k) {
        for (Street street : STREETS) {
            Block main = Palette.STONE_BRICK;
            Block accent = street.main() ? Palette.ANDESITE : Palette.STONE;
            Block body = street.main() ? Palette.STONE_BRICK : Palette.COBBLE;
            k.pave(street.x1(), street.z1(), street.x2(), street.z2(), body, accent);
        }
        // 街灯：主街每 28 格一盏，交错在两侧
        for (int z = -HALF + 24; z < HALF - 20; z += 28) {
            k.lamp(-AVENUE - 2, z);
            k.lamp(AVENUE + 2, z + 14);
        }
        for (int x = -HALF + 24; x < HALF - 20; x += 28) {
            k.lamp(x, -AVENUE - 2);
            k.lamp(x + 14, AVENUE + 2);
        }
    }

    // -- 城墙与城门 ---------------------------------------------------------

    private static void walls(BuildKit k) {
        int h = WALL_HEIGHT;
        for (int side = 0; side < 4; side++) {
            boolean horizontal = side < 2;
            int fixed = side % 2 == 0 ? HALF : -HALF;
            for (int t = 0; t < WALL_THICK; t++) {
                int offset = fixed > 0 ? -t : t;
                for (int along = -HALF; along <= HALF; along++) {
                    if (Math.abs(along) <= 7) {
                        continue; // 城门洞
                    }
                    int x = horizontal ? along : fixed + offset;
                    int z = horizontal ? fixed + offset : along;
                    Block block = (t == 0) ? Palette.STONE_BRICK
                            : ((along % 13 == 0) ? Palette.STONE_MOSSY : Palette.STONE_BRICK);
                    k.fill(x, 0, z, x, h - 1, z, block);
                    if (t == 0 && Math.abs(along % 24) == 8) {
                        k.set(x, 7, z, Palette.STONE_CHISELED);
                        k.set(x, 8, z, Palette.STONE_CHISELED);
                    }
                }
            }
            // 走道与雉堞
            for (int along = -HALF; along <= HALF; along++) {
                for (int t = 0; t < WALL_THICK; t++) {
                    int offset = fixed > 0 ? -t : t;
                    int x = horizontal ? along : fixed + offset;
                    int z = horizontal ? fixed + offset : along;
                    if (Math.abs(along) <= 7) {
                        continue;
                    }
                    k.set(x, h, z, Palette.STONE_BRICK);
                    if (t == 0 && (along & 1) == 0) {
                        k.set(x, h + 1, z, Palette.STONE_BRICK);
                    }
                }
            }
            // 外侧的托石
            for (int along = -HALF; along <= HALF; along += 3) {
                int x = horizontal ? along : fixed + (fixed > 0 ? 1 : -1);
                int z = horizontal ? fixed + (fixed > 0 ? 1 : -1) : along;
                if (Math.abs(along) > 8) {
                    k.set(x, h - 2, z, Palette.STONE_CHISELED);
                }
            }
        }

        // 沿线圆塔：每隔 60 格一座，向外凸出
        for (int side = 0; side < 4; side++) {
            boolean horizontal = side < 2;
            int fixed = side % 2 == 0 ? HALF : -HALF;
            for (int along = -HALF + 60; along < HALF; along += 60) {
                if (Math.abs(along) < 30) {
                    continue;
                }
                int x = horizontal ? along : fixed;
                int z = horizontal ? fixed : along;
                k.roundTower(x, z, 7, 0, 20, Palette.STONE_BRICK, Palette.STONE_CHISELED,
                        Palette.SLATE_STAIR, 1);
                k.bump("墙塔");
            }
        }

        // 四座城门
        gate(k, 0, HALF, "south");
        gate(k, 0, -HALF, "north");
        gate(k, HALF, 0, "east");
        gate(k, -HALF, 0, "west");
        k.bump("城墙");
    }

    private static void gate(BuildKit k, int cx, int cz, String facing) {
        boolean alongZ = facing.equals("north") || facing.equals("south");
        int dir = facing.equals("south") || facing.equals("east") ? 1 : -1;

        // 两座圆塔夹住门洞
        for (int side = -1; side <= 1; side += 2) {
            int tx = alongZ ? cx + side * 13 : cx;
            int tz = alongZ ? cz : cz + side * 13;
            k.roundTower(tx, tz, 8, 0, 24, Palette.STONE_BRICK, Palette.STONE_CHISELED,
                    Palette.SLATE_STAIR, 1);
        }
        // 门楼：一段厚墙
        for (int dx = -11; dx <= 11; dx++) {
            for (int dz = -WALL_THICK; dz <= WALL_THICK; dz++) {
                for (int y = 0; y < 18; y++) {
                    int x = alongZ ? cx + dx : cx + dz;
                    int z = alongZ ? cz + dz : cz + dx;
                    boolean passage = Math.abs(dx) <= 5 && y <= 8;
                    if (passage) {
                        continue;
                    }
                    if (y > WALL_HEIGHT && ((dx + dz) & 1) == 0) {
                        continue;
                    }
                    k.set(x, y, z, y == 9 ? Palette.STONE_CHISELED : Palette.STONE_BRICK);
                }
            }
        }
        // 拱
        for (int i = 0; i <= 5; i++) {
            int x = alongZ ? cx - 5 + i : cx;
            int z = alongZ ? cz : cz - 5 + i;
            for (int y = 9 + (5 - Math.abs(i - 5)); y <= 12; y++) {
                k.set(x, y, z, Palette.STONE_CHISELED);
            }
        }
        // 闸门：竖铁栅，不是一面铁板
        for (int dx = -4; dx <= 4; dx++) {
            if ((dx & 1) != 0) {
                continue;
            }
            for (int y = 0; y <= 7; y++) {
                int x = alongZ ? cx + dx : cx + dir * 2;
                int z = alongZ ? cz + dir * 2 : cz + dx;
                k.set(x, y, z, Palette.IRON_BARS);
            }
        }
        // 门外的石坡道：直接把路铺出去，不吊在半空
        for (int i = 3; i <= 12; i++) {
            for (int dx = -6; dx <= 6; dx++) {
                int x = alongZ ? cx + dx : cx + dir * i;
                int z = alongZ ? cz + dir * i : cz + dx;
                k.set(x, -1, z, Math.abs(dx) == 6 ? Palette.ANDESITE : Palette.STONE_BRICK);
            }
        }
        // 门上的徽记
        int bx = cx;
        int bz = alongZ ? cz + dir * (WALL_THICK + 1) : cz;
        k.fill(bx - 2, 14, bz, bx + 2, 16, bz, Palette.WOOL_RED);
        k.fill(bx - 1, 15, bz, bx + 1, 15, bz, Palette.GOLD);
        k.bump("城门");
    }

    // -- 街区：房子 ---------------------------------------------------------

    private static final List<int[]> RESERVED = new ArrayList<>();

    static {
        for (District d : DISTRICTS) {
            if (!d.id().equals("residential") && !d.id().equals("crafts")) {
                RESERVED.add(new int[] {d.x1(), d.z1(), d.x2(), d.z2()});
            }
        }
        RESERVED.add(new int[] {-298, -298, -144, -144});   // 城堡
        RESERVED.add(new int[] {90, -300, 226, -80});        // 教堂
        RESERVED.add(new int[] {-130, -44, -64, 44});        // 市政厅
        RESERVED.add(new int[] {64, -48, 146, 52});          // 行会
        RESERVED.add(new int[] {-76, -76, 76, 76});          // 市集
    }

    private static void houses(BuildKit k) {
        for (int i = 0; i < STREET_X.length - 1; i++) {
            for (int j = 0; j < STREET_Z.length - 1; j++) {
                int x1 = STREET_X[i] + STREET_W[i] / 2 + 2;
                int x2 = STREET_X[i + 1] - STREET_W[i + 1] / 2 - 2;
                int z1 = STREET_Z[j] + STREET_W[j] / 2 + 2;
                int z2 = STREET_Z[j + 1] - STREET_W[j + 1] / 2 - 2;
                if (x2 - x1 < 26 || z2 - z1 < 26) {
                    continue;
                }
                if (reserved(x1, z1, x2, z2)) {
                    continue;
                }
                if (k.rnd.nextInt(100) < 8) {
                    // 一成的街区留成小广场 / 果园，城才不会密得像军营
                    square(k, x1, z1, x2, z2);
                    continue;
                }
                block(k, x1, z1, x2, z2);
            }
        }
        // 墙边零碎地：补一圈小屋与棚子
        for (int side = 0; side < 4; side++) {
            boolean horizontal = side < 2;
            int fixed = side % 2 == 0 ? HALF - 14 : -HALF + 14;
            for (int along = -HALF + 30; along < HALF - 30; along += 26) {
                int x = horizontal ? along : fixed;
                int z = horizontal ? fixed : along;
                if (!k.free(x - 7, z - 6, x + 7, z + 6)) {
                    continue;
                }
                if (k.rnd.nextInt(100) < 55) {
                    k.house(x - 7, z - 6, 15, 12, 2, horizontal ? "south" : "east", false);
                }
            }
        }
    }

    private static boolean reserved(int x1, int z1, int x2, int z2) {
        for (int[] r : RESERVED) {
            if (x1 <= r[2] && x2 >= r[0] && z1 <= r[3] && z2 >= r[1]) {
                return true;
            }
        }
        return false;
    }

    /** 一个街区：四边临街的房子，中间是院子。 */
    private static void block(BuildKit k, int x1, int z1, int x2, int z2) {
        int inset = 16;
        northRow(k, x1 + 2, z1, x2 - 2, "north");
        southRow(k, x1 + 2, z2, x2 - 2, "south");
        if (z2 - z1 > 50) {
            westRow(k, x1, z1 + inset, z2 - inset, "west");
            eastRow(k, x2, z1 + inset, z2 - inset, "east");
        }
        courtyard(k, x1 + 14, z1 + 14, x2 - 14, z2 - 14);
    }

    private static void northRow(BuildKit k, int fromX, int z, int toX, String facing) {
        int x = fromX;
        while (x + 12 <= toX) {
            int w = 12 + k.rnd.nextInt(6);
            if (x + w > toX) {
                w = toX - x;
            }
            if (w < 10) {
                break;
            }
            int d = 10 + k.rnd.nextInt(4);
            k.house(x, z, w, d, 2 + (k.rnd.nextInt(10) < 4 ? 1 : 0), facing, shopChance(k, facing));
            x += w + 1;
        }
    }

    private static void southRow(BuildKit k, int fromX, int z, int toX, String facing) {
        int x = fromX;
        while (x + 12 <= toX) {
            int w = 12 + k.rnd.nextInt(6);
            if (x + w > toX) {
                w = toX - x;
            }
            if (w < 10) {
                break;
            }
            int d = 10 + k.rnd.nextInt(4);
            k.house(x, z - d + 1, w, d, 2 + (k.rnd.nextInt(10) < 4 ? 1 : 0), facing,
                    shopChance(k, facing));
            x += w + 1;
        }
    }

    private static void westRow(BuildKit k, int x, int fromZ, int toZ, String facing) {
        int z = fromZ;
        while (z + 12 <= toZ) {
            int d = 12 + k.rnd.nextInt(6);
            if (z + d > toZ) {
                d = toZ - z;
            }
            if (d < 10) {
                break;
            }
            int w = 10 + k.rnd.nextInt(4);
            k.house(x, z, w, d, 2, facing, shopChance(k, facing));
            z += d + 1;
        }
    }

    private static void eastRow(BuildKit k, int x, int fromZ, int toZ, String facing) {
        int z = fromZ;
        while (z + 12 <= toZ) {
            int d = 12 + k.rnd.nextInt(6);
            if (z + d > toZ) {
                d = toZ - z;
            }
            if (d < 10) {
                break;
            }
            int w = 10 + k.rnd.nextInt(4);
            k.house(x - w + 1, z, w, d, 2, facing, shopChance(k, facing));
            z += d + 1;
        }
    }

    private static boolean shopChance(BuildKit k, String facing) {
        boolean alongAvenue = facing.equals("east") || facing.equals("west");
        return k.rnd.nextInt(100) < (alongAvenue ? 45 : 18);
    }

    /** 内院：井、菜地、树、柴堆。它在图上看不见，但决定了这座城有没有后院。 */
    private static void courtyard(BuildKit k, int x1, int z1, int x2, int z2) {
        if (x2 - x1 < 8 || z2 - z1 < 8) {
            return;
        }
        k.pave(x1, z1, x2, z2, Palette.COARSE, Palette.GRASS);
        if (x2 - x1 > 14 && z2 - z1 > 14 && k.rnd.nextBoolean()) {
            Civic.well(k, x1 + (x2 - x1) / 2, z1 + (z2 - z1) / 2);
        }
        int trees = 1 + k.rnd.nextInt(2);
        for (int i = 0; i < trees; i++) {
            int tx = x1 + 3 + k.rnd.nextInt(Math.max(1, x2 - x1 - 5));
            int tz = z1 + 3 + k.rnd.nextInt(Math.max(1, z2 - z1 - 5));
            if (k.free(tx - 1, tz - 1, tx + 1, tz + 1)) {
                k.tree(tx, tz, 4 + k.rnd.nextInt(3), k.rnd.nextBoolean());
            }
        }
        if (k.rnd.nextInt(100) < 40) {
            for (int i = 0; i < 4; i++) {
                k.set(x1 + 1 + i, 0, z2 - 1, Palette.HAY);
            }
        }
    }

    /**
     * 街区里的小广场：草坪、十字小径、一口井、几棵树。
     *
     * <p>第一版把它整块铺成石板，结果从空中看是一个空荡荡的灰色停车场。
     * 中世纪的"广场"多半是块绿地加一口井——铺满石头的是市集，不是街角。
     */
    private static void square(BuildKit k, int x1, int z1, int x2, int z2) {
        int cx = (x1 + x2) / 2;
        int cz = (z1 + z2) / 2;
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                boolean path = Math.abs(x - cx) <= 2 || Math.abs(z - cz) <= 2;
                k.set(x, -1, z, path ? Palette.PATH
                        : (k.rnd.nextInt(100) < 5 ? Palette.GRASS_TUFT : Palette.GRASS));
            }
        }
        Civic.well(k, cx + 8, cz + 8);
        k.tree(x1 + 7, z1 + 7, 6, false);
        k.tree(x2 - 7, z2 - 7, 6, false);
        k.tree(x1 + 7, z2 - 7, 5, true);
        k.tree(x2 - 7, z1 + 7, 5, false);
        for (int i = 0; i < 4; i++) {
            k.set(cx - 6 + i * 4, 0, cz + 10, Palette.OAK_PLANK);
            if (i == 0 || i == 3) {
                k.set(cx - 6 + i * 4, 1, cz + 10, Palette.OAK_FENCE);
            }
        }
        // 花圃
        for (int i = 0; i < 6; i++) {
            k.set(x1 + 4 + i * 2, 0, z1 + 4, i % 2 == 0 ? Palette.FLOWER_RED : Palette.FLOWER_YELLOW);
            k.set(x2 - 4 - i * 2, 0, z2 - 4, i % 2 == 0 ? Palette.FLOWER_YELLOW : Palette.FLOWER_RED);
        }
        k.bump("小广场");
    }

    /**
     * 补空：城里总会留下一些零碎地块——地标让出来的边角、街区之间的错位。
     * 空着就是一块草坪，看起来像"还没盖完"。补成菜园、果园或棚屋，它们不是装饰，
     * 是城里的副食来源。
     */
    private static void fillGaps(BuildKit k) {
        int step = 26;
        for (int x = -HALF + 8; x < HALF - 20; x += step) {
            for (int z = -HALF + 8; z < HALF - 20; z += step) {
                int x2 = x + 18;
                int z2 = z + 18;
                if (x2 > HALF - 6 || z2 > HALF - 6) {
                    continue;
                }
                if (!k.openAir(x, z, x2, z2) || k.paved(x, z, x2, z2)) {
                    continue;
                }
                int roll = k.rnd.nextInt(100);
                if (roll < 38) {
                    orchard(k, x, z, x2, z2);
                } else if (roll < 76) {
                    garden(k, x, z, x2, z2);
                } else if (roll < 90) {
                    shed(k, x, z, x2, z2);
                }
            }
        }
    }

    private static void orchard(BuildKit k, int x1, int z1, int x2, int z2) {
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
                k.set(x, edge ? 0 : -1, z, edge ? Palette.SPRUCE_FENCE : Palette.GRASS);
            }
        }
        for (int x = x1 + 5; x < x2 - 2; x += 9) {
            for (int z = z1 + 5; z < z2 - 2; z += 9) {
                k.tree(x, z, 4 + k.rnd.nextInt(2), false);
            }
        }
        k.bump("果园");
    }

    private static void garden(BuildKit k, int x1, int z1, int x2, int z2) {
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
                if (edge) {
                    k.set(x, 0, z, Palette.SPRUCE_FENCE);
                    continue;
                }
                if ((x - x1) % 4 == 0) {
                    k.set(x, -1, z, Palette.WATER);
                } else {
                    k.set(x, -1, z, Palette.FARMLAND);
                    if ((z - z1) % 5 != 0) {
                        k.set(x, 0, z, k.rnd.nextBoolean() ? Palette.WHEAT : Palette.CARROT);
                    }
                }
            }
        }
        k.bump("菜园");
    }

    private static void shed(BuildKit k, int x1, int z1, int x2, int z2) {
        int cx = (x1 + x2) / 2;
        int cz = (z1 + z2) / 2;
        k.fill(cx - 6, -1, cz - 5, cx + 6, 0, cz + 5, Palette.COBBLE);
        k.walls(cx - 6, 0, cz - 5, cx + 6, 4, cz + 5, Palette.SPRUCE_PLANK);
        k.carve(cx - 5, 0, cz - 4, cx + 5, 3, cz + 4);
        k.roof(cx - 8, cz - 7, cx + 8, cz + 7, 5, true, Palette.SHINGLE_STAIR,
                Palette.SPRUCE_PLANK, 1);
        k.set(cx, 0, cz, Palette.BARREL);
        for (int i = 0; i < 3; i++) {
            k.set(cx + 8 + i, 0, cz + 3, Palette.HAY);
        }
        k.bump("棚屋");
    }

    // -- 城外 ---------------------------------------------------------------

    private static void outskirts(BuildKit k) {
        // 四条官道伸出城门
        k.pave(-7, HALF, 7, HALF + 120, Palette.PATH, Palette.COARSE);
        k.pave(-7, -HALF - 140, 7, -HALF, Palette.PATH, Palette.COARSE);
        k.pave(HALF, -7, HALF + 240, 7, Palette.PATH, Palette.COARSE);
        k.pave(-HALF - 240, -7, -HALF, 7, Palette.PATH, Palette.COARSE);
        // 路边灯
        for (int i = 0; i < 4; i++) {
            k.lamp(9, HALF + 20 + i * 26);
            k.lamp(-9, -HALF - 20 - i * 26);
            k.lamp(HALF + 20 + i * 40, 9);
        }
        // 城外树：疏林，不挡视线
        for (int i = 0; i < 260; i++) {
            int x = k.rnd.nextInt(1600) - 800;
            int z = k.rnd.nextInt(1600) - 800;
            if (Math.abs(x) < HALF + 24 && Math.abs(z) < HALF + 24) {
                continue;
            }
            if (Math.abs(x) < 20 || Math.abs(z) < 20) {
                continue;
            }
            if (!k.free(x - 2, z - 2, x + 2, z + 2)) {
                continue;
            }
            k.tree(x, z, 5 + k.rnd.nextInt(5), k.rnd.nextInt(100) < 35);
        }
    }

    /** 广场与街口的杂物：车、桶、告示牌。它们让空地看着像有人走过。 */
    private static void plazaDetails(BuildKit k) {
        for (int i = 0; i < 40; i++) {
            int x = k.rnd.nextInt(560) - 280;
            int z = k.rnd.nextInt(560) - 280;
            if (!k.free(x, z, x, z)) {
                continue;
            }
            if (Math.abs(x) < 10 || Math.abs(z) < 10) {
                k.set(x, 0, z, k.rnd.nextBoolean() ? Palette.BARREL : Palette.CRATE);
            }
        }
        // 广场四角的长椅
        for (int dx : new int[] {-52, 52}) {
            for (int dz : new int[] {-52, 52}) {
                for (int i = 0; i < 4; i++) {
                    k.set(dx + i, 0, dz, Palette.OAK_PLANK);
                }
                k.set(dx, 1, dz, Palette.OAK_FENCE);
                k.set(dx + 3, 1, dz, Palette.OAK_FENCE);
            }
        }
    }

    public static String summary() {
        return String.format(Locale.ROOT, "%d×%d 城墙 · %d 个分区 · 墙外含港区/农田/竞技场/村落",
                HALF * 2, HALF * 2, DISTRICTS.size());
    }

    /** 出生点：南门内四十格，正对中央广场与钟楼。 */
    public static int[] spawn() {
        return new int[] {0, HALF - 40};
    }

    /** 城里一共要铺多少地——落地器据此决定平整范围。 */
    public static int groundRadius() {
        return HALF + 8;
    }
}
