package com.dragonroll;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class Config {
    private Config() {}

    // ---------- COMMON (physics + which mounts count as dragons) ----------
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec.DoubleValue GRAVITY;
    public static final ForgeConfigSpec.DoubleValue THRUST;
    public static final ForgeConfigSpec.DoubleValue FLAP_LIFT;
    public static final ForgeConfigSpec.DoubleValue BRAKE;
    public static final ForgeConfigSpec.DoubleValue MAX_SPEED;
    public static final ForgeConfigSpec.BooleanValue REQUIRE_NON_VANILLA;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> WHITELIST;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLACKLIST;

    // ---------- CLIENT (controls, camera, rendering) ----------
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final ForgeConfigSpec.BooleanValue MOUSE_X_ROLLS;
    public static final ForgeConfigSpec.DoubleValue PITCH_SENSITIVITY;
    public static final ForgeConfigSpec.DoubleValue ROLL_SENSITIVITY;
    public static final ForgeConfigSpec.DoubleValue KEY_TURN_SPEED;
    public static final ForgeConfigSpec.DoubleValue BANKING_STRENGTH;
    public static final ForgeConfigSpec.BooleanValue INVERT_PITCH;
    public static final ForgeConfigSpec.BooleanValue INVERT_ROLL_CAMERA;
    public static final ForgeConfigSpec.BooleanValue AUTO_OFF_ON_LANDING;
    public static final ForgeConfigSpec.BooleanValue RENDER_MODEL_ROTATION;
    public static final ForgeConfigSpec.BooleanValue RENDER_MODEL_PITCH;
    public static final ForgeConfigSpec.BooleanValue ROTATE_RIDERS;

    static {
        ForgeConfigSpec.Builder c = new ForgeConfigSpec.Builder();
        c.comment("Flight physics. Based on vanilla elytra physics plus dragon wing thrust.").push("physics");
        GRAVITY = c.comment("Gravity per tick (vanilla elytra = 0.08)").defineInRange("gravity", 0.08, 0.0, 1.0);
        THRUST = c.comment("Forward acceleration per tick while holding forward (wing flaps)").defineInRange("thrust", 0.035, 0.0, 1.0);
        FLAP_LIFT = c.comment("Lift per tick while holding jump, along the dragon's up direction").defineInRange("flapLift", 0.05, 0.0, 1.0);
        BRAKE = c.comment("Speed multiplier per tick while holding back (lower = stronger brake)").defineInRange("brake", 0.94, 0.0, 1.0);
        MAX_SPEED = c.comment("Max speed in blocks per tick (20 ticks = 1 second)").defineInRange("maxSpeed", 2.2, 0.1, 9.0);
        c.pop();
        c.comment("Which ridden entities can use barrel-roll flight. Patterns use * as a wildcard and match the",
                  "entity id like 'somemod:night_fury'. Check an entity id with F3 + looking at it, or /summon tab-complete.").push("mounts");
        WHITELIST = c.comment("If not empty, ONLY these entities work. Example: [\"clawsofberk:*\", \"*dragon*\"]")
                .defineList("whitelist", List.of(), o -> o instanceof String);
        BLACKLIST = c.comment("These entities never work").defineList("blacklist", List.of(), o -> o instanceof String);
        REQUIRE_NON_VANILLA = c.comment("When the whitelist is empty, only allow modded (non-minecraft:) mounts")
                .define("requireNonVanilla", true);
        c.pop();
        COMMON_SPEC = c.build();

        ForgeConfigSpec.Builder cl = new ForgeConfigSpec.Builder();
        cl.push("controls");
        MOUSE_X_ROLLS = cl.comment("true = mouse left/right rolls and A/D yaws (Do a Barrel Roll default).",
                "false = mouse left/right yaws and A/D rolls.").define("mouseXRolls", true);
        PITCH_SENSITIVITY = cl.defineInRange("pitchSensitivity", 1.0, 0.0, 10.0);
        ROLL_SENSITIVITY = cl.defineInRange("rollSensitivity", 1.0, 0.0, 10.0);
        KEY_TURN_SPEED = cl.comment("Degrees per tick for the keyboard axis (A/D)").defineInRange("keyTurnSpeed", 4.0, 0.0, 45.0);
        BANKING_STRENGTH = cl.comment("How much rolling sideways turns you, like banking a plane. 0 = off")
                .defineInRange("bankingStrength", 3.0, 0.0, 20.0);
        INVERT_PITCH = cl.define("invertPitch", false);
        INVERT_ROLL_CAMERA = cl.comment("Flip this if the camera tilts the wrong way").define("invertRollCamera", false);
        AUTO_OFF_ON_LANDING = cl.comment("Switch back to the dragon's normal controls when it touches the ground")
                .define("autoOffOnLanding", true);
        cl.pop();
        cl.push("rendering");
        RENDER_MODEL_ROTATION = cl.comment("Tilt the dragon model to match your flight orientation").define("rotateModel", true);
        RENDER_MODEL_PITCH = cl.comment("Also pitch the model (turn off if the dragon mod already tilts it up/down itself)")
                .define("rotateModelPitch", true);
        ROTATE_RIDERS = cl.comment("Rotate riders together with the dragon").define("rotateRiders", true);
        cl.pop();
        CLIENT_SPEC = cl.build();
    }
}
