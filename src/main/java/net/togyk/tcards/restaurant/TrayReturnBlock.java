package net.togyk.tcards.restaurant;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Tray rack (German "Tablettwagen"): a steel cart with 12 rails in a wooden niche, two blocks tall.
 * Empty trays slide in from the top rail down; a tray with leftovers is refused (empty it at the waste station first).
 * Empty hand takes the last tray out again. Breaking it drops the trays.
 */
public class TrayReturnBlock extends Block {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final int MAX = 12;
    public static final IntegerProperty TRAYS = IntegerProperty.create("trays", 0, MAX);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    public TrayReturnBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(HALF, DoubleBlockHalf.LOWER).setValue(TRAYS, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF, TRAYS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() >= level.getMaxY() || !level.getBlockState(pos.above()).canBeReplaced(context)) return null;
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        DoubleBlockHalf half = state.getValue(HALF);
        if (direction.getAxis() == Direction.Axis.Y && (half == DoubleBlockHalf.LOWER) == (direction == Direction.UP)) {
            boolean partner = neighbor.is(this) && neighbor.getValue(HALF) != half && neighbor.getValue(FACING) == state.getValue(FACING);
            return partner ? state.setValue(TRAYS, neighbor.getValue(TRAYS)) : Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random);
    }

    /** Breaking the top must not also drop the rack from the bottom half in creative / without drops. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockPos below = pos.below();
            BlockState lower = level.getBlockState(below);
            if (lower.is(this) && lower.getValue(HALF) == DoubleBlockHalf.LOWER) {
                if (player.preventsBlockDrops()) {
                    level.setBlock(below, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                } else {
                    // drop what the lower half would drop (rack + trays), then remove it quietly
                    Block.dropResources(lower, level, below, null, player, player.getMainHandItem());
                    level.setBlock(below, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                }
                level.levelEvent(player, 2001, below, Block.getId(lower));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** Both halves always show the same number of trays. */
    private static void setTrays(Level level, BlockPos pos, BlockState state, int n) {
        BlockPos lower = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        BlockState l = level.getBlockState(lower), u = level.getBlockState(lower.above());
        if (l.getBlock() instanceof TrayReturnBlock) level.setBlock(lower, l.setValue(TRAYS, n), Block.UPDATE_ALL);
        if (u.getBlock() instanceof TrayReturnBlock) level.setBlock(lower.above(), u.setValue(TRAYS, n), Block.UPDATE_ALL);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(RestaurantRegistry.TRAY.asItem())) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItemCopyStream().findAny().isPresent()) {
            player.sendOverlayMessage(Component.literal("§ePlease empty your tray at the waste station first"));
            level.playSound(null, pos, SoundEvents.CHAIN_HIT, SoundSource.BLOCKS, 0.5f, 0.8f);
            return InteractionResult.SUCCESS;
        }
        int n = state.getValue(TRAYS);
        if (n >= MAX) {
            player.sendOverlayMessage(Component.literal("§eThe tray rack is full"));
            return InteractionResult.SUCCESS;
        }
        if (!player.getAbilities().instabuild) stack.shrink(1);
        setTrays(level, pos, state, n + 1);
        player.sendOverlayMessage(Component.literal("§7Thank you!"));
        level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.6f, 1.6f);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        int n = state.getValue(TRAYS);
        if (n == 0) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        ItemStack tray = new ItemStack(RestaurantRegistry.TRAY.asItem());
        if (!player.getInventory().add(tray)) player.spawnAtLocation((ServerLevel) level, tray);
        setTrays(level, pos, state, n - 1);
        level.playSound(null, pos, SoundEvents.CHAIN_HIT, SoundSource.BLOCKS, 0.6f, 1.4f);
        return InteractionResult.SUCCESS;
    }
}
