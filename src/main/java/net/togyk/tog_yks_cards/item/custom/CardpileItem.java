package net.togyk.tog_yks_cards.item.custom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.togyk.tog_yks_cards.block.custom.CardpileBlock;
import net.togyk.tog_yks_cards.block.custom.MatBlock;
import net.togyk.tog_yks_cards.component.ModComponents;
import net.togyk.tog_yks_cards.item.custom.MatPlaceable;
import net.togyk.tog_yks_cards.mat_item.CardPileMatItem;

public class CardpileItem
extends BlockItem
implements MatPlaceable {
    public CardpileItem(CardpileBlock block, Item.Properties properties) {
        super((Block)block, properties);
    }

    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        if (super.placeBlock(context, state)) {
            Level level = context.getLevel();
            BlockPos pos = context.getClickedPos();
            ItemStack stack = context.getItemInHand();
            Block block = state.getBlock();
            if (block instanceof CardpileBlock) {
                CardpileBlock cardpile = (CardpileBlock)block;
                if (context.getPlayer() instanceof Player player) {
                    List<ItemStack> cards = this.getCards(stack);
                    if (!cards.isEmpty()) {
                        cardpile.setCards(level, pos, cards);
                        cardpile.setFaceDown(level, pos, !context.getPlayer().isShiftKeyDown());
                    } else {
                        level.removeBlock(pos, false);
                        player.setItemInHand(context.getHand(), ItemStack.EMPTY);
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }

    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            List<ItemStack> cards = this.getCards(stack);
            Collections.shuffle(cards);
            this.setCards(cards, stack);
        }
        level.playSound((Entity)player, player.getOnPos(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0f, 1.0f);
        return InteractionResult.SUCCESS;
    }

    public List<ItemStack> getCards(ItemStack stack) {
        return (List)stack.getOrDefault(ModComponents.CARDS, new ArrayList());
    }

    public void setCards(List<ItemStack> cards, ItemStack stack) {
        stack.set(ModComponents.CARDS, cards);
    }

    @Override
    public CardPileMatItem getMatItem(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult blockHitResult) {
        List<ItemStack> cards = this.getCards(stack);
        if (cards.isEmpty()) {
            return null;
        }
        BlockState itemState = (BlockState)((BlockState)this.getBlock().defaultBlockState().setValue(CardpileBlock.FACING, player.getDirection())).setValue(CardpileBlock.FACE_DOWN, Boolean.valueOf(true));
        Vec3 relativeHit = blockHitResult.getLocation().subtract((double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
        return new CardPileMatItem(itemState, (int)(relativeHit.x * 16.0) - 8, (int)(relativeHit.z * 16.0) - 8, cards, this);
    }

    @Override
    public boolean place(MatBlock mat, ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult blockHitResult) {
        if (!MatPlaceable.super.place(mat, stack, state, level, pos, player, hand, blockHitResult)) {
            player.setItemInHand(hand, ItemStack.EMPTY);
            return false;
        }
        return true;
    }
}

