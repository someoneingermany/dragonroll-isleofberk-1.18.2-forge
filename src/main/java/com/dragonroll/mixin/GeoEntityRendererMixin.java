package com.dragonroll.mixin;

import com.dragonroll.client.ModelRotation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * GeckoLib 3 (used by Isle of Berk) doesn't fire Forge's RenderLivingEvent, so hook its render
 * method directly. @Pseudo + require = 0: harmless if GeckoLib isn't installed.
 */
@Pseudo
@Mixin(targets = "software.bernie.geckolib3.renderers.geo.GeoEntityRenderer", remap = false)
public abstract class GeoEntityRendererMixin {
    private static final String RENDER =
            "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V";

    @Inject(method = RENDER, at = @At("HEAD"), remap = false, require = 0)
    private void dragonroll$tiltBegin(LivingEntity entity, float yaw, float partialTick, PoseStack poseStack,
                                      MultiBufferSource buffer, int light, CallbackInfo ci) {
        ModelRotation.begin(entity, partialTick, poseStack);
    }

    @Inject(method = RENDER, at = @At("RETURN"), remap = false, require = 0)
    private void dragonroll$tiltEnd(LivingEntity entity, float yaw, float partialTick, PoseStack poseStack,
                                    MultiBufferSource buffer, int light, CallbackInfo ci) {
        ModelRotation.end(entity, poseStack);
    }
}
