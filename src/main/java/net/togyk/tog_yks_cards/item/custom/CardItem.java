package net.togyk.tog_yks_cards.item.custom;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.togyk.tcards.Pair;
import net.togyk.tog_yks_cards.block.custom.CardpileBlock;
import net.togyk.tog_yks_cards.component.ModComponents;
import net.togyk.tog_yks_cards.decks.Deck;
import net.togyk.tog_yks_cards.item.custom.CardpileItem;
import net.togyk.tog_yks_cards.item.custom.MatPlaceable;
import net.togyk.tog_yks_cards.mat_item.CardPileMatItem;

public class CardItem
extends BlockItem
implements MatPlaceable {
    private final Deck deck;

    public CardItem(Item.Properties properties, Deck deck, CardpileBlock cardpile) {
        super((Block)cardpile, properties);
        this.deck = deck;
    }

    public InteractionResult useOn(UseOnContext useOnContext) {
        return super.useOn(useOnContext);
    }

    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        if (super.placeBlock(context, state)) {
            Level level = context.getLevel();
            BlockPos pos = context.getClickedPos();
            Block block = state.getBlock();
            if (block instanceof CardpileBlock) {
                CardpileBlock cardPile = (CardpileBlock)block;
                cardPile.setCards(level, pos, List.of(context.getItemInHand().copy()));
                if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
                    cardPile.setFaceDown(level, pos, false);
                }
            }
            return true;
        }
        return false;
    }

    public void appendHoverText(ItemStack stack, Item.TooltipContext tooltipContext, TooltipDisplay tooltipDisplay, Consumer<Component> consumer, TooltipFlag tooltipFlag) {
        Map<String, String> data = this.getCardData(stack);
        for (String key : data.keySet()) {
            consumer.accept((Component)Component.literal((String)data.get(key)).withStyle(ChatFormatting.GOLD));
        }
    }

    public Deck getDeck() {
        return this.deck;
    }

    public void setCardData(List<Pair<String, String>> properties, ItemStack stack) {
        HashMap<String, String> cardData = new HashMap<String, String>();
        for (Pair<String, String> property : properties) {
            cardData.put((String)property.first, (String)property.second);
        }
        this.setCardData(cardData, stack);
    }

    public void setCardData(Map<String, String> cardData, ItemStack stack) {
        stack.set(ModComponents.CARD_DATA, cardData);
    }

    public Map<String, String> getCardData(ItemStack stack) {
        return (Map)stack.getOrDefault(ModComponents.CARD_DATA, new HashMap());
    }

    public void registerBlocks(Map<Block, Item> map, Item item) {
        if (item instanceof CardItem) {
            return;
        }
        super.registerBlocks(map, item);
    }

    @Override
    public CardPileMatItem getMatItem(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult blockHitResult) {
        BlockState itemState = (BlockState)((BlockState)this.getBlock().defaultBlockState().setValue(CardpileBlock.FACING, player.getDirection())).setValue(CardpileBlock.FACE_DOWN, Boolean.valueOf(false));
        Vec3 relativeHit = blockHitResult.getLocation().subtract((double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
        return new CardPileMatItem(itemState, (int)(relativeHit.x * 16.0) - 8, (int)(relativeHit.z * 16.0) - 8, List.of(stack.copy()), (CardpileItem)itemState.getBlock().asItem());
    }
}

