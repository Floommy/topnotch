package net.togyk.tcards.restaurant;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Menu of the restaurant: the finished meals, drinks and dips of the Better McDonald's Mod.
 * Items are looked up by name, so this jar still loads without that mod (the menu is then empty).
 */
public final class RestaurantMenu {
    private RestaurantMenu() {}

    public static final String NS = "better_mcdonalds_mod";

    public record Category(String name, String icon, List<String> items) {}

    public static final List<Category> CATEGORIES = List.of(
            new Category("Burgers", "big_mac", List.of("big_mac", "hamburger", "cheeseburger", "mcbacon")),
            new Category("Chicken", "mcchicken", List.of("mcchicken", "chickenburger", "chicken_mcnuggets", "filet_o_fish", "mcwrap")),
            new Category("Sides", "fries", List.of("fries", "snack_salad")),
            new Category("Happy Meal", "happy_meal", List.of("happy_meal")),
            new Category("Desserts", "mcflurry", List.of("mcflurry", "mcflurry_chocolate")),
            new Category("Drinks", "coca_cola", List.of("coca_cola", "fanta", "sprite", "lipton_ice_tea_peach")),
            new Category("Dips", "ketchup", List.of("ketchup", "mayonnaise", "mustard", "sweet_sour_sauce")));

    /** All menu item paths in menu order. */
    public static List<String> all() {
        List<String> list = new ArrayList<>();
        for (Category c : CATEGORIES) list.addAll(c.items());
        return list;
    }

    public static boolean onMenu(String path) {
        return all().contains(path);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(NS, path);
    }

    public static Item item(String path) {
        return BuiltInRegistries.ITEM.getValue(id(path));
    }

    /** True when the McDonald's mod is installed (the item exists). */
    public static boolean available(String path) {
        Item i = item(path);
        return i != null && i != Items.AIR;
    }

    public static ItemStack stack(String path) {
        return available(path) ? new ItemStack(item(path)) : ItemStack.EMPTY;
    }

    /** Menu path of a stack, or null if it is not a menu item. */
    public static String pathOf(ItemStack stack) {
        if (stack.isEmpty()) return null;
        Identifier key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!NS.equals(key.getNamespace()) || !onMenu(key.getPath())) return null;
        return key.getPath();
    }

    /** Short display name ("Chicken McNuggets" -> "McNuggets") for small places. */
    public static String shortName(String path) {
        return switch (path) {
            case "chicken_mcnuggets" -> "McNuggets";
            case "lipton_ice_tea_peach" -> "Ice Tea";
            case "mcflurry_chocolate" -> "McFlurry Choc";
            case "sweet_sour_sauce" -> "Sweet & Sour";
            case "snack_salad" -> "Salad";
            case "coca_cola" -> "Coca-Cola";
            case "filet_o_fish" -> "Filet-O-Fish";
            default -> {
                ItemStack s = stack(path);
                yield s.isEmpty() ? path : s.getHoverName().getString();
            }
        };
    }
}
