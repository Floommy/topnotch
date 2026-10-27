package net.togyk.tcards.atm;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class AtmPayloads {
    private AtmPayloads() {}

    // ATM actions
    public static final int REFRESH = 0, BUY = 1, EMPTY_VAULT = 2, LINK_START = 3, UNLINK = 4, TOGGLE_COINS = 5, SHOW_LINK = 6,
            SELL = 7, LINK_BANK = 8;
    // Central Bank actions
    public static final int SET_PRICE = 20, SET_NAME = 21;

    // result codes in AtmState
    public static final int RESULT_NONE = 0, RESULT_DISPENSED = 1, RESULT_ERROR = 2, RESULT_PRICE_CHANGED = 3, RESULT_INFO = 4, RESULT_SOLD = 5;

    // link status
    public static final int LINK_NONE = 0, LINK_OK = 1, LINK_MISSING = 2, LINK_FULL = 3, LINK_UNLOADED = 4;

    public record PriceEntry(String id, int diamonds, int amount) {
        static void write(RegistryFriendlyByteBuf buf, List<PriceEntry> list) {
            buf.writeVarInt(list.size());
            for (PriceEntry e : list) {
                buf.writeUtf(e.id());
                buf.writeVarInt(e.diamonds());
                buf.writeVarInt(e.amount());
            }
        }

        static List<PriceEntry> read(RegistryFriendlyByteBuf buf) {
            int n = buf.readVarInt();
            List<PriceEntry> list = new ArrayList<>();
            for (int i = 0; i < n && i < 64; i++) list.add(new PriceEntry(buf.readUtf(), buf.readVarInt(), buf.readVarInt()));
            return list;
        }
    }

    /** Client -> server: a key on the ATM or a change on the Central Bank. */
    public record Action(BlockPos pos, int action, String item, int a, int b, int c) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(Identifier.fromNamespaceAndPath("tog_yks-cards", "atm_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Action> CODEC = StreamCodec.of(
                (buf, x) -> {
                    buf.writeBlockPos(x.pos());
                    buf.writeVarInt(x.action());
                    buf.writeUtf(x.item());
                    buf.writeVarInt(x.a());
                    buf.writeVarInt(x.b());
                    buf.writeVarInt(x.c());
                },
                buf -> new Action(buf.readBlockPos(), buf.readVarInt(), buf.readUtf(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));

        @Override
        public Type<Action> type() {
            return TYPE;
        }
    }

    /** Server -> client: open / refresh the ATM screen, optionally with the result of a transaction. */
    public record AtmState(BlockPos pos, boolean open, String owner, boolean manage, boolean coins, int diamonds,
                           int vault, int vaultCapacity, int linkStatus, String linkText, long turnover,
                           List<PriceEntry> prices, int result, String resultItem, int resultQty, int resultCost,
                           String message, String bankName, List<Integer> held, long paidOut) implements CustomPacketPayload {
        public static final Type<AtmState> TYPE = new Type<>(Identifier.fromNamespaceAndPath("tog_yks-cards", "atm_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, AtmState> CODEC = StreamCodec.of(
                (buf, s) -> {
                    buf.writeBlockPos(s.pos());
                    buf.writeBoolean(s.open());
                    buf.writeUtf(s.owner());
                    buf.writeBoolean(s.manage());
                    buf.writeBoolean(s.coins());
                    buf.writeVarInt(s.diamonds());
                    buf.writeVarInt(s.vault());
                    buf.writeVarInt(s.vaultCapacity());
                    buf.writeVarInt(s.linkStatus());
                    buf.writeUtf(s.linkText());
                    buf.writeVarLong(s.turnover());
                    PriceEntry.write(buf, s.prices());
                    buf.writeVarInt(s.result());
                    buf.writeUtf(s.resultItem());
                    buf.writeVarInt(s.resultQty());
                    buf.writeVarInt(s.resultCost());
                    buf.writeUtf(s.message());
                    buf.writeUtf(s.bankName());
                    buf.writeVarInt(s.held().size());
                    for (int h : s.held()) buf.writeVarInt(h);
                    buf.writeVarLong(s.paidOut());
                },
                buf -> new AtmState(buf.readBlockPos(), buf.readBoolean(), buf.readUtf(), buf.readBoolean(), buf.readBoolean(),
                        buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readUtf(), buf.readVarLong(),
                        PriceEntry.read(buf), buf.readVarInt(), buf.readUtf(), buf.readVarInt(), buf.readVarInt(), buf.readUtf(),
                        buf.readUtf(), readInts(buf), buf.readVarLong()));

        @Override
        public Type<AtmState> type() {
            return TYPE;
        }
    }

    /** Server -> client: open / refresh the Central Bank price board. */
    static List<Integer> readInts(RegistryFriendlyByteBuf buf) {
        int n = Math.min(64, buf.readVarInt());
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < n; i++) list.add(buf.readVarInt());
        return list;
    }

    public record BankState(BlockPos pos, boolean open, boolean canEdit, List<PriceEntry> prices, String message, String name) implements CustomPacketPayload {
        public static final Type<BankState> TYPE = new Type<>(Identifier.fromNamespaceAndPath("tog_yks-cards", "bank_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, BankState> CODEC = StreamCodec.of(
                (buf, s) -> {
                    buf.writeBlockPos(s.pos());
                    buf.writeBoolean(s.open());
                    buf.writeBoolean(s.canEdit());
                    PriceEntry.write(buf, s.prices());
                    buf.writeUtf(s.message());
                    buf.writeUtf(s.name());
                },
                buf -> new BankState(buf.readBlockPos(), buf.readBoolean(), buf.readBoolean(), PriceEntry.read(buf), buf.readUtf(), buf.readUtf()));

        @Override
        public Type<BankState> type() {
            return TYPE;
        }
    }
}
