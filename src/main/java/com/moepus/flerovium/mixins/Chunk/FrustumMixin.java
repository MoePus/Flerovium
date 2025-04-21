package com.moepus.flerovium.mixins.Chunk;

import com.moepus.flerovium.functions.Chunk.FastSimpleFrustum;
import me.jellysquid.mods.sodium.client.render.viewport.Viewport;
import me.jellysquid.mods.sodium.client.render.viewport.ViewportProvider;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.FrustumIntersection;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Frustum.class)
public class FrustumMixin implements ViewportProvider {
    @Shadow
    public double camX;

    @Shadow
    public double camY;

    @Shadow
    public double camZ;

    @Shadow
    @Final
    public FrustumIntersection intersection;

    @Override
    public Viewport sodium$createViewport() {
        return new Viewport(new FastSimpleFrustum(this.intersection), new Vector3d(this.camX, this.camY, this.camZ));
    }
}
