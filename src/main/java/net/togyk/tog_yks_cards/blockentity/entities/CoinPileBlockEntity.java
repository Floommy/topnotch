package net.togyk.tog_yks_cards.blockentity.entities;

import net.minecraft.core.HolderLookup;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.togyk.tog_yks_cards.blockentity.ModBlockEntities;

public class CoinPileBlockEntity
extends BlockEntity
implements Container {
    public static Codec<List<Pair<ItemStack, Float>>> COINS_CODEC = Codec.list(RecordCodecBuilder.create(instance -> instance.group(ItemStack.CODEC.fieldOf("a").forGetter(Pair::getFirst), Codec.FLOAT.fieldOf("b").forGetter(Pair::getSecond)).apply(instance, Pair::new)));
    private List<Pair<ItemStack, Float>> coins = new ArrayList<Pair<ItemStack, Float>>();

    public CoinPileBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(ModBlockEntities.COIN_PILE, blockPos, blockState);
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

    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity((BlockEntity)this, (Player)player);
    }

    public void clearContent() {
        this.coins.clear();
        this.setChanged();
    }

    public void preRemoveSideEffects(BlockPos blockPos, BlockState blockState) {
    }

    public void setChanged() {
        super.setChanged();
        if (this.level == null) {
            return;
        }
        BlockState state = this.getBlockState();
        this.level.sendBlockUpdated(this.worldPosition, state, state, 3);
        if (this.coins.isEmpty()) {
            this.remove(this.level, this.worldPosition);
        }
    }

    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create((BlockEntity)this);
    }

    public CompoundTag getUpdateTag(HolderLookup.Provider registryLookup) {
        return this.saveWithoutMetadata(registryLookup);
    }

    protected void saveAdditional(ValueOutput valueOutput) {
        super.saveAdditional(valueOutput);
        valueOutput.store("Items", COINS_CODEC, this.getCoins());
    }

    protected void loadAdditional(ValueInput valueInput) {
        super.loadAdditional(valueInput);
        valueInput.read("Items", COINS_CODEC).ifPresent(coins -> {
            this.coins = new ArrayList<Pair<ItemStack, Float>>((Collection<Pair<ItemStack, Float>>)coins);
        });
    }
}

