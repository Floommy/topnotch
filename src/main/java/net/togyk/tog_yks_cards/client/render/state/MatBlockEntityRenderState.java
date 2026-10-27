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
import net.minecraft.world.level.block.state.BlockState;
import net.togyk.tog_yks_cards.blockentity.entities.MatBlockEntity;
import net.togyk.tog_yks_cards.mat_item.MatItem;

@Environment(value=EnvType.CLIENT)
public class MatBlockEntityRenderState
extends BlockEntityRenderState {
    public List<MatItem> items;
    public BlockState blockState;
    public MatBlockEntity mat;

    public void updateItemRenderState(ItemModelResolver resolver, ItemStackRenderState state, ItemStack stack, ItemDisplayContext context) {
        resolver.updateForNonLiving(state, stack, context, (Entity)Minecraft.getInstance().player);
    }
}

