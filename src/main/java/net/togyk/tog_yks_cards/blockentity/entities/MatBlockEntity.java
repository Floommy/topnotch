package net.togyk.tog_yks_cards.blockentity.entities;

import net.minecraft.core.HolderLookup;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.togyk.tog_yks_cards.block.custom.MatBlock;
import net.togyk.tog_yks_cards.blockentity.ModBlockEntities;
import net.togyk.tog_yks_cards.mat_item.MatItem;

public class MatBlockEntity
extends BlockEntity {
    private Codec<List<MatItem>> ITEM_CODEC = Codec.list(MatItem.DISPATCH_CODEC).xmap(items -> {
        items.forEach(item -> item.setMat(this));
        return items;
    }, items -> items);
    private List<MatItem> matItems = new ArrayList<MatItem>();

    public MatBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(ModBlockEntities.MAT, blockPos, blockState);
        this.setChanged();
    }

    public List<MatItem> getMatItems() {
        return this.matItems;
    }

    public List<MatItem> getSupportedItems() {
        BlockPos center = this.getBlockPos();
        Level level = this.getLevel();
        if (level == null) {
            return new ArrayList<MatItem>();
        }
        List<BlockPos> neighbors = List.of(center.west(), center.west().north(), center.west().south(), center.east(), center.east().north(), center.east().south(), center.north(), center.south());
        ArrayList<MatItem> supported = new ArrayList<MatItem>();
        for (BlockPos pos : neighbors) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof MatBlockEntity)) continue;
            MatBlockEntity neighbor = (MatBlockEntity)blockEntity;
            supported.addAll(neighbor.getMatItems().stream().filter(item -> item.overHangs(center)).toList());
        }
        return supported;
    }

    public List<MatItem> getAllMatItems() {
        ArrayList<MatItem> items = new ArrayList<MatItem>(this.getMatItems());
        items.addAll(this.getSupportedItems());
        return items;
    }

    public List<VoxelShape> getVoxelShapes() {
        return this.getAllMatItems().stream().map(item -> item.getRelativeShape(this.getBlockPos())).toList();
    }

    public void setMatItems(List<MatItem> items) {
        this.matItems = new ArrayList<MatItem>(items);
        this.matItems.stream().forEach(item -> item.setMat(this));
        this.setChanged();
    }

    public void removeItem(MatItem item) {
        this.matItems.remove(item);
        this.setChanged();
    }

    public void addMatItem(MatItem item) {
        item.setMat(this);
        this.matItems.add(item);
        this.setChanged();
    }

    public void setChanged() {
        super.setChanged();
        if (this.level == null) {
            return;
        }
        BlockState state = this.getBlockState();
        int lightLevel = this.getMatItems().stream().map(item -> item.getState().getLightEmission()).max(Integer::compareTo).orElse(0);
        this.level.setBlock(this.worldPosition, (BlockState)state.setValue(MatBlock.LIGHT_LEVEL, (MatBlock.LightLevel.fromInt(lightLevel))), 6);
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
        if (!this.matItems.isEmpty()) {
            valueOutput.store("mat_items", this.ITEM_CODEC, this.matItems);
        }
    }

    protected void loadAdditional(ValueInput valueInput) {
        super.loadAdditional(valueInput);
        this.setMatItems(valueInput.read("mat_items", this.ITEM_CODEC).orElse(new ArrayList()));
    }
}

