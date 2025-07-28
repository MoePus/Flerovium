package com.moepus.flerovium.mixins.Particle;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ParticleEngine.class, remap = false)
public abstract class ParticleEngineMixin {
    @Redirect(method = "render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/culling/Frustum;isVisible(Lnet/minecraft/world/phys/AABB;)Z"))
    boolean FastFrustumCheck(Frustum instance, AABB aabb, @Local Particle particle) {
        if (particle.getBoundingBox().minX == Double.NEGATIVE_INFINITY) return true;

        float x = (float) (particle.x - instance.camX);
        float y = (float) (particle.y - instance.camY);
        float z = (float) (particle.z - instance.camZ);

        float width = particle.bbWidth;
        float height = particle.bbHeight;
        float diameter = Math.max(width, height);

        return instance.intersection.testSphere(x, y, z, diameter * 0.5f);
    }

    @Redirect(method = "render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;getRenderBoundingBox(F)Lnet/minecraft/world/phys/AABB;"))
    AABB skipGeneratingAABB(Particle instance, float partialTicks, @Local Particle particle) {
        return null;
    }
}
