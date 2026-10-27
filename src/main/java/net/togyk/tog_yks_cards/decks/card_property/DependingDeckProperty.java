package net.togyk.tog_yks_cards.decks.card_property;

import java.util.ArrayList;
import java.util.List;
import net.togyk.tcards.Pair;
import net.togyk.tog_yks_cards.decks.card_property.DeckProperty;

public class DependingDeckProperty
extends DeckProperty {
    private final DeckProperty dependedProperty;

    public DependingDeckProperty(String name, int count, DeckProperty dependedProperty, List<String> options) {
        super(name, count, options);
        this.dependedProperty = dependedProperty;
    }

    public DependingDeckProperty(String name, int count, DeckProperty dependedProperty, String ... options) {
        super(name, count, options);
        this.dependedProperty = dependedProperty;
    }

    public DependingDeckProperty(String name, int count, DeckProperty dependedProperty, String option) {
        super(name, count, option);
        this.dependedProperty = dependedProperty;
    }

    public DependingDeckProperty(String name, DeckProperty dependedProperty, List<String> options) {
        super(name, options);
        this.dependedProperty = dependedProperty;
    }

    public DependingDeckProperty(String name, DeckProperty dependedProperty, String ... options) {
        super(name, options);
        this.dependedProperty = dependedProperty;
    }

    public DependingDeckProperty(String name, DeckProperty dependedProperty, String option) {
        super(name, option);
        this.dependedProperty = dependedProperty;
    }

    public DeckProperty getDependedProperty() {
        return this.dependedProperty;
    }

    @Override
    public List<List<Pair<String, String>>> getCardProperties(List<Pair<String, String>> otherProperties) {
        ArrayList<List<Pair<String, String>>> cardProperties = new ArrayList<List<Pair<String, String>>>();
        for (String option : this.getOptions()) {
            ArrayList<Pair<String, String>> oneCardProperties = new ArrayList<Pair<String, String>>(otherProperties);
            oneCardProperties.add((Pair<String, String>)Pair.of(this.getName(), option));
            for (int i = 0; i < this.getCount(); ++i) {
                cardProperties.addAll(this.getDependedProperty().getCardProperties(oneCardProperties));
            }
        }
        return cardProperties;
    }
}

