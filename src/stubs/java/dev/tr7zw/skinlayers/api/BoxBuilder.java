package dev.tr7zw.skinlayers.api;
import net.minecraft.client.model.geom.ModelPart.Cube;
public interface BoxBuilder {
    Cube build(BoxDefinition box);
    record BoxDefinition(int u, int v, boolean mirror, float x, float y, float z, float width, float height,
            float depth, int textureWidth, int textureHeight) {
    }
}
