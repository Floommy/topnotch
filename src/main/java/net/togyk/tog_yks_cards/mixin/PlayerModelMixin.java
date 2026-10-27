package net.togyk.tog_yks_cards.mixin;

import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.togyk.tog_yks_cards.tag.ModTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={PlayerModel.class})
public abstract class PlayerModelMixin {
    @Inject(method={"setupAnim*"}, at={@At(value="TAIL")})
    private void setCustomArmAngle(AvatarRenderState state, CallbackInfo ci) {
        PlayerModel model = (PlayerModel)(Object)this;
        if (state.rightHandItemStack.is(ModTags.Items.CARDS)) {
            model.rightArm.xRot = (float)Math.toRadians(-75.0);
        }
        if (state.leftHandItemStack.is(ModTags.Items.CARDS)) {
            model.leftArm.xRot = (float)Math.toRadians(-75.0);
        }
    }
}

