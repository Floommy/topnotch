package net.togyk.tcards.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.togyk.tcards.client.MessyPiles;
import net.togyk.tog_yks_cards.block.custom.CardpileBlock;
import net.togyk.tog_yks_cards.client.render.MatBlockEntityRenderer;
import net.togyk.tog_yks_cards.client.render.state.MatBlockEntityRenderState;
import net.togyk.tog_yks_cards.mat_item.CardPileMatItem;
import net.togyk.tog_yks_cards.mat_item.MatItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MatBlockEntityRenderer.class)
public class MessyPileMatMixin {
    @WrapOperation(method = "renderItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"))
    private void tcards$messy(ItemStackRenderState card, PoseStack pose, SubmitNodeCollector queue, int light, int overlay, int outline,
                              Operation<Void> original, @Local(argsOnly = true) MatItem item, @Local(argsOnly = true) MatBlockEntityRenderState state) {
        if (!(item instanceof CardPileMatItem pile) || !(item.getState().getBlock() instanceof CardpileBlock block)) {
            original.call(card, pose, queue, light, overlay, outline);
            return;
        }
        int stage = block.getCountStage(pile.getCards().size());
        boolean faceDown = item.getState().getValue(CardpileBlock.FACE_DOWN);
        long base = state.blockPos == null ? 0 : state.blockPos.asLong();
        long seed = base * 131 + item.getX() * 17L + item.getZ() * 7919L + pile.getCards().size();
        // face up: XP +90 -> local +Z is down; face down: XP -90 -> local -Z is down
        MessyPiles.submit(original, card, pose, queue, light, overlay, outline, seed, faceDown ? -1f : 1f, stage, MessyPiles.heightPx(stage));
    }
}
