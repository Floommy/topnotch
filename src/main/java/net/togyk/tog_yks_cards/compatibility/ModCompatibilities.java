package net.togyk.tog_yks_cards.compatibility;

import net.fabricmc.loader.api.FabricLoader;
import net.togyk.tog_yks_cards.Tog_yksCards;
import net.togyk.tog_yks_cards.block.ModBlocks;
import net.togyk.tog_yks_cards.compatibility.BiomesOPlenty;
import net.togyk.tog_yks_cards.compatibility.Promenade;

public class ModCompatibilities {
    public static void register() {
        if (FabricLoader.getInstance().isModLoaded("biomesoplenty")) {
            Tog_yksCards.LOGGER.info("Biomes O' Plenty found, adding cardholders.");
            BiomesOPlenty.addCardHolders(ModBlocks.CARD_HOLDERS);
        }
        if (FabricLoader.getInstance().isModLoaded("promenade")) {
            Tog_yksCards.LOGGER.info("Promenade found, adding cardholders.");
            Promenade.addCardHolders(ModBlocks.CARD_HOLDERS);
        }
    }
}

