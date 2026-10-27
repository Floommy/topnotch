package net.togyk.tog_yks_cards.mat_item;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.togyk.tog_yks_cards.block.custom.CardHolderBlock;
import net.togyk.tog_yks_cards.client.menu.SelectCardMenu;
import net.togyk.tog_yks_cards.item.custom.CardHolderItem;
import net.togyk.tog_yks_cards.item.custom.CardItem;
import net.togyk.tog_yks_cards.item.custom.CardpileItem;
import net.togyk.tog_yks_cards.mat_item.MatItem;
import net.togyk.tog_yks_cards.mat_item.MatItemType;

public class CardHolderMatItem
extends MatItem
implements Container,
MenuProvider {
    public static MapCodec<CardHolderMatItem> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(BlockState.CODEC.fieldOf("state").forGetter(MatItem::getState), Codec.INT.fieldOf("x").forGetter(MatItem::getX), Codec.INT.fieldOf("z").forGetter(MatItem::getZ), Codec.list(ItemStack.CODEC).fieldOf("cards").forGetter(CardHolderMatItem::getCards), BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(CardHolderMatItem::getCardHolder)).apply(instance, CardHolderMatItem::new));
    private List<ItemStack> cards;
    private final CardHolderItem cardHolder;

    public CardHolderMatItem(BlockState state, int x, int z, List<ItemStack> cards, Item item) {
        super(state, x, z);
        this.cards = new ArrayList<ItemStack>(cards);
        this.cardHolder = (CardHolderItem)item;
    }

    public CardHolderMatItem(BlockState state, int x, int z, List<ItemStack> cards, CardHolderItem cardPile) {
        super(state, x, z);
        this.cards = new ArrayList<ItemStack>(cards);
        this.cardHolder = cardPile;
    }

    @Override
    public MatItemType getType() {
        return MatItemType.CARD_HOLDER;
    }

    @Override
    public List<ItemStack> getDrops() {
        CardHolderItem item = this.getCardHolder();
        ItemStack holder = item.getDefaultInstance();
        if (!this.getCards().isEmpty()) {
            item.setCards(this.getCards(), holder);
        }
        return List.of(holder);
    }

    public List<ItemStack> getCards() {
        return this.cards;
    }

    public void setCards(List<ItemStack> cards) {
        this.cards = new ArrayList<ItemStack>(cards);
    }

    public void addCard(ItemStack card) {
        this.cards.add(card);
        this.setChanged();
    }

    public void addCards(List<ItemStack> cards) {
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
        ItemStack result = this.cards.remove(i);
        this.setChanged();
        return result;
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
        this.cards.set(slot, stack);
        this.setChanged();
    }

    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity((BlockEntity)this.mat, (Player)player);
    }

    public void clearContent() {
        this.cards.clear();
        this.setChanged();
    }

    public CardHolderItem getCardHolder() {
        return this.cardHolder;
    }

    @Override
    public boolean canOpenMenu(Player player) {
        return this.isInFront(player);
    }

    public boolean isInFront(Player player) {
        Block block;
        BlockState state = this.getState();
        if (!this.isEmpty() && (block = state.getBlock()) instanceof CardHolderBlock) {
            float difference;
            CardHolderBlock cardHolderBlock = (CardHolderBlock)block;
            float rotation = cardHolderBlock.getYRotationDegrees(state);
            AABB bounds = this.getShape().bounds();
            Vec3 itemCenter = Vec3.atCenterOf((Vec3i)this.getMat().getBlockPos()).add((double)this.getX() / 16.0 + (bounds.maxX - bounds.minX) / 2.0, 0.0, (double)this.getZ() / 16.0 + (bounds.maxZ - bounds.minZ) / 2.0);
            Vec3 playerPos = player.position();
            double dz = playerPos.z - itemCenter.z;
            double dx = playerPos.x - itemCenter.x;
            float playerRotation = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
            if (playerRotation < 0.0f) {
                playerRotation += 360.0f;
            }
            return Math.abs(difference = (playerRotation - rotation + 540.0f) % 360.0f - 180.0f) <= 90.0f;
        }
        return false;
    }

    @Override
    public InteractionResult interact(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult result) {
        if (hand.equals(InteractionHand.MAIN_HAND)) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
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
                    }
                }
            } else if (!this.getCard().isEmpty()) {
                ItemStack card = this.getCard();
                this.removeCard(card);
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
        return Component.translatable((String)"block.tog_yks-cards.card_holder");
    }

    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new SelectCardMenu(containerId, inventory, this);
    }

    @Override
    public MatItem getRenderState(Player player) {
        List<ItemStack> cards = this.isInFront(player) ? this.getCards() : this.getCards().stream().map(stack -> stack.getItem().getDefaultInstance()).toList();
        return new CardHolderMatItem(this.getState(), this.getX(), this.getZ(), cards, this.getCardHolder());
    }
}

