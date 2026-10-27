package net.togyk.tog_yks_cards.client.render.state;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

@Environment(value=EnvType.CLIENT)
public class CardHolderBlockEntityRenderState
extends BlockEntityRenderState {
    public List<ItemStack> cards;
    public float rotation;

    public void updateItemRenderState(ItemModelResolver resolver, ItemStackRenderState state, ItemStack stack, ItemDisplayContext context) {
        resolver.updateForNonLiving(state, stack, context, (Entity)Minecraft.getInstance().player);
    }
}

