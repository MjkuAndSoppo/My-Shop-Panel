package com.example.myshoppanel.shop;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 内置默认价格表。
 * 优先级：ProjectE EMC > 默认价格表 > 1.0
 */
public class DefaultPricing {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 注册名 → 单价 */
    private static final Map<String, Long> ITEM_PRICES = new LinkedHashMap<>();
    /** 标签 → 单价 */
    private static final Map<String, Long> TAG_PRICES = new LinkedHashMap<>();
    /** NBT物品: 注册名 → (NBT字符串 → 单价) */
    private static final Map<String, Map<String, Long>> NBT_PRICES = new LinkedHashMap<>();

    private static volatile boolean initialized = false;

    private static void init() {
        if (initialized) return;
        initialized = true;

        // ==================== 标签定价 ====================
        TAG_PRICES.put("forge:crops/beetroot", 64L);
        TAG_PRICES.put("forge:crops/carrot", 64L);
        TAG_PRICES.put("forge:crops/nether_wart", 24L);
        TAG_PRICES.put("forge:crops/potato", 64L);
        TAG_PRICES.put("forge:crops/wheat", 24L);
        TAG_PRICES.put("forge:dusts/glowstone", 384L);
        TAG_PRICES.put("forge:dusts/redstone", 64L);
        TAG_PRICES.put("forge:gems/diamond", 8192L);
        TAG_PRICES.put("forge:gems/emerald", 16384L);
        TAG_PRICES.put("forge:gems/quartz", 256L);
        TAG_PRICES.put("forge:nether_stars", 139264L);
        TAG_PRICES.put("forge:rods/blaze", 768L);
        TAG_PRICES.put("forge:rods/wooden", 4L);
        TAG_PRICES.put("forge:seeds/beetroot", 16L);
        TAG_PRICES.put("forge:seeds/wheat", 16L);
        TAG_PRICES.put("minecraft:decorated_pot_sherds", 216L);
        TAG_PRICES.put("minecraft:leaves", 1L);
        TAG_PRICES.put("minecraft:logs", 32L);
        TAG_PRICES.put("minecraft:music_discs", 2048L);
        TAG_PRICES.put("minecraft:planks", 8L);
        TAG_PRICES.put("minecraft:saplings", 32L);
        TAG_PRICES.put("minecraft:small_flowers", 16L);
        TAG_PRICES.put("minecraft:tall_flowers", 32L);
        TAG_PRICES.put("minecraft:wool", 48L);

        // ==================== 物品精确定价 ====================
        ITEM_PRICES.put("minecraft:andesite", 16L);
        ITEM_PRICES.put("minecraft:apple", 128L);
        ITEM_PRICES.put("minecraft:bamboo", 32L);
        ITEM_PRICES.put("minecraft:basalt", 4L);
        ITEM_PRICES.put("minecraft:beef", 64L);
        ITEM_PRICES.put("minecraft:big_dripleaf", 32L);
        ITEM_PRICES.put("minecraft:blackstone", 4L);
        ITEM_PRICES.put("minecraft:blaze_rod", 1536L);
        ITEM_PRICES.put("minecraft:bone", 144L);
        ITEM_PRICES.put("minecraft:brain_coral", 16L);
        ITEM_PRICES.put("minecraft:brain_coral_block", 64L);
        ITEM_PRICES.put("minecraft:brain_coral_fan", 16L);
        ITEM_PRICES.put("minecraft:brown_mushroom", 32L);
        ITEM_PRICES.put("minecraft:bubble_coral", 16L);
        ITEM_PRICES.put("minecraft:bubble_coral_block", 64L);
        ITEM_PRICES.put("minecraft:bubble_coral_fan", 16L);
        ITEM_PRICES.put("minecraft:cactus", 8L);
        ITEM_PRICES.put("minecraft:calcite", 32L);
        ITEM_PRICES.put("minecraft:chicken", 64L);
        ITEM_PRICES.put("minecraft:chorus_flower", 96L);
        ITEM_PRICES.put("minecraft:chorus_fruit", 192L);
        ITEM_PRICES.put("minecraft:chorus_plant", 64L);
        ITEM_PRICES.put("minecraft:clay_ball", 16L);
        ITEM_PRICES.put("minecraft:coal", 128L);
        ITEM_PRICES.put("minecraft:coast_armor_trim_smithing_template", 12271L);
        ITEM_PRICES.put("minecraft:cobbled_deepslate", 2L);
        ITEM_PRICES.put("minecraft:cobblestone", 1L);
        ITEM_PRICES.put("minecraft:cobweb", 12L);
        ITEM_PRICES.put("minecraft:cocoa_beans", 64L);
        ITEM_PRICES.put("minecraft:cod", 64L);
        ITEM_PRICES.put("minecraft:creeper_head", 256L);
        ITEM_PRICES.put("minecraft:crimson_fungus", 32L);
        ITEM_PRICES.put("minecraft:crimson_roots", 1L);
        ITEM_PRICES.put("minecraft:crying_obsidian", 768L);
        ITEM_PRICES.put("minecraft:dead_brain_coral", 1L);
        ITEM_PRICES.put("minecraft:dead_brain_coral_block", 4L);
        ITEM_PRICES.put("minecraft:dead_brain_coral_fan", 1L);
        ITEM_PRICES.put("minecraft:dead_bubble_coral", 1L);
        ITEM_PRICES.put("minecraft:dead_bubble_coral_block", 4L);
        ITEM_PRICES.put("minecraft:dead_bubble_coral_fan", 1L);
        ITEM_PRICES.put("minecraft:dead_bush", 1L);
        ITEM_PRICES.put("minecraft:dead_fire_coral", 1L);
        ITEM_PRICES.put("minecraft:dead_fire_coral_block", 4L);
        ITEM_PRICES.put("minecraft:dead_fire_coral_fan", 1L);
        ITEM_PRICES.put("minecraft:dead_horn_coral", 1L);
        ITEM_PRICES.put("minecraft:dead_horn_coral_block", 4L);
        ITEM_PRICES.put("minecraft:dead_horn_coral_fan", 1L);
        ITEM_PRICES.put("minecraft:dead_tube_coral", 1L);
        ITEM_PRICES.put("minecraft:dead_tube_coral_block", 4L);
        ITEM_PRICES.put("minecraft:dead_tube_coral_fan", 1L);
        ITEM_PRICES.put("minecraft:diorite", 16L);
        ITEM_PRICES.put("minecraft:dirt", 1L);
        ITEM_PRICES.put("minecraft:dragon_egg", 262144L);
        ITEM_PRICES.put("minecraft:dune_armor_trim_smithing_template", 53898L);
        ITEM_PRICES.put("minecraft:echo_shard", 192L);
        ITEM_PRICES.put("minecraft:egg", 32L);
        ITEM_PRICES.put("minecraft:end_stone", 1L);
        ITEM_PRICES.put("minecraft:ender_pearl", 1024L);
        ITEM_PRICES.put("minecraft:eye_armor_trim_smithing_template", 23017L);
        ITEM_PRICES.put("minecraft:feather", 48L);
        ITEM_PRICES.put("minecraft:fern", 1L);
        ITEM_PRICES.put("minecraft:filled_map", 1472L);
        ITEM_PRICES.put("minecraft:fire_coral", 16L);
        ITEM_PRICES.put("minecraft:fire_coral_block", 64L);
        ITEM_PRICES.put("minecraft:fire_coral_fan", 16L);
        ITEM_PRICES.put("minecraft:flint", 4L);
        ITEM_PRICES.put("minecraft:ghast_tear", 4096L);
        ITEM_PRICES.put("minecraft:glow_berries", 16L);
        ITEM_PRICES.put("minecraft:glow_lichen", 8L);
        ITEM_PRICES.put("minecraft:granite", 16L);
        ITEM_PRICES.put("minecraft:grass", 1L);
        ITEM_PRICES.put("minecraft:gravel", 4L);
        ITEM_PRICES.put("minecraft:gunpowder", 192L);
        ITEM_PRICES.put("minecraft:heart_of_the_sea", 32768L);
        ITEM_PRICES.put("minecraft:honey_bottle", 48L);
        ITEM_PRICES.put("minecraft:horn_coral", 16L);
        ITEM_PRICES.put("minecraft:horn_coral_block", 64L);
        ITEM_PRICES.put("minecraft:horn_coral_fan", 16L);
        ITEM_PRICES.put("minecraft:host_armor_trim_smithing_template", 10176L);
        ITEM_PRICES.put("minecraft:ice", 1L);
        ITEM_PRICES.put("minecraft:ink_sac", 16L);
        ITEM_PRICES.put("minecraft:kelp", 1L);
        ITEM_PRICES.put("minecraft:lapis_lazuli", 864L);
        ITEM_PRICES.put("minecraft:large_fern", 1L);
        ITEM_PRICES.put("minecraft:lily_pad", 16L);
        ITEM_PRICES.put("minecraft:magma_block", 128L);
        ITEM_PRICES.put("minecraft:mangrove_roots", 4L);
        ITEM_PRICES.put("minecraft:melon_slice", 16L);
        ITEM_PRICES.put("minecraft:moss_block", 12L);
        ITEM_PRICES.put("minecraft:mutton", 64L);
        ITEM_PRICES.put("minecraft:name_tag", 192L);
        ITEM_PRICES.put("minecraft:nautilus_shell", 1024L);
        ITEM_PRICES.put("minecraft:nether_sprouts", 1L);
        ITEM_PRICES.put("minecraft:netherite_scrap", 12288L);
        ITEM_PRICES.put("minecraft:netherite_upgrade_smithing_template", 7497L);
        ITEM_PRICES.put("minecraft:netherrack", 1L);
        ITEM_PRICES.put("minecraft:obsidian", 64L);
        ITEM_PRICES.put("minecraft:phantom_membrane", 192L);
        ITEM_PRICES.put("minecraft:piglin_banner_pattern", 512L);
        ITEM_PRICES.put("minecraft:piglin_head", 256L);
        ITEM_PRICES.put("minecraft:pink_petals", 4L);
        ITEM_PRICES.put("minecraft:pointed_dripstone", 16L);
        ITEM_PRICES.put("minecraft:poisonous_potato", 64L);
        ITEM_PRICES.put("minecraft:porkchop", 64L);
        ITEM_PRICES.put("minecraft:prismarine_crystals", 512L);
        ITEM_PRICES.put("minecraft:prismarine_shard", 256L);
        ITEM_PRICES.put("minecraft:pufferfish", 64L);
        ITEM_PRICES.put("minecraft:pumpkin", 144L);
        ITEM_PRICES.put("minecraft:rabbit", 64L);
        ITEM_PRICES.put("minecraft:rabbit_foot", 128L);
        ITEM_PRICES.put("minecraft:rabbit_hide", 16L);
        ITEM_PRICES.put("minecraft:raiser_armor_trim_smithing_template", 10176L);
        ITEM_PRICES.put("minecraft:red_mushroom", 32L);
        ITEM_PRICES.put("minecraft:red_sand", 1L);
        ITEM_PRICES.put("minecraft:rib_armor_trim_smithing_template", 10310L);
        ITEM_PRICES.put("minecraft:rotten_flesh", 32L);
        ITEM_PRICES.put("minecraft:saddle", 192L);
        ITEM_PRICES.put("minecraft:salmon", 64L);
        ITEM_PRICES.put("minecraft:sand", 1L);
        ITEM_PRICES.put("minecraft:sculk_catalyst", 8040L);
        ITEM_PRICES.put("minecraft:sculk_vein", 4L);
        ITEM_PRICES.put("minecraft:scute", 96L);
        ITEM_PRICES.put("minecraft:sea_pickle", 16L);
        ITEM_PRICES.put("minecraft:seagrass", 1L);
        ITEM_PRICES.put("minecraft:sentry_armor_trim_smithing_template", 57345L);
        ITEM_PRICES.put("minecraft:shaper_armor_trim_smithing_template", 10176L);
        ITEM_PRICES.put("minecraft:shulker_shell", 2048L);
        ITEM_PRICES.put("minecraft:silence_armor_trim_smithing_template", 39465L);
        ITEM_PRICES.put("minecraft:skeleton_skull", 256L);
        ITEM_PRICES.put("minecraft:slime_ball", 32L);
        ITEM_PRICES.put("minecraft:small_dripleaf", 24L);
        ITEM_PRICES.put("minecraft:sniffer_egg", 2048L);
        ITEM_PRICES.put("minecraft:snout_armor_trim_smithing_template", 7533L);
        ITEM_PRICES.put("minecraft:snow", 1L);
        ITEM_PRICES.put("minecraft:snowball", 1L);
        ITEM_PRICES.put("minecraft:soul_sand", 49L);
        ITEM_PRICES.put("minecraft:spider_eye", 128L);
        ITEM_PRICES.put("minecraft:spire_armor_trim_smithing_template", 18588L);
        ITEM_PRICES.put("minecraft:sponge", 128L);
        ITEM_PRICES.put("minecraft:spore_blossom", 64L);
        ITEM_PRICES.put("minecraft:string", 12L);
        ITEM_PRICES.put("minecraft:sugar_cane", 32L);
        ITEM_PRICES.put("minecraft:sweet_berries", 16L);
        ITEM_PRICES.put("minecraft:tall_grass", 1L);
        ITEM_PRICES.put("minecraft:tide_armor_trim_smithing_template", 22116L);
        ITEM_PRICES.put("minecraft:trident", 16398L);
        ITEM_PRICES.put("minecraft:tropical_fish", 64L);
        ITEM_PRICES.put("minecraft:tube_coral", 16L);
        ITEM_PRICES.put("minecraft:tube_coral_block", 64L);
        ITEM_PRICES.put("minecraft:tube_coral_fan", 16L);
        ITEM_PRICES.put("minecraft:tuff", 4L);
        ITEM_PRICES.put("minecraft:turtle_egg", 192L);
        ITEM_PRICES.put("minecraft:twisting_vines", 8L);
        ITEM_PRICES.put("minecraft:vex_armor_trim_smithing_template", 51917L);
        ITEM_PRICES.put("minecraft:vine", 8L);
        ITEM_PRICES.put("minecraft:ward_armor_trim_smithing_template", 19677L);
        ITEM_PRICES.put("minecraft:warped_fungus", 32L);
        ITEM_PRICES.put("minecraft:warped_roots", 1L);
        ITEM_PRICES.put("minecraft:wayfinder_armor_trim_smithing_template", 10176L);
        ITEM_PRICES.put("minecraft:weeping_vines", 8L);
        ITEM_PRICES.put("minecraft:wild_armor_trim_smithing_template", 42641L);
        ITEM_PRICES.put("minecraft:zombie_head", 256L);

        // ==================== NBT 特定定价 ====================
        addNBTPrice("minecraft:goat_horn", "instrument", "minecraft:admire_goat_horn", 192L);
        addNBTPrice("minecraft:goat_horn", "instrument", "minecraft:call_goat_horn", 192L);
        addNBTPrice("minecraft:goat_horn", "instrument", "minecraft:dream_goat_horn", 192L);
        addNBTPrice("minecraft:goat_horn", "instrument", "minecraft:feel_goat_horn", 96L);
        addNBTPrice("minecraft:goat_horn", "instrument", "minecraft:ponder_goat_horn", 96L);
        addNBTPrice("minecraft:goat_horn", "instrument", "minecraft:seek_goat_horn", 96L);
        addNBTPrice("minecraft:goat_horn", "instrument", "minecraft:sing_goat_horn", 96L);
        addNBTPrice("minecraft:goat_horn", "instrument", "minecraft:yearn_goat_horn", 192L);

        LOGGER.info("[MyShopPanel] 默认价格表已加载: {} 物品 + {} 标签 + {} NBT条目",
                ITEM_PRICES.size(), TAG_PRICES.size(), countNBT());
    }

    private static void addNBTPrice(String itemId, String tagKey, String tagValue, long price) {
        NBT_PRICES.computeIfAbsent(itemId, k -> new LinkedHashMap<>())
                .put(tagKey + "=" + tagValue, price);
    }

    private static int countNBT() {
        int count = 0;
        for (var m : NBT_PRICES.values()) count += m.size();
        return count;
    }

    /**
     * 获取物品的默认价格。
     * 匹配优先级：NBT精确 → 注册名精确 → 标签（取最高价） → 0（无匹配）
     */
    public static long getDefaultPrice(ItemStack stack) {
        init();
        if (stack.isEmpty()) return 0;

        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (rl == null) return 0;

        // 1. NBT 精确匹配
        if (stack.hasTag()) {
            Map<String, Long> nbtMap = NBT_PRICES.get(rl.toString());
            if (nbtMap != null) {
                var tag = stack.getTag();
                for (var entry : nbtMap.entrySet()) {
                    String[] kv = entry.getKey().split("=", 2);
                    if (kv.length == 2 && tag.contains(kv[0])) {
                        String val = tag.get(kv[0]).getAsString();
                        if (kv[1].equals(val)) {
                            return entry.getValue();
                        }
                    }
                }
            }
        }

        // 2. 注册名精确匹配
        Long price = ITEM_PRICES.get(rl.toString());
        if (price != null) return price;

        // 3. 标签匹配 — 取所有匹配标签中的最高价
        long bestPrice = 0;
        for (var entry : TAG_PRICES.entrySet()) {
            int colon = entry.getKey().indexOf(':');
            if (colon <= 0) continue;
            String ns = entry.getKey().substring(0, colon);
            String path = entry.getKey().substring(colon + 1);
            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, new ResourceLocation(ns, path));
            if (stack.is(tagKey)) {
                if (entry.getValue() > bestPrice) {
                    bestPrice = entry.getValue();
                }
            }
        }

        return bestPrice;
    }
}
