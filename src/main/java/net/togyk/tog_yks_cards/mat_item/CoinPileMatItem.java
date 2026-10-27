package net.togyk.tog_yks_cards.mat_item;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.togyk.tog_yks_cards.blockentity.entities.CoinPileBlockEntity;
import net.togyk.tog_yks_cards.item.custom.CoinItem;
import net.togyk.tog_yks_cards.mat_item.MatItem;
import net.togyk.tog_yks_cards.mat_item.MatItemType;

public class CoinPileMatItem
extends MatItem
implements Container {
    public static MapCodec<CoinPileMatItem> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(BlockState.CODEC.fieldOf("state").forGetter(MatItem::getState), Codec.INT.fieldOf("x").forGetter(MatItem::getX), Codec.INT.fieldOf("z").forGetter(MatItem::getZ), CoinPileBlockEntity.COINS_CODEC.fieldOf("coins").forGetter(CoinPileMatItem::getCoins)).apply(instance, CoinPileMatItem::new));
    private List<Pair<ItemStack, Float>> coins;

    public CoinPileMatItem(BlockState state, int x, int z, List<Pair<ItemStack, Float>> coins) {
        super(state, x, z);
        this.setCoinsAndRotation(coins);
        this.updateState();
    }

    public CoinPileMatItem(BlockState state, int x, int z, ItemStack coin) {
        this(state, x, z, List.of(new Pair(coin, Float.valueOf((float)new Random().nextInt(0, 16) * 22.5f))));
    }

    @Override
    public MatItemType getType() {
        return MatItemType.COIN_PILE;
    }

    @Override
    public VoxelShape getShape() {
        ArrayList<VoxelShape> shapes = new ArrayList<VoxelShape>();
        int y = 0;
        for (Pair<ItemStack, Float> tuple : this.getCoins()) {
            Item item = ((ItemStack)tuple.getFirst()).getItem();
            if (!(item instanceof CoinItem)) continue;
            CoinItem coin = (CoinItem)item;
            shapes.add(Block.column((double)coin.getWidth(), (double)y, (double)(y + coin.getHeight())));
            y += coin.getHeight();
        }
        return shapes.stream().reduce(Shapes.empty(), Shapes::or);
    }

    @Override
    public List<ItemStack> getDrops() {
        return this.getCoins().stream().map(Pair::getFirst).toList();
    }

    public List<Pair<ItemStack, Float>> getCoins() {
        return this.coins;
    }

    public void remove(Level level, BlockPos pos) {
        level.removeBlock(pos, false);
    }

    public void setCoins(List<ItemStack> coins) {
        this.setCoinsAndRotation(coins.stream().map(coin -> new Pair<ItemStack, Float>(coin, Float.valueOf((float)new Random().nextInt(0, 16) * 22.5f))).toList());
    }

    public void setCoinsAndRotation(List<Pair<ItemStack, Float>> coins) {
        coins.forEach(stack -> ((ItemStack)stack.getFirst()).setCount(1));
        this.coins = new ArrayList<Pair<ItemStack, Float>>(coins);
        this.setChanged();
    }

    public Pair<ItemStack, Float> getCoin() {
        return this.coins.isEmpty() ? new Pair(ItemStack.EMPTY, Float.valueOf(0.0f)) : this.coins.getLast();
    }

    public void addCoin(ItemStack coin) {
        coin.setCount(1);
        this.coins.add((Pair<ItemStack, Float>)new Pair(coin, Float.valueOf((float)new Random().nextInt(0, 16) * 22.5f)));
        this.setChanged();
    }

    public void removeLastCoin() {
        this.coins.removeLast();
        this.setChanged();
    }

    public int getContainerSize() {
        return this.coins.size();
    }

    public boolean isEmpty() {
        return this.coins.isEmpty();
    }

    public ItemStack getItem(int slot) {
        if (slot < 0 || slot >= this.coins.size()) {
            return ItemStack.EMPTY;
        }
        return (ItemStack)this.coins.get(slot).getFirst();
    }

    public ItemStack removeItem(int slot, int amount) {
        if (slot < 0 || slot >= this.coins.size()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = (ItemStack)this.coins.get(slot).getFirst();
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = stack.split(amount);
        if (stack.isEmpty()) {
            this.coins.remove(slot);
        }
        this.setChanged();
        return result;
    }

    public ItemStack removeItemNoUpdate(int slot) {
        if (slot < 0 || slot >= this.coins.size()) {
            return ItemStack.EMPTY;
        }
        return (ItemStack)this.coins.remove(slot).getFirst();
    }

    public void setItem(int slot, ItemStack stack) {
        stack.setCount(1);
        if (this.coins.size() <= slot) {
            return;
        }
        this.coins.set(slot, (Pair<ItemStack, Float>)new Pair(stack, Float.valueOf((float)new Random().nextInt(0, 16) * 22.5f)));
        this.setChanged();
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (this.coins.isEmpty() && this.mat != null) {
            this.mat.removeItem(this);
        }
    }

    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity((BlockEntity)this.mat, (Player)player);
    }

    public void clearContent() {
        this.coins.clear();
        this.setChanged();
    }

    @Override
    public boolean canOpenMenu(Player player) {
        return !this.isEmpty();
    }

    @Override
    public InteractionResult interact(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult blockHitResult) {
        if (!level.isClientSide() && hand.equals(InteractionHand.MAIN_HAND)) {
            if (!stack.isEmpty()) {
                if (this.getCoins().size() < 15 && stack.getItem() instanceof CoinItem) {
                    level.playSound((Entity)player, player.getOnPos(), SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 1.0f, 1.0f);
                    if (!level.isClientSide()) {
                        ItemStack coinStack = stack.copy();
                        coinStack.setCount(1);
                        this.addCoin(coinStack);
                        stack.shrink(1);
                        return InteractionResult.SUCCESS;
                    }
                }
            } else if (!((ItemStack)this.getCoin().getFirst()).isEmpty()) {
                ItemStack coin = ((ItemStack)this.getCoin().getFirst()).copy();
                this.removeLastCoin();
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

    @Override
    public CoinPileMatItem getRenderState(Player player) {
        return new CoinPileMatItem(this.getState(), this.getX(), this.getZ(), this.getCoins());
    }
}

