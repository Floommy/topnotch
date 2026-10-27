package tcardstest;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/** Elbow split: unbent arm must look exactly like the vanilla arm (same surface, same texture). Run only in the elbow test build. */
public class ElbowTests {
    static void check(String n, boolean ok, Object info) {
        TestMain.check(n, ok, info);
    }

    record Sample(Vector3f pos, Vector3f normal, float u, float v) {}

    static boolean skipSleeves = false;

    static List<Sample> surface(ModelPart part) {
        List<Sample> out = new ArrayList<>();
        part.visit(new PoseStack(), (pose, path, index, cube) -> {
            if (skipSleeves && path.contains("sleeve")) return;
            if (!path.contains("tcards_forearm") && index >= 1) return;   // joint filler on the upper arm: hidden inside while straight
            Matrix4f m = pose.pose();
            for (ModelPart.Polygon poly : cube.polygons) {
                ModelPart.Vertex[] v = poly.vertices();
                if (v.length != 4) continue;
                Vector3f n = new Vector3f(poly.normal());
                pose.normal().transform(n);
                for (int i = 1; i < 8; i++) for (int j = 1; j < 8; j++) {
                    float a = i / 8f, b = j / 8f;
                    // bilinear over the quad v0 v1 v2 v3
                    float x = lerp2(v[0].x(), v[1].x(), v[2].x(), v[3].x(), a, b);
                    float y = lerp2(v[0].y(), v[1].y(), v[2].y(), v[3].y(), a, b);
                    float z = lerp2(v[0].z(), v[1].z(), v[2].z(), v[3].z(), a, b);
                    float uu = lerp2(v[0].u(), v[1].u(), v[2].u(), v[3].u(), a, b);
                    float vv = lerp2(v[0].v(), v[1].v(), v[2].v(), v[3].v(), a, b);
                    Vector4f p = m.transform(new Vector4f(x / 16f, y / 16f, z / 16f, 1f));
                    out.add(new Sample(new Vector3f(p.x * 16, p.y * 16, p.z * 16), n, uu, vv));
                }
            }
        });
        return out;
    }

    static float lerp2(float p0, float p1, float p2, float p3, float a, float b) {
        float top = p0 + (p1 - p0) * a, bottom = p3 + (p2 - p3) * a;
        return top + (bottom - top) * b;
    }

    /** every vanilla sample point must be covered by a split polygon with the same normal and texture coordinate */
    static int mismatches(List<Sample> vanilla, List<ModelPart> splitParts, float posTol) {
        List<Sample> split = new ArrayList<>();
        for (ModelPart p : splitParts) split.addAll(surface(p));
        int bad = 0;
        for (Sample s : vanilla) {
            boolean found = false;
            // project the sample onto the split surface: same normal, same plane (within tol), uv must match there
            for (ModelPart p : splitParts) {
                if (covered(p, s, posTol)) { found = true; break; }
            }
            if (!found) {
                if (bad < 4) System.out.println("TCARDS-DEBUG miss pos=" + s.pos + " n=" + s.normal + " uv=" + s.u * 64 + "," + s.v * 64);
                bad++;
            }
        }
        return bad;
    }

    static boolean covered(ModelPart part, Sample s, float tol) {
        boolean[] hit = {false};
        part.visit(new PoseStack(), (pose, path, index, cube) -> {
            if (hit[0]) return;
            if (skipSleeves && path.contains("sleeve")) return;
            Matrix4f m = pose.pose();
            for (ModelPart.Polygon poly : cube.polygons) {
                Vector3f n = new Vector3f(poly.normal());
                pose.normal().transform(n);
                if (n.dot(s.normal) < 0.99f) continue;
                ModelPart.Vertex[] v = poly.vertices();
                Vector3f[] w = new Vector3f[4];
                for (int i = 0; i < 4; i++) {
                    Vector4f p = m.transform(new Vector4f(v[i].x() / 16f, v[i].y() / 16f, v[i].z() / 16f, 1f));
                    w[i] = new Vector3f(p.x * 16, p.y * 16, p.z * 16);
                }
                // plane distance
                if (Math.abs(new Vector3f(s.pos).sub(w[0]).dot(n)) > tol) continue;
                // param in quad via axes e1 = w1-w0, e2 = w3-w0 (rectangles)
                Vector3f e1 = new Vector3f(w[1]).sub(w[0]), e2 = new Vector3f(w[3]).sub(w[0]);
                Vector3f d = new Vector3f(s.pos).sub(w[0]);
                float a = d.dot(e1) / e1.lengthSquared(), b = d.dot(e2) / e2.lengthSquared();
                if (a < -1e-3 || a > 1.001 || b < -1e-3 || b > 1.001) continue;
                float uu = v[0].u() + (v[1].u() - v[0].u()) * a + (v[3].u() - v[0].u()) * b;
                float vv = v[0].v() + (v[1].v() - v[0].v()) * a + (v[3].v() - v[0].v()) * b;
                if (Math.abs(uu - s.u) < 0.004f && Math.abs(vv - s.v) < 0.004f) { hit[0] = true; return; }
            }
        });
        return hit[0];
    }

    static void run() {
        for (boolean slim : new boolean[]{false, true}) {
            String kind = slim ? "slim" : "wide";
            ModelPart root = LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, slim), 64, 64).bakeRoot();
            ModelPart arm = root.getChild("right_arm");
            check(kind + ": forearm exists", arm.hasChild("tcards_forearm"), "");
            check(kind + ": lower sleeve exists", arm.hasChild("tcards_forearm") && arm.getChild("tcards_forearm").hasChild("right_sleeve_tcards"), "");
            check(kind + ": left forearm exists", root.getChild("left_arm").hasChild("tcards_forearm"), "");
            try {
                new PlayerModel(root, slim);
                check(kind + ": player model builds", true, "");
            } catch (Throwable t) {
                check(kind + ": player model builds", false, t);
            }

            // vanilla reference arm (skin only) built by hand like PlayerModel/HumanoidModel do
            MeshDefinition ref = new MeshDefinition();
            float w = slim ? 3 : 4, x0 = slim ? -2 : -3;
            ref.getRoot().addOrReplaceChild("arm", CubeListBuilder.create().texOffs(40, 16).addBox(x0, -2, -2, w, 12, 4), PartPose.offset(-5, 2, 0));
            ModelPart refArm = LayerDefinition.create(ref, 64, 64).bakeRoot().getChild("arm");
            // split arm without sleeves
            ModelPart sleeve = arm.getChild("right_sleeve");
            sleeve.visible = false;
            ModelPart lowerSleeve = arm.getChild("tcards_forearm").getChild("right_sleeve_tcards");
            lowerSleeve.visible = false;
            skipSleeves = true;
            int bad = mismatches(surface(refArm), List.of(arm), 0.01f);
            check(kind + ": straight arm looks exactly like vanilla (skin)", bad == 0, bad + " of " + surface(refArm).size() + " samples differ");
            int extra = mismatches(surface(arm), List.of(refArm), 0.01f);
            skipSleeves = false;
            check(kind + ": no extra faces on the arm", extra == 0, extra);

            MeshDefinition refS = new MeshDefinition();
            refS.getRoot().addOrReplaceChild("arm", CubeListBuilder.create().texOffs(40, 32).addBox(x0, -2, -2, w, 12, 4, new CubeDeformation(0.25f)), PartPose.offset(-5, 2, 0));
            ModelPart refSleeve = LayerDefinition.create(refS, 64, 64).bakeRoot().getChild("arm");
            sleeve.visible = true;
            lowerSleeve.visible = true;
            ModelPart skinOff = arm;   // visit includes skin cubes too; covered() needs any matching face
            int badS = mismatches(surface(refSleeve), List.of(skinOff), 0.06f);
            check(kind + ": straight sleeve looks like vanilla", badS == 0, badS + " of " + surface(refSleeve).size());

            // bent elbow: item in hand follows the forearm
            PlayerModel model = new PlayerModel(root, slim);
            AvatarRenderState state = new AvatarRenderState();
            model.setupAnim(state);
            PoseStack straight = new PoseStack();
            model.translateToHand(state, HumanoidArm.RIGHT, straight);
            ModelPart fore = model.rightArm.getChild("tcards_forearm");
            fore.xRot = (float) (-Math.PI / 2);
            PoseStack bent = new PoseStack();
            model.translateToHand(state, HumanoidArm.RIGHT, bent);
            Vector4f handStraight = straight.last().pose().transform(new Vector4f(-1 / 16f, 10 / 16f, 0, 1));
            Vector4f handBent = bent.last().pose().transform(new Vector4f(-1 / 16f, 10 / 16f, 0, 1));
            // elbow at arm y=4: bent 90 deg forward -> hand 6px in front of the elbow at elbow height
            check(kind + ": held item follows bent forearm", Math.abs(handBent.y * 16 - (handStraight.y * 16 - 6)) < 0.1 && Math.abs(handBent.z * 16 - (handStraight.z * 16 - 6)) < 0.1,
                    "straight=" + handStraight + " bent=" + handBent);
            model.setupAnim(state);
            check(kind + ": next frame straightens the elbow", fore.xRot == 0f, fore.xRot);
        }
        // armour: chest piece gets elbows, other pieces stay without arms
        var set = PlayerModel.createArmorMeshSet(new CubeDeformation(0.5f), new CubeDeformation(1.0f));
        ModelPart chest = LayerDefinition.create(set.chest(), 64, 32).bakeRoot();
        check("chest armour arms bend too", chest.getChild("right_arm").hasChild("tcards_forearm") && chest.getChild("left_arm").hasChild("tcards_forearm"), "");
        ModelPart legs = LayerDefinition.create(set.legs(), 64, 32).bakeRoot();
        check("legging model gets no arm parts", !legs.getChild("right_arm").hasChild("tcards_forearm"), "");
        MeshDefinition refA = new MeshDefinition();
        refA.getRoot().addOrReplaceChild("arm", CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1, -2, -2, 4, 12, 4, new CubeDeformation(1.0f)), PartPose.offset(5, 2, 0));
        int badA = mismatches(surface(LayerDefinition.create(refA, 64, 32).bakeRoot().getChild("arm")), List.of(chest.getChild("left_arm")), 0.06f);
        check("straight armour arm (mirrored) looks like vanilla", badA == 0, badA);
    }
}
