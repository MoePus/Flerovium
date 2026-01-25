package com.moepus.flerovium.functions.BlockBreaking;

import com.mojang.blaze3d.vertex.*;
import me.jellysquid.mods.sodium.client.render.vertex.buffer.SodiumBufferBuilder;
import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import me.jellysquid.mods.sodium.client.model.quad.BakedQuadView;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.FastColor;
import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

public class BlockBreakingDecalGenerator extends DefaultedVertexConsumer {
    private final VertexConsumer delegate;

    public BlockBreakingDecalGenerator(VertexConsumer delegate) {
        this.delegate = delegate;
    }

    static Vector3f calcUV(float normalX, float normalY, float normalZ, float dx, float dy, float dz) {
        Direction direction = Direction.getNearest(normalX, normalY, normalZ);
        float u, v;
        switch (direction) {
            case DOWN -> {
                u = dx;
                v = -dz;
            }
            case UP -> {
                u = dx;
                v = dz;
            }
            case NORTH -> {
                u = -dx;
                v = -dy;
            }
            case SOUTH -> {
                u = dx;
                v = -dy;
            }
            case WEST -> {
                u = dz;
                v = -dy;
            }
            case EAST -> {
                u = -dz;
                v = -dy;
            }
            default -> {
                u = 0;
                v = 0;
            }
        }
        return new Vector3f(u, v, 0);
    }

    static void putBulkDataSodium(
            VertexBufferWriter writer,
            PoseStack.Pose pose,
            BakedQuad bakedQuad,
            int[] lightmap
    ) {
        BakedQuadView quad = (BakedQuadView) bakedQuad;
        Matrix3f matNormal = pose.normal();
        Matrix4f matPosition = pose.pose();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            long buffer = stack.nmalloc(4 * BlockVertex.STRIDE);
            long ptr = buffer;

            for (int i = 0; i < 4; i++) {
                float x = quad.getX(i);
                float y = quad.getY(i);
                float z = quad.getZ(i);

                int bakedLight = quad.getLight(i);
                int light = lightmap[i];
                int newLight = Math.max(((bakedLight & 0xffff) << 16) | (bakedLight >> 16), light);

                int rawNormal = quad.getComputedFaceNormal();
                int normal = MatrixHelper.transformNormal(matNormal, rawNormal);
                float nx = NormI8.unpackX(rawNormal);
                float ny = NormI8.unpackY(rawNormal);
                float nz = NormI8.unpackZ(rawNormal);

                Vector3f uv = calcUV(nx, ny, nz, x, y, z);

                float xt = MatrixHelper.transformPositionX(matPosition, x, y, z);
                float yt = MatrixHelper.transformPositionY(matPosition, x, y, z);
                float zt = MatrixHelper.transformPositionZ(matPosition, x, y, z);

                BlockVertex.write(ptr, xt, yt, zt, -1,
                        uv.x, uv.y,
                        newLight, normal);
                ptr += BlockVertex.STRIDE;
            }

            writer.push(stack, buffer, 4, BlockVertex.FORMAT);
        }
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        this.delegate.vertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer color(int i, int i1, int i2, int i3) {
        this.delegate.color(-1);
        return this;
    }

    @Override
    public VertexConsumer uv(float u, float v) {
        this.delegate.uv(u, v);
        return this;
    }

    @Override
    public VertexConsumer overlayCoords(int i, int j) {
        this.delegate.overlayCoords(i, j);
        return this;
    }

    @Override
    public VertexConsumer uv2(int i, int j) {
        this.delegate.uv2(i, j);
        return this;
    }

    @Override
    public VertexConsumer normal(float normalX, float normalY, float normalZ) {
        this.delegate.normal(normalX, normalY, normalZ);
        return this;
    }

    @Override
    public void endVertex() {
        delegate.endVertex();
    }

    @Override
    public void putBulkData(
            PoseStack.Pose pose,
            BakedQuad bakedQuad,
            float[] brightness,
            float red,
            float green,
            float blue,
            float alpha,
            int[] lightmap,
            int packedOverlay,
            boolean readAlpha
    ) {
        if (bakedQuad.getVertices().length < 32) {
            return;
        }
        VertexBufferWriter writer = VertexBufferWriter.tryOf(delegate);
        if (writer == null) {
            super.putBulkData(
                    pose,
                    bakedQuad,
                    brightness,
                    FastColor.ARGB32.red(0xFFFFFFFF),
                    FastColor.ARGB32.green(0xFFFFFFFF),
                    FastColor.ARGB32.blue(0xFFFFFFFF),
                    FastColor.ARGB32.alpha(0xFFFFFFFF),
                    lightmap,
                    packedOverlay,
                    readAlpha
            );
            return;
        }
        if (delegate instanceof BufferBuilder bb) {
            if (bb.format == DefaultVertexFormat.BLOCK) {
                putBulkDataSodium(writer, pose, bakedQuad, lightmap);
            } else {
                BlockBreakingDecalGeneratorIris.putBulkDataIris(writer, pose, bakedQuad, lightmap);
            }
        } else if (delegate instanceof SodiumBufferBuilder sbb) {
            if (sbb.getOriginalBufferBuilder().format == DefaultVertexFormat.BLOCK) {
                putBulkDataSodium(writer, pose, bakedQuad, lightmap);
            }
        }
    }
}