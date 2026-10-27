package net.togyk.tcards.atm;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Holds the id of its casino (the data lives in {@link CasinoBanks}). The id travels with the dropped item. */
public class CentralBankBlockEntity extends BlockEntity {
    private UUID bank;
    private String name = "";
    private UUID owner;

    public CentralBankBlockEntity(BlockPos pos, BlockState state) {
        super(AtmRegistry.BANK_BE, pos, state);
    }

    public UUID bank() {
        return bank;
    }

    /** Called once when placed: reuses the casino from the item, or opens a new one. */
    public void setUp(Player placer) {
        if (level == null || level.getServer() == null) return;
        CasinoBanks banks = CasinoBanks.get(level.getServer());
        if (bank == null) {
            bank = banks.create(placer);
        } else {
            banks.ensure(bank, name.isEmpty() ? "Casino" : name, placer);
        }
        CasinoBanks.Bank b = banks.get(bank);
        name = b.name();
        owner = b.owner().orElse(null);
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
    }

    public CasinoBanks.Bank data() {
        if (level == null || level.getServer() == null) return null;
        CasinoBanks.Bank b = CasinoBanks.get(level.getServer()).get(bank);
        if (b != null && !b.name().equals(name)) {
            name = b.name();
            setChanged();
        }
        return b;
    }

    /** Owner and ops; also works on the client (owner is synced) for the mining check. */
    public boolean canManage(Player player) {
        if (level != null && !level.isClientSide()) {
            CasinoBanks.Bank b = data();
            if (b != null) return b.canManage(player);
        }
        return owner == null || owner.equals(player.getUUID()) || AtmBlockEntity.isAdmin(player);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (bank != null) components.set(CasinoMark.TYPE, new CasinoMark(bank, name));
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        CasinoMark m = components.get(CasinoMark.TYPE);
        if (m != null) {
            bank = m.bank();
            name = m.name();
        }
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("bank");
        output.discard("name");
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        if (owner != null) tag.store("owner", UUIDUtil.CODEC, owner);
        return tag;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (bank != null) output.store("bank", UUIDUtil.CODEC, bank);
        output.putString("name", name);
        if (owner != null) output.store("owner", UUIDUtil.CODEC, owner);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        bank = input.read("bank", UUIDUtil.CODEC).orElse(bank);
        name = input.getStringOr("name", name);
        owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
    }
}
