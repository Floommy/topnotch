package net.togyk.tcards.mixin;

import java.util.Set;

import net.minecraft.client.model.geom.builders.CubeDefinition;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.core.Direction;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CubeDefinition.class)
public interface CubeDefinitionAccessor {
    @Accessor("origin") Vector3fc tcards$origin();
    @Accessor("dimensions") Vector3fc tcards$dimensions();
    @Accessor("grow") CubeDeformation tcards$grow();
    @Accessor("mirror") boolean tcards$mirror();
    @Accessor("texCoord") UVPair tcards$texCoord();
    @Accessor("texScale") UVPair tcards$texScale();
    @Accessor("visibleFaces") Set<Direction> tcards$visibleFaces();
}
