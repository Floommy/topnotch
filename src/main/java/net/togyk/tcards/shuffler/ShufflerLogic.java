package net.togyk.tcards.shuffler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.togyk.tog_yks_cards.decks.PlayingCardDeck;
import net.togyk.tog_yks_cards.decks.UnoDeck;
import net.togyk.tog_yks_cards.item.ModItems;
import net.togyk.tog_yks_cards.item.custom.CardItem;
import net.togyk.tog_yks_cards.item.custom.CardpileItem;

/** Presets, card matching and deck composition for the card shuffler. Pure logic, no world access. */
public final class ShufflerLogic {
    private ShufflerLogic() {}

    public enum Kind { FRENCH, UNO }

    public static final String[] RANKS = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "jack", "queen", "king", "ace"};
    public static final String[] RANK_LABELS = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A"};
    public static final int ALL_RANKS = (1 << RANKS.length) - 1;

    public record Preset(String label, Kind kind, int decks, int rankMask, boolean jokers) {}

    private static int ranksFrom(String lowest) {
        int mask = 0;
        boolean on = false;
        for (int i = 0; i < RANKS.length; i++) {
            if (RANKS[i].equals(lowest)) on = true;
            if (on) mask |= 1 << i;
        }
        return mask;
    }

    /** Index = network id. The last entry is the custom preset (its values come from the block entity). */
    public static final List<Preset> PRESETS = List.of(
            new Preset("Poker / Bridge (52)", Kind.FRENCH, 1, ALL_RANKS, false),
            new Preset("Deck + Jokers (54)", Kind.FRENCH, 1, ALL_RANKS, true),
            new Preset("Blackjack 1 Deck", Kind.FRENCH, 1, ALL_RANKS, false),
            new Preset("Blackjack 2 Decks", Kind.FRENCH, 2, ALL_RANKS, false),
            new Preset("Blackjack 4 Decks", Kind.FRENCH, 4, ALL_RANKS, false),
            new Preset("Blackjack 6 Decks", Kind.FRENCH, 6, ALL_RANKS, false),
            new Preset("Blackjack 8 Decks", Kind.FRENCH, 8, ALL_RANKS, false),
            new Preset("Skat (32)", Kind.FRENCH, 1, ranksFrom("7"), false),
            new Preset("Doppelkopf (48)", Kind.FRENCH, 2, ranksFrom("9"), false),
            new Preset("Euchre (24)", Kind.FRENCH, 1, ranksFrom("9"), false),
            new Preset("Durak (36)", Kind.FRENCH, 1, ranksFrom("6"), false),
            new Preset("Canasta / Rummy (108)", Kind.FRENCH, 2, ALL_RANKS, true),
            new Preset("Uno 1 Deck", Kind.UNO, 1, 0, false),
            new Preset("Uno 2 Decks", Kind.UNO, 2, 0, false),
            new Preset("Uno 3 Decks", Kind.UNO, 3, 0, false),
            new Preset("Uno 4 Decks", Kind.UNO, 4, 0, false),
            new Preset("Custom", Kind.FRENCH, 1, ALL_RANKS, false));

    public static final int CUSTOM = PRESETS.size() - 1;

    public static Preset preset(int id, int customRanks, int customDecks, boolean customJokers) {
        if (id == CUSTOM) return new Preset("Custom", Kind.FRENCH, Math.max(1, customDecks), customRanks, customJokers);
        if (id < 0 || id >= PRESETS.size()) return PRESETS.get(0);
        return PRESETS.get(id);
    }

    // ------------------------------------------------------------------ card keys

    public static Map<String, String> data(ItemStack card) {
        if (card.getItem() instanceof CardItem ci) return ci.getCardData(card);
        return Map.of();
    }

    /** Identity of a card regardless of its back colour, e.g. {joker_or_card=suit, rank=king, suit=heart}. */
    public static String key(ItemStack card) {
        return new TreeMap<>(data(card)).toString();
    }

    public static Kind kind(ItemStack card) {
        if (card.getItem() instanceof CardItem ci) {
            if (ci.getDeck() instanceof UnoDeck) return Kind.UNO;
            if (ci.getDeck() instanceof PlayingCardDeck) return Kind.FRENCH;
        }
        return null;
    }

    public static boolean isJoker(ItemStack card) {
        return "joker".equals(data(card).get("joker_or_card"));
    }

    private static List<ItemStack> baseDeck(Kind kind) {
        return kind == Kind.UNO ? ModItems.UNO_CARDBOX.getCards() : ModItems.RED_CARDBOX.getCards();
    }

    /** Canonical order of card keys (for sorting a deck back into order). */
    private static Map<String, Integer> canonicalOrder() {
        Map<String, Integer> order = new HashMap<>();
        int i = 0;
        for (ItemStack c : baseDeck(Kind.FRENCH)) order.putIfAbsent(key(c), i++);
        for (ItemStack c : baseDeck(Kind.UNO)) order.putIfAbsent(key(c), i++);
        return order;
    }

    /** How many of each card key a preset needs. */
    public static Map<String, Integer> requirement(Preset p) {
        Map<String, Integer> req = new LinkedHashMap<>();
        for (ItemStack card : baseDeck(p.kind())) {
            if (p.kind() == Kind.FRENCH) {
                Map<String, String> d = data(card);
                if (isJoker(card)) {
                    if (!p.jokers()) continue;
                } else {
                    int rank = indexOf(d.get("rank"));
                    if (rank < 0 || (p.rankMask() & (1 << rank)) == 0) continue;
                }
            }
            req.merge(key(card), p.decks(), Integer::sum);
        }
        return req;
    }

    private static int indexOf(String rank) {
        for (int i = 0; i < RANKS.length; i++) if (RANKS[i].equals(rank)) return i;
        return -1;
    }

    public static int size(Map<String, Integer> req) {
        int n = 0;
        for (int c : req.values()) n += c;
        return n;
    }

    // ------------------------------------------------------------------ composition

    private static boolean matches(ItemStack card, Kind kind, String colorFilter) {
        if (kind(card) != kind) return false;
        return colorFilter == null || colorFilter.isEmpty() || colorId(card.getItem()).equals(colorFilter);
    }

    public static String colorId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    public static boolean canCompose(List<ItemStack> pool, Preset p, String colorFilter) {
        Map<String, Integer> req = requirement(p);
        if (req.isEmpty()) return false;
        Map<String, Integer> have = new HashMap<>();
        for (ItemStack c : pool) if (matches(c, p.kind(), colorFilter)) have.merge(key(c), 1, Integer::sum);
        for (Map.Entry<String, Integer> e : req.entrySet()) {
            if (have.getOrDefault(e.getKey(), 0) < e.getValue()) return false;
        }
        return true;
    }

    /**
     * Takes the cards for a preset out of the pool and returns them shuffled, or null if not enough cards.
     * Cards of the most common back colour are preferred, so decks stay as tidy as possible.
     */
    public static List<ItemStack> compose(List<ItemStack> pool, Preset p, String colorFilter) {
        if (!canCompose(pool, p, colorFilter)) return null;
        Map<String, Integer> req = requirement(p);
        Map<Item, Integer> colorCount = new HashMap<>();
        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < pool.size(); i++) {
            ItemStack c = pool.get(i);
            if (matches(c, p.kind(), colorFilter)) {
                candidates.add(i);
                colorCount.merge(c.getItem(), 1, Integer::sum);
            }
        }
        candidates.sort((a, b) -> Integer.compare(colorCount.get(pool.get(b).getItem()), colorCount.get(pool.get(a).getItem())));
        Map<String, Integer> still = new HashMap<>(req);
        List<Integer> chosen = new ArrayList<>();
        for (int i : candidates) {
            String k = key(pool.get(i));
            int need = still.getOrDefault(k, 0);
            if (need > 0) {
                chosen.add(i);
                still.put(k, need - 1);
            }
        }
        chosen.sort(Collections.reverseOrder());
        List<ItemStack> out = new ArrayList<>();
        for (int i : chosen) out.add(pool.remove(i));
        Collections.shuffle(out);
        return out;
    }

    /** Cards in canonical order (suit by suit, 2..A, jokers last; Uno by colour and value). */
    public static List<ItemStack> sorted(List<ItemStack> cards) {
        Map<String, Integer> order = canonicalOrder();
        List<ItemStack> out = new ArrayList<>(cards);
        out.sort((a, b) -> Integer.compare(order.getOrDefault(key(a), 9999), order.getOrDefault(key(b), 9999)));
        return out;
    }

    /** Groups cards by back colour, each group in canonical order. */
    public static Map<Item, List<ItemStack>> sortByColor(List<ItemStack> cards) {
        Map<String, Integer> order = canonicalOrder();
        Map<Item, List<ItemStack>> groups = new LinkedHashMap<>();
        for (ItemStack c : cards) groups.computeIfAbsent(c.getItem(), k -> new ArrayList<>()).add(c);
        for (List<ItemStack> g : groups.values()) {
            g.sort((a, b) -> Integer.compare(order.getOrDefault(key(a), 9999), order.getOrDefault(key(b), 9999)));
        }
        return groups;
    }

    /** Pile item that should hold these cards (pile of the most common back colour). */
    public static ItemStack toPile(List<ItemStack> cards) {
        Map<Item, Integer> count = new HashMap<>();
        for (ItemStack c : cards) count.merge(c.getItem(), 1, Integer::sum);
        Item best = null;
        int bestN = -1;
        for (Map.Entry<Item, Integer> e : count.entrySet()) {
            if (e.getValue() > bestN) {
                best = e.getKey();
                bestN = e.getValue();
            }
        }
        if (best instanceof CardItem ci && ci.getBlock().asItem() instanceof CardpileItem pileItem) {
            ItemStack pile = pileItem.getDefaultInstance();
            List<ItemStack> copy = new ArrayList<>();
            for (ItemStack c : cards) copy.add(c.copyWithCount(1));
            pileItem.setCards(copy, pile);
            return pile;
        }
        return ItemStack.EMPTY;
    }

    /** Back colours present (item id -> count), most common first. */
    public static Map<String, Integer> colors(List<ItemStack> cards) {
        Map<String, Integer> m = new HashMap<>();
        for (ItemStack c : cards) m.merge(colorId(c.getItem()), 1, Integer::sum);
        List<Map.Entry<String, Integer>> list = new ArrayList<>(m.entrySet());
        list.sort((a, b) -> b.getValue() - a.getValue());
        Map<String, Integer> out = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : list) out.put(e.getKey(), e.getValue());
        return out;
    }

}
