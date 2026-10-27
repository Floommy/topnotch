package net.togyk.tog_yks_cards.block.custom;

import net.minecraft.world.level.storage.loot.LootParams;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
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
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.togyk.tog_yks_cards.blockentity.entities.MatBlockEntity;
import net.togyk.tog_yks_cards.item.custom.MatPlaceable;
import net.togyk.tog_yks_cards.mat_item.MatItem;
import net.togyk.tog_yks_cards.mat_item.SimpleMatItem;
import net.togyk.tog_yks_cards.tag.ModTags;

public class MatBlock
extends BaseEntityBlock
implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty CONNECTED_N = BooleanProperty.create((String)"connected_n");
    public static final BooleanProperty CONNECTED_E = BooleanProperty.create((String)"connected_e");
    public static final BooleanProperty CONNECTED_S = BooleanProperty.create((String)"connected_s");
    public static final BooleanProperty CONNECTED_W = BooleanProperty.create((String)"connected_w");
    public static final BooleanProperty CONNECTED_NE = BooleanProperty.create((String)"connected_ne");
    public static final BooleanProperty CONNECTED_SE = BooleanProperty.create((String)"connected_nw");
    public static final BooleanProperty CONNECTED_NW = BooleanProperty.create((String)"connected_se");
    public static final BooleanProperty CONNECTED_SW = BooleanProperty.create((String)"connected_sw");
    public static final EnumProperty<LightLevel> LIGHT_LEVEL = EnumProperty.create((String)"light_level", LightLevel.class);
    public static final BooleanProperty PREVIEW = BooleanProperty.create((String)"preview");
    private static final VoxelShape SHAPE = Block.column((double)18.0, (double)-9.0, (double)1.0);
    private static final VoxelShape COLLISION_SHAPE = Block.column((double)16.0, (double)0.0, (double)1.0);
    private final String trimMaterial;

    public String getTrimMaterial() {
        return this.trimMaterial;
    }

    protected VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        ArrayList<VoxelShape> shapes = new ArrayList<VoxelShape>(List.of(SHAPE));
        shapes.addAll(this.getVoxelShapes(blockGetter, blockPos));
        return shapes.stream().reduce(Shapes.empty(), Shapes::or);
    }

    protected VoxelShape getCollisionShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        return COLLISION_SHAPE;
    }

    public MatBlock(BlockBehaviour.Properties properties, String trimMaterial) {
        super(properties.lightLevel(MatBlock::getLightLevel));
        this.trimMaterial = trimMaterial;
        this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)this.defaultBlockState().setValue(CONNECTED_N, Boolean.valueOf(false))).setValue(CONNECTED_E, Boolean.valueOf(false))).setValue(CONNECTED_S, Boolean.valueOf(false))).setValue(CONNECTED_W, Boolean.valueOf(false))).setValue(CONNECTED_NE, Boolean.valueOf(false))).setValue(CONNECTED_SE, Boolean.valueOf(false))).setValue(CONNECTED_SW, Boolean.valueOf(false))).setValue(CONNECTED_NW, Boolean.valueOf(false))).setValue(LIGHT_LEVEL, (LightLevel.ZERO)));
    }

    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult blockHitResult) {
        MatItem matItem = null;
        for (MatItem item : this.getAllMatItems((BlockGetter)level, pos)) {
            if (!item.isHit(blockHitResult.getLocation())) continue;
            matItem = item;
            break;
        }
        if (matItem != null) {
            InteractionResult result;
            if (matItem instanceof MenuProvider) {
                MenuProvider menu = (MenuProvider)matItem;
                if (!level.isClientSide() && player.isCrouching() && matItem.canOpenMenu(player)) {
                    player.openMenu(menu);
                    return InteractionResult.SUCCESS;
                }
            }
            if ((result = matItem.interact(stack, state, level, pos, player, hand, blockHitResult)) != InteractionResult.PASS) {
                matItem.getMat().setChanged();
                return result;
            }
        } else {
            Item item = stack.getItem();
            if (item instanceof MatPlaceable) {
                MatPlaceable placeable = (MatPlaceable)item;
                placeable.place(this, stack, state, level, pos, player, hand, blockHitResult);
                return InteractionResult.SUCCESS;
            }
            if (stack.is(ModTags.Items.DECORATIVE_MAT_ITEM) && item instanceof BlockItem) {
                BlockItem blockItem = (BlockItem)item;
                Vec3 relativeHit = blockHitResult.getLocation().subtract((double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
                SimpleMatItem placeable = new SimpleMatItem(blockItem, level, player, hand, blockHitResult, (int)(relativeHit.x * 16.0) - 8, (int)(relativeHit.z * 16.0) - 8, stack);
                placeable.setMat(this.getMat((BlockGetter)level, pos));
                if (this.getMatItems((BlockGetter)level, pos).stream().map(item1 -> item1.getAbsoluteShape().bounds()).noneMatch(box -> box.intersects(placeable.getAbsoluteShape().bounds()))) {
                    this.addMatItem((BlockGetter)level, pos, placeable);
                    if (!player.isCreative()) {
                        stack.shrink(1);
                    }
                    this.setChanged((BlockGetter)level, pos);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.FAIL;
    }

    public MatBlockEntity getMat(BlockGetter blockGetter, BlockPos blockPos) {
        BlockEntity blockEntity = blockGetter.getBlockEntity(blockPos);
        if (!(blockEntity instanceof MatBlockEntity)) {
            return null;
        }
        MatBlockEntity mat = (MatBlockEntity)blockEntity;
        return mat;
    }

    public List<MatItem> getMatItems(BlockGetter blockGetter, BlockPos blockPos) {
        MatBlockEntity matBlockEntity = this.getMat(blockGetter, blockPos);
        if (!(matBlockEntity instanceof MatBlockEntity)) {
            return new ArrayList<MatItem>();
        }
        MatBlockEntity mat = matBlockEntity;
        return mat.getMatItems();
    }

    public List<MatItem> getSupportedItems(BlockGetter blockGetter, BlockPos blockPos) {
        MatBlockEntity matBlockEntity = this.getMat(blockGetter, blockPos);
        if (!(matBlockEntity instanceof MatBlockEntity)) {
            return new ArrayList<MatItem>();
        }
        MatBlockEntity mat = matBlockEntity;
        return mat.getSupportedItems();
    }

    public List<MatItem> getAllMatItems(BlockGetter blockGetter, BlockPos blockPos) {
        MatBlockEntity matBlockEntity = this.getMat(blockGetter, blockPos);
        if (!(matBlockEntity instanceof MatBlockEntity)) {
            return new ArrayList<MatItem>();
        }
        MatBlockEntity mat = matBlockEntity;
        return mat.getAllMatItems();
    }

    public void setChanged(BlockGetter blockGetter, BlockPos blockPos) {
        MatBlockEntity matBlockEntity = this.getMat(blockGetter, blockPos);
        if (!(matBlockEntity instanceof MatBlockEntity)) {
            return;
        }
        MatBlockEntity mat = matBlockEntity;
        mat.setChanged();
    }

    public List<VoxelShape> getVoxelShapes(BlockGetter blockGetter, BlockPos blockPos) {
        MatBlockEntity matBlockEntity = this.getMat(blockGetter, blockPos);
        if (!(matBlockEntity instanceof MatBlockEntity)) {
            return new ArrayList<VoxelShape>();
        }
        MatBlockEntity mat = matBlockEntity;
        return mat.getVoxelShapes();
    }

    public void setMatItems(BlockGetter blockGetter, BlockPos blockPos, List<MatItem> items) {
        MatBlockEntity matBlockEntity = this.getMat(blockGetter, blockPos);
        if (!(matBlockEntity instanceof MatBlockEntity)) {
            return;
        }
        MatBlockEntity mat = matBlockEntity;
        mat.setMatItems(items);
    }

    public void removeItem(BlockGetter blockGetter, BlockPos blockPos, MatItem item) {
        MatBlockEntity matBlockEntity = this.getMat(blockGetter, blockPos);
        if (!(matBlockEntity instanceof MatBlockEntity)) {
            return;
        }
        MatBlockEntity mat = matBlockEntity;
        mat.removeItem(item);
    }

    public void addMatItem(BlockGetter blockGetter, BlockPos blockPos, MatItem item) {
        MatBlockEntity matBlockEntity = this.getMat(blockGetter, blockPos);
        if (!(matBlockEntity instanceof MatBlockEntity)) {
            return;
        }
        MatBlockEntity mat = matBlockEntity;
        mat.addMatItem(item);
    }

    public boolean breakItem(Level level, Player player, BlockPos pos, BlockState state, MatBlockEntity mat) {
        BlockHitResult hit = (BlockHitResult)player.pick(5.0, 0.0f, false);
        MatItem target = null;
        for (MatItem item : mat.getAllMatItems()) {
            if (!item.isHit(hit.getLocation())) continue;
            target = item;
            break;
        }
        if (target != null) {
            target.getMat().removeItem(target);
            if (!player.preventsBlockDrops() && !level.isClientSide()) {
                for (ItemStack drop : target.getDrops()) {
                    MatBlock.popResource((Level)level, (BlockPos)pos, (ItemStack)drop);
                }
            }
            target.getMat().setChanged();
            return true;
        }
        return false;
    }

    protected List<ItemStack> getDrops(BlockState state,
     LootParams.Builder builder) {
        List<ItemStack> drops = super.getDrops(state, builder);
        if (builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof MatBlockEntity mat) {
            for (MatItem matItem : mat.getMatItems()) {
                drops.addAll(matItem.getDrops());
            }
        }
        return drops;
    }

    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MatBlockEntity(pos, state);
    }

    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        return this.updateConnectedState((BlockState)this.defaultBlockState().setValue(WATERLOGGED, Boolean.valueOf(fluidState.getType() == Fluids.WATER)), (BlockGetter)context.getLevel(), context.getClickedPos());
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{WATERLOGGED, CONNECTED_N, CONNECTED_E, CONNECTED_S, CONNECTED_W, CONNECTED_NE, CONNECTED_SE, CONNECTED_NW, CONNECTED_SW, LIGHT_LEVEL});
    }

    protected FluidState getFluidState(BlockState blockState) {
        return (Boolean)blockState.getValue(WATERLOGGED) != false ? Fluids.WATER.getSource(false) : super.getFluidState(blockState);
    }

    public BlockState updateConnectedState(BlockState state, BlockGetter level, BlockPos pos) {
        return (BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)state.setValue(CONNECTED_N, Boolean.valueOf(level.getBlockState(pos.north()).getBlock() instanceof MatBlock))).setValue(CONNECTED_E, Boolean.valueOf(level.getBlockState(pos.east()).getBlock() instanceof MatBlock))).setValue(CONNECTED_S, Boolean.valueOf(level.getBlockState(pos.south()).getBlock() instanceof MatBlock))).setValue(CONNECTED_W, Boolean.valueOf(level.getBlockState(pos.west()).getBlock() instanceof MatBlock))).setValue(CONNECTED_NE, Boolean.valueOf(level.getBlockState(pos.north().east()).getBlock() instanceof MatBlock))).setValue(CONNECTED_SE, Boolean.valueOf(level.getBlockState(pos.north().west()).getBlock() instanceof MatBlock))).setValue(CONNECTED_NW, Boolean.valueOf(level.getBlockState(pos.south().east()).getBlock() instanceof MatBlock))).setValue(CONNECTED_SW, Boolean.valueOf(level.getBlockState(pos.south().west()).getBlock() instanceof MatBlock));
    }

    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, Orientation orientation, boolean bl) {
        super.neighborChanged(state, level, pos, block, orientation, bl);
        level.setBlock(pos, this.updateConnectedState(state, (BlockGetter)level, pos), 3);
        for (MatItem item : this.getMatItems((BlockGetter)level, pos)) {
            if (item.isFullySupported()) continue;
            item.getMat().removeItem(item);
            for (ItemStack drop : item.getDrops()) {
                MatBlock.popResource((Level)level, (BlockPos)pos, (ItemStack)drop);
            }
            item.getMat().setChanged();
        }
    }

    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState blockState2, boolean bl) {
        super.onPlace(state, level, pos, blockState2, bl);
        for (BlockPos blockPos : List.of(pos.north().east(), pos.north().west(), pos.south().east(), pos.south().west())) {
            BlockState blockState = level.getBlockState(blockPos);
            Block block = blockState.getBlock();
            if (!(block instanceof MatBlock)) continue;
            MatBlock block2 = (MatBlock)block;
            block2.neighborChanged(blockState, level, blockPos, (Block)this, null, false);
        }
    }

    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean bl) {
        super.affectNeighborsAfterRemoval(state, level, pos, bl);
        for (BlockPos blockPos : List.of(pos.north().east(), pos.north().west(), pos.south().east(), pos.south().west())) {
            BlockState blockState = level.getBlockState(blockPos);
            Block block = blockState.getBlock();
            if (!(block instanceof MatBlock)) continue;
            MatBlock block2 = (MatBlock)block;
            block2.neighborChanged(blockState, (Level)level, blockPos, (Block)this, null, bl);
        }
    }

    protected boolean canSurvive(BlockState state, LevelReader levelReader, BlockPos blockPos) {
        return !levelReader.isEmptyBlock(blockPos.below());
    }

    public static int getLightLevel(BlockState state) {
        return ((LightLevel)(state.getValue(LIGHT_LEVEL))).getLightLevel();
    }

    public static enum LightLevel implements StringRepresentable
    {
        ZERO("0", 0),
        ONE("1", 1),
        TWO("2", 2),
        THREE("3", 3),
        FOUR("4", 4),
        FIVE("5", 5),
        SIX("6", 6),
        SEVEN("7", 7),
        EIGHT("8", 8),
        NINE("9", 9),
        TEN("10", 10),
        ELEVEN("11", 11),
        TWELVE("12", 12),
        THIRTEEN("13", 13),
        FOURTEEN("14", 14),
        FIFTEEN("15", 15);

        private final String name;
        private final int lightLevel;

        private LightLevel(String name, int lightLevel) {
            this.name = name;
            this.lightLevel = lightLevel;
        }

        public String getSerializedName() {
            return this.name;
        }

        public int getLightLevel() {
            return this.lightLevel;
        }

        public static LightLevel fromInt(int lightLevel) {
            return switch (lightLevel) {
                case 0 -> ZERO;
                case 1 -> ONE;
                case 2 -> TWO;
                case 3 -> THREE;
                case 4 -> FOUR;
                case 5 -> FIVE;
                case 6 -> SIX;
                case 7 -> SEVEN;
                case 8 -> EIGHT;
                case 9 -> NINE;
                case 10 -> TEN;
                case 11 -> ELEVEN;
                case 12 -> TWELVE;
                case 13 -> THIRTEEN;
                case 14 -> FOURTEEN;
                case 15 -> FIFTEEN;
                default -> throw new IllegalArgumentException("Invalid light level: " + lightLevel);
            };
        }
    }
}

