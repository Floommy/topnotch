package net.togyk.tog_yks_cards.item.custom;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.togyk.tcards.Pair;
import net.togyk.tog_yks_cards.block.custom.CardboxBlock;
import net.togyk.tog_yks_cards.block.custom.CardpileBlock;
import net.togyk.tog_yks_cards.block.custom.MatBlock;
import net.togyk.tog_yks_cards.blockentity.entities.MatBlockEntity;
import net.togyk.tog_yks_cards.component.ModComponents;
import net.togyk.tog_yks_cards.decks.Deck;
import net.togyk.tog_yks_cards.item.custom.CardItem;
import net.togyk.tog_yks_cards.item.custom.CardpileItem;
import net.togyk.tog_yks_cards.item.custom.MatPlaceable;
import net.togyk.tog_yks_cards.mat_item.CardBoxMatItem;
import net.togyk.tog_yks_cards.mat_item.CardPileMatItem;
import net.togyk.tog_yks_cards.mat_item.MatItem;

public class CardboxItem
extends BlockItem
implements MatPlaceable {
    private final CardpileBlock cardpileBlock;
    private final Deck deck;
    private final Item cardItem;

    public CardboxItem(Block block, CardpileBlock cardpileBlock, Item.Properties properties, Deck deck, Item cardItem) {
        super(block, properties);
        this.cardpileBlock = cardpileBlock;
        this.deck = deck;
        this.cardItem = cardItem;
    }

    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        ItemStack stack = context.getItemInHand();
        if (super.placeBlock(context, state)) {
            Block block = state.getBlock();
            if (block instanceof CardpileBlock) {
                CardpileBlock cardpile = (CardpileBlock)block;
                if (context.getPlayer() != null) {
                    Level level = context.getLevel();
                    BlockPos pos = context.getClickedPos();
                    cardpile.setCards(level, pos, this.getCards(stack));
                    cardpile.setFaceDown(level, pos, !context.getPlayer().isShiftKeyDown());
                    this.setCards(stack, new ArrayList<ItemStack>());
                    return true;
                }
            }
            if ((block = state.getBlock()) instanceof CardboxBlock) {
                CardboxBlock cardbox = (CardboxBlock)block;
                cardbox.setCardbox(context.getLevel(), context.getClickedPos(), stack.copy());
                return true;
            }
        }
        return false;
    }

    public InteractionResult place(BlockPlaceContext context) {
        ItemStack stack = context.getItemInHand();
        if (!this.isOpen(stack)) {
            return super.place(context);
        }
        if (!this.getBlock().isEnabled(context.getLevel().enabledFeatures())) {
            return InteractionResult.FAIL;
        }
        if (!context.canPlace()) {
            return InteractionResult.FAIL;
        }
        BlockPlaceContext updatedPlaceContext = this.updatePlacementContext(context);
        if (updatedPlaceContext == null) {
            return InteractionResult.FAIL;
        }
        BlockState placementState = this.getPlacementState(updatedPlaceContext);
        if (placementState == null) {
            return InteractionResult.FAIL;
        }
        if (!this.placeBlock(updatedPlaceContext, placementState)) {
            return InteractionResult.FAIL;
        }
        BlockPos pos = updatedPlaceContext.getClickedPos();
        Level level = updatedPlaceContext.getLevel();
        Player player = updatedPlaceContext.getPlayer();
        ItemStack itemStack = updatedPlaceContext.getItemInHand();
        BlockState placedState = level.getBlockState(pos);
        if (placedState.is(placementState.getBlock())) {
            placedState = this.updateBlockStateFromTag(pos, level, itemStack, placedState);
            CardboxItem.updateCustomBlockEntityTag((Level)level, (Player)player, (BlockPos)pos, (ItemStack)itemStack);
            CardboxItem.updateBlockEntityComponents(level, pos, itemStack);
            placedState.getBlock().setPlacedBy(level, pos, placedState, (LivingEntity)player, itemStack);
            if (player instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer)player;
                CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, pos, (ItemInstance)itemStack);
            }
        }
        SoundType soundType = placedState.getSoundType();
        level.playSound((Entity)player, pos, this.getPlaceSound(placedState), SoundSource.BLOCKS, (soundType.getVolume() + 1.0f) / 2.0f, soundType.getPitch() * 0.8f);
        level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of((Entity)player, (BlockState)placedState));
        return InteractionResult.SUCCESS;
    }

    private BlockState updateBlockStateFromTag(BlockPos blockPos, Level level, ItemStack itemStack, BlockState blockState) {
        BlockItemStateProperties blockItemStateProperties = (BlockItemStateProperties)itemStack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
        if (blockItemStateProperties.isEmpty()) {
            return blockState;
        }
        BlockState blockState2 = blockItemStateProperties.apply(blockState);
        if (blockState2 != blockState) {
            level.setBlock(blockPos, blockState2, 2);
        }
        return blockState2;
    }

    private static void updateBlockEntityComponents(Level level, BlockPos blockPos, ItemStack itemStack) {
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if (blockEntity != null) {
            blockEntity.applyComponentsFromItemStack(itemStack);
            blockEntity.setChanged();
        }
    }

    public Deck getDeck() {
        return this.deck;
    }

    public List<ItemStack> getCards() {
        ArrayList<ItemStack> cards = new ArrayList<ItemStack>();
        for (List<Pair<String, String>> properties : this.deck.getProperty().getCardProperties()) {
            ItemStack cardStack = this.cardItem.getDefaultInstance();
            Item item = this.cardItem;
            if (item instanceof CardItem) {
                CardItem card = (CardItem)item;
                card.setCardData(properties, cardStack);
            }
            cards.add(cardStack);
        }
        return cards;
    }

    public List<ItemStack> getCards(ItemStack stack) {
        List cards = (List)stack.get(ModComponents.CARDS);
        return cards == null ? this.getCards() : new ArrayList(cards);
    }

    public void addCards(ItemStack stack, List<ItemStack> cards) {
        List<ItemStack> stacks = this.getCards(stack);
        stacks.addAll(cards.stream().filter(stack1 -> !stack1.isEmpty()).toList());
        this.setCards(stack, stacks);
    }

    public void setCards(ItemStack stack, List<ItemStack> cards) {
        stack.set(ModComponents.CARDS, cards.stream().filter(stack1 -> !stack1.isEmpty()).toList());
        this.setEmpty(stack, cards.isEmpty());
    }

    public void removeCards(ItemStack stack) {
        stack.remove(ModComponents.CARDS);
        this.setEmpty(stack, true);
    }

    public boolean isOpen(ItemStack stack) {
        return (Boolean)stack.getOrDefault(ModComponents.OPEN, false);
    }

    public void setOpen(ItemStack stack, boolean open) {
        stack.set(ModComponents.OPEN, open);
    }

    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.has(ModComponents.CARDS)) {
            level.playSound((Entity)player, player.getOnPos(), SoundEvents.AZALEA_BREAK, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
        if (!level.isClientSide()) {
            this.setOpen(stack, !this.isOpen(stack));
            if (!stack.has(ModComponents.CARDS)) {
                this.setCards(stack, this.getCards());
            }
        }
        level.playSound((Entity)player, player.getOnPos(), SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 1.0f, 1.0f);
        return InteractionResult.SUCCESS;
    }

    public InteractionResult useOn(UseOnContext context) {
        BlockPos blockPos;
        Level level;
        BlockState state;
        Block block;
        ItemStack stack = context.getItemInHand();
        if (this.isOpen(stack) && (block = (state = (level = context.getLevel()).getBlockState(blockPos = context.getClickedPos())).getBlock()) instanceof CardpileBlock) {
            CardpileBlock cardpile = (CardpileBlock)block;
            this.addCards(stack, cardpile.getCards(level, blockPos));
            level.removeBlock(blockPos, false);
            return InteractionResult.SUCCESS;
        }
        return super.useOn(context);
    }

    protected BlockState getPlacementState(BlockPlaceContext context) {
        ItemStack stack = context.getItemInHand();
        if (this.isOpen(stack)) {
            if (!this.getCards(stack).isEmpty()) {
                BlockState blockState = this.cardpileBlock.getStateForPlacement(context);
                return blockState != null && this.canPlace(context, blockState) ? blockState : null;
            }
        } else {
            return super.getPlacementState(context);
        }
        return null;
    }

    public boolean isEmpty(ItemStack stack) {
        return (Boolean)stack.getOrDefault(ModComponents.EMPTY, false);
    }

    public void setEmpty(ItemStack stack, boolean empty) {
        stack.set(ModComponents.EMPTY, empty);
    }

    public boolean isWrapped(ItemStack stack) {
        return !stack.has(ModComponents.CARDS);
    }

    @Override
    public MatItem getMatItem(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult blockHitResult) {
        Vec3 relativeHit = blockHitResult.getLocation().subtract((double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
        if (this.isOpen(stack)) {
            Item item = this.cardpileBlock.asItem();
            if (item instanceof CardpileItem) {
                CardpileItem cardpile = (CardpileItem)item;
                if (!this.getCards(stack).isEmpty()) {
                    BlockState itemState = (BlockState)((BlockState)cardpile.getBlock().defaultBlockState().setValue(CardpileBlock.FACING, player.getDirection())).setValue(CardpileBlock.FACE_DOWN, Boolean.valueOf(true));
                    return new CardPileMatItem(itemState, (int)(relativeHit.x * 16.0) - 8, (int)(relativeHit.z * 16.0) - 8, this.getCards(stack), cardpile);
                }
            }
            return null;
        }
        return new CardBoxMatItem((BlockState)((BlockState)((BlockState)this.getBlock().defaultBlockState().setValue(CardboxBlock.EMPTY, Boolean.valueOf(this.getCards(stack).isEmpty()))).setValue(CardboxBlock.FACING, player.getDirection().getOpposite())).setValue(CardboxBlock.WRAPPED, Boolean.valueOf(this.isWrapped(stack))), (int)(relativeHit.x * 16.0) - 8, (int)(relativeHit.z * 16.0) - 8, stack);
    }

    @Override
    public boolean place(MatBlock mat, ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult blockHitResult) {
        BlockEntity blockEntity;
        MatItem newItem = this.getMatItem(stack, state, level, pos, player, hand, blockHitResult);
        if (newItem instanceof CardBoxMatItem && (blockEntity = level.getBlockEntity(pos)) instanceof MatBlockEntity) {
            MatBlockEntity matEntity = (MatBlockEntity)blockEntity;
            newItem.setMat(matEntity);
            if (this.canPlace(newItem, mat, level, pos)) {
                mat.addMatItem((BlockGetter)level, pos, newItem);
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
            }
            mat.setChanged((BlockGetter)level, pos);
            return true;
        }
        if (newItem instanceof CardPileMatItem && (blockEntity = level.getBlockEntity(pos)) instanceof MatBlockEntity) {
            MatBlockEntity matEntity = (MatBlockEntity)blockEntity;
            newItem.setMat(matEntity);
            if (this.canPlace(newItem, mat, level, pos)) {
                mat.addMatItem((BlockGetter)level, pos, newItem);
                this.setCards(stack, new ArrayList<ItemStack>());
            }
            mat.setChanged((BlockGetter)level, pos);
            return true;
        }
        return false;
    }
}

