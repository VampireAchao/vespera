package io.github.vampireachao.vespera.core.voxel;

import java.util.HashMap;
import java.util.Map;

/**
 * 建材表：一边是方块的 id，一边是它在图上是什么颜色。
 *
 * <p>两份东西放在一起，是因为它们必须一起改。改了屋顶材质却忘了改颜色，图上就会出现
 * 一块不存在的灰色；那种图比没有图更糟，因为它让你以为已经检查过了。
 */
public final class Palette {

    // -- 石 ---------------------------------------------------------------
    public static final Block STONE = Block.of("minecraft:stone");
    public static final Block STONE_BRICK = Block.of("minecraft:stone_bricks");
    public static final Block STONE_CRACKED = Block.of("minecraft:cracked_stone_bricks");
    public static final Block STONE_MOSSY = Block.of("minecraft:mossy_stone_bricks");
    public static final Block STONE_CHISELED = Block.of("minecraft:chiseled_stone_bricks");
    public static final Block COBBLE = Block.of("minecraft:cobblestone");
    public static final Block COBBLE_MOSSY = Block.of("minecraft:mossy_cobblestone");
    public static final Block GRAVEL = Block.of("minecraft:gravel");
    public static final Block ANDESITE = Block.of("minecraft:andesite");
    public static final Block ANDESITE_POLISHED = Block.of("minecraft:polished_andesite");
    public static final Block DEEPSLATE_TILE = Block.of("minecraft:deepslate_tiles");
    public static final Block DEEPSLATE_BRICK = Block.of("minecraft:deepslate_bricks");
    public static final Block BLACKSTONE = Block.of("minecraft:polished_blackstone_bricks");
    public static final Block SANDSTONE = Block.of("minecraft:sandstone");
    public static final Block TERRACOTTA = Block.of("minecraft:terracotta");

    // -- 木与墙 ------------------------------------------------------------
    public static final Block PLASTER = Block.of("minecraft:white_terracotta");
    public static final Block PLASTER_CREAM = Block.of("minecraft:smooth_sandstone");
    public static final Block PLASTER_GRAY = Block.of("minecraft:light_gray_terracotta");
    public static final Block PLASTER_OCHRE = Block.of("minecraft:yellow_terracotta");
    public static final Block DARK_LOG = Block.of("minecraft:dark_oak_log");
    public static final Block DARK_PLANK = Block.of("minecraft:dark_oak_planks");
    public static final Block SPRUCE_LOG = Block.of("minecraft:spruce_log");
    public static final Block SPRUCE_PLANK = Block.of("minecraft:spruce_planks");
    public static final Block OAK_LOG = Block.of("minecraft:oak_log");
    public static final Block OAK_PLANK = Block.of("minecraft:oak_planks");
    public static final Block OAK_FENCE = Block.of("minecraft:oak_fence");
    public static final Block SPRUCE_FENCE = Block.of("minecraft:spruce_fence");
    public static final Block DARK_FENCE = Block.of("minecraft:dark_oak_fence");
    public static final Block OAK_TRAPDOOR = Block.of("minecraft:oak_trapdoor", "facing=north,half=top,open=true");

    // -- 屋顶 --------------------------------------------------------------
    public static final Block TILE = Block.of("minecraft:bricks");
    public static final Block SLATE = Block.of("minecraft:deepslate_tiles");
    public static final Block SHINGLE = Block.of("minecraft:spruce_planks");
    public static final String TILE_STAIR = "minecraft:brick_stairs";
    public static final String SLATE_STAIR = "minecraft:deepslate_tile_stairs";
    public static final String SHINGLE_STAIR = "minecraft:spruce_stairs";
    public static final String TILE_SLAB = "minecraft:brick_slab";
    public static final String SLATE_SLAB = "minecraft:deepslate_tile_slab";

    // -- 玻璃与光 ----------------------------------------------------------
    public static final Block GLASS = Block.of("minecraft:glass_pane");
    public static final Block GLASS_BROWN = Block.of("minecraft:brown_stained_glass_pane");
    public static final Block GLASS_GRAY = Block.of("minecraft:light_gray_stained_glass_pane");
    public static final Block GLASS_BLUE = Block.of("minecraft:light_blue_stained_glass_pane");
    public static final Block GLASS_ROSE = Block.of("minecraft:red_stained_glass_pane");
    public static final Block LANTERN = Block.of("minecraft:lantern", "hanging=false");
    public static final Block LANTERN_HANGING = Block.of("minecraft:lantern", "hanging=true");
    public static final Block TORCH = Block.of("minecraft:torch");
    public static final Block CAMPFIRE = Block.of("minecraft:campfire", "lit=true");
    public static final Block SOUL_LANTERN = Block.of("minecraft:soul_lantern", "hanging=false");

    // -- 地 ---------------------------------------------------------------
    public static final Block GRASS = Block.of("minecraft:grass_block", "snowy=false");
    public static final Block DIRT = Block.of("minecraft:dirt");
    public static final Block COARSE = Block.of("minecraft:coarse_dirt");
    public static final Block PATH = Block.of("minecraft:dirt_path");
    public static final Block FARMLAND = Block.of("minecraft:farmland", "moisture=7");
    public static final Block WATER = Block.of("minecraft:water", "level=0");
    public static final Block SAND = Block.of("minecraft:sand");
    public static final Block CLAY = Block.of("minecraft:clay");
    public static final Block SNOW = Block.of("minecraft:snow_block");

    // -- 作物与植被 --------------------------------------------------------
    public static final Block WHEAT = Block.of("minecraft:wheat", "age=7");
    public static final Block CARROT = Block.of("minecraft:carrots", "age=7");
    public static final Block POTATO = Block.of("minecraft:potatoes", "age=7");
    public static final Block BEET = Block.of("minecraft:beetroots", "age=3");
    public static final Block HAY = Block.of("minecraft:hay_block", "axis=y");

    /**
     * 一扇门。门是两格高的：下半 {@code half=lower} 是门板，上半 {@code half=upper} 是门楣那半扇。
     *
     * <p>这两格必须成对写，且 {@code facing} 要跟墙的走向一致——门装错了朝向，
     * 方块照样存在，但它在墙里是斜着卡的，玩家右键打不开，看上去就是"这栋房子没有门"。
     *
     * @param wood   oak / spruce / birch / dark_oak / jungle / acacia / iron
     * @param facing 门所朝的方向（也是它开出去的方向）
     */
    public static Block door(String wood, String facing, boolean upper) {
        return Block.of("minecraft:" + wood + "_door",
                "facing=" + facing + ",half=" + (upper ? "upper" : "lower")
                        + ",hinge=left,open=false,powered=false");
    }
    public static final Block OAK_LEAVES = Block.of("minecraft:oak_leaves", "persistent=true");
    public static final Block DARK_LEAVES = Block.of("minecraft:dark_oak_leaves", "persistent=true");
    public static final Block SPRUCE_LEAVES = Block.of("minecraft:spruce_leaves", "persistent=true");
    public static final Block GRASS_TUFT = Block.of("minecraft:short_grass");
    public static final Block FLOWER_RED = Block.of("minecraft:poppy");
    public static final Block FLOWER_YELLOW = Block.of("minecraft:dandelion");
    public static final Block VINE = Block.of("minecraft:vine", "up=true");

    // -- 金属与织物 --------------------------------------------------------
    public static final Block IRON = Block.of("minecraft:iron_block");
    public static final Block IRON_BARS = Block.of("minecraft:iron_bars");
    public static final Block GOLD = Block.of("minecraft:gold_block");
    public static final Block COPPER = Block.of("minecraft:copper_block");
    public static final Block COPPER_OXIDIZED = Block.of("minecraft:oxidized_copper");
    // 1.21.9 之后 chain 拆成了 iron_chain / copper_chain，老的 minecraft:chain 已经不存在。
    // 服务端每次启动都会为它打一行"方块状态不认识"，然后跳过——灯笼全都挂在天上
    public static final Block CHAIN = Block.of("minecraft:iron_chain", "axis=y");
    public static final Block WOOL_RED = Block.of("minecraft:red_wool");
    public static final Block WOOL_BLUE = Block.of("minecraft:blue_wool");
    public static final Block WOOL_YELLOW = Block.of("minecraft:yellow_wool");
    public static final Block WOOL_GREEN = Block.of("minecraft:green_wool");
    public static final Block WOOL_CREAM = Block.of("minecraft:white_wool");
    public static final Block WOOL_BLACK = Block.of("minecraft:black_wool");
    public static final Block CARPET_RED = Block.of("minecraft:red_carpet");

    // -- 陈设 --------------------------------------------------------------
    public static final Block BARREL = Block.of("minecraft:barrel", "facing=up");
    public static final Block CHEST = Block.of("minecraft:chest", "facing=south");
    public static final Block CRATE = Block.of("minecraft:oak_trapdoor", "facing=north,half=bottom,open=false");
    public static final Block CRAFTING = Block.of("minecraft:crafting_table");
    public static final Block ANVIL = Block.of("minecraft:anvil", "facing=south");
    public static final Block SMITHING = Block.of("minecraft:smithing_table");
    public static final Block SMOKER = Block.of("minecraft:smoker", "facing=south,lit=true");
    public static final Block FURNACE = Block.of("minecraft:furnace", "facing=south,lit=true");
    public static final Block BLAST = Block.of("minecraft:blast_furnace", "facing=south,lit=true");
    public static final Block BOOKSHELF = Block.of("minecraft:bookshelf");
    public static final Block LECTERN = Block.of("minecraft:lectern", "facing=south");
    public static final Block CANDLE = Block.of("minecraft:candle", "lit=true");
    public static final Block CAULDRON = Block.of("minecraft:cauldron");
    public static final Block POT = Block.of("minecraft:flower_pot");
    public static final Block BELL = Block.of("minecraft:bell", "attachment=floor,facing=north");
    public static final Block SIGN = Block.of("minecraft:oak_sign", "rotation=8,waterlogged=false");
    public static final Block CART = Block.of("minecraft:minecart");
    public static final Block SCAFFOLD = Block.of("minecraft:scaffolding", "bottom=false");

    private static final Map<String, Integer> COLORS = new HashMap<>();

    static {
        // 石
        color("stone", 0x8b8b88);
        color("stone_bricks", 0xaaa193);
        color("cracked_stone_bricks", 0x8d877c);
        color("mossy_stone_bricks", 0x86906f);
        color("chiseled_stone_bricks", 0xa39d90);
        color("cobblestone", 0x8c8883);
        color("mossy_cobblestone", 0x74806a);
        color("gravel", 0x8e877a);
        color("andesite", 0x8a8a86);
        color("polished_andesite", 0x9e9e9a);
        color("deepslate_tiles", 0x54555b);
        color("deepslate_bricks", 0x4b4c50);
        color("polished_blackstone_bricks", 0x35343a);
        color("sandstone", 0xd8cba4);
        color("sandstone_stairs", 0xd8cba4);
        color("smooth_sandstone", 0xdfd2ae);
        color("terracotta", 0x9a5b40);

        // 木与墙
        color("white_terracotta", 0xd9cbb6);
        color("smooth_sandstone", 0xdfd2ae);
        color("light_gray_terracotta", 0xa9a093);
        color("yellow_terracotta", 0xc9a24a);
        color("dark_oak_log", 0x3f2c19);
        color("dark_oak_planks", 0x4b3419);
        color("spruce_log", 0x624428);
        color("spruce_planks", 0x7a5734);
        color("oak_log", 0x9c7b4d);
        color("oak_planks", 0xb08a52);
        // 门：比同色的木板略深一档，不然在立面上一整片看不出哪是门
        color("oak_door", 0x8f6b3c);
        color("spruce_door", 0x5c4326);
        color("birch_door", 0xc9bb88);
        color("dark_oak_door", 0x3d2a14);
        color("jungle_door", 0xa0703f);
        color("acacia_door", 0xa85c33);
        color("iron_door", 0xb9bcc0);
        color("oak_fence", 0xa98450);
        color("spruce_fence", 0x6d4c2c);
        color("dark_oak_fence", 0x453019);

        // 屋顶
        color("bricks", 0xab5a3f);
        color("brick_stairs", 0xab5a3f);
        color("brick_slab", 0xab5a3f);
        color("deepslate_tile_stairs", 0x54555b);
        color("deepslate_tile_slab", 0x45464a);
        color("spruce_stairs", 0x74512f);
        color("spruce_slab", 0x74512f);

        // 玻璃与光
        color("glass_pane", 0xc9dde4);
        color("glass", 0xc9dde4);
        color("brown_stained_glass_pane", 0x6b4a2e);
        color("light_gray_stained_glass_pane", 0xb9b8b1);
        color("light_blue_stained_glass_pane", 0x8fc0dd);
        color("red_stained_glass_pane", 0xc0503f);
        color("lantern", 0xf0c56a);
        color("soul_lantern", 0x7fc9c9);
        color("torch", 0xf3c968);
        color("campfire", 0xd08a3a);
        color("chain", 0x4a4a4d);
        color("iron_chain", 0x4a4a4d);

        // 地
        color("grass_block", 0x6f9a4c);
        color("dirt", 0x8a6a45);
        color("coarse_dirt", 0x7d5e3c);
        color("dirt_path", 0x9d7f52);
        color("farmland", 0x6d4f31);
        color("water", 0x3f6f9e);
        color("sand", 0xd8cc9a);
        color("clay", 0x9fa3a9);
        color("snow_block", 0xe9eef2);

        // 作物
        color("wheat", 0xd8c064);
        color("carrots", 0x4f8a3a);
        color("potatoes", 0x4a7d3c);
        color("beetroots", 0x6a4a3a);
        color("hay_block", 0xc7a34a);
        color("oak_leaves", 0x4d7a34);
        color("dark_oak_leaves", 0x34532a);
        color("spruce_leaves", 0x3b6135);
        color("short_grass", 0x679a46);
        color("poppy", 0xa83a34);
        color("dandelion", 0xd6c24a);
        color("vine", 0x4a6b33);

        // 金属与织物
        color("iron_block", 0xd2d2d2);
        color("iron_bars", 0x8d8d90);
        color("gold_block", 0xefcf4a);
        color("copper_block", 0xb06b4a);
        color("oxidized_copper", 0x4f9e8a);
        color("red_wool", 0xa33131);
        color("blue_wool", 0x334f86);
        color("yellow_wool", 0xd0b646);
        color("green_wool", 0x3c703c);
        color("white_wool", 0xe2ded4);
        color("black_wool", 0x24221f);
        color("red_carpet", 0xa33131);

        // 陈设
        color("barrel", 0x8a6538);
        color("chest", 0x9c7436);
        color("crafting_table", 0x8b6534);
        color("anvil", 0x3d3d42);
        color("smithing_table", 0x3b4a5a);
        color("smoker", 0x5c5148);
        color("furnace", 0x757575);
        color("blast_furnace", 0x5a5a5c);
        color("bookshelf", 0x8a6a42);
        color("lectern", 0x9c7a4c);
        color("cauldron", 0x55565a);
        color("flower_pot", 0xa06a4a);
        color("bell", 0xcfae4e);
        color("oak_sign", 0xa98450);
        color("oak_trapdoor", 0xa98450);
        color("minecart", 0x6a6a6e);
        color("scaffolding", 0xb8965a);
    }

    private Palette() {
    }

    private static void color(String name, int rgb) {
        COLORS.put(name, rgb);
    }

    /** 楼梯对应的实心料：屋面垫层用它，颜色才对得上。 */
    public static Block roofBody(String stairId) {
        return switch (stairId) {
            case "minecraft:brick_stairs" -> TILE;
            case "minecraft:deepslate_tile_stairs" -> SLATE;
            default -> SHINGLE;
        };
    }

    /** 未知方块给一个稳定的灰：它不会好看，但也不会让整张图崩掉。 */
    public static int colorOf(String id) {
        String name = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        Integer rgb = COLORS.get(name);
        if (rgb != null) {
            return rgb;
        }
        int hash = name.hashCode();
        int r = 110 + Math.floorMod(hash, 40);
        int g = 110 + Math.floorMod(hash >> 8, 40);
        int b = 110 + Math.floorMod(hash >> 16, 40);
        return (r << 16) | (g << 8) | b;
    }
}
