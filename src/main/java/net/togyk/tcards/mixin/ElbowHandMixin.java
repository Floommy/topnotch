package net.togyk.tcards.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.resources.Identifier;
import net.togyk.tcards.client.Elbows;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** First-person hand: always a straight arm, lower sleeve like the upper one. */
@Mixin(AvatarRenderer.class)
public class ElbowHandMixin {
    @Inject(method = "renderHand", at = @At("HEAD"))
    private void tcards$straightHand(PoseStack pose, SubmitNodeCollector collector, int light, Identifier skin, ModelPart arm, boolean hasSleeve, CallbackInfo ci) {
        Elbows.reset(arm);
        Elbows.setSleeve(arm, "right_sleeve", hasSleeve);
        Elbows.setSleeve(arm, "left_sleeve", hasSleeve);
    }
}
