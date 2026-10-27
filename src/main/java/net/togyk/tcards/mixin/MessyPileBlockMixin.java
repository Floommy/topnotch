package net.togyk.tcards.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.togyk.tcards.client.MessyPiles;
import net.togyk.tog_yks_cards.client.render.CardPileBlockEntityRenderer;
import net.togyk.tog_yks_cards.client.render.state.CardPileBlockEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CardPileBlockEntityRenderer.class)
public class MessyPileBlockMixin {
    @WrapOperation(method = "submit(Lnet/togyk/tog_yks_cards/client/render/state/CardPileBlockEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"))
    private void tcards$messy(ItemStackRenderState card, PoseStack pose, SubmitNodeCollector queue, int light, int overlay, int outline,
                              Operation<Void> original, @Local(argsOnly = true) CardPileBlockEntityRenderState state) {
        long seed = state.blockPos == null ? 0 : state.blockPos.asLong() * 7 + state.countStage;
        // ground piles are always rendered card-face-up (XP +90): local +Z points down
        MessyPiles.submit(original, card, pose, queue, light, overlay, outline, seed, 1f, state.countStage, MessyPiles.heightPx(state.countStage));
    }
}
