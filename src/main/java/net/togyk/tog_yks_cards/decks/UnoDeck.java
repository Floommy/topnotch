package net.togyk.tog_yks_cards.decks;

import java.util.List;
import java.util.Map;
import net.togyk.tog_yks_cards.decks.Deck;
import net.togyk.tog_yks_cards.decks.card_property.CompositeDeckProperty;
import net.togyk.tog_yks_cards.decks.card_property.DeckProperty;
import net.togyk.tog_yks_cards.decks.card_property.DependingDeckProperty;
import net.togyk.tog_yks_cards.decks.card_property.SimpleDeckProperty;
import net.togyk.tog_yks_cards.decks.card_property.WeightedDeckProperty;

public class UnoDeck
extends Deck {
    public UnoDeck(String name) {
        super(name, new CompositeDeckProperty("color_wild_or_plus4", new DependingDeckProperty("color", (DeckProperty)new WeightedDeckProperty("type", List.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "skip", "reverse", "plus2"), Map.of("0", 1), 2), "red", "green", "blue", "yellow"), new SimpleDeckProperty("wild", 4), new SimpleDeckProperty("plus4", 4)));
    }
}

