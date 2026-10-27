package net.togyk.tcards.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix3f;
import org.joml.Vector3f;

/**
 * Bending of a 3D Skin Layers arm mesh at the elbow. The mesh lives in its own frame ("mesh space", blocks):
 * arm frame = scale(voxel, 1.035, voxel) * (mesh + (meshX/16, -0.1/16, 0)). The arm mesh is 12 px tall starting at
 * -2 px, so the elbow row line is at mesh y = 4 px; everything below it turns with the forearm around its pivot.
 */
@Environment(EnvType.CLIENT)
public final class ElbowBend {
    public static final float SPLIT_Y = 4f / 16f;            // mesh space: 6 pixel rows below the top (-2 px)
    private static final float HEIGHT_SCALE = 1.035f, MESH_Y = -0.1f;

    public final boolean bent;
    private final Matrix3f rot;
    private final float ex, ey, ez, sx, sy, tx, ty;

    public ElbowBend(ModelPart forearm, float meshX, float voxel) {
        bent = forearm != null && (forearm.xRot != 0 || forearm.yRot != 0 || forearm.zRot != 0);
        rot = bent ? new Matrix3f().rotationZYX(forearm.zRot, forearm.yRot, forearm.xRot) : null;
        ex = forearm == null ? 0 : forearm.x / 16f;
        ey = forearm == null ? 0 : forearm.y / 16f;
        ez = forearm == null ? 0 : forearm.z / 16f;
        sx = voxel;
        sy = HEIGHT_SCALE;
        tx = meshX / 16f;
        ty = MESH_Y / 16f;
    }

    /** Is a face with this centre (mesh space) and normal part of the forearm? Faces on the elbow line go with the side they face. */
    public static boolean lower(float centreY, float normalY) {
        return centreY - 0.02f * Math.signum(normalY) > SPLIT_Y;
    }

    /** Moves one mesh-space point with the forearm (in place). */
    public void point(Vector3f p) {
        if (!bent) return;
        float ax = sx * (p.x + tx) - ex, ay = sy * (p.y + ty) - ey, az = sx * p.z - ez;
        Vector3f q = rot.transform(new Vector3f(ax, ay, az));
        p.set((q.x + ex) / sx - tx, (q.y + ey) / sy - ty, (q.z + ez) / sx);
    }

    public void normal(Vector3f n) {
        if (!bent) return;
        // point map is S^-1 R S (S = voxel scale); normals use its inverse transpose S R S^-1
        n.set(n.x / sx, n.y / sy, n.z / sx);
        rot.transform(n);
        n.set(n.x * sx, n.y * sy, n.z * sx);
        n.normalize();
    }
}
