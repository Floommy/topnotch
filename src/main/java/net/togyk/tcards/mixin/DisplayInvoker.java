package net.togyk.tcards.mixin;

import com.mojang.math.Transformation;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Display entity setters are private in vanilla; used for the coin flip / card flourish animation. */
@Mixin(Display.class)
public interface DisplayInvoker {
    @Invoker("setTransformation")
    void tcards$setTransformation(Transformation transformation);

    @Invoker("setTransformationInterpolationDuration")
    void tcards$setInterpolationDuration(int ticks);

    @Invoker("setTransformationInterpolationDelay")
    void tcards$setInterpolationDelay(int ticks);

    @Invoker("setPosRotInterpolationDuration")
    void tcards$setPosRotInterpolationDuration(int ticks);

    @Invoker("setBillboardConstraints")
    void tcards$setBillboard(Display.BillboardConstraints constraints);

    @Invoker("setBrightnessOverride")
    void tcards$setBrightness(net.minecraft.util.Brightness brightness);

    @Invoker("setViewRange")
    void tcards$setViewRange(float range);
}
