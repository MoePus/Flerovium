package com.moepus.flerovium.mixins.Particle;

import com.llamalad7.mixinextras.sugar.Local;
import com.moepus.flerovium.Flerovium;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

    @Inject(method = "destroy", at = @At("HEAD"), cancellable = true)
    void skipFarDestroy(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (!Flerovium.config.reduceTerrainParticles) return;

        Minecraft client = Minecraft.getInstance();
        Camera cam = client.gameRenderer.getMainCamera();

        Vec3 camPos = cam.getPosition();
        Vec3 blockPos = pos.getCenter();
        double dx = blockPos.x - camPos.x;
        double dy = blockPos.y - camPos.y;
        double dz = blockPos.z - camPos.z;
        double distSq = dx * dx + dy * dy + dz * dz;

        if (distSq > 64.0) {
            ci.cancel();
            return;
        }

        Vector3f look = cam.getLookVector();
        double len = Math.sqrt(distSq);
        float dot = (float) ((dx * look.x + dy * look.y + dz * look.z) / len);
        float threshold = Mth.lerp(
                Mth.clamp((float) len - 2, 0, 6) / 6.0f, 0.5f, 0.98f); // ≈ 60° - 10°

        if (dot < threshold) {
            ci.cancel();
        }
    }
}
