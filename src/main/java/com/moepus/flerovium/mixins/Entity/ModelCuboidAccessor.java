package com.moepus.flerovium.mixins.Entity;

import net.caffeinemc.mods.sodium.client.render.immediate.model.ModelCuboid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ModelCuboid.class, remap = false)
public interface ModelCuboidAccessor {
    @Accessor
    int getCullMask();
}
