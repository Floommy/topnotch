package net.togyk.tcards.mixin;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.togyk.tcards.TableMain;
import net.togyk.tog_yks_cards.item.ModItemTabs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The TopNotch tab shows the TopNotch chip instead of a card box. */
@Mixin(CreativeModeTab.class)
public class TabIconMixin {
    @Inject(method = "getIconItem", at = @At("HEAD"), cancellable = true)
    private void tcards$logo(CallbackInfoReturnable<ItemStack> cir) {
        if ((Object) this == ModItemTabs.EXAMPLE_TAB && TableMain.LOGO != null) cir.setReturnValue(new ItemStack(TableMain.LOGO));
    }
}
