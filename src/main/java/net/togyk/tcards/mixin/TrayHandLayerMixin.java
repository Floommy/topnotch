package net.togyk.tcards.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.togyk.tcards.client.TrayHold;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Third person: a tray carried with both hands is drawn level in front of the player. */
@Mixin(ItemInHandLayer.class)
public class TrayHandLayerMixin {
    @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
    private void tcards$tray(ArmedEntityRenderState state, ItemStackRenderState item, ItemStack stack, HumanoidArm arm,
                             PoseStack pose, SubmitNodeCollector collector, int light, CallbackInfo ci) {
        if (!(state instanceof AvatarRenderState) || !TrayHold.isTray(stack) || !TrayHold.twoHanded(state)) return;
        Object model = ((RenderLayer<?, ?>) (Object) this).getParentModel();
        if (!(model instanceof HumanoidModel<?> humanoid)) return;
        ci.cancel();
        TrayHold.submitThirdPerson(humanoid, stack, pose, collector, light, state.outlineColor);
    }
}
