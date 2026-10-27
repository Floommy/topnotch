package net.togyk.tog_yks_cards.decks;

import net.togyk.tog_yks_cards.decks.Deck;
import net.togyk.tog_yks_cards.decks.card_property.CompositeDeckProperty;
import net.togyk.tog_yks_cards.decks.card_property.DeckProperty;
import net.togyk.tog_yks_cards.decks.card_property.DependingDeckProperty;
import net.togyk.tog_yks_cards.decks.card_property.SimpleDeckProperty;

public class PlayingCardDeck
extends Deck {
    public PlayingCardDeck(String name) {
        super(name, new CompositeDeckProperty("joker_or_card", new DependingDeckProperty("suit", new DeckProperty("rank", "2", "3", "4", "5", "6", "7", "8", "9", "10", "jack", "queen", "king", "ace"), "club", "heart", "spade", "diamond"), new SimpleDeckProperty("joker", 2)));
    }
}

