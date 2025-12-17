package com.moepus.flerovium.functions.BlockBreaking;

import com.moepus.flerovium.Iris.IrisTerrainVertex;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import net.caffeinemc.mods.sodium.api.util.ColorMixer;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.common.EntityVertex;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.FastColor;
import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

public class BlockBreakingDecalGenerator implements VertexConsumer {
    private final VertexConsumer delegate;
    private final float relX;
    private final float relY;
    private final float relZ;
    private float x;
    private float y;
    private float z;

    public BlockBreakingDecalGenerator(VertexConsumer delegate, float relX, float blockY, float blockZ) {
        this.delegate = delegate;
        this.relX = relX;
        this.relY = blockY;
        this.relZ = blockZ;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.delegate.addVertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setColor(int red, int green, int blue, int alpha) {
        this.delegate.setColor(-1);
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        this.delegate.setUv1(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        this.delegate.setUv2(u, v);
        return this;
    }

    private static Vector3f calcUV(float normalX, float normalY, float normalZ, float dx, float dy, float dz) {
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

    @Override
    public VertexConsumer setNormal(float normalX, float normalY, float normalZ) {
        this.delegate.setNormal(normalX, normalY, normalZ);
        float dx = this.x - this.relX;
        float dy = this.y - this.relY;
        float dz = this.z - this.relZ;
        Vector3f uv = calcUV(normalX, normalY, normalZ, dx, dy, dz);

        delegate.setUv(uv.x, uv.y);
        return this;
    }

    void putBulkDataSodium(
            VertexBufferWriter writer,
            PoseStack.Pose pose,
            BakedQuad bakedQuad,
            int[] lightmap
    ) {
        ModelQuadView quad = (ModelQuadView) bakedQuad;
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

                int normal = MatrixHelper.transformNormal(matNormal, false, quad.getAccurateNormal(i));
                float nx = NormI8.unpackX(normal);
                float ny = NormI8.unpackY(normal);
                float nz = NormI8.unpackZ(normal);

                Vector3f uv = calcUV(nx, ny, nz, x, y, z);

                float xt = MatrixHelper.transformPositionX(matPosition, x, y, z);
                float yt = MatrixHelper.transformPositionY(matPosition, x, y, z);
                float zt = MatrixHelper.transformPositionZ(matPosition, x, y, z);

                BlockVertex.write(ptr, xt, yt, zt, -1,
                        Float.floatToIntBits(uv.x), Float.floatToIntBits(uv.y),
                        newLight, normal);
                ptr += BlockVertex.STRIDE;
            }

            writer.push(stack, buffer, 4, BlockVertex.FORMAT);
        }
    }

    void putBulkDataIris(
            VertexBufferWriter writer,
            PoseStack.Pose pose,
            BakedQuad bakedQuad,
            int[] lightmap
    ) {
        ModelQuadView quad = (ModelQuadView) bakedQuad;
        Matrix3f matNormal = pose.normal();
        Matrix4f matPosition = pose.pose();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            long buffer = stack.nmalloc(4 * IrisTerrainVertex.STRIDE);
            long ptr = buffer;

            for (int i = 0; i < 4; i++) {
                float x = quad.getX(i);
                float y = quad.getY(i);
                float z = quad.getZ(i);

                int bakedLight = quad.getLight(i);
                int light = lightmap[i];
                int newLight = Math.max(((bakedLight & 0xffff) << 16) | (bakedLight >> 16), light);

                int normal = MatrixHelper.transformNormal(matNormal, false, quad.getAccurateNormal(i));
                float nx = NormI8.unpackX(normal);
                float ny = NormI8.unpackY(normal);
                float nz = NormI8.unpackZ(normal);

                Vector3f uv = calcUV(nx, ny, nz, x, y, z);

                float xt = MatrixHelper.transformPositionX(matPosition, x, y, z);
                float yt = MatrixHelper.transformPositionY(matPosition, x, y, z);
                float zt = MatrixHelper.transformPositionZ(matPosition, x, y, z);

                IrisTerrainVertex.write(ptr, xt, yt, zt, -1,
                        uv.x, uv.y,
                        0, 0,
                        newLight, normal,
                        -1);
                ptr += IrisTerrainVertex.STRIDE;
            }

            writer.push(stack, buffer, 4, IrisTerrainVertex.FORMAT);
        }
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
            VertexConsumer.super.putBulkData(
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
        if (!(delegate instanceof BufferBuilder bb)) return;

        if (bb.format == BlockVertex.FORMAT) {
            putBulkDataSodium(writer, pose, bakedQuad, lightmap);
        } else {
            putBulkDataIris(writer, pose, bakedQuad, lightmap);
        }
    }
}