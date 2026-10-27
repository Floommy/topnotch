package net.togyk.tcards.client;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.togyk.tcards.TableSounds;
import net.togyk.tcards.atm.AtmPayloads;
import net.togyk.tcards.atm.AtmPayloads.AtmState;
import net.togyk.tcards.atm.AtmPayloads.PriceEntry;

/**
 * Cash machine screen. The display is a touch screen: every option is a clearly drawn button you can click.
 * The grey keys next to the display press the button in the same row, the keypad types the amount
 * (CANCEL = back, CLEAR = delete, ENTER = buy). Layout coordinates match textures/gui/atm.png.
 */
@Environment(EnvType.CLIENT)
public class AtmScreen extends Screen {
    public static final int W = 280, H = 224;
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("tog_yks-cards", "textures/gui/atm.png");

    // layout (same numbers as assets_gen/atm_assets.py)
    static final int LCD_X0 = 44, LCD_Y0 = 24, LCD_X1 = 236, LCD_Y1 = 120;
    static final int[] KEY_ROWS = {46, 64, 82, 100};
    static final int KEY_L_X = 20, KEY_R_X = 244, KEY_W = 16, KEY_H = 14;
    static final int[] DIGIT_X = {70, 96, 122};
    static final int[] PAD_Y = {139, 157, 175, 193};
    static final int DIGIT_W = 22, PAD_H = 15, FUNC_X = 152, FUNC_W = 46;
    static final int TRAY_X0 = 213, TRAY_Y0 = 143, TRAY_X1 = 267, TRAY_Y1 = 179;
    static final int MAX_QTY = 36 * 64;
    // the two button columns on the display, aligned with the side keys
    static final int COL_L = 48, COL_R = 141, COL_W = 91;
    // amount box on the AMOUNT page
    static final int AMOUNT_X = 112, AMOUNT_Y = 45, AMOUNT_W = 56, AMOUNT_H = 15;

    // LCD colours
    static final int TXT = 0xFFDDEEFF, DIM = 0xFF8FA8D0, HI = 0xFF8FE8FF, OK = 0xFF86FCA0, ERR = 0xFFFF8080, WARN = 0xFFFFD866;
    static final int BTN = 0xFF1C3A72, BTN_HOVER = 0xFF2D5AA8, BTN_EDGE = 0xFF6F9BE0;
    static final int GO = 0xFF1B6A3C, GO_HOVER = 0xFF25904F, GO_EDGE = 0xFF6FE09B;
    static final int BACK = 0xFF3A3F55, BACK_HOVER = 0xFF51587A, BACK_EDGE = 0xFF9AA3C8;

    enum Page { HOME, SELECT_CHIPS, SELECT_COINS, SELL_SELECT, AMOUNT, PROCESSING, DISPENSED, ERROR, SERVICE }

    /** A touch button on the display. side: 0 = left key column, 1 = right, -1 = no side key. */
    record Btn(int x, int y, int w, int h, String label, int style, Runnable action, int side, int row) {
        static final int NORMAL = 0, GREEN = 1, GREY = 2;

        boolean hit(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private AtmState state;
    private Page page = Page.HOME;
    private Page backPage = Page.HOME;
    private String selected = "";
    /** AMOUNT page sells back instead of buying. */
    private boolean selling;
    private String input = "";
    private long processingSince;
    private AtmState pendingResult;
    private long dispenseStart;
    private AtmState lastResult;
    private String errorTitle = "", errorText = "";
    private String headerMsg = "";
    private int headerColor = OK;
    private long headerUntil;
    private String pressedKey = "";
    private long pressedUntil;
    private final List<Btn> buttons = new ArrayList<>();

    public AtmScreen(AtmState state) {
        super(Component.translatable("block.tog_yks-cards.atm"));
        this.state = state;
    }

    public static void registerReceiver() {
        ClientPlayNetworking.registerGlobalReceiver(AtmState.TYPE, (payload, context) -> context.client().execute(() -> {
            Minecraft mc = context.client();
            if (mc.gui.screen() instanceof AtmScreen open && open.state.pos().equals(payload.pos())) {
                open.update(payload);
            } else if (payload.open()) {
                mc.gui.setScreen(new AtmScreen(payload));
                sound(TableSounds.ATM_CHIME, 1.0f);
            }
        }));
    }

    static void sound(SoundEvent e, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(e, pitch, 0.6f));
    }

    private void update(AtmState s) {
        this.state = s;
        switch (s.result()) {
            case AtmPayloads.RESULT_DISPENSED, AtmPayloads.RESULT_SOLD, AtmPayloads.RESULT_ERROR, AtmPayloads.RESULT_PRICE_CHANGED -> {
                if (page == Page.PROCESSING) pendingResult = s;
                else if (s.result() == AtmPayloads.RESULT_ERROR || s.result() == AtmPayloads.RESULT_PRICE_CHANGED) showError(s);
            }
            case AtmPayloads.RESULT_INFO -> header(s.message(), OK);
            default -> {}
        }
        if (page == Page.SERVICE && !s.manage()) page = Page.HOME;
    }

    private void send(int action, String item, int a, int b, int c) {
        ClientPlayNetworking.send(new AtmPayloads.Action(state.pos(), action, item, a, b, c));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------------ data helpers

    private PriceEntry price(String id) {
        for (PriceEntry e : state.prices()) if (e.id().equals(id)) return e;
        return new PriceEntry(id, 0, 0);
    }

    private static boolean onSale(PriceEntry e) {
        return e.diamonds() > 0 && e.amount() > 0;
    }

    private static long cost(PriceEntry e, long qty) {
        return e.amount() <= 0 ? 0 : (qty * e.diamonds() + e.amount() - 1) / e.amount();
    }

    static ItemStack stack(String id) {
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
        return new ItemStack(item);
    }

    static boolean isCoin(String id) {
        return id.endsWith("_coin");
    }

    /** "Red Chip" -> "Red", "Copper Coin" -> "Copper" (short names for small places). */
    static String shortName(String id) {
        String n = stack(id).getHoverName().getString();
        return n.replace(" Chip", "").replace(" Coin", "");
    }

    private List<PriceEntry> catalog(boolean coins) {
        List<PriceEntry> list = new ArrayList<>();
        for (PriceEntry e : state.prices()) if (isCoin(e.id()) == coins) list.add(e);
        return list;
    }

    private boolean coinsAvailable() {
        if (!state.coins()) return false;
        for (PriceEntry e : catalog(true)) if (onSale(e)) return true;
        return false;
    }

    private int qty() {
        try {
            return input.isEmpty() ? 0 : Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** "1:1" style price used in the small chip grid. */
    static String ratio(PriceEntry e) {
        return onSale(e) ? e.amount() + ":" + e.diamonds() : "--";
    }

    static String priceText(PriceEntry e) {
        if (!onSale(e)) return "not available";
        return e.amount() + " for " + e.diamonds() + (e.diamonds() == 1 ? " diamond" : " diamonds");
    }

    private String fit(String s, int w) {
        if (font.width(s) <= w) return s;
        return font.plainSubstrByWidth(s, w - font.width("..")) + "..";
    }

    // ------------------------------------------------------------------ navigation

    private void go(Page p) {
        page = p;
        if (p == Page.HOME || p == Page.SELECT_CHIPS || p == Page.SELECT_COINS) input = "";
    }

    private void header(String msg, int color) {
        headerMsg = msg;
        headerColor = color;
        headerUntil = System.currentTimeMillis() + 3500;
    }

    private void showError(AtmState s) {
        boolean changed = s.result() == AtmPayloads.RESULT_PRICE_CHANGED;
        errorTitle = changed ? "Price changed" : "Sorry";
        errorText = s.message();
        backPage = changed ? selectPage() : Page.AMOUNT;
        page = Page.ERROR;
        sound(TableSounds.ATM_ERROR, 1.0f);
    }

    private Page selectPage() {
        return selling ? Page.SELL_SELECT : isCoin(selected) ? Page.SELECT_COINS : Page.SELECT_CHIPS;
    }

    private int held(String id) {
        List<PriceEntry> p = state.prices();
        for (int i = 0; i < p.size() && i < state.held().size(); i++) if (p.get(i).id().equals(id)) return state.held().get(i);
        return 0;
    }

    private boolean holdsAny() {
        for (int h : state.held()) if (h > 0) return true;
        return false;
    }

    private static long payout(PriceEntry e, long qty) {
        return e.amount() <= 0 ? 0 : qty * e.diamonds() / e.amount();
    }

    private void chooseSell(PriceEntry e) {
        if (!onSale(e)) {
            sound(TableSounds.ATM_ERROR, 1.3f);
            header("Not taken right now", WARN);
            return;
        }
        selected = e.id();
        selling = true;
        input = String.valueOf(held(e.id()));
        if (input.equals("0")) input = "";
        page = Page.AMOUNT;
    }

    private void choose(PriceEntry e) {
        if (!onSale(e)) {
            sound(TableSounds.ATM_ERROR, 1.3f);
            header("Not for sale right now", WARN);
            return;
        }
        selected = e.id();
        selling = false;
        input = String.valueOf(e.amount());     // one "bundle" ready to buy
        page = Page.AMOUNT;
    }

    private void addQty(int n) {
        long q = (long) qty() + n;
        input = String.valueOf(Math.max(0, Math.min(MAX_QTY, q)));
        if (input.equals("0")) input = "";
    }

    private void maxQty() {
        PriceEntry p = price(selected);
        long max = selling ? held(selected) : p.diamonds() <= 0 ? 0 : (long) state.diamonds() * p.amount() / p.diamonds();
        input = String.valueOf(Math.min(MAX_QTY, max));
        if (input.equals("0")) input = "";
    }

    private void confirm() {
        int q = qty();
        if (q <= 0) {
            header("Type an amount first", WARN);
            sound(TableSounds.ATM_ERROR, 1.2f);
            return;
        }
        PriceEntry p = price(selected);
        page = Page.PROCESSING;
        processingSince = System.currentTimeMillis();
        pendingResult = null;
        send(selling ? AtmPayloads.SELL : AtmPayloads.BUY, selected, q, p.diamonds(), p.amount());
    }

    private void typeDigit(char c) {
        if (page != Page.AMOUNT) return;
        if (input.length() >= 4 || (input.isEmpty() && c == '0')) return;
        input += c;
        if (qty() > MAX_QTY) input = String.valueOf(MAX_QTY);
    }

    private void cancelKey() {
        switch (page) {
            case HOME -> onClose();
            case PROCESSING -> {}
            case AMOUNT -> go(selectPage());
            case ERROR -> go(backPage);
            default -> go(Page.HOME);
        }
    }

    private void clearKey() {
        if (page == Page.AMOUNT && !input.isEmpty()) input = input.substring(0, input.length() - 1);
    }

    private void enterKey() {
        switch (page) {
            case AMOUNT -> confirm();
            case ERROR -> go(backPage);
            case DISPENSED -> go(Page.HOME);
            default -> {}
        }
    }

    // ------------------------------------------------------------------ buttons per page

    private void row(int side, int row, String label, int style, Runnable action) {
        int x = side == 0 ? COL_L : COL_R;
        buttons.add(new Btn(x, KEY_ROWS[row], COL_W, KEY_H, label, style, action, side, row));
    }

    private void layout() {
        buttons.clear();
        switch (page) {
            case HOME -> {
                if (!state.bankName().isEmpty()) {
                    row(0, 1, "Buy chips", Btn.GREEN, () -> go(Page.SELECT_CHIPS));
                    if (coinsAvailable()) row(1, 1, "Buy coins", Btn.GREEN, () -> go(Page.SELECT_COINS));
                    if (holdsAny()) row(0, 2, "Cash out", Btn.NORMAL, () -> go(Page.SELL_SELECT));
                }
                if (state.manage()) row(0, 3, "Service", Btn.GREY, () -> go(Page.SERVICE));
                row(1, 3, "Exit", Btn.GREY, this::onClose);
            }
            case SELECT_CHIPS, SELECT_COINS, SELL_SELECT -> row(0, 3, "Back", Btn.GREY, () -> go(Page.HOME));
            case AMOUNT -> {
                int y = KEY_ROWS[1], w = 44;
                buttons.add(new Btn(COL_L, y, w, KEY_H, "-1", Btn.NORMAL, () -> addQty(-1), 0, 1));
                buttons.add(new Btn(COL_L + 47, y, w, KEY_H, "+1", Btn.NORMAL, () -> addQty(1), -1, 1));
                buttons.add(new Btn(COL_R, y, w, KEY_H, "+10", Btn.NORMAL, () -> addQty(10), -1, 1));
                buttons.add(new Btn(COL_R + 47, y, w, KEY_H, "Max", Btn.NORMAL, this::maxQty, 1, 1));
                row(0, 3, "Back", Btn.GREY, () -> go(selectPage()));
                row(1, 3, selling ? "Cash out" : "Buy", Btn.GREEN, this::confirm);
            }
            case DISPENSED -> {
                row(0, 3, "More", Btn.NORMAL, () -> go(Page.HOME));
                row(1, 3, "Done", Btn.GREEN, this::onClose);
            }
            case ERROR -> {
                row(0, 3, "Back", Btn.GREY, () -> go(backPage));
                row(1, 3, "Exit", Btn.GREY, this::onClose);
            }
            case SERVICE -> {
                row(0, 0, "Empty vault", Btn.NORMAL, () -> send(AtmPayloads.EMPTY_VAULT, "", 0, 0, 0));
                row(0, 1, "Link chest", Btn.NORMAL, () -> {
                    send(AtmPayloads.LINK_START, "", 0, 0, 0);
                    onClose();
                });
                if (state.linkStatus() != AtmPayloads.LINK_NONE) {
                    row(0, 2, "Unlink", Btn.NORMAL, () -> send(AtmPayloads.UNLINK, "", 0, 0, 0));
                    row(1, 1, "Show link", Btn.NORMAL, () -> send(AtmPayloads.SHOW_LINK, "", 0, 0, 0));
                }
                row(1, 0, "Coins: " + (state.coins() ? "on" : "off"), state.coins() ? Btn.GREEN : Btn.GREY,
                        () -> send(AtmPayloads.TOGGLE_COINS, "", 0, 0, 0));
                row(1, 2, "Link bank", state.bankName().isEmpty() ? Btn.GREEN : Btn.NORMAL, () -> {
                    send(AtmPayloads.LINK_BANK, "", 0, 0, 0);
                    onClose();
                });
                row(0, 3, "Back", Btn.GREY, () -> go(Page.HOME));
                row(1, 3, "Exit", Btn.GREY, this::onClose);
            }
            default -> {}
        }
    }

    // ------------------------------------------------------------------ input

    private int x0() {
        return (width - W) / 2;
    }

    private int y0() {
        return (height - H) / 2;
    }

    private static boolean in(double x, double y, int x0, int y0, int w, int h) {
        return x >= x0 && x < x0 + w && y >= y0 && y < y0 + h;
    }

    private void press(String key) {
        pressedKey = key;
        pressedUntil = System.currentTimeMillis() + 140;
        sound(TableSounds.ATM_BEEP, 1.0f);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // 26.x uses SDL: the left mouse button is 1 (InputConstants.MOUSE_BUTTON_LEFT), not 0 like GLFW
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return super.mouseClicked(event, doubleClick);
        double mx = event.x() - x0(), my = event.y() - y0();
        layout();
        // touch buttons on the display
        for (int i = 0; i < buttons.size(); i++) {
            Btn b = buttons.get(i);
            if (b.hit(mx, my)) {
                press("B" + i);
                b.action().run();
                return true;
            }
        }
        // grey keys next to the display press the button in that row
        for (int r = 0; r < 4; r++) {
            for (int side = 0; side < 2; side++) {
                if (in(mx, my, side == 0 ? KEY_L_X : KEY_R_X, KEY_ROWS[r], KEY_W, KEY_H)) {
                    press((side == 0 ? "L" : "R") + r);
                    for (Btn b : buttons) {
                        if (b.side() == side && b.row() == r) {
                            b.action().run();
                            break;
                        }
                    }
                    return true;
                }
            }
        }
        // keypad
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 3; c++) {
                if (r == 3 && c != 1) continue;
                if (in(mx, my, DIGIT_X[c], PAD_Y[r], DIGIT_W, PAD_H)) {
                    char d = r == 3 ? '0' : (char) ('1' + r * 3 + c);
                    press("D" + d);
                    typeDigit(d);
                    return true;
                }
            }
            if (r < 3 && in(mx, my, FUNC_X, PAD_Y[r], FUNC_W, PAD_H)) {
                press("F" + r);
                if (r == 0) cancelKey();
                else if (r == 1) clearKey();
                else enterKey();
                return true;
            }
        }
        // touch grid: chips / coins
        if (page == Page.SELECT_CHIPS || page == Page.SELECT_COINS || page == Page.SELL_SELECT) {
            for (Cell cell : cells()) {
                if (in(mx, my, cell.x, cell.y, Cell.W, Cell.H)) {
                    press("C" + cell.entry.id());
                    if (page == Page.SELL_SELECT) chooseSell(cell.entry);
                    else choose(cell.entry);
                    return true;
                }
            }
        }
        // clicking the amount box clears it, like a real terminal
        if (page == Page.AMOUNT && in(mx, my, AMOUNT_X, AMOUNT_Y, AMOUNT_W, AMOUNT_H)) {
            press("A");
            input = "";
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    /** Digit for a key (SDL scancodes in 26.x: KEY_1..KEY_9, KEY_0, numpad), or 0. */
    private static char digit(int k) {
        if (k >= InputConstants.KEY_1 && k <= InputConstants.KEY_9) return (char) ('1' + (k - InputConstants.KEY_1));
        if (k == InputConstants.KEY_0 || k == InputConstants.KEY_NUMPAD0) return '0';
        if (k >= InputConstants.KEY_NUMPAD1 && k <= InputConstants.KEY_NUMPAD9) return (char) ('1' + (k - InputConstants.KEY_NUMPAD1));
        return 0;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int k = event.key();
        char d = digit(k);
        if (d != 0) {
            press("D" + d);
            typeDigit(d);
            return true;
        }
        if (k == InputConstants.KEY_RETURN || k == InputConstants.KEY_NUMPADENTER) {
            press("F2");
            enterKey();
            return true;
        }
        if (k == InputConstants.KEY_BACKSPACE || k == InputConstants.KEY_DELETE) {
            press("F1");
            clearKey();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void tick() {
        if (page == Page.PROCESSING && pendingResult != null && System.currentTimeMillis() - processingSince > 900) {
            AtmState r = pendingResult;
            pendingResult = null;
            if (r.result() == AtmPayloads.RESULT_DISPENSED || r.result() == AtmPayloads.RESULT_SOLD) {
                lastResult = r;
                page = Page.DISPENSED;
                dispenseStart = System.currentTimeMillis();
            } else {
                showError(r);
            }
        }
        if (page == Page.PROCESSING && System.currentTimeMillis() - processingSince > 8000) {
            errorTitle = "Sorry";
            errorText = "No answer from the bank. Please try again.";
            backPage = Page.AMOUNT;
            page = Page.ERROR;
        }
    }

    // ------------------------------------------------------------------ touch grid

    record Cell(PriceEntry entry, int x, int y) {
        static final int W = 24, H = 27;
    }

    private List<Cell> cells() {
        List<Cell> out = new ArrayList<>();
        boolean coins = page == Page.SELECT_COINS;
        if (page == Page.SELL_SELECT) {
            List<PriceEntry> mine = new ArrayList<>();
            for (PriceEntry e : state.prices()) if (held(e.id()) > 0) mine.add(e);
            for (int i = 0; i < mine.size(); i++) out.add(new Cell(mine.get(i), LCD_X0 + (i % 8) * Cell.W, 38 + (i / 8) * Cell.H));
            return out;
        }
        List<PriceEntry> list = catalog(coins);
        if (coins) {
            int gap = 48, total = (list.size() - 1) * gap + Cell.W;
            int sx = (LCD_X0 + LCD_X1) / 2 - total / 2;
            for (int i = 0; i < list.size(); i++) out.add(new Cell(list.get(i), sx + i * gap, 50));
        } else {
            for (int i = 0; i < list.size(); i++) {
                out.add(new Cell(list.get(i), LCD_X0 + (i % 8) * Cell.W, 38 + (i / 8) * Cell.H));
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0x70000000);
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x0(), y0(), 0, 0, W, H, W, H);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
        super.extractRenderState(g, mx, my, pt);
        int x0 = x0(), y0 = y0();
        long now = System.currentTimeMillis();
        layout();

        g.pose().pushMatrix();
        g.pose().translate(x0, y0);
        int rx = mx - x0, ry = my - y0;

        // header: name plate + owner or a short message
        g.text(font, Component.literal("ATM").withStyle(net.minecraft.ChatFormatting.BOLD), 10, 6, 0xFFE8C25A, true);
        boolean msg = now < headerUntil;
        String hdr = msg ? headerMsg : state.bankName().isEmpty() ? "Out of service" : state.bankName();
        hdr = fit(hdr, W - 50);
        g.text(font, hdr, W - 10 - font.width(hdr), 6, msg ? headerColor : 0xFF9DB0D0, false);

        // panel labels
        g.centeredText(font, Component.literal("DIAMONDS"), 34, 131, 0xFF4A4E58);
        g.centeredText(font, Component.literal("CHIPS"), 240, 131, 0xFF4A4E58);

        // hardware keys: hover + press feedback, labels
        for (int i = 0; i < 4; i++) {
            keyFx(g, "L" + i, KEY_L_X, KEY_ROWS[i], KEY_W, KEY_H, rx, ry, now);
            keyFx(g, "R" + i, KEY_R_X, KEY_ROWS[i], KEY_W, KEY_H, rx, ry, now);
        }
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 3; c++) {
                if (r == 3 && c != 1) continue;
                char d = r == 3 ? '0' : (char) ('1' + r * 3 + c);
                keyFx(g, "D" + d, DIGIT_X[c], PAD_Y[r], DIGIT_W, PAD_H, rx, ry, now);
                int off = pressedKey.equals("D" + d) && now < pressedUntil ? 1 : 0;
                g.centeredText(font, Component.literal(String.valueOf(d)), DIGIT_X[c] + DIGIT_W / 2 + off, PAD_Y[r] + 4 + off, 0xFF30333A);
            }
            if (r < 3) {
                keyFx(g, "F" + r, FUNC_X, PAD_Y[r], FUNC_W, PAD_H, rx, ry, now);
                String lbl = r == 0 ? "CANCEL" : r == 1 ? "CLEAR" : "ENTER";
                int off = pressedKey.equals("F" + r) && now < pressedUntil ? 1 : 0;
                g.text(font, lbl, FUNC_X + FUNC_W / 2 - font.width(lbl) / 2 + off, PAD_Y[r] + 4 + off, r == 1 ? 0xFF3A3000 : 0xFFFFFFFF, r != 1);
            }
        }

        // LED next to the diamond slot: blinks while processing
        boolean led = page != Page.PROCESSING || (now / 150) % 2 == 0;
        g.fill(48, 164, 54, 168, led ? (page == Page.ERROR ? 0xFFFF4040 : 0xFF40F070) : 0xFF1A4022);

        // display
        g.enableScissor(LCD_X0, LCD_Y0, LCD_X1, LCD_Y1);
        drawPage(g, rx, ry, now);
        for (int i = 0; i < buttons.size(); i++) drawButton(g, buttons.get(i), "B" + i, rx, ry, now);
        g.disableScissor();

        drawTray(g, now);
        g.pose().popMatrix();

        // tooltips (screen coordinates)
        if (page == Page.SELECT_CHIPS || page == Page.SELECT_COINS || page == Page.SELL_SELECT) {
            for (Cell cell : cells()) {
                if (in(rx, ry, cell.x, cell.y, Cell.W, Cell.H)) {
                    g.setComponentTooltipForNextFrame(font, List.of(stack(cell.entry.id()).getHoverName(),
                            Component.literal("§7" + priceText(cell.entry))), mx, my);
                }
            }
        }
    }

    private void keyFx(GuiGraphicsExtractor g, String id, int x, int y, int w, int h, int rx, int ry, long now) {
        if (pressedKey.equals(id) && now < pressedUntil) g.fill(x, y, x + w, y + h, 0x55000000);
        else if (in(rx, ry, x, y, w, h)) g.fill(x, y, x + w, y + h, 0x30FFFFFF);
    }

    private void drawButton(GuiGraphicsExtractor g, Btn b, String id, int rx, int ry, long now) {
        boolean hover = b.hit(rx, ry) || (b.side() >= 0 && in(rx, ry, b.side() == 0 ? KEY_L_X : KEY_R_X, KEY_ROWS[b.row()], KEY_W, KEY_H));
        boolean pressed = now < pressedUntil && (pressedKey.equals(id)
                || (b.side() >= 0 && pressedKey.equals((b.side() == 0 ? "L" : "R") + b.row())));
        int bg, edge;
        switch (b.style()) {
            case Btn.GREEN -> { bg = hover ? GO_HOVER : GO; edge = GO_EDGE; }
            case Btn.GREY -> { bg = hover ? BACK_HOVER : BACK; edge = BACK_EDGE; }
            default -> { bg = hover ? BTN_HOVER : BTN; edge = BTN_EDGE; }
        }
        int x = b.x(), y = b.y(), x1 = x + b.w(), y1 = y + b.h();
        int off = pressed ? 1 : 0;
        g.fill(x + 1, y, x1 - 1, y1, edge);                      // rounded edge
        g.fill(x, y + 1, x1, y1 - 1, edge);
        g.fill(x + 1, y + 1, x1 - 1, y1 - 1, pressed ? 0xFF0C1A38 : bg);
        if (!pressed) g.fill(x + 1, y + 1, x1 - 1, y + 2, 0x30FFFFFF);   // top shine
        String label = fit(b.label(), b.w() - 6);
        g.text(font, label, x + b.w() / 2 - font.width(label) / 2 + off, y + 3 + off, 0xFFFFFFFF, false);
    }

    private void title(GuiGraphicsExtractor g, String text, int color) {
        g.text(font, fit(text, 120), LCD_X0 + 4, 27, color, false);
    }

    /** Diamond balance top right of the display. */
    private void balance(GuiGraphicsExtractor g) {
        String b = String.valueOf(state.diamonds());
        int w = font.width(b);
        g.text(font, b, LCD_X1 - 14 - w, 27, TXT, false);
        g.pose().pushMatrix();
        g.pose().translate(LCD_X1 - 12, 26);
        g.pose().scale(0.625f, 0.625f);
        g.item(new ItemStack(Items.DIAMOND), 0, 0);
        g.pose().popMatrix();
    }

    private void center(GuiGraphicsExtractor g, String text, int y, int color) {
        text = fit(text, LCD_X1 - LCD_X0 - 8);
        g.text(font, text, (LCD_X0 + LCD_X1) / 2 - font.width(text) / 2, y, color, false);
    }

    private void drawPage(GuiGraphicsExtractor g, int rx, int ry, long now) {
        int cx = (LCD_X0 + LCD_X1) / 2;
        switch (page) {
            case HOME -> {
                if (state.bankName().isEmpty()) {
                    title(g, "Out of service", ERR);
                    center(g, "This ATM has no bank yet", 48, TXT);
                    center(g, state.manage() ? "Service > Link bank" : "Please try another ATM", 62, DIM);
                    break;
                }
                title(g, "Welcome!", HI);
                balance(g);
                String[] deco = {"tog_yks-cards:red_chip", "tog_yks-cards:black_chip", "tog_yks-cards:green_chip"};
                for (int i = 0; i < 3; i++) g.item(stack(deco[i]), cx - 28 + i * 20, 81 - (i == 1 ? 3 : 0));
                center(g, "What would you like?", 40, DIM);
            }
            case SELL_SELECT -> {
                title(g, "Cash out", HI);
                balance(g);
                for (Cell cell : cells()) {
                    boolean hover = in(rx, ry, cell.x, cell.y, Cell.W, Cell.H);
                    if (hover) g.fill(cell.x, cell.y, cell.x + Cell.W, cell.y + Cell.H, 0x40FFFFFF);
                    g.item(stack(cell.entry.id()), cell.x + 4, cell.y + 1);
                    String n = "x" + held(cell.entry.id());
                    if (font.width(n) > Cell.W) n = String.valueOf(held(cell.entry.id()));
                    g.text(font, n, cell.x + Cell.W / 2 - font.width(n) / 2, cell.y + 18, TXT, false);
                }
                String hint = "Your chips from here";
                g.text(font, hint, COL_R + COL_W - font.width(hint), KEY_ROWS[3] + 3, DIM, false);
            }
            case SELECT_CHIPS, SELECT_COINS -> {
                title(g, page == Page.SELECT_CHIPS ? "Pick a chip" : "Pick a coin", HI);
                balance(g);
                for (Cell cell : cells()) {
                    boolean sale = onSale(cell.entry);
                    boolean hover = in(rx, ry, cell.x, cell.y, Cell.W, Cell.H);
                    if (hover && sale) g.fill(cell.x, cell.y, cell.x + Cell.W, cell.y + Cell.H, 0x40FFFFFF);
                    g.item(stack(cell.entry.id()), cell.x + 4, cell.y + 1);
                    if (!sale) g.fill(cell.x + 2, cell.y, cell.x + Cell.W - 2, cell.y + 18, 0xA0102040);
                    String p = ratio(cell.entry);
                    g.text(font, p, cell.x + Cell.W / 2 - font.width(p) / 2, cell.y + 18, sale ? TXT : DIM, false);
                    if (page == Page.SELECT_COINS) {
                        String n = shortName(cell.entry.id());
                        g.text(font, n, cell.x + Cell.W / 2 - font.width(n) / 2, cell.y + 28, DIM, false);
                    }
                }
                String hint = page == Page.SELECT_COINS ? "coins : diamonds" : "chips : diamonds";
                g.text(font, hint, COL_R + COL_W - font.width(hint), KEY_ROWS[3] + 3, DIM, false);
            }
            case AMOUNT -> {
                PriceEntry p = price(selected);
                g.item(stack(selected), LCD_X0 + 3, LCD_Y0 + 1);
                g.text(font, fit(shortName(selected) + ": " + (selling ? "sell " : "") + priceText(p), 122), LCD_X0 + 21, 27, HI, false);
                balance(g);
                // amount box
                g.text(font, "Amount", COL_L, AMOUNT_Y + 4, DIM, false);
                g.fill(AMOUNT_X, AMOUNT_Y, AMOUNT_X + AMOUNT_W, AMOUNT_Y + AMOUNT_H, 0xFF0A1630);
                g.outline(AMOUNT_X, AMOUNT_Y, AMOUNT_W, AMOUNT_H, 0xFF6F9BE0);
                String shown = input.isEmpty() ? "0" : input;
                boolean caret = (now / 400) % 2 == 0;
                g.text(font, shown + (caret ? "_" : ""), AMOUNT_X + AMOUNT_W / 2 - font.width(shown) / 2, AMOUNT_Y + 4, 0xFFFFFFFF, false);
                int q = qty();
                long c = selling ? payout(p, q) : cost(p, q);
                boolean enough = selling ? q <= held(selected) && c >= 1 : c <= state.diamonds();
                String costText = (selling ? "You get: " : "Cost: ") + c;
                g.text(font, costText, LCD_X1 - 16 - font.width(costText), AMOUNT_Y + 4, q == 0 ? DIM : enough ? TXT : ERR, false);
                g.pose().pushMatrix();
                g.pose().translate(LCD_X1 - 14, AMOUNT_Y + 3);
                g.pose().scale(0.625f, 0.625f);
                g.item(new ItemStack(Items.DIAMOND), 0, 0);
                g.pose().popMatrix();
                // hint line
                String hint;
                int hc = DIM;
                if (selling) {
                    long used = p.diamonds() <= 0 ? 0 : (c * p.amount() + p.diamonds() - 1) / p.diamonds();
                    if (q > held(selected)) {
                        hint = "You only have " + held(selected);
                        hc = ERR;
                    } else if (q > 0 && c < 1) {
                        hint = "Too few for 1 diamond";
                        hc = ERR;
                    } else if (q > used) {
                        hint = (q - used) + " stay with you (not a full diamond)";
                        hc = WARN;
                    } else {
                        hint = "You have " + held(selected) + ", then Cash out";
                    }
                } else if (q > 0 && !enough) {
                    hint = "Not enough diamonds";
                    hc = ERR;
                } else if (q > 0 && p.amount() > 0 && q % p.amount() != 0 && c * p.amount() / p.diamonds() <= MAX_QTY) {
                    hint = "Tip: " + (c * p.amount() / p.diamonds()) + " cost the same";
                    hc = WARN;
                } else {
                    hint = "Type with the keypad, then Buy";
                }
                center(g, hint, KEY_ROWS[2] + 3, hc);
            }
            case PROCESSING -> {
                title(g, "Please wait", HI);
                int dots = (int) ((now - processingSince) / 300 % 4);
                center(g, "Counting your chips" + ".".repeat(dots), 48, TXT);
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4;
                    int px = cx + (int) Math.round(Math.cos(a) * 10), py = 84 + (int) Math.round(Math.sin(a) * 10);
                    int lit = (int) ((now / 110) % 8);
                    int col = i == lit ? 0xFFFFFFFF : ((i + 1) % 8 == lit || (i + 2) % 8 == lit) ? 0xFF9FB8E0 : 0xFF3A5280;
                    g.fill(px - 1, py - 1, px + 2, py + 2, col);
                }
            }
            case DISPENSED -> {
                title(g, "Done!", OK);
                balance(g);
                AtmState r = lastResult;
                if (r != null) {
                    String what = r.resultQty() + "x " + shortName(r.resultItem());
                    int w = font.width(what) + 18;
                    g.item(stack(r.resultItem()), cx - w / 2, 42);
                    g.text(font, what, cx - w / 2 + 18, 46, HI, false);
                    boolean sold = r.result() == AtmPayloads.RESULT_SOLD;
                    String money = r.resultCost() + (r.resultCost() == 1 ? " diamond" : " diamonds");
                    center(g, sold ? "Sold - you got " + money : "Paid " + money, 64, sold ? OK : DIM);
                    center(g, sold ? "Thank you for playing!" : "Added to your inventory", 80, OK);
                }
            }
            case ERROR -> {
                title(g, errorTitle, errorTitle.equals("Sorry") ? ERR : WARN);
                List<FormattedCharSequence> lines = font.split(Component.literal(errorText), LCD_X1 - LCD_X0 - 16);
                int y = 44;
                for (FormattedCharSequence line : lines) {
                    g.centeredText(font, line, cx, y, TXT);
                    y += 10;
                }
            }
            case SERVICE -> {
                title(g, "Service", WARN);
                String t = state.bankName().isEmpty() ? "no bank" : state.bankName();
                t = fit(t, 80);
                g.text(font, t, LCD_X1 - 4 - font.width(t), 27, DIM, false);
                String vault = fit("Vault " + state.vault() + "  +" + state.turnover() + "/-" + state.paidOut(), COL_W);
                g.text(font, vault, COL_L, 36, state.vault() >= state.vaultCapacity() ? ERR : TXT, false);
                String st;
                int sc;
                switch (state.linkStatus()) {
                    case AtmPayloads.LINK_OK -> { st = "Chest: linked"; sc = OK; }
                    case AtmPayloads.LINK_FULL -> { st = "Chest: full!"; sc = ERR; }
                    case AtmPayloads.LINK_MISSING -> { st = "Chest: missing!"; sc = ERR; }
                    case AtmPayloads.LINK_UNLOADED -> { st = "Chest: far away"; sc = WARN; }
                    default -> { st = "No chest linked"; sc = DIM; }
                }
                g.text(font, st, COL_R, 36, sc, false);

            }
        }
    }

    /** Chips drop out of the dispenser and pile up in the tray. */
    private void drawTray(GuiGraphicsExtractor g, long now) {
        if (page != Page.DISPENSED || lastResult == null) return;
        ItemStack chip = stack(lastResult.resultItem());
        int n = Math.min(9, Math.max(1, lastResult.resultQty() / 4 + 1));
        float t = (now - dispenseStart) / 1000f;
        g.enableScissor(TRAY_X0, TRAY_Y0, TRAY_X1, TRAY_Y1);
        for (int i = 0; i < n; i++) {
            float start = 0.25f + i * 0.12f;
            if (t < start) continue;
            float k = Math.min(1f, (t - start) / 0.35f);
            int restX = TRAY_X0 + 4 + (i % 3) * 15 + (i / 3) * 3;
            int restY = TRAY_Y1 - 18 - (i / 3) * 5;
            float y = TRAY_Y0 - 16 + (restY - (TRAY_Y0 - 16)) * k * k;
            g.item(chip, restX, Math.round(y));
        }
        g.disableScissor();
        if (t < 1.6f && (now / 120) % 2 == 0) g.fill(TRAY_X0 - 1, TRAY_Y0 - 3, TRAY_X1 + 1, TRAY_Y0 - 2, 0xFF60FF90);
    }
}
