package net.togyk.tog_yks_cards.component;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public class ModComponents {
    public static final DataComponentType<Boolean> OPEN = (DataComponentType)Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, (Identifier)Identifier.fromNamespaceAndPath((String)"tog_yks-cards", (String)"open"), DataComponentType.<Boolean>builder().persistent(Codec.BOOL).build());
    public static final DataComponentType<Boolean> EMPTY = (DataComponentType)Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, (Identifier)Identifier.fromNamespaceAndPath((String)"tog_yks-cards", (String)"empty"), DataComponentType.<Boolean>builder().persistent(Codec.BOOL).build());
    public static final DataComponentType<Map<String, String>> CARD_DATA = (DataComponentType)Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, (Identifier)Identifier.fromNamespaceAndPath((String)"tog_yks-cards", (String)"card_data"), DataComponentType.<Map<String, String>>builder().persistent(Codec.unboundedMap(Codec.STRING, Codec.STRING)).build());
    public static final DataComponentType<List<ItemStack>> CARDS = (DataComponentType)Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, (Identifier)Identifier.fromNamespaceAndPath((String)"tog_yks-cards", (String)"cards"), DataComponentType.<List<ItemStack>>builder().persistent(Codec.list(ItemStack.CODEC)).build());
    public static final DataComponentType<Boolean> FLIPPED = (DataComponentType)Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, (Identifier)Identifier.fromNamespaceAndPath((String)"tog_yks-cards", (String)"flipped"), DataComponentType.<Boolean>builder().persistent(Codec.BOOL).build());

    public static void registerModComponents() {
    }

    public static DataComponentType<?> getComponent(Identifier id) {
        return (DataComponentType)BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(id);
    }
}

