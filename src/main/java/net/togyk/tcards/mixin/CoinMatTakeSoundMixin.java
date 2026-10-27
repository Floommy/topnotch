package net.togyk.tcards.mixin;

import net.togyk.tcards.TableSounds;
import net.togyk.tog_yks_cards.blockentity.entities.MatBlockEntity;
import net.togyk.tog_yks_cards.mat_item.CoinPileMatItem;
import net.togyk.tog_yks_cards.mat_item.MatItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Chip clink when a chip is taken from a pile on a mat. */
@Mixin(CoinPileMatItem.class)
public class CoinMatTakeSoundMixin {
    @Inject(method = "removeLastCoin", at = @At("HEAD"))
    private void tcards$takeSound(CallbackInfo ci) {
        MatBlockEntity mat = ((MatItem) (Object) this).getMat();
        if (mat != null) TableSounds.play(mat.getLevel(), mat.getBlockPos(), TableSounds.CHIP_CLINK, 0.55f);
    }
}
