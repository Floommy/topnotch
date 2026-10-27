package net.togyk.tog_yks_cards.client.render;

import net.minecraft.client.renderer.feature.ModelFeatureRenderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.togyk.tog_yks_cards.blockentity.entities.CoinPileBlockEntity;
import net.togyk.tog_yks_cards.client.render.state.CoinPileBlockEntityRenderState;
import net.togyk.tog_yks_cards.item.custom.CoinItem;
import org.joml.Quaternionfc;

public class CoinPileBlockEntityRenderer
implements BlockEntityRenderer<CoinPileBlockEntity, CoinPileBlockEntityRenderState> {
    private final ItemModelResolver itemModelResolver;

    public CoinPileBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    public CoinPileBlockEntityRenderState createRenderState() {
        return new CoinPileBlockEntityRenderState();
    }

    public void extractRenderState(CoinPileBlockEntity blockEntity, CoinPileBlockEntityRenderState state, float tickProgress, Vec3 cameraPos,
     ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, tickProgress, cameraPos, crumblingOverlay);
        state.coins = blockEntity.getCoins();
    }

    public void submit(CoinPileBlockEntityRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraRenderState) {
        matrices.pushPose();
        matrices.translate(0.5, 0.03125, 0.5);
        matrices.rotate((Quaternionfc)Axis.XP.rotationDegrees(90.0f));
        List<Pair<ItemStack, Float>> coins = state.coins;
        for (int i = 0; i < coins.size(); ++i) {
            Item item;
            ItemStack stack = (ItemStack)coins.get(i).getFirst();
            ItemStackRenderState itemState = new ItemStackRenderState();
            state.updateItemRenderState(this.itemModelResolver, itemState, stack, ItemDisplayContext.FIXED);
            matrices.pushPose();
            matrices.rotate((Quaternionfc)Axis.ZP.rotationDegrees(((Float)coins.get(i).getSecond()).floatValue()));
            matrices.scale(0.665f, 0.665f, 0.665f);
            itemState.submit(matrices, queue, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            matrices.popPose();
            if (i == coins.size() - 1 || !((item = stack.getItem()) instanceof CoinItem)) continue;
            CoinItem coin = (CoinItem)item;
            matrices.translate(0.0, 0.0, -((double)coin.getHeight() / 16.0));
        }
        matrices.popPose();
    }
}

