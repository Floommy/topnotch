package net.togyk.tog_yks_cards.client.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.togyk.tog_yks_cards.component.ModComponents;
import org.joml.Matrix3x2fStack;

@Environment(value=EnvType.CLIENT)
public class CardItemButton
extends Button {
    private final ItemStack stack;
    private final float scale;
    private final int itemXOffset;
    private final int itemYOffset;
    private final boolean faceDown;

    public CardItemButton(int x, int y, ItemStack stack, int itemXOffset, int itemYOffset, float scale, int width, int height, Component component, Button.OnPress onPress, Button.CreateNarration createNarration, boolean faceDown) {
        super(x, y, width, height, component, onPress, createNarration);
        this.stack = stack;
        this.scale = scale;
        this.itemXOffset = itemXOffset;
        this.itemYOffset = itemYOffset;
        this.faceDown = faceDown;
    }

    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate((float)(this.getX() + this.itemXOffset), (float)(this.getY() + this.itemYOffset));
        pose.scale(this.scale);
        if (!this.stack.isEmpty()) {
            ItemStack card = this.stack.copy();
            if (this.faceDown) {
                card.set(ModComponents.FLIPPED, true);
            }
            graphics.item(card, 0, 0);
            graphics.itemDecorations(Minecraft.getInstance().font, card, 0, 0);
        }
        pose.popMatrix();
        if (this.isHovered) {
            graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0x40FFFFFF);
        }
    }

    public boolean isFaceDown() {
        return this.faceDown;
    }

    public static Builder builder(Component component, ItemStack stack, Button.OnPress onPress) {
        return new Builder(component, stack, onPress);
    }

    static /* synthetic */ Button.CreateNarration access$000() {
        return DEFAULT_NARRATION;
    }

    @Environment(value=EnvType.CLIENT)
    public static class Builder {
        private final Component message;
        private final Button.OnPress onPress;
        private Tooltip tooltip;
        private int x;
        private int y;
        private int width = 16;
        private int height = 16;
        private int itemXOffset = 0;
        private int itemYOffset = 0;
        private float scale = 1.0f;
        private Button.CreateNarration createNarration = CardItemButton.access$000();
        private boolean faceDown = true;
        private final ItemStack stack;

        public Builder(Component component, ItemStack stack, Button.OnPress onPress) {
            this.message = component;
            this.onPress = onPress;
            this.stack = stack;
        }

        public Builder pos(int i, int j) {
            this.x = i;
            this.y = j;
            return this;
        }

        public Builder width(int i) {
            this.width = i;
            return this;
        }

        public Builder size(int i, int j) {
            this.width = i;
            this.height = j;
            return this;
        }

        public Builder bounds(int i, int j, int k, int l) {
            return this.pos(i, j).size(k, l);
        }

        public Builder itemOffset(int x, int y) {
            this.itemXOffset = x;
            this.itemYOffset = y;
            return this;
        }

        public Builder scale(float i) {
            this.scale = i;
            this.width = (int)((float)this.width * i);
            this.height = (int)((float)this.height * i);
            this.itemXOffset = (int)((float)this.itemXOffset * i);
            this.itemYOffset = (int)((float)this.itemYOffset * i);
            return this;
        }

        public Builder tooltip(Tooltip tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        public Builder createNarration(Button.CreateNarration createNarration) {
            this.createNarration = createNarration;
            return this;
        }

        public Builder faceDown(boolean faceDown) {
            this.faceDown = faceDown;
            return this;
        }

        public CardItemButton build() {
            CardItemButton button = new CardItemButton(this.x, this.y, this.stack, this.itemXOffset, this.itemYOffset, this.scale, this.width, this.height, this.message, this.onPress, this.createNarration, this.faceDown);
            button.setTooltip(this.tooltip);
            return button;
        }
    }
}

