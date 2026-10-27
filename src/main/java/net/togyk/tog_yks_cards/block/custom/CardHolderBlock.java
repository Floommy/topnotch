package net.togyk.tog_yks_cards.block.custom;

import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.togyk.tog_yks_cards.block.custom.MatBlock;
import net.togyk.tog_yks_cards.blockentity.entities.CardHolderBlockEntity;
import net.togyk.tog_yks_cards.item.custom.CardHolderItem;
import net.togyk.tog_yks_cards.item.custom.CardItem;
import net.togyk.tog_yks_cards.item.custom.CardboxItem;
import net.togyk.tog_yks_cards.item.custom.CardpileItem;

public class CardHolderBlock
extends BaseEntityBlock
implements SimpleWaterloggedBlock {
    public static final IntegerProperty ROTATION = BlockStateProperties.ROTATION_16;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final VoxelShape SHAPE_NS = Block.box((double)2.0, (double)0.0, (double)7.0, (double)14.0, (double)2.0, (double)9.0);
    public static final VoxelShape SHAPE_EW = Block.box((double)7.0, (double)0.0, (double)2.0, (double)9.0, (double)2.0, (double)14.0);
    public static final VoxelShape SHAPE_NESW = Block.box((double)4.0, (double)0.0, (double)4.0, (double)12.0, (double)2.0, (double)12.0);

    public VoxelShape getShape(BlockState state, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        float rotation = this.getYRotationDegrees(state);
        if (rotation % 180.0f == 0.0f) {
            return SHAPE_NS;
        }
        if (rotation % 90.0f == 0.0f) {
            return SHAPE_EW;
        }
        return SHAPE_NESW;
    }

    public CardHolderBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)this.defaultBlockState().setValue(MatBlock.PREVIEW, Boolean.valueOf(false)));
    }

    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CardHolderBlockEntity(pos, state);
    }

    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult result) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof CardHolderBlockEntity)) {
            return super.useItemOn(stack, state, level, pos, player, hand, result);
        }
        CardHolderBlockEntity cardHolder = (CardHolderBlockEntity)blockEntity;
        if (!level.isClientSide() && player.isCrouching() && !cardHolder.getCards().isEmpty()) {
            float difference;
            float rotation = this.getYRotationDegrees(state);
            float playerRotation = player.getYRot() - 180.0f;
            if (playerRotation < 0.0f) {
                playerRotation += 360.0f;
            }
            if (Math.abs(difference = (playerRotation - rotation + 540.0f) % 360.0f - 180.0f) <= 67.5f) {
                player.openMenu((MenuProvider)cardHolder);
            }
            return InteractionResult.SUCCESS;
        }
        if (hand.equals(InteractionHand.MAIN_HAND)) {
            if (!stack.isEmpty()) {
                if (stack.getItem() instanceof CardItem) {
                    level.playSound((Entity)player, player.getOnPos(), SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 1.0f, 1.0f);
                    if (!level.isClientSide()) {
                        ItemStack cardStack = stack.copy();
                        cardStack.setCount(1);
                        cardHolder.addCard(cardStack);
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
                            cardHolder.addCards(cards);
                            stack.shrink(1);
                            return InteractionResult.SUCCESS;
                        }
                    }
                }
            } else if (!cardHolder.getCard().isEmpty() && !(stack.getItem() instanceof CardboxItem)) {
                level.playSound((Entity)player, player.getOnPos(), SoundEvents.BUNDLE_REMOVE_ONE, SoundSource.PLAYERS, 1.0f, 1.0f);
                if (!level.isClientSide()) {
                    ItemStack card = cardHolder.getCard();
                    cardHolder.removeCard(card);
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

    protected List<ItemStack> getDrops(BlockState state,
     LootParams.Builder builder) {
        List drops = super.getDrops(state, builder);
        Object object = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (object instanceof CardHolderBlockEntity) {
            CardHolderBlockEntity cardHolder = (CardHolderBlockEntity)(object);
            ItemStack stack = this.asItem().getDefaultInstance();
            Item item = stack.getItem();
            if (item instanceof CardHolderItem) {
                CardHolderItem cardHolderItem = (CardHolderItem)item;
                if (!cardHolder.isEmpty()) {
                    cardHolderItem.setCards((List<ItemStack>)cardHolder.getCards(), stack);
                }
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
        return (BlockState)((BlockState)this.defaultBlockState().setValue(ROTATION, Integer.valueOf(RotationSegment.convertToSegment((float)(blockPlaceContext.getRotation() + 180.0f))))).setValue(WATERLOGGED, Boolean.valueOf(fluidState.getType() == Fluids.WATER));
    }

    protected BlockState updateShape(BlockState blockState, LevelReader levelReader, ScheduledTickAccess scheduledTickAccess, BlockPos blockPos, Direction direction, BlockPos blockPos2, BlockState blockState2, RandomSource randomSource) {
        return direction == Direction.DOWN && !this.canSurvive(blockState, levelReader, blockPos) ? Blocks.AIR.defaultBlockState() : super.updateShape(blockState, levelReader, scheduledTickAccess, blockPos, direction, blockPos2, blockState2, randomSource);
    }

    public float getYRotationDegrees(BlockState blockState) {
        return RotationSegment.convertToDegrees((int)((Integer)blockState.getValue(ROTATION)));
    }

    protected BlockState rotate(BlockState blockState, Rotation rotation) {
        return (BlockState)blockState.setValue(ROTATION, Integer.valueOf(rotation.rotate(((Integer)blockState.getValue(ROTATION)).intValue(), 16)));
    }

    protected BlockState mirror(BlockState blockState, Mirror mirror) {
        return (BlockState)blockState.setValue(ROTATION, Integer.valueOf(mirror.mirror(((Integer)blockState.getValue(ROTATION)).intValue(), 16)));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{ROTATION, WATERLOGGED});
        builder.add(new Property[]{MatBlock.PREVIEW});
    }

    protected FluidState getFluidState(BlockState blockState) {
        return (Boolean)blockState.getValue(WATERLOGGED) != false ? Fluids.WATER.getSource(false) : super.getFluidState(blockState);
    }
}

