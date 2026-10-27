package net.togyk.tog_yks_cards.client.menu.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.togyk.tog_yks_cards.client.menu.SelectCardMenu;
import net.togyk.tog_yks_cards.client.widget.CardItemButton;

public class CardSelectScreen
extends AbstractContainerScreen<SelectCardMenu> {
    Minecraft mc = Minecraft.getInstance();
    private Integer oldGuiScale;
    private int MAX_COLUMNS = 6;
    private int MAX_ROWS = 4;
    private int page = 0;

    public CardSelectScreen(SelectCardMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
        this.oldGuiScale = (Integer)this.mc.options.guiScale().get();
        this.mc.options.guiScale().set(4);
        this.mc.resizeGui();
        menu.setUpdateButtons(this::UpdateButtons);
    }

    public void removed() {
        if (this.oldGuiScale != null) {
            this.mc.options.guiScale().set(this.oldGuiScale);
            this.mc.resizeGui();
        }
        super.removed();
    }

    private int cardsPerPage() {
        return this.MAX_COLUMNS * this.MAX_ROWS;
    }

    private int pageCount() {
        return Math.max(1, Mth.ceil((float)((float)((SelectCardMenu)this.menu).getContainerSize() / (float)this.cardsPerPage())));
    }

    public void UpdateButtons(boolean isFacedown, boolean isPile) {
        this.page = Math.min(this.page, this.pageCount() - 1);
        boolean isShown = false;
        this.clearWidgets();
        int start = this.page * this.cardsPerPage();
        int end = Math.min(start + this.cardsPerPage(), ((SelectCardMenu)this.menu).getContainerSize());
        int columns = Math.min(((SelectCardMenu)this.menu).getContainerSize(), this.MAX_COLUMNS);
        int cardsWidth = (columns - 1) * 24 + 20;
        int cardsX = this.width / 2 - cardsWidth / 2;
        int rows = Math.min(((SelectCardMenu)this.menu).getContainerSize() / columns, this.MAX_ROWS);
        int height = (rows - 1) * 34 + 28;
        int cardsY = this.height / 2 - height / 2;
        for (int i = start; i < end; ++i) {
            int local = i - start;
            int x = cardsX + local % columns * 24;
            int y = cardsY + local / columns * 34;
            int index = isPile ? ((SelectCardMenu)this.menu).getContainerSize() - i - 1 : i;
            ItemStack stack = ((SelectCardMenu)this.menu).getContainer().getItem(index);
            this.addRenderableWidget(CardItemButton.builder((Component)Component.empty(), stack, button -> {
                if (this.minecraft.gameMode != null) {
                    this.minecraft.gameMode.handleInventoryButtonClick(((SelectCardMenu)this.menu).containerId, index);
                }
                this.onClose();
            }).bounds(x, y, 10, 14).itemOffset(-3, -1).scale(2.0f).faceDown(isFacedown || isPile && !isShown && index != ((SelectCardMenu)this.menu).getContainerSize() - 1).build());
        }
        if (this.pageCount() != 1) {
            Button prev = Button.builder((Component)Component.literal((String)"<"), b -> {
                if (this.page > 0) {
                    --this.page;
                    this.UpdateButtons(isFacedown, isPile);
                }
            }).bounds(cardsX - 25, cardsY + height / 2 - 10, 20, 20).build();
            Button next = Button.builder((Component)Component.literal((String)">"), b -> {
                if (this.page < this.pageCount() - 1) {
                    ++this.page;
                    this.UpdateButtons(isFacedown, isPile);
                }
            }).bounds(cardsX + cardsWidth + 5, cardsY + height / 2 - 10, 20, 20).build();
            this.addRenderableWidget(prev);
            this.addRenderableWidget(next);
            prev.active = this.page > 0;
            next.active = this.page < this.pageCount() - 1;
        }
    }

    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        this.extractTransparentBackground(graphics);
    }

    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
    }

    public void resize(int i, int j) {
        super.resize(i, j);
        this.UpdateButtons(true, false);
    }
}

