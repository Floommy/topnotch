package net.togyk.tog_yks_cards.client.render.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

@Environment(value=EnvType.CLIENT)
public class CardPileBlockEntityRenderState
extends BlockEntityRenderState {
    public Direction facing;
    public ItemStack card;
    public boolean faceDown;
    public int countStage;

    public void updateItemRenderState(ItemModelResolver resolver, ItemStackRenderState state, ItemStack stack, ItemDisplayContext context) {
        resolver.updateForNonLiving(state, stack, context, (Entity)Minecraft.getInstance().player);
    }
}

