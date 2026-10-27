package net.togyk.tog_yks_cards.item;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.ItemLike;
import net.togyk.tog_yks_cards.block.ModBlocks;
import net.togyk.tog_yks_cards.item.ModItems;

public class ModItemTabs {
    public static final CreativeModeTab EXAMPLE_TAB = (CreativeModeTab)Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, (Identifier)Identifier.fromNamespaceAndPath((String)"tog_yks-cards", (String)"tog_yks-cards"), CreativeModeTab.builder((CreativeModeTab.Row)CreativeModeTab.Row.TOP, (int)0).icon(() -> ModBlocks.RED_CARDBOX.asItem().getDefaultInstance()).title((Component)Component.translatable((String)"creativeTab.tog_yks-cards")).displayItems((params, output) -> {
        output.accept((ItemLike)ModBlocks.WHITE_CARDBOX);
        output.accept((ItemLike)ModBlocks.LIGHT_GRAY_CARDBOX);
        output.accept((ItemLike)ModBlocks.GRAY_CARDBOX);
        output.accept((ItemLike)ModBlocks.BLACK_CARDBOX);
        output.accept((ItemLike)ModBlocks.BROWN_CARDBOX);
        output.accept((ItemLike)ModBlocks.RED_CARDBOX);
        output.accept((ItemLike)ModBlocks.ORANGE_CARDBOX);
        output.accept((ItemLike)ModBlocks.YELLOW_CARDBOX);
        output.accept((ItemLike)ModBlocks.LIME_CARDBOX);
        output.accept((ItemLike)ModBlocks.GREEN_CARDBOX);
        output.accept((ItemLike)ModBlocks.CYAN_CARDBOX);
        output.accept((ItemLike)ModBlocks.LIGHT_BLUE_CARDBOX);
        output.accept((ItemLike)ModBlocks.BLUE_CARDBOX);
        output.accept((ItemLike)ModBlocks.PURPLE_CARDBOX);
        output.accept((ItemLike)ModBlocks.MAGENTA_CARDBOX);
        output.accept((ItemLike)ModBlocks.PINK_CARDBOX);
        output.accept((ItemLike)ModBlocks.COPPER_CARDBOX);
        output.accept((ItemLike)ModBlocks.IRON_CARDBOX);
        output.accept((ItemLike)ModBlocks.GOLD_CARDBOX);
        output.accept((ItemLike)ModBlocks.DIAMOND_CARDBOX);
        output.accept((ItemLike)ModBlocks.NETHERITE_CARDBOX);
        output.accept((ItemLike)ModBlocks.AMETHYST_CARDBOX);
        output.accept((ItemLike)ModBlocks.EMERALD_CARDBOX);
        output.accept((ItemLike)ModBlocks.RESIN_CARDBOX);
        output.accept((ItemLike)ModBlocks.ECHO_CARDBOX);
        output.accept((ItemLike)ModBlocks.PREMIUM_CARDBOX);
        output.accept((ItemLike)ModBlocks.LUXURY_CARDBOX);
        output.accept((ItemLike)ModBlocks.UNO_CARDBOX);
        for (ModBlocks.CardHolderData data : ModBlocks.CARD_HOLDERS) {
            output.accept((ItemLike)data.cardHolder());
        }
        output.accept((ItemLike)ModBlocks.WHITE_GOLD_GAME_MAT);
        output.accept((ItemLike)ModBlocks.LIGHT_GRAY_GOLD_GAME_MAT);
        output.accept((ItemLike)ModBlocks.GRAY_GOLD_GAME_MAT);
        output.accept((ItemLike)ModBlocks.BLACK_GOLD_GAME_MAT);
        output.accept((ItemLike)ModBlocks.BROWN_GOLD_GAME_MAT);
        output.accept((ItemLike)ModBlocks.RED_GOLD_GAME_MAT);
        output.accept((ItemLike)ModBlocks.ORANGE_GOLD_GAME_MAT);
        output.accept((ItemLike)ModBlocks.YELLOW_GOLD_GAME_MAT);
        output.accept((ItemLike)ModBlocks.LIME_GOLD_GAME_MAT);
        output.accept((ItemLike)ModBlocks.GREEN_GOLD_GAME_MAT);
        output.accept((ItemLike)ModBlocks.CYAN_GOLD_GAME_MAT);
        output.accept((ItemLike)ModBlocks.LIGHT_BLUE_GOLD_GAME_MAT);
        output.accept((ItemLike)ModBlocks.BLUE_GOLD_GAME_MAT);
        output.accept((ItemLike)ModBlocks.PURPLE_GOLD_GAME_MAT);
        output.accept((ItemLike)ModBlocks.MAGENTA_GOLD_GAME_MAT);
        output.accept((ItemLike)ModBlocks.PINK_GOLD_GAME_MAT);
        output.accept((ItemLike)ModBlocks.WHITE_IRON_GAME_MAT);
        output.accept((ItemLike)ModBlocks.LIGHT_GRAY_IRON_GAME_MAT);
        output.accept((ItemLike)ModBlocks.GRAY_IRON_GAME_MAT);
        output.accept((ItemLike)ModBlocks.BLACK_IRON_GAME_MAT);
        output.accept((ItemLike)ModBlocks.BROWN_IRON_GAME_MAT);
        output.accept((ItemLike)ModBlocks.RED_IRON_GAME_MAT);
        output.accept((ItemLike)ModBlocks.ORANGE_IRON_GAME_MAT);
        output.accept((ItemLike)ModBlocks.YELLOW_IRON_GAME_MAT);
        output.accept((ItemLike)ModBlocks.LIME_IRON_GAME_MAT);
        output.accept((ItemLike)ModBlocks.GREEN_IRON_GAME_MAT);
        output.accept((ItemLike)ModBlocks.CYAN_IRON_GAME_MAT);
        output.accept((ItemLike)ModBlocks.LIGHT_BLUE_IRON_GAME_MAT);
        output.accept((ItemLike)ModBlocks.BLUE_IRON_GAME_MAT);
        output.accept((ItemLike)ModBlocks.PURPLE_IRON_GAME_MAT);
        output.accept((ItemLike)ModBlocks.MAGENTA_IRON_GAME_MAT);
        output.accept((ItemLike)ModBlocks.PINK_IRON_GAME_MAT);
        output.accept((ItemLike)ModBlocks.WHITE_COPPER_GAME_MAT);
        output.accept((ItemLike)ModBlocks.LIGHT_GRAY_COPPER_GAME_MAT);
        output.accept((ItemLike)ModBlocks.GRAY_COPPER_GAME_MAT);
        output.accept((ItemLike)ModBlocks.BLACK_COPPER_GAME_MAT);
        output.accept((ItemLike)ModBlocks.BROWN_COPPER_GAME_MAT);
        output.accept((ItemLike)ModBlocks.RED_COPPER_GAME_MAT);
        output.accept((ItemLike)ModBlocks.ORANGE_COPPER_GAME_MAT);
        output.accept((ItemLike)ModBlocks.YELLOW_COPPER_GAME_MAT);
        output.accept((ItemLike)ModBlocks.LIME_COPPER_GAME_MAT);
        output.accept((ItemLike)ModBlocks.GREEN_COPPER_GAME_MAT);
        output.accept((ItemLike)ModBlocks.CYAN_COPPER_GAME_MAT);
        output.accept((ItemLike)ModBlocks.LIGHT_BLUE_COPPER_GAME_MAT);
        output.accept((ItemLike)ModBlocks.BLUE_COPPER_GAME_MAT);
        output.accept((ItemLike)ModBlocks.PURPLE_COPPER_GAME_MAT);
        output.accept((ItemLike)ModBlocks.MAGENTA_COPPER_GAME_MAT);
        output.accept((ItemLike)ModBlocks.PINK_COPPER_GAME_MAT);
        output.accept((ItemLike)ModItems.WHITE_CHIP);
        output.accept((ItemLike)ModItems.LIGHT_GRAY_CHIP);
        output.accept((ItemLike)ModItems.GRAY_CHIP);
        output.accept((ItemLike)ModItems.BLACK_CHIP);
        output.accept((ItemLike)ModItems.BROWN_CHIP);
        output.accept((ItemLike)ModItems.RED_CHIP);
        output.accept((ItemLike)ModItems.ORANGE_CHIP);
        output.accept((ItemLike)ModItems.YELLOW_CHIP);
        output.accept((ItemLike)ModItems.LIME_CHIP);
        output.accept((ItemLike)ModItems.GREEN_CHIP);
        output.accept((ItemLike)ModItems.CYAN_CHIP);
        output.accept((ItemLike)ModItems.LIGHT_BLUE_CHIP);
        output.accept((ItemLike)ModItems.BLUE_CHIP);
        output.accept((ItemLike)ModItems.PURPLE_CHIP);
        output.accept((ItemLike)ModItems.MAGENTA_CHIP);
        output.accept((ItemLike)ModItems.PINK_CHIP);
        output.accept((ItemLike)ModItems.COPPER_COIN);
        output.accept((ItemLike)ModItems.IRON_COIN);
        output.accept((ItemLike)ModItems.GOLD_COIN);
    }).build());

    public static void registerTabs() {
    }
}

