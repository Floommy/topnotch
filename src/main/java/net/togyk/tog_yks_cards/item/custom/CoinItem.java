package net.togyk.tog_yks_cards.item.custom;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.togyk.tog_yks_cards.block.ModBlocks;
import net.togyk.tog_yks_cards.block.custom.CoinPileblock;
import net.togyk.tog_yks_cards.item.custom.MatPlaceable;
import net.togyk.tog_yks_cards.mat_item.CoinPileMatItem;
import net.togyk.tog_yks_cards.mat_item.MatItem;

public class CoinItem
extends BlockItem
implements MatPlaceable {
    private final int width;
    private final int height;

    public CoinItem(int width, int height, Item.Properties properties) {
        super((Block)ModBlocks.COIN_PIlE, properties);
        this.width = width;
        this.height = height;
    }

    public CoinItem(Item.Properties properties) {
        super((Block)ModBlocks.COIN_PIlE, properties);
        this.width = 6;
        this.height = 1;
    }

    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        if (super.placeBlock(context, state)) {
            Level level = context.getLevel();
            BlockPos pos = context.getClickedPos();
            ItemStack stack = context.getItemInHand();
            Block block = state.getBlock();
            if (block instanceof CoinPileblock) {
                CoinPileblock coinPile = (CoinPileblock)block;
                coinPile.setCoins((BlockGetter)level, pos, List.of(stack.copy()));
            }
            return true;
        }
        return false;
    }

    @Override
    public MatItem getMatItem(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult blockHitResult) {
        BlockState itemState = this.getBlock().defaultBlockState();
        Vec3 relativeHit = blockHitResult.getLocation().subtract((double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
        return new CoinPileMatItem(itemState, (int)(relativeHit.x * 16.0) - 8, (int)(relativeHit.z * 16.0) - 8, stack.copy());
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }
}

