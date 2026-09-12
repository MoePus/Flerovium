package com.moepus.flerovium.mixins.Render;

import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import java.nio.ByteBuffer;

@Mixin(value = MeshData.class, remap = false)
public abstract class MeshDataMixin {
    @Unique
    private static final int FLEROVIUM_VERTICES_PER_QUAD = 4;

    /**
     * @author MoePus
     * @reason Read quad positions using byte offsets, including unaligned Iris vertex formats.
     */
    @Overwrite
    private static Vector3f[] unpackQuadCentroids(ByteBuffer buffer, int vertexCount, VertexFormat format) {
        int vertexStride = format.getVertexSize();
        int positionOffset = format.getOffset(VertexFormatElement.POSITION);
        if (positionOffset < 0) {
            throw new IllegalArgumentException("Cannot identify quad centers with no position element");
        }

        // Iris ENTITY vertices are 54 bytes. Vanilla's FloatBuffer indexing truncates this to 52,
        // corrupting the sort keys when Iris calls MeshData outside Sodium's BufferSource hook.
        Vector3f[] centroids = new Vector3f[vertexCount / FLEROVIUM_VERTICES_PER_QUAD];
        int quadStride = vertexStride * FLEROVIUM_VERTICES_PER_QUAD;
        int firstVertex = buffer.position() + positionOffset;
        for (int quad = 0; quad < centroids.length; quad++) {
            int oppositeVertex = firstVertex + vertexStride * 2;
            centroids[quad] = new Vector3f(
                    (buffer.getFloat(firstVertex) + buffer.getFloat(oppositeVertex)) * 0.5F,
                    (buffer.getFloat(firstVertex + Float.BYTES) + buffer.getFloat(oppositeVertex + Float.BYTES)) * 0.5F,
                    (buffer.getFloat(firstVertex + 2 * Float.BYTES) + buffer.getFloat(oppositeVertex + 2 * Float.BYTES)) * 0.5F);
            firstVertex += quadStride;
        }
        return centroids;
    }
}
