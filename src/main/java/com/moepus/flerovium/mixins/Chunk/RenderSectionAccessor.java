package com.moepus.flerovium.mixins.Chunk;

import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = RenderSection.class, remap = false)
public interface RenderSectionAccessor {
    @Accessor
    boolean getBuilt();

    @Accessor
    int getFlags();

    @Accessor
    long getVisibilityData();
}
