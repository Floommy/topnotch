package net.togyk.tog_yks_cards.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.togyk.tog_yks_cards.tag.ModTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(value=EnvType.CLIENT)
@Mixin(value={ArmedEntityRenderState.class})
public class ArmedEntityRenderStateMixin {
    @Inject(method={"extractArmedEntityRenderState"}, at={@At(value="TAIL")})
    private static void extractArmedEntityRenderState(LivingEntity entity, ArmedEntityRenderState state, ItemModelResolver resolver, float f, CallbackInfo ci) {
        LocalPlayer player;
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (localPlayer instanceof LocalPlayer && entity != (player = localPlayer) && !ArmedEntityRenderStateMixin.isInFrontOf(player, entity)) {
            ItemStack rightHandItemStack;
            ItemStack leftHandItemStack = state.leftHandItemStack;
            if (leftHandItemStack.is(ModTags.Items.CARDS)) {
                ItemStack card = leftHandItemStack.getItem().getDefaultInstance();
                resolver.updateForLiving(state.leftHandItemState, card, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, entity);
                state.leftHandItemStack = card;
            }
            if ((rightHandItemStack = state.rightHandItemStack).is(ModTags.Items.CARDS)) {
                ItemStack card = rightHandItemStack.getItem().getDefaultInstance();
                resolver.updateForLiving(state.rightHandItemState, card, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, entity);
                state.rightHandItemStack = card;
            }
        }
    }

    @Unique
    private static boolean isInFrontOf(LocalPlayer player, LivingEntity entity) {
        float difference;
        float entityRotation = entity.getYRot();
        Vec3 entityView = entity.getViewVector(1.0f);
        Vec3 entityPos = entity.position();
        Vec3 cardPos = entityPos.add(entityView.multiply(0.5, 0.5, 0.5));
        Vec3 playerPos = player.position();
        double dz = playerPos.z - cardPos.z;
        double dx = playerPos.x - cardPos.x;
        float playerRotation = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        if (playerRotation < 0.0f) {
            playerRotation += 360.0f;
        }
        return Math.abs(difference = (playerRotation - entityRotation + 180.0f + 540.0f) % 360.0f - 180.0f) <= 90.0f;
    }
}

