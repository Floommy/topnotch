package net.togyk.tcards.restaurant;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

/** The printed receipt: restaurant, order number, what was ordered, total - and how long the paper got. */
public record Receipt(String restaurant, int number, List<String> lines, long total, String currency, int length) {
    public static final Codec<Receipt> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("restaurant").forGetter(Receipt::restaurant),
            Codec.INT.fieldOf("number").forGetter(Receipt::number),
            Codec.STRING.listOf().fieldOf("lines").forGetter(Receipt::lines),
            Codec.LONG.fieldOf("total").forGetter(Receipt::total),
            Codec.STRING.fieldOf("currency").forGetter(Receipt::currency),
            Codec.INT.fieldOf("length").forGetter(Receipt::length)).apply(i, Receipt::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, Receipt> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, Receipt::restaurant,
            ByteBufCodecs.VAR_INT, Receipt::number,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(64)), Receipt::lines,
            ByteBufCodecs.VAR_LONG, Receipt::total,
            ByteBufCodecs.STRING_UTF8, Receipt::currency,
            ByteBufCodecs.VAR_INT, Receipt::length,
            Receipt::new);

    public static DataComponentType<Receipt> TYPE;
    public static Item ITEM;

    public static void register() {
        TYPE = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath("tog_yks-cards", "receipt"),
                DataComponentType.<Receipt>builder().persistent(CODEC).networkSynchronized(STREAM_CODEC).cacheEncoding().build());
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("tog_yks-cards", "receipt"));
        ITEM = Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties().setId(key).stacksTo(1)));
    }

    /** The receipt item; its model gets longer with the length (range dispatch on custom model data). */
    public ItemStack toStack() {
        ItemStack stack = new ItemStack(ITEM);
        stack.set(TYPE, this);
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of((float) length), List.of(), List.of(), List.of()));
        return stack;
    }

    /** Tooltip lines: the longer the receipt, the more "thank you"s and coupons. */
    public List<String> tooltip() {
        List<String> out = new ArrayList<>();
        out.add("§6" + restaurant);
        out.add("§fOrder §l#" + number);
        for (String l : lines) out.add("§7" + l);
        out.add("§fTotal: " + total + " " + currency + (total == 1 ? "" : "s"));
        String[] extras = {"§8Thank you!", "§8Have a nice day!", "§8Come again!", "§8i'm lovin' it",
                "§8Coupon: free fries (not valid)", "§8Please keep your receipt", "§8* * * * * * * * *"};
        int n = Math.min(40, length / 2);
        for (int k = 0; k < n; k++) out.add(extras[k % extras.length]);
        if (length > 0) out.add("§8(" + (length * 2 + 10) + " cm of paper)");
        return out;
    }
}
