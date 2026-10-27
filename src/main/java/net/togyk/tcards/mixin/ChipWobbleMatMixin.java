package net.togyk.tcards.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.togyk.tcards.client.ChipWobble;
import net.togyk.tog_yks_cards.client.render.MatBlockEntityRenderer;
import net.togyk.tog_yks_cards.client.render.state.MatBlockEntityRenderState;
import net.togyk.tog_yks_cards.mat_item.CoinPileMatItem;
import net.togyk.tog_yks_cards.mat_item.MatItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MatBlockEntityRenderer.class)
public class ChipWobbleMatMixin {
    @WrapOperation(method = "renderItem", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V"))
    private void tcards$wobble(PoseStack pose, double x, double y, double z, Operation<Void> original,
                               @Local(argsOnly = true) MatItem item, @Local(argsOnly = true) MatBlockEntityRenderState state) {
        original.call(pose, x, y, z);
        if (x == 0.0 && y == 0.0 && z < 0.0 && item instanceof CoinPileMatItem pile) {
            long base = state.blockPos == null ? 0 : state.blockPos.asLong();
            ChipWobble.step(pose, pile.getCoins().size(), base * 31 + item.getX() * 17L + item.getZ());
        }
    }
}
