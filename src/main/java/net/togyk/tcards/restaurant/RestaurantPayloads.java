package net.togyk.tcards.restaurant;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class RestaurantPayloads {
    private RestaurantPayloads() {}

    public static final int REFRESH = 0, ORDER = 1, DONE = 2, PICKED = 3, CANCEL = 4, SET_NAME = 5, SET_CURRENCY = 6,
            SET_PRICE = 7, EMPTY_VAULT = 8, LINK_CHEST = 9, UNLINK_CHEST = 10, SHOW_LINK = 11;
    public static final int RESULT_NONE = 0, RESULT_ORDERED = 1, RESULT_ERROR = 2, RESULT_INFO = 3;
    public static final int KIND_KIOSK = 0, KIND_REGISTER = 1;

    /** Client -> server. ORDER sends the cart as "item:qty,item:qty". */
    public record Action(BlockPos pos, int action, String text, int a) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(Identifier.fromNamespaceAndPath("tog_yks-cards", "restaurant_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Action> CODEC = StreamCodec.of(
                (buf, x) -> {
                    buf.writeBlockPos(x.pos());
                    buf.writeVarInt(x.action());
                    buf.writeUtf(x.text(), 1024);
                    buf.writeVarInt(x.a());
                },
                buf -> new Action(buf.readBlockPos(), buf.readVarInt(), buf.readUtf(1024), buf.readVarInt()));

        @Override
        public Type<Action> type() {
            return TYPE;
        }
    }

    public record OrderInfo(int number, int status, String lines, int ageSeconds, String customer) {}

    /** Server -> client: kiosk / register screen data. */
    public record State(BlockPos pos, boolean open, int kind, String name, String currency, boolean manage, int money,
                        List<String> items, List<Integer> prices, List<OrderInfo> orders, long vault, int linkStatus,
                        String linkText, long sales, int result, int number, String message) implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(Identifier.fromNamespaceAndPath("tog_yks-cards", "restaurant_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC = StreamCodec.of(
                (buf, s) -> {
                    buf.writeBlockPos(s.pos());
                    buf.writeBoolean(s.open());
                    buf.writeVarInt(s.kind());
                    buf.writeUtf(s.name());
                    buf.writeUtf(s.currency());
                    buf.writeBoolean(s.manage());
                    buf.writeVarInt(s.money());
                    buf.writeVarInt(s.items().size());
                    for (int i = 0; i < s.items().size(); i++) {
                        buf.writeUtf(s.items().get(i));
                        buf.writeVarInt(s.prices().get(i));
                    }
                    buf.writeVarInt(s.orders().size());
                    for (OrderInfo o : s.orders()) {
                        buf.writeVarInt(o.number());
                        buf.writeVarInt(o.status());
                        buf.writeUtf(o.lines());
                        buf.writeVarInt(o.ageSeconds());
                        buf.writeUtf(o.customer());
                    }
                    buf.writeVarLong(s.vault());
                    buf.writeVarInt(s.linkStatus());
                    buf.writeUtf(s.linkText());
                    buf.writeVarLong(s.sales());
                    buf.writeVarInt(s.result());
                    buf.writeVarInt(s.number());
                    buf.writeUtf(s.message());
                },
                buf -> {
                    BlockPos pos = buf.readBlockPos();
                    boolean open = buf.readBoolean();
                    int kind = buf.readVarInt();
                    String name = buf.readUtf(), currency = buf.readUtf();
                    boolean manage = buf.readBoolean();
                    int money = buf.readVarInt();
                    int n = Math.min(64, buf.readVarInt());
                    List<String> items = new ArrayList<>();
                    List<Integer> prices = new ArrayList<>();
                    for (int i = 0; i < n; i++) {
                        items.add(buf.readUtf());
                        prices.add(buf.readVarInt());
                    }
                    int m = Math.min(400, buf.readVarInt());
                    List<OrderInfo> orders = new ArrayList<>();
                    for (int i = 0; i < m; i++) orders.add(new OrderInfo(buf.readVarInt(), buf.readVarInt(), buf.readUtf(), buf.readVarInt(), buf.readUtf()));
                    return new State(pos, open, kind, name, currency, manage, money, items, prices, orders, buf.readVarLong(),
                            buf.readVarInt(), buf.readUtf(), buf.readVarLong(), buf.readVarInt(), buf.readVarInt(), buf.readUtf());
                });

        @Override
        public Type<State> type() {
            return TYPE;
        }
    }
}
