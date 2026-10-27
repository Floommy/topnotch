package net.togyk.tcards;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/** Client -> server: 0 = collect (left click), 1 = table key (shuffle / take coin / clear table). */
public record TableActionPayload(byte action, BlockPos pos, Vec3 hit) implements CustomPacketPayload {
    public static final byte COLLECT = 0;
    public static final byte TABLE_KEY = 1;

    public static final CustomPacketPayload.Type<TableActionPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("tog_yks-cards", "table_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TableActionPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, TableActionPayload::action,
            BlockPos.STREAM_CODEC, TableActionPayload::pos,
            Vec3.STREAM_CODEC, TableActionPayload::hit,
            TableActionPayload::new);

    @Override
    public CustomPacketPayload.Type<TableActionPayload> type() {
        return TYPE;
    }
}
