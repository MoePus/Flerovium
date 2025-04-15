package com.moepus.flerovium.mixins.Chunk;

import me.jellysquid.mods.sodium.client.render.viewport.frustum.SimpleFrustum;
import org.joml.FrustumIntersection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = SimpleFrustum.class, remap = false)
public interface SimpleFrustumAccessor {
    @Accessor
    FrustumIntersection getFrustum();
}
