package com.dragonroll;

import net.minecraft.world.phys.Vec3;

/**
 * Vanilla elytra glide physics (LivingEntity#travel, 1.18.2) driven by the dragon's forward
 * vector instead of the player's look, plus wing thrust/lift/brake so dragons can climb.
 */
public final class FlightPhysics {
    private FlightPhysics() {}

    public static Vec3 step(Vec3 vel, Vec3 f, Vec3 u, boolean thrust, boolean brake, boolean flap) {
        double gravity = Config.GRAVITY.get();
        double hLook = Math.sqrt(f.x * f.x + f.z * f.z);      // = cos(pitch)
        double hVel = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        double lift = hLook * hLook;

        vel = vel.add(0, gravity * (-1.0 + lift * 0.75), 0);
        if (vel.y < 0 && hLook > 0) {                         // diving converts fall speed into forward speed
            double d = vel.y * -0.1 * lift;
            vel = vel.add(f.x * d / hLook, d, f.z * d / hLook);
        }
        if (f.y > 0 && hLook > 0) {                           // climbing trades forward speed for height
            double d = hVel * f.y * 0.04;
            vel = vel.add(-f.x * d / hLook, d * 3.2, -f.z * d / hLook);
        }
        if (hLook > 0) {                                      // steer horizontal velocity toward the nose
            vel = vel.add((f.x / hLook * hVel - vel.x) * 0.1, 0, (f.z / hLook * hVel - vel.z) * 0.1);
        }
        vel = vel.multiply(0.99, 0.98, 0.99);

        if (thrust) vel = vel.add(f.scale(Config.THRUST.get()));
        if (flap) vel = vel.add(u.scale(Config.FLAP_LIFT.get()));
        if (brake) vel = vel.scale(Config.BRAKE.get());

        double max = Config.MAX_SPEED.get();
        if (vel.lengthSqr() > max * max) vel = vel.normalize().scale(max);
        return FlightMath.finite(vel) ? vel : Vec3.ZERO;
    }
}
