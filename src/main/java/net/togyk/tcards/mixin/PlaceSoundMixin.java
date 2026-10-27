package net.togyk.tcards.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.togyk.tcards.TableSounds;
import net.togyk.tog_yks_cards.item.custom.CardItem;
import net.togyk.tog_yks_cards.item.custom.CardboxItem;
import net.togyk.tog_yks_cards.item.custom.CardpileItem;
import net.togyk.tog_yks_cards.item.custom.CoinItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Card snap + puff (or chip clink) when cards / piles / chips are put on the ground. */
@Mixin({CardItem.class, CardpileItem.class, CoinItem.class, CardboxItem.class})
public class PlaceSoundMixin {
    @Inject(method = "placeBlock", at = @At("RETURN"))
    private void tcards$placeSound(BlockPlaceContext context, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;
        Level level = context.getLevel();
        if (level.isClientSide()) return;
        BlockPos pos = context.getClickedPos();
        if ((Object) this instanceof CoinItem) {
            TableSounds.play(level, pos, TableSounds.CHIP_CLINK, 0.7f);
        } else if (!((Object) this instanceof CardboxItem) || state.getBlock() instanceof net.togyk.tog_yks_cards.block.custom.CardpileBlock) {
            TableSounds.play(level, pos, TableSounds.CARD_PLACE, 0.8f);
            TableSounds.puff(level, pos.getX() + 0.5, pos.getY() + 0.05, pos.getZ() + 0.5);
        }
    }
}
