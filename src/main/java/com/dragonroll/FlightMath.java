package com.dragonroll;

import net.minecraft.world.phys.Vec3;

/**
 * Orientation math. A flight orientation is stored as two orthonormal vectors:
 * forward (where the nose points) and up (where the dragon's back points).
 * Uses Minecraft's yaw/pitch conventions (yaw 0 = +Z/south, positive pitch = looking down).
 */
public final class FlightMath {
    private FlightMath() {}

    public static final Vec3 WORLD_UP = new Vec3(0, 1, 0);

    /** Rotate v around a unit axis by angle (radians), right-hand rule (Rodrigues). */
    public static Vec3 rotate(Vec3 v, Vec3 axis, double angle) {
        double c = Math.cos(angle), s = Math.sin(angle);
        Vec3 cross = axis.cross(v);
        double dot = axis.dot(v);
        return v.scale(c).add(cross.scale(s)).add(axis.scale(dot * (1 - c)));
    }

    public static Vec3 forwardFrom(float yawDeg, float pitchDeg) {
        double y = Math.toRadians(yawDeg), p = Math.toRadians(pitchDeg);
        return new Vec3(-Math.sin(y) * Math.cos(p), -Math.sin(p), Math.cos(y) * Math.cos(p));
    }

    /** Up vector for the given yaw/pitch with zero roll. */
    public static Vec3 levelUpFrom(float yawDeg, float pitchDeg) {
        double y = Math.toRadians(yawDeg), p = Math.toRadians(pitchDeg);
        return new Vec3(-Math.sin(y) * Math.sin(p), Math.cos(p), Math.cos(y) * Math.sin(p));
    }

    /** Re-orthonormalize; returns {forward, up}. Falls back to level flight if the input is garbage. */
    public static Vec3[] orthonormalize(Vec3 f, Vec3 u) {
        if (!finite(f) || !finite(u) || f.lengthSqr() < 1e-8) return new Vec3[]{new Vec3(0, 0, 1), WORLD_UP};
        f = f.normalize();
        Vec3 r = f.cross(u);
        if (r.lengthSqr() < 1e-8) {
            r = f.cross(Math.abs(f.y) < 0.99 ? WORLD_UP : new Vec3(1, 0, 0));
        }
        r = r.normalize();
        u = r.cross(f).normalize();
        return new Vec3[]{f, u};
    }

    public static boolean finite(Vec3 v) {
        return Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z);
    }

    /** Returns {yaw, pitch, roll} in degrees. Positive roll = banked right. */
    public static float[] toEuler(Vec3 f, Vec3 u) {
        double pitch = Math.toDegrees(-Math.asin(clamp(f.y, -1, 1)));
        double yaw = Math.toDegrees(Math.atan2(-f.x, f.z));
        Vec3 up0 = levelUpFrom((float) yaw, (float) pitch);
        Vec3 right0 = f.cross(up0);
        double roll = Math.toDegrees(Math.atan2(u.dot(right0), u.dot(up0)));
        return new float[]{(float) yaw, (float) pitch, (float) roll};
    }

    public static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : Math.min(v, hi);
    }

    /**
     * Quaternion (x, y, z, w) of the rotation that takes the frame (fromF, fromU) onto (toF, toU).
     */
    public static float[] frameRotation(Vec3 fromF, Vec3 fromU, Vec3 toF, Vec3 toU) {
        Vec3 fromR = fromF.cross(fromU), toR = toF.cross(toU);
        // R = To * From^T, columns of To/From are (F, U, R)
        double[][] m = new double[3][3];
        double[][] to = {{toF.x, toU.x, toR.x}, {toF.y, toU.y, toR.y}, {toF.z, toU.z, toR.z}};
        double[][] fr = {{fromF.x, fromU.x, fromR.x}, {fromF.y, fromU.y, fromR.y}, {fromF.z, fromU.z, fromR.z}};
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++) {
                double s = 0;
                for (int k = 0; k < 3; k++) s += to[i][k] * fr[j][k];
                m[i][j] = s;
            }
        double tr = m[0][0] + m[1][1] + m[2][2];
        double x, y, z, w;
        if (tr > 0) {
            double s = Math.sqrt(tr + 1.0) * 2;
            w = 0.25 * s; x = (m[2][1] - m[1][2]) / s; y = (m[0][2] - m[2][0]) / s; z = (m[1][0] - m[0][1]) / s;
        } else if (m[0][0] > m[1][1] && m[0][0] > m[2][2]) {
            double s = Math.sqrt(1.0 + m[0][0] - m[1][1] - m[2][2]) * 2;
            w = (m[2][1] - m[1][2]) / s; x = 0.25 * s; y = (m[0][1] + m[1][0]) / s; z = (m[0][2] + m[2][0]) / s;
        } else if (m[1][1] > m[2][2]) {
            double s = Math.sqrt(1.0 + m[1][1] - m[0][0] - m[2][2]) * 2;
            w = (m[0][2] - m[2][0]) / s; x = (m[0][1] + m[1][0]) / s; y = 0.25 * s; z = (m[1][2] + m[2][1]) / s;
        } else {
            double s = Math.sqrt(1.0 + m[2][2] - m[0][0] - m[1][1]) * 2;
            w = (m[1][0] - m[0][1]) / s; x = (m[0][2] + m[2][0]) / s; y = (m[1][2] + m[2][1]) / s; z = 0.25 * s;
        }
        return new float[]{(float) x, (float) y, (float) z, (float) w};
    }

    /** Rotate v by quaternion q = {x, y, z, w}. */
    public static Vec3 rotate(float[] q, Vec3 v) {
        Vec3 u = new Vec3(q[0], q[1], q[2]);
        double w = q[3];
        Vec3 t = u.cross(v).scale(2);
        return v.add(t.scale(w)).add(u.cross(t));
    }
}
