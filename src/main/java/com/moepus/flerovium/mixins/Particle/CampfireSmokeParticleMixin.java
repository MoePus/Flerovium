package com.moepus.flerovium.mixins.Particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.CampfireSmokeParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(value = CampfireSmokeParticle.class, remap = false)
public abstract class CampfireSmokeParticleMixin extends Particle {
    protected CampfireSmokeParticleMixin(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
    }

    @ModifyArgs(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/particle/CampfireSmokeParticle;move(DDD)V"
            )
    )
    void beforeMove(Args args) {
        if (((ParticleAccessor) this).isStoppedByCollision()) {
            return;
        }
        if (age <= 2 || !this.hasPhysics) {
            BlockPos bottom = BlockPos.containing(this.x, this.y - 0.1, this.z);
            this.hasPhysics = !this.level.canSeeSky(bottom);
        }
    }
}
