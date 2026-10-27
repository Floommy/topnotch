package net.togyk.tcards.mixin;

import net.togyk.tcards.ChipTower;
import net.togyk.tog_yks_cards.block.custom.CoinPileblock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Ground coin piles may grow up to 30 coins (was 16). */
@Mixin(CoinPileblock.class)
public class ChipTowerBlockMixin {
    @ModifyConstant(method = "useItemOn", constant = @Constant(intValue = 16))
    private int tcards$maxHeight(int original) {
        return ChipTower.MAX_HEIGHT;
    }
}
