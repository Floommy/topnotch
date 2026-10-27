package net.togyk.tcards.client;

import java.util.List;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.togyk.tcards.shuffler.ShufflerBlockEntity;
import net.togyk.tcards.shuffler.ShufflerLogic;
import net.togyk.tcards.shuffler.ShufflerPayloads;
import net.togyk.tcards.shuffler.ShufflerPayloads.State;

@Environment(EnvType.CLIENT)
public class ShufflerScreen extends Screen {
    private static final int W = 316, H = 232;
    private State state;
    private String message = "";
    private long messageUntil = 0;

    public ShufflerScreen(State state) {
        super(Component.translatable("block.tog_yks-cards.card_shuffler"));
        this.state = state;
    }

    public static void registerReceiver() {
        ClientPlayNetworking.registerGlobalReceiver(State.TYPE, (payload, context) -> context.client().execute(() -> {
            Minecraft mc = context.client();
            if (mc.gui.screen() instanceof ShufflerScreen open && open.state.pos().equals(payload.pos())) {
                open.update(payload);
            } else if (payload.open()) {
                mc.gui.setScreen(new ShufflerScreen(payload));
            }
        }));
    }

    private void update(State s) {
        this.state = s;
        if (!s.message().isEmpty()) {
            message = s.message();
            messageUntil = System.currentTimeMillis() + 4000;
        }
        rebuildWidgets();
    }

    private void send(int action, int arg, String text) {
        ClientPlayNetworking.send(new ShufflerPayloads.Action(state.pos(), action, arg, text == null ? "" : text));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static Component colorName(String id) {
        if (id.isEmpty()) return Component.literal("Mixed (all)");
        var item = BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
        return new ItemStack(item).getHoverName();
    }

    @Override
    protected void init() {
        int x0 = (width - W) / 2, y0 = (height - H) / 2;
        int mode = state.mode();
        boolean table = mode != ShufflerBlockEntity.SORTER;
        String modeLabel = mode == ShufflerBlockEntity.TABLE ? "Table: deck" : mode == ShufflerBlockEntity.DEALER ? "Table: single card" : "Sorter";
        int nextMode = (mode + 1) % 3;

        // row 1: mode + colours
        addRenderableWidget(Button.builder(Component.literal(modeLabel),
                b -> send(ShufflerPayloads.SET_MODE, nextMode, ""))
                .bounds(x0 + 6, y0 + 20, 100, 18).build());
        List<String> ids = state.colorIds();
        int current = ids.indexOf(state.color());
        String next = current + 1 < ids.size() ? ids.get(current + 1) : "";
        addRenderableWidget(Button.builder(Component.literal("Back: ").append(colorName(state.color())),
                b -> send(ShufflerPayloads.SET_COLOR, 0, next)).bounds(x0 + 110, y0 + 20, 200, 18).build());

        // presets: 3 columns
        int cols = 3, bw = 100, bh = 16;
        for (int i = 0; i < ShufflerLogic.PRESETS.size(); i++) {
            int id = i;
            String label = ShufflerLogic.PRESETS.get(i).label();
            if (table && state.preset() == i) label = "> " + label;
            Button b = Button.builder(Component.literal(label), btn -> send(ShufflerPayloads.PRESET, id, ""))
                    .bounds(x0 + 6 + (i % cols) * (bw + 4), y0 + 44 + (i / cols) * (bh + 2), bw, bh).build();
            b.active = (state.available() & (1 << i)) != 0 || (table && state.preset() == i);
            addRenderableWidget(b);
        }

        // custom settings
        int cy = y0 + 44 + ((ShufflerLogic.PRESETS.size() + cols - 1) / cols) * (bh + 2) + 6;
        for (int r = 0; r < ShufflerLogic.RANKS.length; r++) {
            int bit = 1 << r;
            boolean on = (state.customRanks() & bit) != 0;
            Button rb = Button.builder(Component.literal((on ? "§a" : "§8") + ShufflerLogic.RANK_LABELS[r]),
                    btn -> send(ShufflerPayloads.CUSTOM_RANKS, state.customRanks() ^ bit, ""))
                    .bounds(x0 + 6 + r * 23, cy, 21, 16).build();
            addRenderableWidget(rb);
        }
        addRenderableWidget(Button.builder(Component.literal("Joker: " + (state.customJokers() ? "§ayes" : "§7no")),
                b -> send(ShufflerPayloads.CUSTOM_JOKERS, state.customJokers() ? 0 : 1, "")).bounds(x0 + 6, cy + 18, 70, 16).build());
        addRenderableWidget(Button.builder(Component.literal("-"),
                b -> send(ShufflerPayloads.CUSTOM_DECKS, state.customDecks() - 1, "")).bounds(x0 + 80, cy + 18, 16, 16).build());
        addRenderableWidget(Button.builder(Component.literal("+"),
                b -> send(ShufflerPayloads.CUSTOM_DECKS, state.customDecks() + 1, "")).bounds(x0 + 150, cy + 18, 16, 16).build());

        // bottom row
        int by = y0 + H - 24;
        addRenderableWidget(Button.builder(Component.literal("Sort by back colour"),
                b -> send(ShufflerPayloads.SORT_BY_COLOR, 0, "")).bounds(x0 + 6, by, 110, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Eject all"),
                b -> send(ShufflerPayloads.EJECT_ALL, 0, "")).bounds(x0 + 120, by, 90, 18).build());
        if (table) {
            Button take = Button.builder(Component.literal(mode == ShufflerBlockEntity.DEALER ? "Take card" : "Take deck"),
                    b -> send(ShufflerPayloads.TAKE, 0, "")).bounds(x0 + 214, by, 96, 18).build();
            take.active = state.ready() > 0;
            addRenderableWidget(take);
        } else {
            addRenderableWidget(Button.builder(Component.literal(state.sorted() ? "Output: §esorted" : "Output: §ashuffled"),
                    b -> send(ShufflerPayloads.SORTED, state.sorted() ? 0 : 1, "")).bounds(x0 + 214, by, 96, 18).build());
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float pt) {
        int x0 = (width - W) / 2, y0 = (height - H) / 2;
        g.fill(0, 0, width, height, 0x60000000);
        g.fill(x0 - 1, y0 - 1, x0 + W + 1, y0 + H + 1, 0xFF3A3A40);
        g.fill(x0, y0, x0 + W, y0 + H, 0xF0141418);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
        super.extractRenderState(g, mx, my, pt);
        int x0 = (width - W) / 2, y0 = (height - H) / 2;
        boolean table = state.mode() != ShufflerBlockEntity.SORTER;
        g.text(font, Component.translatable("block.tog_yks-cards.card_shuffler").withStyle(net.minecraft.ChatFormatting.BOLD), x0 + 6, y0 + 6, 0xFFFFFFFF);
        String info = state.total() + " cards" + (table ? (state.mode() == ShufflerBlockEntity.DEALER ? "  |  in shoe: " : "  |  deck ready: ") + (state.ready() > 0 ? "§a" + state.ready() : "§cno") : "");
        g.text(font, Component.literal(info), x0 + W - 6 - font.width(Component.literal(info)), y0 + 6, 0xFFBBBBBB);

        int cols = 3, bh = 16;
        int cy = y0 + 44 + ((ShufflerLogic.PRESETS.size() + cols - 1) / cols) * (bh + 2) + 6;
        g.centeredText(font, Component.literal(state.customDecks() + (state.customDecks() == 1 ? " Deck" : " Decks")), x0 + 123, cy + 22, 0xFFFFFFFF);
        g.text(font, Component.literal("§7< custom preset"), x0 + 172, cy + 22, 0xFFFFFFFF);

        if (!message.isEmpty() && System.currentTimeMillis() < messageUntil) {
            g.centeredText(font, Component.literal(message), x0 + W / 2, cy + 40, 0xFFFFDD55);
        } else {
            g.centeredText(font, Component.literal(table
                    ? (state.mode() == ShufflerBlockEntity.DEALER ? "§7Empty hand: 1 card  |  Sneak: menu" : "§7Empty hand: take deck  |  Sneak: menu")
                    : (state.sorted() ? "§7Click a preset = sorted deck to inventory" : "§7Click a preset = shuffled deck to inventory")), x0 + W / 2, cy + 40, 0xFFFFFFFF);
        }
    }
}
