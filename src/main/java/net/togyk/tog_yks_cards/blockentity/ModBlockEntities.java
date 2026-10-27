package net.togyk.tog_yks_cards.blockentity;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.togyk.tog_yks_cards.block.ModBlocks;
import net.togyk.tog_yks_cards.blockentity.entities.CardHolderBlockEntity;
import net.togyk.tog_yks_cards.blockentity.entities.CardPileBlockEntity;
import net.togyk.tog_yks_cards.blockentity.entities.CardboxBlockEntity;
import net.togyk.tog_yks_cards.blockentity.entities.CoinPileBlockEntity;
import net.togyk.tog_yks_cards.blockentity.entities.MatBlockEntity;

public class ModBlockEntities {
    public static final BlockEntityType<CardHolderBlockEntity> CARD_HOLDER = ModBlockEntities.register("card_holder", CardHolderBlockEntity::new, (Block[])ModBlocks.CARD_HOLDERS.stream().map(ModBlocks.CardHolderData::cardHolder).toArray(Block[]::new));
    public static final BlockEntityType<CardPileBlockEntity> CARD_PILE = ModBlockEntities.register("card_pile", CardPileBlockEntity::new, new Block[]{ModBlocks.BLACK_CARDPILE, ModBlocks.BLUE_CARDPILE, ModBlocks.BROWN_CARDPILE, ModBlocks.CYAN_CARDPILE, ModBlocks.GRAY_CARDPILE, ModBlocks.GREEN_CARDPILE, ModBlocks.LIGHT_BLUE_CARDPILE, ModBlocks.LIGHT_GRAY_CARDPILE, ModBlocks.LIME_CARDPILE, ModBlocks.MAGENTA_CARDPILE, ModBlocks.ORANGE_CARDPILE, ModBlocks.PINK_CARDPILE, ModBlocks.PURPLE_CARDPILE, ModBlocks.RED_CARDPILE, ModBlocks.WHITE_CARDPILE, ModBlocks.YELLOW_CARDPILE, ModBlocks.COPPER_CARDPILE, ModBlocks.IRON_CARDPILE, ModBlocks.GOLD_CARDPILE, ModBlocks.DIAMOND_CARDPILE, ModBlocks.NETHERITE_CARDPILE, ModBlocks.AMETHYST_CARDPILE, ModBlocks.EMERALD_CARDPILE, ModBlocks.RESIN_CARDPILE, ModBlocks.ECHO_CARDPILE, ModBlocks.PREMIUM_CARDPILE, ModBlocks.LUXURY_CARDPILE, ModBlocks.UNO_CARDPILE});
    public static final BlockEntityType<CardboxBlockEntity> CARD_BOX = ModBlockEntities.register("card_box", CardboxBlockEntity::new, new Block[]{ModBlocks.BLACK_CARDBOX, ModBlocks.BLUE_CARDBOX, ModBlocks.BROWN_CARDBOX, ModBlocks.CYAN_CARDBOX, ModBlocks.GRAY_CARDBOX, ModBlocks.GREEN_CARDBOX, ModBlocks.LIGHT_BLUE_CARDBOX, ModBlocks.LIGHT_GRAY_CARDBOX, ModBlocks.LIME_CARDBOX, ModBlocks.MAGENTA_CARDBOX, ModBlocks.ORANGE_CARDBOX, ModBlocks.PINK_CARDBOX, ModBlocks.PURPLE_CARDBOX, ModBlocks.RED_CARDBOX, ModBlocks.WHITE_CARDBOX, ModBlocks.YELLOW_CARDBOX, ModBlocks.COPPER_CARDBOX, ModBlocks.IRON_CARDBOX, ModBlocks.GOLD_CARDBOX, ModBlocks.DIAMOND_CARDBOX, ModBlocks.NETHERITE_CARDBOX, ModBlocks.AMETHYST_CARDBOX, ModBlocks.EMERALD_CARDBOX, ModBlocks.RESIN_CARDBOX, ModBlocks.ECHO_CARDBOX, ModBlocks.PREMIUM_CARDBOX, ModBlocks.LUXURY_CARDBOX, ModBlocks.UNO_CARDBOX});
    public static final BlockEntityType<MatBlockEntity> MAT = ModBlockEntities.register("mat", MatBlockEntity::new, new Block[]{ModBlocks.WHITE_GOLD_GAME_MAT, ModBlocks.LIGHT_GRAY_GOLD_GAME_MAT, ModBlocks.GRAY_GOLD_GAME_MAT, ModBlocks.BLACK_GOLD_GAME_MAT, ModBlocks.BROWN_GOLD_GAME_MAT, ModBlocks.RED_GOLD_GAME_MAT, ModBlocks.ORANGE_GOLD_GAME_MAT, ModBlocks.YELLOW_GOLD_GAME_MAT, ModBlocks.LIME_GOLD_GAME_MAT, ModBlocks.GREEN_GOLD_GAME_MAT, ModBlocks.CYAN_GOLD_GAME_MAT, ModBlocks.LIGHT_BLUE_GOLD_GAME_MAT, ModBlocks.BLUE_GOLD_GAME_MAT, ModBlocks.PURPLE_GOLD_GAME_MAT, ModBlocks.MAGENTA_GOLD_GAME_MAT, ModBlocks.PINK_GOLD_GAME_MAT, ModBlocks.WHITE_IRON_GAME_MAT, ModBlocks.LIGHT_GRAY_IRON_GAME_MAT, ModBlocks.GRAY_IRON_GAME_MAT, ModBlocks.BLACK_IRON_GAME_MAT, ModBlocks.BROWN_IRON_GAME_MAT, ModBlocks.RED_IRON_GAME_MAT, ModBlocks.ORANGE_IRON_GAME_MAT, ModBlocks.YELLOW_IRON_GAME_MAT, ModBlocks.LIME_IRON_GAME_MAT, ModBlocks.GREEN_IRON_GAME_MAT, ModBlocks.CYAN_IRON_GAME_MAT, ModBlocks.LIGHT_BLUE_IRON_GAME_MAT, ModBlocks.BLUE_IRON_GAME_MAT, ModBlocks.PURPLE_IRON_GAME_MAT, ModBlocks.MAGENTA_IRON_GAME_MAT, ModBlocks.PINK_IRON_GAME_MAT, ModBlocks.WHITE_COPPER_GAME_MAT, ModBlocks.LIGHT_GRAY_COPPER_GAME_MAT, ModBlocks.GRAY_COPPER_GAME_MAT, ModBlocks.BLACK_COPPER_GAME_MAT, ModBlocks.BROWN_COPPER_GAME_MAT, ModBlocks.RED_COPPER_GAME_MAT, ModBlocks.ORANGE_COPPER_GAME_MAT, ModBlocks.YELLOW_COPPER_GAME_MAT, ModBlocks.LIME_COPPER_GAME_MAT, ModBlocks.GREEN_COPPER_GAME_MAT, ModBlocks.CYAN_COPPER_GAME_MAT, ModBlocks.LIGHT_BLUE_COPPER_GAME_MAT, ModBlocks.BLUE_COPPER_GAME_MAT, ModBlocks.PURPLE_COPPER_GAME_MAT, ModBlocks.MAGENTA_COPPER_GAME_MAT, ModBlocks.PINK_COPPER_GAME_MAT});
    public static final BlockEntityType<CoinPileBlockEntity> COIN_PILE = ModBlockEntities.register("coin_pile", CoinPileBlockEntity::new, ModBlocks.COIN_PIlE);

    private static <T extends BlockEntity> BlockEntityType<T> register(String name, FabricBlockEntityTypeBuilder.Factory<? extends T> entityFactory, Block ... blocks) {
        Identifier id = Identifier.fromNamespaceAndPath((String)"tog_yks-cards", (String)name);
        return (BlockEntityType)Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, (Identifier)id, FabricBlockEntityTypeBuilder.create(entityFactory, (Block[])blocks).build());
    }

    public static void registerBlockEntities() {
    }
}

