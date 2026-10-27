package net.togyk.tcards.mixin;

import net.minecraft.world.item.ItemStack;
import net.togyk.tcards.ChipTower;
import net.togyk.tog_yks_cards.mat_item.CoinPileMatItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Coin piles on mats may grow up to 30 coins (was 15) and can fall over. */
@Mixin(CoinPileMatItem.class)
public class ChipTowerMatMixin {
    @ModifyConstant(method = "interact", constant = @Constant(intValue = 15))
    private int tcards$maxHeight(int original) {
        return ChipTower.MAX_HEIGHT;
    }

    @Inject(method = "addCoin", at = @At("TAIL"))
    private void tcards$maybeFall(ItemStack coin, CallbackInfo ci) {
        ChipTower.coinAdded((CoinPileMatItem) (Object) this);
    }
}
