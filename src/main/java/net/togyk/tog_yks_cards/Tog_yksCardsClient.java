package net.togyk.tog_yks_cards;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.MenuScreens;
import net.togyk.tog_yks_cards.client.menu.ModMenuTypes;
import net.togyk.tog_yks_cards.client.menu.screen.CardSelectScreen;
import net.togyk.tog_yks_cards.client.render.ModRenderers;
import net.togyk.tog_yks_cards.decks.DeckPropertyLayers;
import net.togyk.tog_yks_cards.decks.ModDecks;
import net.togyk.tog_yks_cards.property.ModItemModelProperties;

@Environment(value=EnvType.CLIENT)
public class Tog_yksCardsClient
implements ClientModInitializer {
    public void onInitializeClient() {
        ModItemModelProperties.registerModProperties();
        DeckPropertyLayers.registerDeckLayers(ModDecks.BLACK_PLAYING_CARDS, ModDecks.BLUE_PLAYING_CARDS, ModDecks.BROWN_PLAYING_CARDS, ModDecks.CYAN_PLAYING_CARDS, ModDecks.GRAY_PLAYING_CARDS, ModDecks.GREEN_PLAYING_CARDS, ModDecks.LIGHT_BLUE_PLAYING_CARDS, ModDecks.LIGHT_GRAY_PLAYING_CARDS, ModDecks.LIME_PLAYING_CARDS, ModDecks.MAGENTA_PLAYING_CARDS, ModDecks.ORANGE_PLAYING_CARDS, ModDecks.PINK_PLAYING_CARDS, ModDecks.PURPLE_PLAYING_CARDS, ModDecks.RED_PLAYING_CARDS, ModDecks.WHITE_PLAYING_CARDS, ModDecks.YELLOW_PLAYING_CARDS, ModDecks.COPPER_PLAYING_CARDS, ModDecks.IRON_PLAYING_CARDS, ModDecks.GOLD_PLAYING_CARDS, ModDecks.DIAMOND_PLAYING_CARDS, ModDecks.NETHERITE_PLAYING_CARDS, ModDecks.AMETHYST_PLAYING_CARDS, ModDecks.EMERALD_PLAYING_CARDS, ModDecks.RESIN_PLAYING_CARDS, ModDecks.ECHO_PLAYING_CARDS, ModDecks.PREMIUM_PLAYING_CARDS, ModDecks.LUXURY_PLAYING_CARDS, ModDecks.UNO);
        ModRenderers.registerRenderers();
        MenuScreens.register(ModMenuTypes.CARD_SELECT, CardSelectScreen::new);
    }
}

