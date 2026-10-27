package net.togyk.tcards.mixin;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.togyk.tcards.TableSounds;
import net.togyk.tog_yks_cards.blockentity.entities.MatBlockEntity;
import net.togyk.tog_yks_cards.mat_item.CardHolderMatItem;
import net.togyk.tog_yks_cards.mat_item.CardPileMatItem;
import net.togyk.tog_yks_cards.mat_item.CoinPileMatItem;
import net.togyk.tog_yks_cards.mat_item.MatItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Card snap + puff / chip clink when something is put onto a game mat. */
@Mixin(MatBlockEntity.class)
public class MatPlaceSoundMixin {
    @Inject(method = "addMatItem", at = @At("TAIL"))
    private void tcards$placeSound(MatItem item, CallbackInfo ci) {
        MatBlockEntity self = (MatBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null || level.isClientSide()) return;
        AABB box = item.getAbsoluteShape() != null ? item.getAbsoluteShape().bounds() : new AABB(self.getBlockPos());
        if (item instanceof CoinPileMatItem) {
            TableSounds.play(level, self.getBlockPos(), TableSounds.CHIP_CLINK, 0.7f);
        } else if (item instanceof CardPileMatItem || item instanceof CardHolderMatItem) {
            TableSounds.play(level, self.getBlockPos(), TableSounds.CARD_PLACE, 0.8f);
            if (item instanceof CardPileMatItem) TableSounds.puff(level, box.getCenter().x, box.maxY + 0.02, box.getCenter().z);
        }
    }
}
