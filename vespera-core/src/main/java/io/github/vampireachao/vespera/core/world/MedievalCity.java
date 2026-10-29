package io.github.vampireachao.vespera.core.world;

import io.github.vampireachao.vespera.core.voxel.Block;
import io.github.vampireachao.vespera.core.voxel.Palette;
import io.github.vampireachao.vespera.core.voxel.Scene;
import java.util.List;
import java.util.Locale;

/**
 * 流明陨城：以圣座金字塔为心、以王座大厅为北端的一座中世纪城市。
 *
 * <p>这一版的布局只有一句话：<b>三条轴线，一个中心</b>。
 *
 * <ul>
 *   <li><b>中心</b>是七层圣座金字塔——教会不靠尖顶够天，靠一级级台阶把人和神分开。</li>
 *   <li><b>正北</b>是梯形向上的王座大厅，来朝见的人必须一级级走上去。</li>
 *   <li><b>东南</b>是罗马式角斗场，教会把"不该留在城里的人"放逐到沙地上。</li>
 * </ul>
 *
 * <p>城市不是先画轮廓再填空，而是先定这三件事，其余的房子、作坊、水井、街灯
 * 都长在它们之间的缝里。
 */
public final class MedievalCity {

    public static final int HALF = 300;
    public static final int WALL_THICK = 5;
    public static final int WALL_HEIGHT = 12;
    public static final int AVENUE = 8;

    /** 金字塔与环市集的边界。 */
    public static final int SACRED = 66;
    public static final int PLAZA = 112;
    public static final int RING = 120;

    public record District(String id, String name, int x1, int z1, int x2, int z2, String note) {

        public int width() {
            return x2 - x1;
        }

        public int depth() {
            return z2 - z1;
        }
    }

    public static final List<District> DISTRICTS = List.of(
            new District("sacred", "圣座金字塔", -66, -66, 66, 66, "七层，高 63，顶置祭坛，内部为教会居所"),
            new District("plaza", "环市集", -112, -112, 112, 112, "绕塔一周的市集大道，摊位与市场棚"),
            new District("throne", "王座大厅", -80, -300, 80, -150, "三级台地，梯形向上，尽头是王座"),
            new District("castle", "灰烬堡", -300, -300, -146, -146, "台地、幕墙、主楼、马厩与营房"),
            new District("colosseum", "角斗场", 105, 125, 265, 261, "椭圆看台、四门、包厢、沙地、地下兽笼"),
            new District("guild", "断弦", 130, -76, 192, -14, "一层酒馆与穿堂巷、西半内厅、二层名单"),
            new District("townhall", "市政厅", -190, -76, -142, -8, "阶梯山墙，钟塔 68 格"),
            new District("barracks", "兵营", -236, -120, -190, -80, "长屋、训练场、草人"),
            new District("graveyard", "安息园", 210, -290, 292, -200, "铁栅、碑列、紫杉"),
            new District("crafts", "工匠街", 120, 20, 164, 110, "铁匠、制革、织工、陶坊"),
            new District("south-market", "南市", 20, 130, 100, 280, "面包、酒、鱼、布——过日子的一条街"),
            new District("residential", "民居带", -260, 20, -120, 280, "半木联排，院里一口井"),
            new District("docks", "南港", -256, 236, -90, 412, "运河、仓库、吊杆、驳船"),
            new District("farm", "西田", -520, 40, -340, 260, "条田、水渠、谷仓、风车"),
            new District("hamlet", "北村", 310, -430, 430, -310, "六户人家、水井、小堂"),
            new District("clergy-nw", "修士居所", -99, -99, -72, -72, "神职院落，四角切 45° 让开水井"),
            new District("clergy-ne", "缮写院", 72, -99, 99, -72, "抄书、修书、存卷"),
            new District("clergy-se", "圣器库", 72, 72, 99, 99, "无窗单铁门，教会最硬的一间"),
            new District("clergy-sw", "济贫院", -99, 72, -72, 99, "通铺与粥棚"),
            new District("palace-wing", "王室内苑", -143, -294, 96, -254, "西翼国库与居所，东翼学堂"),
            new District("noble", "贵族府邸", 100, -292, 206, -140, "六座府邸，带院、池、地窖，侧门开巷"),
            new District("slum", "贫民窟", -192, 122, -122, 234, "六乘八联排，巷宽三，靠港吃饭"),
            new District("middle-class", "中产区", 166, 20, 234, 110, "十乘十二带院，行会师傅住这儿"),
            new District("vendors", "角斗场摊贩街", 103, 125, 294, 293, "矮铺面与固定摊位，卖的是当天"),
            new District("gate-quarter", "南门里", -80, 236, -24, 296, "客栈、马厩、车马店——旅人第一站"),
            new District("bank", "天平与钥匙", 22, 122, 62, 148, "银行与典当行二合一，地下有库"));

    public record Street(int x1, int z1, int x2, int z2, int width, boolean main) {
    }

    /**
     * 街道：一圈环路、四条出城大道、若干街巷。
     *
     * <p>两条竖巷（x 156..164、236..244）与两条横巷（z 156..164、236..244）在角斗场南移之后
     * 必须拆成两段：不拆的话，`blocked()` 会在场子那里把整条路吞掉——环路东南角曾经
     * 就这样断了 162 格，走上去是一片草地。
     */
    public static final List<Street> STREETS = List.of(
            new Street(-RING, -RING, RING, -RING + 8, 8, true),
            new Street(-RING, RING - 8, RING, RING, 8, true),
            new Street(-RING, -RING, -RING + 8, RING, 8, true),
            new Street(RING - 8, -RING, RING, RING, 8, true),
            new Street(-AVENUE, RING, AVENUE, HALF, AVENUE * 2, true),
            new Street(RING, -AVENUE, HALF, AVENUE, AVENUE * 2, true),
            new Street(-HALF, -AVENUE, -RING, AVENUE, AVENUE * 2, true),
            new Street(-10, -147, 10, -RING, 20, true),
            // 竖巷：在角斗场处断开
            new Street(-245, -140, -239, 232, 7, false),
            new Street(-164, -6, -156, 232, 7, false),
            new Street(156, -HALF, 164, 122, 7, false),
            new Street(156, 265, 164, HALF, 7, false),
            new Street(236, -196, 244, 122, 7, false),
            new Street(236, 265, 244, HALF, 7, false),
            // 横巷：同样断开
            new Street(-140, -244, -84, -236, 7, false),
            new Street(84, -244, 206, -236, 7, false),
            new Street(-140, -164, -84, -156, 7, false),
            new Street(84, -164, HALF, -156, 7, false),
            new Street(-HALF, 156, 101, 164, 7, false),
            new Street(-HALF, 236, -256, 244, 7, false),
            new Street(-84, 236, 101, 244, 7, false),
            new Street(84, 236, 101, 244, 7, false));

    /**
     * 需要给地标让路的地方。铺街与盖房都要先问一句。
     *
     * <p>规矩只有一条：**矩形绝不跨街**。跨了街，`blocked()` 就会把那条路整段跳过，
     * 街道断头而没有任何报错——这比盖错一栋房子难查得多。
     */
    private static final List<int[]> RESERVED = List.of(
            new int[] {-66, -66, 66, 66},           // 圣座
            new int[] {-111, -111, 111, 111},       // 环市集（不能碰到 112 的环路内沿）
            new int[] {-82, -300, 82, -148},        // 王座厅
            new int[] {-300, -300, -144, -144},     // 城堡
            new int[] {103, 123, 267, 263},         // 角斗场（南移 8 格后不再压环路）
            // 行会拆成东西两半：中间的穿堂巷是公共通道，不能算作建筑占地
            new int[] {128, -76, 155, -12},
            new int[] {165, -76, 194, -12},
            new int[] {-192, -78, -140, -10},       // 市政厅（南墙让出西大道）
            new int[] {-238, -122, -188, -78},      // 兵营
            new int[] {208, -292, 294, -198},       // 墓地
            new int[] {-252, 234, -88, 414},        // 码头
            new int[] {-524, 36, -336, 264},        // 农田
            new int[] {306, -434, 434, -306},       // 北村
            new int[] {-101, -101, -70, -70},       // 修士居所
            new int[] {70, -101, 101, -70},         // 缮写院
            new int[] {70, 70, 101, 101},           // 圣器库
            new int[] {-101, 70, -70, 101},         // 济贫院
            new int[] {-15, -94, 15, -78},          // 演讲台
            new int[] {-143, -294, -50, -254},      // 国库 + 王室居所
            new int[] {-160, -296, -146, -238},     // 城堡与王室居所之间的服务巷：不许再塞房子
            new int[] {52, -292, 96, -260},         // 王室学堂
            new int[] {-188, -142, -122, -122},     // 城堡前庭
            new int[] {-76, 246, -26, 282},         // 客栈与马厩
            new int[] {22, 122, 62, 148},           // 天平与钥匙
            new int[] {166, -144, 194, -98},        // 公会靶场
            new int[] {128, -118, 154, -80},        // 学徒宿舍西
            new int[] {166, -118, 192, -80},        // 学徒宿舍东
            new int[] {98, -294, 154, -246},        // 府邸 G1
            new int[] {166, -294, 206, -246},       // 府邸 G2
            new int[] {98, -234, 154, -166},        // 府邸 G3
            new int[] {166, -234, 206, -166},       // 府邸 G4
            new int[] {98, -155, 154, -138},        // 马厩 S1
            new int[] {166, -155, 206, -138},       // 马厩 S2
            new int[] {-192, 122, -165, 155},       // 贫民窟 S1a
            new int[] {-192, 165, -165, 233},       // 贫民窟 S1b
            new int[] {-155, 122, -122, 155},       // 贫民窟 S2
            new int[] {-155, 165, -122, 233},       // 贫民窟 S3
            new int[] {166, 22, 234, 110},          // 中产区
            new int[] {268, 125, 294, 234},         // 东摊贩街
            new int[] {103, 264, 155, 293},         // 南摊贩街西段
            new int[] {165, 264, 234, 293},         // 南摊贩街东段
            new int[] {246, 264, 267, 293},         // 东南角小块
            new int[] {58, 198, 98, 228},           // 粮仓新址
            new int[] {-140, -210, -86, -172});     // 图书馆新址

    private MedievalCity() {
    }

    public static Scene build() {
        return build(20261028L);
    }

    public static Scene build(long seed) {
        Scene scene = new Scene();
        BuildKit k = new BuildKit(scene, seed);

        ground(k);
        streets(k);
        frontages(k);
        houses(k);
        walls(k);
        banks(k);

        Sacred.pyramid(k);
        Sacred.throneHall(k);
        // 角斗场南移八格：原先 (185,185) 的北缘压在南环路上，把路吃了 108 格
        Sacred.colosseum(k, 185, 193);

        Landmarks.castle(k);
        Landmarks.townHall(k);
        Landmarks.guildHall(k);

        Civic.guildTavern(k);
        Civic.guildYard(k);
        Civic.townHallOffices(k);
        Civic.guildDorms(k);

        // 圆盘铺装会重刷 y=-1，所以神职院与讲台必须在它之后
        marketRing(k);
        Sacred.clergyCourts(k);
        Sacred.tribune(k);
        Landmarks.palaceWings(k);

        // 三座功能商铺：钱、床、委托
        Landmarks.bankAndPawn(k, 24, 124);
        Landmarks.inn(k, -72, 246);

        life(k);
        Civic.tavern(k, -150, 150);
        Civic.smithy(k, 130, 40);
        Civic.bathhouse(k, -276, 200);
        Civic.library(k, -136, -206);
        Civic.granary(k, 60, 200);
        Civic.barracks(k, -236, -120);
        Civic.graveyard(k, 210, -290, 82, 92);
        Civic.docks(k);
        Civic.windmill(k, -420, 100);
        Civic.farm(k, -500, 160, 108, 84);
        Civic.farm(k, -500, 40, 108, 84);
        Civic.barn(k, -360, 250);
        Civic.hamlet(k, 320, -420);

        // 密度分级的三个样本，以及角斗场周边的小生意
        Civic.slum(k);
        Civic.middleClass(k);
        Civic.arenaVendors(k);
        Sacred.colosseumInterior(k);
        Landmarks.nobleQuarter(k);

        fillGaps(k);
        outskirts(k);
        lights(k);

        return scene;
    }

    // -- 地 -----------------------------------------------------------------

    /**
     * 城里的地面。这一层很关键：有了它，房子才有"底"。
     *
     * <p>之前地面交给落地器整平，图上用一张平面代替——结果城堡那种抬高两格的建筑，
     * 在图上像浮着。地面进场景之后，图和游戏看到的是同一块地。
     */
    private static void ground(BuildKit k) {
        for (int x = -HALF; x <= HALF; x++) {
            for (int z = -HALF; z <= HALF; z++) {
                int roll = k.rnd.nextInt(100);
                Block top = roll < 6 ? Palette.PATH : (roll < 12 ? Palette.COARSE : Palette.GRASS);
                k.set(x, -1, z, top);
            }
        }
        // 城外一圈裙边：三级缓降，城墙不会直接切在空气上
        for (int r = HALF + 1; r <= HALF + 10; r++) {
            int y = -1 - (r - HALF) / 4;
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    if (Math.max(Math.abs(x), Math.abs(z)) != r) {
                        continue;
                    }
                    k.set(x, y, z, (r % 3 == 0) ? Palette.COARSE : Palette.GRASS);
                }
            }
        }
    }

    // -- 街道 ---------------------------------------------------------------

    private static void streets(BuildKit k) {
        for (Street street : STREETS) {
            paveStreet(k, street);
        }
        for (int z = -HALF + 24; z < HALF - 20; z += 28) {
            k.lamp(-AVENUE - 2, z);
            k.lamp(AVENUE + 2, z + 14);
        }
        for (int x = -HALF + 24; x < HALF - 20; x += 28) {
            k.lamp(x, -AVENUE - 2);
            k.lamp(x + 14, AVENUE + 2);
        }
    }

    private static void paveStreet(BuildKit k, Street street) {
        int ax = Math.min(street.x1(), street.x2());
        int bx = Math.max(street.x1(), street.x2());
        int az = Math.min(street.z1(), street.z2());
        int bz = Math.max(street.z1(), street.z2());
        Block body = street.main() ? Palette.STONE_BRICK : Palette.COBBLE;
        Block accent = street.main() ? Palette.ANDESITE : Palette.STONE;
        for (int x = ax; x <= bx; x++) {
            for (int z = az; z <= bz; z++) {
                if (blocked(x, z)) {
                    continue;
                }
                int roll = k.rnd.nextInt(100);
                Block block = roll < 8 ? accent : (roll < 20 ? Palette.COBBLE : body);
                k.set(x, -1, z, block);
            }
        }
    }

    private static boolean blocked(int x, int z) {
        for (int[] r : RESERVED) {
            if (x >= r[0] && x <= r[2] && z >= r[1] && z <= r[3]) {
                return true;
            }
        }
        return false;
    }

    /**
     * 沿街立面：一条街两侧连续排屋。
     *
     * <p>中世纪城市的密度不是靠"多盖几栋房子"，是靠"每一条街都贴着房子"。
     * 临街的山墙一栋接一栋，只在巷口断开——远看就是一道连续的木与灰泥的墙。
     */
    private static void frontages(BuildKit k) {
        for (Street street : STREETS) {
            boolean horizontal = Math.abs(street.z2() - street.z1())
                    <= Math.abs(street.x2() - street.x1());
            int half = street.width() / 2;
            if (horizontal) {
                int zc = (street.z1() + street.z2()) / 2;
                int x1 = Math.min(street.x1(), street.x2());
                int x2 = Math.max(street.x1(), street.x2());
                frontageRow(k, x1, x2, zc - half - 12, "south");
                frontageRow(k, x1, x2, zc + half + 2, "north");
            } else {
                int xc = (street.x1() + street.x2()) / 2;
                int z1 = Math.min(street.z1(), street.z2());
                int z2 = Math.max(street.z1(), street.z2());
                frontageColumn(k, xc - half - 12, z1, z2, "east");
                frontageColumn(k, xc + half + 2, z1, z2, "west");
            }
        }
    }

    private static void frontageRow(BuildKit k, int fromX, int toX, int z, String front) {
        int x = fromX;
        while (x + 12 <= toX) {
            int w = 12 + k.rnd.nextInt(6);
            if (intersectsReserved(x, z, x + w - 1, z + 11)
                    || onStreet(x, z, x + w - 1, z + 11)) {
                x += 4;
                continue;
            }
            if (!k.free(x, z, x + w - 1, z + 11)) {
                x += 2;
                continue;
            }
            k.house(x, z, w, 12, 2 + (k.rnd.nextInt(10) < 4 ? 1 : 0), front,
                    k.rnd.nextInt(100) < 22);
            x += w + 1;
        }
    }

    private static void frontageColumn(BuildKit k, int x, int fromZ, int toZ, String front) {
        int z = fromZ;
        while (z + 12 <= toZ) {
            int d = 12 + k.rnd.nextInt(6);
            if (intersectsReserved(x, z, x + 11, z + d - 1)
                    || onStreet(x, z, x + 11, z + d - 1)) {
                z += 4;
                continue;
            }
            if (!k.free(x, z, x + 11, z + d - 1)) {
                z += 2;
                continue;
            }
            k.house(x, z, 12, d, 2 + (k.rnd.nextInt(10) < 4 ? 1 : 0), front,
                    k.rnd.nextInt(100) < 22);
            z += d + 1;
        }
    }

    private static boolean intersectsReserved(int x1, int z1, int x2, int z2) {
        for (int[] r : RESERVED) {
            if (x1 <= r[2] && x2 >= r[0] && z1 <= r[3] && z2 >= r[1]) {
                return true;
            }
        }
        return false;
    }

    /** 这块地压到任何一条街了吗。压到就不盖房——否则会把街口堵死。 */
    private static boolean onStreet(int x1, int z1, int x2, int z2) {
        for (Street street : STREETS) {
            int ax = Math.min(street.x1(), street.x2());
            int bx = Math.max(street.x1(), street.x2());
            int az = Math.min(street.z1(), street.z2());
            int bz = Math.max(street.z1(), street.z2());
            if (x1 <= bx && x2 >= ax && z1 <= bz && z2 >= az) {
                return true;
            }
        }
        return false;
    }

    // -- 城墙：能走、能守、能巡逻 -------------------------------------------

    /**
     * 城墙不是一堵墙，是一条环形的路。
     *
     * <p>墙身十二格，墙顶一条走道；外侧女儿墙开垛口，内侧一道矮墙防跌落；
     * 每九十格一座登城梯，塔里留一间守卫室，墙上每隔一段一支火盆。
     * 站上去能绕着城走一圈——这才是"巡逻"。
     */
    private static void walls(BuildKit k) {
        int h = WALL_HEIGHT;
        for (int side = 0; side < 4; side++) {
            boolean horizontal = side < 2;
            int fixed = side % 2 == 0 ? HALF : -HALF;
            int dirIn = fixed > 0 ? -1 : 1;

            for (int t = 0; t < WALL_THICK; t++) {
                int offset = dirIn * t;
                for (int along = -HALF; along <= HALF; along++) {
                    if (Math.abs(along) <= 7) {
                        continue;
                    }
                    int x = horizontal ? along : fixed + offset;
                    int z = horizontal ? fixed + offset : along;
                    for (int y = 0; y <= h; y++) {
                        Block block = (y == 0) ? Palette.STONE_CHISELED
                                : ((y % 6 == 0) ? Palette.STONE_MOSSY : Palette.STONE_BRICK);
                        k.set(x, y, z, block);
                    }
                    if (t == 0 && Math.floorMod(along, 12) == 5) {
                        k.set(x, 7, z, Palette.STONE_CHISELED);
                    }
                }
            }

            for (int along = -HALF; along <= HALF; along++) {
                if (Math.abs(along) <= 7) {
                    continue;
                }
                for (int t = 0; t < WALL_THICK; t++) {
                    int offset = dirIn * t;
                    int x = horizontal ? along : fixed + offset;
                    int z = horizontal ? fixed + offset : along;
                    k.set(x, h + 1, z, Palette.STONE_BRICK);
                    if (t == 0 && Math.floorMod(along, 2) == 0) {
                        k.set(x, h + 2, z, Palette.STONE_BRICK);
                    }
                    if (t == WALL_THICK - 1) {
                        k.set(x, h + 2, z, Palette.STONE_CHISELED);
                    }
                }
            }

            for (int along = -HALF; along <= HALF; along += 3) {
                int x = horizontal ? along : fixed + dirIn * -1;
                int z = horizontal ? fixed + dirIn * -1 : along;
                if (Math.abs(along) > 8) {
                    k.set(x, h - 1, z, Palette.STONE_CHISELED);
                    if (Math.floorMod(along, 9) == 0) {
                        k.set(x, h, z, Palette.STONE_BRICK);
                    }
                }
            }

            for (int along = -HALF + 45; along < HALF - 30; along += 90) {
                wallStairs(k, horizontal, fixed, dirIn, along);
            }

            for (int along = -HALF + 30; along < HALF - 20; along += 48) {
                int x = horizontal ? along : fixed + dirIn * (WALL_THICK - 1);
                int z = horizontal ? fixed + dirIn * (WALL_THICK - 1) : along;
                k.set(x, h + 2, z, Palette.STONE_BRICK);
                k.set(x, h + 3, z, Palette.CAMPFIRE);
            }
        }

        for (int side = 0; side < 4; side++) {
            boolean horizontal = side < 2;
            int fixed = side % 2 == 0 ? HALF : -HALF;
            for (int along = -HALF + 60; along < HALF; along += 60) {
                if (Math.abs(along) < 30) {
                    continue;
                }
                int x = horizontal ? along : fixed;
                int z = horizontal ? fixed : along;
                k.roundTower(x, z, 7, 0, 22, Palette.STONE_BRICK, Palette.STONE_CHISELED,
                        Palette.SLATE_STAIR, 1);
                k.disc(x, z, 5, WALL_HEIGHT + 1, Palette.SPRUCE_PLANK);
                k.set(x + 3, WALL_HEIGHT + 2, z, Palette.CAMPFIRE);
                k.set(x - 3, WALL_HEIGHT + 2, z, Palette.CHEST);
                k.bump("墙塔");
            }
        }

        gate(k, 0, HALF, "south", true);
        // 北门是"仪式门"：门后就是王座大厅的台基，常年闭着，只在加冕与出殡时开
        gate(k, 0, -HALF, "north", false);
        gate(k, HALF, 0, "east", true);
        gate(k, -HALF, 0, "west", true);
        k.bump("城墙");
    }

    private static void wallStairs(BuildKit k, boolean horizontal, int fixed, int dirIn, int along) {
        int h = WALL_HEIGHT;
        for (int i = 0; i <= h; i++) {
            for (int w = 0; w < 3; w++) {
                int x;
                int z;
                if (horizontal) {
                    x = along + w;
                    z = fixed + dirIn * (WALL_THICK - 1);
                } else {
                    x = fixed + dirIn * (WALL_THICK - 1);
                    z = along + w;
                }
                k.set(x, i, z, Block.stairs("minecraft:stone_brick_stairs",
                        horizontal ? "east" : "south"));
                for (int up = 1; up <= 3; up++) {
                    k.set(x, i + up, z, null);
                }
            }
        }
        k.bump("登城梯");
    }

    private static void gate(BuildKit k, int cx, int cz, String facing, boolean open) {
        boolean alongZ = facing.equals("north") || facing.equals("south");
        int dir = facing.equals("south") || facing.equals("east") ? 1 : -1;

        for (int side = -1; side <= 1; side += 2) {
            int tx = alongZ ? cx + side * 13 : cx;
            int tz = alongZ ? cz : cz + side * 13;
            k.roundTower(tx, tz, 8, 0, 26, Palette.STONE_BRICK, Palette.STONE_CHISELED,
                    Palette.SLATE_STAIR, 1);
        }
        for (int dx = -11; dx <= 11; dx++) {
            for (int dz = -WALL_THICK; dz <= WALL_THICK; dz++) {
                for (int y = 0; y < 18; y++) {
                    int x = alongZ ? cx + dx : cx + dz;
                    int z = alongZ ? cz + dz : cz + dx;
                    boolean passage = open && Math.abs(dx) <= 5 && y <= 9;
                    if (passage) {
                        continue;
                    }
                    if (y > 13 && ((dx + dz) & 1) == 0) {
                        continue;
                    }
                    k.set(x, y, z, y == 10 ? Palette.STONE_CHISELED : Palette.STONE_BRICK);
                }
            }
        }
        for (int i = 0; i <= 5; i++) {
            int x = alongZ ? cx - 5 + i : cx;
            int z = alongZ ? cz : cz - 5 + i;
            for (int y = 10 + (5 - Math.abs(i - 5)); y <= 13; y++) {
                k.set(x, y, z, Palette.STONE_CHISELED);
            }
        }
        for (int dx = -4; dx <= 4; dx++) {
            if ((dx & 1) != 0) {
                continue;
            }
            for (int y = 0; y <= 8; y++) {
                int x = alongZ ? cx + dx : cx + dir * 2;
                int z = alongZ ? cz + dir * 2 : cz + dx;
                k.set(x, y, z, Palette.IRON_BARS);
            }
        }
        for (int i = 3; i <= 12; i++) {
            for (int dx = -6; dx <= 6; dx++) {
                int x = alongZ ? cx + dx : cx + dir * i;
                int z = alongZ ? cz + dir * i : cz + dx;
                k.set(x, -1, z, Math.abs(dx) == 6 ? Palette.ANDESITE : Palette.STONE_BRICK);
            }
        }
        int bx = alongZ ? cx : cx + dir * (WALL_THICK + 1);
        int bz = alongZ ? cz + dir * (WALL_THICK + 1) : cz;
        k.fill(bx - 2, 15, bz, bx + 2, 17, bz, Palette.WOOL_RED);
        k.fill(bx - 1, 16, bz, bx + 1, 16, bz, Palette.GOLD);
        for (int side = -1; side <= 1; side += 2) {
            int fx = alongZ ? cx + side * 20 : cx;
            int fz = alongZ ? cz : cz + side * 20;
            k.set(fx, 15, fz, Palette.CAMPFIRE);
        }
        k.bump("城门");
    }

    // -- 河岸 ---------------------------------------------------------------

    /**
     * 运河两岸：不做垂直的切面，做三级石阶 + 一段草坡。
     *
     * <p>水与陆之间要有过渡，人才走得下去、船才靠得住、草才长到边上。
     * 之前的河是一刀切下去的，看着像地形被人用刀裁过。
     */
    private static void banks(BuildKit k) {
        int zWater = 306;
        int zEnd = 404;
        for (int x = -252; x <= -92; x++) {
            for (int i = 0; i < 6; i++) {
                int z = zWater - 1 - i;
                int y = -1 - (i / 2);
                k.set(x, y, z, i < 2 ? Palette.GRASS : Palette.STONE_BRICK);
                if (i == 5) {
                    k.set(x, y + 1, z, Palette.GRASS_TUFT);
                }
            }
            for (int i = 0; i < 6; i++) {
                int z = zEnd + 1 + i;
                int y = -1 - (i / 2);
                k.set(x, y, z, i < 2 ? Palette.GRASS : Palette.STONE_BRICK);
                if (i == 0) {
                    k.set(x, y + 1, z, Palette.GRASS_TUFT);
                }
            }
            if (Math.floorMod(x, 24) == 0) {
                for (int i = 0; i < 5; i++) {
                    k.set(x, -1 - i, zWater - 1 - i, Palette.STONE_CHISELED);
                    k.set(x + 1, -1 - i, zWater - 1 - i, Palette.STONE_CHISELED);
                }
            }
        }
        for (int i = 0; i < 120; i++) {
            int x = -250 + k.rnd.nextInt(156);
            int z = k.rnd.nextBoolean() ? zWater - 1 : zEnd + 1;
            k.set(x, 0, z, Palette.GRASS_TUFT);
        }
        // 一座石桥跨过运河，接上南门大路
        int bx = -8;
        for (int z = 296; z <= 414; z++) {
            for (int x = bx - 5; x <= bx + 5; x++) {
                k.set(x, 1, z, Palette.STONE_BRICK);
                k.set(x, 0, z, Palette.STONE_BRICK);
            }
            for (int x : new int[] {bx - 6, bx + 6}) {
                k.set(x, 2, z, Palette.STONE_BRICK);
            }
        }
        k.bump("河岸");
    }

    // -- 环市集 -------------------------------------------------------------

    private static void marketRing(BuildKit k) {
        for (int x = -PLAZA; x <= PLAZA; x++) {
            for (int z = -PLAZA; z <= PLAZA; z++) {
                double d = Math.sqrt(x * x + z * z);
                Block block;
                if (d < SACRED + 4) {
                    block = Palette.STONE_CHISELED;
                } else if (d < PLAZA - 4) {
                    block = ((int) (d / 7)) % 2 == 0 ? Palette.STONE_BRICK : Palette.ANDESITE;
                } else {
                    block = Palette.ANDESITE_POLISHED;
                }
                if (k.rnd.nextInt(100) < 5) {
                    block = Palette.STONE_CRACKED;
                }
                k.set(x, -1, z, block);
            }
        }
        Block[][] cloth = {
                {Palette.WOOL_RED, Palette.WOOL_CREAM},
                {Palette.WOOL_BLUE, Palette.WOOL_CREAM},
                {Palette.WOOL_GREEN, Palette.WOOL_YELLOW},
                {Palette.WOOL_YELLOW, Palette.WOOL_RED}};
        int index = 0;
        for (int i = -2; i <= 2; i++) {
            int x = i * 24;
            if (Math.abs(x) < 10) {
                continue;
            }
            for (int side = -1; side <= 1; side += 2) {
                Block[] pair = cloth[index++ % cloth.length];
                k.stall(x - 3, side * 86, pair[0], pair[1]);
            }
        }
        for (int i = -2; i <= 2; i++) {
            int z = i * 24;
            if (Math.abs(z) < 10) {
                continue;
            }
            for (int side = -1; side <= 1; side += 2) {
                Block[] pair = cloth[index++ % cloth.length];
                k.stall(side * 86 - 3, z, pair[0], pair[1]);
            }
        }
        for (int x : new int[] {-96, 96}) {
            for (int z : new int[] {-96, 96}) {
                Civic.well(k, x, z);
                k.lamp(x, z + 6);
                k.lamp(x + 6, z);
            }
        }
        k.bump("环市集");
    }

    // -- 市井 ---------------------------------------------------------------

    /** 过日子的一条街：面包、酒、肉、鱼、布、药。 */
    private static void life(BuildKit k) {
        String[] types = {"bakery", "brewery", "butcher", "fish", "tailor", "apothecary",
                "chandler", "potter", "weaver", "tanner"};
        int index = 0;
        for (int z = 150; z <= 270; z += 20) {
            for (int side = -1; side <= 1; side += 2) {
                int x = side > 0 ? 22 : -26;
                if (k.openAir(x, z, x + 14, z + 12) && !k.paved(x, z, x + 14, z + 12)) {
                    Civic.shop(k, x, z, side > 0 ? "west" : "east", types[index++ % types.length]);
                }
            }
        }
        k.pave(20, 280, 60, 296, Palette.COARSE, Palette.PATH);
        for (int i = 0; i < 6; i++) {
            k.set(24 + i * 6, 0, 284, Palette.HAY);
            k.set(24 + i * 6, 0, 290, Palette.BARREL);
        }
        k.bump("市井");
    }

    // -- 房子 ---------------------------------------------------------------

    private static void houses(BuildKit k) {
        int[] xs = {-296, -240, -160, -80, 80, 160, 240, 296};
        int[] zs = {-296, -240, -160, -80, 80, 160, 240, 296};
        for (int i = 0; i < xs.length - 1; i++) {
            for (int j = 0; j < zs.length - 1; j++) {
                int x1 = xs[i] + 4;
                int x2 = xs[i + 1] - 4;
                int z1 = zs[j] + 4;
                int z2 = zs[j + 1] - 4;
                if (x2 - x1 < 26 || z2 - z1 < 26) {
                    continue;
                }
                if (reserved(x1, z1, x2, z2)) {
                    continue;
                }
                if (k.rnd.nextInt(100) < 8) {
                    square(k, x1, z1, x2, z2);
                    continue;
                }
                block(k, x1, z1, x2, z2);
            }
        }
        for (int side = 0; side < 4; side++) {
            boolean horizontal = side < 2;
            int fixed = side % 2 == 0 ? HALF - 12 : -HALF + 12;
            for (int along = -HALF + 30; along < HALF - 30; along += 26) {
                int x = horizontal ? along : fixed;
                int z = horizontal ? fixed : along;
                if (!k.free(x - 7, z - 6, x + 7, z + 6)) {
                    continue;
                }
                // 这一圈是贴墙的房子，最容易漏掉预留表——它们曾经长进了王室居所的院子里
                if (intersectsReserved(x - 7, z - 6, x + 7, z + 6)) {
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

    private static void block(BuildKit k, int x1, int z1, int x2, int z2) {
        int inset = 16;
        northRow(k, x1 + 2, z1, x2 - 2, "north");
        southRow(k, x1 + 2, z2, x2 - 2, "south");
        if (z2 - z1 > 50) {
            westRow(k, x1, z1 + inset, z2 - inset, "west");
            eastRow(k, x2, z1 + inset, z2 - inset, "east");
        }
        // 内院会铺草：压在街上就是把大道铺成草坪
        if (!onStreet(x1 + 14, z1 + 14, x2 - 14, z2 - 14)) {
            courtyard(k, x1 + 14, z1 + 14, x2 - 14, z2 - 14);
        }
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
            if (k.free(x, z, x + w - 1, z + d - 1)
                    && !intersectsReserved(x, z, x + w - 1, z + d - 1)
                    && !onStreet(x, z, x + w - 1, z + d - 1)) {
                k.house(x, z, w, d, 2 + (k.rnd.nextInt(10) < 4 ? 1 : 0), facing,
                        k.rnd.nextInt(100) < 15);
            }
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
            if (k.free(x, z - d + 1, x + w - 1, z)
                    && !intersectsReserved(x, z - d + 1, x + w - 1, z)
                    && !onStreet(x, z - d + 1, x + w - 1, z)) {
                k.house(x, z - d + 1, w, d, 2 + (k.rnd.nextInt(10) < 4 ? 1 : 0), facing,
                        k.rnd.nextInt(100) < 15);
            }
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
            if (k.free(x, z, x + w - 1, z + d - 1)
                    && !intersectsReserved(x, z, x + w - 1, z + d - 1)
                    && !onStreet(x, z, x + w - 1, z + d - 1)) {
                k.house(x, z, w, d, 2, facing, k.rnd.nextInt(100) < 20);
            }
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
            if (k.free(x - w + 1, z, x, z + d - 1)
                    && !intersectsReserved(x - w + 1, z, x, z + d - 1)
                    && !onStreet(x - w + 1, z, x, z + d - 1)) {
                k.house(x - w + 1, z, w, d, 2, facing, k.rnd.nextInt(100) < 20);
            }
            z += d + 1;
        }
    }

    private static void courtyard(BuildKit k, int x1, int z1, int x2, int z2) {
        if (x2 - x1 < 8 || z2 - z1 < 8) {
            return;
        }
        k.pave(x1, z1, x2, z2, Palette.GRASS, Palette.PATH);
        if (x2 - x1 > 14 && z2 - z1 > 14 && k.rnd.nextBoolean()) {
            Civic.well(k, x1 + (x2 - x1) / 2, z1 + (z2 - z1) / 2);
        }
        for (int i = 0; i < 2; i++) {
            int tx = x1 + 3 + k.rnd.nextInt(Math.max(1, x2 - x1 - 5));
            int tz = z1 + 3 + k.rnd.nextInt(Math.max(1, z2 - z1 - 5));
            if (k.free(tx - 1, tz - 1, tx + 1, tz + 1)) {
                k.tree(tx, tz, 4 + k.rnd.nextInt(3), k.rnd.nextBoolean());
            }
        }
    }

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
        k.bump("小广场");
    }

    // -- 补空 ---------------------------------------------------------------

    private static void fillGaps(BuildKit k) {
        int step = 26;
        for (int x = -HALF + 8; x < HALF - 20; x += step) {
            for (int z = -HALF + 8; z < HALF - 20; z += step) {
                int x2 = x + 18;
                int z2 = z + 18;
                if (x2 > HALF - 6 || z2 > HALF - 6) {
                    continue;
                }
                if (!k.openAir(x, z, x2, z2) || k.paved(x, z, x2, z2)
                        || intersectsReserved(x, z, x2, z2)) {
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
        k.pave(-7, HALF, 7, HALF + 120, Palette.PATH, Palette.COARSE);
        k.pave(-7, -HALF - 140, 7, -HALF, Palette.PATH, Palette.COARSE);
        k.pave(HALF, -7, HALF + 240, 7, Palette.PATH, Palette.COARSE);
        k.pave(-HALF - 240, -7, -HALF, 7, Palette.PATH, Palette.COARSE);
        for (int i = 0; i < 4; i++) {
            k.lamp(9, HALF + 20 + i * 26);
            k.lamp(-9, -HALF - 20 - i * 26);
            k.lamp(HALF + 20 + i * 40, 9);
        }
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

    /** 街灯与火盆：夜里这条路要看得见。 */
    private static void lights(BuildKit k) {
        // 四角火盆挪到 ±104：原来的 ±81 落在神职院正中，会把院子点着
        for (int i = 0; i < 4; i++) {
            double angle = Math.PI / 4 + i * Math.PI / 2;
            int x = (int) Math.round(Math.cos(angle) * (PLAZA + 2));
            int z = (int) Math.round(Math.sin(angle) * (PLAZA + 2));
            k.fill(x, 0, z, x, 4, z, Palette.STONE_BRICK);
            k.set(x, 5, z, Palette.CAMPFIRE);
        }
        for (int z = 140; z <= 290; z += 30) {
            k.lamp(-12, z);
            k.lamp(12, z);
        }
        for (int x = 140; x <= 290; x += 30) {
            k.lamp(x, -12);
            k.lamp(x, 12);
        }
        // 仪式道两侧：只铺到环路为止。再往北就是台地，灯会埋进挡土墙里
        for (int z = -150; z <= -124; z += 12) {
            k.lamp(-13, z);
            k.lamp(13, z);
        }
    }

    public static String summary() {
        return String.format(Locale.ROOT, "%d×%d 城墙 · %d 个分区 · 圣座金字塔 / 王座大厅 / 角斗场",
                HALF * 2, HALF * 2, DISTRICTS.size());
    }

    /**
     * 出生点：圣座顶坛，祭坛正中往南一格。
     *
     * <p>为什么偏一格外：祭坛正中那一格是金柱的底座，站上去会卡在方块里。
     * 往南一步仍然在 5×5 的金盘上，脚下是台面，头顶三格净空，退一步落到顶层平台也才两格高。
     */
    public static int[] spawn() {
        return new int[] {0, 1};
    }

    /**
     * 出生点的高度（场景坐标）。落地时是 {@code ground + 这个值}。
     *
     * <p>顶坛的金盘在 y=66，所以脚底站在 67。
     */
    public static int spawnHeight() {
        return 67;
    }

    public static int groundRadius() {
        return HALF + 8;
    }

    /** 给单测用：预留表与街道表都只读。它们错了不会报错，只会让路断掉。 */
    static java.util.List<int[]> reservedAreas() {
        return RESERVED;
    }
}
