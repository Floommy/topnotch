package net.togyk.tog_yks_cards.decks.card_property;

import net.togyk.tog_yks_cards.decks.card_property.DeckProperty;

public class SimpleDeckProperty
extends DeckProperty {
    public SimpleDeckProperty(String name, int count) {
        super(name, count, name);
    }

    public SimpleDeckProperty(String name) {
        super(name, name);
    }

    @Override
    public String getTexturePath(String deckName, String option) {
        return "item/deck/" + deckName + "/" + this.getName();
    }
}

