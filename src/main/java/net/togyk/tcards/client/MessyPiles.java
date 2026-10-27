package net.togyk.tcards.client;

import java.util.Random;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;

/**
 * Makes card piles look like real, slightly messy stacks: the top card is turned a few degrees and a couple of
 * cards below it stick out at other angles. Angles are stable per pile (seeded), so nothing jitters.
 */
@Environment(EnvType.CLIENT)
public final class MessyPiles {
    private MessyPiles() {}

    /**
     * Called with the pose the original renderer set up for the top card (card lying flat, local Z = card normal).
     *
     * @param zDown     local Z direction that points down into the pile (+1 or -1)
     * @param stage     pile size stage 0..4 of the original mod (0 = single card)
     * @param heightPx  pile height in pixels (1/16 block) as rendered by the original block model
     */
    public static void submit(Operation<Void> original, ItemStackRenderState card, PoseStack pose, SubmitNodeCollector queue,
                              int light, int overlay, int outline, long seed, float zDown, int stage, double heightPx) {
        Random r = new Random(seed * 31L + 17L);
        int under = Math.min(3, stage);
        // world distance -> local distance (the original renderer scales by 0.4)
        double step = heightPx / 16.0 / (under + 1) / 0.4;
        for (int k = under; k >= 1; k--) {
            pose.pushPose();
            pose.translate(0.0, 0.0, zDown * step * k);
            pose.rotate(Axis.ZP.rotationDegrees((r.nextFloat() * 2 - 1) * 9f));
            pose.translate((r.nextFloat() * 2 - 1) * 0.05, (r.nextFloat() * 2 - 1) * 0.05, 0.0);
            original.call(card, pose, queue, light, overlay, outline);
            pose.popPose();
        }
        pose.pushPose();
        pose.rotate(Axis.ZP.rotationDegrees((r.nextFloat() * 2 - 1) * 5f));
        original.call(card, pose, queue, light, overlay, outline);
        pose.popPose();
    }

    public static double heightPx(int stage) {
        return switch (stage) {
            case 4 -> 2.0;
            case 3 -> 1.0;
            case 2 -> 0.5;
            case 1 -> 0.25;
            default -> 0.0;
        };
    }
}
