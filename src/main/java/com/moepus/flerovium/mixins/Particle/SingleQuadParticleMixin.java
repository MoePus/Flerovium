package com.moepus.flerovium.mixins.Particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.common.ParticleVertex;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.lwjgl.system.MemoryStack;


@Mixin(SingleQuadParticle.class)
public abstract class SingleQuadParticleMixin extends Particle {
    @Shadow
    public abstract float getQuadSize(float pt);

    @Shadow
    protected abstract float getU0();

    @Shadow
    protected abstract float getU1();

    @Shadow
    protected abstract float getV0();

    @Shadow
    protected abstract float getV1();

    protected SingleQuadParticleMixin(ClientLevel p_107234_, double p_107235_, double p_107236_, double p_107237_) {
        super(p_107234_, p_107235_, p_107236_, p_107237_);
    }

    @Unique
    int flerovium$lastTick = 0;

    @Unique
    int flerovium$cachedLight = 0;

    @Unique
    private int flerovium$getLightColorCached(float pt, int tickCount) {
        if(tickCount == flerovium$lastTick) {
            return flerovium$cachedLight;
        }
        flerovium$lastTick = tickCount;
        flerovium$cachedLight = getLightColor(pt);
        return flerovium$cachedLight;
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void renderFast(VertexConsumer vertexConsumer, Camera camera, float pt, CallbackInfo ci) {
        VertexBufferWriter writer = VertexBufferWriter.tryOf(vertexConsumer);
        if (writer == null)
            return;
        ci.cancel();

        Vec3 camPos = camera.getPosition();
        float x = (float) (Mth.lerp(pt, this.xo, this.x) - camPos.x());
        float y = (float) (Mth.lerp(pt, this.yo, this.y) - camPos.y());
        float z = (float) (Mth.lerp(pt, this.zo, this.z) - camPos.z());
        Quaternionf q;
        if (this.roll == 0.0F) {
            q = camera.rotation();
        } else {
            q = new Quaternionf(camera.rotation());
            q.rotateZ(Mth.lerp(pt, this.oRoll, this.roll));
        }

        float minU = this.getU0();
        float maxU = this.getU1();
        float minV = this.getV0();
        float maxV = this.getV1();
        int light = flerovium$getLightColorCached(pt, camera.getEntity().tickCount);
        float size = this.getQuadSize(pt);
        int color = ColorABGR.pack(this.rCol, this.gCol, this.bCol, this.alpha);

        final float qxx = q.x * q.x, qyy = q.y * q.y, qzz = q.z * q.z, qww = q.w * q.w;
        final float qxy = q.x * q.y, qxz = q.x * q.z, qyz = q.y * q.z, qxw = q.x * q.w;
        final float qzw = q.z * q.w, qyw = q.y * q.w, qk = 1 / (qxx + qyy + qzz + qww);

        final float v1x = (qxx - qyy - qzz + qww) * qk;
        final float v1y = 2 * (qxy + qzw) * qk;
        final float v1z = 2 * (qxz - qyw) * qk;

        final float v2x = 2 * (qxy - qzw) * qk;
        final float v2y = (qyy - qxx - qzz + qww) * qk;
        final float v2z = 2 * (qyz + qxw) * qk;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            long buffer = stack.nmalloc(4 * ParticleVertex.STRIDE);
            long ptr = buffer;

            ParticleVertex.put(ptr, Math.fma(v1x + v2x, -size, x), Math.fma(v1y + v2y, -size, y), Math.fma(v1z + v2z, -size, z), maxU, maxV, color, light);
            ptr += ParticleVertex.STRIDE;

            ParticleVertex.put(ptr, Math.fma(-v1x + v2x, size, x), Math.fma(-v1y + v2y, size, y), Math.fma(-v1z + v2z, size, z), maxU, minV, color, light);
            ptr += ParticleVertex.STRIDE;

            ParticleVertex.put(ptr, Math.fma(v1x + v2x, size, x), Math.fma(v1y + v2y, size, y), Math.fma(v1z + v2z, size, z), minU, minV, color, light);
            ptr += ParticleVertex.STRIDE;

            ParticleVertex.put(ptr, Math.fma(-v1x + v2x, -size, x), Math.fma(-v1y + v2y, -size, y), Math.fma(-v1z + v2z, -size, z), minU, maxV, color, light);

            writer.push(stack, buffer, 4, ParticleVertex.FORMAT);
        }
    }

}