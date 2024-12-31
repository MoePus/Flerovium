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
import org.joml.Math;
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
    private int flerovium$getLightColorCached(float pt, Camera camera) {
        if (camera.getEntity() == null)
            return getLightColor(pt);
        int tickCount = camera.getEntity().tickCount;
        if (tickCount == 0)
            return getLightColor(pt);
        if (tickCount == flerovium$lastTick) {
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

        float minU = this.getU0();
        float maxU = this.getU1();
        float minV = this.getV0();
        float maxV = this.getV1();
        int light = flerovium$getLightColorCached(pt, camera);
        float size = this.getQuadSize(pt);
        int color = ColorABGR.pack(this.rCol, this.gCol, this.bCol, this.alpha);

        float lx = camera.getLeftVector().x, ly = camera.getLeftVector().y, lz = camera.getLeftVector().z;
        float ux = camera.getUpVector().x, uy = camera.getUpVector().y, uz = camera.getUpVector().z;
        if (roll != 0) {
            // Manually rotate the left and up vectors by the roll angle
            float nroll = Mth.lerp(pt, this.oRoll, this.roll);
            float sinRoll = Math.sin(nroll);
            float cosRoll = Math.cosFromSin(sinRoll, nroll);

            float rv1x = Math.fma(cosRoll, lx, sinRoll * ux),
                    rv1y = Math.fma(cosRoll, ly, sinRoll * uy),
                    rv1z = Math.fma(cosRoll, lz, sinRoll * uz);
            float rv2x = Math.fma(-sinRoll, lx, cosRoll * ux),
                    rv2y = Math.fma(-sinRoll, ly, cosRoll * uy),
                    rv2z = Math.fma(-sinRoll, lz, cosRoll * uz);
            lx = rv1x;
            ly = rv1y;
            lz = rv1z;
            ux = rv2x;
            uy = rv2y;
            uz = rv2z;
        }

        /**
         * Constructs a sprite's four vertices using the camera's leftVector and upVector.
         *
         *                  +---------------------+
         *                  |                     |
         *     (-left, +up) |         +up         | (+left, +up)
         *                  |                     |
         *          Vertex 2|          ^          | Vertex 3
         *                  |          |          |
         *                  |   <------+------>   |
         *                  |       left          |
         *                  |                     |
         *     (-left, -up) |         -up         | (+left, -up)
         *                  |                     |
         *          Vertex 1|                     | Vertex 4
         *                  +---------------------+
         *
         * The sprite lies in a plane defined by the camera's orientation. Using the
         * size of the sprite (S), leftVector (L), and upVector (U), we calculate:
         *
         * Vertex 1: (L + U) * -S
         * Vertex 2: (-L + U) * S
         * Vertex 3: (L + U) * S
         * Vertex 4: (-L + U) * -S
         *
         * Each vertex is then transformed to the world position of the sprite
         * by adding the sprite's position (P).
         *
         * This approach avoids expensive quaternion operations and directly
         * leverages the camera's orientation to efficiently calculate vertices.
         */
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long buffer = stack.nmalloc(4 * ParticleVertex.STRIDE);
            long ptr = buffer;

            ParticleVertex.put(ptr, Math.fma(lx + ux, -size, x), Math.fma(ly + uy, -size, y), Math.fma(lz + uz, -size, z), maxU, maxV, color, light);
            ptr += ParticleVertex.STRIDE;

            ParticleVertex.put(ptr, Math.fma(-lx + ux, size, x), Math.fma(-ly + uy, size, y), Math.fma(-lz + uz, size, z), maxU, minV, color, light);
            ptr += ParticleVertex.STRIDE;

            ParticleVertex.put(ptr, Math.fma(lx + ux, size, x), Math.fma(ly + uy, size, y), Math.fma(lz + uz, size, z), minU, minV, color, light);
            ptr += ParticleVertex.STRIDE;

            ParticleVertex.put(ptr, Math.fma(-lx + ux, -size, x), Math.fma(-ly + uy, -size, y), Math.fma(-lz + uz, -size, z), minU, maxV, color, light);

            writer.push(stack, buffer, 4, ParticleVertex.FORMAT);
        }
    }
}
