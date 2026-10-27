package dev.tr7zw.skinlayers.api;
import net.minecraft.client.model.geom.ModelPart;
public interface MeshTransformerProvider {
    MeshTransformer prepareTransformer(ModelPart vanillaModel);
}
