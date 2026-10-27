package net.togyk.tcards.client;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDefinition;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.core.Direction;
import net.togyk.tcards.mixin.CubeDefinitionAccessor;
import net.togyk.tcards.mixin.PartDefinitionAccessor;
import org.joml.Vector3fc;

/**
 * Splits the player's arms (skin, sleeves and chest armour) at the elbow: the upper arm keeps its name and pose,
 * the lower arm becomes a child part "tcards_forearm" that can bend. Unbent it looks exactly like before
 * (same texture rows, the hand end keeps its own texture).
 */
@Environment(EnvType.CLIENT)
public final class Elbows {
    private Elbows() {}

    public static final String FOREARM = "tcards_forearm";
    public static final String SUFFIX = "_tcards";
    private static final float ELBOW = 6f;   // upper arm length in pixels (arm is 12 long)
    private static final float FILLER = 2f;  // joint filler length (= half the arm depth, covers a 90 degree bend)

    private static final Set<Direction> SIDES = EnumSet.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST);
    private static final Set<Direction> NOT_ELBOW_END = EnumSet.complementOf(EnumSet.of(Direction.UP)); // UP = hand end (max y)
    private static final Set<Direction> HAND_END = EnumSet.of(Direction.UP);

    public static void splitArms(MeshDefinition mesh) {
        PartDefinition root = mesh.getRoot();
        for (String arm : new String[]{"right_arm", "left_arm"}) {
            PartDefinition part = child(root, arm);
            if (part != null) split(part);
        }
    }

    private static PartDefinition child(PartDefinition parent, String name) {
        for (Map.Entry<String, PartDefinition> e : parent.getChildren()) if (e.getKey().equals(name)) return e.getValue();
        return null;
    }

    private static void split(PartDefinition arm) {
        CubeDefinition cube = single(arm);
        if (cube == null || child(arm, FOREARM) != null) return;
        CubeDefinitionAccessor c = (CubeDefinitionAccessor) (Object) cube;
        float top = c.tcards$origin().y();
        if (Math.abs(c.tcards$dimensions().y() - 12f) > 0.01f) return;   // not a normal arm
        float elbowY = top + ELBOW;
        // the elbow turns around the middle of the arm (not the shoulder pivot line), so sideways bends stay round
        float centreX = c.tcards$origin().x() + c.tcards$dimensions().x() / 2f;

        PartDefinition forearm = arm.addOrReplaceChild(FOREARM, CubeListBuilder.create(), PartPose.offset(centreX, elbowY, 0));
        CubeDefinition[] armSplit = cut(cube, top, centreX);
        ((PartDefinitionAccessor) arm).tcards$setCubes(List.of(armSplit[0], armSplit[3], armSplit[4]));
        ((PartDefinitionAccessor) forearm).tcards$setCubes(List.of(armSplit[1], armSplit[2]));

        // sleeves (children of the arm with one 12-long cube): upper half stays, lower half moves to the forearm
        for (Map.Entry<String, PartDefinition> e : new ArrayList<>(arm.getChildren())) {
            if (e.getKey().equals(FOREARM)) continue;
            CubeDefinition sleeve = single(e.getValue());
            if (sleeve == null) continue;
            CubeDefinitionAccessor s = (CubeDefinitionAccessor) (Object) sleeve;
            if (Math.abs(s.tcards$dimensions().y() - 12f) > 0.01f) continue;
            CubeDefinition[] sleeveSplit = cut(sleeve, top, centreX);
            ((PartDefinitionAccessor) e.getValue()).tcards$setCubes(List.of(sleeveSplit[0], sleeveSplit[3], sleeveSplit[4]));
            PartDefinition lower = forearm.addOrReplaceChild(e.getKey() + SUFFIX, CubeListBuilder.create(), PartPose.ZERO);
            ((PartDefinitionAccessor) lower).tcards$setCubes(List.of(sleeveSplit[1], sleeveSplit[2]));
        }
    }

    private static CubeDefinition single(PartDefinition part) {
        List<CubeDefinition> cubes = ((PartDefinitionAccessor) part).tcards$cubes();
        return cubes != null && cubes.size() == 1 ? cubes.get(0) : null;
    }

    /**
     * [upper part in arm space, lower part in forearm space, hand-end cap in forearm space].
     * Inflated cubes (sleeves, armour) stretch their texture over the grown size; the halves are grown only on the
     * outer ends (half the growth, shifted) so every texture row lands exactly where it was on the whole arm.
     */
    private static CubeDefinition[] cut(CubeDefinition cube, float armTop, float centreX) {
        CubeDefinitionAccessor c = (CubeDefinitionAccessor) (Object) cube;
        Vector3fc o = c.tcards$origin(), d = c.tcards$dimensions();
        net.togyk.tcards.mixin.CubeDeformationAccessor g = (net.togyk.tcards.mixin.CubeDeformationAccessor) (Object) c.tcards$grow();
        float gx = g.tcards$growX(), gy = g.tcards$growY(), gz = g.tcards$growZ();
        CubeDeformation half = new CubeDeformation(gx, gy / 2f, gz);
        float u = c.tcards$texCoord().u(), v = c.tcards$texCoord().v();
        float su = c.tcards$texScale().u(), sv = c.tcards$texScale().v();
        boolean mirror = c.tcards$mirror();
        float lowerH = d.y() - ELBOW;
        // arm space: from (top - gy) to the elbow
        CubeDefinition upper = new CubeDefinition(null, u, v, o.x(), o.y() - gy / 2f, o.z(), d.x(), ELBOW, d.z(), half, mirror, su, sv, NOT_ELBOW_END);
        // forearm space (0 = elbow): from the elbow to (lowerH + gy)
        float fx = o.x() - centreX;   // forearm space x
        CubeDefinition lower = new CubeDefinition(null, u, v + ELBOW, fx, gy / 2f, o.z(), d.x(), lowerH, d.z(), half, mirror, su, sv, SIDES);
        CubeDefinition cap = new CubeDefinition(null, u, v, fx, lowerH + gy / 2f, o.z(), d.x(), 0f, d.z(), half, mirror, su, sv, HAND_END);
        // joint filler on the upper arm: the 2 rows below the elbow, a hair thinner and hidden inside the forearm while
        // straight. It stays with the upper arm, so when the elbow bends it fills the gap on the outside of the joint
        // and never sticks out above the elbow.
        CubeDeformation thinner = new CubeDeformation(gx - 0.03f, 0f, gz - 0.03f);
        CubeDefinition filler = new CubeDefinition(null, u, v + ELBOW, o.x(), o.y() + ELBOW, o.z(), d.x(), FILLER, d.z(), thinner, mirror, su, sv, SIDES);
        // end cap of the band: closes the open tube you would look into on the inside of a bent elbow.
        // Its texture is the front of the arm just below the elbow (UP face region u+d+w -> front face at u+d).
        // outer layers sit a hair further out than the skin cap, so they win where both are seen
        float capY = o.y() + ELBOW + FILLER + (gy > 0.001f ? 0.03f : 0f);
        CubeDefinition fillerCap = new CubeDefinition(null, u - d.x(), v + d.z() + ELBOW, o.x(), capY, o.z(),
                d.x(), 0f, d.z(), thinner, mirror, su, sv, HAND_END);
        return new CubeDefinition[]{upper, lower, cap, filler, fillerCap};
    }

    // ---------------------------------------------------------------- runtime

    public static ModelPart forearm(ModelPart arm) {
        return arm.hasChild(FOREARM) ? arm.getChild(FOREARM) : null;
    }

    /** Straighten both elbows (start of every pose / first-person hand). */
    public static void reset(ModelPart arm) {
        ModelPart f = forearm(arm);
        if (f != null) f.resetPose();
    }

    /** The lower sleeve shows exactly when the upper sleeve does (and not when 3D Skin Layers draws the sleeve). */
    public static void syncSleeve(ModelPart arm, String sleeveName, boolean right, boolean slim) {
        ModelPart f = forearm(arm);
        if (f == null || !arm.hasChild(sleeveName) || !f.hasChild(sleeveName + SUFFIX)) return;
        ModelPart upper = arm.getChild(sleeveName);
        SkinLayersCompat.remember(upper, f, right, slim);
        f.getChild(sleeveName + SUFFIX).visible = upper.visible && !SkinLayersCompat.hasMesh(upper);
    }

    public static void setSleeve(ModelPart arm, String sleeveName, boolean visible) {
        ModelPart f = forearm(arm);
        if (f != null && f.hasChild(sleeveName + SUFFIX)) f.getChild(sleeveName + SUFFIX).visible = visible;
    }

    /** Held items follow the bent forearm. Call after the arm transform is on the stack. */
    public static void toHand(ModelPart arm, PoseStack pose) {
        ModelPart f = forearm(arm);
        if (f == null || (f.xRot == 0 && f.yRot == 0 && f.zRot == 0)) return;
        f.translateAndRotate(pose);
        pose.translate(-f.x / 16f, -f.y / 16f, -f.z / 16f);
    }
}
