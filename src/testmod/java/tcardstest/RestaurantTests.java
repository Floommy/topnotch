package tcardstest;

import net.togyk.tcards.restaurant.TrayReturnBlock;

import java.util.UUID;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.togyk.tcards.restaurant.KioskBlock;
import net.togyk.tcards.restaurant.Receipt;
import net.togyk.tcards.restaurant.RestaurantBlockEntity;
import net.togyk.tcards.restaurant.RestaurantMenu;
import net.togyk.tcards.restaurant.RestaurantPayloads;
import net.togyk.tcards.restaurant.RestaurantRegistry;
import net.togyk.tcards.restaurant.Restaurants;
import net.togyk.tcards.restaurant.ScreenFx;
import net.togyk.tcards.restaurant.TrayBlockEntity;

/** Restaurant: register, kiosk + receipt, kitchen / pickup screens, order flow, cash box, tray, persistence. */
public class RestaurantTests {
    static final int Y = -60;
    static final BlockPos REG = new BlockPos(70, Y, 10), KIOSK = new BlockPos(73, Y, 10), KITCHEN = new BlockPos(70, Y, 14),
            PICKUP = new BlockPos(72, Y, 14), PREP = new BlockPos(74, Y, 14), CHEST = new BlockPos(66, Y, 10), TRAY = new BlockPos(75, Y, 12), FAR = new BlockPos(70, Y, 40),
            KEEP_TRAY = new BlockPos(77, Y, 16);

    /** The test server has no McDonald's mod (it needs Balm): register stand-ins with the same ids. */
    static void fakeMenuItems() {
        if (RestaurantMenu.available("big_mac")) return;
        for (String path : RestaurantMenu.all()) {
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(RestaurantMenu.NS, path));
            Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties().setId(key)));
        }
    }

    static void check(String n, boolean ok, Object info) {
        TestMain.check("restaurant: " + n, ok, info);
    }

    static int count(FakePlayer p, Item item) {
        int n = 0;
        for (ItemStack s : p.getInventory().getNonEquipmentItems()) if (s.is(item)) n += s.getCount();
        return n;
    }

    static RestaurantBlockEntity be(ServerLevel level, BlockPos pos) {
        return (RestaurantBlockEntity) level.getBlockEntity(pos);
    }

    static void tick(ServerLevel level, RestaurantBlockEntity be, int n) {
        for (int i = 0; i < n; i++) RestaurantBlockEntity.serverTick(level, be.getBlockPos(), be.getBlockState(), be);
    }

    static long fx(ServerLevel level, BlockPos around, Class<?> type) {
        return level.getEntities((Entity) null, new AABB(around).inflate(0.6),
                e -> e.entityTags().contains(ScreenFx.TAG) && type.isInstance(e)).size();
    }

    static void action(FakePlayer p, BlockPos pos, int action, String text, int a) {
        RestaurantRegistry.handle(p, new RestaurantPayloads.Action(pos, action, text, a));
    }

    static void run(ServerLevel level) {
        level.setChunkForced(4, 0, true);
        level.setChunkForced(4, 1, true);
        level.setChunkForced(4, 2, true);
        for (int x = 60; x < 82; x++) for (int z = 4; z < 44; z++) {
            level.setBlock(new BlockPos(x, Y - 1, z), Blocks.STONE.defaultBlockState(), 3);
            for (int y = Y; y < Y + 3; y++) level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
        }
        FakePlayer owner = FakePlayer.get(level, new GameProfile(UUID.fromString("00000000-0000-0000-0000-0000000aaa01"), "Chef"));
        FakePlayer guest = FakePlayer.get(level, new GameProfile(UUID.fromString("00000000-0000-0000-0000-0000000bbb02"), "Guest"));
        owner.setPos(71.5, Y, 12.5);
        guest.setPos(73.5, Y, 12.5);
        owner.getInventory().clearContent();
        guest.getInventory().clearContent();
        Restaurants data = Restaurants.get(level.getServer());

        // --- register opens a restaurant, the rest links itself to it
        level.setBlock(REG, RestaurantRegistry.REGISTER.defaultBlockState(), 3);
        RestaurantBlockEntity reg = be(level, REG);
        RestaurantRegistry.setUpPlaced(owner, reg);
        Restaurants.Restaurant r = reg.restaurant();
        check("register opens a restaurant", r != null && r.name.equals("Chef's McDonald's") && r.canManage(owner) && !r.canManage(guest), r == null ? null : r.name);
        check("all menu items priced 1 by default", RestaurantMenu.all().stream().allMatch(i -> r.price(i) == 1) && RestaurantMenu.all().size() == 22,
                RestaurantMenu.all().size());

        var k = RestaurantRegistry.KIOSK.defaultBlockState();
        level.setBlock(KIOSK, k, 3);
        level.setBlock(KIOSK.above(), k.setValue(KioskBlock.HALF, DoubleBlockHalf.UPPER), 3);
        RestaurantBlockEntity kiosk = be(level, KIOSK);
        RestaurantRegistry.setUpPlaced(owner, kiosk);
        check("kiosk links to the register nearby", reg.restaurantId().equals(kiosk.restaurantId()), kiosk.restaurantId());
        check("upper kiosk half finds the kiosk", RestaurantRegistry.KIOSK.entity(level, KIOSK.above(), level.getBlockState(KIOSK.above())) == kiosk, "");
        level.setBlock(KITCHEN, RestaurantRegistry.KITCHEN_SCREEN.defaultBlockState(), 3);
        level.setBlock(PICKUP, RestaurantRegistry.PICKUP_SCREEN.defaultBlockState(), 3);
        level.setBlock(PREP, RestaurantRegistry.PREPARING_SCREEN.defaultBlockState(), 3);
        RestaurantBlockEntity kitchen = be(level, KITCHEN), pickup = be(level, PICKUP), prep = be(level, PREP);
        RestaurantRegistry.setUpPlaced(owner, prep);
        RestaurantRegistry.setUpPlaced(owner, kitchen);
        RestaurantRegistry.setUpPlaced(owner, pickup);
        check("screens link too", reg.restaurantId().equals(kitchen.restaurantId()) && reg.restaurantId().equals(pickup.restaurantId())
                && reg.restaurantId().equals(prep.restaurantId()), "");
        level.setBlock(FAR, RestaurantRegistry.PICKUP_SCREEN.defaultBlockState(), 3);
        RestaurantBlockEntity far = be(level, FAR);
        RestaurantRegistry.setUpPlaced(owner, far);
        check("screen far away stays unlinked", far.restaurantId() == null, far.restaurantId());
        RestaurantRegistry.startJoin(owner, far);
        UseBlockCallback.EVENT.invoker().interact(owner, level, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(REG), Direction.UP, REG, false));
        check("sneak-link: click a register", reg.restaurantId().equals(far.restaurantId()) && !RestaurantRegistry.isLinking(owner), far.restaurantId());
        level.setBlock(FAR, Blocks.AIR.defaultBlockState(), 3);

        // --- ordering at the kiosk
        guest.getInventory().add(new ItemStack(Items.DIAMOND, 5));
        action(guest, KIOSK, RestaurantPayloads.ORDER, "big_mac:2,fries:1,not_food:3", 0);
        Restaurants.Order o = r.order(Restaurants.FIRST_NUMBER);
        check("first order is #103, paid 3 diamonds", o != null && o.lines.size() == 2 && count(guest, Items.DIAMOND) == 2 && r.vault == 3 && r.sales == 3,
                (o == null ? "none" : o.lines) + " dia=" + count(guest, Items.DIAMOND) + " vault=" + r.vault);
        check("kiosk prints the receipt", kiosk.isPrinting(), kiosk.isPrinting());
        check("paper strip comes out of the printer", fx(level, KIOSK, Display.ItemDisplay.class) == 1, fx(level, KIOSK, Display.ItemDisplay.class));
        for (int i = 0; i < 7; i++) kiosk.stretch();
        check("spamming right click makes it longer", kiosk.printLength() == 7, kiosk.printLength());
        tick(level, kiosk, RestaurantBlockEntity.PRINT_TICKS * 3 + 5);
        ItemStack receipt = guest.getInventory().getNonEquipmentItems().stream().filter(s -> s.is(Receipt.ITEM)).findFirst().orElse(ItemStack.EMPTY);
        Receipt rc = receipt.get(Receipt.TYPE);
        check("receipt in the inventory with number and length", rc != null && rc.number() == 103 && rc.length() == 7 && rc.total() == 3
                && receipt.get(DataComponents.CUSTOM_MODEL_DATA).getFloat(0) == 7f && rc.tooltip().size() > 6, rc);
        check("strip gone after printing", !kiosk.isPrinting() && fx(level, KIOSK, Display.ItemDisplay.class) == 0, fx(level, KIOSK, Display.ItemDisplay.class));

        action(guest, KIOSK, RestaurantPayloads.ORDER, "big_mac:5", 0);
        check("not enough diamonds -> no order", r.orders.size() == 1 && count(guest, Items.DIAMOND) == 2, r.orders.size());
        action(guest, KIOSK, RestaurantPayloads.ORDER, "", 0);
        check("empty order refused", r.orders.size() == 1, r.orders.size());
        r.prices.put("sprite", 0);
        action(guest, KIOSK, RestaurantPayloads.ORDER, "sprite:1", 0);
        check("price 0 = not sold", r.orders.size() == 1 && count(guest, Items.DIAMOND) == 2, r.orders.size());
        r.prices.put("sprite", 1);
        action(guest, REG, RestaurantPayloads.ORDER, "coca_cola:1,ketchup:1", 0);
        Restaurants.Order o2 = r.order(104);
        check("register takes orders too (receipt straight to inventory)", o2 != null && count(guest, Receipt.ITEM) == 2, o2 == null ? null : o2.lines);

        // --- screens
        tick(level, kitchen, 12);
        tick(level, pickup, 12);
        tick(level, prep, 12);
        String kt = kitchen.screenText(0);
        check("kitchen screen lists the orders", kt != null && kt.contains("#103") && kt.contains("2x " + RestaurantMenu.shortName("big_mac")) && kt.contains("#104"), kt);
        check("kitchen screen is a text display", fx(level, KITCHEN, Display.TextDisplay.class) == 1, fx(level, KITCHEN, Display.TextDisplay.class));
        check("preparing screen shows both", prep.screenText(0).contains("103") && prep.screenText(0).contains("104")
                && prep.screenText(0).contains("Preparing"), prep.screenText(0));
        check("ready screen empty so far", pickup.screenText(0).contains("Ready") && !pickup.screenText(0).contains("103"), pickup.screenText(0));
        check("one text display per status screen", fx(level, PREP, Display.TextDisplay.class) == 1 && fx(level, PICKUP, Display.TextDisplay.class) == 1,
                fx(level, PREP, Display.TextDisplay.class) + " " + fx(level, PICKUP, Display.TextDisplay.class));

        action(guest, REG, RestaurantPayloads.DONE, "", 103);
        check("register: Done -> ready", r.order(103).status == Restaurants.READY, r.order(103).status);
        tick(level, kitchen, 12);
        tick(level, pickup, 12);
        tick(level, prep, 12);
        check("ready screen shows 103, preparing screen not", pickup.screenText(0).contains("103") && !prep.screenText(0).contains("103")
                && prep.screenText(0).contains("104"), pickup.screenText(0));
        check("kitchen no longer shows 103", !kitchen.screenText(0).contains("#103") && kitchen.screenText(0).contains("#104"), kitchen.screenText(0));
        action(guest, REG, RestaurantPayloads.PICKED, "", 103);
        check("Picked up removes it", r.order(103) == null, r.orders.size());
        action(guest, REG, RestaurantPayloads.CANCEL, "", 104);
        check("Cancel removes it", r.orders.isEmpty(), r.orders.size());

        // numbers: 103..400 and around again, skipping open orders
        r.next = 400;
        guest.getInventory().add(new ItemStack(Items.DIAMOND, 10));
        action(guest, REG, RestaurantPayloads.ORDER, "fries:1", 0);
        action(guest, REG, RestaurantPayloads.ORDER, "fries:1", 0);
        check("after 400 comes 103", r.order(400) != null && r.order(103) != null, r.orders.stream().map(x -> x.number).toList());
        long now = level.getServer().overworld().getGameTime();
        data.setStatus(reg.restaurantId(), 400, Restaurants.READY, now);
        data.expire(now + Restaurants.READY_TIMEOUT + 1);
        check("ready orders vanish after 10 minutes", r.order(400) == null && r.order(103) != null, r.orders.size());
        r.orders.clear();

        // --- settings: only the owner
        action(guest, REG, RestaurantPayloads.SET_NAME, "Hacked", 0);
        action(guest, REG, RestaurantPayloads.SET_PRICE, "big_mac", 50);
        check("guest cannot change name or prices", r.name.equals("Chef's McDonald's") && r.price("big_mac") == 1, r.name);
        action(owner, REG, RestaurantPayloads.SET_NAME, "Golden Arches", 0);
        action(owner, REG, RestaurantPayloads.SET_PRICE, "big_mac", 4);
        check("owner renames and sets prices", r.name.equals("Golden Arches") && r.price("big_mac") == 4, r.name + " " + r.price("big_mac"));
        action(owner, REG, RestaurantPayloads.SET_CURRENCY, "", 1);
        check("currency change blocked while the cash box has money", r.currency.equals("diamond"), r.currency);
        long vault = r.vault;
        action(owner, REG, RestaurantPayloads.EMPTY_VAULT, "", 0);
        check("owner empties the cash box", r.vault == 0 && count(owner, Items.DIAMOND) == vault, count(owner, Items.DIAMOND));
        action(owner, REG, RestaurantPayloads.SET_CURRENCY, "", 1);
        guest.getInventory().clearContent();
        guest.getInventory().add(new ItemStack(Items.EMERALD, 3));
        action(guest, KIOSK, RestaurantPayloads.ORDER, "hamburger:3", 0);
        check("emerald restaurant takes emeralds", r.currency.equals("emerald") && count(guest, Items.EMERALD) == 0 && r.vault == 3, r.vault);
        tick(level, kiosk, RestaurantBlockEntity.PRINT_TICKS + 2);

        // --- chest for the money
        level.setBlock(CHEST, Blocks.CHEST.defaultBlockState(), 3);
        RestaurantRegistry.startChestLink(owner, reg);
        UseBlockCallback.EVENT.invoker().interact(owner, level, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(CHEST), Direction.UP, CHEST, false));
        Container chest = (Container) level.getBlockEntity(CHEST);
        int inChest = 0;
        for (int i = 0; i < chest.getContainerSize(); i++) if (chest.getItem(i).is(Items.EMERALD)) inChest += chest.getItem(i).getCount();
        check("linked chest gets the money", r.link.isPresent() && r.vault == 0 && inChest == 3, "vault=" + r.vault + " chest=" + inChest);

        // --- protection
        boolean guestBreak = PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, guest, REG, level.getBlockState(REG), reg);
        boolean guestBreakKiosk = PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, guest, KIOSK.above(), level.getBlockState(KIOSK.above()), null);
        check("guests cannot break the restaurant", !guestBreak && !guestBreakKiosk, guestBreak + " " + guestBreakKiosk);

        // --- screen entities are cleaned up
        level.setBlock(KITCHEN, Blocks.AIR.defaultBlockState(), 3);
        check("removing a screen removes its text", fx(level, KITCHEN, Display.TextDisplay.class) == 0, fx(level, KITCHEN, Display.TextDisplay.class));

        check("creative tab shows the TopNotch logo", net.togyk.tog_yks_cards.item.ModItemTabs.EXAMPLE_TAB.getIconItem().is(net.togyk.tcards.TableMain.LOGO), net.togyk.tog_yks_cards.item.ModItemTabs.EXAMPLE_TAB.getIconItem());
        // --- tray
        level.setBlock(TRAY, RestaurantRegistry.TRAY.defaultBlockState(), 3);
        TrayBlockEntity tray = (TrayBlockEntity) level.getBlockEntity(TRAY);
        boolean a1 = tray.add(RestaurantMenu.stack("big_mac"));
        boolean a2 = tray.add(new ItemStack(Items.APPLE));
        tray.add(RestaurantMenu.stack("fries"));
        tray.add(RestaurantMenu.stack("coca_cola"));
        TrayBlockEntity.serverTick(level, TRAY, level.getBlockState(TRAY), tray);
        long shown = fx(level, TRAY, Display.ItemDisplay.class);
        ItemStack firstShown = level.getEntitiesOfClass(Display.ItemDisplay.class, new AABB(TRAY).inflate(1)).stream()
                .map(d -> d.getSlot(0).get()).filter(s -> !s.isEmpty()).findFirst().orElse(ItemStack.EMPTY);
        check("food goes on the tray, other things not", a1 && !a2 && tray.count() == 3, tray.count());
        check("each item shown as its 3D model", shown == 3, shown);
        for (int i = 0; i < 4; i++) tray.add(RestaurantMenu.stack("ketchup"));
        check("tray holds 6", tray.count() == 6, tray.count());
        ItemStack last = tray.takeLast();
        check("take the last one back", RestaurantMenu.pathOf(last).equals("ketchup") && tray.count() == 5, last);
        // pick the tray up with food (sneak + use) and put it down again
        owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        owner.setShiftKeyDown(true);
        level.getBlockState(TRAY).useWithoutItem(level, owner, new BlockHitResult(Vec3.atCenterOf(TRAY), Direction.UP, TRAY, false));
        owner.setShiftKeyDown(false);
        ItemStack trayItem = owner.getInventory().getNonEquipmentItems().stream().filter(s -> s.is(RestaurantRegistry.TRAY.asItem())).findFirst().orElse(ItemStack.EMPTY);
        long inItem = trayItem.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItemCopyStream().count();
        check("picked up tray keeps its food", level.getBlockState(TRAY).isAir() && inItem == 5 && fx(level, TRAY, Display.ItemDisplay.class) == 0, inItem);
        var cmd = trayItem.get(DataComponents.CUSTOM_MODEL_DATA);
        check("held tray shows its food (model data)", cmd != null && cmd.strings().size() == 6 && "big_mac".equals(cmd.strings().get(0)) && "".equals(cmd.strings().get(5)), cmd);
        owner.setItemInHand(InteractionHand.MAIN_HAND, trayItem.copy());
        owner.getInventory().removeItem(trayItem);
        owner.setPos(KEEP_TRAY.getX() + 0.5, Y, KEEP_TRAY.getZ() + 2.5);
        trayItem.getItem().useOn(new UseOnContext(owner, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(KEEP_TRAY.below()).add(0, 0.5, 0), Direction.UP, KEEP_TRAY.below(), false)));
        TrayBlockEntity placed = level.getBlockEntity(KEEP_TRAY) instanceof TrayBlockEntity t ? t : null;
        check("placed again with the food on it", placed != null && placed.count() == 5, placed == null ? level.getBlockState(KEEP_TRAY) : placed.count());
        check("food keeps its normal item (3D model only on the tray)", firstShown.get(DataComponents.ITEM_MODEL) != null
                && firstShown.get(DataComponents.ITEM_MODEL).toString().startsWith("tog_yks-cards:tray/") && !last.get(DataComponents.ITEM_MODEL).toString().startsWith("tog_yks-cards:tray/"),
                firstShown.get(DataComponents.ITEM_MODEL));

        // --- German style: waste station empties the tray, tray rack (2 tall) takes clean trays from the top rail down
        BlockPos ret = new BlockPos(79, Y, 12), waste = new BlockPos(81, Y, 12);
        level.setBlockAndUpdate(ret.above(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(ret, RestaurantRegistry.TRAY_RETURN.defaultBlockState());
        RestaurantRegistry.TRAY_RETURN.setPlacedBy(level, ret, level.getBlockState(ret), owner, ItemStack.EMPTY);
        level.setBlockAndUpdate(waste, RestaurantRegistry.WASTE_STATION.defaultBlockState());
        BlockHitResult retHit = new BlockHitResult(Vec3.atCenterOf(ret.above()), Direction.NORTH, ret.above(), false);
        BlockHitResult wasteHit = new BlockHitResult(Vec3.atCenterOf(waste), Direction.NORTH, waste, false);
        check("tray rack is two blocks tall", level.getBlockState(ret.above()).is(RestaurantRegistry.TRAY_RETURN)
                && level.getBlockState(ret.above()).getValue(TrayReturnBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER, level.getBlockState(ret.above()));
        ItemStack dirty = new ItemStack(RestaurantRegistry.TRAY.asItem());
        dirty.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(java.util.List.of(RestaurantMenu.stack("fries"))));
        owner.setItemInHand(InteractionHand.MAIN_HAND, dirty);
        level.getBlockState(ret.above()).useItemOn(owner.getMainHandItem(), level, owner, InteractionHand.MAIN_HAND, retHit);
        check("rack refuses a tray with leftovers", level.getBlockState(ret).getValue(TrayReturnBlock.TRAYS) == 0 && owner.getMainHandItem().getCount() == 1, level.getBlockState(ret));
        level.getBlockState(waste).useItemOn(owner.getMainHandItem(), level, owner, InteractionHand.MAIN_HAND, wasteHit);
        boolean cleaned = owner.getMainHandItem().is(RestaurantRegistry.TRAY.asItem())
                && owner.getMainHandItem().getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItemCopyStream().findAny().isEmpty();
        check("waste station empties the tray", cleaned, owner.getMainHandItem());
        level.getBlockState(ret.above()).useItemOn(owner.getMainHandItem(), level, owner, InteractionHand.MAIN_HAND, retHit);
        ItemStack two = new ItemStack(RestaurantRegistry.TRAY.asItem(), 2);
        owner.setItemInHand(InteractionHand.MAIN_HAND, two);
        level.getBlockState(ret).useItemOn(owner.getMainHandItem(), level, owner, InteractionHand.MAIN_HAND, retHit);
        int stacked = level.getBlockState(ret).getValue(TrayReturnBlock.TRAYS);
        check("clean trays slide into the rack (both halves agree)", stacked == 2 && level.getBlockState(ret.above()).getValue(TrayReturnBlock.TRAYS) == 2
                && owner.getMainHandItem().getCount() == 1, stacked);
        ItemStack fries = RestaurantMenu.stack("fries");
        owner.setItemInHand(InteractionHand.MAIN_HAND, fries);
        level.getBlockState(waste).useItemOn(owner.getMainHandItem(), level, owner, InteractionHand.MAIN_HAND, wasteHit);
        check("leftover food goes in the bin", owner.getMainHandItem().isEmpty(), owner.getMainHandItem());
        owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        int before = count(owner, RestaurantRegistry.TRAY.asItem());
        level.getBlockState(ret.above()).useWithoutItem(level, owner, retHit);
        check("empty hand takes a clean tray", level.getBlockState(ret).getValue(TrayReturnBlock.TRAYS) == 1 && count(owner, RestaurantRegistry.TRAY.asItem()) == before + 1,
                count(owner, RestaurantRegistry.TRAY.asItem()));
        java.util.List<ItemStack> drops = net.minecraft.world.level.block.Block.getDrops(level.getBlockState(ret), level, ret, null);
        java.util.List<ItemStack> upperDrops = net.minecraft.world.level.block.Block.getDrops(level.getBlockState(ret.above()), level, ret.above(), null);
        check("breaking drops the rack + its trays once", drops.stream().anyMatch(st -> st.is(RestaurantRegistry.TRAY_RETURN.asItem()))
                && drops.stream().anyMatch(st -> st.is(RestaurantRegistry.TRAY.asItem()) && st.getCount() == 1) && upperDrops.isEmpty(), drops + " / " + upperDrops);
        check("rack uses the item.* name key (Tray Rack)", RestaurantRegistry.TRAY_RETURN.asItem().getDescriptionId().equals("item.tog_yks-cards.tray_return"),
                RestaurantRegistry.TRAY_RETURN.asItem().getDescriptionId());
    }

    static void checkPersisted(ServerLevel level) {
        Restaurants data = Restaurants.get(level.getServer());
        boolean found = data.all().values().stream().anyMatch(r -> r.name.equals("Golden Arches") && r.currency.equals("emerald") && r.price("big_mac") == 4);
        check("persist: restaurant name, currency, prices", found, data.all().size());
        level.getChunk(KEEP_TRAY);
        check("persist: tray with food", level.getBlockEntity(KEEP_TRAY) instanceof TrayBlockEntity t && t.count() == 5, level.getBlockState(KEEP_TRAY));
    }
}
