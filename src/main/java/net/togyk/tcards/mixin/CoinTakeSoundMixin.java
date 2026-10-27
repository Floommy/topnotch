package net.togyk.tcards.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.togyk.tcards.TableSounds;
import net.togyk.tog_yks_cards.block.custom.CoinPileblock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Chip clink when a chip is taken from a pile on the ground. */
@Mixin(CoinPileblock.class)
public class CoinTakeSoundMixin {
    @Inject(method = "removeLastCoin", at = @At("HEAD"))
    private void tcards$takeSound(Level level, BlockPos pos, CallbackInfo ci) {
        TableSounds.play(level, pos, TableSounds.CHIP_CLINK, 0.55f);
    }
}
