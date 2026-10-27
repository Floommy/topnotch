package net.togyk.tcards.mixin;

import java.util.List;

import net.minecraft.client.model.geom.builders.CubeDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PartDefinition.class)
public interface PartDefinitionAccessor {
    @Accessor("cubes")
    List<CubeDefinition> tcards$cubes();

    @Mutable
    @Accessor("cubes")
    void tcards$setCubes(List<CubeDefinition> cubes);
}
