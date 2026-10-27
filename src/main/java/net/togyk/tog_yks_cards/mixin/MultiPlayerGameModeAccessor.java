package net.togyk.tog_yks_cards.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(value=EnvType.CLIENT)
@Mixin(value={MultiPlayerGameMode.class})
public interface MultiPlayerGameModeAccessor {
    @Accessor(value="minecraft")
    public Minecraft getMinecraft();
}

