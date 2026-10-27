package net.togyk.tog_yks_cards.property;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperties;
import net.minecraft.resources.Identifier;
import net.togyk.tog_yks_cards.property.DeckSelectProperty;
import net.togyk.tog_yks_cards.property.EmptyProperty;
import net.togyk.tog_yks_cards.property.FlippedProperty;
import net.togyk.tog_yks_cards.property.HasProperty;
import net.togyk.tog_yks_cards.property.OpenProperty;

@Environment(value=EnvType.CLIENT)
public class ModItemModelProperties {
    public static final Identifier DECK_SELECT_PROPERTY_ID = Identifier.fromNamespaceAndPath((String)"tog_yks-cards", (String)"deck_select");
    public static final Identifier OPEN_PROPERTY_ID = Identifier.fromNamespaceAndPath((String)"tog_yks-cards", (String)"open");
    public static final Identifier EMPTY_PROPERTY_ID = Identifier.fromNamespaceAndPath((String)"tog_yks-cards", (String)"empty");
    public static final Identifier HAS_PROPERTY_ID = Identifier.fromNamespaceAndPath((String)"tog_yks-cards", (String)"has");
    public static final Identifier FLIPPED_PROPERTY_ID = Identifier.fromNamespaceAndPath((String)"tog_yks-cards", (String)"flipped");

    public static void registerModProperties() {
        SelectItemModelProperties.ID_MAPPER.put(DECK_SELECT_PROPERTY_ID, DeckSelectProperty.TYPE);
        SelectItemModelProperties.ID_MAPPER.put(OPEN_PROPERTY_ID, OpenProperty.TYPE);
        SelectItemModelProperties.ID_MAPPER.put(EMPTY_PROPERTY_ID, EmptyProperty.TYPE);
        SelectItemModelProperties.ID_MAPPER.put(HAS_PROPERTY_ID, HasProperty.TYPE);
        SelectItemModelProperties.ID_MAPPER.put(FLIPPED_PROPERTY_ID, FlippedProperty.TYPE);
    }
}

