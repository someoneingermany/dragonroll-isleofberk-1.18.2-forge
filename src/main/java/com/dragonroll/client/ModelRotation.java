package com.dragonroll.client;

import com.dragonroll.Config;
import com.dragonroll.FlightMath;
import com.dragonroll.SyncStore;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Quaternion;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Tilts a flying dragon's model (and riders drawn separately from it) to its flight orientation.
 * Called from Forge's RenderLivingEvent for normal renderers and from a GeckoLib mixin for
 * GeckoLib renderers (Isle of Berk), since GeckoLib 3 doesn't fire the Forge event.
 */
public final class ModelRotation {
    private ModelRotation() {}

    private static final Deque<Entity> PUSHED = new ArrayDeque<>();

    static Vec3[] orientationFor(Entity e) {
        if (ClientFlightHandler.isActive() && e == ClientFlightHandler.activeVehicle()) {
            return ClientFlightHandler.orientation();
        }
        return SyncStore.get(e.getId());
    }

    /** Quaternion {x,y,z,w} taking the dragon's level, yaw-only frame to its flight frame. */
    static float[] rotationFor(Entity dragon, Vec3[] o, float pt) {
        float bodyYaw = dragon instanceof LivingEntity l
                ? Mth.rotLerp(pt, l.yBodyRotO, l.yBodyRot)
                : Mth.rotLerp(pt, dragon.yRotO, dragon.getYRot());
        Vec3 baseF = FlightMath.forwardFrom(bodyYaw, 0);
        Vec3 tf = o[0], tu = o[1];
        if (!Config.RENDER_MODEL_PITCH.get()) {
            float[] e = FlightMath.toEuler(tf, tu);
            tf = FlightMath.forwardFrom(e[0], 0);
            Vec3 r0 = tf.cross(FlightMath.WORLD_UP);
            double r = Math.toRadians(e[2]);
            tu = FlightMath.WORLD_UP.scale(Math.cos(r)).add(r0.scale(Math.sin(r)));
        }
        return FlightMath.frameRotation(baseF, FlightMath.WORLD_UP, tf, tu);
    }

    static Vec3 lerpPos(Entity e, float pt) {
        return new Vec3(Mth.lerp(pt, e.xo, e.getX()), Mth.lerp(pt, e.yo, e.getY()), Mth.lerp(pt, e.zo, e.getZ()));
    }

    static Vec3 pivot(Entity dragon, float pt) {
        return lerpPos(dragon, pt).add(0, dragon.getBbHeight() * 0.5, 0);
    }

    public static void begin(LivingEntity entity, float pt, PoseStack ps) {
        if (!Config.RENDER_MODEL_ROTATION.get() || PUSHED.contains(entity)) return;
        Entity dragon = entity;
        Vec3[] o = orientationFor(entity);
        if (o == null) {
            Entity v = entity.getVehicle();
            // Skip riders that are being drawn inside the dragon's own (already rotated) model,
            // like Isle of Berk Variant Loader's passenger layer.
            if (v == null || !Config.ROTATE_RIDERS.get() || PUSHED.contains(v)) return;
            o = orientationFor(v);
            if (o == null) return;
            dragon = v;
        }
        float[] q = rotationFor(dragon, o, pt);
        Vec3 p = pivot(dragon, pt).subtract(lerpPos(entity, pt)); // pivot relative to this entity's render origin
        ps.pushPose();
        ps.translate(p.x, p.y, p.z);
        ps.mulPose(new Quaternion(q[0], q[1], q[2], q[3]));
        ps.translate(-p.x, -p.y, -p.z);
        PUSHED.push(entity);
    }

    public static void end(LivingEntity entity, PoseStack ps) {
        if (!PUSHED.isEmpty() && PUSHED.peek() == entity) {
            PUSHED.pop();
            ps.popPose();
        }
    }
}
