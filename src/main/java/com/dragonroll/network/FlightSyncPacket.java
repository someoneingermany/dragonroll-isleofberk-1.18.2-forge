package com.dragonroll.network;

import com.dragonroll.SyncStore;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> clients: a flying dragon's orientation, so other players see it roll too. */
public record FlightSyncPacket(int entityId, boolean active, Vec3 forward, Vec3 up) {

    public static void encode(FlightSyncPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.entityId);
        buf.writeBoolean(p.active);
        buf.writeFloat((float) p.forward.x); buf.writeFloat((float) p.forward.y); buf.writeFloat((float) p.forward.z);
        buf.writeFloat((float) p.up.x); buf.writeFloat((float) p.up.y); buf.writeFloat((float) p.up.z);
    }

    public static FlightSyncPacket decode(FriendlyByteBuf buf) {
        int id = buf.readVarInt();
        boolean active = buf.readBoolean();
        Vec3 fw = new Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat());
        Vec3 up = new Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat());
        return new FlightSyncPacket(id, active, fw, up);
    }

    public static void handle(FlightSyncPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> SyncStore.apply(p.entityId, p.active, p.forward, p.up));
        ctx.get().setPacketHandled(true);
    }
}
