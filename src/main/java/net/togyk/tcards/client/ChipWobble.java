package net.togyk.tcards.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Quaternionf;

/** Tall coin towers sway: every coin step tilts a little, so the tower bends more the higher it gets. */
@Environment(EnvType.CLIENT)
public final class ChipWobble {
    private ChipWobble() {}

    private static final int CALM = 12;

    /** Called after each "one coin up" translate of a coin pile render (local frame: XP +90, so X/Y are horizontal). */
    public static void step(PoseStack pose, int coins, long seed) {
        if (coins <= CALM) return;
        double t = System.currentTimeMillis() / 1000.0;
        double phase = Math.floorMod(seed * 2654435761L, 10000L) / 10000.0 * Math.PI * 2;
        double amp = (coins - CALM) * 0.00035;
        double angle = amp * (Math.sin(t * 2.3 + phase) + 0.4 * Math.sin(t * 3.7 + phase * 1.3));
        pose.rotate(new Quaternionf().rotationAxis((float) angle, (float) Math.cos(phase), (float) Math.sin(phase), 0f));
    }
}
