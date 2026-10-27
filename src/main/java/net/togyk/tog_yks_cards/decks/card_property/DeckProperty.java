package net.togyk.tog_yks_cards.decks.card_property;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.togyk.tcards.Pair;

public class DeckProperty {
    private final String name;
    private final List<String> options;
    private final int count;

    public DeckProperty(String name, int count, List<String> options) {
        this.name = name;
        this.options = options;
        this.count = count;
    }

    public DeckProperty(String name, int count, String ... options) {
        this.name = name;
        this.options = List.of(options);
        this.count = count;
    }

    public DeckProperty(String name, int count, String option) {
        this.name = name;
        this.options = List.of(option);
        this.count = count;
    }

    public DeckProperty(String name, List<String> options) {
        this(name, 1, options);
    }

    public DeckProperty(String name, String ... options) {
        this(name, 1, options);
    }

    public DeckProperty(String name, String option) {
        this(name, 1, option);
    }

    public int getCount() {
        return this.count;
    }

    public String getName() {
        return this.name;
    }

    public List<String> getOptions() {
        return this.options;
    }

    public Set<String> getUniqueOptions() {
        return new HashSet<String>(this.options);
    }

    public List<List<Pair<String, String>>> getCardProperties() {
        return this.getCardProperties(new ArrayList<Pair<String, String>>());
    }

    public List<List<Pair<String, String>>> getCardProperties(List<Pair<String, String>> otherProperties) {
        ArrayList<List<Pair<String, String>>> cardProperties = new ArrayList<List<Pair<String, String>>>();
        for (String option : this.getOptions()) {
            ArrayList<Pair<String, String>> oneCardProperties = new ArrayList<Pair<String, String>>(otherProperties);
            oneCardProperties.add((Pair<String, String>)Pair.of(this.getName(), option));
            for (int i = 0; i < this.getCount(); ++i) {
                cardProperties.add(oneCardProperties);
            }
        }
        return cardProperties;
    }

    public String getTexturePath(String deckName, String option) {
        return "item/deck/" + deckName + "/" + this.getName() + "/" + option;
    }
}

