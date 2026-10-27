package net.togyk.tcards.client;

import java.util.HashMap;
import java.util.Map;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.togyk.tcards.Gesture;
import net.togyk.tcards.GesturePayloads;

/** Client-side state of currently shown gestures and the arm poses for them. */
@Environment(EnvType.CLIENT)
public final class GestureClient {
    private GestureClient() {}

    private static final long FADE_IN_MS = 150;
    private static final long FADE_OUT_MS = 250;

    private static final class Active {
        final Gesture gesture;
        final long startMs;
        int soundsPlayed;
        Active(Gesture gesture, long startMs) {
            this.gesture = gesture;
            this.startMs = startMs;
        }
        Gesture gesture() { return gesture; }
        long startMs() { return startMs; }
    }

    private static final Map<Integer, Active> ACTIVE = new HashMap<>();

    public static void init() {
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(GestureClient::tick);
        ClientPlayNetworking.registerGlobalReceiver(GesturePayloads.Show.TYPE, (payload, context) -> {
            Gesture gesture = Gesture.byId(payload.gesture());
            if (gesture == null) return;
            context.client().execute(() -> {
                if (context.player() != null && context.player().getId() == payload.entityId()) return; // already shown locally
                ACTIVE.put(payload.entityId(), new Active(gesture, System.currentTimeMillis()));
            });
        });
    }

    public static void send(Gesture gesture) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null) return;
        // show it right away for yourself (F5 / overlay), the server shows it to everyone else
        ACTIVE.put(mc.player.getId(), new Active(gesture, System.currentTimeMillis()));
        mc.player.sendOverlayMessage(label(gesture));
        if (ClientPlayNetworking.canSend(GesturePayloads.Make.TYPE)) {
            ClientPlayNetworking.send(new GesturePayloads.Make((byte) gesture.ordinal()));
        } else {
            TableClient.warnServerOutdated(mc);
        }
    }

    public static MutableComponent label(Gesture gesture) {
        return Component.literal(gesture.label).withStyle(gesture.color, ChatFormatting.BOLD);
    }

    /** Progress info of an active gesture, or null. */
    private record Anim(Gesture gesture, float t, float blend) {}

    private static Anim anim(int entityId) {
        Active a = ACTIVE.get(entityId);
        if (a == null) return null;
        long duration = a.gesture().durationMs;
        long elapsed = System.currentTimeMillis() - a.startMs();
        if (elapsed < 0 || elapsed > duration) {
            if (elapsed > duration + 1000) ACTIVE.remove(entityId);
            return null;
        }
        float b = Math.min(1.0f, Math.min(elapsed / (float) FADE_IN_MS, (duration - elapsed) / (float) FADE_OUT_MS));
        return new Anim(a.gesture(), elapsed / 1000.0f, b * b * (3 - 2 * b));
    }

    private static float wave(float t, double hz) {
        return (float) Math.sin(t * Math.PI * 2 * hz);
    }

    /**
     * Arm poses (third person). Right arm: negative xRot = forward/up, positive zRot = outward.
     * Forearms (elbow joint): negative xRot bends the lower arm forward/up. Left arm = mirrored (yRot / zRot negated).
     */
    public static void applyPose(int entityId, ModelPart rightArm, ModelPart leftArm, ModelPart head) {
        Anim a = anim(entityId);
        if (a == null) return;
        applyPoseAt(a.gesture(), a.t(), a.blend(), rightArm, leftArm, head);
    }

    /** The pose of a gesture at time t (seconds) with fade b (0..1). Public for the render tests. */
    public static void applyPoseAt(Gesture gesture, float t, float b, ModelPart rightArm, ModelPart leftArm, ModelPart head) {
        ModelPart rf = Elbows.forearm(rightArm), lf = Elbows.forearm(leftArm);
        switch (gesture) {
            case HIT -> {                                                                         // finger taps the table
                pose(rightArm, -0.55f, -0.25f, 0.0f, b);
                pose(rf, -0.6f - 0.35f * (0.5f + 0.5f * wave(t, 2.5)), 0.0f, 0.0f, b);
            }
            case STAND -> {                                                                       // flat forearm wiped sideways
                pose(rightArm, -0.55f, 0.5f * wave(t, 2.0), 0.0f, b);
                pose(rf, -1.0f, 0.0f, 0.0f, b);
            }
            case DOUBLE -> pose(rightArm, -3.05f + 0.06f * Math.abs(wave(t, 1.5)), 0.0f, 0.12f, b); // arm straight up, one finger
            case SPLIT -> {                                                                       // both hands forward, apart
                pose(rightArm, -1.2f, -0.55f, 0.0f, b);
                pose(leftArm, -1.2f, 0.55f, 0.0f, b);
                pose(rf, -0.5f, 0.0f, 0.0f, b);
                pose(lf, -0.5f, 0.0f, 0.0f, b);
            }
            case WAVE -> {                                                                        // upper arm out to the side, forearm up and waving
                pose(rightArm, -0.25f, 0.0f, 1.15f, b);
                pose(rf, 0.0f, 0.0f, 1.55f + 0.35f * wave(t, 2.5), b);
            }
            case CHEER -> {                                                                       // both arms up, bouncing
                float up = -3.0f + 0.18f * wave(t, 3.0);
                pose(rightArm, up, 0.0f, 0.35f, b);
                pose(leftArm, up, 0.0f, -0.35f, b);
                pose(rf, -0.35f + 0.2f * wave(t, 3.0), 0.0f, 0.0f, b);
                pose(lf, -0.35f + 0.2f * wave(t, 3.0), 0.0f, 0.0f, b);
            }
            case FACEPALM -> {                                                                    // elbows out, both hands on the face, head down
                pose(rightArm, -2.1f, -0.2f, 0.6f, b);
                pose(leftArm, -2.1f, 0.2f, -0.6f, b);
                pose(rf, -0.8f, 0.0f, 0.0f, b);
                pose(lf, -0.8f, 0.0f, 0.0f, b);
                pose(head, 0.35f, 0.12f * wave(t, 2.0), 0.0f, b);   // absolute: head goes down even if you were looking up
            }
            case CLAP -> {                                                                        // forearms swing together in front of the chest
                float in = 0.45f + 0.25f * (0.5f + 0.5f * wave(t, 2.5));
                pose(rightArm, -0.7f, -in, 0.0f, b);
                pose(leftArm, -0.7f, in, 0.0f, b);
                pose(rf, -1.1f, 0.0f, 0.0f, b);
                pose(lf, -1.1f, 0.0f, 0.0f, b);
            }
            case POINT -> pose(rightArm, Math.max(-2.05f, Math.min(-1.15f, head.xRot * 0.5f - 1.5708f)) + 0.04f * wave(t, 3.0),
                    Math.max(-0.30f, Math.min(0.30f, head.yRot * 0.5f)) - 0.05f, 0.0f, b); // forward, only follows the view a little
            case SHRUG -> {                                                                       // elbows at the sides, palms up, shoulders up twice
                float bump = Math.abs(wave(t, 1.25));
                pose(rightArm, 0.05f, 0.0f, 0.25f + 0.1f * bump, b);
                pose(leftArm, 0.05f, 0.0f, -0.25f - 0.1f * bump, b);
                pose(rf, -1.45f, 0.35f, 0.0f, b);
                pose(lf, -1.45f, -0.35f, 0.0f, b);
                rightArm.y -= b * 1.2f * bump;
                leftArm.y -= b * 1.2f * bump;
            }
            case DRUM -> {                                                                        // fingers drumming on the table
                pose(rightArm, -0.8f, -0.4f, 0.0f, b);
                pose(leftArm, -0.8f, 0.4f, 0.0f, b);
                pose(rf, -0.25f + 0.25f * wave(t, 4.0), 0.0f, 0.0f, b);
                pose(lf, -0.25f - 0.25f * wave(t, 4.0), 0.0f, 0.0f, b);
            }
            case BECKON -> {                                                                      // "come here": forearm curls in
                pose(rightArm, -1.2f, -0.2f, 0.0f, b);
                pose(rf, -0.5f - 0.6f * (0.5f + 0.5f * wave(t, 2.0)), 0.0f, 0.0f, b);
            }
            case ANGRY -> {                                                                       // both fists shaking, glaring
                float shake = wave(t, 6.0);
                pose(rightArm, -0.9f + 0.08f * shake, -0.2f, 0.6f, b);
                pose(leftArm, -0.9f - 0.08f * shake, 0.2f, -0.6f, b);
                pose(rf, -1.5f + 0.28f * shake, 0.0f, 0.0f, b);
                pose(lf, -1.5f - 0.28f * shake, 0.0f, 0.0f, b);
                pose(head, 0.2f, 0.1f * wave(t, 3.5), 0.0f, b);
            }
            case SWEAT -> {                                                                       // fanning the face, nervous glances
                float glance = 0.5f * (float) Math.tanh(3.0 * wave(t, 0.9));
                pose(head, 0.05f, glance, 0.0f, b);
                pose(rightArm, -1.6f, -0.6f, 0.0f, b);
                pose(rf, -0.9f, 0.0f, 0.35f * wave(t, 3.0), b);
            }
        }
    }

    private static void pose(ModelPart part, float x, float y, float z, float blend) {
        if (part == null) return;   // e.g. a model without elbows (other mods)
        part.xRot = part.xRot + (x - part.xRot) * blend;
        part.yRot = part.yRot + (y - part.yRot) * blend;
        part.zRot = part.zRot + (z - part.zRot) * blend;
    }

    /** First-person hand movement for your own gesture. */
    public static void applyFirstPerson(int entityId, boolean mainHand, boolean rightSide, com.mojang.blaze3d.vertex.PoseStack pose) {
        Anim a = anim(entityId);
        if (a == null) return;
        boolean twoHanded = switch (a.gesture()) {
            case SPLIT, CHEER, CLAP, SHRUG, DRUM, FACEPALM, ANGRY -> true;
            default -> false;
        };
        if (!mainHand && !twoHanded) return;
        float t = a.t(), b = a.blend();
        float side = rightSide ? 1.0f : -1.0f;
        switch (a.gesture()) {
            case HIT -> pose.translate(0.0f, b * (-0.12f - 0.10f * Math.abs(wave(t, 1.25))), b * -0.05f);
            case STAND -> pose.translate(b * 0.18f * wave(t, 2.0), b * -0.05f, 0.0f);
            case DOUBLE -> pose.translate(b * -0.06f * side, b * (0.45f + 0.03f * Math.abs(wave(t, 1.5))), b * 0.05f);
            case SPLIT -> pose.translate(b * 0.12f * side, b * 0.12f, b * -0.05f);
            case WAVE -> pose.translate(b * (0.05f * side + 0.10f * wave(t, 2.5)), b * 0.35f, 0.0f);
            case CHEER -> pose.translate(b * 0.05f * side, b * (0.40f + 0.06f * wave(t, 3.0)), 0.0f);
            case FACEPALM -> pose.translate(b * -0.16f * side, b * 0.30f, b * 0.02f);
            case CLAP -> pose.translate(b * -side * (0.10f + 0.12f * (0.5f + 0.5f * wave(t, 2.5))), 0.0f, b * -0.05f);
            case POINT -> pose.translate(b * -0.08f * side, b * 0.10f, b * -0.20f);
            case SHRUG -> pose.translate(b * 0.10f * side, b * (0.05f + 0.08f * Math.abs(wave(t, 1.25))), 0.0f);
            case DRUM -> pose.translate(0.0f, b * (-0.10f + 0.05f * side * wave(t, 4.0)), b * -0.05f);
            case BECKON -> pose.translate(0.0f, b * 0.05f, b * (-0.05f + 0.08f * wave(t, 2.0)));
            case ANGRY -> pose.translate(b * -0.05f * side, b * (0.18f + 0.06f * side * wave(t, 6.0)), b * -0.08f);
            case SWEAT -> pose.translate(b * (-0.10f * side + 0.07f * wave(t, 3.0)), b * 0.32f, b * -0.05f);
        }
    }

    /** When (seconds into the gesture) a sound plays; matched to the arm animation. */
    private static final double[] NONE = {};
    private static final double[] FACEPALM_T = {0.2, 0.5};
    private static final double[] CLAP_T = {0.5, 0.9, 1.3, 1.7};
    private static final double[] DRUM_T = drumTimes();
    private static final double[] BECKON_T = {0.15};
    private static final double[] SWEAT_T = {1.2};
    private static final double[] ANGRY_T = {0.2};

    private static double[] drumTimes() {
        java.util.List<Double> times = new java.util.ArrayList<>();
        for (int k = 0; k < 10; k++) {
            for (double off : new double[]{1 / 16.0, 3 / 16.0}) {
                double time = off + k / 4.0;
                if (time >= 0.15 && time <= 2.25) times.add(time);
            }
        }
        return times.stream().mapToDouble(Double::doubleValue).toArray();
    }

    private static double[] soundTimes(Gesture g) {
        return switch (g) {
            case FACEPALM -> FACEPALM_T;
            case CLAP -> CLAP_T;
            case DRUM -> DRUM_T;
            case BECKON -> BECKON_T;
            case SWEAT -> SWEAT_T;
            case ANGRY -> ANGRY_T;
            default -> NONE;
        };
    }

    private static void playSound(net.minecraft.world.entity.Entity e, Gesture g, int index) {
        net.minecraft.sounds.SoundEvent sound;
        float vol = 0.8f, pitch = 1.0f;
        switch (g) {
            case FACEPALM -> {
                sound = index == 0 ? net.togyk.tcards.TableSounds.GESTURE_SLAP : net.togyk.tcards.TableSounds.GESTURE_SAD;
                vol = index == 0 ? 0.8f : 0.6f;
            }
            case CLAP -> {
                sound = net.togyk.tcards.TableSounds.GESTURE_CLAP;
                pitch = 0.95f + (index % 3) * 0.05f;
            }
            case DRUM -> {
                sound = net.togyk.tcards.TableSounds.GESTURE_KNOCK;
                vol = 0.45f;
                pitch = index % 2 == 0 ? 1.0f : 0.88f;
            }
            case BECKON -> {
                sound = net.togyk.tcards.TableSounds.GESTURE_WHISTLE;
                vol = 0.6f;
            }
            case SWEAT -> sound = net.togyk.tcards.TableSounds.GESTURE_GULP;
            case ANGRY -> {
                sound = net.togyk.tcards.TableSounds.GESTURE_ANGRY;
                vol = 0.9f;
            }
            default -> {
                return;
            }
        }
        e.level().playLocalSound(e.getX(), e.getY() + 1.2, e.getZ(), sound, net.minecraft.sounds.SoundSource.VOICE, vol, pitch, false);   // "Voice/Speech" slider
    }

    /** Sounds and particles of all shown gestures. Every client plays them itself, so nobody hears them twice. */
    private static void tick(net.minecraft.client.Minecraft mc) {
        if (mc.level == null) {
            ACTIVE.clear();
            return;
        }
        long now = System.currentTimeMillis();
        java.util.Iterator<Map.Entry<Integer, Active>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Active> entry = it.next();
            Active a = entry.getValue();
            long elapsed = now - a.startMs;
            if (elapsed > a.gesture.durationMs + 1000) {
                it.remove();
                continue;
            }
            if (elapsed > a.gesture.durationMs) continue;
            net.minecraft.world.entity.Entity e = mc.level.getEntity(entry.getKey());
            double t = elapsed / 1000.0;
            double[] times = soundTimes(a.gesture);
            while (a.soundsPlayed < times.length && times[a.soundsPlayed] <= t) {
                if (e != null) playSound(e, a.gesture, a.soundsPlayed);
                a.soundsPlayed++;
            }
            if (e != null && a.gesture == Gesture.ANGRY && t > 0.15 && elapsed < a.gesture.durationMs - 200 && mc.level.getGameTime() % 8 == 0) {
                net.minecraft.util.RandomSource r = e.getRandom();   // grumpy clouds above the head
                mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.ANGRY_VILLAGER,
                        e.getX() + (r.nextDouble() - 0.5) * 0.6, e.getEyeY() + 0.55 + r.nextDouble() * 0.2, e.getZ() + (r.nextDouble() - 0.5) * 0.6, 0, 0, 0);
            }
            if (e != null && a.gesture == Gesture.SWEAT && t > 0.15 && elapsed < a.gesture.durationMs - 200 && mc.level.getGameTime() % 2 == 0) {
                net.minecraft.util.RandomSource r = e.getRandom();
                double side = r.nextBoolean() ? 1 : -1;
                double yaw = Math.toRadians(e.getYHeadRot());
                // drops leave from the temples, left and right of the face
                double ox = Math.cos(yaw) * 0.28 * side, oz = Math.sin(yaw) * 0.28 * side;
                mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.FALLING_WATER,
                        e.getX() + ox + (r.nextDouble() - 0.5) * 0.1, e.getEyeY() + 0.12 + r.nextDouble() * 0.12,
                        e.getZ() + oz + (r.nextDouble() - 0.5) * 0.1, 0, 0, 0);
                if (r.nextInt(5) == 0) {
                    mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.SPLASH,
                            e.getX(), e.getEyeY() + 0.45, e.getZ(), (r.nextDouble() - 0.5) * 0.1, 0.1, (r.nextDouble() - 0.5) * 0.1);
                }
            }
        }
    }
}
