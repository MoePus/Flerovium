package com.moepus.flerovium.mixins.Particle;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Particle.class, remap = false)
public interface ParticleAccessor {
    @Accessor("stoppedByCollision")
    boolean isStoppedByCollision();
}
