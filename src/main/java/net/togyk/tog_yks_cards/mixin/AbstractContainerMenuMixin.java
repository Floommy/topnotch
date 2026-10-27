package net.togyk.tog_yks_cards.mixin;

import it.unimi.dsi.fastutil.ints.IntList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.RemoteSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={AbstractContainerMenu.class})
public interface AbstractContainerMenuMixin {
    @Accessor(value="stateId")
    public void setStateId(int var1);

    @Accessor(value="remoteSlots")
    public NonNullList<RemoteSlot> getRemoteSlots();

    @Accessor(value="remoteCarried")
    public RemoteSlot getRemoteCarried();

    @Accessor(value="dataSlots")
    public List<DataSlot> getDataSlots();

    @Accessor(value="remoteDataSlots")
    public IntList getRemoteDataSlots();

    @Accessor(value="synchronizer")
    public ContainerSynchronizer getSynchronizer();
}

