package net.togyk.tog_yks_cards.blockentity.entities;

import net.minecraft.core.HolderLookup;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.togyk.tog_yks_cards.blockentity.ModBlockEntities;
import net.togyk.tog_yks_cards.client.menu.SelectCardMenu;

public class CardPileBlockEntity
extends BlockEntity
implements MenuProvider,
Container {
    Codec<List<ItemStack>> CARDS_CODEC = Codec.list(ItemStack.CODEC);
    private List<ItemStack> cards = new ArrayList<ItemStack>();
    private boolean faceDown = true;

    public CardPileBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(ModBlockEntities.CARD_PILE, blockPos, blockState);
    }

    public List<ItemStack> getCards() {
        return this.cards;
    }

    public void remove(Level level, BlockPos pos) {
        level.removeBlock(pos, false);
    }

    public void setCards(List<ItemStack> cards) {
        cards.forEach(stack -> stack.setCount(1));
        this.cards = new ArrayList<ItemStack>(cards);
        this.setChanged();
    }

    public ItemStack getCard() {
        return this.cards.isEmpty() ? ItemStack.EMPTY : this.cards.getLast();
    }

    public void addCard(ItemStack card) {
        card.setCount(1);
        this.cards.add(card);
        this.setChanged();
    }

    public void removeLastCard() {
        this.cards.removeLast();
        this.setChanged();
    }

    public void removeLastCard(Level level, BlockPos pos) {
        this.removeLastCard();
        if (this.cards.isEmpty()) {
            this.remove(level, pos);
        }
    }

    public boolean isFaceDown() {
        return this.faceDown;
    }

    public void setFaceDown(boolean faceDown) {
        this.faceDown = faceDown;
    }

    public int getContainerSize() {
        return this.cards.size();
    }

    public boolean isEmpty() {
        return this.cards.isEmpty();
    }

    public ItemStack getItem(int slot) {
        if (slot < 0 || slot >= this.cards.size()) {
            return ItemStack.EMPTY;
        }
        return this.cards.get(slot);
    }

    public ItemStack removeItem(int slot, int amount) {
        if (slot < 0 || slot >= this.cards.size()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = this.cards.get(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = stack.split(amount);
        if (stack.isEmpty()) {
            this.cards.remove(slot);
        }
        this.setChanged();
        return result;
    }

    public ItemStack removeItemNoUpdate(int slot) {
        if (slot < 0 || slot >= this.cards.size()) {
            return ItemStack.EMPTY;
        }
        return this.cards.remove(slot);
    }

    public void setItem(int slot, ItemStack stack) {
        stack.setCount(1);
        if (this.cards.size() <= slot) {
            return;
        }
        this.cards.set(slot, stack);
        this.setChanged();
    }

    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity((BlockEntity)this, (Player)player);
    }

    public void clearContent() {
        this.cards.clear();
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
        if (this.cards.isEmpty()) {
            this.remove(this.level, this.worldPosition);
        }
    }

    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create((BlockEntity)this);
    }

    public CompoundTag getUpdateTag(HolderLookup.Provider registryLookup) {
        return this.saveWithoutMetadata(registryLookup);
    }

    /*
     * Issues handling annotations - annotations may be inaccurate
     */
    protected void saveAdditional(ValueOutput valueOutput) {
        super.saveAdditional(valueOutput);
       
        ValueOutput.TypedOutputList typedOutputList = valueOutput.list("Items", ItemStackWithSlot.CODEC);
        for (int i = 0; i < this.cards.size(); ++i) {
            ItemStack itemStack = this.cards.get(i);
            if (itemStack.isEmpty()) continue;
            typedOutputList.add(new ItemStackWithSlot(i, itemStack));
        }
        if (typedOutputList.isEmpty()) {
            valueOutput.discard("Items");
        }
        valueOutput.store("face_down", Codec.BOOL, this.faceDown);
    }

    protected void loadAdditional(ValueInput valueInput) {
        super.loadAdditional(valueInput);
        this.cards.clear();
        for (ItemStackWithSlot itemStackWithSlot : valueInput.listOrEmpty("Items", ItemStackWithSlot.CODEC)) {
            this.cards.add(itemStackWithSlot.stack());
        }
        this.setFaceDown(valueInput.read("face_down", Codec.BOOL).orElse(true));
    }

    public Component getDisplayName() {
        return Component.translatable((String)"block.tog_yks-cards.card_pile");
    }

    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new SelectCardMenu(containerId, inventory, this);
    }
}

