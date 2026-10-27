package net.togyk.tog_yks_cards.decks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.data.models.model.TextureSlot;
import net.togyk.tog_yks_cards.decks.Deck;
import net.togyk.tog_yks_cards.decks.card_property.CompositeDeckProperty;
import net.togyk.tog_yks_cards.decks.card_property.DeckProperty;
import net.togyk.tog_yks_cards.decks.card_property.DependingDeckProperty;

@Environment(value=EnvType.CLIENT)
public class DeckPropertyLayers {
    private static final Map<DeckProperty, TextureSlot> LAYERS = new HashMap<DeckProperty, TextureSlot>();

    public static TextureSlot getLayer(DeckProperty property) {
        return LAYERS.get(property);
    }

    public static void registerLayer(DeckProperty property, String suffix) {
        if (property instanceof DependingDeckProperty) {
            DependingDeckProperty dependingDeckProperty = (DependingDeckProperty)property;
            LAYERS.put(property, TextureSlot.create((String)(property.getName() + suffix)));
            DeckPropertyLayers.registerLayer(dependingDeckProperty.getDependedProperty());
        } else if (property instanceof CompositeDeckProperty) {
            CompositeDeckProperty compositeProperty = (CompositeDeckProperty)property;
            DeckPropertyLayers.registerLayers(compositeProperty.getCompositeProperties());
        } else {
            LAYERS.put(property, TextureSlot.create((String)(property.getName() + suffix)));
        }
    }

    public static void registerLayer(DeckProperty property) {
        if (property instanceof DependingDeckProperty) {
            DependingDeckProperty dependingDeckProperty = (DependingDeckProperty)property;
            LAYERS.put(property, TextureSlot.create((String)property.getName()));
            DeckPropertyLayers.registerLayer(dependingDeckProperty.getDependedProperty());
        } else if (property instanceof CompositeDeckProperty) {
            CompositeDeckProperty compositeProperty = (CompositeDeckProperty)property;
            DeckPropertyLayers.registerLayers(compositeProperty.getCompositeProperties());
        } else {
            LAYERS.put(property, TextureSlot.create((String)property.getName()));
        }
    }

    public static void registerLayers(List<DeckProperty> properties) {
        for (DeckProperty property : properties) {
            DeckPropertyLayers.registerLayer(property);
        }
    }

    public static void registerLayers(DeckProperty ... properties) {
        DeckPropertyLayers.registerLayers(List.of(properties));
    }

    public static void registerDeckLayers(List<Deck> decks) {
        for (Deck deck : decks) {
            DeckProperty property = deck.getProperty();
            if (property instanceof DependingDeckProperty) {
                DependingDeckProperty dependingDeckProperty = (DependingDeckProperty)property;
                DeckPropertyLayers.registerLayer(property);
                DeckPropertyLayers.registerLayer(dependingDeckProperty.getDependedProperty());
                continue;
            }
            if (property instanceof CompositeDeckProperty) {
                CompositeDeckProperty compositeProperty = (CompositeDeckProperty)property;
                DeckPropertyLayers.registerLayers(compositeProperty.getCompositeProperties());
                continue;
            }
            DeckPropertyLayers.registerLayer(property);
        }
    }

    public static void registerDeckLayers(Deck ... decks) {
        DeckPropertyLayers.registerDeckLayers(List.of(decks));
    }
}

