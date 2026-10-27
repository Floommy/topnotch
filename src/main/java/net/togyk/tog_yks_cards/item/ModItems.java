package net.togyk.tog_yks_cards.item;

import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.togyk.tog_yks_cards.block.ModBlocks;
import net.togyk.tog_yks_cards.decks.ModDecks;
import net.togyk.tog_yks_cards.item.custom.CardItem;
import net.togyk.tog_yks_cards.item.custom.CardboxItem;
import net.togyk.tog_yks_cards.item.custom.CoinItem;

public class ModItems {
    public static CardItem BLACK_PLAYING_CARD = ModItems.registerItem("black_playing_card", properties -> new CardItem((Item.Properties)properties, ModDecks.BLACK_PLAYING_CARDS, ModBlocks.BLACK_CARDPILE), new Item.Properties());
    public static CardboxItem BLACK_CARDBOX = ModItems.registerItem("black_cardbox", properties -> new CardboxItem((Block)ModBlocks.BLACK_CARDBOX, ModBlocks.BLACK_CARDPILE, (Item.Properties)properties, ModDecks.BLACK_PLAYING_CARDS, (Item)BLACK_PLAYING_CARD), new Item.Properties().stacksTo(1));
    public static CardItem BLUE_PLAYING_CARD = ModItems.registerItem("blue_playing_card", properties -> new CardItem((Item.Properties)properties, ModDecks.BLUE_PLAYING_CARDS, ModBlocks.BLUE_CARDPILE), new Item.Properties());
    public static CardboxItem BLUE_CARDBOX = ModItems.registerItem("blue_cardbox", properties -> new CardboxItem((Block)ModBlocks.BLUE_CARDBOX, ModBlocks.BLUE_CARDPILE, (Item.Properties)properties, ModDecks.BLUE_PLAYING_CARDS, (Item)BLUE_PLAYING_CARD), new Item.Properties().stacksTo(1));
    public static CardItem BROWN_PLAYING_CARD = ModItems.registerItem("brown_playing_card", properties -> new CardItem((Item.Properties)properties, ModDecks.BROWN_PLAYING_CARDS, ModBlocks.BROWN_CARDPILE), new Item.Properties());
    public static CardboxItem BROWN_CARDBOX = ModItems.registerItem("brown_cardbox", properties -> new CardboxItem((Block)ModBlocks.BROWN_CARDBOX, ModBlocks.BROWN_CARDPILE, (Item.Properties)properties, ModDecks.BROWN_PLAYING_CARDS, (Item)BROWN_PLAYING_CARD), new Item.Properties().stacksTo(1));
    public static CardItem CYAN_PLAYING_CARD = ModItems.registerItem("cyan_playing_card", properties -> new CardItem((Item.Properties)properties, ModDecks.CYAN_PLAYING_CARDS, ModBlocks.CYAN_CARDPILE), new Item.Properties());
    public static CardboxItem CYAN_CARDBOX = ModItems.registerItem("cyan_cardbox", properties -> new CardboxItem((Block)ModBlocks.CYAN_CARDBOX, ModBlocks.CYAN_CARDPILE, (Item.Properties)properties, ModDecks.CYAN_PLAYING_CARDS, (Item)CYAN_PLAYING_CARD), new Item.Properties().stacksTo(1));
    public static CardItem GRAY_PLAYING_CARD = ModItems.registerItem("gray_playing_card", properties -> new CardItem((Item.Properties)properties, ModDecks.GRAY_PLAYING_CARDS, ModBlocks.GRAY_CARDPILE), new Item.Properties());
    public static CardboxItem GRAY_CARDBOX = ModItems.registerItem("gray_cardbox", properties -> new CardboxItem((Block)ModBlocks.GRAY_CARDBOX, ModBlocks.GRAY_CARDPILE, (Item.Properties)properties, ModDecks.GRAY_PLAYING_CARDS, (Item)GRAY_PLAYING_CARD), new Item.Properties().stacksTo(1));
    public static CardItem GREEN_PLAYING_CARD = ModItems.registerItem("green_playing_card", properties -> new CardItem((Item.Properties)properties, ModDecks.GREEN_PLAYING_CARDS, ModBlocks.GREEN_CARDPILE), new Item.Properties());
    public static CardboxItem GREEN_CARDBOX = ModItems.registerItem("green_cardbox", properties -> new CardboxItem((Block)ModBlocks.GREEN_CARDBOX, ModBlocks.GREEN_CARDPILE, (Item.Properties)properties, ModDecks.GREEN_PLAYING_CARDS, (Item)GREEN_PLAYING_CARD), new Item.Properties().stacksTo(1));
    public static CardItem LIGHT_BLUE_PLAYING_CARD = ModItems.registerItem("light_blue_playing_card", properties -> new CardItem((Item.Properties)properties, ModDecks.LIGHT_BLUE_PLAYING_CARDS, ModBlocks.LIGHT_BLUE_CARDPILE), new Item.Properties());
    public static CardboxItem LIGHT_BLUE_CARDBOX = ModItems.registerItem("light_blue_cardbox", properties -> new CardboxItem((Block)ModBlocks.LIGHT_BLUE_CARDBOX, ModBlocks.LIGHT_BLUE_CARDPILE, (Item.Properties)properties, ModDecks.LIGHT_BLUE_PLAYING_CARDS, (Item)LIGHT_BLUE_PLAYING_CARD), new Item.Properties().stacksTo(1));
    public static CardItem LIGHT_GRAY_PLAYING_CARD = ModItems.registerItem("light_gray_playing_card", properties -> new CardItem((Item.Properties)properties, ModDecks.LIGHT_GRAY_PLAYING_CARDS, ModBlocks.LIGHT_GRAY_CARDPILE), new Item.Properties());
    public static CardboxItem LIGHT_GRAY_CARDBOX = ModItems.registerItem("light_gray_cardbox", properties -> new CardboxItem((Block)ModBlocks.LIGHT_GRAY_CARDBOX, ModBlocks.LIGHT_GRAY_CARDPILE, (Item.Properties)properties, ModDecks.LIGHT_GRAY_PLAYING_CARDS, (Item)LIGHT_GRAY_PLAYING_CARD), new Item.Properties().stacksTo(1));
    public static CardItem LIME_PLAYING_CARD = ModItems.registerItem("lime_playing_card", properties -> new CardItem((Item.Properties)properties, ModDecks.LIME_PLAYING_CARDS, ModBlocks.LIME_CARDPILE), new Item.Properties());
    public static CardboxItem LIME_CARDBOX = ModItems.registerItem("lime_cardbox", properties -> new CardboxItem((Block)ModBlocks.LIME_CARDBOX, ModBlocks.LIME_CARDPILE, (Item.Properties)properties, ModDecks.LIME_PLAYING_CARDS, (Item)LIME_PLAYING_CARD), new Item.Properties().stacksTo(1));
    public static CardItem MAGENTA_PLAYING_CARD = ModItems.registerItem("magenta_playing_card", properties -> new CardItem((Item.Properties)properties, ModDecks.MAGENTA_PLAYING_CARDS, ModBlocks.MAGENTA_CARDPILE), new Item.Properties());
    public static CardboxItem MAGENTA_CARDBOX = ModItems.registerItem("magenta_cardbox", properties -> new CardboxItem((Block)ModBlocks.MAGENTA_CARDBOX, ModBlocks.MAGENTA_CARDPILE, (Item.Properties)properties, ModDecks.MAGENTA_PLAYING_CARDS, (Item)MAGENTA_PLAYING_CARD), new Item.Properties().stacksTo(1));
    public static CardItem ORANGE_PLAYING_CARD = ModItems.registerItem("orange_playing_card", properties -> new CardItem((Item.Properties)properties, ModDecks.ORANGE_PLAYING_CARDS, ModBlocks.ORANGE_CARDPILE), new Item.Properties());
    public static CardboxItem ORANGE_CARDBOX = ModItems.registerItem("orange_cardbox", properties -> new CardboxItem((Block)ModBlocks.ORANGE_CARDBOX, ModBlocks.ORANGE_CARDPILE, (Item.Properties)properties, ModDecks.ORANGE_PLAYING_CARDS, (Item)ORANGE_PLAYING_CARD), new Item.Properties().stacksTo(1));
    public static CardItem PINK_PLAYING_CARD = ModItems.registerItem("pink_playing_card", properties -> new CardItem((Item.Properties)properties, ModDecks.PINK_PLAYING_CARDS, ModBlocks.PINK_CARDPILE), new Item.Properties());
    public static CardboxItem PINK_CARDBOX = ModItems.registerItem("pink_cardbox", properties -> new CardboxItem((Block)ModBlocks.PINK_CARDBOX, ModBlocks.PINK_CARDPILE, (Item.Properties)properties, ModDecks.PINK_PLAYING_CARDS, (Item)PINK_PLAYING_CARD), new Item.Properties().stacksTo(1));
    public static CardItem PURPLE_PLAYING_CARD = ModItems.registerItem("purple_playing_card", properties -> new CardItem((Item.Properties)properties, ModDecks.PURPLE_PLAYING_CARDS, ModBlocks.PURPLE_CARDPILE), new Item.Properties());
    public static CardboxItem PURPLE_CARDBOX = ModItems.registerItem("purple_cardbox", properties -> new CardboxItem((Block)ModBlocks.PURPLE_CARDBOX, ModBlocks.PURPLE_CARDPILE, (Item.Properties)properties, ModDecks.PURPLE_PLAYING_CARDS, (Item)PURPLE_PLAYING_CARD), new Item.Properties().stacksTo(1));
    public static CardItem RED_PLAYING_CARD = ModItems.registerItem("red_playing_card", properties -> new CardItem((Item.Properties)properties, ModDecks.RED_PLAYING_CARDS, ModBlocks.RED_CARDPILE), new Item.Properties());
    public static CardboxItem RED_CARDBOX = ModItems.registerItem("red_cardbox", properties -> new CardboxItem((Block)ModBlocks.RED_CARDBOX, ModBlocks.RED_CARDPILE, (Item.Properties)properties, ModDecks.RED_PLAYING_CARDS, (Item)RED_PLAYING_CARD), new Item.Properties().stacksTo(1));
    public static CardItem WHITE_PLAYING_CARD = ModItems.registerItem("white_playing_card", properties -> new CardItem((Item.Properties)properties, ModDecks.WHITE_PLAYING_CARDS, ModBlocks.WHITE_CARDPILE), new Item.Properties());
    public static CardboxItem WHITE_CARDBOX = ModItems.registerItem("white_cardbox", properties -> new CardboxItem((Block)ModBlocks.WHITE_CARDBOX, ModBlocks.WHITE_CARDPILE, (Item.Properties)properties, ModDecks.WHITE_PLAYING_CARDS, (Item)WHITE_PLAYING_CARD), new Item.Properties().stacksTo(1));
    public static CardItem YELLOW_PLAYING_CARD = ModItems.registerItem("yellow_playing_card", properties -> new CardItem((Item.Properties)properties, ModDecks.YELLOW_PLAYING_CARDS, ModBlocks.YELLOW_CARDPILE), new Item.Properties());
    public static CardboxItem YELLOW_CARDBOX = ModItems.registerItem("yellow_cardbox", properties -> new CardboxItem((Block)ModBlocks.YELLOW_CARDBOX, ModBlocks.YELLOW_CARDPILE, (Item.Properties)properties, ModDecks.YELLOW_PLAYING_CARDS, (Item)YELLOW_PLAYING_CARD), new Item.Properties().stacksTo(1));
    public static CardItem COPPER_PLAYING_CARD = ModItems.registerItem("copper_playing_card", properties -> new CardItem((Item.Properties)properties, ModDecks.COPPER_PLAYING_CARDS, ModBlocks.COPPER_CARDPILE), new Item.Properties());
    public static CardboxItem COPPER_CARDBOX = ModItems.registerItem("copper_cardbox", properties -> new CardboxItem((Block)ModBlocks.COPPER_CARDBOX, ModBlocks.COPPER_CARDPILE, (Item.Properties)properties, ModDecks.COPPER_PLAYING_CARDS, (Item)COPPER_PLAYING_CARD), new Item.Properties().stacksTo(1));
    public static CardItem IRON_PLAYING_CARD = ModItems.registerItem("iron_playing_card", properties -> new CardItem((Item.Properties)properties, ModDecks.IRON_PLAYING_CARDS, ModBlocks.IRON_CARDPILE), new Item.Properties());
    public static CardboxItem IRON_CARDBOX = ModItems.registerItem("iron_cardbox", properties -> new CardboxItem((Block)ModBlocks.IRON_CARDBOX, ModBlocks.IRON_CARDPILE, (Item.Properties)properties, ModDecks.IRON_PLAYING_CARDS, (Item)IRON_PLAYING_CARD), new Item.Properties().stacksTo(1));
    public static CardItem GOLD_PLAYING_CARD = ModItems.registerItem("gold_playing_card", properties -> new CardItem((Item.Properties)properties, ModDecks.GOLD_PLAYING_CARDS, ModBlocks.GOLD_CARDPILE), new Item.Properties());
    public static CardboxItem GOLD_CARDBOX = ModItems.registerItem("gold_cardbox", properties -> new CardboxItem((Block)ModBlocks.GOLD_CARDBOX, ModBlocks.GOLD_CARDPILE, (Item.Properties)properties, ModDecks.GOLD_PLAYING_CARDS, (Item)GOLD_PLAYING_CARD), new Item.Properties().stacksTo(1));
    public static CardItem DIAMOND_PLAYING_CARD = ModItems.registerItem("diamond_playing_card", properties -> new CardItem((Item.Properties)properties, ModDecks.DIAMOND_PLAYING_CARDS, ModBlocks.DIAMOND_CARDPILE), new Item.Properties());
    public static CardboxItem DIAMOND_CARDBOX = ModItems.registerItem("diamond_cardbox", properties -> new CardboxItem((Block)ModBlocks.DIAMOND_CARDBOX, ModBlocks.DIAMOND_CARDPILE, (Item.Properties)properties, ModDecks.DIAMOND_PLAYING_CARDS, (Item)DIAMOND_PLAYING_CARD), new Item.Properties().stacksTo(1));
    public static CardItem NETHERITE_PLAYING_CARD = ModItems.registerItem("netherite_playing_card", properties -> new CardItem((Item.Properties)properties, ModDecks.NETHERITE_PLAYING_CARDS, ModBlocks.NETHERITE_CARDPILE), new Item.Properties());
    public static CardboxItem NETHERITE_CARDBOX = ModItems.registerItem("netherite_cardbox", properties -> new CardboxItem((Block)ModBlocks.NETHERITE_CARDBOX, ModBlocks.NETHERITE_CARDPILE, (Item.Properties)properties, ModDecks.NETHERITE_PLAYING_CARDS, (Item)NETHERITE_PLAYING_CARD), new Item.Properties().stacksTo(1));
    public static CardItem AMETHYST_PLAYING_CARD = ModItems.registerItem("amethyst_playing_card", properties -> new CardItem((Item.Properties)properties, ModDecks.AMETHYST_PLAYING_CARDS, ModBlocks.AMETHYST_CARDPILE), new Item.Properties());
    public static CardboxItem AMETHYST_CARDBOX = ModItems.registerItem("amethyst_cardbox", properties -> new CardboxItem((Block)ModBlocks.AMETHYST_CARDBOX, ModBlocks.AMETHYST_CARDPILE, (Item.Properties)properties, ModDecks.AMETHYST_PLAYING_CARDS, (Item)AMETHYST_PLAYING_CARD), new Item.Properties().stacksTo(1));
    public static CardItem EMERALD_PLAYING_CARD = ModItems.registerItem("emerald_playing_card", properties -> new CardItem((Item.Properties)properties, ModDecks.EMERALD_PLAYING_CARDS, ModBlocks.EMERALD_CARDPILE), new Item.Properties());
    public static CardboxItem EMERALD_CARDBOX = ModItems.registerItem("emerald_cardbox", properties -> new CardboxItem((Block)ModBlocks.EMERALD_CARDBOX, ModBlocks.EMERALD_CARDPILE, (Item.Properties)properties, ModDecks.EMERALD_PLAYING_CARDS, (Item)EMERALD_PLAYING_CARD), new Item.Properties().stacksTo(1));
    public static CardItem RESIN_PLAYING_CARD = ModItems.registerItem("resin_playing_card", properties -> new CardItem((Item.Properties)properties, ModDecks.RESIN_PLAYING_CARDS, ModBlocks.RESIN_CARDPILE), new Item.Properties());
    public static CardboxItem RESIN_CARDBOX = ModItems.registerItem("resin_cardbox", properties -> new CardboxItem((Block)ModBlocks.RESIN_CARDBOX, ModBlocks.RESIN_CARDPILE, (Item.Properties)properties, ModDecks.RESIN_PLAYING_CARDS, (Item)RESIN_PLAYING_CARD), new Item.Properties().stacksTo(1));
    public static CardItem ECHO_PLAYING_CARD = ModItems.registerItem("echo_playing_card", properties -> new CardItem((Item.Properties)properties, ModDecks.ECHO_PLAYING_CARDS, ModBlocks.ECHO_CARDPILE), new Item.Properties());
    public static CardboxItem ECHO_CARDBOX = ModItems.registerItem("echo_cardbox", properties -> new CardboxItem((Block)ModBlocks.ECHO_CARDBOX, ModBlocks.ECHO_CARDPILE, (Item.Properties)properties, ModDecks.ECHO_PLAYING_CARDS, (Item)ECHO_PLAYING_CARD), new Item.Properties().stacksTo(1));
    public static CardItem PREMIUM_PLAYING_CARD = ModItems.registerItem("premium_playing_card", properties -> new CardItem((Item.Properties)properties, ModDecks.PREMIUM_PLAYING_CARDS, ModBlocks.PREMIUM_CARDPILE), new Item.Properties());
    public static CardboxItem PREMIUM_CARDBOX = ModItems.registerItem("premium_cardbox", properties -> new CardboxItem((Block)ModBlocks.PREMIUM_CARDBOX, ModBlocks.PREMIUM_CARDPILE, (Item.Properties)properties, ModDecks.PREMIUM_PLAYING_CARDS, (Item)PREMIUM_PLAYING_CARD), new Item.Properties().stacksTo(1));
    public static CardItem LUXURY_PLAYING_CARD = ModItems.registerItem("luxury_playing_card", properties -> new CardItem((Item.Properties)properties, ModDecks.LUXURY_PLAYING_CARDS, ModBlocks.LUXURY_CARDPILE), new Item.Properties());
    public static CardboxItem LUXURY_CARDBOX = ModItems.registerItem("luxury_cardbox", properties -> new CardboxItem((Block)ModBlocks.LUXURY_CARDBOX, ModBlocks.LUXURY_CARDPILE, (Item.Properties)properties, ModDecks.LUXURY_PLAYING_CARDS, (Item)LUXURY_PLAYING_CARD), new Item.Properties().stacksTo(1));
    public static CardItem UNO_PLAYING_CARD = ModItems.registerItem("uno_card", properties -> new CardItem((Item.Properties)properties, ModDecks.UNO, ModBlocks.UNO_CARDPILE), new Item.Properties());
    public static CardboxItem UNO_CARDBOX = ModItems.registerItem("uno_cardbox", properties -> new CardboxItem((Block)ModBlocks.UNO_CARDBOX, ModBlocks.UNO_CARDPILE, (Item.Properties)properties, ModDecks.UNO, (Item)UNO_PLAYING_CARD), new Item.Properties().stacksTo(1));
    public static CoinItem WHITE_CHIP = ModItems.registerItem("white_chip", properties -> new CoinItem(4, 1, (Item.Properties)properties), new Item.Properties());
    public static CoinItem LIGHT_GRAY_CHIP = ModItems.registerItem("light_gray_chip", properties -> new CoinItem(4, 1, (Item.Properties)properties), new Item.Properties());
    public static CoinItem GRAY_CHIP = ModItems.registerItem("gray_chip", properties -> new CoinItem(4, 1, (Item.Properties)properties), new Item.Properties());
    public static CoinItem BLACK_CHIP = ModItems.registerItem("black_chip", properties -> new CoinItem(4, 1, (Item.Properties)properties), new Item.Properties());
    public static CoinItem BROWN_CHIP = ModItems.registerItem("brown_chip", properties -> new CoinItem(4, 1, (Item.Properties)properties), new Item.Properties());
    public static CoinItem RED_CHIP = ModItems.registerItem("red_chip", properties -> new CoinItem(4, 1, (Item.Properties)properties), new Item.Properties());
    public static CoinItem ORANGE_CHIP = ModItems.registerItem("orange_chip", properties -> new CoinItem(4, 1, (Item.Properties)properties), new Item.Properties());
    public static CoinItem YELLOW_CHIP = ModItems.registerItem("yellow_chip", properties -> new CoinItem(4, 1, (Item.Properties)properties), new Item.Properties());
    public static CoinItem LIME_CHIP = ModItems.registerItem("lime_chip", properties -> new CoinItem(4, 1, (Item.Properties)properties), new Item.Properties());
    public static CoinItem GREEN_CHIP = ModItems.registerItem("green_chip", properties -> new CoinItem(4, 1, (Item.Properties)properties), new Item.Properties());
    public static CoinItem CYAN_CHIP = ModItems.registerItem("cyan_chip", properties -> new CoinItem(4, 1, (Item.Properties)properties), new Item.Properties());
    public static CoinItem LIGHT_BLUE_CHIP = ModItems.registerItem("light_blue_chip", properties -> new CoinItem(4, 1, (Item.Properties)properties), new Item.Properties());
    public static CoinItem BLUE_CHIP = ModItems.registerItem("blue_chip", properties -> new CoinItem(4, 1, (Item.Properties)properties), new Item.Properties());
    public static CoinItem PURPLE_CHIP = ModItems.registerItem("purple_chip", properties -> new CoinItem(4, 1, (Item.Properties)properties), new Item.Properties());
    public static CoinItem MAGENTA_CHIP = ModItems.registerItem("magenta_chip", properties -> new CoinItem(4, 1, (Item.Properties)properties), new Item.Properties());
    public static CoinItem PINK_CHIP = ModItems.registerItem("pink_chip", properties -> new CoinItem(4, 1, (Item.Properties)properties), new Item.Properties());
    public static CoinItem COPPER_COIN = ModItems.registerItem("copper_coin", CoinItem::new, new Item.Properties());
    public static CoinItem IRON_COIN = ModItems.registerItem("iron_coin", CoinItem::new, new Item.Properties());
    public static CoinItem GOLD_COIN = ModItems.registerItem("gold_coin", CoinItem::new, new Item.Properties());

    public static <T extends Item> T registerItem(String name, Function<Item.Properties, T> itemFactory, Item.Properties settings) {
        ResourceKey itemKey = ResourceKey.create(Registries.ITEM, (Identifier)Identifier.fromNamespaceAndPath((String)"tog_yks-cards", (String)name));
        Item item = (Item)itemFactory.apply(settings.setId(itemKey));
        Registry.register(BuiltInRegistries.ITEM, itemKey, item);
        return (T)item;
    }

    public static void registerItems() {
    }
}

