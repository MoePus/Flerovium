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

        float width = (float) Math.max((aabb.maxX - aabb.minX), (aabb.maxZ - aabb.minZ));
        float height = (float) (aabb.maxY - aabb.minY);
        float max = Math.max(width, height);
        float min = Math.min(width, height);

        float radius = (max + 0.4142f * min) * 0.5f;
        return instance.intersection.testSphere(x, y, z, radius);
    }
}
