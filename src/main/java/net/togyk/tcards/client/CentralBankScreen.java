package net.togyk.tcards.client;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.togyk.tcards.atm.AtmPayloads;
import net.togyk.tcards.atm.AtmPayloads.BankState;
import net.togyk.tcards.atm.AtmPayloads.PriceEntry;

/**
 * Price board for every ATM on the server. Ops type the numbers straight into the fields
 * ("amount for diamonds"), or set all chips / all coins at once, then press Save (or Enter).
 */
@Environment(EnvType.CLIENT)
public class CentralBankScreen extends Screen {
    private static final int W = 330, H = 252, ROW = 17, COL_W = 162, LIST_Y = 60;
    private static final int GOLD = 0xFFE8C25A, SOFT = 0xFF9DB0D0;
    private BankState state;
    private String message = "";
    private int messageColor = 0xFF86FCA0;
    private long messageUntil;

    private final List<EditBox[]> rows = new ArrayList<>();
    private EditBox allChipsAmount, allChipsDiamonds, allCoinsAmount, allCoinsDiamonds;
    private EditBox nameBox;

    public CentralBankScreen(BankState state) {
        super(Component.translatable("block.tog_yks-cards.central_bank"));
        this.state = state;
    }

    public static void registerReceiver() {
        ClientPlayNetworking.registerGlobalReceiver(BankState.TYPE, (payload, context) -> context.client().execute(() -> {
            Minecraft mc = context.client();
            if (mc.gui.screen() instanceof CentralBankScreen open) {
                open.state = payload;                      // keep what is being typed, just refresh the data
                if (!payload.message().isEmpty()) open.say(payload.message(), 0xFFFF8080);
            } else if (payload.open()) {
                mc.gui.setScreen(new CentralBankScreen(payload));
            }
        }));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void say(String msg, int color) {
        message = msg;
        messageColor = color;
        messageUntil = System.currentTimeMillis() + 3000;
    }

    private int left() {
        return (width - W) / 2;
    }

    private int top() {
        return (height - H) / 2;
    }

    private int rowX(int i) {
        return left() + 8 + (i < 10 ? 0 : COL_W);
    }

    private int rowY(int i) {
        return top() + LIST_Y + (i % 10) * ROW;
    }

    private EditBox numberBox(int x, int y, int w, int value) {
        EditBox box = new EditBox(font, x, y, w, 13, Component.empty());
        box.setMaxLength(3);
        box.setValue(String.valueOf(value));
        box.setResponder(s -> {
            String digits = s.replaceAll("[^0-9]", "");
            if (!digits.equals(s)) box.setValue(digits);
        });
        return box;
    }

    private static int num(EditBox box) {
        try {
            return box.getValue().isEmpty() ? 0 : Math.min(999, Integer.parseInt(box.getValue()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void send(PriceEntry e, int amount, int diamonds) {
        ClientPlayNetworking.send(new AtmPayloads.Action(state.pos(), AtmPayloads.SET_PRICE, e.id(), diamonds, amount, 0));
    }

    @Override
    protected void init() {
        rows.clear();
        nameBox = null;
        if (!state.canEdit()) return;
        nameBox = addRenderableWidget(new EditBox(font, left() + 120, top() + 4, 140, 13, Component.literal("Casino name")));
        nameBox.setMaxLength(24);
        nameBox.setValue(state.name());
        addRenderableWidget(Button.builder(Component.literal("Rename"), b -> rename()).bounds(left() + 264, top() + 3, 58, 15).build());
        List<PriceEntry> list = state.prices();
        for (int i = 0; i < list.size(); i++) {
            PriceEntry e = list.get(i);
            int x = rowX(i), y = rowY(i);
            EditBox amount = addRenderableWidget(numberBox(x + 74, y + 1, 24, e.amount()));
            EditBox diamonds = addRenderableWidget(numberBox(x + 120, y + 1, 22, e.diamonds()));
            rows.add(new EditBox[]{amount, diamonds});
        }
        // quick setters
        int qy = top() + 36;
        PriceEntry firstChip = list.stream().filter(p -> !AtmScreen.isCoin(p.id())).findFirst().orElse(new PriceEntry("", 1, 1));
        PriceEntry firstCoin = list.stream().filter(p -> AtmScreen.isCoin(p.id())).findFirst().orElse(new PriceEntry("", 1, 1));
        allChipsAmount = addRenderableWidget(numberBox(left() + 8 + 46, qy, 24, firstChip.amount()));
        allChipsDiamonds = addRenderableWidget(numberBox(left() + 8 + 90, qy, 22, firstChip.diamonds()));
        addRenderableWidget(Button.builder(Component.literal("Set"), b -> setAll(false)).bounds(left() + 8 + 116, qy - 1, 30, 15).build());
        allCoinsAmount = addRenderableWidget(numberBox(left() + 8 + 46 + COL_W, qy, 24, firstCoin.amount()));
        allCoinsDiamonds = addRenderableWidget(numberBox(left() + 8 + 90 + COL_W, qy, 22, firstCoin.diamonds()));
        addRenderableWidget(Button.builder(Component.literal("Set"), b -> setAll(true)).bounds(left() + 8 + 116 + COL_W, qy - 1, 30, 15).build());

        addRenderableWidget(Button.builder(Component.literal("Save"), b -> save()).bounds(left() + W - 68, top() + H - 20, 60, 16).build());
        addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose()).bounds(left() + W - 132, top() + H - 20, 60, 16).build());
    }

    private void rename() {
        if (nameBox == null || nameBox.getValue().isBlank() || nameBox.getValue().equals(state.name())) return;
        ClientPlayNetworking.send(new AtmPayloads.Action(state.pos(), AtmPayloads.SET_NAME, nameBox.getValue(), 0, 0, 0));
        say("Renamed to " + nameBox.getValue(), 0xFF86FCA0);
    }

    /** Copies the quick setter into every chip (or coin) row and saves right away. */
    private void setAll(boolean coins) {
        int amount = num(coins ? allCoinsAmount : allChipsAmount);
        int diamonds = num(coins ? allCoinsDiamonds : allChipsDiamonds);
        List<PriceEntry> list = state.prices();
        for (int i = 0; i < list.size() && i < rows.size(); i++) {
            if (AtmScreen.isCoin(list.get(i).id()) != coins) continue;
            rows.get(i)[0].setValue(String.valueOf(amount));
            rows.get(i)[1].setValue(String.valueOf(diamonds));
        }
        save();
    }

    private void save() {
        if (!state.canEdit()) return;
        List<PriceEntry> list = state.prices();
        int changed = 0;
        for (int i = 0; i < list.size() && i < rows.size(); i++) {
            PriceEntry e = list.get(i);
            int amount = num(rows.get(i)[0]), diamonds = num(rows.get(i)[1]);
            if (amount != e.amount() || diamonds != e.diamonds()) {
                send(e, amount, diamonds);
                changed++;
            }
        }
        say(changed == 0 ? "Nothing changed" : "Saved " + changed + (changed == 1 ? " price" : " prices"), 0xFF86FCA0);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_RETURN
                || event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_NUMPADENTER) {     // Enter = save
            if (nameBox != null && nameBox.isFocused()) rename();
            else save();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float pt) {
        int x0 = left(), y0 = top();
        g.fill(0, 0, width, height, 0x70000000);
        g.fill(x0 - 2, y0 - 2, x0 + W + 2, y0 + H + 2, 0xFFB08A30);
        g.fill(x0 - 1, y0 - 1, x0 + W + 1, y0 + H + 1, 0xFF4A3A14);
        g.fill(x0, y0, x0 + W, y0 + H, 0xF0101820);
        g.fill(x0 + 4, y0 + 30, x0 + W - 4, y0 + 31, 0xFF6A5420);
        g.fill(x0 + 4, y0 + 54, x0 + W - 4, y0 + 55, 0xFF6A5420);
        g.fill(x0 + 4, y0 + H - 25, x0 + W - 4, y0 + H - 24, 0xFF6A5420);
    }

    private void diamond(GuiGraphicsExtractor g, int x, int y) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(0.625f, 0.625f);
        g.item(new ItemStack(Items.DIAMOND), 0, 0);
        g.pose().popMatrix();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
        super.extractRenderState(g, mx, my, pt);
        int x0 = left(), y0 = top();
        g.text(font, Component.literal("CENTRAL BANK").withStyle(ChatFormatting.BOLD), x0 + 8, y0 + 7, GOLD, true);
        if (nameBox == null) {
            String n = state.name();
            g.text(font, n, x0 + W - 8 - font.width(n), y0 + 7, 0xFFFFFFFF, false);
        }
        g.text(font, "Prices for the ATMs of this casino", x0 + 8, y0 + 18, SOFT, false);
        String hint = state.canEdit() ? "0 = not sold" : "Owner only";
        g.text(font, hint, x0 + W - 8 - font.width(hint), y0 + 18, 0xFF707C90, false);

        // quick setters
        if (state.canEdit()) {
            int qy = y0 + 36;
            for (int c = 0; c < 2; c++) {
                int x = x0 + 8 + c * COL_W;
                g.text(font, c == 0 ? "All chips" : "All coins", x, qy + 3, GOLD, false);
                g.text(font, "for", x + 73, qy + 3, SOFT, false);
            }
        } else {
            g.text(font, "items for diamonds", x0 + 8, y0 + 40, SOFT, false);
        }

        List<PriceEntry> list = state.prices();
        for (int i = 0; i < list.size(); i++) {
            PriceEntry e = list.get(i);
            int x = rowX(i), y = rowY(i);
            if ((i % 2) == 0) g.fill(x - 2, y - 1, x + COL_W - 8, y + ROW - 1, 0x14FFFFFF);
            ItemStack item = AtmScreen.stack(e.id());
            g.item(item, x, y - 1);
            String name = AtmScreen.shortName(e.id());
            if (font.width(name) > 54) name = font.plainSubstrByWidth(name, 50) + "..";
            g.text(font, name, x + 18, y + 4, 0xFFFFFFFF, false);
            g.text(font, "for", x + 101, y + 4, SOFT, false);
            if (!state.canEdit()) {
                boolean sale = e.amount() > 0 && e.diamonds() > 0;
                int col = sale ? 0xFFFFFFFF : 0xFF707070;
                g.text(font, sale ? String.valueOf(e.amount()) : "-", x + 96 - font.width(String.valueOf(e.amount())), y + 4, col, false);
                g.text(font, sale ? String.valueOf(e.diamonds()) : "-", x + 122, y + 4, col, false);
            }
            diamond(g, x + 145, y + 2);
            if (mx >= x && mx < x + 74 && my >= y && my < y + ROW) {
                g.setComponentTooltipForNextFrame(font, List.of(item.getHoverName(),
                        Component.literal("§7" + AtmScreen.priceText(e))), mx, my);
            }
        }
        if (System.currentTimeMillis() < messageUntil) {
            g.text(font, message, x0 + 8, y0 + H - 16, messageColor, false);
        } else if (state.canEdit()) {
            g.text(font, "Type the numbers, then Save", x0 + 8, y0 + H - 16, 0xFF707C90, false);
        }
    }
}
