package net.togyk.tcards.mixin;

import net.minecraft.client.Minecraft;
import net.togyk.tcards.client.TableClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftPickBlockMixin {
    @Inject(method = "pickBlockOrEntity", at = @At("HEAD"), cancellable = true)
    private void tcards$suppressPickOnTable(CallbackInfo ci) {
        if (TableClient.shouldSuppressPickBlock((Minecraft) (Object) this)) {
            ci.cancel();
        }
    }
}
