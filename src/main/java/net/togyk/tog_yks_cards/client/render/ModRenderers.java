package net.togyk.tog_yks_cards.client.render;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.togyk.tog_yks_cards.blockentity.ModBlockEntities;
import net.togyk.tog_yks_cards.client.render.CardHolderBlockEntityRenderer;
import net.togyk.tog_yks_cards.client.render.CardPileBlockEntityRenderer;
import net.togyk.tog_yks_cards.client.render.CoinPileBlockEntityRenderer;
import net.togyk.tog_yks_cards.client.render.MatBlockEntityRenderer;

public class ModRenderers {
    public static void registerRenderers() {
        BlockEntityRenderers.register(ModBlockEntities.CARD_HOLDER, CardHolderBlockEntityRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.CARD_PILE, CardPileBlockEntityRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.MAT, MatBlockEntityRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.COIN_PILE, CoinPileBlockEntityRenderer::new);
    }
}

