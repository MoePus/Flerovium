package com.moepus.flerovium.mixins.Chunk;

import com.moepus.flerovium.functions.Chunk.FastSimpleFrustum;
import me.jellysquid.mods.sodium.client.render.viewport.frustum.SimpleFrustum;
import org.joml.FrustumIntersection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SimpleFrustum.class, remap = false)
public abstract class SimpleFrustumMixin {
    @Unique
    FastSimpleFrustum flerovium$fastFrustum;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(FrustumIntersection frustumIntersection, CallbackInfo ci) {
        flerovium$fastFrustum = new FastSimpleFrustum(this);
    }

    /**
     * @author MoePus
     * @reason Faster AABB test
     */
    @Overwrite
    public boolean testAab(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        return flerovium$fastFrustum.testCubeQuick(minX + maxX, minY + maxY, minZ + maxZ);
    }
}
