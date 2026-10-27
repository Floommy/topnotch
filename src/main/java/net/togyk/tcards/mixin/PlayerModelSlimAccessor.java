package net.togyk.tcards.mixin;

import net.minecraft.client.model.player.PlayerModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerModel.class)
public interface PlayerModelSlimAccessor {
    @Accessor("slim")
    boolean tcards$slim();
}
