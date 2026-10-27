package net.togyk.tcards.mixin;

import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.togyk.tcards.client.GestureClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Arm animation for gestures; runs after the card-holding pose. */
@Mixin(value = PlayerModel.class, priority = 1500)
public class PlayerModelGestureMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
    private void tcards$gesturePose(AvatarRenderState state, CallbackInfo ci) {
        PlayerModel model = (PlayerModel) (Object) this;
        if (net.togyk.tcards.client.TrayHold.twoHanded(state)) net.togyk.tcards.client.TrayHold.poseArms(model.rightArm, model.leftArm);
        boolean slim = ((PlayerModelSlimAccessor) model).tcards$slim();
        net.togyk.tcards.client.Elbows.syncSleeve(model.rightArm, "right_sleeve", true, slim);
        net.togyk.tcards.client.Elbows.syncSleeve(model.leftArm, "left_sleeve", false, slim);
        GestureClient.applyPose(state.id, model.rightArm, model.leftArm, model.head);
    }
}
