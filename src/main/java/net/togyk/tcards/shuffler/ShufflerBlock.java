package net.togyk.tcards.shuffler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ShufflerBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<Status> STATUS = EnumProperty.create("status", Status.class);
    public static final BooleanProperty SHUFFLING = BooleanProperty.create("shuffling");
    public static final BooleanProperty FILLED = BooleanProperty.create("filled");
    public static final EnumProperty<Chute> CHUTE = EnumProperty.create("chute", Chute.class);

    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 10, 15);

    public enum Status implements StringRepresentable {
        OFF("off"), READY("ready"), LOW("low"), EMPTY("empty"), OUT("out");

        private final String name;

        Status(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public enum Chute implements StringRepresentable {
        NONE("none"), CARDS("cards"), PILE("pile"), DEALING("dealing");

        private final String name;

        Chute(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public ShufflerBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(STATUS, Status.OFF)
                .setValue(SHUFFLING, false)
                .setValue(FILLED, false)
                .setValue(CHUTE, Chute.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, STATUS, SHUFFLING, FILLED, CHUTE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShufflerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, ShufflerRegistry.SHUFFLER_BE, ShufflerBlockEntity::serverTick);
    }

    /** Empty hand: table modes hand out a pile / one card; sorter mode or sneaking opens the menu. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof ShufflerBlockEntity be && player instanceof ServerPlayer sp) {
            if (be.getMode() != ShufflerBlockEntity.SORTER && !player.isShiftKeyDown()) {
                be.dispense(sp);
            } else {
                be.sendState(sp, true, "");
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Cards, piles and boxes go into the machine. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!ShufflerBlockEntity.isInsertable(stack)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof ShufflerBlockEntity be) {
            be.insertFrom(player, hand, stack);
        }
        return InteractionResult.SUCCESS;
    }
}
