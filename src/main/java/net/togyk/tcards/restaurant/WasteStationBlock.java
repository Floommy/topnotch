package net.togyk.tcards.restaurant;

import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Waste station (German waste sorting: packaging / paper / residual waste). Right-click with a tray to empty it,
 * or with leftover food or a receipt to throw it away. Then the clean tray goes into the tray rack.
 */
public class WasteStationBlock extends Block {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    public WasteStationBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        boolean tray = stack.is(RestaurantRegistry.TRAY.asItem());
        boolean food = RestaurantMenu.pathOf(stack) != null;
        boolean receipt = stack.is(Receipt.ITEM);
        if (!tray && !food && !receipt) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (tray) {
            boolean leftovers = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItemCopyStream().findAny().isPresent();
            if (!leftovers) {
                player.sendOverlayMessage(Component.literal("§7Your tray is already empty - put it in the tray rack"));
                return InteractionResult.SUCCESS;
            }
            ItemStack clean = stack.copyWithCount(1);
            clean.remove(DataComponents.CONTAINER);
            if (stack.getCount() == 1) {
                player.setItemInHand(hand, clean);
            } else {
                stack.shrink(1);
                if (!player.getInventory().add(clean)) player.spawnAtLocation((net.minecraft.server.level.ServerLevel) level, clean);
            }
            player.sendOverlayMessage(Component.literal("§aSorted! Now put the tray in the tray rack"));
        } else {
            stack.shrink(1);
            player.sendOverlayMessage(Component.literal(receipt ? "§7Receipt -> paper" : "§7Leftovers -> residual waste"));
        }
        level.playSound(null, pos, SoundEvents.COMPOSTER_FILL, SoundSource.BLOCKS, 0.8f, 0.9f);
        return InteractionResult.SUCCESS;
    }
}
