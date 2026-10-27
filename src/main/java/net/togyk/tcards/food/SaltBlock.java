package net.togyk.tcards.food;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

/** Salt block (falls like sand). From the Better McDonald's Mod by Kyronis (GPL-3.0). */
public class SaltBlock extends FallingBlock {
    public SaltBlock(BlockBehaviour.Properties properties) {
        super(properties.mapColor(DyeColor.WHITE).instrument(NoteBlockInstrument.SNARE).strength(0.5f).sound(SoundType.SAND));
    }

    @Override
    public int getDustColor(BlockState state, BlockGetter level, BlockPos pos) {
        return 0;
    }
}
