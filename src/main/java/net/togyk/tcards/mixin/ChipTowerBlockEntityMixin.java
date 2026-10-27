package net.togyk.tcards.mixin;

import net.minecraft.world.item.ItemStack;
import net.togyk.tcards.ChipTower;
import net.togyk.tog_yks_cards.blockentity.entities.CoinPileBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CoinPileBlockEntity.class)
public class ChipTowerBlockEntityMixin {
    @Inject(method = "addCoin", at = @At("TAIL"))
    private void tcards$maybeFall(ItemStack coin, CallbackInfo ci) {
        ChipTower.coinAdded((CoinPileBlockEntity) (Object) this);
    }
}
