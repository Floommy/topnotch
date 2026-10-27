package net.togyk.tcards.restaurant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.togyk.tcards.atm.AtmBlockEntity;

/** All restaurants of the server: name, owner, currency, prices, open orders, cash box. */
public class Restaurants extends SavedData {
    public static final int FIRST_NUMBER = 103, LAST_NUMBER = 400;
    public static final int KITCHEN = 0, READY = 1;
    /** A ready order disappears from the pickup screen after 10 minutes. */
    public static final long READY_TIMEOUT = 20 * 60 * 10;
    /** Forgotten kitchen orders are dropped after 1 hour. */
    public static final long KITCHEN_TIMEOUT = 20 * 60 * 60;
    public static final long VAULT_CAPACITY = 27 * 64;

    public record Line(String item, int qty) {
        public static final Codec<Line> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("item").forGetter(Line::item),
                Codec.INT.fieldOf("qty").forGetter(Line::qty)).apply(i, Line::new));
    }

    public static final class Order {
        public static final Codec<Order> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("number").forGetter(o -> o.number),
                Line.CODEC.listOf().fieldOf("lines").forGetter(o -> o.lines),
                Codec.INT.fieldOf("status").forGetter(o -> o.status),
                Codec.LONG.fieldOf("created").forGetter(o -> o.created),
                Codec.LONG.optionalFieldOf("ready_at", 0L).forGetter(o -> o.readyAt),
                Codec.STRING.optionalFieldOf("customer", "").forGetter(o -> o.customer)).apply(i, Order::new));

        public final int number;
        public final List<Line> lines;
        public int status;
        public final long created;
        public long readyAt;
        public final String customer;

        public Order(int number, List<Line> lines, int status, long created, long readyAt, String customer) {
            this.number = number;
            this.lines = List.copyOf(lines);
            this.status = status;
            this.created = created;
            this.readyAt = readyAt;
            this.customer = customer;
        }
    }

    public static final class Restaurant {
        public static final Codec<Restaurant> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("name").forGetter(r -> r.name),
                UUIDUtil.CODEC.optionalFieldOf("owner").forGetter(r -> r.owner),
                Codec.STRING.optionalFieldOf("owner_name", "").forGetter(r -> r.ownerName),
                Codec.STRING.optionalFieldOf("currency", "diamond").forGetter(r -> r.currency),
                Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("prices").forGetter(r -> r.prices),
                Codec.INT.optionalFieldOf("next", FIRST_NUMBER).forGetter(r -> r.next),
                Order.CODEC.listOf().fieldOf("orders").forGetter(r -> r.orders),
                Codec.LONG.optionalFieldOf("vault", 0L).forGetter(r -> r.vault),
                GlobalPos.CODEC.optionalFieldOf("link").forGetter(r -> r.link),
                Codec.LONG.optionalFieldOf("sales", 0L).forGetter(r -> r.sales)).apply(i, Restaurant::new));

        public String name;
        public Optional<UUID> owner;
        public String ownerName;
        public String currency;
        public final Map<String, Integer> prices;
        public int next;
        public final List<Order> orders;
        public long vault;
        public Optional<GlobalPos> link;
        public long sales;

        Restaurant(String name, Optional<UUID> owner, String ownerName, String currency, Map<String, Integer> prices,
                   int next, List<Order> orders, long vault, Optional<GlobalPos> link, long sales) {
            this.name = name;
            this.owner = owner;
            this.ownerName = ownerName;
            this.currency = "emerald".equals(currency) ? "emerald" : "diamond";
            this.prices = new LinkedHashMap<>(prices);
            this.next = next;
            this.orders = new ArrayList<>(orders);
            this.vault = vault;
            this.link = link;
            this.sales = sales;
        }

        public boolean canManage(Player player) {
            return owner.isEmpty() || owner.get().equals(player.getUUID()) || AtmBlockEntity.isAdmin(player);
        }

        public Item currencyItem() {
            return "emerald".equals(currency) ? Items.EMERALD : Items.DIAMOND;
        }

        /** Price of one item in the restaurant currency; 0 = not sold. */
        public int price(String item) {
            return prices.getOrDefault(item, 1);
        }

        public long total(List<Line> lines) {
            long t = 0;
            for (Line l : lines) t += (long) price(l.item()) * l.qty();
            return t;
        }

        public Order order(int number) {
            for (Order o : orders) if (o.number == number) return o;
            return null;
        }

        int takeNumber() {
            for (int tries = 0; tries <= LAST_NUMBER - FIRST_NUMBER; tries++) {
                int n = next;
                next = next >= LAST_NUMBER ? FIRST_NUMBER : next + 1;
                if (order(n) == null) return n;
            }
            return next;
        }
    }

    public static final Codec<Restaurants> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, Restaurant.CODEC)
            .xmap(Restaurants::new, r -> r.restaurants);
    public static final SavedDataType<Restaurants> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("tog_yks-cards", "restaurants"), Restaurants::new, CODEC, null);

    private final Map<UUID, Restaurant> restaurants = new HashMap<>();

    public Restaurants() {
    }

    private Restaurants(Map<UUID, Restaurant> loaded) {
        restaurants.putAll(loaded);
    }

    public static Restaurants get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public Restaurant get(UUID id) {
        return id == null ? null : restaurants.get(id);
    }

    public Map<UUID, Restaurant> all() {
        return restaurants;
    }

    public UUID create(Player owner) {
        UUID id = UUID.randomUUID();
        Map<String, Integer> prices = new LinkedHashMap<>();
        for (String item : RestaurantMenu.all()) prices.put(item, 1);
        String who = owner == null ? "" : owner.getName().getString();
        restaurants.put(id, new Restaurant(who.isEmpty() ? "McDonald's" : who + "'s McDonald's",
                owner == null ? Optional.empty() : Optional.of(owner.getUUID()), who, "diamond", prices,
                FIRST_NUMBER, List.of(), 0, Optional.empty(), 0));
        setDirty();
        return id;
    }

    public Order placeOrder(UUID id, List<Line> lines, String customer, long time) {
        Restaurant r = restaurants.get(id);
        if (r == null) return null;
        Order o = new Order(r.takeNumber(), lines, KITCHEN, time, 0, customer);
        r.orders.add(o);
        setDirty();
        return o;
    }

    public boolean setStatus(UUID id, int number, int status, long time) {
        Restaurant r = restaurants.get(id);
        Order o = r == null ? null : r.order(number);
        if (o == null) return false;
        o.status = status;
        if (status == READY) o.readyAt = time;
        setDirty();
        return true;
    }

    public boolean remove(UUID id, int number) {
        Restaurant r = restaurants.get(id);
        if (r == null) return false;
        boolean removed = r.orders.removeIf(o -> o.number == number);
        if (removed) setDirty();
        return removed;
    }

    /** Drops ready orders nobody picked up and forgotten kitchen orders. */
    public void expire(long time) {
        boolean changed = false;
        for (Restaurant r : restaurants.values()) {
            changed |= r.orders.removeIf(o -> (o.status == READY && time - o.readyAt > READY_TIMEOUT)
                    || (o.status == KITCHEN && time - o.created > KITCHEN_TIMEOUT));
        }
        if (changed) setDirty();
    }

    public void changed() {
        setDirty();
    }
}
