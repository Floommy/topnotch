package net.togyk.tcards.shuffler;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class ShufflerPayloads {
    private ShufflerPayloads() {}

    public static final int SET_MODE = 0, PRESET = 1, SET_COLOR = 2, SORT_BY_COLOR = 3, EJECT_ALL = 4,
            CUSTOM_RANKS = 5, CUSTOM_DECKS = 6, CUSTOM_JOKERS = 7, TAKE = 8, RESHUFFLE = 9, REFRESH = 10, SORTED = 11;

    /** Client -> server: a button in the shuffler menu. */
    public record Action(BlockPos pos, int action, int arg, String text) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(Identifier.fromNamespaceAndPath("tog_yks-cards", "shuffler_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Action> CODEC = StreamCodec.of(
                (buf, a) -> {
                    buf.writeBlockPos(a.pos());
                    buf.writeVarInt(a.action());
                    buf.writeVarInt(a.arg());
                    buf.writeUtf(a.text());
                },
                buf -> new Action(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt(), buf.readUtf()));

        @Override
        public Type<Action> type() {
            return TYPE;
        }
    }

    /** Server -> client: open / refresh the shuffler menu. */
    public record State(BlockPos pos, boolean open, int mode, int preset, String color, int total, int ready,
                        int available, int customRanks, int customDecks, boolean customJokers, boolean sorted,
                        List<String> colorIds, List<Integer> colorCounts, String message) implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(Identifier.fromNamespaceAndPath("tog_yks-cards", "shuffler_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC = StreamCodec.of(
                (buf, s) -> {
                    buf.writeBlockPos(s.pos());
                    buf.writeBoolean(s.open());
                    buf.writeVarInt(s.mode());
                    buf.writeVarInt(s.preset());
                    buf.writeUtf(s.color());
                    buf.writeVarInt(s.total());
                    buf.writeVarInt(s.ready());
                    buf.writeVarInt(s.available());
                    buf.writeVarInt(s.customRanks());
                    buf.writeVarInt(s.customDecks());
                    buf.writeBoolean(s.customJokers());
                    buf.writeBoolean(s.sorted());
                    buf.writeVarInt(s.colorIds().size());
                    for (int i = 0; i < s.colorIds().size(); i++) {
                        buf.writeUtf(s.colorIds().get(i));
                        buf.writeVarInt(s.colorCounts().get(i));
                    }
                    buf.writeUtf(s.message());
                },
                buf -> {
                    BlockPos pos = buf.readBlockPos();
                    boolean open = buf.readBoolean();
                    int mode = buf.readVarInt(), preset = buf.readVarInt();
                    String color = buf.readUtf();
                    int total = buf.readVarInt(), ready = buf.readVarInt(), available = buf.readVarInt();
                    int ranks = buf.readVarInt(), decks = buf.readVarInt();
                    boolean jokers = buf.readBoolean();
                    boolean sorted = buf.readBoolean();
                    int n = buf.readVarInt();
                    List<String> ids = new ArrayList<>();
                    List<Integer> counts = new ArrayList<>();
                    for (int i = 0; i < n; i++) {
                        ids.add(buf.readUtf());
                        counts.add(buf.readVarInt());
                    }
                    String message = buf.readUtf();
                    return new State(pos, open, mode, preset, color, total, ready, available, ranks, decks, jokers, sorted, ids, counts, message);
                });

        @Override
        public Type<State> type() {
            return TYPE;
        }
    }
}
