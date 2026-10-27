package net.togyk.tog_yks_cards;

import net.fabricmc.api.ModInitializer;
import net.togyk.tog_yks_cards.block.ModBlocks;
import net.togyk.tog_yks_cards.blockentity.ModBlockEntities;
import net.togyk.tog_yks_cards.client.menu.ModMenuTypes;
import net.togyk.tog_yks_cards.compatibility.ModCompatibilities;
import net.togyk.tog_yks_cards.component.ModComponents;
import net.togyk.tog_yks_cards.item.ModItemTabs;
import net.togyk.tog_yks_cards.item.ModItems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tog_yksCards
implements ModInitializer {
    public static final String MOD_ID = "tog_yks-cards";
    public static final Logger LOGGER = LoggerFactory.getLogger((String)"tog_yks-cards");

    public void onInitialize() {
        LOGGER.info("initializing tog_yks-cards");
        ModItems.registerItems();
        ModBlocks.registerBlocks();
        ModComponents.registerModComponents();
        ModCompatibilities.register();
        ModBlockEntities.registerBlockEntities();
        ModItemTabs.registerTabs();
        ModMenuTypes.registerMenuTypes();
        LOGGER.info("initialized tog_yks-cards");
    }
}

