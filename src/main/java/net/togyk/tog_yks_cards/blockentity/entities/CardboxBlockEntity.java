package net.togyk.tog_yks_cards.blockentity.entities;

import net.minecraft.core.HolderLookup;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.togyk.tog_yks_cards.blockentity.ModBlockEntities;

public class CardboxBlockEntity
extends BlockEntity {
    private ItemStack box = ItemStack.EMPTY;

    public CardboxBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(ModBlockEntities.CARD_BOX, blockPos, blockState);
    }

    public ItemStack getCardbox() {
        return this.box;
    }

    public void setCardbox(ItemStack cardbox) {
        this.box = cardbox;
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

    protected void saveAdditional(ValueOutput valueOutput) {
        super.saveAdditional(valueOutput);
        if (!this.box.isEmpty()) {
            valueOutput.store("cardbox", ItemStack.CODEC, this.box);
        }
    }

    protected void loadAdditional(ValueInput valueInput) {
        super.loadAdditional(valueInput);
        this.setCardbox(valueInput.read("cardbox", ItemStack.CODEC).orElse(ItemStack.EMPTY));
    }
}

