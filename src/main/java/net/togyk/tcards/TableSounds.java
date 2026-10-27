package net.togyk.tcards;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/** Card / chip sounds. All sounds are played server-side to everyone (no client prediction), so nobody hears them twice. */
public final class TableSounds {
    private TableSounds() {}

    public static SoundEvent CARD_PLACE, CARD_TAKE, CARD_SLIDE, CHIP_CLINK, SHUFFLE;
    public static SoundEvent GESTURE_ANGRY, GESTURE_SLAP, GESTURE_SAD, GESTURE_CLAP, GESTURE_KNOCK, GESTURE_WHISTLE, GESTURE_GULP;
    public static SoundEvent COIN_FLIP, CARD_FLICK, BELL_DING, TOWER_FALL;
    public static SoundEvent ATM_DISPENSE, ATM_BEEP, ATM_ERROR, ATM_CHIME, RECEIPT_PRINT, KIOSK_TAP;

    private static SoundEvent register(String name) {
        Identifier id = Identifier.fromNamespaceAndPath("tog_yks-cards", name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    public static void init() {
        CARD_PLACE = register("card_place");
        CARD_TAKE = register("card_take");
        CARD_SLIDE = register("card_slide");
        CHIP_CLINK = register("chip_clink");
        SHUFFLE = register("shuffle");
        GESTURE_SLAP = register("gesture_slap");
        GESTURE_ANGRY = register("gesture_angry");
        GESTURE_SAD = register("gesture_sad");
        GESTURE_CLAP = register("gesture_clap");
        GESTURE_KNOCK = register("gesture_knock");
        GESTURE_WHISTLE = register("gesture_whistle");
        GESTURE_GULP = register("gesture_gulp");
        COIN_FLIP = register("coin_flip");
        CARD_FLICK = register("card_flick");
        BELL_DING = register("bell_ding");
        TOWER_FALL = register("chip_tower_fall");
        ATM_DISPENSE = register("atm_dispense");
        ATM_BEEP = register("atm_beep");
        ATM_ERROR = register("atm_error");
        ATM_CHIME = register("atm_chime");
        RECEIPT_PRINT = register("receipt_print");
        KIOSK_TAP = register("kiosk_tap");
    }

    public static void play(Level level, BlockPos pos, SoundEvent sound, float volume) {
        if (level == null || level.isClientSide()) return;
        float pitch = 0.92f + level.getRandom().nextFloat() * 0.16f;
        level.playSound(null, pos, sound, SoundSource.BLOCKS, volume, pitch);
    }

    /** Small puff where a card lands. */
    public static void puff(Level level, double x, double y, double z) {
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.WHITE_SMOKE, x, y, z, 3, 0.06, 0.0, 0.06, 0.004);
        }
    }

    /** Replacement for the original mod's bundle sounds. chips = coin / chip context. */
    public static void replace(Level level, BlockPos pos, SoundEvent original, boolean chips) {
        if (level == null || level.isClientSide()) return;
        SoundEvent s;
        float vol = 0.7f;
        if (original == SoundEvents.BUNDLE_INSERT) s = chips ? CHIP_CLINK : CARD_PLACE;
        else if (original == SoundEvents.BUNDLE_REMOVE_ONE) s = chips ? CHIP_CLINK : CARD_TAKE;
        else if (original == SoundEvents.BOOK_PAGE_TURN) { s = SHUFFLE; vol = 0.8f; }
        else s = original;
        play(level, pos, s, vol);
    }
}
