package com.dragonroll.network;

import com.dragonroll.DragonRoll;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public final class Network {
    private Network() {}

    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(DragonRoll.MODID, "main"), () -> PROTOCOL,
            NetworkRegistry.acceptMissingOr(PROTOCOL), NetworkRegistry.acceptMissingOr(PROTOCOL));

    public static void register() {
        CHANNEL.registerMessage(0, FlightInputPacket.class, FlightInputPacket::encode, FlightInputPacket::decode,
                FlightInputPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(1, FlightSyncPacket.class, FlightSyncPacket::encode, FlightSyncPacket::decode,
                FlightSyncPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
}
