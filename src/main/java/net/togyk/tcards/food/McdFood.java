package net.togyk.tcards.food;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.functions.SmeltItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

/**
 * The Better McDonald's Mod (by Kyronis and contributors, GPL-3.0), built into TopNotch without Balm.
 * Same ids (better_mcdonalds_mod:*), so existing worlds keep their items. Salt no longer generates in the world;
 * it is crafted (water bucket + sand -> salt block -> salt). No structure chest loot either.
 */
public final class McdFood {
    private McdFood() {}

    public static final String NS = "better_mcdonalds_mod";
    public static final List<Item> ITEMS = new ArrayList<>();

    public static Block SALT_BLOCK_BLOCK, TOMATO_CROP, LETTUCE_CROP;
    public static Item SALT_BLOCK, TOMATO, TOMATO_SEEDS, LETTUCE, LETTUCE_SEEDS, SALT, CHEESE, TORTILLA, BEEF_PATTY, COOKED_BEEF_PATTY,
            RAW_BACON, COOKED_BACON, MAYONNAISE, SWEET_SOUR_SAUCE, KETCHUP, MUSTARD, HAMBURGER, CHEESEBURGER, MCBACON, BIG_MAC,
            CHICKENBURGER, MCCHICKEN, FILET_O_FISH, MCWRAP, SNACK_SALAD, CHICKEN_MCNUGGETS, FRIES, HAPPY_MEAL, COCA_COLA, FANTA,
            SPRITE, LIPTON_ICE_TEA_PEACH, MCFLURRY, MCFLURRY_CHOCOLATE;

    public static void init() {
        SALT_BLOCK_BLOCK = block("salt_block", SaltBlock::new);
        TOMATO_CROP = block("tomato_crop", p -> new FoodCropBlock(p, 4, () -> TOMATO_SEEDS));
        LETTUCE_CROP = block("lettuce_crop", p -> new FoodCropBlock(p, 3, () -> LETTUCE_SEEDS));

        SALT_BLOCK = item("salt_block", p -> new BlockItem(SALT_BLOCK_BLOCK, p.useBlockDescriptionPrefix()));
        TOMATO = item("tomato", p -> new Item(p.food(food(1, 0.3f)).compostable(ContextIntProviders.COMPOSTABLE_MEDIUM)));
        TOMATO_SEEDS = item("tomato_seeds", p -> new BlockItem(TOMATO_CROP, p.compostable(ContextIntProviders.COMPOSTABLE_LOW).useItemDescriptionPrefix()));
        LETTUCE = item("lettuce", p -> new Item(p.food(food(1, 0.3f)).compostable(ContextIntProviders.COMPOSTABLE_MEDIUM)));
        LETTUCE_SEEDS = item("lettuce_seeds", p -> new BlockItem(LETTUCE_CROP, p.compostable(ContextIntProviders.COMPOSTABLE_LOW).useItemDescriptionPrefix()));
        SALT = item("salt", Item::new);
        CHEESE = food("cheese", 2, 0.4f);
        TORTILLA = food("tortilla", 2, 0.4f);
        BEEF_PATTY = food("beef_patty", 4, 0.4f);
        COOKED_BEEF_PATTY = food("cooked_beef_patty", 5, 0.4f);
        RAW_BACON = food("raw_bacon", 3, 0.4f);
        COOKED_BACON = food("cooked_bacon", 4, 0.4f);
        MAYONNAISE = food("mayonnaise", 1, 0.3f);
        SWEET_SOUR_SAUCE = food("sweet_sour_sauce", 1, 0.3f);
        KETCHUP = food("ketchup", 1, 0.3f);
        MUSTARD = food("mustard", 1, 0.3f);
        HAMBURGER = food("hamburger", 6, 0.5f);
        CHEESEBURGER = food("cheeseburger", 8, 0.5f);
        MCBACON = food("mcbacon", 8, 0.5f);
        BIG_MAC = food("big_mac", 10, 0.5f);
        CHICKENBURGER = food("chickenburger", 7, 0.5f);
        MCCHICKEN = food("mcchicken", 8, 0.5f);
        FILET_O_FISH = food("filet_o_fish", 6, 0.5f);
        MCWRAP = food("mcwrap", 5, 0.4f);
        SNACK_SALAD = food("snack_salad", 4, 0.4f);
        CHICKEN_MCNUGGETS = food("chicken_mcnuggets", 4, 0.4f);
        FRIES = food("fries", 5, 0.4f);
        HAPPY_MEAL = item("happy_meal", p -> new Item(p.rarity(Rarity.UNCOMMON).food(food(14, 0.5f))));
        COCA_COLA = drink("coca_cola");
        FANTA = drink("fanta");
        SPRITE = drink("sprite");
        LIPTON_ICE_TEA_PEACH = drink("lipton_ice_tea_peach");
        MCFLURRY = food("mcflurry", 6, 0.4f);
        MCFLURRY_CHOCOLATE = food("mcflurry_chocolate", 6, 0.4f);


        LootTableEvents.MODIFY_DROPS.register(McdFood::modifyDrops);
    }

    /** All food goes into the one TopNotch tab (after the casino and restaurant blocks). */
    public static void addToTab() {
        CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath("tog_yks-cards", "tog_yks-cards")))
                .register(output -> { for (Item i : ITEMS) output.accept(i); });
    }

    // ------------------------------------------------------------------ registration

    static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(NS, path);
    }

    private static Block block(String name, Function<BlockBehaviour.Properties, Block> factory) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id(name));
        return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(BlockBehaviour.Properties.of().setId(key)));
    }

    private static Item item(String name, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id(name));
        Item item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
        ITEMS.add(item);
        return item;
    }

    private static Item food(String name, int nutrition, float saturation) {
        return item(name, p -> new Item(p.food(food(nutrition, saturation))));
    }

    private static Item drink(String name) {
        return item(name, p -> new Item(p.food(food(3, 0.4f), Consumables.DEFAULT_DRINK)));
    }

    private static FoodProperties food(int nutrition, float saturation) {
        return new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build();
    }

    // ------------------------------------------------------------------ loot (seeds from grass, mob drops)

    private static void modifyDrops(Holder<LootTable> table, LootContext context, List<ItemStack> drops) {
        Optional<ResourceKey<LootTable>> key = table.unwrapKey();
        if (key.isEmpty()) return;
        ResourceKey<LootTable> id = key.get();
        RandomSource random = context.getRandom();

        // 15 % each: tomato / lettuce seeds from short grass and ferns
        BlockState state = context.getOptional(LootContextParams.BLOCK_STATE);
        if (state != null && (state.is(Blocks.SHORT_GRASS) || state.is(Blocks.FERN))) {
            if (random.nextFloat() < 0.15f) drops.add(new ItemStack(TOMATO_SEEDS));
            if (random.nextFloat() < 0.15f) drops.add(new ItemStack(LETTUCE_SEEDS));
        }

        // (no structure chest loot: TopNotch keeps food out of world generation)

        mob(context, drops, id, EntityTypes.COW.getDefaultLootTable(), BEEF_PATTY);
        mob(context, drops, id, EntityTypes.PIG.getDefaultLootTable(), RAW_BACON);
    }

    private static void mob(LootContext context, List<ItemStack> drops, ResourceKey<LootTable> current, Optional<ResourceKey<LootTable>> target, Item item) {
        if (target.isEmpty() || !current.equals(target.get()) || context.getRandom().nextFloat() >= 0.5f) return;
        ItemStack stack = new ItemStack(item, UniformInt.of(1, 2).sample(context.getRandom()));
        if (smelts(context)) stack = SmeltItemFunction.smelted().build().apply(stack, context);
        if (!stack.isEmpty()) drops.add(stack);
    }

    private static boolean smelts(LootContext context) {
        Entity entity = context.getOptional(LootContextParams.THIS_ENTITY);
        if (entity != null && entity.isOnFire()) return true;
        if (!(context.getOptional(LootContextParams.DIRECT_ATTACKING_ENTITY) instanceof LivingEntity attacker)) return false;
        ItemEnchantments enchantments = attacker.getMainHandItem().getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (var e : enchantments.keySet()) if (e.is(EnchantmentTags.SMELTS_LOOT)) return true;
        return false;
    }
}
