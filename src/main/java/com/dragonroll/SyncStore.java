package com.dragonroll;

import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Client-side cache of other players' dragon orientations (entity id -> {forward, up}). */
public final class SyncStore {
    private SyncStore() {}

    private static final Map<Integer, Vec3[]> ORIENTATIONS = new ConcurrentHashMap<>();

    public static void apply(int entityId, boolean active, Vec3 forward, Vec3 up) {
        if (active) ORIENTATIONS.put(entityId, FlightMath.orthonormalize(forward, up));
        else ORIENTATIONS.remove(entityId);
    }

    public static Vec3[] get(int entityId) {
        return ORIENTATIONS.get(entityId);
    }

    public static void clear() {
        ORIENTATIONS.clear();
    }
}
