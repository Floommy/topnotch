package net.togyk.tcards.client;

import java.util.EnumSet;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The big flat box 3D Skin Layers uses for the front pixels of an arm ("fast render"). One 12 px box cannot bend,
 * so this box draws itself in two halves split at the elbow; the lower half turns with the forearm, and a hidden
 * 2 px band below the elbow closes the gap on the outside of a bent elbow.
 */
@Environment(EnvType.CLIENT)
public class BendableBox extends ModelPart.Cube {
    public interface QuadSink {
        void quad(Vector3f[] pos, float[] uv, Vector3f normal);
    }

    /** set by the mesh transformer right before every draw */
    public ElbowBend bend;
    private final float capU0, capU1, capV0, capV1;

    public BendableBox(int u, int v, float x, float y, float z, float w, float h, float d, boolean mirror, int texW, int texH) {
        super(u, v, x, y, z, w, h, d, 0f, 0f, 0f, mirror, texW, texH, EnumSet.allOf(Direction.class));
        // band end cap shows the front of the arm just below the elbow
        capU0 = (u + d) / (float) texW;
        capU1 = (u + d + w) / (float) texW;
        capV0 = (v + d + (ElbowBend.SPLIT_Y * 16f - y)) / (float) texH;
        capV1 = capV0 + d / (float) texH;
    }

    @Override
    public void compile(PoseStack.Pose pose, VertexConsumer builder, int light, int overlay, int color) {
        ElbowBend b = bend;
        bend = null;
        if (b == null || !b.bent) {
            super.compile(pose, builder, light, overlay, color);
            return;
        }
        Matrix4f m = pose.pose();
        emit(b, (pos, uv, normal) -> {
            Vector3f n = pose.transformNormal(normal, new Vector3f());
            for (int i = 0; i < 4; i++) {
                Vector3f p = m.transformPosition(pos[i], new Vector3f());
                builder.addVertex(p.x, p.y, p.z, color, uv[i * 2], uv[i * 2 + 1], overlay, light, n.x, n.y, n.z);
            }
        });
    }

    /** Quads in mesh space (blocks): side faces split at the elbow, ends whole, plus the joint band. */
    public void emit(ElbowBend b, QuadSink sink) {
        emitFaces(b, sink);
        emitCap(sink);
    }

    private void emitFaces(ElbowBend b, QuadSink sink) {
        for (ModelPart.Polygon poly : polygons) {
            ModelPart.Vertex[] v = poly.vertices();
            Vector3f normal = new Vector3f(poly.normal());
            float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
            for (ModelPart.Vertex vx : v) {
                minY = Math.min(minY, vx.worldY());
                maxY = Math.max(maxY, vx.worldY());
            }
            if (maxY - minY < 1e-5f) {                       // top or bottom face: whole
                Vector3f[] p = pos(v);
                float[] uv = uv(v);
                if (ElbowBend.lower(minY, normal.y())) bendQuad(b, p, normal);
                sink.quad(p, uv, normal);
                continue;
            }
            float split = ElbowBend.SPLIT_Y;
            if (split <= minY || split >= maxY) {           // does not cross the elbow
                Vector3f[] p = pos(v);
                if (ElbowBend.lower((minY + maxY) / 2f, normal.y())) bendQuad(b, p, normal);
                sink.quad(p, uv(v), normal);
                continue;
            }
            // side face: cut at the elbow (vertices of a side face are two at minY and two at maxY)
            Vector3f[] upper = new Vector3f[4], lower = new Vector3f[4];
            float[] uvUp = new float[8], uvLo = new float[8];
            for (int i = 0; i < 4; i++) {
                ModelPart.Vertex a = v[i];
                ModelPart.Vertex partner = null;           // the vertex straight above/below it on the same edge
                for (ModelPart.Vertex o : v) {
                    if (o != a && Math.abs(o.worldX() - a.worldX()) < 1e-5f && Math.abs(o.worldZ() - a.worldZ()) < 1e-5f) partner = o;
                }
                if (partner == null) partner = a;
                ModelPart.Vertex top = a.worldY() < partner.worldY() ? a : partner, bot = top == a ? partner : a;
                float f = (split - top.worldY()) / (bot.worldY() - top.worldY());
                Vector3f cut = new Vector3f(a.worldX(), split, a.worldZ());
                float cu = top.u() + (bot.u() - top.u()) * f, cv = top.v() + (bot.v() - top.v()) * f;
                boolean isTop = a == top;
                upper[i] = isTop ? new Vector3f(a.worldX(), a.worldY(), a.worldZ()) : new Vector3f(cut);
                lower[i] = isTop ? new Vector3f(cut) : new Vector3f(a.worldX(), a.worldY(), a.worldZ());
                uvUp[i * 2] = isTop ? a.u() : cu;
                uvUp[i * 2 + 1] = isTop ? a.v() : cv;
                uvLo[i * 2] = isTop ? cu : a.u();
                uvLo[i * 2 + 1] = isTop ? cv : a.v();
            }
            sink.quad(upper, uvUp, normal);
            // joint band: the 2 px below the elbow, a bit inside the box, staying with the upper arm
            Vector3f[] band = new Vector3f[4];
            float[] uvBand = new float[8];
            float cx = (minX + maxX) / 32f, cz = (minZ + maxZ) / 32f;
            float bandBottom = split + 2f / 16f;
            for (int i = 0; i < 4; i++) {
                Vector3f lp = lower[i];
                boolean atSplit = Math.abs(lp.y - split) < 1e-5f;
                float y = atSplit ? split : bandBottom;
                float g = (y - split) / (maxY - split);       // uv along the lower half
                float u0 = uvLo[i * 2], v0 = uvLo[i * 2 + 1];
                // partner uv at the split for interpolation
                int j = partnerIndex(lower, i);
                float us = atSplit ? u0 : uvLo[j * 2], vs = atSplit ? v0 : uvLo[j * 2 + 1];
                float ue = atSplit ? uvLo[j * 2] : u0, ve = atSplit ? uvLo[j * 2 + 1] : v0;
                band[i] = new Vector3f(cx + (lp.x - cx) * 0.97f, y, cz + (lp.z - cz) * 0.97f);
                uvBand[i * 2] = us + (ue - us) * g;
                uvBand[i * 2 + 1] = vs + (ve - vs) * g;
            }
            sink.quad(band, uvBand, normal);
            bandCorners(band);
            Vector3f n2 = new Vector3f(normal);
            bendQuad(b, lower, n2);
            sink.quad(lower, uvLo, n2);
        }
    }

    private float bx0 = Float.MAX_VALUE, bx1 = -Float.MAX_VALUE, bz0 = Float.MAX_VALUE, bz1 = -Float.MAX_VALUE, by = 0;

    private void bandCorners(Vector3f[] band) {
        for (Vector3f p : band) {
            if (p.y > ElbowBend.SPLIT_Y + 1e-5f) {
                bx0 = Math.min(bx0, p.x);
                bx1 = Math.max(bx1, p.x);
                bz0 = Math.min(bz0, p.z);
                bz1 = Math.max(bz1, p.z);
                by = p.y;
            }
        }
    }

    private void emitCap(QuadSink sink) {
        if (bx0 > bx1) return;
        // facing down the arm (+y in model space), closes the band tube
        Vector3f[] p = {new Vector3f(bx1, by, bz0), new Vector3f(bx0, by, bz0), new Vector3f(bx0, by, bz1), new Vector3f(bx1, by, bz1)};
        float[] uv = {capU1, capV0, capU0, capV0, capU0, capV1, capU1, capV1};
        sink.quad(p, uv, new Vector3f(0, 1, 0));
        bx0 = bz0 = Float.MAX_VALUE;
        bx1 = bz1 = -Float.MAX_VALUE;
    }

    private static int partnerIndex(Vector3f[] q, int i) {
        for (int j = 0; j < 4; j++) {
            if (j != i && Math.abs(q[j].x - q[i].x) < 1e-5f && Math.abs(q[j].z - q[i].z) < 1e-5f) return j;
        }
        return i;
    }

    private static void bendQuad(ElbowBend b, Vector3f[] p, Vector3f normal) {
        for (Vector3f v : p) b.point(v);
        b.normal(normal);
    }

    private static Vector3f[] pos(ModelPart.Vertex[] v) {
        Vector3f[] p = new Vector3f[4];
        for (int i = 0; i < 4; i++) p[i] = new Vector3f(v[i].worldX(), v[i].worldY(), v[i].worldZ());
        return p;
    }

    private static float[] uv(ModelPart.Vertex[] v) {
        float[] uv = new float[8];
        for (int i = 0; i < 4; i++) {
            uv[i * 2] = v[i].u();
            uv[i * 2 + 1] = v[i].v();
        }
        return uv;
    }
}
