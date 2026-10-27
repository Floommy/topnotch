package net.togyk.tog_yks_cards.mat_item;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.togyk.tog_yks_cards.mat_item.MatItem;
import net.togyk.tog_yks_cards.mat_item.MatItemType;

public class SimpleMatItem
extends MatItem {
    public static MapCodec<SimpleMatItem> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(BlockState.CODEC.fieldOf("state").forGetter(MatItem::getState), Codec.INT.fieldOf("x").forGetter(MatItem::getX), Codec.INT.fieldOf("z").forGetter(MatItem::getZ), ItemStack.CODEC.fieldOf("stack").forGetter(SimpleMatItem::getBox)).apply(instance, SimpleMatItem::new));
    private ItemStack stack;

    public SimpleMatItem(BlockState state, int x, int z, ItemStack stack) {
        super(state, x, z);
        this.stack = stack.copy();
    }

    public SimpleMatItem(BlockItem item, BlockPlaceContext placeContext, int x, int z, ItemStack stack) {
        this(item.getBlock().getStateForPlacement(placeContext), x, z, stack);
    }

    public SimpleMatItem(BlockItem item, Level level, Player player, InteractionHand hand, BlockHitResult blockHitResult, int x, int z, ItemStack stack) {
        this(item, new BlockPlaceContext(level, player, hand, stack, blockHitResult), x, z, stack);
    }

    public SimpleMatItem(BlockItem item, UseOnContext useOnContext, int x, int z, ItemStack stack) {
        this(item, new BlockPlaceContext(useOnContext), x, z, stack);
    }

    @Override
    public MatItemType getType() {
        return MatItemType.SIMPLE;
    }

    @Override
    public List<ItemStack> getDrops() {
        return List.of(this.stack);
    }

    public ItemStack getBox() {
        return this.stack;
    }

    public void setCards(ItemStack stack) {
        this.stack = stack;
    }
}

