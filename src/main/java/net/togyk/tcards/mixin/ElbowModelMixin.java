package net.togyk.tcards.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.togyk.tcards.client.Elbows;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Player arms (skin, sleeves, chest armour) get an elbow joint for the gestures. */
@Mixin(PlayerModel.class)
public class ElbowModelMixin {
    @Inject(method = "createMesh", at = @At("RETURN"))
    private static void tcards$elbows(CubeDeformation scale, boolean slim, CallbackInfoReturnable<MeshDefinition> cir) {
        Elbows.splitArms(cir.getReturnValue());
    }

    @Inject(method = "createArmorMeshSet", at = @At("RETURN"))
    private static void tcards$armorElbows(CubeDeformation inner, CubeDeformation outer, CallbackInfoReturnable<ArmorModelSet<MeshDefinition>> cir) {
        ArmorModelSet<MeshDefinition> set = cir.getReturnValue();
        Elbows.splitArms(set.head());
        Elbows.splitArms(set.chest());
        Elbows.splitArms(set.legs());
        Elbows.splitArms(set.feet());
    }

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("HEAD"))
    private void tcards$straighten(AvatarRenderState state, CallbackInfo ci) {
        PlayerModel model = (PlayerModel) (Object) this;
        Elbows.reset(model.rightArm);
        Elbows.reset(model.leftArm);
    }

    @Inject(method = "translateToHand(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;)V", at = @At("TAIL"))
    private void tcards$itemFollowsForearm(AvatarRenderState state, HumanoidArm arm, PoseStack pose, CallbackInfo ci) {
        PlayerModel model = (PlayerModel) (Object) this;
        Elbows.toHand(arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm, pose);
    }
}
