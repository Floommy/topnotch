package net.togyk.tcards.client;

import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * "3D Skin Layers" (tr7zw) draws the outer skin layer of each arm as a voxel mesh hung on the sleeve part:
 * edge voxels as single faces plus (with its default "fast render") one big flat box for the front pixels.
 * Through its official API (made for bendy-lib) we bend both at the elbow exactly like the forearm, and hide our
 * flat lower sleeve while the 3D sleeve is drawn.
 */
@Environment(EnvType.CLIENT)
public final class SkinLayersCompat {
    private SkinLayersCompat() {}

    static boolean active;
    /** upper sleeve part -> [forearm part, x offset of the 3D mesh in pixels] */
    static final Map<ModelPart, Object[]> SLEEVES = new WeakHashMap<>();

    public static void init() {
        if (!FabricLoader.getInstance().isModLoaded("skinlayers3d")) return;
        try {
            Hook.install();
            active = true;
            org.slf4j.LoggerFactory.getLogger("tog_yks-cards").info("3D Skin Layers found: 3D sleeves bend with the elbows");
        } catch (Throwable t) {
            org.slf4j.LoggerFactory.getLogger("tog_yks-cards").warn("3D Skin Layers elbow support disabled: {}", t.toString());
        }
    }

    static void remember(ModelPart upperSleeve, ModelPart forearm, boolean right, boolean slim) {
        if (!active) return;
        SLEEVES.put(upperSleeve, new Object[]{forearm, meshX(right, slim)});
    }

    static float meshX(boolean right, boolean slim) {
        return (slim ? 0.499f : 0.998f) * (right ? -1 : 1);   // 3D Skin Layers' OffsetProvider for arms
    }

    static boolean hasMesh(ModelPart part) {
        if (!active) return false;
        try {
            return Hook.hasMesh(part);
        } catch (Throwable t) {
            active = false;
            return false;
        }
    }

    static float voxelSize() {
        try {
            Field config = Class.forName("dev.tr7zw.skinlayers.SkinLayersModBase").getField("config");
            Object c = config.get(null);
            return c.getClass().getField("baseVoxelSize").getFloat(c);
        } catch (Throwable t) {
            return 1.15f;
        }
    }

    /** Bends one voxel face (mesh space) like the transformer does in game. */
    public static void bendFace(ElbowBend bend, Vector3f normal, Vector4f[] v) {
        if (!bend.bent) return;
        float cy = 0;
        for (Vector4f p : v) cy += p.y();
        if (!ElbowBend.lower(cy / v.length, normal.y())) return;
        Vector3f q = new Vector3f();
        for (Vector4f p : v) {
            q.set(p.x(), p.y(), p.z());
            bend.point(q);
            p.set(q.x, q.y, q.z, p.w());
        }
        bend.normal(normal);
    }

    /** Render tests: same as in game for one face. */
    public static void bendForTest(ModelPart forearm, boolean right, boolean slim, float voxel, Vector3f normal, Vector4f[] verts) {
        bendFace(new ElbowBend(forearm, meshX(right, slim), voxel), normal, verts);
    }

    /** Only loaded when the mod is there (references its API). */
    private static final class Hook {
        private static float voxel = 1.15f;
        private static long voxelRead;

        static void install() {
            dev.tr7zw.skinlayers.api.MeshTransformerProvider previous = dev.tr7zw.skinlayers.api.SkinLayersAPI.getMeshTransformerProvider();
            dev.tr7zw.skinlayers.api.SkinLayersAPI.setupMeshTransformerProvider(part -> {
                Object[] info = part == null ? null : SLEEVES.get(part);
                if (info != null) {
                    // always ours for known sleeves: also puts reused cubes back when the arm is straight again
                    return new Transformer(new ElbowBend((ModelPart) info[0], (Float) info[1], voxel()));
                }
                return previous != null ? previous.prepareTransformer(part) : Transformer.NONE;
            });
            dev.tr7zw.skinlayers.api.BoxBuilder previousBoxes = dev.tr7zw.skinlayers.api.SkinLayersAPI.getBoxBuilder();
            dev.tr7zw.skinlayers.api.SkinLayersAPI.setupBoxBuilder(box -> {
                boolean arm = Math.abs(box.height() - 12f) < 0.01f && box.y() < -1.9f && box.y() > -2.1f
                        && ((box.u() == 40 && box.v() == 32) || (box.u() == 48 && box.v() == 48));
                if (arm) {
                    return new BendableBox(box.u(), box.v(), box.x(), box.y(), box.z(), box.width(), box.height(), box.depth(),
                            box.mirror(), box.textureWidth(), box.textureHeight());
                }
                return previousBoxes.build(box);
            });
        }

        static float voxel() {
            long now = System.currentTimeMillis();
            if (now - voxelRead > 2000) {     // the setting can change in its menu
                voxelRead = now;
                voxel = voxelSize();
            }
            return voxel;
        }

        static boolean hasMesh(ModelPart part) {
            return ((dev.tr7zw.skinlayers.accessor.ModelPartInjector) (Object) part).getInjectedMesh() != null;
        }
    }

    private static final class Transformer implements dev.tr7zw.skinlayers.api.MeshTransformer {
        static final Transformer NONE = new Transformer(null);
        /** original faces of reused plain cubes (Iris compatibility mode draws every voxel face as a cube) */
        private static final Map<ModelPart.Cube, ModelPart.Polygon[]> ORIGINALS = new IdentityHashMap<>();
        private final ElbowBend bend;

        Transformer(ElbowBend bend) {
            this.bend = bend;
        }

        @Override
        public void transform(Vector3f normal, Vector4f[] vertexData) {
            if (bend != null) bendFace(bend, normal, vertexData);
        }

        @Override
        public void transform(ModelPart.Cube cube) {
            if (bend == null) return;
            if (cube instanceof BendableBox box) {
                box.bend = bend;
                return;
            }
            // plain cube (Iris mode): put the original faces back, then swap in bent copies of faces below the elbow
            ModelPart.Polygon[] orig = ORIGINALS.get(cube);
            if (orig == null) {
                if (!bend.bent) return;
                orig = cube.polygons.clone();
                if (ORIGINALS.size() > 20000) ORIGINALS.clear();
                ORIGINALS.put(cube, orig);
            }
            System.arraycopy(orig, 0, cube.polygons, 0, orig.length);
            if (!bend.bent) return;
            for (int i = 0; i < cube.polygons.length; i++) {
                ModelPart.Polygon poly = cube.polygons[i];
                ModelPart.Vertex[] v = poly.vertices();
                float cy = 0;
                for (ModelPart.Vertex x : v) cy += x.worldY();
                if (!ElbowBend.lower(cy / v.length, poly.normal().y())) continue;
                ModelPart.Vertex[] nv = new ModelPart.Vertex[v.length];
                for (int k = 0; k < v.length; k++) {
                    Vector3f p = new Vector3f(v[k].worldX(), v[k].worldY(), v[k].worldZ());
                    bend.point(p);
                    nv[k] = new ModelPart.Vertex(p.x * 16f, p.y * 16f, p.z * 16f, v[k].u(), v[k].v());
                }
                Vector3f n = new Vector3f(poly.normal());
                bend.normal(n);
                cube.polygons[i] = new ModelPart.Polygon(nv, n);
            }
        }
    }
}
