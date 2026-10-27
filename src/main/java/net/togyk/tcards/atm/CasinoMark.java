package net.togyk.tcards.atm;

import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * "Issued by casino X": put on chips and coins an ATM hands out (they can only be sold back at ATMs
 * of the same bank) and on a Central Bank item, so a broken bank keeps its prices when placed again.
 */
public record CasinoMark(UUID bank, String name) {
    public static final Codec<CasinoMark> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("bank").forGetter(CasinoMark::bank),
            Codec.STRING.fieldOf("name").forGetter(CasinoMark::name)).apply(i, CasinoMark::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, CasinoMark> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, CasinoMark::bank, ByteBufCodecs.STRING_UTF8, CasinoMark::name, CasinoMark::new);

    public static DataComponentType<CasinoMark> TYPE;

    public static void register() {
        TYPE = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath("tog_yks-cards", "casino"),
                DataComponentType.<CasinoMark>builder().persistent(CODEC).networkSynchronized(STREAM_CODEC).cacheEncoding().build());
    }

    public static UUID bankOf(ItemStack stack) {
        CasinoMark m = stack.get(TYPE);
        return m == null ? null : m.bank();
    }
}
