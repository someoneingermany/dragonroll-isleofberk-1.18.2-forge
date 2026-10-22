package com.dragonroll.client;

import com.dragonroll.DragonRoll;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Model tilt for dragons/riders drawn by normal (non-GeckoLib) renderers. */
@Mod.EventBusSubscriber(modid = DragonRoll.MODID, value = Dist.CLIENT)
public final class RenderRollHandler {
    private RenderRollHandler() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPre(RenderLivingEvent.Pre<?, ?> event) {
        ModelRotation.begin(event.getEntity(), event.getPartialTick(), event.getPoseStack());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPost(RenderLivingEvent.Post<?, ?> event) {
        ModelRotation.end(event.getEntity(), event.getPoseStack());
    }
}
