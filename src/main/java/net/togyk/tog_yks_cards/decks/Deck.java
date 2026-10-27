package net.togyk.tog_yks_cards.decks;

import com.mojang.serialization.Codec;
import net.togyk.tog_yks_cards.decks.ModDecks;
import net.togyk.tog_yks_cards.decks.card_property.DeckProperty;

public class Deck {
    public static Codec<Deck> CODEC = Codec.STRING.xmap(ModDecks::get, Deck::getName);
    public final String name;
    private final DeckProperty property;

    public Deck(String name, DeckProperty property) {
        this.name = name;
        this.property = property;
    }

    public DeckProperty getProperty() {
        return this.property;
    }

    public String getName() {
        return this.name;
    }
}

