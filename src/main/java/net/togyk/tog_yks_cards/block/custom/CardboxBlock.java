package net.togyk.tog_yks_cards.block.custom;

import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.togyk.tog_yks_cards.block.custom.MatBlock;
import net.togyk.tog_yks_cards.blockentity.entities.CardboxBlockEntity;
import net.togyk.tog_yks_cards.item.custom.CardboxItem;

public class CardboxBlock
extends BaseEntityBlock
implements SimpleWaterloggedBlock {
    public static final BooleanProperty OPEN = BooleanProperty.create((String)"open");
    public static final BooleanProperty EMPTY = BooleanProperty.create((String)"empty");
    public static final BooleanProperty WRAPPED = BooleanProperty.create((String)"wrapped");
    public static final VoxelShape SHAPE_NS = Block.box((double)6.0, (double)0.0, (double)5.2, (double)10.0, (double)2.0, (double)10.8);
    public static final VoxelShape SHAPE_EW = Block.box((double)5.2, (double)0.0, (double)6.0, (double)10.8, (double)2.0, (double)10.0);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    public VoxelShape getShape(BlockState state, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return switch ((Direction)state.getValue(FACING)) {
            case Direction.EAST, Direction.WEST -> SHAPE_EW;
            default -> SHAPE_NS;
        };
    }

    public CardboxBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)((BlockState)this.defaultBlockState().setValue(OPEN, Boolean.valueOf(false))).setValue(WRAPPED, Boolean.valueOf(false))).setValue(MatBlock.PREVIEW, Boolean.valueOf(false)));
    }

    protected List<ItemStack> getDrops(BlockState state,
     LootParams.Builder builder) {
        List drops = super.getDrops(state, builder);
        Object object = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (object instanceof CardboxBlockEntity) {
            CardboxBlockEntity cardbox = (CardboxBlockEntity)(object);
            drops.add(cardbox.getCardbox());
        }
        return drops;
    }

    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos blockPos, Player player, BlockHitResult blockHitResult) {
        if (!this.isWrapped(state)) {
            boolean currentState = this.isOpen(state);
            this.setOpen(level, blockPos, state, !currentState);
            return InteractionResult.SUCCESS;
        }
        return super.useWithoutItem(state, level, blockPos, player, blockHitResult);
    }

    public BlockState getStateForPlacement(BlockPlaceContext blockPlaceContext) {
        FluidState fluidState = blockPlaceContext.getLevel().getFluidState(blockPlaceContext.getClickedPos());
        return (BlockState)((BlockState)this.defaultBlockState().setValue(FACING, blockPlaceContext.getHorizontalDirection().getOpposite())).setValue(WATERLOGGED, Boolean.valueOf(fluidState.getType() == Fluids.WATER));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{OPEN});
        builder.add(new Property[]{EMPTY});
        builder.add(new Property[]{WATERLOGGED});
        builder.add(new Property[]{FACING});
        builder.add(new Property[]{WRAPPED});
        builder.add(new Property[]{MatBlock.PREVIEW});
    }

    protected FluidState getFluidState(BlockState state) {
        return (Boolean)state.getValue(WATERLOGGED) != false ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    public Direction getFacing(BlockState state) {
        return (Direction)state.getValue(FACING);
    }

    public boolean isEmpty(BlockState state) {
        return (Boolean)state.getValueOrElse(EMPTY, Boolean.valueOf(true));
    }

    public void setEmpty(Level level, BlockPos pos, BlockState state, boolean empty) {
        level.setBlock(pos, (BlockState)state.setValue(EMPTY, Boolean.valueOf(empty)), 3);
    }

    public boolean isOpen(BlockState state) {
        return (Boolean)state.getValueOrElse(OPEN, Boolean.valueOf(true));
    }

    public void setOpen(Level level, BlockPos pos, BlockState state, boolean open) {
        level.setBlock(pos, (BlockState)state.setValue(OPEN, Boolean.valueOf(open)), 3);
    }

    public boolean isWrapped(BlockState state) {
        return (Boolean)state.getValueOrElse(WRAPPED, Boolean.valueOf(true));
    }

    public void setWrapped(Level level, BlockPos pos, BlockState state, boolean open) {
        level.setBlock(pos, (BlockState)state.setValue(WRAPPED, Boolean.valueOf(open)), 3);
    }

    public ItemStack getCardbox(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CardboxBlockEntity) {
            CardboxBlockEntity cardbox = (CardboxBlockEntity)blockEntity;
            return cardbox.getCardbox();
        }
        return ItemStack.EMPTY;
    }

    public void setCardbox(Level level, BlockPos pos, ItemStack stack) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CardboxBlockEntity) {
            CardboxBlockEntity cardbox = (CardboxBlockEntity)blockEntity;
            cardbox.setCardbox(stack);
        }
        BlockState state = level.getBlockState(pos);
        Item item = stack.getItem();
        if (item instanceof CardboxItem) {
            CardboxItem cardbox = (CardboxItem)item;
            this.setEmpty(level, pos, state, cardbox.isEmpty(stack));
            this.setWrapped(level, pos, state, cardbox.isWrapped(stack));
        } else {
            this.setEmpty(level, pos, state, false);
        }
    }

    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new CardboxBlockEntity(blockPos, blockState);
    }
}

