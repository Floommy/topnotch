package net.togyk.tog_yks_cards.mat_item;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.togyk.tog_yks_cards.block.custom.CardpileBlock;
import net.togyk.tog_yks_cards.client.menu.SelectCardMenu;
import net.togyk.tog_yks_cards.item.custom.CardItem;
import net.togyk.tog_yks_cards.item.custom.CardboxItem;
import net.togyk.tog_yks_cards.item.custom.CardpileItem;
import net.togyk.tog_yks_cards.mat_item.MatItem;
import net.togyk.tog_yks_cards.mat_item.MatItemType;

public class CardPileMatItem
extends MatItem
implements Container,
MenuProvider {
    public static MapCodec<CardPileMatItem> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(BlockState.CODEC.fieldOf("state").forGetter(MatItem::getState), Codec.INT.fieldOf("x").forGetter(MatItem::getX), Codec.INT.fieldOf("z").forGetter(MatItem::getZ), Codec.list(ItemStack.CODEC).fieldOf("cards").forGetter(CardPileMatItem::getCards), BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(CardPileMatItem::getCardPile)).apply(instance, CardPileMatItem::new));
    private List<ItemStack> cards;
    private final CardpileItem cardPile;

    private CardPileMatItem(BlockState state, int x, int z, List<ItemStack> cards, Item item) {
        this(state, x, z, cards, (CardpileItem)item);
    }

    public CardPileMatItem(BlockState state, int x, int z, List<ItemStack> cards, CardpileItem cardPile) {
        super(state, x, z);
        this.setCards(cards);
        this.cardPile = cardPile;
        this.updateState();
    }

    @Override
    public MatItemType getType() {
        return MatItemType.CARD_PILE;
    }

    @Override
    public List<ItemStack> getDrops() {
        CardpileItem item = this.getCardPile();
        ItemStack pile = item.getDefaultInstance();
        if (!this.getCards().isEmpty()) {
            item.setCards(this.getCards(), pile);
        }
        return List.of(pile);
    }

    public List<ItemStack> getCards() {
        return this.cards;
    }

    public void setCards(List<ItemStack> cards) {
        cards.forEach(item -> item.setCount(1));
        this.cards = new ArrayList<ItemStack>(cards);
        this.setChanged();
    }

    public void addCard(ItemStack card) {
        card.setCount(1);
        this.cards.add(card);
        this.setChanged();
    }

    public void addCards(List<ItemStack> cards) {
        cards.forEach(item -> item.setCount(1));
        this.cards.addAll(cards);
        this.setChanged();
    }

    public ItemStack getCard() {
        return this.cards.isEmpty() ? ItemStack.EMPTY : this.cards.getLast();
    }

    public void removeCard(ItemStack card) {
        this.cards.remove(card);
        this.setChanged();
    }

    public ItemStack removeCard(int i) {
        ItemStack stack = this.cards.remove(i);
        this.setChanged();
        return stack;
    }

    public ItemStack removeLast() {
        ItemStack stack = this.cards.removeLast();
        this.setChanged();
        return stack;
    }

    public int getContainerSize() {
        return this.cards.size();
    }

    public boolean isEmpty() {
        return this.cards.isEmpty();
    }

    public ItemStack getItem(int slot) {
        if (slot < 0 || slot >= this.cards.size()) {
            return ItemStack.EMPTY;
        }
        return this.cards.get(slot);
    }

    public ItemStack removeItem(int slot, int amount) {
        if (slot < 0 || slot >= this.cards.size()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = this.cards.get(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = stack.split(amount);
        if (stack.isEmpty()) {
            this.cards.remove(slot);
        }
        this.setChanged();
        return result;
    }

    public ItemStack removeItemNoUpdate(int slot) {
        if (slot < 0 || slot >= this.cards.size()) {
            return ItemStack.EMPTY;
        }
        return this.cards.remove(slot);
    }

    public void setItem(int slot, ItemStack stack) {
        if (this.cards.size() <= slot) {
            return;
        }
        stack.setCount(1);
        this.cards.set(slot, stack);
        this.setChanged();
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (this.cards.isEmpty() && this.mat != null) {
            this.mat.removeItem(this);
        }
    }

    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity((BlockEntity)this.mat, (Player)player);
    }

    public void clearContent() {
        this.cards.clear();
        this.setChanged();
    }

    public CardpileItem getCardPile() {
        return this.cardPile;
    }

    @Override
    public boolean canOpenMenu(Player player) {
        return !this.isEmpty();
    }

    @Override
    public void updateState() {
        super.updateState();
        BlockState state = this.getState();
        Block block = state.getBlock();
        if (block instanceof CardpileBlock) {
            CardpileBlock block2 = (CardpileBlock)block;
            this.setState((BlockState)state.setValue(CardpileBlock.STAGE, Integer.valueOf(block2.getCountStage(this.cards.size()))));
        }
    }

    @Override
    public InteractionResult interact(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult blockHitResult) {
        if (!level.isClientSide() && hand.equals(InteractionHand.MAIN_HAND)) {
            if (!stack.isEmpty()) {
                if (stack.getItem() instanceof CardItem) {
                    level.playSound((Entity)player, player.getOnPos(), SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 1.0f, 1.0f);
                    if (!level.isClientSide()) {
                        ItemStack cardStack = stack.copy();
                        cardStack.setCount(1);
                        this.addCard(cardStack);
                        stack.shrink(1);
                        return InteractionResult.SUCCESS;
                    }
                } else {
                    Item cardStack = stack.getItem();
                    if (cardStack instanceof CardpileItem) {
                        CardpileItem cardpile = (CardpileItem)cardStack;
                        level.playSound((Entity)player, player.getOnPos(), SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 1.0f, 1.0f);
                        if (!level.isClientSide()) {
                            List<ItemStack> cards = cardpile.getCards(stack);
                            this.addCards(cards);
                            stack.shrink(1);
                            return InteractionResult.SUCCESS;
                        }
                    } else {
                        CardboxItem box;
                        Item item = stack.getItem();
                        if (item instanceof CardboxItem && (box = (CardboxItem)item).isOpen(stack)) {
                            box.addCards(stack, this.getCards());
                            this.setCards(new ArrayList<ItemStack>());
                            return InteractionResult.SUCCESS;
                        }
                    }
                }
            } else if (!this.getCard().isEmpty()) {
                ItemStack card = this.getCard().copy();
                this.removeLast();
                if (stack.isEmpty()) {
                    player.setItemInHand(hand, card);
                } else if (!player.addItem(card)) {
                    player.drop(card, false, Prediction.PREDICTED);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    public Component getDisplayName() {
        return Component.translatable((String)"block.tog_yks-cards.card_pile");
    }

    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new SelectCardMenu(containerId, inventory, this);
    }

    @Override
    public CardPileMatItem getRenderState(Player player) {
        Stream<ItemStack> cards = (Boolean)this.getState().getValueOrElse(CardpileBlock.FACE_DOWN, Boolean.valueOf(true)) != false ? this.getCards().stream().map(stack -> stack.getItem().getDefaultInstance()) : this.getCards().stream().map(stack -> stack != this.getCard() ? stack.getItem().getDefaultInstance() : stack);
        return new CardPileMatItem(this.getState(), this.getX(), this.getZ(), cards.toList(), this.getCardPile());
    }
}

