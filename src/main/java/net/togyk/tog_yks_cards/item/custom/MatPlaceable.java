package net.togyk.tog_yks_cards.item.custom;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.togyk.tog_yks_cards.block.custom.MatBlock;
import net.togyk.tog_yks_cards.blockentity.entities.MatBlockEntity;
import net.togyk.tog_yks_cards.mat_item.MatItem;

public interface MatPlaceable {
    public MatItem getMatItem(ItemStack var1, BlockState var2, Level var3, BlockPos var4, Player var5, InteractionHand var6, BlockHitResult var7);

    public static boolean canPlaceItem(MatItem item, List<MatItem> items) {
        return item.isFullySupported() && items.stream().map(item1 -> item1.getAbsoluteShape().bounds()).noneMatch(box -> box.intersects(item.getAbsoluteShape().bounds()));
    }

    default public boolean canPlace(MatItem item, List<MatItem> items) {
        return item.isFullySupported() && items.stream().map(item1 -> item1.getAbsoluteShape().bounds()).noneMatch(box -> box.intersects(item.getAbsoluteShape().bounds()));
    }

    default public boolean canPlace(MatItem item, MatBlock mat, Level level, BlockPos pos) {
        return this.canPlace(item, mat.getAllMatItems((BlockGetter)level, pos));
    }

    default public boolean place(MatBlock mat, ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult blockHitResult) {
        BlockEntity blockEntity;
        MatItem newItem = this.getMatItem(stack, state, level, pos, player, hand, blockHitResult);
        if (newItem != null && (blockEntity = level.getBlockEntity(pos)) instanceof MatBlockEntity) {
            MatBlockEntity matEntity = (MatBlockEntity)blockEntity;
            newItem.setMat(matEntity);
            if (this.canPlace(newItem, mat, level, pos)) {
                mat.addMatItem((BlockGetter)level, pos, newItem);
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
            }
            mat.setChanged((BlockGetter)level, pos);
            return true;
        }
        return false;
    }
}

