package net.togyk.tog_yks_cards.decks.card_property;

import java.util.ArrayList;
import java.util.List;
import net.togyk.tcards.Pair;
import net.togyk.tog_yks_cards.decks.card_property.DeckProperty;

public class CompositeDeckProperty
extends DeckProperty {
    private final List<DeckProperty> compositeProperties;

    public CompositeDeckProperty(String name, int count, List<DeckProperty> compositeProperties) {
        super(name, count, compositeProperties.stream().map(DeckProperty::getName).toList());
        this.compositeProperties = compositeProperties;
    }

    public CompositeDeckProperty(String name, int count, DeckProperty ... compositeProperties) {
        this(name, count, List.of(compositeProperties));
    }

    public CompositeDeckProperty(String name, int count, DeckProperty compositeProperty) {
        this(name, count, List.of(compositeProperty));
    }

    public CompositeDeckProperty(String name, List<DeckProperty> compositeProperties) {
        this(name, 1, compositeProperties);
    }

    public CompositeDeckProperty(String name, DeckProperty ... compositeProperties) {
        this(name, 1, List.of(compositeProperties));
    }

    public CompositeDeckProperty(String name, DeckProperty compositeProperty) {
        this(name, 1, List.of(compositeProperty));
    }

    public List<DeckProperty> getCompositeProperties() {
        return this.compositeProperties;
    }

    public DeckProperty getCompositeProperty(String name) {
        return this.compositeProperties.stream().filter(property -> property.getName().equals(name)).findFirst().orElse(null);
    }

    @Override
    public List<List<Pair<String, String>>> getCardProperties(List<Pair<String, String>> otherProperties) {
        ArrayList<List<Pair<String, String>>> cardProperties = new ArrayList<List<Pair<String, String>>>();
        for (String option : this.getOptions()) {
            ArrayList<Pair<String, String>> oneCardProperties = new ArrayList<Pair<String, String>>(otherProperties);
            oneCardProperties.add((Pair<String, String>)Pair.of(this.getName(), option));
            DeckProperty deckProperty = this.getCompositeProperty(option);
            if (!(deckProperty instanceof DeckProperty)) continue;
            DeckProperty property = deckProperty;
            for (int i = 0; i < this.getCount(); ++i) {
                cardProperties.addAll(property.getCardProperties(oneCardProperties));
            }
        }
        return cardProperties;
    }
}

