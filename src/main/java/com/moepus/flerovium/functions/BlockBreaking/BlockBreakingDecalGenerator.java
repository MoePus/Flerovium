package com.moepus.flerovium.functions.BlockBreaking;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.FastColor;
import org.joml.Vector3f;

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

    @Override
    public VertexConsumer setNormal(float normalX, float normalY, float normalZ) {
        this.delegate.setNormal(normalX, normalY, normalZ);
        Direction direction = Direction.getNearest(normalX, normalY, normalZ);
        float dx = this.x - this.relX;
        float dy = this.y - this.relY;
        float dz = this.z - this.relZ;

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

        delegate.setUv(u, v);
        return this;
    }

    @Override
    public void putBulkData(
            PoseStack.Pose pose,
            BakedQuad quad,
            float[] brightness,
            float red,
            float green,
            float blue,
            float alpha,
            int[] lightmap,
            int packedOverlay,
            boolean readAlpha
    ) {
        VertexBufferWriter writer = VertexBufferWriter.tryOf(delegate);
        if (writer == null) {
            VertexConsumer.super.putBulkData(
                    pose,
                    quad,
                    brightness,
                    FastColor.ARGB32.red(0xFFFFFFFF),
                    FastColor.ARGB32.green(0xFFFFFFFF),
                    FastColor.ARGB32.blue(0xFFFFFFFF),
                    FastColor.ARGB32.alpha(0xFFFFFFFF),
                    lightmap,
                    packedOverlay,
                    readAlpha
            );
        }
    }
}