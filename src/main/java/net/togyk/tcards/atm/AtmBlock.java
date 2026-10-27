package net.togyk.tcards.atm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Two blocks tall, like a door: the lower half holds the block entity (vault, owner, link),
 * the upper half is the screen and forwards everything to the lower half.
 */
public class AtmBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    public AtmBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF);
    }

    /** Position of the lower half for either half. */
    public static BlockPos lowerPos(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
    }

    /** The ATM block entity for a click on either half (null if the lower half is missing). */
    public static AtmBlockEntity atmAt(BlockGetter level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof AtmBlock)) return null;
        return level.getBlockEntity(lowerPos(pos, state)) instanceof AtmBlockEntity be ? be : null;
    }

    /** The ATM block entity for a position of either half (null if it is no ATM). */
    public static AtmBlockEntity entity(BlockGetter level, BlockPos pos) {
        return atmAt(level, pos, level.getBlockState(pos));
    }

    private static final net.minecraft.world.phys.shapes.VoxelShape SHAPE = Block.box(1, 0, 1, 15, 16, 15);

    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                                                  net.minecraft.world.phys.shapes.CollisionContext context) {
        return SHAPE;
    }

    /** Places a complete ATM (both halves) - used by tests and commands. */
    public static void place(Level level, BlockPos lower, Direction facing) {
        BlockState s = AtmRegistry.ATM.defaultBlockState().setValue(FACING, facing);
        level.setBlock(lower, s.setValue(HALF, DoubleBlockHalf.LOWER), Block.UPDATE_ALL);
        level.setBlock(lower.above(), s.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() >= level.getMaxY() || !level.getBlockState(pos.above()).canBeReplaced(context)) return null;
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(HALF, DoubleBlockHalf.LOWER);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
        if (!level.isClientSide() && placer instanceof Player p && level.getBlockEntity(pos) instanceof AtmBlockEntity be) {
            be.setOwner(p);
        }
    }

    /** One half without the other disappears (like a door). */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        DoubleBlockHalf half = state.getValue(HALF);
        if (direction.getAxis() == Direction.Axis.Y && (half == DoubleBlockHalf.LOWER) == (direction == Direction.UP)) {
            boolean partner = neighbor.is(this) && neighbor.getValue(HALF) != half && neighbor.getValue(FACING) == state.getValue(FACING);
            return partner ? state : Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random);
    }

    /** Breaking the top in creative or without a pickaxe must not drop the ATM from the bottom half. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && state.getValue(HALF) == DoubleBlockHalf.UPPER
                && (player.preventsBlockDrops() || !player.hasCorrectToolForDrops(state))) {
            BlockPos below = pos.below();
            BlockState lower = level.getBlockState(below);
            if (lower.is(this) && lower.getValue(HALF) == DoubleBlockHalf.LOWER) {
                level.setBlock(below, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                level.levelEvent(player, 2001, below, Block.getId(lower));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** Only the owner and ops can mine it; for everybody else it is as hard as bedrock. */
    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        AtmBlockEntity be = atmAt(level, pos, state);
        if (be != null && !be.canManage(player)) return 0.0f;
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new AtmBlockEntity(pos, state) : null;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || state.getValue(HALF) != DoubleBlockHalf.LOWER) return null;
        return createTickerHelper(type, AtmRegistry.ATM_BE, AtmBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        AtmBlockEntity be = atmAt(level, pos, state);
        if (be != null && player instanceof ServerPlayer sp) {
            if (AtmRegistry.cancelLinking(sp)) return InteractionResult.SUCCESS;
            be.sendState(sp, true, AtmPayloads.RESULT_NONE, "", 0, 0, "");
        }
        return InteractionResult.SUCCESS;
    }
}
