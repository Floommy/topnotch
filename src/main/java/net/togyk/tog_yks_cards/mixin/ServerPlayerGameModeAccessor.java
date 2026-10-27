package net.togyk.tog_yks_cards.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={ServerPlayerGameMode.class})
public interface ServerPlayerGameModeAccessor {
    @Accessor(value="level")
    public ServerLevel getLevel();

    @Accessor(value="player")
    public ServerPlayer getPlayer();
}

