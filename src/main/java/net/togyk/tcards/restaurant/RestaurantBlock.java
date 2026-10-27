package net.togyk.tcards.restaurant;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.togyk.tcards.restaurant.RestaurantBlockEntity.Kind;

/** Register, kitchen screen and pickup screen (1 block each). The kiosk extends this with a second half. */
public class RestaurantBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public final Kind kind;
    private final Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);

    public RestaurantBlock(BlockBehaviour.Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
        for (Direction d : Direction.Plane.HORIZONTAL) shapes.put(d, makeShape(kind, d));
    }

    /** Shape box given for "facing north" (front toward -z), rotated for the other directions. */
    private static VoxelShape makeShape(Kind kind, Direction d) {
        double[] b = switch (kind) {
            case REGISTER -> new double[]{2, 0, 3, 14, 10, 14};
            case KITCHEN, PICKUP, PREPARING -> new double[]{0, 0, 13, 16, 16, 16};
            case KIOSK -> new double[]{3, 0, 3, 13, 16, 13};
        };
        return rotate(b, d);
    }

    static VoxelShape rotate(double[] b, Direction d) {
        double x0 = b[0], y0 = b[1], z0 = b[2], x1 = b[3], y1 = b[4], z1 = b[5];
        return switch (d) {
            case SOUTH -> Block.box(16 - x1, y0, 16 - z1, 16 - x0, y1, 16 - z0);
            case WEST -> Block.box(z0, y0, 16 - x1, z1, y1, 16 - x0);
            case EAST -> Block.box(16 - z1, y0, x0, 16 - z0, y1, x1);
            default -> Block.box(x0, y0, z0, x1, y1, z1);
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape s = shapes.get(state.getValue(FACING));
        return s == null ? Shapes.block() : s;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RestaurantBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, RestaurantRegistry.RESTAURANT_BE, RestaurantBlockEntity::serverTick);
    }

    /** The block entity for a click on this block (the kiosk overrides this for its upper half). */
    public RestaurantBlockEntity entity(BlockGetter level, BlockPos pos, BlockState state) {
        return level.getBlockEntity(pos) instanceof RestaurantBlockEntity be ? be : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer instanceof ServerPlayer p) {
            RestaurantBlockEntity be = entity(level, pos, state);
            if (be != null) RestaurantRegistry.setUpPlaced(p, be);
        }
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        RestaurantBlockEntity be = entity(level, pos, state);
        if (be != null && !be.canManage(player)) return 0.0f;
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide() || !(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        RestaurantBlockEntity be = entity(level, pos, state);
        if (be == null) return InteractionResult.SUCCESS;
        if (RestaurantRegistry.cancelLinking(sp)) return InteractionResult.SUCCESS;
        if (sp.isShiftKeyDown() && be.canManage(sp)) {
            RestaurantRegistry.startJoin(sp, be);
            return InteractionResult.SUCCESS;
        }
        switch (be.kind()) {
            case KIOSK -> {
                if (be.isPrinting()) be.stretch();
                else RestaurantRegistry.sendState(sp, be, true, 0, 0, "");
            }
            case REGISTER -> RestaurantRegistry.sendState(sp, be, true, 0, 0, "");
            default -> {
                if (be.restaurant() == null) {
                    sp.sendOverlayMessage(Component.literal("§eNot linked: sneak + right-click this screen, then click a register"));
                } else {
                    sp.sendOverlayMessage(Component.literal("§7" + be.restaurant().name));
                }
            }
        }
        return InteractionResult.SUCCESS;
    }
}
