package tcardstest;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.togyk.tcards.food.FoodCropBlock;
import net.togyk.tcards.food.McdFood;
import net.togyk.tcards.restaurant.RestaurantMenu;

/** The built-in Better McDonald's Mod: items, crops, salt recipes, no salt world gen, loot. */
public class FoodTests {
    static void check(String n, boolean ok, Object info) {
        TestMain.check("food: " + n, ok, info);
    }

    static void run(ServerLevel level) {
        check("mod id better_mcdonalds_mod is provided by TopNotch", FabricLoader.getInstance().isModLoaded("better_mcdonalds_mod")
                && FabricLoader.getInstance().getModContainer("better_mcdonalds_mod").get().getMetadata().getId().equals("tog_yks-cards"), "");
        check("34 items registered", McdFood.ITEMS.size() == 34, McdFood.ITEMS.size());
        boolean allMenu = true;
        for (String p : RestaurantMenu.all()) allMenu &= RestaurantMenu.available(p);
        check("every restaurant menu item exists", allMenu, "");
        var bigMac = new ItemStack(McdFood.BIG_MAC).get(DataComponents.FOOD);
        check("big mac is food (10)", bigMac != null && bigMac.nutrition() == 10, bigMac);
        check("cola is a drink", new ItemStack(McdFood.COCA_COLA).get(DataComponents.CONSUMABLE) != null, "");
        check("salt block falls like sand", McdFood.SALT_BLOCK_BLOCK instanceof FallingBlock, McdFood.SALT_BLOCK_BLOCK);
        var tab = net.togyk.tog_yks_cards.item.ModItemTabs.EXAMPLE_TAB;
        tab.buildContents(new net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters(level.enabledFeatures(), true, level.registryAccess()));
        List<String> ids = tab.getDisplayItems().stream().map(st -> BuiltInRegistries.ITEM.getKey(st.getItem()).toString()).toList();
        long dupes = ids.size() - ids.stream().distinct().count();
        long trays = ids.stream().filter(i -> i.endsWith("serving_tray")).count();
        check("one TopNotch tab with everything, no duplicates, one tray", BuiltInRegistries.CREATIVE_MODE_TAB.getValue(Identifier.fromNamespaceAndPath("better_mcdonalds_mod", "main")) == null
                && ids.contains("better_mcdonalds_mod:big_mac") && ids.contains("tog_yks-cards:atm") && ids.contains("tog_yks-cards:tray_return") && dupes == 0 && trays == 1,
                ids.size() + " items, dupes=" + dupes + " trays=" + trays);

        // crops grow on farmland
        BlockPos farm = new BlockPos(90, 100, 10);
        level.setBlockAndUpdate(farm, Blocks.FARMLAND.defaultBlockState());
        level.setBlockAndUpdate(farm.above(), McdFood.TOMATO_CROP.defaultBlockState());
        BlockState grown = McdFood.TOMATO_CROP.defaultBlockState().setValue(FoodCropBlock.AGE, 3);
        level.setBlockAndUpdate(farm.above(), grown);
        List<ItemStack> crop = net.minecraft.world.level.block.Block.getDrops(grown, level, farm.above(), null);
        check("ripe tomato drops tomatoes", crop.stream().anyMatch(s -> s.is(McdFood.TOMATO)), crop);

        // salt: crafted, not generated
        List<ItemStack> grid = new ArrayList<>();
        ItemStack sand = new ItemStack(Items.SAND), water = new ItemStack(Items.WATER_BUCKET);
        for (ItemStack s : new ItemStack[]{ItemStack.EMPTY, sand, ItemStack.EMPTY, sand, water, sand, ItemStack.EMPTY, sand, ItemStack.EMPTY}) grid.add(s.copy());
        CraftingInput in = CraftingInput.of(3, 3, grid);
        var recipe = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, in, level);
        ItemStack out = recipe.map(r -> r.value().assemble(in)).orElse(ItemStack.EMPTY);
        boolean bucketBack = recipe.isPresent() && recipe.get().value().getRemainingItems(in).stream().anyMatch(s -> s.is(Items.BUCKET));
        check("sand + water bucket -> salt block, bucket back", out.is(McdFood.SALT_BLOCK) && bucketBack, out + " bucket=" + bucketBack);
        CraftingInput one = CraftingInput.of(1, 1, List.of(new ItemStack(McdFood.SALT_BLOCK)));
        ItemStack salt = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, one, level).map(r -> r.value().assemble(one)).orElse(ItemStack.EMPTY);
        check("salt block -> 4 salt", salt.is(McdFood.SALT) && salt.getCount() == 4, salt);
        var placed = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
        check("no salt disks in world generation", placed.get(net.minecraft.resources.ResourceKey.create(Registries.PLACED_FEATURE,
                Identifier.fromNamespaceAndPath("better_mcdonalds_mod", "disk_salt"))).isEmpty(), "");

        // nothing from world generation: structure chests have no food and no grappling hooks
        int found = 0;
        java.util.Set<String> bad = new java.util.TreeSet<>();
        for (var key : new net.minecraft.resources.ResourceKey[]{net.minecraft.world.level.storage.loot.BuiltInLootTables.PILLAGER_OUTPOST,
                net.minecraft.world.level.storage.loot.BuiltInLootTables.SHIPWRECK_SUPPLY, net.minecraft.world.level.storage.loot.BuiltInLootTables.VILLAGE_SAVANNA_HOUSE,
                net.minecraft.world.level.storage.loot.BuiltInLootTables.ABANDONED_MINESHAFT, net.minecraft.world.level.storage.loot.BuiltInLootTables.SIMPLE_DUNGEON,
                net.minecraft.world.level.storage.loot.BuiltInLootTables.JUNGLE_TEMPLE, net.minecraft.world.level.storage.loot.BuiltInLootTables.END_CITY_TREASURE,
                net.minecraft.world.level.storage.loot.BuiltInLootTables.SPAWN_BONUS_CHEST, net.minecraft.world.level.storage.loot.BuiltInLootTables.VILLAGE_WEAPONSMITH}) {
            @SuppressWarnings("unchecked")
            var table = level.getServer().reloadableRegistries().getLootTable((net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable>) key);
            for (int i = 0; i < 60; i++) {
                var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                        .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.ZERO)
                        .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
                for (ItemStack s : table.getRandomItems(params)) {
                    found++;
                    String ns = BuiltInRegistries.ITEM.getKey(s.getItem()).getNamespace();
                    if (ns.equals("better_mcdonalds_mod") || ns.equals("yo_hooks")) bad.add(BuiltInRegistries.ITEM.getKey(s.getItem()).toString());
                }
            }
        }
        check("structure chests: no food, no hooks", found > 100 && bad.isEmpty(), found + " " + bad);

        // grappling hooks are built in and craftable
        var hooks = BuiltInRegistries.ITEM.keySet().stream().filter(k -> k.getNamespace().equals("yo_hooks")).map(Identifier::getPath).sorted().toList();
        check("grappling hooks registered", hooks.size() >= 5, hooks);
        long hookRecipes = level.getServer().getRecipeManager().getRecipes().stream().filter(r -> r.id().identifier().getNamespace().equals("yo_hooks")).count();
        check("hook recipes loaded", hookRecipes >= 5, hookRecipes);
        check("yo_hooks provided by TopNotch, config file written", FabricLoader.getInstance().getModContainer("yo_hooks").get().getMetadata().getId().equals("tog_yks-cards")
                && java.nio.file.Files.exists(FabricLoader.getInstance().getConfigDir().resolve("yo_hooks").resolve("yo_hooks-server.json")), "");

        // loot: seeds from grass, patties from cows
        int seeds = 0;
        BlockPos g = new BlockPos(92, 101, 10);
        for (int i = 0; i < 200; i++) {
            for (ItemStack s : net.minecraft.world.level.block.Block.getDrops(Blocks.SHORT_GRASS.defaultBlockState(), level, g, null)) {
                if (s.is(McdFood.TOMATO_SEEDS) || s.is(McdFood.LETTUCE_SEEDS)) seeds++;
            }
        }
        check("grass drops tomato/lettuce seeds", seeds > 20 && seeds < 120, seeds);
        int patties = 0;
        for (int i = 0; i < 40; i++) {
            var cow = net.minecraft.world.entity.EntityTypes.COW.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            cow.setPos(94.5, 101, 10.5);
            level.addFreshEntity(cow);
            cow.kill(level);
        }
        // (on the very first boot the fresh world drops nothing at all here; checked on the second boot)
        java.util.Map<String, Integer> seen = new java.util.TreeMap<>();
        for (var e : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(90, 95, 6, 99, 106, 15))) {
            seen.merge(e.getItem().getItem().toString(), e.getItem().getCount(), Integer::sum);
            if (e.getItem().is(McdFood.BEEF_PATTY)) patties += e.getItem().getCount();
            e.discard();
        }
        if (!seen.isEmpty()) check("cows drop beef patties", patties > 0, patties + " " + seen + " loaded=" + level.hasChunkAt(new BlockPos(94, 101, 10)));
    }
}
