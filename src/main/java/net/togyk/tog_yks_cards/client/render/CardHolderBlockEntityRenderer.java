package net.togyk.tog_yks_cards.client.render;

import net.minecraft.client.renderer.feature.ModelFeatureRenderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.togyk.tog_yks_cards.block.custom.CardHolderBlock;
import net.togyk.tog_yks_cards.blockentity.entities.CardHolderBlockEntity;
import net.togyk.tog_yks_cards.client.render.state.CardHolderBlockEntityRenderState;
import org.joml.Quaternionfc;

public class CardHolderBlockEntityRenderer
implements BlockEntityRenderer<CardHolderBlockEntity, CardHolderBlockEntityRenderState> {
    private final ItemModelResolver itemModelResolver;

    public CardHolderBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    public CardHolderBlockEntityRenderState createRenderState() {
        return new CardHolderBlockEntityRenderState();
    }

    public void extractRenderState(CardHolderBlockEntity blockEntity, CardHolderBlockEntityRenderState state, float tickProgress, Vec3 cameraPos,
     ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        LocalPlayer player;
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, tickProgress, cameraPos, crumblingOverlay);
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer localPlayer = mc.player;
        state.cards = localPlayer instanceof LocalPlayer && blockEntity.isInFront((Player)(player = localPlayer)) ? blockEntity.getCards() : blockEntity.getCards().stream().map(stack -> stack.getItem().getDefaultInstance()).toList();
        if (blockEntity.getBlockState().getBlock() instanceof CardHolderBlock cardHolder) {
            state.rotation = cardHolder.getYRotationDegrees(blockEntity.getBlockState());
        }
    }

    public void submit(CardHolderBlockEntityRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraRenderState) {
        matrices.pushPose();
        matrices.translate(0.5, 0.25, 0.5);
        matrices.scale(0.4f, 0.4f, 0.4f);
        matrices.rotate((Quaternionfc)Axis.YN.rotationDegrees(state.rotation + 180.0f));
        List<ItemStack> stacks = state.cards;
        int size = stacks.size();
        int amountRendered = Math.min(size, 10);
        for (int i = 0; i < amountRendered; ++i) {
            ItemStackRenderState itemState = new ItemStackRenderState();
            ItemStack stack = stacks.get(i == amountRendered - 1 ? size - 1 : i);
            state.updateItemRenderState(this.itemModelResolver, itemState, stack, ItemDisplayContext.FIXED);
            matrices.pushPose();
            if (amountRendered > 1) {
                matrices.translate(((double)i / (double)(amountRendered - 1) - 0.5) * -1.0, 0.0, 0.0);
                matrices.rotate((Quaternionfc)Axis.YP.rotationDegrees(10.0f));
            }
            itemState.submit(matrices, queue, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            matrices.popPose();
        }
        matrices.popPose();
    }
}

