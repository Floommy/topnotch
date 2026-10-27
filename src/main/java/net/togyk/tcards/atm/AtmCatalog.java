package net.togyk.tcards.atm;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.togyk.tog_yks_cards.item.ModItems;

/** Everything an ATM can hand out: 16 chip colours (dye order) and the 3 coins. */
public final class AtmCatalog {
    private AtmCatalog() {}

    public record Entry(String id, Item item, boolean coin, int defaultDiamonds, int defaultAmount) {}

    private static List<Entry> entries;

    public static List<Entry> all() {
        if (entries == null) {
            List<Entry> list = new ArrayList<>();
            Item[] chips = {ModItems.WHITE_CHIP, ModItems.LIGHT_GRAY_CHIP, ModItems.GRAY_CHIP, ModItems.BLACK_CHIP,
                    ModItems.BROWN_CHIP, ModItems.RED_CHIP, ModItems.ORANGE_CHIP, ModItems.YELLOW_CHIP,
                    ModItems.LIME_CHIP, ModItems.GREEN_CHIP, ModItems.CYAN_CHIP, ModItems.LIGHT_BLUE_CHIP,
                    ModItems.BLUE_CHIP, ModItems.PURPLE_CHIP, ModItems.MAGENTA_CHIP, ModItems.PINK_CHIP};
            for (Item chip : chips) list.add(new Entry(id(chip), chip, false, 1, 1));
            list.add(new Entry(id(ModItems.COPPER_COIN), ModItems.COPPER_COIN, true, 1, 1));
            list.add(new Entry(id(ModItems.IRON_COIN), ModItems.IRON_COIN, true, 1, 1));
            list.add(new Entry(id(ModItems.GOLD_COIN), ModItems.GOLD_COIN, true, 1, 1));
            entries = List.copyOf(list);
        }
        return entries;
    }

    public static Entry byId(String id) {
        for (Entry e : all()) if (e.id().equals(id)) return e;
        return null;
    }

    public static String id(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }
}
