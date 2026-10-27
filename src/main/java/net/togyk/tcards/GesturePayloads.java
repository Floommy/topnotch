package net.togyk.tcards;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class GesturePayloads {
    private GesturePayloads() {}

    /** Client -> server: "I make this gesture". */
    public record Make(byte gesture) implements CustomPacketPayload {
        public static final Type<Make> TYPE = new Type<>(Identifier.fromNamespaceAndPath("tog_yks-cards", "gesture_make"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Make> CODEC =
                StreamCodec.composite(ByteBufCodecs.BYTE, Make::gesture, Make::new);

        @Override
        public Type<Make> type() {
            return TYPE;
        }
    }

    /** Server -> clients: "entity X makes this gesture". */
    public record Show(int entityId, byte gesture) implements CustomPacketPayload {
        public static final Type<Show> TYPE = new Type<>(Identifier.fromNamespaceAndPath("tog_yks-cards", "gesture_show"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Show> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Show::entityId,
                ByteBufCodecs.BYTE, Show::gesture,
                Show::new);

        @Override
        public Type<Show> type() {
            return TYPE;
        }
    }
}
