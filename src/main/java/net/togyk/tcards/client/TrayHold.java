package net.togyk.tcards.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.togyk.tcards.restaurant.RestaurantRegistry;
import org.joml.Vector3f;

/**
 * Carrying the serving tray like a map: with the off hand empty it is held level in front with both hands
 * (first person: the map hands; third person: both forearms under the tray). Otherwise it is a normal held item.
 */
@Environment(EnvType.CLIENT)
public final class TrayHold {
    private TrayHold() {}

    /** Tray size in third person (1 = block size). */
    public static final float THIRD_SCALE = 0.75f;
    /** Tray size in first person, in the map's frame. */
    public static final float FIRST_SCALE = 0.45f;

    private static ItemStackRenderState renderState;

    public static boolean isTray(ItemStack s) {
        return s != null && !s.isEmpty() && s.is(RestaurantRegistry.TRAY.asItem());
    }

    public static boolean twoHanded(ItemStack main, ItemStack off) {
        return isTray(main) && (off == null || off.isEmpty());
    }

    public static boolean twoHanded(ArmedEntityRenderState s) {
        ItemStack r = s.rightHandItemStack, l = s.leftHandItemStack;
        boolean rightMain = s.mainArm != HumanoidArm.LEFT;
        return twoHanded(rightMain ? r : l, rightMain ? l : r);
    }

    /** Third-person arms: upper arms down at the sides, forearms forward and level, a bit inward. */
    public static void poseArms(ModelPart rightArm, ModelPart leftArm) {
        ModelPart rf = Elbows.forearm(rightArm), lf = Elbows.forearm(leftArm);
        if (rf != null && lf != null) {
            set(rightArm, -0.20f, -0.30f, 0.0f);
            set(leftArm, -0.20f, 0.30f, 0.0f);
            set(rf, -1.35f, 0.0f, 0.0f);
            set(lf, -1.35f, 0.0f, 0.0f);
        } else {   // no elbows (another mod replaced the arms): straight arms forward
            set(rightArm, -1.35f, -0.30f, 0.0f);
            set(leftArm, -1.35f, 0.30f, 0.0f);
        }
    }

    private static void set(ModelPart p, float x, float y, float z) {
        p.xRot = x;
        p.yRot = y;
        p.zRot = z;
    }

    /** Top of the hand (where the tray rests), in model space. */
    static Vector3f hand(HumanoidModel<?> model, boolean right) {
        ModelPart arm = right ? model.rightArm : model.leftArm;
        PoseStack p = new PoseStack();
        model.root().translateAndRotate(p);
        arm.translateAndRotate(p);
        ModelPart f = Elbows.forearm(arm);
        Vector3f local;
        if (f != null) {
            f.translateAndRotate(p);
            local = new Vector3f(0f, 5f, -2f);
        } else {
            local = new Vector3f(right ? -1f : 1f, 9f, -2f);
        }
        return p.last().pose().transformPosition(local.div(16f));
    }

    /** Pose stack from model space to the tray item (before the item's own -0.5 offset). Public for the render test. */
    public static void toThirdPersonTray(HumanoidModel<?> model, PoseStack pose) {
        Vector3f r = hand(model, true), l = hand(model, false);
        Vector3f mid = r.add(l).mul(0.5f);
        pose.translate(mid.x, mid.y - 0.5f * THIRD_SCALE, mid.z - 1f / 16f);
        pose.rotateDegrees(Axis.ZP, 180f);   // model space is y-down: tray top faces up
        pose.scale(THIRD_SCALE, THIRD_SCALE, THIRD_SCALE);
    }

    public static void submitThirdPerson(HumanoidModel<?> model, ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light, int outline) {
        pose.pushPose();
        toThirdPersonTray(model, pose);
        submit(stack, pose, collector, light, outline);
        pose.popPose();
    }

    /** In the frame of a two-handed map (after its scale 2): the tray lies where the map would be. */
    public static void submitFirstPerson(ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {
        pose.pushPose();
        pose.translate(0f, -0.07f, 0.5f * FIRST_SCALE);
        pose.rotateDegrees(Axis.XP, 90f);   // tray top faces the map's front
        pose.scale(FIRST_SCALE, FIRST_SCALE, FIRST_SCALE);
        submit(stack, pose, collector, light, 0);
        pose.popPose();
    }

    private static void submit(ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light, int outline) {
        Minecraft mc = Minecraft.getInstance();
        if (renderState == null) renderState = new ItemStackRenderState();
        mc.getItemModelResolver().updateForTopItem(renderState, stack, ItemDisplayContext.NONE, mc.level, null, 0);
        renderState.submit(pose, collector, light, OverlayTexture.NO_OVERLAY, outline);
    }
}
