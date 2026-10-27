package net.togyk.tcards.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.togyk.tcards.client.ChipWobble;
import net.togyk.tog_yks_cards.client.render.CoinPileBlockEntityRenderer;
import net.togyk.tog_yks_cards.client.render.state.CoinPileBlockEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CoinPileBlockEntityRenderer.class)
public class ChipWobbleBlockMixin {
    @WrapOperation(method = "submit(Lnet/togyk/tog_yks_cards/client/render/state/CoinPileBlockEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V"))
    private void tcards$wobble(PoseStack pose, double x, double y, double z, Operation<Void> original,
                               @Local(argsOnly = true) CoinPileBlockEntityRenderState state) {
        original.call(pose, x, y, z);
        if (x == 0.0 && y == 0.0 && state.coins != null) {
            ChipWobble.step(pose, state.coins.size(), state.blockPos == null ? 0 : state.blockPos.asLong());
        }
    }
}
