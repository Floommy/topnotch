package net.togyk.tog_yks_cards.block.custom;

import net.minecraft.world.level.storage.loot.LootParams;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.togyk.tog_yks_cards.block.custom.MatBlock;
import net.togyk.tog_yks_cards.blockentity.entities.CardPileBlockEntity;
import net.togyk.tog_yks_cards.decks.Deck;
import net.togyk.tog_yks_cards.item.custom.CardItem;
import net.togyk.tog_yks_cards.item.custom.CardboxItem;
import net.togyk.tog_yks_cards.item.custom.CardpileItem;

public class CardpileBlock
extends BaseEntityBlock
implements SimpleWaterloggedBlock {
    public static final VoxelShape SHAPE_NS_0 = Block.box((double)6.0, (double)0.0, (double)5.2, (double)10.0, (double)0.1, (double)10.8);
    public static final VoxelShape SHAPE_EW_0 = Block.box((double)5.2, (double)0.0, (double)6.0, (double)10.8, (double)0.1, (double)10.0);
    public static final VoxelShape SHAPE_NS_1 = Block.box((double)6.0, (double)0.0, (double)5.2, (double)10.0, (double)0.35, (double)10.8);
    public static final VoxelShape SHAPE_EW_1 = Block.box((double)5.2, (double)0.0, (double)6.0, (double)10.8, (double)0.35, (double)10.0);
    public static final VoxelShape SHAPE_NS_2 = Block.box((double)6.0, (double)0.0, (double)5.2, (double)10.0, (double)0.6, (double)10.8);
    public static final VoxelShape SHAPE_EW_2 = Block.box((double)5.2, (double)0.0, (double)6.0, (double)10.8, (double)0.6, (double)10.0);
    public static final VoxelShape SHAPE_NS_3 = Block.box((double)6.0, (double)0.0, (double)5.2, (double)10.0, (double)1.1, (double)10.8);
    public static final VoxelShape SHAPE_EW_3 = Block.box((double)5.2, (double)0.0, (double)6.0, (double)10.8, (double)1.1, (double)10.0);
    public static final VoxelShape SHAPE_NS_4 = Block.box((double)6.0, (double)0.0, (double)5.2, (double)10.0, (double)2.1, (double)10.8);
    public static final VoxelShape SHAPE_EW_4 = Block.box((double)5.2, (double)0.0, (double)6.0, (double)10.8, (double)2.1, (double)10.0);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty FACE_DOWN = BooleanProperty.create((String)"face_down");
    public static final IntegerProperty STAGE = IntegerProperty.create((String)"count", (int)0, (int)4);
    public final Deck deck;

    public VoxelShape getShape(BlockState state, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return switch ((Integer)state.getValue(STAGE)) {
            case 4 -> {
                switch ((Direction)state.getValue(FACING)) {
                    case EAST: 
                    case WEST: {
                        yield SHAPE_EW_4;
                    }
                }
                yield SHAPE_NS_4;
            }
            case 3 -> {
                switch ((Direction)state.getValue(FACING)) {
                    case EAST: 
                    case WEST: {
                        yield SHAPE_EW_3;
                    }
                }
                yield SHAPE_NS_3;
            }
            case 2 -> {
                switch ((Direction)state.getValue(FACING)) {
                    case EAST: 
                    case WEST: {
                        yield SHAPE_EW_2;
                    }
                }
                yield SHAPE_NS_2;
            }
            case 1 -> {
                switch ((Direction)state.getValue(FACING)) {
                    case EAST: 
                    case WEST: {
                        yield SHAPE_EW_1;
                    }
                }
                yield SHAPE_NS_1;
            }
            default -> {
                switch ((Direction)state.getValue(FACING)) {
                    case EAST: 
                    case WEST: {
                        yield SHAPE_EW_0;
                    }
                }
                yield SHAPE_NS_0;
            }
        };
    }

    public CardpileBlock(BlockBehaviour.Properties pProperties, Deck deck) {
        super(pProperties);
        this.deck = deck;
        this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACE_DOWN, Boolean.valueOf(true))).setValue(STAGE, Integer.valueOf(0))).setValue(MatBlock.PREVIEW, Boolean.valueOf(false)));
    }

    public Deck getDeck() {
        return this.deck;
    }

    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CardPileBlockEntity(pos, state);
    }

    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult result) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof CardPileBlockEntity)) {
            return super.useItemOn(stack, state, level, pos, player, hand, result);
        }
        CardPileBlockEntity cardPile = (CardPileBlockEntity)blockEntity;
        if (!level.isClientSide() && player.isCrouching() && !cardPile.getCards().isEmpty()) {
            player.openMenu((MenuProvider)cardPile);
            return InteractionResult.SUCCESS;
        }
        if (hand.equals(InteractionHand.MAIN_HAND)) {
            if (!stack.isEmpty()) {
                if (stack.getItem() instanceof CardItem) {
                    level.playSound((Entity)player, player.getOnPos(), SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 1.0f, 1.0f);
                    if (!level.isClientSide()) {
                        ItemStack cardStack = stack.copy();
                        cardStack.setCount(1);
                        this.addCard(level, pos, cardStack);
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
                            this.addCards(level, pos, cards);
                            stack.shrink(1);
                            return InteractionResult.SUCCESS;
                        }
                    }
                }
            } else if (!cardPile.getCard().isEmpty() && !(stack.getItem() instanceof CardboxItem)) {
                level.playSound((Entity)player, player.getOnPos(), SoundEvents.BUNDLE_REMOVE_ONE, SoundSource.PLAYERS, 1.0f, 1.0f);
                if (!level.isClientSide()) {
                    ItemStack card = cardPile.getCard().copy();
                    this.removeLastCard(level, pos);
                    if (stack.isEmpty()) {
                        player.setItemInHand(hand, card);
                    } else if (!player.addItem(card)) {
                        player.drop(card, false, Prediction.PREDICTED);
                    }
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return InteractionResult.PASS;
    }

    public List<ItemStack> getCards(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CardPileBlockEntity) {
            CardPileBlockEntity cardPile = (CardPileBlockEntity)blockEntity;
            return cardPile.getCards();
        }
        return new ArrayList<ItemStack>();
    }

    public void setCards(Level level, BlockPos pos, List<ItemStack> cards) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CardPileBlockEntity) {
            CardPileBlockEntity cardPile = (CardPileBlockEntity)blockEntity;
            cardPile.setCards(cards);
            if (!cards.isEmpty()) {
                this.setCountStage(level, pos, this.getCountStage(this.getCards(level, pos).size()));
            }
        }
    }

    public ItemStack getCard(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CardPileBlockEntity) {
            CardPileBlockEntity cardPile = (CardPileBlockEntity)blockEntity;
            return cardPile.getCard();
        }
        return ItemStack.EMPTY;
    }

    public void addCard(Level level, BlockPos pos, ItemStack card) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CardPileBlockEntity) {
            CardPileBlockEntity cardPile = (CardPileBlockEntity)blockEntity;
            cardPile.addCard(card);
            this.setCountStage(level, pos, this.getCountStage(this.getCards(level, pos).size()));
        }
    }

    public void addCards(Level level, BlockPos pos, List<ItemStack> cards) {
        for (ItemStack card : cards) {
            card.setCount(1);
            this.addCard(level, pos, card);
        }
    }

    public void removeLastCard(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CardPileBlockEntity) {
            CardPileBlockEntity cardPile = (CardPileBlockEntity)blockEntity;
            cardPile.removeLastCard(level, pos);
        }
        if (level.getBlockEntity(pos) instanceof CardPileBlockEntity) {
            this.setCountStage(level, pos, this.getCountStage(this.getCards(level, pos).size()));
        }
    }

    public boolean isFaceDown(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CardPileBlockEntity) {
            CardPileBlockEntity cardPile = (CardPileBlockEntity)blockEntity;
            return cardPile.isFaceDown();
        }
        return false;
    }

    public void setFaceDown(Level level, BlockPos pos, boolean faceDown) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CardPileBlockEntity) {
            CardPileBlockEntity cardPile = (CardPileBlockEntity)blockEntity;
            cardPile.setFaceDown(faceDown);
        }
        BlockState state = level.getBlockState(pos);
        state.setValue(FACE_DOWN, Boolean.valueOf(faceDown));
        level.setBlock(pos, (BlockState)state.setValue(FACE_DOWN, Boolean.valueOf(faceDown)), 2);
    }

    public int getCountStage(int count) {
        if (count >= 26) {
            return 4;
        }
        if (count >= 13) {
            return 3;
        }
        if (count >= 5) {
            return 2;
        }
        if (count >= 2) {
            return 1;
        }
        return 0;
    }

    public int getCountStage(BlockState state) {
        return (Integer)state.getValue(STAGE);
    }

    public void setCountStage(Level level, BlockPos pos, int stage) {
        BlockState state = level.getBlockState(pos);
        state.setValue(STAGE, Integer.valueOf(stage));
        level.setBlock(pos, (BlockState)state.setValue(STAGE, Integer.valueOf(stage)), 2);
    }

    protected List<ItemStack> getDrops(BlockState state,
     LootParams.Builder builder) {
        List drops = super.getDrops(state, builder);
        Object object = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (object instanceof CardPileBlockEntity) {
            CardPileBlockEntity cardPile = (CardPileBlockEntity)(object);
            ItemStack stack = this.asItem().getDefaultInstance();
            Item item = stack.getItem();
            if (item instanceof CardpileItem) {
                CardpileItem cardPileItem = (CardpileItem)item;
                cardPileItem.setCards(cardPile.getCards(), stack);
            }
            drops.add(stack);
        }
        return drops;
    }

    protected boolean canSurvive(BlockState blockState, LevelReader levelReader, BlockPos blockPos) {
        return levelReader.getBlockState(blockPos.below()).isSolid();
    }

    public BlockState getStateForPlacement(BlockPlaceContext blockPlaceContext) {
        FluidState fluidState = blockPlaceContext.getLevel().getFluidState(blockPlaceContext.getClickedPos());
        return (BlockState)((BlockState)this.defaultBlockState().setValue(FACING, blockPlaceContext.getHorizontalDirection())).setValue(WATERLOGGED, Boolean.valueOf(fluidState.getType() == Fluids.WATER));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{WATERLOGGED});
        builder.add(new Property[]{FACING});
        builder.add(new Property[]{FACE_DOWN});
        builder.add(new Property[]{STAGE});
        builder.add(new Property[]{MatBlock.PREVIEW});
    }

    protected FluidState getFluidState(BlockState blockState) {
        return (Boolean)blockState.getValue(WATERLOGGED) != false ? Fluids.WATER.getSource(false) : super.getFluidState(blockState);
    }

    public Direction getFacing(BlockState blockState) {
        return (Direction)blockState.getValue(FACING);
    }
}

