package com.dragonroll.client;

import com.dragonroll.DragonRoll;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = DragonRoll.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientModEvents {
    private ClientModEvents() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ClientRegistry.registerKeyBinding(ClientFlightHandler.TOGGLE);
    }
}
