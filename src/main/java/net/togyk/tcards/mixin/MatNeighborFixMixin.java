package net.togyk.tcards.mixin;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.togyk.tog_yks_cards.block.custom.MatBlock;
import net.togyk.tog_yks_cards.mat_item.MatItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Bug fix in the original mod: removing a mat while a neighbour mat holds an item hanging over onto it
 * removed that item while iterating the item list -> ConcurrentModificationException -> server crash.
 */
@Mixin(MatBlock.class)
public class MatNeighborFixMixin {
    @Redirect(method = "neighborChanged", at = @At(value = "INVOKE",
            target = "Lnet/togyk/tog_yks_cards/block/custom/MatBlock;getMatItems(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Ljava/util/List;"))
    private List<MatItem> tcards$copy(MatBlock self, BlockGetter level, BlockPos pos) {
        return new ArrayList<>(self.getMatItems(level, pos));
    }
}
