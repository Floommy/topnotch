package net.togyk.tcards.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.TextDisplay.class)
public interface TextDisplayInvoker {
    @Invoker("setText")
    void tcards$setText(Component text);

    @Invoker("setLineWidth")
    void tcards$setLineWidth(int width);

    @Invoker("setBackgroundColor")
    void tcards$setBackgroundColor(int argb);

    @Invoker("setFlags")
    void tcards$setFlags(byte flags);
}
