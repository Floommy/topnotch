package net.togyk.tog_yks_cards.blockentity.entities;

import net.minecraft.core.HolderLookup;

import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Vec3i;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.togyk.tog_yks_cards.block.custom.CardHolderBlock;
import net.togyk.tog_yks_cards.blockentity.ModBlockEntities;
import net.togyk.tog_yks_cards.client.menu.SelectCardMenu;

public class CardHolderBlockEntity
extends BlockEntity
implements MenuProvider,
Container {
    Codec<List<ItemStack>> CARDS_CODEC = Codec.list(ItemStack.CODEC);
    private NonNullList<ItemStack> cards = NonNullList.create();

    public CardHolderBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(ModBlockEntities.CARD_HOLDER, blockPos, blockState);
    }

    public NonNullList<ItemStack> getCards() {
        return this.cards;
    }

    public void setCards(List<ItemStack> cards) {
        this.cards = NonNullList.create();
        this.cards.addAll(cards);
        this.setChanged();
    }

    public ItemStack getCard() {
        return this.cards.isEmpty() ? ItemStack.EMPTY : (ItemStack)this.cards.getLast();
    }

    public void addCard(ItemStack card) {
        this.cards.add(card);
        this.setChanged();
    }

    public void addCards(List<ItemStack> cards) {
        this.cards.addAll(cards);
        this.setChanged();
    }

    public void removeCard(ItemStack card) {
        this.cards.remove(card);
        this.setChanged();
    }

    public void setChanged() {
        super.setChanged();
        if (this.level == null) {
            return;
        }
        BlockState state = this.getBlockState();
        this.level.sendBlockUpdated(this.worldPosition, state, state, 3);
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
       
        ValueOutput.TypedOutputList typedOutputList = valueOutput.list("Items", ItemStackWithSlot.CODEC);
        for (int i = 0; i < this.cards.size(); ++i) {
            ItemStack itemStack = (ItemStack)this.cards.get(i);
            if (itemStack.isEmpty()) continue;
            typedOutputList.add(new ItemStackWithSlot(i, itemStack));
        }
        if (typedOutputList.isEmpty()) {
            valueOutput.discard("Items");
        }
        super.saveAdditional(valueOutput);
    }

    protected void loadAdditional(ValueInput valueInput) {
        super.loadAdditional(valueInput);
        this.cards.clear();
        for (ItemStackWithSlot itemStackWithSlot : valueInput.listOrEmpty("Items", ItemStackWithSlot.CODEC)) {
            this.cards.add(itemStackWithSlot.stack());
        }
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
        return (ItemStack)this.cards.get(slot);
    }

    public ItemStack removeItem(int slot, int amount) {
        if (slot < 0 || slot >= this.cards.size()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = (ItemStack)this.cards.get(slot);
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
        return (ItemStack)this.cards.remove(slot);
    }

    public void setItem(int slot, ItemStack stack) {
        if (this.cards.size() <= slot) {
            return;
        }
        this.cards.set(slot, stack);
        this.setChanged();
    }

    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity((BlockEntity)this, (Player)player) && this.isInFront(player);
    }

    public void clearContent() {
        this.cards.clear();
        this.setChanged();
    }

    public void preRemoveSideEffects(BlockPos blockPos, BlockState blockState) {
    }

    public Component getDisplayName() {
        return Component.translatable((String)"block.tog_yks-cards.card_holder");
    }

    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new SelectCardMenu(containerId, inventory, this);
    }

    public boolean isInFront(Player player) {
        Block block;
        BlockState state = this.getBlockState();
        if (!this.isEmpty() && (block = state.getBlock()) instanceof CardHolderBlock) {
            float difference;
            CardHolderBlock cardHolderBlock = (CardHolderBlock)block;
            float rotation = cardHolderBlock.getYRotationDegrees(state);
            Vec3 blockCenter = Vec3.atCenterOf((Vec3i)this.getBlockPos());
            Vec3 playerPos = player.position();
            double dz = playerPos.z - blockCenter.z;
            double dx = playerPos.x - blockCenter.x;
            float playerRotation = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
            if (playerRotation < 0.0f) {
                playerRotation += 360.0f;
            }
            return Math.abs(difference = (playerRotation - rotation + 540.0f) % 360.0f - 180.0f) <= 90.0f;
        }
        return false;
    }
}

