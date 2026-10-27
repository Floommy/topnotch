package net.togyk.tcards.atm;

import java.util.LinkedHashMap;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * One price list for the whole server (every ATM uses it). Changed at a Central Bank block by ops.
 * A price is "amount items for diamonds diamonds"; 0 diamonds or 0 amount = not for sale.
 */
public class AtmPrices extends SavedData {
    public static final int MAX_VALUE = 999;

    public record Price(int diamonds, int amount) {
        public static final Codec<Price> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("diamonds").forGetter(Price::diamonds),
                Codec.INT.fieldOf("amount").forGetter(Price::amount)).apply(i, Price::new));

        public boolean onSale() {
            return diamonds > 0 && amount > 0;
        }

        /** Diamonds for qty items, rounded up (a single chip of a "10 for 1" price still costs 1 diamond). */
        public long cost(long qty) {
            return (qty * diamonds + amount - 1) / amount;
        }

        /** How many items the given diamonds buy. */
        public long affordable(long diamondsHeld) {
            return diamondsHeld * amount / diamonds;
        }
    }

    public static final Codec<AtmPrices> CODEC = Codec.unboundedMap(Codec.STRING, Price.CODEC)
            .xmap(AtmPrices::new, p -> p.prices);
    public static final SavedDataType<AtmPrices> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("tog_yks-cards", "atm_prices"), AtmPrices::new, CODEC, null);

    private final Map<String, Price> prices = new LinkedHashMap<>();

    public AtmPrices() {
        this(Map.of());
    }

    private AtmPrices(Map<String, Price> loaded) {
        for (AtmCatalog.Entry e : AtmCatalog.all()) {
            Price p = loaded.get(e.id());
            if (p != null && p.equals(oldDefault(e))) p = null;   // untouched value of blackjack.24 -> new default
            prices.put(e.id(), p != null ? clamp(p) : new Price(e.defaultDiamonds(), e.defaultAmount()));
        }
    }

    /** Defaults of blackjack.24 (10 chips per diamond, coins 64/16/4) - replaced by the new 1:1 default. */
    private static Price oldDefault(AtmCatalog.Entry e) {
        if (!e.coin()) return new Price(1, 10);
        if (e.id().endsWith("copper_coin")) return new Price(1, 64);
        if (e.id().endsWith("iron_coin")) return new Price(1, 16);
        return new Price(1, 4);
    }

    private static Price clamp(Price p) {
        return new Price(Math.max(0, Math.min(MAX_VALUE, p.diamonds())), Math.max(0, Math.min(MAX_VALUE, p.amount())));
    }

    public static AtmPrices get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public Price get(String id) {
        return prices.getOrDefault(id, new Price(0, 0));
    }

    public void set(String id, int diamonds, int amount) {
        if (AtmCatalog.byId(id) == null) return;
        prices.put(id, clamp(new Price(diamonds, amount)));
        setDirty();
    }
}
