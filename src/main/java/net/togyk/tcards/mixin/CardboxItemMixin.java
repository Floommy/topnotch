package net.togyk.tcards.mixin;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.togyk.tcards.TableActions;
import net.togyk.tog_yks_cards.block.custom.CardpileBlock;
import net.togyk.tog_yks_cards.block.custom.MatBlock;
import net.togyk.tog_yks_cards.item.custom.CardboxItem;
import net.togyk.tog_yks_cards.mat_item.CardPileMatItem;
import net.togyk.tog_yks_cards.mat_item.MatItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CardboxItem.class)
public abstract class CardboxItemMixin {
    /** Sneak + right click with an open box on a pile: put the whole deck onto that pile (build a shoe). */
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void tcards$dumpOntoPile(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) return;
        CardboxItem self = (CardboxItem) (Object) this;
        ItemStack stack = context.getItemInHand();
        if (!self.isOpen(stack)) return;
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        CardPileMatItem matPile = null;
        boolean groundPile = state.getBlock() instanceof CardpileBlock;
        if (!groundPile) {
            MatItem item = TableActions.findMatItem(level, pos, context.getClickLocation());
            if (item instanceof CardPileMatItem p) matPile = p;
        }
        if (!groundPile && matPile == null) return;

        if (!level.isClientSide()) {
            List<ItemStack> boxCards = new ArrayList<>();
            for (ItemStack c : self.getCards(stack)) boxCards.add(c.copyWithCount(1));
            if (boxCards.isEmpty()) {
                cir.setReturnValue(InteractionResult.PASS);
                return;
            }
            if (groundPile) {
                CardpileBlock cardpile = (CardpileBlock) state.getBlock();
                List<ItemStack> merged = new ArrayList<>(cardpile.getCards(level, pos));
                merged.addAll(boxCards);
                cardpile.setCards(level, pos, merged);
            } else {
                matPile.addCards(boxCards);
            }
            self.setCards(stack, new ArrayList<>());
        }
        cir.setReturnValue(InteractionResult.SUCCESS);
    }
}
