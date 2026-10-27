package net.togyk.tog_yks_cards.item.custom;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.togyk.tog_yks_cards.block.custom.CardHolderBlock;
import net.togyk.tog_yks_cards.blockentity.entities.CardHolderBlockEntity;
import net.togyk.tog_yks_cards.component.ModComponents;
import net.togyk.tog_yks_cards.item.custom.MatPlaceable;
import net.togyk.tog_yks_cards.mat_item.CardHolderMatItem;

public class CardHolderItem
extends BlockItem
implements MatPlaceable {
    public CardHolderItem(CardHolderBlock block, Item.Properties properties) {
        super((Block)block, properties);
    }

    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        if (super.placeBlock(context, state)) {
            BlockPos pos;
            Level level = context.getLevel();
            BlockEntity blockEntity = level.getBlockEntity(pos = context.getClickedPos());
            if (blockEntity instanceof CardHolderBlockEntity) {
                CardHolderBlockEntity cardHolder = (CardHolderBlockEntity)blockEntity;
                cardHolder.setCards(this.getCards(context.getItemInHand()));
            }
            return true;
        }
        return false;
    }

    public List<ItemStack> getCards(ItemStack stack) {
        return (List)stack.getOrDefault(ModComponents.CARDS, new ArrayList());
    }

    public void setCards(List<ItemStack> cards, ItemStack stack) {
        stack.set(ModComponents.CARDS, cards.stream().filter(stack1 -> !stack1.isEmpty()).toList());
    }

    @Override
    public CardHolderMatItem getMatItem(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult blockHitResult) {
        BlockState itemState = (BlockState)this.getBlock().defaultBlockState().setValue(CardHolderBlock.ROTATION, Integer.valueOf(RotationSegment.convertToSegment((float)(player.getYRot() + 180.0f))));
        Vec3 relativeHit = blockHitResult.getLocation().subtract((double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
        return new CardHolderMatItem(itemState, (int)(relativeHit.x * 16.0) - 8, (int)(relativeHit.z * 16.0) - 8, this.getCards(stack), this);
    }
}

