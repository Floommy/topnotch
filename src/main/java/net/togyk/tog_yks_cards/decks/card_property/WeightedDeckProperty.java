package net.togyk.tog_yks_cards.decks.card_property;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.togyk.tcards.Pair;
import net.togyk.tog_yks_cards.decks.card_property.DeckProperty;

public class WeightedDeckProperty
extends DeckProperty {
    private final Map<String, Integer> weights;

    public WeightedDeckProperty(String name, int count, List<String> options, Map<String, Integer> weights) {
        super(name, count, options);
        weights = new HashMap<String, Integer>(weights);
        for (String option : options) {
            if (weights.containsKey(option)) continue;
            weights.put(option, 1);
        }
        this.weights = weights;
    }

    public WeightedDeckProperty(String name, int count, List<String> options, int defaultWeight) {
        super(name, count, options);
        HashMap<String, Integer> weights = new HashMap<String, Integer>();
        for (String option : options) {
            if (weights.containsKey(option)) continue;
            weights.put(option, defaultWeight);
        }
        this.weights = weights;
    }

    public WeightedDeckProperty(String name, int count, List<String> options, Map<String, Integer> weights, int defaultWeight) {
        super(name, count, options);
        weights = new HashMap<String, Integer>(weights);
        for (String option : options) {
            if (weights.containsKey(option)) continue;
            weights.put(option, defaultWeight);
        }
        this.weights = weights;
    }

    public WeightedDeckProperty(String name, List<String> options, Map<String, Integer> weights) {
        super(name, options);
        weights = new HashMap<String, Integer>(weights);
        for (String option : options) {
            if (weights.containsKey(option)) continue;
            weights.put(option, 1);
        }
        this.weights = weights;
    }

    public WeightedDeckProperty(String name, List<String> options, int defaultWeight) {
        super(name, options);
        HashMap<String, Integer> weights = new HashMap<String, Integer>();
        for (String option : options) {
            if (weights.containsKey(option)) continue;
            weights.put(option, defaultWeight);
        }
        this.weights = weights;
    }

    public WeightedDeckProperty(String name, List<String> options, Map<String, Integer> weights, int defaultWeight) {
        super(name, options);
        weights = new HashMap<String, Integer>(weights);
        for (String option : options) {
            if (weights.containsKey(option)) continue;
            weights.put(option, defaultWeight);
        }
        this.weights = weights;
    }

    @Override
    public List<List<Pair<String, String>>> getCardProperties(List<Pair<String, String>> otherProperties) {
        Map<String, Integer> weights = this.getWeights();
        ArrayList<List<Pair<String, String>>> cardProperties = new ArrayList<List<Pair<String, String>>>();
        for (String option : this.getOptions()) {
            ArrayList<Pair<String, String>> oneCardProperties = new ArrayList<Pair<String, String>>(otherProperties);
            oneCardProperties.add((Pair<String, String>)Pair.of(this.getName(), option));
            for (int i = 0; i < this.getCount(); ++i) {
                int weight = weights.getOrDefault(option, 1);
                for (int k = 0; k < weight; ++k) {
                    cardProperties.add(oneCardProperties);
                }
            }
        }
        return cardProperties;
    }

    public Map<String, Integer> getWeights() {
        return this.weights;
    }
}

