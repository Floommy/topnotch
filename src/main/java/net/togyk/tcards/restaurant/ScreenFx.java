package net.togyk.tcards.restaurant;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.mojang.math.Transformation;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.togyk.tcards.mixin.DisplayInvoker;
import net.togyk.tcards.mixin.ItemDisplayInvoker;
import net.togyk.tcards.mixin.TextDisplayInvoker;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Text and item display entities used as screen content, receipt paper and food on trays.
 * They are owned by block entities and rebuilt by them; copies saved with the chunk are thrown away on load.
 */
public final class ScreenFx {
    private ScreenFx() {}

    public static final String TAG = "tcards_rest_fx";
    private static final Set<UUID> LIVE = new HashSet<>();

    public static void init() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity.entityTags().contains(TAG) && !LIVE.contains(entity.getUUID())) entity.discard();
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            if (entity.entityTags().contains(TAG)) LIVE.remove(entity.getUUID());
        });
    }

    public static boolean alive(Display d) {
        return d != null && !d.isRemoved();
    }

    public static void kill(Display d) {
        if (d != null) {
            LIVE.remove(d.getUUID());
            d.discard();
        }
    }

    /** Yaw that makes a display face the given horizontal direction. */
    public static float yaw(Direction facing) {
        return facing.toYRot();
    }

    public static Display.TextDisplay text(ServerLevel level, Vec3 at, Direction facing, float scale, int lineWidth, int background) {
        Display.TextDisplay d = EntityTypes.TEXT_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);
        if (d == null) return null;
        d.setPos(at);
        d.setYRot(yaw(facing));
        d.setXRot(0);
        TextDisplayInvoker t = (TextDisplayInvoker) d;
        t.tcards$setLineWidth(lineWidth);
        t.tcards$setBackgroundColor(background);
        t.tcards$setText(Component.empty());
        DisplayInvoker di = (DisplayInvoker) d;
        di.tcards$setBillboard(Display.BillboardConstraints.FIXED);
        di.tcards$setBrightness(Brightness.FULL_BRIGHT);
        di.tcards$setViewRange(0.5f);
        di.tcards$setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(scale), new Quaternionf()));
        d.addTag(TAG);
        LIVE.add(d.getUUID());
        level.addFreshEntity(d);
        return d;
    }

    public static Display.ItemDisplay item(ServerLevel level, Vec3 at, float yaw, ItemStack stack) {
        Display.ItemDisplay d = EntityTypes.ITEM_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);
        if (d == null) return null;
        d.setPos(at);
        d.setYRot(yaw);
        ((ItemDisplayInvoker) d).tcards$setItemStack(stack.copy());
        ((ItemDisplayInvoker) d).tcards$setItemTransform(ItemDisplayContext.FIXED);
        ((DisplayInvoker) d).tcards$setPosRotInterpolationDuration(0);
        ((DisplayInvoker) d).tcards$setViewRange(0.5f);
        d.addTag(TAG);
        LIVE.add(d.getUUID());
        level.addFreshEntity(d);
        return d;
    }

    public static void setText(Display.TextDisplay d, Component text) {
        ((TextDisplayInvoker) d).tcards$setText(text);
    }

    public static void transform(Display d, Vector3f translation, Quaternionf rotation, Vector3f scale, int interpolate) {
        DisplayInvoker di = (DisplayInvoker) d;
        di.tcards$setTransformation(new Transformation(translation, rotation, scale, new Quaternionf()));
        di.tcards$setInterpolationDelay(0);
        di.tcards$setInterpolationDuration(interpolate);
    }
}
