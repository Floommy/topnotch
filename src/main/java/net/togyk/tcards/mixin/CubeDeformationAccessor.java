package net.togyk.tcards.mixin;

import net.minecraft.client.model.geom.builders.CubeDeformation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CubeDeformation.class)
public interface CubeDeformationAccessor {
    @Accessor("growX") float tcards$growX();
    @Accessor("growY") float tcards$growY();
    @Accessor("growZ") float tcards$growZ();
}
