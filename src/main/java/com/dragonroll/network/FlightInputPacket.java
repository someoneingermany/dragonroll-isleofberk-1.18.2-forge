package com.dragonroll.network;

import com.dragonroll.server.ServerFlightHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: the rider's flight orientation and wing inputs, sent every tick while flying. */
public record FlightInputPacket(boolean active, boolean serverMove, boolean thrust, boolean brake, boolean flap,
                                Vec3 forward, Vec3 up) {

    public static void encode(FlightInputPacket p, FriendlyByteBuf buf) {
        int flags = (p.active ? 1 : 0) | (p.serverMove ? 2 : 0) | (p.thrust ? 4 : 0) | (p.brake ? 8 : 0) | (p.flap ? 16 : 0);
        buf.writeByte(flags);
        buf.writeFloat((float) p.forward.x); buf.writeFloat((float) p.forward.y); buf.writeFloat((float) p.forward.z);
        buf.writeFloat((float) p.up.x); buf.writeFloat((float) p.up.y); buf.writeFloat((float) p.up.z);
    }

    public static FlightInputPacket decode(FriendlyByteBuf buf) {
        byte f = buf.readByte();
        Vec3 fw = new Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat());
        Vec3 up = new Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat());
        return new FlightInputPacket((f & 1) != 0, (f & 2) != 0, (f & 4) != 0, (f & 8) != 0, (f & 16) != 0, fw, up);
    }

    public static void handle(FlightInputPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) ServerFlightHandler.onInput(player, p);
        });
        ctx.get().setPacketHandled(true);
    }
}
