package com.dragonroll.server;

import com.dragonroll.DragonRoll;
import com.dragonroll.FlightMath;
import com.dragonroll.FlightPhysics;
import com.dragonroll.Mounts;
import com.dragonroll.network.FlightInputPacket;
import com.dragonroll.network.FlightSyncPacket;
import com.dragonroll.network.Network;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server side: keeps fall damage off, syncs the dragon's rotation to other players, and, for dragon
 * mods that move their mounts on the server, runs the flight physics here.
 */
@Mod.EventBusSubscriber(modid = DragonRoll.MODID)
public final class ServerFlightHandler {
    private ServerFlightHandler() {}

    private static final class State {
        Entity vehicle;
        boolean prevNoGravity, serverMove, thrust, brake, flap;
        Vec3 forward, up, velocity;
        int lastPacketTick;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();
    private static int tickCounter;

    public static void onInput(ServerPlayer player, FlightInputPacket p) {
        Entity vehicle = player.getVehicle();
        State s = STATES.get(player.getUUID());
        if (!p.active() || vehicle == null || !Mounts.isEligible(vehicle, player)) {
            if (s != null) stop(player.getUUID(), s);
            return;
        }
        if (s != null && s.vehicle != vehicle) {
            stop(player.getUUID(), s);
            s = null;
        }
        if (s == null) {
            s = new State();
            s.vehicle = vehicle;
            s.prevNoGravity = vehicle.isNoGravity();
            s.velocity = vehicle.getDeltaMovement();
            vehicle.setNoGravity(true);
            STATES.put(player.getUUID(), s);
        }
        Vec3[] o = FlightMath.orthonormalize(p.forward(), p.up());
        s.forward = o[0];
        s.up = o[1];
        s.serverMove = p.serverMove();
        s.thrust = p.thrust();
        s.brake = p.brake();
        s.flap = p.flap();
        s.lastPacketTick = tickCounter;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        tickCounter++;
        if (STATES.isEmpty()) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        for (Map.Entry<UUID, State> e : new ArrayList<>(STATES.entrySet())) {
            State s = e.getValue();
            ServerPlayer player = server.getPlayerList().getPlayer(e.getKey());
            Entity vehicle = s.vehicle;
            if (player == null || vehicle.isRemoved() || player.getVehicle() != vehicle
                    || tickCounter - s.lastPacketTick > 20) {
                stop(e.getKey(), s);
                continue;
            }

            if (s.serverMove) {
                s.velocity = FlightPhysics.step(s.velocity, s.forward, s.up, s.thrust, s.brake, s.flap);
                Vec3 before = vehicle.position();
                vehicle.move(MoverType.SELF, s.velocity);
                if (vehicle.horizontalCollision || vehicle.verticalCollision) {
                    s.velocity = vehicle.position().subtract(before);
                }
                vehicle.setDeltaMovement(Vec3.ZERO);
                vehicle.positionRider(player);
            }

            float yaw = FlightMath.toEuler(s.forward, s.up)[0];
            vehicle.setYRot(yaw);
            vehicle.setXRot(0);
            if (vehicle instanceof LivingEntity living) {
                living.yBodyRot = yaw;
                living.yHeadRot = yaw;
            }
            vehicle.fallDistance = 0;
            player.fallDistance = 0;

            Network.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> vehicle),
                    new FlightSyncPacket(vehicle.getId(), true, s.forward, s.up));
        }
    }

    private static void stop(UUID id, State s) {
        STATES.remove(id);
        Entity vehicle = s.vehicle;
        if (vehicle != null && !vehicle.isRemoved()) {
            vehicle.setNoGravity(s.prevNoGravity);
            vehicle.fallDistance = 0;
            Network.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> vehicle),
                    new FlightSyncPacket(vehicle.getId(), false, Vec3.ZERO, Vec3.ZERO));
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        State s = STATES.get(event.getPlayer().getUUID());
        if (s != null) stop(event.getPlayer().getUUID(), s);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        for (Map.Entry<UUID, State> e : new ArrayList<>(STATES.entrySet())) stop(e.getKey(), e.getValue());
    }
}
