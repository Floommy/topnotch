package net.togyk.tog_yks_cards.decks;

import java.util.HashMap;
import java.util.Map;
import net.togyk.tog_yks_cards.decks.Deck;
import net.togyk.tog_yks_cards.decks.PlayingCardDeck;
import net.togyk.tog_yks_cards.decks.UnoDeck;

public class ModDecks {
    public static final Map<String, Deck> decks = new HashMap<String, Deck>();
    public static final Deck BLACK_PLAYING_CARDS = ModDecks.register(new PlayingCardDeck("black_playing_cards"));
    public static final Deck BLUE_PLAYING_CARDS = ModDecks.register(new PlayingCardDeck("blue_playing_cards"));
    public static final Deck BROWN_PLAYING_CARDS = ModDecks.register(new PlayingCardDeck("brown_playing_cards"));
    public static final Deck CYAN_PLAYING_CARDS = ModDecks.register(new PlayingCardDeck("cyan_playing_cards"));
    public static final Deck GRAY_PLAYING_CARDS = ModDecks.register(new PlayingCardDeck("gray_playing_cards"));
    public static final Deck GREEN_PLAYING_CARDS = ModDecks.register(new PlayingCardDeck("green_playing_cards"));
    public static final Deck LIGHT_BLUE_PLAYING_CARDS = ModDecks.register(new PlayingCardDeck("light_blue_playing_cards"));
    public static final Deck LIGHT_GRAY_PLAYING_CARDS = ModDecks.register(new PlayingCardDeck("light_gray_playing_cards"));
    public static final Deck LIME_PLAYING_CARDS = ModDecks.register(new PlayingCardDeck("lime_playing_cards"));
    public static final Deck MAGENTA_PLAYING_CARDS = ModDecks.register(new PlayingCardDeck("magenta_playing_cards"));
    public static final Deck ORANGE_PLAYING_CARDS = ModDecks.register(new PlayingCardDeck("orange_playing_cards"));
    public static final Deck PINK_PLAYING_CARDS = ModDecks.register(new PlayingCardDeck("pink_playing_cards"));
    public static final Deck PURPLE_PLAYING_CARDS = ModDecks.register(new PlayingCardDeck("purple_playing_cards"));
    public static final Deck RED_PLAYING_CARDS = ModDecks.register(new PlayingCardDeck("red_playing_cards"));
    public static final Deck WHITE_PLAYING_CARDS = ModDecks.register(new PlayingCardDeck("white_playing_cards"));
    public static final Deck YELLOW_PLAYING_CARDS = ModDecks.register(new PlayingCardDeck("yellow_playing_cards"));
    public static final Deck COPPER_PLAYING_CARDS = ModDecks.register(new PlayingCardDeck("copper_playing_cards"));
    public static final Deck IRON_PLAYING_CARDS = ModDecks.register(new PlayingCardDeck("iron_playing_cards"));
    public static final Deck GOLD_PLAYING_CARDS = ModDecks.register(new PlayingCardDeck("gold_playing_cards"));
    public static final Deck DIAMOND_PLAYING_CARDS = ModDecks.register(new PlayingCardDeck("diamond_playing_cards"));
    public static final Deck NETHERITE_PLAYING_CARDS = ModDecks.register(new PlayingCardDeck("netherite_playing_cards"));
    public static final Deck AMETHYST_PLAYING_CARDS = ModDecks.register(new PlayingCardDeck("amethyst_playing_cards"));
    public static final Deck EMERALD_PLAYING_CARDS = ModDecks.register(new PlayingCardDeck("emerald_playing_cards"));
    public static final Deck RESIN_PLAYING_CARDS = ModDecks.register(new PlayingCardDeck("resin_playing_cards"));
    public static final Deck ECHO_PLAYING_CARDS = ModDecks.register(new PlayingCardDeck("echo_playing_cards"));
    public static final Deck PREMIUM_PLAYING_CARDS = ModDecks.register(new PlayingCardDeck("premium_playing_cards"));
    public static final Deck LUXURY_PLAYING_CARDS = ModDecks.register(new PlayingCardDeck("luxury_playing_cards"));
    public static final Deck UNO = ModDecks.register(new UnoDeck("uno"));

    public static <T extends Deck> T register(T deck) {
        if (decks.containsKey(deck.name)) {
            throw new IllegalStateException("Deck with name " + deck.name + " already exists");
        }
        decks.put(deck.name, deck);
        return deck;
    }

    public static Deck get(String name) {
        return decks.get(name);
    }
}

