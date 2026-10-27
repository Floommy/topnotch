package net.togyk.tog_yks_cards.block.custom;

import net.minecraft.world.level.storage.loot.LootParams;

import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.togyk.tog_yks_cards.block.custom.MatBlock;
import net.togyk.tog_yks_cards.blockentity.entities.CoinPileBlockEntity;
import net.togyk.tog_yks_cards.item.custom.CoinItem;

public class CoinPileblock
extends Block
implements SimpleWaterloggedBlock,
EntityBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public CoinPileblock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)this.defaultBlockState().setValue(MatBlock.PREVIEW, Boolean.valueOf(false)));
    }

    protected VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        ArrayList<VoxelShape> shapes = new ArrayList<VoxelShape>();
        int y = 0;
        for (Pair<ItemStack, Float> Pair2 : this.getCoins(blockGetter, blockPos)) {
            Item item = ((ItemStack)Pair2.getFirst()).getItem();
            if (!(item instanceof CoinItem)) continue;
            CoinItem coin = (CoinItem)item;
            shapes.add(Block.column((double)coin.getWidth(), (double)y, (double)(y + coin.getHeight())));
            y += coin.getHeight();
        }
        return shapes.stream().reduce(Shapes.empty(), Shapes::or);
    }

    public Item asItem() {
        return Items.AIR;
    }

    protected boolean skipRendering(BlockState blockState, BlockState blockState2, Direction direction) {
        return true;
    }

    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CoinPileBlockEntity(pos, state);
    }

    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult blockHitResult) {
        if (!level.isClientSide() && hand.equals(InteractionHand.MAIN_HAND)) {
            if (!stack.isEmpty()) {
                CoinItem coinItem;
                int currentHeight = 0;
                for (Pair<ItemStack, Float> Pair22 : this.getCoins((BlockGetter)level, pos)) {
                    Item item = ((ItemStack)Pair22.getFirst()).getItem();
                    if (!(item instanceof CoinItem)) continue;
                    CoinItem coin = (CoinItem)item;
                    currentHeight += coin.getHeight();
                }
                if (stack.getItem() instanceof CoinItem heldCoin && currentHeight + (coinItem = heldCoin).getHeight() <= 16) {
                    level.playSound((Entity)player, player.getOnPos(), SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 1.0f, 1.0f);
                    if (!level.isClientSide()) {
                        ItemStack coinStack = stack.copy();
                        coinStack.setCount(1);
                        this.addCoin((BlockGetter)level, pos, coinStack);
                        stack.shrink(1);
                        return InteractionResult.SUCCESS;
                    }
                }
                return InteractionResult.PASS;
            }
            if (!((ItemStack)this.getCoin((BlockGetter)level, pos).getFirst()).isEmpty()) {
                ItemStack coin = ((ItemStack)this.getCoin((BlockGetter)level, pos).getFirst()).copy();
                this.removeLastCoin(level, pos);
                if (stack.isEmpty()) {
                    player.setItemInHand(hand, coin);
                } else if (!player.addItem(coin)) {
                    player.drop(coin, false, Prediction.PREDICTED);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    protected List<ItemStack> getDrops(BlockState state,
     LootParams.Builder builder) {
        List drops = super.getDrops(state, builder);
        Object object = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (object instanceof CoinPileBlockEntity) {
            CoinPileBlockEntity coinPile = (CoinPileBlockEntity)(object);
            drops.addAll(coinPile.getCoins().stream().map(Pair::getFirst).toList());
        }
        return drops;
    }

    public List<Pair<ItemStack, Float>> getCoins(BlockGetter level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CoinPileBlockEntity) {
            CoinPileBlockEntity coinPile = (CoinPileBlockEntity)blockEntity;
            return coinPile.getCoins();
        }
        return new ArrayList<Pair<ItemStack, Float>>();
    }

    public void setCoins(BlockGetter level, BlockPos pos, List<ItemStack> coins) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CoinPileBlockEntity) {
            CoinPileBlockEntity coinPile = (CoinPileBlockEntity)blockEntity;
            coinPile.setCoins(coins);
        }
    }

    public Pair<ItemStack, Float> getCoin(BlockGetter level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CoinPileBlockEntity) {
            CoinPileBlockEntity coinPile = (CoinPileBlockEntity)blockEntity;
            return coinPile.getCoin();
        }
        return new Pair(ItemStack.EMPTY, Float.valueOf(0.0f));
    }

    public void addCoin(BlockGetter level, BlockPos pos, ItemStack coin) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CoinPileBlockEntity) {
            CoinPileBlockEntity coinPile = (CoinPileBlockEntity)blockEntity;
            coinPile.addCoin(coin);
        }
    }

    public void addCoins(BlockGetter level, BlockPos pos, List<ItemStack> coins) {
        for (ItemStack coin : coins) {
            this.addCoin(level, pos, coin);
        }
    }

    public void removeLastCoin(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CoinPileBlockEntity) {
            CoinPileBlockEntity coinPile = (CoinPileBlockEntity)blockEntity;
            coinPile.removeLastCoin();
        }
    }

    protected boolean canSurvive(BlockState blockState, LevelReader levelReader, BlockPos blockPos) {
        return levelReader.getBlockState(blockPos.below()).isSolid();
    }

    public BlockState getStateForPlacement(BlockPlaceContext blockPlaceContext) {
        FluidState fluidState = blockPlaceContext.getLevel().getFluidState(blockPlaceContext.getClickedPos());
        return (BlockState)this.defaultBlockState().setValue(WATERLOGGED, Boolean.valueOf(fluidState.getType() == Fluids.WATER));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{WATERLOGGED});
        builder.add(new Property[]{MatBlock.PREVIEW});
    }

    protected FluidState getFluidState(BlockState blockState) {
        return (Boolean)blockState.getValue(WATERLOGGED) != false ? Fluids.WATER.getSource(false) : super.getFluidState(blockState);
    }
}

