package com.moepus.flerovium.mixins.Chunk;

import com.moepus.flerovium.functions.Chunk.FastSimpleFrustum;
import com.moepus.flerovium.functions.Chunk.FastViewport;
import me.jellysquid.mods.sodium.client.render.viewport.CameraTransform;
import me.jellysquid.mods.sodium.client.render.viewport.Viewport;
import me.jellysquid.mods.sodium.client.render.viewport.frustum.Frustum;
import me.jellysquid.mods.sodium.client.render.viewport.frustum.SimpleFrustum;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Viewport.class)
public abstract class ViewportMixin implements FastViewport {
    @Unique
    private FastSimpleFrustum flerovium$frustum = null;

    @Shadow
    @Final
    private CameraTransform transform;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(Frustum frustum, Vector3d position, CallbackInfo ci) {
        if (frustum instanceof SimpleFrustum simpleFrustum) {
            this.flerovium$frustum = new FastSimpleFrustum(simpleFrustum);
        } else {
            throw new IllegalStateException("Viewport frustum is not a SimpleFrustum");
        }
    }

    @Override
    public boolean isSectionVisible(int intOriginX, int intOriginY, int intOriginZ) {
        float floatOriginX = (float)(intOriginX - this.transform.intX) - this.transform.fracX;
        float floatOriginY = (float)(intOriginY - this.transform.intY) - this.transform.fracY;
        float floatOriginZ = (float)(intOriginZ - this.transform.intZ) - this.transform.fracZ;
        return flerovium$frustum.testCubeQuick(floatOriginX, floatOriginY, floatOriginZ);
    }
}
