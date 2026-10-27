package net.togyk.tog_yks_cards.client.menu;

import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.Property;
import net.togyk.tog_yks_cards.block.custom.CardpileBlock;
import net.togyk.tog_yks_cards.blockentity.entities.CardPileBlockEntity;
import net.togyk.tog_yks_cards.client.menu.ModMenuTypes;
import net.togyk.tog_yks_cards.mat_item.CardPileMatItem;
import net.togyk.tog_yks_cards.mixin.AbstractContainerMenuMixin;

public class SelectCardMenu
extends AbstractContainerMenu {
    private Container container;
    private BiConsumer<Boolean, Boolean> updateButtons;
    private final ContainerData data;
    private boolean facedown = false;
    private boolean isPile = false;

    public SelectCardMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, (Container)new SimpleContainer(10));
    }

    public SelectCardMenu(int containerId, Inventory inventory, Container container) {
        super(ModMenuTypes.CARD_SELECT, containerId);
        this.container = container;
        container.startOpen((ContainerUser)inventory.player);
        if (container instanceof CardPileMatItem) {
            CardPileMatItem cardpile = (CardPileMatItem)container;
            this.facedown = (Boolean)cardpile.getState().getValue(CardpileBlock.FACE_DOWN);
            this.isPile = true;
        } else if (container instanceof CardPileBlockEntity) {
            CardPileBlockEntity cardpile = (CardPileBlockEntity)container;
            this.facedown = cardpile.isFaceDown();
            this.isPile = true;
        }
        this.data = new ContainerData(){
            private final int[] values = new int[2];
            {
                this.values[0] = SelectCardMenu.this.facedown ? 1 : 0;
                this.values[1] = SelectCardMenu.this.isPile ? 1 : 0;
            }

            public int get(int index) {
                return this.values[index];
            }

            public void set(int index, int value) {
                this.values[index] = value;
                if (index == 0) {
                    boolean bl = SelectCardMenu.this.facedown = value != 0;
                }
                if (index == 1) {
                    SelectCardMenu.this.isPile = value != 0;
                }
                SelectCardMenu.this.updateButtons.accept(SelectCardMenu.this.facedown, SelectCardMenu.this.isPile);
            }

            public int getCount() {
                return this.values.length;
            }
        };
        this.addDataSlots(this.data);
        this.addCardSlots();
    }

    public boolean isFacedown() {
        return this.facedown;
    }

    public boolean isPile() {
        return this.isPile;
    }

    private void addCardSlots() {
        for (int i = 0; i < this.getContainerSize(); ++i) {
            this.addSlot(new Slot(this.container, i, -1000, 0));
        }
    }

    /*
     * Issues handling annotations - annotations may be inaccurate
     */
    public NonNullList<ItemStack> getItems() {
        NonNullList cards = NonNullList.createWithCapacity((int)this.getContainerSize());
        Container container = this.container;
        for (int i = 0; i < this.getContainerSize(); ++i) {
            cards.set(i, container.getItem(i));
        }
        return cards;
    }

    public Container getContainer() {
        return this.container;
    }

    public int getContainerSize() {
        return this.container.getContainerSize();
    }

    public void initializeContents(int stateId, List<ItemStack> list, ItemStack carried) {
        int size = list.size();
        this.container = new SimpleContainer(size);
        for (int i = 0; i < size; ++i) {
            this.container.setItem(i, list.get(i));
        }
        this.updateButtons.accept(this.isFacedown(), this.isPile());
        this.setCarried(carried);
        ((AbstractContainerMenuMixin)(this)).setStateId(stateId);
    }

    public boolean clickMenuButton(Player player, int id) {
        ItemStack stack = this.container.removeItem(id, 1);
        player.addItem(stack);
        return true;
    }

    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (slotIndex >= this.slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = (Slot)this.slots.get(slotIndex);
        return slot.getItem();
    }

    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen((ContainerUser)player);
    }

    public void setUpdateButtons(BiConsumer<Boolean, Boolean> updateButtons) {
        this.updateButtons = updateButtons;
        this.updateButtons.accept(this.facedown, this.isPile);
    }
}

