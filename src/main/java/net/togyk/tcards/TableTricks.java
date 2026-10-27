package net.togyk.tcards;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.mojang.math.Transformation;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.togyk.tcards.mixin.DisplayInvoker;
import net.togyk.tcards.mixin.ItemDisplayInvoker;
import net.togyk.tcards.mixin.TextDisplayInvoker;
import net.togyk.tog_yks_cards.item.custom.CardItem;
import net.togyk.tog_yks_cards.item.custom.CoinItem;
import net.togyk.tog_yks_cards.block.custom.CardpileBlock;
import net.togyk.tog_yks_cards.block.custom.MatBlock;
import net.togyk.tog_yks_cards.blockentity.entities.MatBlockEntity;
import net.togyk.tog_yks_cards.item.custom.CardpileItem;
import net.togyk.tog_yks_cards.item.custom.MatPlaceable;
import net.togyk.tog_yks_cards.mat_item.CardPileMatItem;
import net.togyk.tog_yks_cards.mat_item.MatItem;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Sneak + right-click into the air:
 *  - with a coin / chip: flips it (heads or tails, everyone nearby sees the result)
 *  - with a card: flicks it up, spins it and catches it again.
 * The item really leaves your hand during the trick and comes back when it is caught.
 * The animation uses vanilla display entities, so every player sees it without anything extra on the client.
 */
public final class TableTricks {
    private TableTricks() {}

    public static final String FX_TAG = "tcards_fx";
    private static final int COIN_FLIGHT = 22, COIN_SHOW = 40;
    private static final int CARD_FLIGHT = 18;
    static final int COIN = 0, FLOURISH = 1;

    private static final List<Trick> RUNNING = new ArrayList<>();
    private static final Set<Entity> LIVE = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Map<UUID, Long> LAST_USE = new HashMap<>();

    public static void init() {
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (!player.isShiftKeyDown() || player.isSpectator()) return InteractionResult.PASS;
            ItemStack stack = player.getItemInHand(hand);
            if (!(stack.getItem() instanceof CoinItem) && !(stack.getItem() instanceof CardItem)) return InteractionResult.PASS;
            if (player instanceof ServerPlayer sp) start(sp, hand);
            return InteractionResult.SUCCESS;
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> tick());
        // leftovers of a crash / an unloaded chunk never stay in the world
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity.entityTags().contains(FX_TAG) && !LIVE.contains(entity)) entity.discard();
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> finishFor(handler.player));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            for (Trick t : new ArrayList<>(RUNNING)) finish(t);
            RUNNING.clear();
        });
    }

    /** Starts a coin flip / card flourish with the item in that hand. Returns false if nothing happened. */
    public static boolean start(ServerPlayer player, InteractionHand hand) {
        ServerLevel level = (ServerLevel) player.level();
        long now = level.getGameTime();
        Long last = LAST_USE.get(player.getUUID());
        if (last != null && now - last < 15) return false;
        for (Trick t : RUNNING) if (t.player == player) return false;
        ItemStack held = player.getItemInHand(hand);
        boolean coin = held.getItem() instanceof CoinItem;
        if (held.isEmpty() || !(coin || held.getItem() instanceof CardItem)) return false;
        LAST_USE.put(player.getUUID(), now);

        ItemStack one = held.copyWithCount(1);
        held.shrink(1);

        double yaw = Math.toRadians(player.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        boolean rightHand = (hand == InteractionHand.MAIN_HAND) == (player.getMainArm() == HumanoidArm.RIGHT);
        Vec3 origin = player.getEyePosition().add(forward.scale(0.5)).add(right.scale(rightHand ? 0.28 : -0.28)).add(0, -0.45, 0);

        Display.ItemDisplay display = spawnDisplay(level, origin, one);
        if (display == null) {
            giveBack(player, hand, one, origin);
            return false;
        }
        Trick t = new Trick(coin ? COIN : FLOURISH, player, hand, one, level.getRandom().nextBoolean(), level, display, origin,
                new Vector3f((float) right.x, 0, (float) right.z), (float) -yaw);
        t.pose(0);
        RUNNING.add(t);
        sound(level, origin, coin ? TableSounds.COIN_FLIP : TableSounds.CARD_FLICK, 0.8f);
        return true;
    }

    private static Display.ItemDisplay spawnDisplay(ServerLevel level, Vec3 at, ItemStack item) {
        Display.ItemDisplay display = EntityTypes.ITEM_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);
        if (display == null) return null;
        display.setPos(at);
        ((ItemDisplayInvoker) display).tcards$setItemStack(item.copy());
        ((ItemDisplayInvoker) display).tcards$setItemTransform(ItemDisplayContext.NONE);
        ((DisplayInvoker) display).tcards$setPosRotInterpolationDuration(0);
        display.addTag(FX_TAG);
        LIVE.add(display);
        level.addFreshEntity(display);
        return display;
    }

    public static int running() {
        return RUNNING.size();
    }

    private static void tick() {
        Iterator<Trick> it = RUNNING.iterator();
        List<Trick> done = new ArrayList<>();
        while (it.hasNext()) {
            Trick t = it.next();
            t.age++;
            if (t.display.isRemoved() || t.player.hasDisconnected()) {
                done.add(t);
                continue;
            }
            if (t.kind == COIN) {
                if (t.age <= COIN_FLIGHT) t.pose(t.age / (float) COIN_FLIGHT);
                if (t.age == COIN_FLIGHT) t.landCoin();
                if (t.age >= COIN_FLIGHT + COIN_SHOW) done.add(t);
            } else {
                if (t.age <= CARD_FLIGHT) t.pose(t.age / (float) CARD_FLIGHT);
                if (t.age == CARD_FLIGHT / 2) {
                    Vec3 top = t.origin.add(0, 1.0, 0);
                    t.level.sendParticles(ParticleTypes.END_ROD, top.x, top.y, top.z, 3, 0.08, 0.08, 0.08, 0.01);
                }
                if (t.age == CARD_FLIGHT) sound(t.level, t.origin, TableSounds.CARD_PLACE, 0.5f);
                if (t.age >= CARD_FLIGHT + 2) done.add(t);
            }
        }
        for (Trick t : done) finish(t);
    }

    private static void finishFor(ServerPlayer player) {
        for (Trick t : new ArrayList<>(RUNNING)) if (t.player == player) finish(t);
    }

    private static void finish(Trick t) {
        RUNNING.remove(t);
        LIVE.remove(t.display);
        t.display.discard();
        if (t.text != null) {
            LIVE.remove(t.text);
            t.text.discard();
        }
        if (!t.item.isEmpty()) giveBack(t.player, t.hand, t.item, t.origin);
    }

    private static void giveBack(ServerPlayer player, InteractionHand hand, ItemStack item, Vec3 at) {
        if (item.isEmpty()) return;
        if (!player.isAlive() || player.isRemoved() && !player.hasDisconnected()) {
            ServerLevel level = (ServerLevel) player.level();
            level.addFreshEntity(new ItemEntity(level, at.x, at.y, at.z, item));
            return;
        }
        ItemStack inHand = player.getItemInHand(hand);
        if (inHand.isEmpty()) {
            player.setItemInHand(hand, item);
        } else if (ItemStack.isSameItemSameComponents(inHand, item) && inHand.getCount() < inHand.getMaxStackSize()) {
            inHand.grow(item.getCount());
        } else {
            player.getInventory().placeItemBackInInventory(item, Prediction.SERVER_ONLY);
        }
    }

    private static void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, 0.95f + level.getRandom().nextFloat() * 0.1f);
    }

    private static void transform(Display display, Vector3f translation, Quaternionf rotation, float scale) {
        DisplayInvoker d = (DisplayInvoker) display;
        d.tcards$setTransformation(new Transformation(translation, rotation, new Vector3f(scale), new Quaternionf()));
        d.tcards$setInterpolationDelay(0);
        d.tcards$setInterpolationDuration(1);
    }

    private static final class Trick {
        final int kind;
        final ServerPlayer player;
        final InteractionHand hand;
        ItemStack item;
        final boolean coin, heads;
        final ServerLevel level;
        final Display.ItemDisplay display;
        final Vec3 origin;
        final Vector3f right;
        final float yawFacing;
        Display.TextDisplay text;
        int age;

        Trick(int kind, ServerPlayer player, InteractionHand hand, ItemStack item, boolean heads, ServerLevel level,
              Display.ItemDisplay display, Vec3 origin, Vector3f right, float facing) {
            this.kind = kind;
            this.player = player;
            this.hand = hand;
            this.item = item;
            this.coin = kind == COIN;
            this.heads = heads;
            this.level = level;
            this.display = display;
            this.origin = origin;
            this.right = right;
            this.yawFacing = facing;
        }

        /** s = 0..1 through the flight. */
        void pose(float s) {
            if (coin) {
                // 4 full flips, half a turn more for tails: it lands upside down
                float turns = heads ? 4.0f : 4.5f;
                float height = 4 * 1.15f * s * (1 - s);
                Quaternionf rot = new Quaternionf().rotationAxis((float) (turns * 2 * Math.PI * s), right.x, right.y, right.z);
                transform(display, new Vector3f(0, height, 0), rot, 1.6f);
            } else {
                float height = 4 * 1.0f * s * (1 - s);
                Quaternionf base = new Quaternionf().rotationY(yawFacing).rotateX(-0.4f);
                Quaternionf rot = new Quaternionf().rotationY((float) (4 * Math.PI * s))
                        .mul(new Quaternionf().rotationAxis((float) (2 * Math.PI * s), right.x, right.y, right.z))
                        .mul(base);
                transform(display, new Vector3f(0, height, 0), rot, 0.45f);
            }
        }

        void landCoin() {
            sound(level, origin, TableSounds.CHIP_CLINK, 0.7f);
            level.sendParticles(ParticleTypes.END_ROD, origin.x, origin.y + 0.1, origin.z, 4, 0.06, 0.04, 0.06, 0.015);
            Component result = Component.literal(heads ? "Heads!" : "Tails!")
                    .withStyle(heads ? ChatFormatting.GOLD : ChatFormatting.AQUA, ChatFormatting.BOLD);
            Display.TextDisplay label = EntityTypes.TEXT_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);
            if (label != null) {
                label.setPos(origin.add(0, 0.45, 0));
                ((TextDisplayInvoker) label).tcards$setText(result);
                ((DisplayInvoker) label).tcards$setBillboard(Display.BillboardConstraints.CENTER);
                transform(label, new Vector3f(), new Quaternionf(), 0.8f);
                label.addTag(FX_TAG);
                LIVE.add(label);
                level.addFreshEntity(label);
                text = label;
            }
            Component line = player.getName().copy().append(" flips a coin: ").append(result);
            for (ServerPlayer p : level.players()) {
                if (p.distanceToSqr(origin) < 16 * 16) p.sendOverlayMessage(line);
            }
        }
    }
}
