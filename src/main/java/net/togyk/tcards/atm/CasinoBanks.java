package net.togyk.tcards.atm;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.togyk.tcards.atm.AtmPrices.Price;

/**
 * Every Central Bank is its own casino: name, owner and price list. ATMs link to one bank by its id,
 * so prices work no matter how far away (or unloaded) the bank block is.
 */
public class CasinoBanks extends SavedData {
    public record Bank(String name, Optional<UUID> owner, String ownerName, Map<String, Price> prices) {
        public static final Codec<Bank> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("name").forGetter(Bank::name),
                UUIDUtil.CODEC.optionalFieldOf("owner").forGetter(Bank::owner),
                Codec.STRING.optionalFieldOf("owner_name", "").forGetter(Bank::ownerName),
                Codec.unboundedMap(Codec.STRING, Price.CODEC).fieldOf("prices").forGetter(Bank::prices)).apply(i, Bank::new));

        public Price price(String id) {
            Price p = prices.get(id);
            if (p != null) return p;
            AtmCatalog.Entry e = AtmCatalog.byId(id);
            return e == null ? new Price(0, 0) : new Price(e.defaultDiamonds(), e.defaultAmount());
        }

        public boolean canManage(Player player) {
            return owner.isEmpty() || owner.get().equals(player.getUUID()) || AtmBlockEntity.isAdmin(player);
        }
    }

    public static final Codec<CasinoBanks> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, Bank.CODEC)
            .xmap(CasinoBanks::new, b -> b.banks);
    public static final SavedDataType<CasinoBanks> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("tog_yks-cards", "casino_banks"), CasinoBanks::new, CODEC, null);

    private final Map<UUID, Bank> banks = new HashMap<>();

    public CasinoBanks() {
    }

    private CasinoBanks(Map<UUID, Bank> loaded) {
        banks.putAll(loaded);
    }

    public static CasinoBanks get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public Bank get(UUID id) {
        return id == null ? null : banks.get(id);
    }

    public UUID create(Player owner) {
        UUID id = UUID.randomUUID();
        Map<String, Price> prices = new LinkedHashMap<>();
        for (AtmCatalog.Entry e : AtmCatalog.all()) prices.put(e.id(), new Price(e.defaultDiamonds(), e.defaultAmount()));
        String who = owner == null ? "" : owner.getName().getString();
        banks.put(id, new Bank(who.isEmpty() ? "Casino" : who + "'s Casino",
                owner == null ? Optional.empty() : Optional.of(owner.getUUID()), who, prices));
        setDirty();
        return id;
    }

    /** Used when a bank item without data is placed or old data is missing. */
    public void ensure(UUID id, String name, Player owner) {
        if (banks.containsKey(id)) return;
        Map<String, Price> prices = new LinkedHashMap<>();
        for (AtmCatalog.Entry e : AtmCatalog.all()) prices.put(e.id(), new Price(e.defaultDiamonds(), e.defaultAmount()));
        banks.put(id, new Bank(name, owner == null ? Optional.empty() : Optional.of(owner.getUUID()),
                owner == null ? "" : owner.getName().getString(), prices));
        setDirty();
    }

    public void setPrice(UUID id, String item, int diamonds, int amount) {
        Bank b = banks.get(id);
        if (b == null || AtmCatalog.byId(item) == null) return;
        Map<String, Price> prices = new LinkedHashMap<>(b.prices());
        prices.put(item, new Price(Math.max(0, Math.min(AtmPrices.MAX_VALUE, diamonds)), Math.max(0, Math.min(AtmPrices.MAX_VALUE, amount))));
        banks.put(id, new Bank(b.name(), b.owner(), b.ownerName(), prices));
        setDirty();
    }

    public void rename(UUID id, String name) {
        Bank b = banks.get(id);
        if (b == null) return;
        name = name.strip();
        if (name.isEmpty()) return;
        if (name.length() > 24) name = name.substring(0, 24);
        banks.put(id, new Bank(name, b.owner(), b.ownerName(), b.prices()));
        setDirty();
    }
}
