package com.dragonroll.client;

import com.dragonroll.Config;
import com.dragonroll.DragonRoll;
import com.dragonroll.FlightMath;
import com.dragonroll.FlightPhysics;
import com.dragonroll.Mounts;
import com.dragonroll.SyncStore;
import com.dragonroll.network.FlightInputPacket;
import com.dragonroll.network.Network;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Client side of Do-a-Barrel-Roll style dragon flight.
 *
 * Mouse movement is read as a change in the player's rotation since the last frame, then turned into
 * pitch/roll/yaw of a free orientation (no gimbal lock, full loops and barrel rolls). The camera is
 * rolled through Forge's CameraSetup event. While active, the dragon's own movement inputs are zeroed
 * and the elytra-style physics in FlightPhysics move it instead.
 */
@Mod.EventBusSubscriber(modid = DragonRoll.MODID, value = Dist.CLIENT)
public final class ClientFlightHandler {
    private ClientFlightHandler() {}

    public static final KeyMapping TOGGLE =
            new KeyMapping("key.dragonroll.toggle", GLFW.GLFW_KEY_V, "key.categories.dragonroll");

    private static boolean active;
    private static Entity vehicle;
    private static boolean prevNoGravity;
    private static Vec3 forward = new Vec3(0, 0, 1), up = FlightMath.WORLD_UP, velocity = Vec3.ZERO;
    private static float[] euler = {0, 0, 0};
    private static float neutralYaw;
    private static long lastFrameNanos;
    private static float keyAxis;
    private static boolean thrust, brake, flap;
    private static int activeTicks, groundTicks;
    private static boolean warnedNoServerMod;

    public static boolean isActive() { return active; }
    public static Entity activeVehicle() { return vehicle; }
    public static Vec3[] orientation() { return new Vec3[]{forward, up}; }

    // ------------------------------------------------------------------ toggling

    private static void activate(LocalPlayer player) {
        Entity v = player.getVehicle();
        if (v == null || !Mounts.isEligible(v, player)) {
            player.displayClientMessage(new TranslatableComponent("dragonroll.msg.not_mount"), true);
            return;
        }
        forward = FlightMath.forwardFrom(player.getYRot(), player.getXRot());
        up = FlightMath.levelUpFrom(player.getYRot(), player.getXRot());
        euler = FlightMath.toEuler(forward, up);
        Vec3 moved = v.position().subtract(v.xo, v.yo, v.zo);
        velocity = moved.lengthSqr() > v.getDeltaMovement().lengthSqr() ? moved : v.getDeltaMovement();
        prevNoGravity = v.isNoGravity();
        v.setNoGravity(true);
        vehicle = v;
        neutralYaw = player.getYRot();
        player.setXRot(0);
        player.xRotO = 0;
        active = true;
        activeTicks = 0;
        groundTicks = 0;
        lastFrameNanos = System.nanoTime();
        player.displayClientMessage(new TranslatableComponent("dragonroll.msg.on"), true);
        if (!serverHasMod() && !v.isControlledByLocalInstance() && !warnedNoServerMod) {
            warnedNoServerMod = true;
            player.displayClientMessage(new TextComponent(
                    "Dragon Roll isn't installed on this server and this dragon is server-controlled, so flight may rubber-band."), false);
        }
    }

    private static void deactivate(LocalPlayer player, boolean showMessage) {
        if (!active) return;
        active = false;
        if (vehicle != null && !vehicle.isRemoved()) {
            vehicle.setNoGravity(prevNoGravity);
            if (vehicle.isControlledByLocalInstance()) vehicle.setDeltaMovement(velocity);
        }
        if (serverHasMod()) {
            Network.CHANNEL.sendToServer(new FlightInputPacket(false, false, false, false, false, forward, up));
        }
        if (player != null) {
            float pitch = Mth.clamp(euler[1], -90f, 90f);
            player.setYRot(euler[0]);
            player.yRotO = euler[0];
            player.setXRot(pitch);
            player.xRotO = pitch;
            if (showMessage) player.displayClientMessage(new TranslatableComponent("dragonroll.msg.off"), true);
        }
        vehicle = null;
    }

    private static boolean serverHasMod() {
        ClientPacketListener conn = Minecraft.getInstance().getConnection();
        return conn != null && Network.CHANNEL.isRemotePresent(conn.getConnection());
    }

    // ------------------------------------------------------------------ input

    /** Read WASD/space before the dragon sees them, then hide them from the dragon. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!active || event.getPlayer() != Minecraft.getInstance().player) return;
        Input in = event.getInput();
        keyAxis = in.leftImpulse;
        thrust = in.forwardImpulse > 0;
        brake = in.forwardImpulse < 0;
        flap = in.jumping;
        in.forwardImpulse = 0;
        in.leftImpulse = 0;
        in.up = in.down = in.left = in.right = false;
        in.jumping = false; // sneak is left alone so you can still dismount
    }

    // ------------------------------------------------------------------ physics (20 Hz)

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            active = false;
            vehicle = null;
            return;
        }
        while (TOGGLE.consumeClick()) {
            if (active) deactivate(player, true);
            else activate(player);
        }
        if (!active) return;

        Entity v = player.getVehicle();
        if (v == null || v != vehicle || v.isRemoved() || !Mounts.isEligible(v, player)) {
            deactivate(player, true);
            return;
        }
        activeTicks++;

        boolean local = v.isControlledByLocalInstance();
        boolean modded = serverHasMod();
        if (local || !modded) {
            velocity = FlightPhysics.step(velocity, forward, up, thrust, brake, flap);
            Vec3 before = v.position();
            v.move(MoverType.SELF, velocity);
            if (v.horizontalCollision || v.verticalCollision) velocity = v.position().subtract(before);
            v.setDeltaMovement(Vec3.ZERO);
        }
        applyVehicleRotation(v);
        if (local || !modded) v.positionRider(player);
        v.fallDistance = 0;
        player.fallDistance = 0;

        if (modded) {
            Network.CHANNEL.sendToServer(new FlightInputPacket(true, !local, thrust, brake, flap, forward, up));
        }

        if (Config.AUTO_OFF_ON_LANDING.get() && activeTicks > 10 && v.isOnGround()) {
            if (++groundTicks >= 3) {
                deactivate(player, true);
                return;
            }
        } else {
            groundTicks = 0;
        }

        // Undo anything the tick did to the player's rotation so it isn't read as mouse movement.
        player.setYRot(neutralYaw);
        player.yRotO = neutralYaw;
        player.setXRot(0);
        player.xRotO = 0;
    }

    private static void applyVehicleRotation(Entity v) {
        float[] e = FlightMath.toEuler(forward, up);
        boolean modelPitches = Config.RENDER_MODEL_ROTATION.get() && Config.RENDER_MODEL_PITCH.get();
        v.setYRot(e[0]);
        v.setXRot(modelPitches ? 0 : Mth.clamp(e[1], -90f, 90f));
        if (v instanceof LivingEntity living) {
            living.yBodyRot = e[0];
            living.yHeadRot = e[0];
        }
    }

    // ------------------------------------------------------------------ rotation (every frame)

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (!active) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        if (event.phase == TickEvent.Phase.END) {
            // Park the player's rotation at a neutral value; next frame's mouse movement is measured from it.
            player.setYRot(neutralYaw);
            player.yRotO = neutralYaw;
            player.setXRot(0);
            player.xRotO = 0;
            return;
        }

        long now = System.nanoTime();
        double dt = FlightMath.clamp((now - lastFrameNanos) / 50_000_000.0, 0.0, 3.0); // in ticks
        lastFrameNanos = now;

        float mouseX = Mth.wrapDegrees(player.getYRot() - neutralYaw);
        float mouseY = player.getXRot();
        if (mc.isPaused()) { mouseX = 0; mouseY = 0; dt = 0; }
        if (Config.INVERT_PITCH.get()) mouseY = -mouseY;

        double keys = -keyAxis * Config.KEY_TURN_SPEED.get() * dt; // + = right
        double rollDeg, yawDeg;
        if (Config.MOUSE_X_ROLLS.get()) {
            rollDeg = mouseX * Config.ROLL_SENSITIVITY.get();
            yawDeg = keys;
        } else {
            rollDeg = keys;
            yawDeg = mouseX * Config.ROLL_SENSITIVITY.get();
        }
        double pitchDeg = -mouseY * Config.PITCH_SENSITIVITY.get(); // + = nose up

        Vec3 right = forward.cross(up);
        double a = Math.toRadians(pitchDeg);
        forward = FlightMath.rotate(forward, right, a);
        up = FlightMath.rotate(up, right, a);

        up = FlightMath.rotate(up, forward, Math.toRadians(rollDeg));

        forward = FlightMath.rotate(forward, up, Math.toRadians(-yawDeg));

        // Banking: a rolled dragon slowly turns toward the low wing.
        right = forward.cross(up);
        double bank = Math.toRadians(Config.BANKING_STRENGTH.get() * right.y * dt);
        forward = FlightMath.rotate(forward, FlightMath.WORLD_UP, bank);
        up = FlightMath.rotate(up, FlightMath.WORLD_UP, bank);

        Vec3[] o = FlightMath.orthonormalize(forward, up);
        forward = o[0];
        up = o[1];
        euler = FlightMath.toEuler(forward, up);

        // Give this frame's camera (and 3rd-person camera position) the real heading.
        float pitch = Mth.clamp(euler[1], -90f, 90f);
        player.setYRot(euler[0]);
        player.yRotO = euler[0];
        player.setXRot(pitch);
        player.xRotO = pitch;
        player.yHeadRot = euler[0];
        player.yHeadRotO = euler[0];
        neutralYaw = euler[0];
    }

    @SubscribeEvent
    public static void onCameraSetup(EntityViewRenderEvent.CameraSetup event) {
        if (!active) return;
        float yaw = euler[0], pitch = euler[1], roll = euler[2];
        if (Config.INVERT_ROLL_CAMERA.get()) roll = -roll;
        if (Minecraft.getInstance().options.getCameraType().isMirrored()) {
            yaw += 180f;
            pitch = -pitch;
            roll = -roll;
        }
        event.setYaw(yaw);
        event.setPitch(pitch);
        event.setRoll(roll);
        moveCameraWithSaddle(event.getCamera(), (float) event.getPartialTicks());
    }

    /**
     * The rider physically stays above the dragon's center; only the model is rotated. Swing the
     * camera's eye point around the dragon the same way, so when upside down you're still in the saddle.
     */
    private static void moveCameraWithSaddle(Camera camera, float pt) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || vehicle == null || !Config.RENDER_MODEL_ROTATION.get()) return;
        float[] q = ModelRotation.rotationFor(vehicle, new Vec3[]{forward, up}, pt);
        Vec3 pivot = ModelRotation.pivot(vehicle, pt);
        Vec3 eye = ModelRotation.lerpPos(player, pt).add(0, player.getEyeHeight(), 0);
        Vec3 thirdPersonOffset = camera.getPosition().subtract(eye);
        Vec3 newEye = pivot.add(FlightMath.rotate(q, eye.subtract(pivot)));
        setCameraPosition(camera, newEye.add(thirdPersonOffset));
    }

    private static Field cameraPosField, cameraBlockPosField;
    private static boolean cameraReflectionFailed;

    private static void setCameraPosition(Camera camera, Vec3 pos) {
        if (cameraReflectionFailed) return;
        try {
            if (cameraPosField == null) {
                for (Field f : Camera.class.getDeclaredFields()) {
                    if (Modifier.isStatic(f.getModifiers())) continue;
                    if (f.getType() == Vec3.class) cameraPosField = f;
                    if (f.getType() == BlockPos.MutableBlockPos.class) cameraBlockPosField = f;
                }
                if (cameraPosField == null) {
                    cameraReflectionFailed = true;
                    return;
                }
                cameraPosField.setAccessible(true);
                if (cameraBlockPosField != null) cameraBlockPosField.setAccessible(true);
            }
            cameraPosField.set(camera, pos);
            if (cameraBlockPosField != null) {
                ((BlockPos.MutableBlockPos) cameraBlockPosField.get(camera)).set(pos.x, pos.y, pos.z);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            cameraReflectionFailed = true;
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggedOutEvent event) {
        active = false;
        vehicle = null;
        warnedNoServerMod = false;
        SyncStore.clear();
    }
}
