package net.togyk.tog_yks_cards.mat_item;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.togyk.tog_yks_cards.block.custom.CardboxBlock;
import net.togyk.tog_yks_cards.mat_item.MatItem;
import net.togyk.tog_yks_cards.mat_item.MatItemType;

public class CardBoxMatItem
extends MatItem {
    public static MapCodec<CardBoxMatItem> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(BlockState.CODEC.fieldOf("state").forGetter(MatItem::getState), Codec.INT.fieldOf("x").forGetter(MatItem::getX), Codec.INT.fieldOf("z").forGetter(MatItem::getZ), ItemStack.CODEC.fieldOf("cardbox").forGetter(CardBoxMatItem::getBox)).apply(instance, CardBoxMatItem::new));
    private ItemStack cardbox;

    public CardBoxMatItem(BlockState state, int x, int z, ItemStack cardbox) {
        super(state, x, z);
        this.cardbox = cardbox.copy();
    }

    @Override
    public MatItemType getType() {
        return MatItemType.CARD_BOX;
    }

    @Override
    public List<ItemStack> getDrops() {
        return List.of(this.cardbox);
    }

    public ItemStack getBox() {
        return this.cardbox;
    }

    public void setCards(ItemStack cardbox) {
        this.cardbox = cardbox;
    }

    @Override
    public InteractionResult interact(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult blockHitResult) {
        BlockState blockState = this.getState();
        if (!((Boolean)blockState.getValueOrElse(CardboxBlock.WRAPPED, Boolean.valueOf(true))).booleanValue()) {
            boolean currentState = (Boolean)blockState.getValue(CardboxBlock.OPEN);
            this.setState((BlockState)blockState.setValue(CardboxBlock.OPEN, Boolean.valueOf(!currentState)));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}

