package net.togyk.tcards.shuffler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.togyk.tcards.TableSounds;
import net.togyk.tog_yks_cards.mat_item.SimpleMatItem;

/** Little service bell for the table: "ding!" to call the dealer. Gives a short redstone pulse too. */
public class DealerBellBlock extends Block {
    public static final BooleanProperty PRESSED = BooleanProperty.create("pressed");
    private static final VoxelShape SHAPE = Block.column(7, 0, 5.6);
    private static final int PRESS_TICKS = 6;

    public DealerBellBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PRESSED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PRESSED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(PRESSED)) return InteractionResult.SUCCESS;   // no spamming
        if (level instanceof ServerLevel server) ring(server, pos, state);
        return InteractionResult.SUCCESS;
    }

    public static void ring(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(PRESSED, true), Block.UPDATE_ALL);
        level.scheduleTick(pos, state.getBlock(), PRESS_TICKS);
        level.playSound(null, pos, TableSounds.BELL_DING, SoundSource.BLOCKS, 1.0f, 0.97f + level.getRandom().nextFloat() * 0.06f);
        // note particle: speed x = colour (0..1)
        level.sendParticles(ParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 0, level.getRandom().nextFloat(), 0, 0, 1);
    }

    // ---------------------------------------------------------------- bell standing on a game mat

    private static final java.util.Map<SimpleMatItem, Integer> PRESSED_ON_MAT = new java.util.IdentityHashMap<>();

    public static void init() {
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (PRESSED_ON_MAT.isEmpty()) return;
            java.util.Iterator<java.util.Map.Entry<SimpleMatItem, Integer>> it = PRESSED_ON_MAT.entrySet().iterator();
            while (it.hasNext()) {
                java.util.Map.Entry<SimpleMatItem, Integer> e = it.next();
                if (e.getValue() > 1) {
                    e.setValue(e.getValue() - 1);
                    continue;
                }
                SimpleMatItem bell = e.getKey();
                it.remove();
                if (bell.getState().hasProperty(PRESSED)) {
                    bell.setState(bell.getState().setValue(PRESSED, false));
                    bell.setChanged();
                }
            }
        });
    }

    /** Right click on a bell lying on a mat (the mat routes clicks to its items). */
    public static void ringOnMat(SimpleMatItem bell, Level level) {
        if (!(level instanceof ServerLevel server) || bell.getMat() == null) return;
        BlockState state = bell.getState();
        if (!state.hasProperty(PRESSED) || state.getValue(PRESSED) && PRESSED_ON_MAT.containsKey(bell)) return;
        bell.setState(state.setValue(PRESSED, true));
        bell.setChanged();
        PRESSED_ON_MAT.put(bell, PRESS_TICKS);
        BlockPos mat = bell.getMat().getBlockPos();
        double x = mat.getX() + 0.5 + bell.getX() / 16.0, y = mat.getY() + 0.06, z = mat.getZ() + 0.5 + bell.getZ() / 16.0;
        server.playSound(null, x, y, z, TableSounds.BELL_DING, SoundSource.BLOCKS, 1.0f, 0.97f + server.getRandom().nextFloat() * 0.06f);
        server.sendParticles(ParticleTypes.NOTE, x, y + 0.45, z, 0, server.getRandom().nextFloat(), 0, 0, 1);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(PRESSED)) level.setBlock(pos, state.setValue(PRESSED, false), Block.UPDATE_ALL);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(PRESSED) ? 15 : 0;
    }
}
