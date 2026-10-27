package net.togyk.tog_yks_cards.block;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.togyk.tog_yks_cards.Tog_yksCards;
import net.togyk.tog_yks_cards.block.custom.CardHolderBlock;
import net.togyk.tog_yks_cards.block.custom.CardboxBlock;
import net.togyk.tog_yks_cards.block.custom.CardpileBlock;
import net.togyk.tog_yks_cards.block.custom.CoinPileblock;
import net.togyk.tog_yks_cards.block.custom.MatBlock;
import net.togyk.tog_yks_cards.decks.ModDecks;
import net.togyk.tog_yks_cards.item.ModItems;
import net.togyk.tog_yks_cards.item.custom.CardHolderItem;
import net.togyk.tog_yks_cards.item.custom.CardpileItem;

public class ModBlocks {
    public static List<CardHolderData> CARD_HOLDERS = new ArrayList<CardHolderData>();
    public static CardboxBlock BLACK_CARDBOX;
    public static CardpileBlock BLACK_CARDPILE;
    public static CardboxBlock BLUE_CARDBOX;
    public static CardpileBlock BLUE_CARDPILE;
    public static CardboxBlock BROWN_CARDBOX;
    public static CardpileBlock BROWN_CARDPILE;
    public static CardboxBlock CYAN_CARDBOX;
    public static CardpileBlock CYAN_CARDPILE;
    public static CardboxBlock GRAY_CARDBOX;
    public static CardpileBlock GRAY_CARDPILE;
    public static CardboxBlock GREEN_CARDBOX;
    public static CardpileBlock GREEN_CARDPILE;
    public static CardboxBlock LIGHT_BLUE_CARDBOX;
    public static CardpileBlock LIGHT_BLUE_CARDPILE;
    public static CardboxBlock LIGHT_GRAY_CARDBOX;
    public static CardpileBlock LIGHT_GRAY_CARDPILE;
    public static CardboxBlock LIME_CARDBOX;
    public static CardpileBlock LIME_CARDPILE;
    public static CardboxBlock MAGENTA_CARDBOX;
    public static CardpileBlock MAGENTA_CARDPILE;
    public static CardboxBlock ORANGE_CARDBOX;
    public static CardpileBlock ORANGE_CARDPILE;
    public static CardboxBlock PINK_CARDBOX;
    public static CardpileBlock PINK_CARDPILE;
    public static CardboxBlock PURPLE_CARDBOX;
    public static CardpileBlock PURPLE_CARDPILE;
    public static CardboxBlock RED_CARDBOX;
    public static CardpileBlock RED_CARDPILE;
    public static CardboxBlock WHITE_CARDBOX;
    public static CardpileBlock WHITE_CARDPILE;
    public static CardboxBlock YELLOW_CARDBOX;
    public static CardpileBlock YELLOW_CARDPILE;
    public static CardboxBlock COPPER_CARDBOX;
    public static CardpileBlock COPPER_CARDPILE;
    public static CardboxBlock IRON_CARDBOX;
    public static CardpileBlock IRON_CARDPILE;
    public static CardboxBlock GOLD_CARDBOX;
    public static CardpileBlock GOLD_CARDPILE;
    public static CardboxBlock DIAMOND_CARDBOX;
    public static CardpileBlock DIAMOND_CARDPILE;
    public static CardboxBlock NETHERITE_CARDBOX;
    public static CardpileBlock NETHERITE_CARDPILE;
    public static CardboxBlock AMETHYST_CARDBOX;
    public static CardpileBlock AMETHYST_CARDPILE;
    public static CardboxBlock EMERALD_CARDBOX;
    public static CardpileBlock EMERALD_CARDPILE;
    public static CardboxBlock RESIN_CARDBOX;
    public static CardpileBlock RESIN_CARDPILE;
    public static CardboxBlock ECHO_CARDBOX;
    public static CardpileBlock ECHO_CARDPILE;
    public static CardboxBlock PREMIUM_CARDBOX;
    public static CardpileBlock PREMIUM_CARDPILE;
    public static CardboxBlock LUXURY_CARDBOX;
    public static CardpileBlock LUXURY_CARDPILE;
    public static CardboxBlock UNO_CARDBOX;
    public static CardpileBlock UNO_CARDPILE;
    public static MatBlock WHITE_GOLD_GAME_MAT;
    public static MatBlock LIGHT_GRAY_GOLD_GAME_MAT;
    public static MatBlock GRAY_GOLD_GAME_MAT;
    public static MatBlock BLACK_GOLD_GAME_MAT;
    public static MatBlock BROWN_GOLD_GAME_MAT;
    public static MatBlock RED_GOLD_GAME_MAT;
    public static MatBlock ORANGE_GOLD_GAME_MAT;
    public static MatBlock YELLOW_GOLD_GAME_MAT;
    public static MatBlock LIME_GOLD_GAME_MAT;
    public static MatBlock GREEN_GOLD_GAME_MAT;
    public static MatBlock CYAN_GOLD_GAME_MAT;
    public static MatBlock LIGHT_BLUE_GOLD_GAME_MAT;
    public static MatBlock BLUE_GOLD_GAME_MAT;
    public static MatBlock PURPLE_GOLD_GAME_MAT;
    public static MatBlock MAGENTA_GOLD_GAME_MAT;
    public static MatBlock PINK_GOLD_GAME_MAT;
    public static MatBlock WHITE_IRON_GAME_MAT;
    public static MatBlock LIGHT_GRAY_IRON_GAME_MAT;
    public static MatBlock GRAY_IRON_GAME_MAT;
    public static MatBlock BLACK_IRON_GAME_MAT;
    public static MatBlock BROWN_IRON_GAME_MAT;
    public static MatBlock RED_IRON_GAME_MAT;
    public static MatBlock ORANGE_IRON_GAME_MAT;
    public static MatBlock YELLOW_IRON_GAME_MAT;
    public static MatBlock LIME_IRON_GAME_MAT;
    public static MatBlock GREEN_IRON_GAME_MAT;
    public static MatBlock CYAN_IRON_GAME_MAT;
    public static MatBlock LIGHT_BLUE_IRON_GAME_MAT;
    public static MatBlock BLUE_IRON_GAME_MAT;
    public static MatBlock PURPLE_IRON_GAME_MAT;
    public static MatBlock MAGENTA_IRON_GAME_MAT;
    public static MatBlock PINK_IRON_GAME_MAT;
    public static MatBlock WHITE_COPPER_GAME_MAT;
    public static MatBlock LIGHT_GRAY_COPPER_GAME_MAT;
    public static MatBlock GRAY_COPPER_GAME_MAT;
    public static MatBlock BLACK_COPPER_GAME_MAT;
    public static MatBlock BROWN_COPPER_GAME_MAT;
    public static MatBlock RED_COPPER_GAME_MAT;
    public static MatBlock ORANGE_COPPER_GAME_MAT;
    public static MatBlock YELLOW_COPPER_GAME_MAT;
    public static MatBlock LIME_COPPER_GAME_MAT;
    public static MatBlock GREEN_COPPER_GAME_MAT;
    public static MatBlock CYAN_COPPER_GAME_MAT;
    public static MatBlock LIGHT_BLUE_COPPER_GAME_MAT;
    public static MatBlock BLUE_COPPER_GAME_MAT;
    public static MatBlock PURPLE_COPPER_GAME_MAT;
    public static MatBlock MAGENTA_COPPER_GAME_MAT;
    public static MatBlock PINK_COPPER_GAME_MAT;
    public static CoinPileblock COIN_PIlE;

    public static <T extends Block> T registerBlockDefaultItem(String name, Function<BlockBehaviour.Properties, T> BlockFactory, BlockBehaviour.Properties settings) {
        T block = ModBlocks.registerBlock(name, BlockFactory, settings);
        ModItems.registerItem(name, properties -> new BlockItem(block, properties), new Item.Properties());
        return block;
    }

    public static <T extends Block, Y extends Item> T registerBlockWithItem(String name, Function<BlockBehaviour.Properties, T> BlockFactory, BlockBehaviour.Properties settings, BiFunction<Item.Properties, T, Y> itemFactory, Item.Properties itemSettings) {
        T block = ModBlocks.registerBlock(name, BlockFactory, settings);
        ModItems.registerItem(name, properties -> (Item)itemFactory.apply((Item.Properties)properties, block), itemSettings);
        return block;
    }

    public static <T extends Block> T registerBlock(String name, Function<BlockBehaviour.Properties, T> blockFactory, BlockBehaviour.Properties settings) {
        ResourceKey blockKey = ResourceKey.create(Registries.BLOCK, (Identifier)Identifier.fromNamespaceAndPath((String)"tog_yks-cards", (String)name));
        Block block = (Block)blockFactory.apply(settings.setId(blockKey));
        return (T)((Block)Registry.register(BuiltInRegistries.BLOCK, blockKey, block));
    }

    public static CardHolderBlock makeCardHolder(Block plank) {
        Identifier plankId = BuiltInRegistries.BLOCK.getKey(plank);
        Object name = plankId.getPath().replaceFirst("planks$", "card_holder");
        if (((String)name).equals("air")) {
            Tog_yksCards.LOGGER.warn("{} turns out as air", plank.getName());
        }
        if (!plankId.getNamespace().equals("minecraft")) {
            name = (String)name + "_" + plankId.getNamespace();
        }
        return ModBlocks.registerBlockWithItem((String)name, CardHolderBlock::new, BlockBehaviour.Properties.of().sound(SoundType.WOOD).noCollision(), (properties, block) -> new CardHolderItem((CardHolderBlock)(block), (Item.Properties)properties), new Item.Properties());
    }

    public static void registerBlocks() {
    }

    static {
        CARD_HOLDERS.add(new CardHolderData(ModBlocks.makeCardHolder(Blocks.OAK_PLANKS), Blocks.OAK_PLANKS, Blocks.OAK_SLAB));
        CARD_HOLDERS.add(new CardHolderData(ModBlocks.makeCardHolder(Blocks.SPRUCE_PLANKS), Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_SLAB));
        CARD_HOLDERS.add(new CardHolderData(ModBlocks.makeCardHolder(Blocks.BIRCH_PLANKS), Blocks.BIRCH_PLANKS, Blocks.BIRCH_SLAB));
        CARD_HOLDERS.add(new CardHolderData(ModBlocks.makeCardHolder(Blocks.JUNGLE_PLANKS), Blocks.JUNGLE_PLANKS, Blocks.JUNGLE_SLAB));
        CARD_HOLDERS.add(new CardHolderData(ModBlocks.makeCardHolder(Blocks.ACACIA_PLANKS), Blocks.ACACIA_PLANKS, Blocks.ACACIA_SLAB));
        CARD_HOLDERS.add(new CardHolderData(ModBlocks.makeCardHolder(Blocks.DARK_OAK_PLANKS), Blocks.DARK_OAK_PLANKS, Blocks.DARK_OAK_SLAB));
        CARD_HOLDERS.add(new CardHolderData(ModBlocks.makeCardHolder(Blocks.MANGROVE_PLANKS), Blocks.MANGROVE_PLANKS, Blocks.MANGROVE_SLAB));
        CARD_HOLDERS.add(new CardHolderData(ModBlocks.makeCardHolder(Blocks.CHERRY_PLANKS), Blocks.CHERRY_PLANKS, Blocks.CHERRY_SLAB));
        CARD_HOLDERS.add(new CardHolderData(ModBlocks.makeCardHolder(Blocks.PALE_OAK_PLANKS), Blocks.PALE_OAK_PLANKS, Blocks.PALE_OAK_SLAB));
        CARD_HOLDERS.add(new CardHolderData(ModBlocks.makeCardHolder(Blocks.BAMBOO_PLANKS), Blocks.BAMBOO_PLANKS, Blocks.BAMBOO_SLAB));
        CARD_HOLDERS.add(new CardHolderData(ModBlocks.makeCardHolder(Blocks.CRIMSON_PLANKS), Blocks.CRIMSON_PLANKS, Blocks.CRIMSON_SLAB));
        CARD_HOLDERS.add(new CardHolderData(ModBlocks.makeCardHolder(Blocks.WARPED_PLANKS), Blocks.WARPED_PLANKS, Blocks.WARPED_SLAB));
        BLACK_CARDBOX = ModBlocks.registerBlock("black_cardbox", CardboxBlock::new, BlockBehaviour.Properties.of().noCollision());
        BLACK_CARDPILE = ModBlocks.registerBlockWithItem("black_cardpile", properties -> new CardpileBlock((BlockBehaviour.Properties)properties, ModDecks.BLACK_PLAYING_CARDS), BlockBehaviour.Properties.of().sound(SoundType.EMPTY).noCollision(), (properties, block) -> new CardpileItem((CardpileBlock)(block), (Item.Properties)properties), new Item.Properties());
        BLUE_CARDBOX = ModBlocks.registerBlock("blue_cardbox", CardboxBlock::new, BlockBehaviour.Properties.of().noCollision());
        BLUE_CARDPILE = ModBlocks.registerBlockWithItem("blue_cardpile", properties -> new CardpileBlock((BlockBehaviour.Properties)properties, ModDecks.BLUE_PLAYING_CARDS), BlockBehaviour.Properties.of().sound(SoundType.EMPTY).noCollision(), (properties, block) -> new CardpileItem((CardpileBlock)(block), (Item.Properties)properties), new Item.Properties());
        BROWN_CARDBOX = ModBlocks.registerBlock("brown_cardbox", CardboxBlock::new, BlockBehaviour.Properties.of().noCollision());
        BROWN_CARDPILE = ModBlocks.registerBlockWithItem("brown_cardpile", properties -> new CardpileBlock((BlockBehaviour.Properties)properties, ModDecks.BROWN_PLAYING_CARDS), BlockBehaviour.Properties.of().sound(SoundType.EMPTY).noCollision(), (properties, block) -> new CardpileItem((CardpileBlock)(block), (Item.Properties)properties), new Item.Properties());
        CYAN_CARDBOX = ModBlocks.registerBlock("cyan_cardbox", CardboxBlock::new, BlockBehaviour.Properties.of().noCollision());
        CYAN_CARDPILE = ModBlocks.registerBlockWithItem("cyan_cardpile", properties -> new CardpileBlock((BlockBehaviour.Properties)properties, ModDecks.CYAN_PLAYING_CARDS), BlockBehaviour.Properties.of().sound(SoundType.EMPTY).noCollision(), (properties, block) -> new CardpileItem((CardpileBlock)(block), (Item.Properties)properties), new Item.Properties());
        GRAY_CARDBOX = ModBlocks.registerBlock("gray_cardbox", CardboxBlock::new, BlockBehaviour.Properties.of().noCollision());
        GRAY_CARDPILE = ModBlocks.registerBlockWithItem("gray_cardpile", properties -> new CardpileBlock((BlockBehaviour.Properties)properties, ModDecks.GRAY_PLAYING_CARDS), BlockBehaviour.Properties.of().sound(SoundType.EMPTY).noCollision(), (properties, block) -> new CardpileItem((CardpileBlock)(block), (Item.Properties)properties), new Item.Properties());
        GREEN_CARDBOX = ModBlocks.registerBlock("green_cardbox", CardboxBlock::new, BlockBehaviour.Properties.of().noCollision());
        GREEN_CARDPILE = ModBlocks.registerBlockWithItem("green_cardpile", properties -> new CardpileBlock((BlockBehaviour.Properties)properties, ModDecks.GREEN_PLAYING_CARDS), BlockBehaviour.Properties.of().sound(SoundType.EMPTY).noCollision(), (properties, block) -> new CardpileItem((CardpileBlock)(block), (Item.Properties)properties), new Item.Properties());
        LIGHT_BLUE_CARDBOX = ModBlocks.registerBlock("light_blue_cardbox", CardboxBlock::new, BlockBehaviour.Properties.of().noCollision());
        LIGHT_BLUE_CARDPILE = ModBlocks.registerBlockWithItem("light_blue_cardpile", properties -> new CardpileBlock((BlockBehaviour.Properties)properties, ModDecks.LIGHT_BLUE_PLAYING_CARDS), BlockBehaviour.Properties.of().sound(SoundType.EMPTY).noCollision(), (properties, block) -> new CardpileItem((CardpileBlock)(block), (Item.Properties)properties), new Item.Properties());
        LIGHT_GRAY_CARDBOX = ModBlocks.registerBlock("light_gray_cardbox", CardboxBlock::new, BlockBehaviour.Properties.of().noCollision());
        LIGHT_GRAY_CARDPILE = ModBlocks.registerBlockWithItem("light_gray_cardpile", properties -> new CardpileBlock((BlockBehaviour.Properties)properties, ModDecks.LIGHT_GRAY_PLAYING_CARDS), BlockBehaviour.Properties.of().sound(SoundType.EMPTY).noCollision(), (properties, block) -> new CardpileItem((CardpileBlock)(block), (Item.Properties)properties), new Item.Properties());
        LIME_CARDBOX = ModBlocks.registerBlock("lime_cardbox", CardboxBlock::new, BlockBehaviour.Properties.of().noCollision());
        LIME_CARDPILE = ModBlocks.registerBlockWithItem("lime_cardpile", properties -> new CardpileBlock((BlockBehaviour.Properties)properties, ModDecks.LIME_PLAYING_CARDS), BlockBehaviour.Properties.of().sound(SoundType.EMPTY).noCollision(), (properties, block) -> new CardpileItem((CardpileBlock)(block), (Item.Properties)properties), new Item.Properties());
        MAGENTA_CARDBOX = ModBlocks.registerBlock("magenta_cardbox", CardboxBlock::new, BlockBehaviour.Properties.of().noCollision());
        MAGENTA_CARDPILE = ModBlocks.registerBlockWithItem("magenta_cardpile", properties -> new CardpileBlock((BlockBehaviour.Properties)properties, ModDecks.MAGENTA_PLAYING_CARDS), BlockBehaviour.Properties.of().sound(SoundType.EMPTY).noCollision(), (properties, block) -> new CardpileItem((CardpileBlock)(block), (Item.Properties)properties), new Item.Properties());
        ORANGE_CARDBOX = ModBlocks.registerBlock("orange_cardbox", CardboxBlock::new, BlockBehaviour.Properties.of().noCollision());
        ORANGE_CARDPILE = ModBlocks.registerBlockWithItem("orange_cardpile", properties -> new CardpileBlock((BlockBehaviour.Properties)properties, ModDecks.ORANGE_PLAYING_CARDS), BlockBehaviour.Properties.of().sound(SoundType.EMPTY).noCollision(), (properties, block) -> new CardpileItem((CardpileBlock)(block), (Item.Properties)properties), new Item.Properties());
        PINK_CARDBOX = ModBlocks.registerBlock("pink_cardbox", CardboxBlock::new, BlockBehaviour.Properties.of().noCollision());
        PINK_CARDPILE = ModBlocks.registerBlockWithItem("pink_cardpile", properties -> new CardpileBlock((BlockBehaviour.Properties)properties, ModDecks.PINK_PLAYING_CARDS), BlockBehaviour.Properties.of().sound(SoundType.EMPTY).noCollision(), (properties, block) -> new CardpileItem((CardpileBlock)(block), (Item.Properties)properties), new Item.Properties());
        PURPLE_CARDBOX = ModBlocks.registerBlock("purple_cardbox", CardboxBlock::new, BlockBehaviour.Properties.of().noCollision());
        PURPLE_CARDPILE = ModBlocks.registerBlockWithItem("purple_cardpile", properties -> new CardpileBlock((BlockBehaviour.Properties)properties, ModDecks.PURPLE_PLAYING_CARDS), BlockBehaviour.Properties.of().sound(SoundType.EMPTY).noCollision(), (properties, block) -> new CardpileItem((CardpileBlock)(block), (Item.Properties)properties), new Item.Properties());
        RED_CARDBOX = ModBlocks.registerBlock("red_cardbox", CardboxBlock::new, BlockBehaviour.Properties.of().noCollision());
        RED_CARDPILE = ModBlocks.registerBlockWithItem("red_cardpile", properties -> new CardpileBlock((BlockBehaviour.Properties)properties, ModDecks.RED_PLAYING_CARDS), BlockBehaviour.Properties.of().sound(SoundType.EMPTY).noCollision(), (properties, block) -> new CardpileItem((CardpileBlock)(block), (Item.Properties)properties), new Item.Properties());
        WHITE_CARDBOX = ModBlocks.registerBlock("white_cardbox", CardboxBlock::new, BlockBehaviour.Properties.of().noCollision());
        WHITE_CARDPILE = ModBlocks.registerBlockWithItem("white_cardpile", properties -> new CardpileBlock((BlockBehaviour.Properties)properties, ModDecks.WHITE_PLAYING_CARDS), BlockBehaviour.Properties.of().sound(SoundType.EMPTY).noCollision(), (properties, block) -> new CardpileItem((CardpileBlock)(block), (Item.Properties)properties), new Item.Properties());
        YELLOW_CARDBOX = ModBlocks.registerBlock("yellow_cardbox", CardboxBlock::new, BlockBehaviour.Properties.of().noCollision());
        YELLOW_CARDPILE = ModBlocks.registerBlockWithItem("yellow_cardpile", properties -> new CardpileBlock((BlockBehaviour.Properties)properties, ModDecks.YELLOW_PLAYING_CARDS), BlockBehaviour.Properties.of().sound(SoundType.EMPTY).noCollision(), (properties, block) -> new CardpileItem((CardpileBlock)(block), (Item.Properties)properties), new Item.Properties());
        COPPER_CARDBOX = ModBlocks.registerBlock("copper_cardbox", CardboxBlock::new, BlockBehaviour.Properties.of().noCollision());
        COPPER_CARDPILE = ModBlocks.registerBlockWithItem("copper_cardpile", properties -> new CardpileBlock((BlockBehaviour.Properties)properties, ModDecks.COPPER_PLAYING_CARDS), BlockBehaviour.Properties.of().sound(SoundType.EMPTY).noCollision(), (properties, block) -> new CardpileItem((CardpileBlock)(block), (Item.Properties)properties), new Item.Properties());
        IRON_CARDBOX = ModBlocks.registerBlock("iron_cardbox", CardboxBlock::new, BlockBehaviour.Properties.of().noCollision());
        IRON_CARDPILE = ModBlocks.registerBlockWithItem("iron_cardpile", properties -> new CardpileBlock((BlockBehaviour.Properties)properties, ModDecks.IRON_PLAYING_CARDS), BlockBehaviour.Properties.of().sound(SoundType.EMPTY).noCollision(), (properties, block) -> new CardpileItem((CardpileBlock)(block), (Item.Properties)properties), new Item.Properties());
        GOLD_CARDBOX = ModBlocks.registerBlock("gold_cardbox", CardboxBlock::new, BlockBehaviour.Properties.of().noCollision());
        GOLD_CARDPILE = ModBlocks.registerBlockWithItem("gold_cardpile", properties -> new CardpileBlock((BlockBehaviour.Properties)properties, ModDecks.GOLD_PLAYING_CARDS), BlockBehaviour.Properties.of().sound(SoundType.EMPTY).noCollision(), (properties, block) -> new CardpileItem((CardpileBlock)(block), (Item.Properties)properties), new Item.Properties());
        DIAMOND_CARDBOX = ModBlocks.registerBlock("diamond_cardbox", CardboxBlock::new, BlockBehaviour.Properties.of().noCollision());
        DIAMOND_CARDPILE = ModBlocks.registerBlockWithItem("diamond_cardpile", properties -> new CardpileBlock((BlockBehaviour.Properties)properties, ModDecks.DIAMOND_PLAYING_CARDS), BlockBehaviour.Properties.of().sound(SoundType.EMPTY).noCollision(), (properties, block) -> new CardpileItem((CardpileBlock)(block), (Item.Properties)properties), new Item.Properties());
        NETHERITE_CARDBOX = ModBlocks.registerBlock("netherite_cardbox", CardboxBlock::new, BlockBehaviour.Properties.of().noCollision());
        NETHERITE_CARDPILE = ModBlocks.registerBlockWithItem("netherite_cardpile", properties -> new CardpileBlock((BlockBehaviour.Properties)properties, ModDecks.NETHERITE_PLAYING_CARDS), BlockBehaviour.Properties.of().sound(SoundType.EMPTY).noCollision(), (properties, block) -> new CardpileItem((CardpileBlock)(block), (Item.Properties)properties), new Item.Properties());
        AMETHYST_CARDBOX = ModBlocks.registerBlock("amethyst_cardbox", CardboxBlock::new, BlockBehaviour.Properties.of().noCollision());
        AMETHYST_CARDPILE = ModBlocks.registerBlockWithItem("amethyst_cardpile", properties -> new CardpileBlock((BlockBehaviour.Properties)properties, ModDecks.AMETHYST_PLAYING_CARDS), BlockBehaviour.Properties.of().sound(SoundType.EMPTY).noCollision(), (properties, block) -> new CardpileItem((CardpileBlock)(block), (Item.Properties)properties), new Item.Properties());
        EMERALD_CARDBOX = ModBlocks.registerBlock("emerald_cardbox", CardboxBlock::new, BlockBehaviour.Properties.of().noCollision());
        EMERALD_CARDPILE = ModBlocks.registerBlockWithItem("emerald_cardpile", properties -> new CardpileBlock((BlockBehaviour.Properties)properties, ModDecks.EMERALD_PLAYING_CARDS), BlockBehaviour.Properties.of().sound(SoundType.EMPTY).noCollision(), (properties, block) -> new CardpileItem((CardpileBlock)(block), (Item.Properties)properties), new Item.Properties());
        RESIN_CARDBOX = ModBlocks.registerBlock("resin_cardbox", CardboxBlock::new, BlockBehaviour.Properties.of().noCollision());
        RESIN_CARDPILE = ModBlocks.registerBlockWithItem("resin_cardpile", properties -> new CardpileBlock((BlockBehaviour.Properties)properties, ModDecks.RESIN_PLAYING_CARDS), BlockBehaviour.Properties.of().sound(SoundType.EMPTY).noCollision(), (properties, block) -> new CardpileItem((CardpileBlock)(block), (Item.Properties)properties), new Item.Properties());
        ECHO_CARDBOX = ModBlocks.registerBlock("echo_cardbox", CardboxBlock::new, BlockBehaviour.Properties.of().noCollision());
        ECHO_CARDPILE = ModBlocks.registerBlockWithItem("echo_cardpile", properties -> new CardpileBlock((BlockBehaviour.Properties)properties, ModDecks.ECHO_PLAYING_CARDS), BlockBehaviour.Properties.of().sound(SoundType.EMPTY).noCollision(), (properties, block) -> new CardpileItem((CardpileBlock)(block), (Item.Properties)properties), new Item.Properties());
        PREMIUM_CARDBOX = ModBlocks.registerBlock("premium_cardbox", CardboxBlock::new, BlockBehaviour.Properties.of().noCollision());
        PREMIUM_CARDPILE = ModBlocks.registerBlockWithItem("premium_cardpile", properties -> new CardpileBlock((BlockBehaviour.Properties)properties, ModDecks.PREMIUM_PLAYING_CARDS), BlockBehaviour.Properties.of().sound(SoundType.EMPTY).noCollision(), (properties, block) -> new CardpileItem((CardpileBlock)(block), (Item.Properties)properties), new Item.Properties());
        LUXURY_CARDBOX = ModBlocks.registerBlock("luxury_cardbox", CardboxBlock::new, BlockBehaviour.Properties.of().noCollision());
        LUXURY_CARDPILE = ModBlocks.registerBlockWithItem("luxury_cardpile", properties -> new CardpileBlock((BlockBehaviour.Properties)properties, ModDecks.LUXURY_PLAYING_CARDS), BlockBehaviour.Properties.of().sound(SoundType.EMPTY).noCollision(), (properties, block) -> new CardpileItem((CardpileBlock)(block), (Item.Properties)properties), new Item.Properties());
        UNO_CARDBOX = ModBlocks.registerBlock("uno_cardbox", CardboxBlock::new, BlockBehaviour.Properties.of().noCollision());
        UNO_CARDPILE = ModBlocks.registerBlockWithItem("uno_cardpile", properties -> new CardpileBlock((BlockBehaviour.Properties)properties, ModDecks.UNO), BlockBehaviour.Properties.of().sound(SoundType.EMPTY).noCollision(), (properties, block) -> new CardpileItem((CardpileBlock)(block), (Item.Properties)properties), new Item.Properties());
        WHITE_GOLD_GAME_MAT = ModBlocks.registerBlockDefaultItem("white_gold_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "gold"), BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        LIGHT_GRAY_GOLD_GAME_MAT = ModBlocks.registerBlockDefaultItem("light_gray_gold_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "gold"), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        GRAY_GOLD_GAME_MAT = ModBlocks.registerBlockDefaultItem("gray_gold_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "gold"), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        BLACK_GOLD_GAME_MAT = ModBlocks.registerBlockDefaultItem("black_gold_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "gold"), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        BROWN_GOLD_GAME_MAT = ModBlocks.registerBlockDefaultItem("brown_gold_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "gold"), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        RED_GOLD_GAME_MAT = ModBlocks.registerBlockDefaultItem("red_gold_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "gold"), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        ORANGE_GOLD_GAME_MAT = ModBlocks.registerBlockDefaultItem("orange_gold_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "gold"), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        YELLOW_GOLD_GAME_MAT = ModBlocks.registerBlockDefaultItem("yellow_gold_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "gold"), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        LIME_GOLD_GAME_MAT = ModBlocks.registerBlockDefaultItem("lime_gold_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "gold"), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        GREEN_GOLD_GAME_MAT = ModBlocks.registerBlockDefaultItem("green_gold_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "gold"), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        CYAN_GOLD_GAME_MAT = ModBlocks.registerBlockDefaultItem("cyan_gold_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "gold"), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        LIGHT_BLUE_GOLD_GAME_MAT = ModBlocks.registerBlockDefaultItem("light_blue_gold_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "gold"), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        BLUE_GOLD_GAME_MAT = ModBlocks.registerBlockDefaultItem("blue_gold_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "gold"), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        PURPLE_GOLD_GAME_MAT = ModBlocks.registerBlockDefaultItem("purple_gold_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "gold"), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        MAGENTA_GOLD_GAME_MAT = ModBlocks.registerBlockDefaultItem("magenta_gold_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "gold"), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_MAGENTA).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        PINK_GOLD_GAME_MAT = ModBlocks.registerBlockDefaultItem("pink_gold_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "gold"), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        WHITE_IRON_GAME_MAT = ModBlocks.registerBlockDefaultItem("white_iron_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "iron"), BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        LIGHT_GRAY_IRON_GAME_MAT = ModBlocks.registerBlockDefaultItem("light_gray_iron_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "iron"), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        GRAY_IRON_GAME_MAT = ModBlocks.registerBlockDefaultItem("gray_iron_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "iron"), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        BLACK_IRON_GAME_MAT = ModBlocks.registerBlockDefaultItem("black_iron_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "iron"), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        BROWN_IRON_GAME_MAT = ModBlocks.registerBlockDefaultItem("brown_iron_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "iron"), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        RED_IRON_GAME_MAT = ModBlocks.registerBlockDefaultItem("red_iron_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "iron"), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        ORANGE_IRON_GAME_MAT = ModBlocks.registerBlockDefaultItem("orange_iron_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "iron"), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        YELLOW_IRON_GAME_MAT = ModBlocks.registerBlockDefaultItem("yellow_iron_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "iron"), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        LIME_IRON_GAME_MAT = ModBlocks.registerBlockDefaultItem("lime_iron_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "iron"), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        GREEN_IRON_GAME_MAT = ModBlocks.registerBlockDefaultItem("green_iron_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "iron"), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        CYAN_IRON_GAME_MAT = ModBlocks.registerBlockDefaultItem("cyan_iron_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "iron"), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        LIGHT_BLUE_IRON_GAME_MAT = ModBlocks.registerBlockDefaultItem("light_blue_iron_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "iron"), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        BLUE_IRON_GAME_MAT = ModBlocks.registerBlockDefaultItem("blue_iron_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "iron"), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        PURPLE_IRON_GAME_MAT = ModBlocks.registerBlockDefaultItem("purple_iron_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "iron"), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        MAGENTA_IRON_GAME_MAT = ModBlocks.registerBlockDefaultItem("magenta_iron_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "iron"), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_MAGENTA).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        PINK_IRON_GAME_MAT = ModBlocks.registerBlockDefaultItem("pink_iron_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "iron"), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        WHITE_COPPER_GAME_MAT = ModBlocks.registerBlockDefaultItem("white_copper_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "copper"), BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        LIGHT_GRAY_COPPER_GAME_MAT = ModBlocks.registerBlockDefaultItem("light_gray_copper_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "copper"), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        GRAY_COPPER_GAME_MAT = ModBlocks.registerBlockDefaultItem("gray_copper_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "copper"), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        BLACK_COPPER_GAME_MAT = ModBlocks.registerBlockDefaultItem("black_copper_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "copper"), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        BROWN_COPPER_GAME_MAT = ModBlocks.registerBlockDefaultItem("brown_copper_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "copper"), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        RED_COPPER_GAME_MAT = ModBlocks.registerBlockDefaultItem("red_copper_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "copper"), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        ORANGE_COPPER_GAME_MAT = ModBlocks.registerBlockDefaultItem("orange_copper_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "copper"), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        YELLOW_COPPER_GAME_MAT = ModBlocks.registerBlockDefaultItem("yellow_copper_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "copper"), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        LIME_COPPER_GAME_MAT = ModBlocks.registerBlockDefaultItem("lime_copper_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "copper"), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        GREEN_COPPER_GAME_MAT = ModBlocks.registerBlockDefaultItem("green_copper_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "copper"), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        CYAN_COPPER_GAME_MAT = ModBlocks.registerBlockDefaultItem("cyan_copper_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "copper"), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        LIGHT_BLUE_COPPER_GAME_MAT = ModBlocks.registerBlockDefaultItem("light_blue_copper_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "copper"), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        BLUE_COPPER_GAME_MAT = ModBlocks.registerBlockDefaultItem("blue_copper_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "copper"), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        PURPLE_COPPER_GAME_MAT = ModBlocks.registerBlockDefaultItem("purple_copper_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "copper"), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        MAGENTA_COPPER_GAME_MAT = ModBlocks.registerBlockDefaultItem("magenta_copper_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "copper"), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_MAGENTA).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        PINK_COPPER_GAME_MAT = ModBlocks.registerBlockDefaultItem("pink_copper_game_mat", properties -> new MatBlock((BlockBehaviour.Properties)properties, "copper"), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        COIN_PIlE = ModBlocks.registerBlock("coin_pile", CoinPileblock::new, BlockBehaviour.Properties.of().strength(0.1f).sound(SoundType.EMPTY));
    }

    public static class CardHolderData {
        private final CardHolderBlock cardHolder;
        private final Identifier texture;
        private final Block recipeItem;

        public CardHolderData(CardHolderBlock cardHolder, Identifier texture, Block recipeItem) {
            this.cardHolder = cardHolder;
            this.texture = texture;
            this.recipeItem = recipeItem;
        }

        public CardHolderData(CardHolderBlock cardHolder, Block textureBlock, Block recipeItem) {
            this(cardHolder, BuiltInRegistries.BLOCK.getKey(textureBlock).withPrefix("block/"), recipeItem);
        }

        public CardHolderBlock cardHolder() {
            return this.cardHolder;
        }

        public Identifier getTexture() {
            return this.texture;
        }

        public Block getRecipeItem() {
            return this.recipeItem;
        }
    }
}

