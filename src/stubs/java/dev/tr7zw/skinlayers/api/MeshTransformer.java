package dev.tr7zw.skinlayers.api;
import org.joml.Vector3f;
import org.joml.Vector4f;
import net.minecraft.client.model.geom.ModelPart.Cube;
public interface MeshTransformer {
    void transform(Vector3f position, Vector4f[] vertexData);
    void transform(Cube cube);
}
