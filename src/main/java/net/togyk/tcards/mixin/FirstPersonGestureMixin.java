package net.togyk.tcards.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.togyk.tcards.client.GestureClient;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.togyk.tcards.client.TrayHold;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lets you see your own gesture in first person; carries the serving tray like a map. */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public class FirstPersonGestureMixin {
    @Shadow
    private float calculateMapTilt(float xRot) { throw new AssertionError(); }

    @Shadow
    private void renderMapHand(PoseStack poseStack, SubmitNodeCollector collector, int light, HumanoidArm arm, PlayerRenderState playerState) { throw new AssertionError(); }

    @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
    private void tcards$gestureStart(PlayerRenderState playerState, FirstPersonHandsAndItemsRenderState handsState, float partialTick, float pitch,
                                     InteractionHand hand, float attack, ItemStack stack, float inverseArmHeight,
                                     PoseStack poseStack, SubmitNodeCollector collector, int light, CallbackInfo ci) {
        poseStack.pushPose();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            boolean rightSide = (hand == InteractionHand.MAIN_HAND) == (mc.player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT);
            GestureClient.applyFirstPerson(mc.player.getId(), hand == InteractionHand.MAIN_HAND, rightSide, poseStack);
        }
        AvatarRenderState avatar = playerState.avatarRenderState;
        if (!handsState.isScoping && avatar != null && hand == InteractionHand.MAIN_HAND && TrayHold.twoHanded(stack, handsState.offHandItem)) {
            tcards$twoHandedTray(poseStack, collector, light, pitch, inverseArmHeight, attack, playerState, avatar, stack);
            poseStack.popPose();
            ci.cancel();
        }
    }

    /** Same motion as the two-handed map (hands, tilt with the view, swing), with the tray where the map would be. */
    private void tcards$twoHandedTray(PoseStack pose, SubmitNodeCollector collector, int light, float xRot, float inverseArmHeight, float attack,
                                      PlayerRenderState playerState, AvatarRenderState avatar, ItemStack stack) {
        pose.pushPose();
        float sqrtAttack = Mth.sqrt(attack);
        float ySwing = -0.2f * Mth.sin(attack * (float) Math.PI);
        float zSwing = -0.4f * Mth.sin(sqrtAttack * (float) Math.PI);
        pose.translate(0.0f, -ySwing / 2.0f, zSwing);
        float tilt = calculateMapTilt(xRot);
        pose.translate(0.0f, 0.04f + inverseArmHeight * -1.2f + tilt * -0.5f, -0.72f);
        pose.rotateDegrees(Axis.XP, tilt * -85.0f);
        if (!avatar.isInvisible) {
            pose.pushPose();
            pose.rotateDegrees(Axis.YP, 90.0f);
            renderMapHand(pose, collector, light, HumanoidArm.RIGHT, playerState);
            renderMapHand(pose, collector, light, HumanoidArm.LEFT, playerState);
            pose.popPose();
        }
        pose.rotateDegrees(Axis.XP, Mth.sin(sqrtAttack * (float) Math.PI) * 20.0f);
        pose.scale(2.0f, 2.0f, 2.0f);
        TrayHold.submitFirstPerson(stack, pose, collector, light);
        pose.popPose();
    }

    @Inject(method = "submitArmWithItem", at = @At("RETURN"))
    private void tcards$gestureEnd(PlayerRenderState playerState, FirstPersonHandsAndItemsRenderState handsState, float partialTick, float pitch,
                                   InteractionHand hand, float attack, ItemStack stack, float inverseArmHeight,
                                   PoseStack poseStack, SubmitNodeCollector collector, int light, CallbackInfo ci) {
        poseStack.popPose();
    }
}
