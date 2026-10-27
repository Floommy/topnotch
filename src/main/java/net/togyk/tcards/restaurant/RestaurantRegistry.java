package net.togyk.tcards.restaurant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.MapColor;
import net.togyk.tcards.TableSounds;
import net.togyk.tcards.restaurant.RestaurantBlockEntity.Kind;
import net.togyk.tcards.restaurant.Restaurants.Line;
import net.togyk.tcards.restaurant.Restaurants.Order;
import net.togyk.tcards.restaurant.Restaurants.Restaurant;
import net.togyk.tog_yks_cards.block.ModBlocks;

/** Restaurant blocks, ordering, linking and the cash box. */
public final class RestaurantRegistry {
    private RestaurantRegistry() {}

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("tog_yks-cards");

    public static KioskBlock KIOSK;
    public static RestaurantBlock REGISTER, KITCHEN_SCREEN, PICKUP_SCREEN, PREPARING_SCREEN;
    public static TrayBlock TRAY;
    public static TrayReturnBlock TRAY_RETURN;
    public static WasteStationBlock WASTE_STATION;
    public static BlockEntityType<RestaurantBlockEntity> RESTAURANT_BE;
    public static BlockEntityType<TrayBlockEntity> TRAY_BE;

    public static final int LINK_RANGE = 16, CHEST_RANGE = 64, MAX_ITEMS = 64;

    private record Pending(ServerLevel level, BlockPos block, long until, boolean chest) {}

    private static final Map<UUID, Pending> LINKING = new HashMap<>();

    public static void init() {
        Receipt.register();
        ScreenFx.init();
        KIOSK = ModBlocks.registerBlockDefaultItem("order_kiosk", KioskBlock::new,
                BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 6.0f).sound(SoundType.METAL).noOcclusion());
        REGISTER = ModBlocks.registerBlockDefaultItem("cash_register", p -> new RestaurantBlock(p, Kind.REGISTER),
                BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f, 6.0f).sound(SoundType.METAL).noOcclusion());
        KITCHEN_SCREEN = ModBlocks.registerBlockDefaultItem("kitchen_screen", p -> new RestaurantBlock(p, Kind.KITCHEN),
                BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(1.0f).sound(SoundType.GLASS).noOcclusion().lightLevel(s -> 6));
        PICKUP_SCREEN = ModBlocks.registerBlockDefaultItem("pickup_screen", p -> new RestaurantBlock(p, Kind.PICKUP),
                BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(1.0f).sound(SoundType.GLASS).noOcclusion().lightLevel(s -> 6));
        PREPARING_SCREEN = ModBlocks.registerBlockDefaultItem("preparing_screen", p -> new RestaurantBlock(p, Kind.PREPARING),
                BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(1.0f).sound(SoundType.GLASS).noOcclusion().lightLevel(s -> 6));
        TRAY = ModBlocks.registerBlockDefaultItem("serving_tray", TrayBlock::new,
                BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.3f).sound(SoundType.WOOD).noOcclusion());
        TRAY_RETURN = ModBlocks.registerBlockDefaultItem("tray_return", TrayReturnBlock::new,
                BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.5f).sound(SoundType.METAL).noOcclusion());
        WASTE_STATION = ModBlocks.registerBlockDefaultItem("waste_station", WasteStationBlock::new,
                BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5f).sound(SoundType.WOOD));
        RESTAURANT_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Identifier.fromNamespaceAndPath("tog_yks-cards", "restaurant"),
                FabricBlockEntityTypeBuilder.create(RestaurantBlockEntity::new, KIOSK, REGISTER, KITCHEN_SCREEN, PREPARING_SCREEN, PICKUP_SCREEN).build());
        TRAY_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Identifier.fromNamespaceAndPath("tog_yks-cards", "serving_tray"),
                FabricBlockEntityTypeBuilder.create(TrayBlockEntity::new, TRAY).build());

        PayloadTypeRegistry.serverboundPlay().register(RestaurantPayloads.Action.TYPE, RestaurantPayloads.Action.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(RestaurantPayloads.State.TYPE, RestaurantPayloads.State.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(RestaurantPayloads.Action.TYPE, (payload, context) ->
                context.server().execute(() -> handle(context.player(), payload)));

        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, be) -> {
            if (state.getBlock() instanceof RestaurantBlock rb) {
                RestaurantBlockEntity r = rb.entity(level, pos, state);
                if (r != null && !r.canManage(player)) {
                    player.sendOverlayMessage(Component.literal("§cThis belongs to the restaurant owner"));
                    return false;
                }
            }
            return true;
        });

        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (level.isClientSide() || !(player instanceof ServerPlayer sp) || hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            Pending pending = LINKING.get(sp.getUUID());
            if (pending == null) return InteractionResult.PASS;
            BlockState clicked = level.getBlockState(hit.getBlockPos());
            RestaurantBlockEntity source = pending.level().getBlockEntity(pending.block()) instanceof RestaurantBlockEntity b ? b : null;
            if (source == null) {
                LINKING.remove(sp.getUUID());
                return InteractionResult.PASS;
            }
            if (clicked.getBlock() instanceof RestaurantBlock rb && rb.entity(level, hit.getBlockPos(), clicked) == source) {
                return InteractionResult.PASS;   // clicking itself cancels (see RestaurantBlock)
            }
            String error = pending.chest() ? linkChest(sp, source, (ServerLevel) level, hit.getBlockPos(), hit.getDirection())
                    : join(sp, source, level, hit.getBlockPos(), clicked);
            if (error != null) {
                sp.sendOverlayMessage(Component.literal("§c" + error));
                return InteractionResult.FAIL;
            }
            LINKING.remove(sp.getUUID());
            level.playSound(null, hit.getBlockPos(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.6f, 1.3f);
            return InteractionResult.SUCCESS;
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long now = server.overworld().getGameTime();
            if (now % 20 == 0) {
                Restaurants.get(server).expire(now);
                List<UUID> expired = new ArrayList<>();
                for (Map.Entry<UUID, Pending> e : LINKING.entrySet()) if (now > e.getValue().until()) expired.add(e.getKey());
                for (UUID id : expired) {
                    LINKING.remove(id);
                    ServerPlayer p = server.getPlayerList().getPlayer(id);
                    if (p != null) p.sendOverlayMessage(Component.literal("§7Linking cancelled (time is up)"));
                }
            }
            if (now % 40 == 0) flushAll(server);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> LINKING.remove(handler.getPlayer().getUUID()));

        ResourceKey<CreativeModeTab> tab = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath("tog_yks-cards", "tog_yks-cards"));
        CreativeModeTabEvents.modifyOutputEvent(tab).register(output -> {
            output.accept(new ItemStack(KIOSK));
            output.accept(new ItemStack(REGISTER));
            output.accept(new ItemStack(KITCHEN_SCREEN));
            output.accept(new ItemStack(PREPARING_SCREEN));
            output.accept(new ItemStack(PICKUP_SCREEN));
            output.accept(new ItemStack(TRAY));
            output.accept(new ItemStack(TRAY_RETURN));
            output.accept(new ItemStack(WASTE_STATION));
        });
    }

    // ------------------------------------------------------------------ placing & linking

    /** New block: joins the restaurant of a register nearby (that the player runs), a new register opens one. */
    public static void setUpPlaced(ServerPlayer player, RestaurantBlockEntity be) {
        be.setOwner(player);
        RestaurantBlockEntity near = nearestRegister((ServerLevel) be.getLevel(), be.getBlockPos(), player, be);
        if (near != null) {
            be.setRestaurant(near.restaurantId());
            player.sendOverlayMessage(Component.literal("§aLinked to " + near.restaurant().name));
        } else if (be.kind() == Kind.REGISTER) {
            UUID id = Restaurants.get(player.level().getServer()).create(player);
            be.setRestaurant(id);
            player.sendOverlayMessage(Component.literal("§aNew restaurant: " + be.restaurant().name + " (rename it at the register)"));
        } else {
            player.sendOverlayMessage(Component.literal("§eNot linked yet: sneak + right-click it, then click a register"));
        }
    }

    private static RestaurantBlockEntity nearestRegister(ServerLevel level, BlockPos pos, ServerPlayer player, RestaurantBlockEntity self) {
        RestaurantBlockEntity best = null;
        double bestDist = Double.MAX_VALUE;
        int cx = pos.getX() >> 4, cz = pos.getZ() >> 4;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (!level.hasChunk(cx + dx, cz + dz)) continue;
                LevelChunk chunk = level.getChunk(cx + dx, cz + dz);
                for (BlockEntity e : chunk.getBlockEntities().values()) {
                    if (!(e instanceof RestaurantBlockEntity r) || r == self || r.kind() != Kind.REGISTER) continue;
                    Restaurant data = r.restaurant();
                    if (data == null || !data.canManage(player)) continue;
                    double d = e.getBlockPos().distSqr(pos);
                    if (d <= LINK_RANGE * LINK_RANGE && d < bestDist) {
                        best = r;
                        bestDist = d;
                    }
                }
            }
        }
        return best;
    }

    public static void startJoin(ServerPlayer player, RestaurantBlockEntity be) {
        LINKING.put(player.getUUID(), new Pending((ServerLevel) be.getLevel(), be.getBlockPos(), be.getLevel().getGameTime() + 600, false));
        player.sendOverlayMessage(Component.literal("§eRight-click the register of the restaurant this should belong to. Click it again to cancel."));
    }

    public static void startChestLink(ServerPlayer player, RestaurantBlockEntity be) {
        LINKING.put(player.getUUID(), new Pending((ServerLevel) be.getLevel(), be.getBlockPos(), be.getLevel().getGameTime() + 600, true));
        player.sendOverlayMessage(Component.literal("§eRight-click the chest for the money. Click the register to cancel."));
    }

    public static boolean cancelLinking(ServerPlayer player) {
        if (LINKING.remove(player.getUUID()) == null) return false;
        player.sendOverlayMessage(Component.literal("§7Linking cancelled"));
        return true;
    }

    public static boolean isLinking(ServerPlayer player) {
        return LINKING.containsKey(player.getUUID());
    }

    /** Puts a block into the restaurant of the clicked register. */
    public static String join(ServerPlayer player, RestaurantBlockEntity be, Level level, BlockPos pos, BlockState clicked) {
        if (!(clicked.getBlock() instanceof RestaurantBlock rb)) return "Click a cash register";
        RestaurantBlockEntity target = rb.entity(level, pos, clicked);
        if (target == null || target.kind() != Kind.REGISTER || target.restaurant() == null) return "Click a cash register";
        if (!target.restaurant().canManage(player)) return "Only the owner of " + target.restaurant().name + " can add things to it";
        be.setRestaurant(target.restaurantId());
        player.sendOverlayMessage(Component.literal("§aLinked to " + target.restaurant().name));
        return null;
    }

    public static String linkChest(ServerPlayer player, RestaurantBlockEntity be, ServerLevel level, BlockPos pos, Direction side) {
        Restaurant r = be.restaurant();
        if (r == null) return "This register has no restaurant";
        if (level != be.getLevel()) return "The chest must be in the same dimension";
        if (!pos.closerThan(be.getBlockPos(), CHEST_RANGE)) return "Too far away (max " + CHEST_RANGE + " blocks)";
        Storage<ItemVariant> s = ItemStorage.SIDED.find(level, pos, side);
        if (s == null || !s.supportsInsertion() || level.getBlockState(pos).getBlock() instanceof RestaurantBlock) return "Click a chest, barrel or similar";
        r.link = Optional.of(GlobalPos.of(level.dimension(), pos.immutable()));
        Restaurants.get(level.getServer()).changed();
        flush(level.getServer(), r);
        player.sendOverlayMessage(Component.literal("§aMoney goes to " + level.getBlockState(pos).getBlock().getName().getString()
                + " " + pos.getX() + " " + pos.getY() + " " + pos.getZ()));
        return null;
    }

    // ------------------------------------------------------------------ money

    private static Storage<ItemVariant> chest(MinecraftServer server, Restaurant r) {
        if (r.link.isEmpty()) return null;
        ServerLevel level = server.getLevel(r.link.get().dimension());
        BlockPos pos = r.link.get().pos();
        if (level == null || !level.isLoaded(pos)) return null;
        return ItemStorage.SIDED.find(level, pos, Direction.UP);
    }

    private static long toChest(MinecraftServer server, Restaurant r, Item currency, long n, boolean simulate) {
        if (n <= 0 || Transaction.isOpen()) return 0;
        Storage<ItemVariant> s = chest(server, r);
        if (s == null) return 0;
        try (Transaction tx = Transaction.openOuter()) {
            long done = s.insert(ItemVariant.of(currency), n, tx);
            if (!simulate) tx.commit();
            return done;
        }
    }

    public static void flush(MinecraftServer server, Restaurant r) {
        if (r.vault <= 0) return;
        long moved = toChest(server, r, r.currencyItem(), r.vault, false);
        if (moved > 0) {
            r.vault -= moved;
            Restaurants.get(server).changed();
        }
    }

    private static void flushAll(MinecraftServer server) {
        for (Restaurant r : Restaurants.get(server).all().values()) flush(server, r);
    }

    public static int linkStatus(MinecraftServer server, Restaurant r) {
        if (r.link.isEmpty()) return 0;
        Storage<ItemVariant> s = chest(server, r);
        if (s == null) return 2;
        try (Transaction tx = Transaction.openOuter()) {
            if (s.insert(ItemVariant.of(r.currencyItem()), 1, tx) < 1) return 3;
        }
        return 1;
    }

    public static int count(ServerPlayer player, Item item) {
        int n = 0;
        for (ItemStack s : player.getInventory().getNonEquipmentItems()) if (s.is(item)) n += s.getCount();
        return n;
    }

    private static void take(ServerPlayer player, Item item, long n) {
        List<ItemStack> slots = player.getInventory().getNonEquipmentItems();
        long left = n;
        for (int pass = 0; pass < 2 && left > 0; pass++) {
            for (int i = pass == 0 ? 9 : 0; i < (pass == 0 ? slots.size() : 9) && left > 0; i++) {
                ItemStack s = slots.get(i);
                if (!s.is(item)) continue;
                int t = (int) Math.min(left, s.getCount());
                s.shrink(t);
                left -= t;
            }
        }
        player.getInventory().setChanged();
    }

    // ------------------------------------------------------------------ ordering

    public record OrderResult(Order order, String error) {}

    /** Parses "item:qty,item:qty" into merged lines (menu items only). */
    public static List<Line> parseCart(String cart) {
        Map<String, Integer> merged = new LinkedHashMap<>();
        for (String part : cart.split(",")) {
            String[] kv = part.split(":");
            if (kv.length != 2) continue;
            String item = kv[0].trim();
            int qty;
            try {
                qty = Integer.parseInt(kv[1].trim());
            } catch (NumberFormatException e) {
                continue;
            }
            if (qty <= 0 || !RestaurantMenu.onMenu(item)) continue;
            merged.merge(item, qty, Integer::sum);
        }
        List<Line> lines = new ArrayList<>();
        for (String item : RestaurantMenu.all()) if (merged.containsKey(item)) lines.add(new Line(item, merged.get(item)));
        return lines;
    }

    public static OrderResult order(ServerPlayer player, RestaurantBlockEntity be, String cart) {
        Restaurant r = be.restaurant();
        if (r == null) return new OrderResult(null, "Not linked to a restaurant");
        List<Line> lines = parseCart(cart);
        if (lines.isEmpty()) return new OrderResult(null, "Your order is empty");
        int items = 0;
        for (Line l : lines) {
            items += l.qty();
            if (!RestaurantMenu.available(l.item())) return new OrderResult(null, l.item() + " is not available");
            if (r.price(l.item()) <= 0) return new OrderResult(null, RestaurantMenu.shortName(l.item()) + " is not on sale");
        }
        if (items > MAX_ITEMS) return new OrderResult(null, "Max " + MAX_ITEMS + " items per order");
        long total = r.total(lines);
        Item currency = r.currencyItem();
        int held = count(player, currency);
        if (held < total) return new OrderResult(null, "Not enough " + r.currency + "s (need " + total + ", you have " + held + ")");
        MinecraftServer server = player.level().getServer();
        long free = Restaurants.VAULT_CAPACITY - r.vault + toChest(server, r, currency, total, true);
        if (free < total) return new OrderResult(null, "Sorry, the cash box is full. Please tell the staff.");

        take(player, currency, total);
        long inChest = toChest(server, r, currency, total, false);
        r.vault += total - inChest;
        r.sales += total;
        Restaurants data = Restaurants.get(server);
        Order o = data.placeOrder(be.restaurantId(), lines, player.getName().getString(), server.overworld().getGameTime());
        data.changed();

        List<String> receiptLines = new ArrayList<>();
        for (Line l : lines) receiptLines.add(l.qty() + "x " + RestaurantMenu.shortName(l.item()) + "  " + (long) r.price(l.item()) * l.qty());
        Receipt receipt = new Receipt(r.name, o.number, receiptLines, total, r.currency, 0);
        if (be.kind() == Kind.KIOSK) {
            be.startPrinting(player, receipt);
        } else if (!player.getInventory().add(receipt.toStack())) {
            player.drop(receipt.toStack(), false, net.minecraft.util.Prediction.SERVER_ONLY);
        }
        player.containerMenu.broadcastChanges();
        LOG.info("{}: order #{} by {} ({} {})", r.name, o.number, player.getName().getString(), total, r.currency);
        return new OrderResult(o, null);
    }

    // ------------------------------------------------------------------ menu actions

    public static void handle(ServerPlayer player, RestaurantPayloads.Action a) {
        if (player.isSpectator() || !player.isWithinBlockInteractionRange(a.pos(), 1.5)) return;
        BlockState state = player.level().getBlockState(a.pos());
        if (!(state.getBlock() instanceof RestaurantBlock rb)) return;
        RestaurantBlockEntity be = rb.entity(player.level(), a.pos(), state);
        if (be == null) return;
        Restaurant r = be.restaurant();
        Restaurants data = Restaurants.get(player.level().getServer());
        long now = player.level().getServer().overworld().getGameTime();
        boolean register = be.kind() == Kind.REGISTER;
        boolean manage = r != null && r.canManage(player);
        int result = RestaurantPayloads.RESULT_NONE, number = 0;
        String message = "";
        switch (a.action()) {
            case RestaurantPayloads.ORDER -> {
                OrderResult res = order(player, be, a.text());
                if (res.error() != null) {
                    result = RestaurantPayloads.RESULT_ERROR;
                    message = res.error();
                } else {
                    result = RestaurantPayloads.RESULT_ORDERED;
                    number = res.order().number;
                    player.level().playSound(null, a.pos(), TableSounds.ATM_CHIME, SoundSource.BLOCKS, 0.6f, 1.2f);
                }
            }
            case RestaurantPayloads.DONE -> {
                if (register && r != null && data.setStatus(be.restaurantId(), a.a(), Restaurants.READY, now)) {
                    result = RestaurantPayloads.RESULT_INFO;
                    message = "#" + a.a() + " is ready for pickup";
                    player.level().playSound(null, a.pos(), TableSounds.BELL_DING, SoundSource.BLOCKS, 0.7f, 1.1f);
                    Order o = r.order(a.a());
                    ServerPlayer customer = o == null ? null : player.level().getServer().getPlayerList().getPlayerByName(o.customer);
                    if (customer != null) {
                        customer.sendSystemMessage(Component.literal("§6" + r.name + ": §aYour order §l#" + a.a() + "§r§a is ready for pickup!"));
                        customer.level().playSound(null, customer.blockPosition(), TableSounds.BELL_DING, SoundSource.PLAYERS, 0.6f, 1.2f);
                    }
                }
            }
            case RestaurantPayloads.PICKED, RestaurantPayloads.CANCEL -> {
                if (register && r != null && data.remove(be.restaurantId(), a.a())) {
                    result = RestaurantPayloads.RESULT_INFO;
                    message = "#" + a.a() + (a.action() == RestaurantPayloads.PICKED ? " picked up" : " cancelled");
                }
            }
            case RestaurantPayloads.SET_NAME -> {
                if (manage && !a.text().isBlank()) {
                    r.name = a.text().strip().length() > 24 ? a.text().strip().substring(0, 24) : a.text().strip();
                    data.changed();
                    result = RestaurantPayloads.RESULT_INFO;
                    message = "Renamed to " + r.name;
                }
            }
            case RestaurantPayloads.SET_CURRENCY -> {
                if (manage) {
                    if (r.vault > 0) {
                        result = RestaurantPayloads.RESULT_ERROR;
                        message = "Empty the cash box before changing the currency";
                    } else {
                        r.currency = a.a() == 1 ? "emerald" : "diamond";
                        data.changed();
                    }
                }
            }
            case RestaurantPayloads.SET_PRICE -> {
                if (manage && RestaurantMenu.onMenu(a.text())) {
                    r.prices.put(a.text(), Math.max(0, Math.min(999, a.a())));
                    data.changed();
                }
            }
            case RestaurantPayloads.EMPTY_VAULT -> {
                if (manage) {
                    long taken = 0;
                    Item cur = r.currencyItem();
                    while (r.vault > 0) {
                        int n = (int) Math.min(64, r.vault);
                        ItemStack stack = new ItemStack(cur, n);
                        player.getInventory().add(stack);
                        int moved = n - stack.getCount();
                        r.vault -= moved;
                        taken += moved;
                        if (moved < n) break;
                    }
                    data.changed();
                    result = RestaurantPayloads.RESULT_INFO;
                    message = taken == 0 ? "The cash box is empty" : "Took " + taken + " " + r.currency + "s";
                }
            }
            case RestaurantPayloads.LINK_CHEST -> {
                if (manage) {
                    startChestLink(player, be);
                    return;
                }
            }
            case RestaurantPayloads.UNLINK_CHEST -> {
                if (manage) {
                    r.link = Optional.empty();
                    data.changed();
                    result = RestaurantPayloads.RESULT_INFO;
                    message = "Chest unlinked";
                }
            }
            default -> {}
        }
        sendState(player, be, false, result, number, message);
    }

    public static void sendState(ServerPlayer player, RestaurantBlockEntity be, boolean open, int result, int number, String message) {
        if (!ServerPlayNetworking.canSend(player, RestaurantPayloads.State.TYPE)) return;
        Restaurant r = be.restaurant();
        MinecraftServer server = player.level().getServer();
        long now = server.overworld().getGameTime();
        boolean register = be.kind() == Kind.REGISTER;
        boolean manage = r != null && r.canManage(player);
        List<String> items = new ArrayList<>();
        List<Integer> prices = new ArrayList<>();
        for (String item : RestaurantMenu.all()) {
            if (!RestaurantMenu.available(item)) continue;
            items.add(item);
            prices.add(r == null ? 0 : r.price(item));
        }
        List<RestaurantPayloads.OrderInfo> orders = new ArrayList<>();
        if (r != null && register) {
            for (Order o : r.orders) {
                StringBuilder sb = new StringBuilder();
                for (Line l : o.lines) {
                    if (!sb.isEmpty()) sb.append('|');
                    sb.append(l.qty()).append("x ").append(RestaurantMenu.shortName(l.item()));
                }
                orders.add(new RestaurantPayloads.OrderInfo(o.number, o.status, sb.toString(), (int) Math.max(0, (now - o.created) / 20), o.customer));
            }
        }
        String linkText = "";
        if (r != null && r.link.isPresent()) {
            BlockPos p = r.link.get().pos();
            linkText = p.getX() + " " + p.getY() + " " + p.getZ();
        }
        ServerPlayNetworking.send(player, new RestaurantPayloads.State(be.getBlockPos(), open,
                register ? RestaurantPayloads.KIND_REGISTER : RestaurantPayloads.KIND_KIOSK,
                r == null ? "" : r.name, r == null ? "diamond" : r.currency, manage,
                r == null ? 0 : count(player, r.currencyItem()), items, prices, orders,
                manage ? r.vault : 0, manage ? linkStatus(server, r) : 0, manage ? linkText : "", manage ? r.sales : 0,
                result, number, message));
    }
}
