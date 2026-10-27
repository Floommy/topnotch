package net.togyk.tcards.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.togyk.tcards.TableSounds;
import net.togyk.tcards.restaurant.RestaurantMenu;
import net.togyk.tcards.restaurant.RestaurantPayloads;
import net.togyk.tcards.restaurant.RestaurantPayloads.OrderInfo;
import net.togyk.tcards.restaurant.RestaurantPayloads.State;

/**
 * Self-order kiosk like in a real McDonald's: categories on the left, big pictures with prices,
 * "My order" bar at the bottom, then Pay and your number. The register uses the same screen
 * with tabs: open orders (Done / Picked up / Cancel), new order and settings.
 */
@Environment(EnvType.CLIENT)
public class RestaurantScreen extends Screen {
    static final int W = 300, H = 212;
    static final int RED = 0xFFDA291C, YELLOW = 0xFFFFC72C, BG = 0xFFF7F7F7, DARK = 0xFF27251F, GREY = 0xFF8A8A8A,
            SIDE = 0xFFEDEDED, CARD = 0xFFFFFFFF, EDGE = 0xFFDADADA, GREEN = 0xFF27873A, GREEN_HI = 0xFF31A748;
    static final int SIDE_W = 80, BAR_Y = 176, TOP = 24;

    enum Page { MENU, CART, DONE, ORDERS }

    private State state;
    private Page page;
    private int category;
    private final Map<String, Integer> cart = new LinkedHashMap<>();
    private int scroll;
    private int doneNumber;
    private long doneAt;
    private String banner = "";
    private int bannerColor = RED;
    private long bannerUntil;
    private boolean waiting;

    /** Clickable areas of the current frame. */
    private record Hit(int x, int y, int w, int h, Runnable action) {
        boolean in(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private final List<Hit> hits = new ArrayList<>();

    public RestaurantScreen(State state) {
        super(Component.literal(state.name().isEmpty() ? "Restaurant" : state.name()));
        this.state = state;
        this.page = register() ? Page.ORDERS : Page.MENU;
    }

    public static void registerReceiver() {
        ClientPlayNetworking.registerGlobalReceiver(State.TYPE, (payload, context) -> context.client().execute(() -> {
            Minecraft mc = context.client();
            if (mc.gui.screen() instanceof RestaurantScreen open && open.state.pos().equals(payload.pos())) {
                open.update(payload);
            } else if (mc.gui.screen() instanceof RestaurantSettingsScreen settings && settings.pos().equals(payload.pos())) {
                settings.update(payload);
            } else if (payload.open()) {
                mc.gui.setScreen(new RestaurantScreen(payload));
            }
        }));
    }

    private boolean register() {
        return state.kind() == RestaurantPayloads.KIND_REGISTER;
    }

    void update(State s) {
        state = s;
        switch (s.result()) {
            case RestaurantPayloads.RESULT_ORDERED -> {
                waiting = false;
                cart.clear();
                doneNumber = s.number();
                doneAt = System.currentTimeMillis();
                page = Page.DONE;
                sound(TableSounds.ATM_CHIME, 1.2f);
            }
            case RestaurantPayloads.RESULT_ERROR -> {
                waiting = false;
                say(s.message(), RED);
                sound(TableSounds.ATM_ERROR, 1.0f);
            }
            case RestaurantPayloads.RESULT_INFO -> say(s.message(), GREEN);
            default -> {}
        }
    }

    private void say(String msg, int color) {
        banner = msg;
        bannerColor = color;
        bannerUntil = System.currentTimeMillis() + 3500;
    }

    static void sound(net.minecraft.sounds.SoundEvent e, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(e, pitch, 0.5f));
    }

    private void send(int action, String text, int a) {
        ClientPlayNetworking.send(new RestaurantPayloads.Action(state.pos(), action, text, a));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------------ data

    private int price(String item) {
        for (int i = 0; i < state.items().size(); i++) if (state.items().get(i).equals(item)) return state.prices().get(i);
        return 0;
    }

    private boolean sold(String item) {
        return state.items().contains(item) && price(item) > 0;
    }

    private int cartCount() {
        int n = 0;
        for (int q : cart.values()) n += q;
        return n;
    }

    private long cartTotal() {
        long t = 0;
        for (Map.Entry<String, Integer> e : cart.entrySet()) t += (long) price(e.getKey()) * e.getValue();
        return t;
    }

    private void add(String item, int d) {
        int q = cart.getOrDefault(item, 0) + d;
        if (q <= 0) cart.remove(item);
        else if (cartCount() + d <= 64) cart.put(item, q);
    }

    private void pay() {
        if (cart.isEmpty() || waiting) return;
        if (cartTotal() > state.money()) {
            say("Not enough " + state.currency() + "s", RED);
            sound(TableSounds.ATM_ERROR, 1.1f);
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : cart.entrySet()) {
            if (!sb.isEmpty()) sb.append(',');
            sb.append(e.getKey()).append(':').append(e.getValue());
        }
        waiting = true;
        send(RestaurantPayloads.ORDER, sb.toString(), 0);
    }

    private ItemStack currencyIcon() {
        return new ItemStack("emerald".equals(state.currency()) ? Items.EMERALD : Items.DIAMOND);
    }

    // ------------------------------------------------------------------ input

    private int x0() {
        return (width - W) / 2;
    }

    private int y0() {
        return (height - H) / 2;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return super.mouseClicked(event, doubleClick);
        double mx = event.x() - x0(), my = event.y() - y0();
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (h.in(mx, my)) {
                sound(TableSounds.KIOSK_TAP, 1.0f);
                h.action().run();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        scroll = Math.max(0, scroll - (int) Math.signum(dy));
        return true;
    }

    private long lastRefresh;

    @Override
    public void tick() {
        if (register() && page == Page.ORDERS && System.currentTimeMillis() - lastRefresh > 2000) {
            lastRefresh = System.currentTimeMillis();
            send(RestaurantPayloads.REFRESH, "", 0);
        }
        // the kiosk goes back to the start a few seconds after an order
        if (page == Page.DONE && System.currentTimeMillis() - doneAt > 9000) {
            page = register() ? Page.ORDERS : Page.MENU;
            if (!register()) onClose();
        }
    }

    // ------------------------------------------------------------------ drawing helpers

    private void box(GuiGraphicsExtractor g, int x, int y, int w, int h, int fill, int edge) {
        g.fill(x, y, x + w, y + h, edge);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
    }

    private void button(GuiGraphicsExtractor g, int x, int y, int w, int h, String label, int fill, int text, boolean enabled, Runnable action, int mx, int my) {
        boolean hover = enabled && mx >= x && mx < x + w && my >= y && my < y + h;
        int f = !enabled ? 0xFFC8C8C8 : hover ? brighter(fill) : fill;
        g.fill(x + 1, y, x + w - 1, y + h, f);
        g.fill(x, y + 1, x + w, y + h - 1, f);
        String l = fit(label, w - 4);
        g.text(font, l, x + w / 2 - font.width(l) / 2, y + (h - 8) / 2, enabled ? text : 0xFF888888, false);
        if (enabled && action != null) hits.add(new Hit(x, y, w, h, action));
    }

    private static int brighter(int c) {
        int r = Math.min(255, ((c >> 16) & 255) + 25), g = Math.min(255, ((c >> 8) & 255) + 25), b = Math.min(255, (c & 255) + 25);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private String fit(String s, int w) {
        if (font.width(s) <= w) return s;
        return font.plainSubstrByWidth(s, Math.max(0, w - font.width(".."))) + "..";
    }

    private void icon(GuiGraphicsExtractor g, ItemStack stack, float x, float y, float scale) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(scale, scale);
        g.item(stack, 0, 0);
        g.pose().popMatrix();
    }

    private void money(GuiGraphicsExtractor g, long amount, int rightX, int y, int color) {
        String t = String.valueOf(amount);
        int w = font.width(t);
        g.text(font, t, rightX - 11 - w, y, color, false);
        icon(g, currencyIcon(), rightX - 10, y - 1, 0.625f);
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0x80000000);
        int x0 = x0(), y0 = y0();
        g.fill(x0 - 2, y0 - 2, x0 + W + 2, y0 + H + 2, 0xFF1A1A1A);
        g.fill(x0, y0, x0 + W, y0 + H, BG);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
        super.extractRenderState(g, mx, my, pt);
        hits.clear();
        g.pose().pushMatrix();
        g.pose().translate(x0(), y0());
        int rx = mx - x0(), ry = my - y0();

        // header
        g.fill(0, 0, W, TOP, RED);
        g.pose().pushMatrix();
        g.pose().translate(7, 4);
        g.pose().scale(2f, 2f);
        g.text(font, Component.literal("M").withStyle(ChatFormatting.BOLD), 0, 0, YELLOW, true);
        g.pose().popMatrix();
        String name = state.name().isEmpty() ? "Not linked" : state.name();
        g.text(font, fit(name, register() ? 92 : 170), 30, 8, 0xFFFFFFFF, false);

        if (register()) {
            int tx = 128;
            tab(g, tx, "Orders", page == Page.ORDERS, () -> { page = Page.ORDERS; scroll = 0; }, rx, ry);
            tab(g, tx + 50, "New order", page == Page.MENU || page == Page.CART, () -> { page = Page.MENU; scroll = 0; }, rx, ry);
            if (state.manage()) tab(g, tx + 112, "Settings", false,
                    () -> minecraft.gui.setScreen(new RestaurantSettingsScreen(this, state)), rx, ry);
        } else {
            money(g, state.money(), W - 6, 8, 0xFFFFFFFF);
        }

        if (state.name().isEmpty()) {
            g.centeredText(font, Component.literal("This " + (register() ? "register" : "kiosk") + " is not linked to a restaurant."), W / 2, 80, DARK);
            g.centeredText(font, Component.literal("Sneak + right-click it, then click a register."), W / 2, 94, GREY);
        } else {
            switch (page) {
                case MENU -> drawMenu(g, rx, ry);
                case CART -> drawCart(g, rx, ry);
                case DONE -> drawDone(g, rx, ry);
                case ORDERS -> drawOrders(g, rx, ry);
            }
        }
        if (System.currentTimeMillis() < bannerUntil) {
            g.fill(0, BAR_Y - 14, W, BAR_Y, bannerColor);
            g.centeredText(font, Component.literal(fit(banner, W - 10)), W / 2, BAR_Y - 11, 0xFFFFFFFF);
        }
        g.pose().popMatrix();
    }

    private void tab(GuiGraphicsExtractor g, int x, String label, boolean active, Runnable action, int mx, int my) {
        int w = font.width(label) + 10;
        boolean hover = mx >= x && mx < x + w && my >= 4 && my < 20;
        g.fill(x, 4, x + w, 20, active ? YELLOW : hover ? 0xFFE84A3C : 0xFFB81F14);
        g.text(font, label, x + 5, 8, active ? DARK : 0xFFFFFFFF, false);
        hits.add(new Hit(x, 4, w, 16, action));
    }

    private void drawMenu(GuiGraphicsExtractor g, int mx, int my) {
        // categories
        g.fill(0, TOP, SIDE_W, BAR_Y, SIDE);
        List<RestaurantMenu.Category> cats = RestaurantMenu.CATEGORIES;
        int ch = (BAR_Y - TOP) / cats.size();
        for (int i = 0; i < cats.size(); i++) {
            RestaurantMenu.Category c = cats.get(i);
            int y = TOP + i * ch;
            boolean sel = i == category;
            boolean hover = mx >= 0 && mx < SIDE_W && my >= y && my < y + ch;
            if (sel) {
                g.fill(0, y, SIDE_W, y + ch, 0xFFFFFFFF);
                g.fill(0, y, 3, y + ch, YELLOW);
            } else if (hover) {
                g.fill(0, y, SIDE_W, y + ch, 0xFFF5F5F5);
            }
            icon(g, RestaurantMenu.stack(c.icon()), 5, y + (ch - 16) / 2f, 1f);
            g.text(font, fit(c.name(), SIDE_W - 23), 23, y + (ch - 8) / 2, sel ? DARK : 0xFF555555, false);
            final int idx = i;
            hits.add(new Hit(0, y, SIDE_W, ch, () -> category = idx));
        }
        // item cards
        RestaurantMenu.Category c = cats.get(Math.min(category, cats.size() - 1));
        g.text(font, Component.literal(c.name()).withStyle(ChatFormatting.BOLD), SIDE_W + 8, TOP + 5, DARK, false);
        int cw = 68, chh = 64, gx = SIDE_W + 6, gy = TOP + 17;
        int col = 0, row = 0;
        for (String item : c.items()) {
            if (!state.items().contains(item)) continue;
            int x = gx + col * (cw + 2), y = gy + row * (chh + 3);
            boolean sold = sold(item);
            boolean hover = sold && mx >= x && mx < x + cw && my >= y && my < y + chh;
            box(g, x, y, cw, chh, hover ? 0xFFFFF8DC : CARD, hover ? YELLOW : EDGE);
            icon(g, RestaurantMenu.stack(item), x + cw / 2f - 16, y + 3, 2f);
            String n = fit(RestaurantMenu.shortName(item), cw - 4);
            g.text(font, n, x + cw / 2 - font.width(n) / 2, y + 38, DARK, false);
            if (sold) {
                money(g, price(item), x + cw / 2 + 12, y + 51, RED);
                hits.add(new Hit(x, y, cw, chh, () -> add(item, 1)));
            } else {
                g.text(font, "sold out", x + cw / 2 - font.width("sold out") / 2, y + 51, GREY, false);
            }
            int inCart = cart.getOrDefault(item, 0);
            if (inCart > 0) {
                String b = String.valueOf(inCart);
                int bw = Math.max(11, font.width(b) + 5);
                g.fill(x + cw - bw - 2, y + 2, x + cw - 2, y + 13, RED);
                g.text(font, b, x + cw - 2 - bw / 2 - font.width(b) / 2, y + 4, 0xFFFFFFFF, false);
            }
            if (++col == 3) {
                col = 0;
                row++;
            }
        }
        bottomBar(g, mx, my, false);
    }

    private void bottomBar(GuiGraphicsExtractor g, int mx, int my, boolean inCart) {
        g.fill(0, BAR_Y, W, H, 0xFFFFFFFF);
        g.fill(0, BAR_Y, W, BAR_Y + 1, EDGE);
        int n = cartCount();
        g.text(font, Component.literal("My order").withStyle(ChatFormatting.BOLD), 8, BAR_Y + 7, DARK, false);
        g.text(font, n == 0 ? "empty" : n + (n == 1 ? " item" : " items"), 8, BAR_Y + 19, GREY, false);
        g.text(font, "Total", 72, BAR_Y + 7, GREY, false);
        long total = cartTotal();
        money(g, total, 112, BAR_Y + 19, total > state.money() && !register() ? RED : DARK);
        if (inCart) {
            button(g, 128, BAR_Y + 8, 76, 20, "Back to menu", 0xFFE6E6E6, DARK, true, () -> page = Page.MENU, mx, my);
        } else {
            button(g, 128, BAR_Y + 8, 76, 20, "View order", 0xFFE6E6E6, DARK, n > 0, () -> { page = Page.CART; scroll = 0; }, mx, my);
        }
        button(g, 210, BAR_Y + 8, 82, 20, waiting ? "Paying..." : "Pay", GREEN, 0xFFFFFFFF, n > 0 && !waiting, this::pay, mx, my);
    }

    private void drawCart(GuiGraphicsExtractor g, int mx, int my) {
        g.text(font, Component.literal("Your order").withStyle(ChatFormatting.BOLD), 10, TOP + 6, DARK, false);
        List<String> items = new ArrayList<>(cart.keySet());
        if (items.isEmpty()) g.text(font, "Nothing yet - pick something from the menu", 10, TOP + 26, GREY, false);
        int rowH = 20, visible = 7;
        scroll = Math.min(scroll, Math.max(0, items.size() - visible));
        for (int i = 0; i < Math.min(visible, items.size() - scroll); i++) {
            String item = items.get(i + scroll);
            int y = TOP + 18 + i * rowH;
            if (i % 2 == 0) g.fill(6, y, W - 6, y + rowH, 0xFFFFFFFF);
            icon(g, RestaurantMenu.stack(item), 10, y + 2, 1f);
            g.text(font, fit(RestaurantMenu.shortName(item), 110), 30, y + 6, DARK, false);
            int q = cart.get(item);
            button(g, 150, y + 3, 14, 14, "-", 0xFFE6E6E6, DARK, true, () -> add(item, -1), mx, my);
            String qs = String.valueOf(q);
            g.text(font, qs, 173 - font.width(qs) / 2, y + 6, DARK, false);
            button(g, 182, y + 3, 14, 14, "+", 0xFFE6E6E6, DARK, true, () -> add(item, 1), mx, my);
            money(g, (long) price(item) * q, 258, y + 6, DARK);
            button(g, 268, y + 3, 22, 14, "x", 0xFFF2D0CC, RED, true, () -> cart.remove(item), mx, my);
        }
        bottomBar(g, mx, my, true);
    }

    private void drawDone(GuiGraphicsExtractor g, int mx, int my) {
        g.centeredText(font, Component.literal("Thank you!").withStyle(ChatFormatting.BOLD), W / 2, TOP + 16, GREEN);
        g.centeredText(font, Component.literal("Your order number"), W / 2, TOP + 34, DARK);
        g.pose().pushMatrix();
        String num = String.valueOf(doneNumber);
        g.pose().translate(W / 2f - font.width(num) * 2.5f, TOP + 48);
        g.pose().scale(5f, 5f);
        g.text(font, num, 0, 0, RED, false);
        g.pose().popMatrix();
        g.centeredText(font, Component.literal(register() ? "The receipt is in your inventory" : "Please take your receipt"), W / 2, TOP + 100, DARK);
        g.centeredText(font, Component.literal("Watch the screen - we call your number"), W / 2, TOP + 114, GREY);
        button(g, W / 2 - 50, TOP + 132, 100, 20, register() ? "Back to orders" : "New order", YELLOW, DARK, true, () -> {
            page = register() ? Page.ORDERS : Page.MENU;
            category = 0;
        }, mx, my);
    }

    private void drawOrders(GuiGraphicsExtractor g, int mx, int my) {
        List<OrderInfo> orders = new ArrayList<>(state.orders());
        orders.sort((a, b) -> a.status() != b.status() ? Integer.compare(a.status(), b.status()) : Integer.compare(b.ageSeconds(), a.ageSeconds()));
        int kitchen = 0, ready = 0;
        for (OrderInfo o : orders) if (o.status() == 0) kitchen++; else ready++;
        g.text(font, Component.literal("Open orders").withStyle(ChatFormatting.BOLD), 10, TOP + 6, DARK, false);
        String info = kitchen + " in the kitchen, " + ready + " ready";
        g.text(font, info, W - 10 - font.width(info), TOP + 6, GREY, false);
        if (orders.isEmpty()) g.centeredText(font, Component.literal("No open orders"), W / 2, TOP + 60, GREY);
        int rowH = 25, visible = 6;
        scroll = Math.min(scroll, Math.max(0, orders.size() - visible));
        for (int i = 0; i < Math.min(visible, orders.size() - scroll); i++) {
            OrderInfo o = orders.get(i + scroll);
            int y = TOP + 18 + i * rowH;
            box(g, 6, y, W - 12, rowH - 2, 0xFFFFFFFF, EDGE);
            g.text(font, Component.literal("#" + o.number()).withStyle(ChatFormatting.BOLD), 12, y + 4, DARK, false);
            boolean isReady = o.status() == 1;
            String chip = isReady ? "Ready" : "Kitchen";
            int cw = font.width(chip) + 6;
            g.fill(12, y + 13, 12 + cw, y + 22, isReady ? GREEN : YELLOW);
            g.text(font, chip, 15, y + 14, isReady ? 0xFFFFFFFF : DARK, false);
            String items = o.lines().replace("|", ", ");
            g.text(font, fit(items, 130), 56, y + 4, DARK, false);
            String meta = (o.ageSeconds() / 60) + " min" + (o.customer().isEmpty() ? "" : " - " + o.customer());
            g.text(font, fit(meta, 130), 56, y + 13, GREY, false);
            if (isReady) {
                button(g, 192, y + 4, 94, 15, "Picked up", 0xFFE6E6E6, DARK, true, () -> send(RestaurantPayloads.PICKED, "", o.number()), mx, my);
            } else {
                button(g, 192, y + 4, 62, 15, "Done", GREEN, 0xFFFFFFFF, true, () -> send(RestaurantPayloads.DONE, "", o.number()), mx, my);
                button(g, 258, y + 4, 28, 15, "x", 0xFFF2D0CC, RED, true, () -> send(RestaurantPayloads.CANCEL, "", o.number()), mx, my);
            }
        }
        if (orders.size() > visible) {
            String s = (scroll + 1) + "-" + Math.min(orders.size(), scroll + visible) + " of " + orders.size() + " (scroll)";
            g.text(font, s, W - 10 - font.width(s), H - 10, GREY, false);
        }
    }
}
