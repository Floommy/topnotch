package net.togyk.tcards.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.togyk.tcards.TableSounds;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Replaces the original bundle sounds with card / chip sounds. */
@Mixin(net.togyk.tog_yks_cards.mat_item.CoinPileMatItem.class)
public class SoundCoinPileMatItemMixin {
    @Redirect(method = "interact", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"))
    private void tcards$sound(Level level, Entity entity, BlockPos pos, SoundEvent sound, SoundSource source, float volume, float pitch) {
        TableSounds.replace(level, pos, sound, true);
    }
}
