package net.togyk.tog_yks_cards.client.render;

import net.minecraft.client.renderer.feature.ModelFeatureRenderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.togyk.tog_yks_cards.block.custom.CardpileBlock;
import net.togyk.tog_yks_cards.blockentity.entities.CardPileBlockEntity;
import net.togyk.tog_yks_cards.client.render.state.CardPileBlockEntityRenderState;
import net.togyk.tog_yks_cards.component.ModComponents;
import org.joml.Quaternionfc;

public class CardPileBlockEntityRenderer
implements BlockEntityRenderer<CardPileBlockEntity, CardPileBlockEntityRenderState> {
    private final ItemModelResolver itemModelResolver;

    public CardPileBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    public CardPileBlockEntityRenderState createRenderState() {
        return new CardPileBlockEntityRenderState();
    }

    public void extractRenderState(CardPileBlockEntity blockEntity, CardPileBlockEntityRenderState state, float tickProgress, Vec3 cameraPos,
     ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, tickProgress, cameraPos, crumblingOverlay);
        state.faceDown = blockEntity.isFaceDown();
        Block block = blockEntity.getBlockState().getBlock();
        if (block instanceof CardpileBlock) {
            CardpileBlock cardPile = (CardpileBlock)block;
            state.facing = cardPile.getFacing(blockEntity.getBlockState());
            state.countStage = cardPile.getCountStage(blockEntity.getBlockState());
        }
        if (state.faceDown) {
            ItemStack card = blockEntity.getCard().getItem().getDefaultInstance();
            card.set(ModComponents.FLIPPED, true);
            state.card = card;
        } else {
            state.card = blockEntity.getCard();
        }
    }

    public void submit(CardPileBlockEntityRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraRenderState) {
        matrices.pushPose();
        double height = switch (state.countStage) {
            case 4 -> 2.0;
            case 3 -> 1.0;
            case 2 -> 0.5;
            case 1 -> 0.25;
            default -> 0.0;
        };
        matrices.translate(0.5, (height + 0.075) / 16.0, 0.5);
        matrices.rotate((Quaternionfc)Axis.XP.rotationDegrees(90.0f));
        matrices.scale(0.4f, 0.4f, 0.4f);
        if (state.facing == Direction.NORTH) {
            matrices.rotate((Quaternionfc)Axis.ZP.rotationDegrees(180.0f));
        } else if (state.facing == Direction.EAST) {
            matrices.rotate((Quaternionfc)Axis.ZP.rotationDegrees(270.0f));
        } else if (state.facing == Direction.SOUTH) {
            matrices.rotate((Quaternionfc)Axis.ZP.rotationDegrees(0.0f));
        } else if (state.facing == Direction.WEST) {
            matrices.rotate((Quaternionfc)Axis.ZP.rotationDegrees(90.0f));
        }
        ItemStackRenderState itemState = new ItemStackRenderState();
        ItemStack stack = state.card;
        state.updateItemRenderState(this.itemModelResolver, itemState, stack, ItemDisplayContext.FIXED);
        itemState.submit(matrices, queue, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        matrices.popPose();
    }
}

