package net.togyk.tog_yks_cards.client.render;

import net.minecraft.client.renderer.feature.ModelFeatureRenderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.togyk.tog_yks_cards.block.custom.CardHolderBlock;
import net.togyk.tog_yks_cards.block.custom.CardpileBlock;
import net.togyk.tog_yks_cards.block.custom.MatBlock;
import net.togyk.tog_yks_cards.blockentity.entities.MatBlockEntity;
import net.togyk.tog_yks_cards.client.render.state.MatBlockEntityRenderState;
import net.togyk.tog_yks_cards.item.custom.CoinItem;
import net.togyk.tog_yks_cards.item.custom.MatPlaceable;
import net.togyk.tog_yks_cards.mat_item.CardHolderMatItem;
import net.togyk.tog_yks_cards.mat_item.CardPileMatItem;
import net.togyk.tog_yks_cards.mat_item.CoinPileMatItem;
import net.togyk.tog_yks_cards.mat_item.MatItem;
import net.togyk.tog_yks_cards.mat_item.SimpleMatItem;
import net.togyk.tog_yks_cards.tag.ModTags;
import org.joml.Quaternionfc;

@Environment(value=EnvType.CLIENT)
public class MatBlockEntityRenderer
implements BlockEntityRenderer<MatBlockEntity, MatBlockEntityRenderState> {
    private final ItemModelResolver itemModelResolver;

    public MatBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    public MatBlockEntityRenderState createRenderState() {
        return new MatBlockEntityRenderState();
    }

    public void extractRenderState(MatBlockEntity blockEntity, MatBlockEntityRenderState state, float tickProgress, Vec3 cameraPos,
     ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, tickProgress, cameraPos, crumblingOverlay);
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (localPlayer instanceof LocalPlayer) {
            LocalPlayer player = localPlayer;
            state.items = blockEntity.getMatItems().stream().map(item -> item.getRenderState((Player)player)).toList();
        }
        if (blockEntity.getLevel() instanceof Level level) {
            state.blockState = level.getBlockState(blockEntity.getBlockPos());
        }
        state.mat = blockEntity;
    }

    public void submit(MatBlockEntityRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraRenderState) {
        ItemStack itemStack;
        BlockHitResult result;
        HitResult hitResult;
        for (MatItem item : state.items) {
            this.renderItem(item, state, matrices, queue, cameraRenderState);
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player != null && (hitResult = minecraft.hitResult) instanceof BlockHitResult && (result = (BlockHitResult)hitResult).getBlockPos().equals(state.blockPos) && !(itemStack = player.getItemInHand(InteractionHand.MAIN_HAND)).isEmpty()) {
            Object item;
            Item item2 = itemStack.getItem();
            if (item2 instanceof MatPlaceable) {
                MatPlaceable placeable = (MatPlaceable)item2;
                item = placeable.getMatItem(itemStack, state.blockState, player.level(), state.blockPos, (Player)player, InteractionHand.MAIN_HAND, result);
                if (item != null) {
                    ((MatItem)item).setMat(state.mat);
                    if (MatPlaceable.canPlaceItem((MatItem)item, state.mat.getAllMatItems())) {
                        this.renderItemPreview((MatItem)item, state, matrices, queue, cameraRenderState);
                    }
                }
            } else if (itemStack.is(ModTags.Items.DECORATIVE_MAT_ITEM) && (item = itemStack.getItem()) instanceof BlockItem) {
                BlockItem blockItem = (BlockItem)item;
                Vec3 relativeHit = result.getLocation().subtract((double)state.blockPos.getX(), (double)state.blockPos.getY(), (double)state.blockPos.getZ());
                SimpleMatItem item3 = new SimpleMatItem(blockItem, player.level(), (Player)player, InteractionHand.MAIN_HAND, result, (int)(relativeHit.x * 16.0) - 8, (int)(relativeHit.z * 16.0) - 8, itemStack);
                item3.setMat(state.mat);
                if (MatPlaceable.canPlaceItem(item3, state.mat.getAllMatItems())) {
                    this.renderItem(item3, state, matrices, queue, cameraRenderState);
                }
            }
        }
    }

    private void renderItem(MatItem item, MatBlockEntityRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraRenderState) {
        matrices.pushPose();
        matrices.translate((double)item.getX() / 16.0, 0.0625, (double)item.getZ() / 16.0);
        BlockState blockState = item.getState();
        BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(blockState);
        ArrayList parts = new ArrayList();
        model.collectParts(RandomSource.create(), parts);
        int[] tintColors = new int[]{-1};
        queue.submitBlockModel(matrices, RenderTypes.translucentMovingBlock(), parts, tintColors, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        if (item instanceof CardHolderMatItem cardHolder && blockState.getBlock() instanceof CardHolderBlock cardHolderBlock) {
            matrices.pushPose();
            matrices.translate(0.5, 0.25, 0.5);
            matrices.scale(0.4f, 0.4f, 0.4f);
            matrices.rotate((Quaternionfc)Axis.YN.rotationDegrees(cardHolderBlock.getYRotationDegrees(blockState) + 180.0f));
            var stacks = cardHolder.getCards();
            int size = stacks.size();
            int amountRendered = Math.min(size, 10);
            for (int i = 0; i < amountRendered; ++i) {
                ItemStackRenderState itemState = new ItemStackRenderState();
                ItemStack stack = (ItemStack)stacks.get(i == amountRendered - 1 ? size - 1 : i);
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
        } else if (item instanceof CardPileMatItem cardPile && blockState.getBlock() instanceof CardpileBlock cardpileBlock) {
            matrices.pushPose();
            double height = switch (cardpileBlock.getCountStage(cardPile.getCards().size())) {
                case 4 -> 2.0;
                case 3 -> 1.0;
                case 2 -> 0.5;
                case 1 -> 0.25;
                default -> 0.0;
            };
            if (((Boolean)blockState.getValue(CardpileBlock.FACE_DOWN)).booleanValue()) {
                matrices.translate(0.5, height / 16.0, 0.5);
                matrices.rotate((Quaternionfc)Axis.XP.rotationDegrees(-90.0f));
            } else {
                matrices.translate(0.5, (height + 0.075) / 16.0, 0.5);
                matrices.rotate((Quaternionfc)Axis.XP.rotationDegrees(90.0f));
            }
            matrices.scale(0.4f, 0.4f, 0.4f);
            Direction facing = (Direction)blockState.getValue(CardpileBlock.FACING);
            if (facing == Direction.NORTH) {
                matrices.rotate((Quaternionfc)Axis.ZP.rotationDegrees(180.0f));
            } else if (facing == Direction.EAST) {
                matrices.rotate((Quaternionfc)Axis.ZP.rotationDegrees(270.0f));
            } else if (facing == Direction.SOUTH) {
                matrices.rotate((Quaternionfc)Axis.ZP.rotationDegrees(0.0f));
            } else if (facing == Direction.WEST) {
                matrices.rotate((Quaternionfc)Axis.ZP.rotationDegrees(90.0f));
            }
            ItemStackRenderState itemState = new ItemStackRenderState();
            ItemStack stack = cardPile.getCard();
            state.updateItemRenderState(this.itemModelResolver, itemState, stack, ItemDisplayContext.FIXED);
            itemState.submit(matrices, queue, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            matrices.popPose();
        } else if (item instanceof CoinPileMatItem coinPile) {
            matrices.pushPose();
            matrices.translate(0.5, 0.03125, 0.5);
            matrices.rotate((Quaternionfc)Axis.XP.rotationDegrees(90.0f));
            var coins = coinPile.getCoins();
            for (int i = 0; i < coins.size(); ++i) {
                ItemStack stack = (ItemStack)coins.get(i).getFirst();
                ItemStackRenderState itemState = new ItemStackRenderState();
                state.updateItemRenderState(this.itemModelResolver, itemState, stack, ItemDisplayContext.FIXED);
                matrices.pushPose();
                matrices.rotate((Quaternionfc)Axis.ZP.rotationDegrees(((Float)coins.get(i).getSecond()).floatValue()));
                matrices.scale(0.665f, 0.665f, 0.665f);
                itemState.submit(matrices, queue, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
                matrices.popPose();
                if (i == coins.size() - 1 || !(stack.getItem() instanceof CoinItem coin)) continue;
                matrices.translate(0.0, 0.0, -((double)coin.getHeight() / 16.0));
            }
            matrices.popPose();
        }
        matrices.popPose();
    }

    private void renderItemPreview(MatItem item, MatBlockEntityRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraRenderState) {
        matrices.pushPose();
        matrices.translate((double)item.getX() / 16.0, 0.0625, (double)item.getZ() / 16.0);
        BlockState blockState = (BlockState)item.getState().setValue(MatBlock.PREVIEW, Boolean.valueOf(true));
        BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(blockState);
        ArrayList parts = new ArrayList();
        model.collectParts(RandomSource.create(), parts);
        int[] tintColors = new int[]{-1};
        queue.submitBlockModel(matrices, RenderTypes.translucentMovingBlock(), parts, tintColors, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        matrices.popPose();
    }
}

