package net.togyk.tcards.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.togyk.tcards.shuffler.DealerBellBlock;
import net.togyk.tog_yks_cards.mat_item.MatItem;
import net.togyk.tog_yks_cards.mat_item.SimpleMatItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A dealer bell standing on a game mat rings when clicked (empty hand or anything except sneaking). */
@Mixin(MatItem.class)
public class MatBellMixin {
    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void tcards$ring(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                             BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if ((Object) this instanceof SimpleMatItem bell && bell.getState().getBlock() instanceof DealerBellBlock
                && hand == InteractionHand.MAIN_HAND && !player.isShiftKeyDown()) {
            DealerBellBlock.ringOnMat(bell, level);
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }
}
