package com.dragonroll;

import com.dragonroll.network.Network;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(DragonRoll.MODID)
public class DragonRoll {
    public static final String MODID = "dragonroll";

    public DragonRoll() {
        ModLoadingContext ctx = ModLoadingContext.get();
        ctx.registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC);
        ctx.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
        // Works client-only too, so don't mark servers without it as incompatible.
        ctx.registerExtensionPoint(IExtensionPoint.DisplayTest.class, IExtensionPoint.DisplayTest.IGNORE_SERVER_VERSION);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
    }

    private void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(Network::register);
    }
}
