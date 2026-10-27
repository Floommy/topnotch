package net.togyk.tcards;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class TableMain implements ModInitializer {
    private static final Map<UUID, Long> LAST_GESTURE = new HashMap<>();

    /** Logo item, only used as the creative tab icon. */
    public static net.minecraft.world.item.Item LOGO;

    @Override
    public void onInitialize() {
        net.minecraft.resources.ResourceKey<net.minecraft.world.item.Item> logoKey = net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("tog_yks-cards", "topnotch"));
        LOGO = net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.ITEM, logoKey,
                new net.minecraft.world.item.Item(new net.minecraft.world.item.Item.Properties().setId(logoKey)));
        org.slf4j.LoggerFactory.getLogger("tog_yks-cards").info("TopNotch extras loaded (blackjack.40)");
        net.togyk.tcards.food.McdFood.init();
        TableSounds.init();
        net.togyk.tcards.shuffler.ShufflerRegistry.init();
        net.togyk.tcards.atm.AtmRegistry.init();
        net.togyk.tcards.restaurant.RestaurantRegistry.init();
        net.togyk.tcards.food.McdFood.addToTab();
        TableTricks.init();
        ChipTower.init();
        PayloadTypeRegistry.serverboundPlay().register(TableActionPayload.TYPE, TableActionPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(TableActionPayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    if (context.player().isSpectator()) return;
                    if (payload.action() == TableActionPayload.COLLECT) {
                        TableActions.collect(context.player(), payload.pos(), payload.hit());
                    } else if (payload.action() == TableActionPayload.TABLE_KEY) {
                        TableActions.tableKey(context.player(), payload.pos(), payload.hit());
                    }
                }));

        // Hand gestures: client asks, server rate-limits and shows it to everyone around (and the player himself).
        PayloadTypeRegistry.serverboundPlay().register(GesturePayloads.Make.TYPE, GesturePayloads.Make.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(GesturePayloads.Show.TYPE, GesturePayloads.Show.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(GesturePayloads.Make.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    ServerPlayer player = context.player();
                    if (player.isSpectator() || Gesture.byId(payload.gesture()) == null) return;
                    long now = player.level().getGameTime();
                    Long last = LAST_GESTURE.get(player.getUUID());
                    if (last != null && now - last < 10) return;
                    LAST_GESTURE.put(player.getUUID(), now);
                    org.slf4j.LoggerFactory.getLogger("tog_yks-cards").info("Gesture {} by {}", Gesture.byId(payload.gesture()), player.getName().getString());
                    GesturePayloads.Show show = new GesturePayloads.Show(player.getId(), payload.gesture());
                    for (ServerPlayer viewer : PlayerLookup.tracking(player)) {
                        if (viewer != player && ServerPlayNetworking.canSend(viewer, GesturePayloads.Show.TYPE)) {
                            ServerPlayNetworking.send(viewer, show);
                        }
                    }
                }));

        // Server side safety net: never break a card pile by left click (collecting happens via the payload).
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            if (level.isClientSide() || player.isShiftKeyDown() || player.isSpectator()) return InteractionResult.PASS;
            HitResult hit = player.pick(player.blockInteractionRange(), 0.0f, false);
            if (hit instanceof BlockHitResult bhr && bhr.getBlockPos().equals(pos)
                    && TableActions.isCollectTarget(level, pos, bhr.getLocation())) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
    }
}
