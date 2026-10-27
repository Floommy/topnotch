package net.togyk.tcards.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.togyk.tcards.TableActionPayload;
import net.togyk.tcards.TableActions;

@Environment(EnvType.CLIENT)
public class TableClient implements ClientModInitializer {
    public static KeyMapping TABLE_KEY;
    public static KeyMapping GESTURE_KEY;

    /** Ticks during which further left clicks are swallowed after a collect (prevents breaking what is behind). */
    private static int collectCooldown = 0;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("tog_yks-cards", "cards"));
        TABLE_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.tog_yks-cards.table_action", InputConstants.Type.MOUSE, 2 /* middle mouse */, category));
        GESTURE_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.tog_yks-cards.gesture_wheel", InputConstants.Type.KEYBOARD, 10 /* G (scancode) */, category));
        GestureClient.init();
        SkinLayersCompat.init();
        ShufflerScreen.registerReceiver();
        AtmScreen.registerReceiver();
        CentralBankScreen.registerReceiver();
        RestaurantScreen.registerReceiver();
        net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
            net.togyk.tcards.restaurant.Receipt r = stack.get(net.togyk.tcards.restaurant.Receipt.TYPE);
            if (r != null) for (String l : r.tooltip()) lines.add(net.minecraft.network.chat.Component.literal(l));
        });
        net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
            net.togyk.tcards.atm.CasinoMark mark = stack.get(net.togyk.tcards.atm.CasinoMark.TYPE);
            if (mark != null) lines.add(net.minecraft.network.chat.Component.literal("§7Casino: §6" + mark.name()));
        });

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (collectCooldown > 0) collectCooldown--;
            while (GESTURE_KEY.consumeClick()) {
                if (mc.player != null && mc.gui.screen() == null) mc.gui.setScreen(new GestureWheelScreen());
            }
            while (TABLE_KEY.consumeClick()) {
                BlockHitResult hit = currentBlockHit(mc);
                if (hit == null) continue;
                if (TableActions.isTableKeyTarget(mc.level, hit.getBlockPos(), hit.getLocation(), mc.player.isShiftKeyDown())) {
                    if (ClientPlayNetworking.canSend(TableActionPayload.TYPE)) {
                        ClientPlayNetworking.send(new TableActionPayload(TableActionPayload.TABLE_KEY, hit.getBlockPos(), hit.getLocation()));
                    } else {
                        warnServerOutdated(mc);
                    }
                }
            }
        });

        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            if (!level.isClientSide() || player.isSpectator()) return InteractionResult.PASS;
            if (player.isShiftKeyDown()) return InteractionResult.PASS; // sneak + left click = normal breaking
            if (collectCooldown > 0) return InteractionResult.FAIL;
            Minecraft mc = Minecraft.getInstance();
            BlockHitResult hit = currentBlockHit(mc);
            if (hit == null || !hit.getBlockPos().equals(pos)) return InteractionResult.PASS;
            if (!TableActions.isCollectTarget(level, pos, hit.getLocation())) return InteractionResult.PASS;
            if (!ClientPlayNetworking.canSend(TableActionPayload.TYPE)) {
                warnServerOutdated(mc);
                return InteractionResult.PASS; // old server: behave like before
            }
            ClientPlayNetworking.send(new TableActionPayload(TableActionPayload.COLLECT, pos, hit.getLocation()));
            collectCooldown = 6;
            return InteractionResult.FAIL;
        });
    }

    private static long lastWarn = 0;

    /** The server runs a mod version without these features. */
    public static void warnServerOutdated(Minecraft mc) {
        long now = System.currentTimeMillis();
        if (mc.player == null || now - lastWarn < 5000) return;
        lastWarn = now;
        mc.player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(
                "The server does not have the blackjack version of the cards mod").withStyle(net.minecraft.ChatFormatting.RED));
    }

    private static BlockHitResult currentBlockHit(Minecraft mc) {
        if (mc.player == null || mc.level == null) return null;
        HitResult hit = mc.hitResult;
        if (hit instanceof BlockHitResult bhr && hit.getType() == HitResult.Type.BLOCK) return bhr;
        return null;
    }

    /** Used by the pick-block mixin: true if the table key shares the pick key and would act on the crosshair target. */
    public static boolean shouldSuppressPickBlock(Minecraft mc) {
        if (TABLE_KEY == null || !TABLE_KEY.same(mc.options.keyPickItem)) return false;
        BlockHitResult hit = currentBlockHit(mc);
        return hit != null && TableActions.isTableKeyTarget(mc.level, hit.getBlockPos(), hit.getLocation(), mc.player.isShiftKeyDown());
    }
}
