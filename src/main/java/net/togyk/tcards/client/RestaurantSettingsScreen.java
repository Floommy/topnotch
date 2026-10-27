package net.togyk.tcards.client;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.togyk.tcards.restaurant.RestaurantMenu;
import net.togyk.tcards.restaurant.RestaurantPayloads;
import net.togyk.tcards.restaurant.RestaurantPayloads.State;

/** Owner settings of a restaurant: name, currency, prices, cash box and the chest for the money. */
@Environment(EnvType.CLIENT)
public class RestaurantSettingsScreen extends Screen {
    private static final int W = 330, H = 256, ROW = 14, COL_W = 162, LIST_Y = 58;
    private final RestaurantScreen parent;
    private State state;
    private final List<EditBox> prices = new ArrayList<>();
    private EditBox nameBox;
    private String message = "";
    private long messageUntil;
    private int messageColor = 0xFF86FCA0;

    public RestaurantSettingsScreen(RestaurantScreen parent, State state) {
        super(Component.literal("Restaurant settings"));
        this.parent = parent;
        this.state = state;
    }

    public BlockPos pos() {
        return state.pos();
    }

    void update(State s) {
        boolean currencyChanged = !s.currency().equals(state.currency());
        state = s;
        parent.update(s);
        if (!s.message().isEmpty()) say(s.message(), s.result() == RestaurantPayloads.RESULT_ERROR ? 0xFFFF8080 : 0xFF86FCA0);
        if (currencyChanged) rebuildWidgets();
    }

    private void say(String m, int c) {
        message = m;
        messageColor = c;
        messageUntil = System.currentTimeMillis() + 3500;
    }

    private void send(int action, String text, int a) {
        ClientPlayNetworking.send(new RestaurantPayloads.Action(state.pos(), action, text, a));
    }

    private int left() {
        return (width - W) / 2;
    }

    private int top() {
        return (height - H) / 2;
    }

    private EditBox numberBox(int x, int y, int value) {
        EditBox box = new EditBox(font, x, y, 26, 13, Component.empty());
        box.setMaxLength(3);
        box.setValue(String.valueOf(value));
        box.setResponder(s -> {
            String d = s.replaceAll("[^0-9]", "");
            if (!d.equals(s)) box.setValue(d);
        });
        return box;
    }

    @Override
    protected void init() {
        prices.clear();
        int x0 = left(), y0 = top();
        nameBox = addRenderableWidget(new EditBox(font, x0 + 8, y0 + 20, 150, 14, Component.literal("Name")));
        nameBox.setMaxLength(24);
        nameBox.setValue(state.name());
        addRenderableWidget(Button.builder(Component.literal("Rename"), b -> rename()).bounds(x0 + 162, y0 + 19, 56, 16).build());
        boolean emerald = "emerald".equals(state.currency());
        addRenderableWidget(Button.builder(Component.literal("Pay with: " + (emerald ? "Emeralds" : "Diamonds")),
                b -> send(RestaurantPayloads.SET_CURRENCY, "", emerald ? 0 : 1)).bounds(x0 + 222, y0 + 19, 100, 16).build());

        for (int i = 0; i < state.items().size(); i++) {
            int x = x0 + 8 + (i < 11 ? 0 : COL_W) + 104, y = y0 + LIST_Y + (i % 11) * ROW;
            prices.add(addRenderableWidget(numberBox(x, y, state.prices().get(i))));
        }
        int by = y0 + H - 38;
        addRenderableWidget(Button.builder(Component.literal("Empty cash box"), b -> send(RestaurantPayloads.EMPTY_VAULT, "", 0)).bounds(x0 + 8, by, 96, 16).build());
        addRenderableWidget(Button.builder(Component.literal("Link chest"), b -> {
            send(RestaurantPayloads.LINK_CHEST, "", 0);
            onClose();
        }).bounds(x0 + 108, by, 70, 16).build());
        Button unlink = addRenderableWidget(Button.builder(Component.literal("Unlink"), b -> send(RestaurantPayloads.UNLINK_CHEST, "", 0)).bounds(x0 + 182, by, 50, 16).build());
        unlink.active = state.linkStatus() != 0;
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> minecraft.gui.setScreen(parent)).bounds(x0 + W - 132, y0 + H - 22, 60, 16).build());
        addRenderableWidget(Button.builder(Component.literal("Save prices"), b -> save()).bounds(x0 + W - 68, y0 + H - 22, 60, 16).build());
    }

    private void rename() {
        if (!nameBox.getValue().isBlank() && !nameBox.getValue().equals(state.name())) send(RestaurantPayloads.SET_NAME, nameBox.getValue(), 0);
    }

    private void save() {
        int changed = 0;
        for (int i = 0; i < prices.size() && i < state.items().size(); i++) {
            int v;
            try {
                v = prices.get(i).getValue().isEmpty() ? 0 : Math.min(999, Integer.parseInt(prices.get(i).getValue()));
            } catch (NumberFormatException e) {
                v = 0;
            }
            if (v != state.prices().get(i)) {
                send(RestaurantPayloads.SET_PRICE, state.items().get(i), v);
                changed++;
            }
        }
        say(changed == 0 ? "Nothing changed" : "Saved " + changed + (changed == 1 ? " price" : " prices"), 0xFF86FCA0);
        if (changed > 0) send(RestaurantPayloads.REFRESH, "", 0);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER) {
            if (nameBox.isFocused()) rename();
            else save();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(null);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float pt) {
        int x0 = left(), y0 = top();
        g.fill(0, 0, width, height, 0x80000000);
        g.fill(x0 - 2, y0 - 2, x0 + W + 2, y0 + H + 2, 0xFFDA291C);
        g.fill(x0, y0, x0 + W, y0 + H, 0xF0181818);
        g.fill(x0 + 4, y0 + 40, x0 + W - 4, y0 + 41, 0xFF5A1A14);
        g.fill(x0 + 4, y0 + H - 42, x0 + W - 4, y0 + H - 41, 0xFF5A1A14);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
        super.extractRenderState(g, mx, my, pt);
        int x0 = left(), y0 = top();
        g.text(font, Component.literal("RESTAURANT SETTINGS").withStyle(ChatFormatting.BOLD), x0 + 8, y0 + 6, 0xFFFFC72C, true);
        boolean msg = System.currentTimeMillis() < messageUntil;
        String sales = msg ? message : "Sales: " + state.sales();
        g.text(font, sales, x0 + W - 8 - font.width(sales), y0 + 6, msg ? messageColor : 0xFF9A9A9A, false);
        g.text(font, "Menu prices (" + state.currency() + "s, 0 = not sold)", x0 + 8, y0 + 45, 0xFFBBBBBB, false);
        ItemStack cur = new ItemStack("emerald".equals(state.currency()) ? Items.EMERALD : Items.DIAMOND);
        for (int i = 0; i < state.items().size(); i++) {
            int x = x0 + 8 + (i < 11 ? 0 : COL_W), y = y0 + LIST_Y + (i % 11) * ROW;
            String item = state.items().get(i);
            g.pose().pushMatrix();
            g.pose().translate(x, y - 1);
            g.pose().scale(0.875f, 0.875f);
            g.item(RestaurantMenu.stack(item), 0, 0);
            g.pose().popMatrix();
            String n = RestaurantMenu.shortName(item);
            if (font.width(n) > 82) n = font.plainSubstrByWidth(n, 78) + "..";
            g.text(font, n, x + 17, y + 3, 0xFFFFFFFF, false);
            g.pose().pushMatrix();
            g.pose().translate(x + 133, y + 2);
            g.pose().scale(0.625f, 0.625f);
            g.item(cur, 0, 0);
            g.pose().popMatrix();
        }
        int by = y0 + H - 38;
        String box = "Cash box: " + state.vault();
        String chest = switch (state.linkStatus()) {
            case 1 -> "Chest: " + state.linkText();
            case 2 -> "Chest: not loaded";
            case 3 -> "Chest: full!";
            default -> "No chest linked";
        };
        g.text(font, box, x0 + 240, by + 4, 0xFFFFFFFF, false);
        g.text(font, chest, x0 + 8, by + 20, state.linkStatus() == 3 ? 0xFFFF8080 : 0xFFBBBBBB, false);
    }
}
