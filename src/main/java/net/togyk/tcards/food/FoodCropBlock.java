package net.togyk.tcards.food;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Tomato / lettuce crop, 4 stages. From the Better McDonald's Mod by Kyronis (GPL-3.0). */
public class FoodCropBlock extends CropBlock {
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    private final VoxelShape[] shapes;
    private final Supplier<ItemLike> seeds;

    public FoodCropBlock(BlockBehaviour.Properties properties, int stepHeight, Supplier<ItemLike> seeds) {
        super(properties.mapColor(MapColor.PLANT).noCollision().randomTicks().instabreak().sound(SoundType.CROP)
                .pushReaction(PushReaction.POPPED).noOcclusion());
        this.seeds = seeds;
        shapes = new VoxelShape[4];
        for (int i = 0; i < 4; i++) shapes[i] = Block.box(0, 0, 0, 16, stepHeight * (i + 1), 16);
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return seeds.get();
    }

    @Override
    protected IntegerProperty getAgeProperty() {
        return AGE;
    }

    @Override
    public int getMaxAge() {
        return 3;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes[state.getValue(AGE)];
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }
}
