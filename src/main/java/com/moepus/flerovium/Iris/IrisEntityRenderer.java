package com.moepus.flerovium.Iris;

import com.moepus.flerovium.Flerovium;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.client.render.immediate.model.ModelCuboid;
import net.caffeinemc.mods.sodium.client.util.Int2;
import org.joml.Vector2f;
import net.irisshaders.iris.vertices.NormalHelper;

public class IrisEntityRenderer {
    private static final Vector2f uv0 = new Vector2f();
    private static final Vector2f uv1 = new Vector2f();
    private static final Vector2f uv2 = new Vector2f();
    private static final Vector2f uv3 = new Vector2f();

    public static int emitQuads(long buffer, ModelCuboid cuboid, long packedOverlayLight, int cullMask, long[] CUBE_VERTEX_XY, long[] CUBE_VERTEX_ZW, int[] CUBE_FACE_NORMAL) {
        long ptr = buffer;
        int[] normals = cuboid.normals;
        int[] positions = cuboid.positions;
        long[] textures = cuboid.textures;
        int vertexCount = 0;

        for (int faceIndex = 0; faceIndex < 6; ++faceIndex) {
            if ((cullMask & (1 << faceIndex)) == 0) {
                continue;
            }
            final int elementOffset = faceIndex * 4;
            int packedNormal = CUBE_FACE_NORMAL[normals[faceIndex]];
            unpackUV(textures[elementOffset + 0], uv0);
            unpackUV(textures[elementOffset + 1], uv1);
            unpackUV(textures[elementOffset + 2], uv2);
            unpackUV(textures[elementOffset + 3], uv3);
            float mid_u = (uv0.x + uv1.x + uv2.x + uv3.x) / 4;
            float mid_v = (uv0.y + uv1.y + uv2.y + uv3.y) / 4;
            long midUV = Int2.pack(Float.floatToRawIntBits(mid_u), Float.floatToRawIntBits(mid_v));

            int tangent = Flerovium.config.skipEntityTangentCompute ? -1 : computeTangent(packedNormal, positions, CUBE_VERTEX_XY, CUBE_VERTEX_ZW);

            int position = positions[elementOffset + 0];
            IrisEntityVertex.write(ptr, CUBE_VERTEX_XY[position], CUBE_VERTEX_ZW[position], textures[elementOffset + 0], packedOverlayLight, packedNormal, midUV, tangent);
            ptr += IrisEntityVertex.STRIDE;

            position = positions[elementOffset + 1];
            IrisEntityVertex.write(ptr, CUBE_VERTEX_XY[position], CUBE_VERTEX_ZW[position], textures[elementOffset + 1], packedOverlayLight, packedNormal, midUV, tangent);
            ptr += IrisEntityVertex.STRIDE;

            position = positions[elementOffset + 2];
            IrisEntityVertex.write(ptr, CUBE_VERTEX_XY[position], CUBE_VERTEX_ZW[position], textures[elementOffset + 2], packedOverlayLight, packedNormal, midUV, tangent);
            ptr += IrisEntityVertex.STRIDE;

            position = positions[elementOffset + 3];
            IrisEntityVertex.write(ptr, CUBE_VERTEX_XY[position], CUBE_VERTEX_ZW[position], textures[elementOffset + 3], packedOverlayLight, packedNormal, midUV, tangent);
            ptr += IrisEntityVertex.STRIDE;

            vertexCount += 4;
        }

        return vertexCount;
    }

    private static void unpackUV(long uv, Vector2f out) {
        out.x = Float.intBitsToFloat((int) (uv & 0xFFFFFFFFL));
        out.y = Float.intBitsToFloat((int) ((uv >> 32) & 0xFFFFFFFFL));
    }

    private static int computeTangent(int packedNormal, int[] positions, long[] CUBE_VERTEX_XY, long[] CUBE_VERTEX_ZW) {
        float unpackedX = NormI8.unpackX(packedNormal);
        float unpackedY = NormI8.unpackY(packedNormal);
        float unpackedZ = NormI8.unpackZ(packedNormal);
        float pos0x = Float.intBitsToFloat((int) (CUBE_VERTEX_XY[positions[0]] & 0xFFFFFFFFL));
        float pos0y = Float.intBitsToFloat((int) ((CUBE_VERTEX_XY[positions[0]] >> 32) & 0xFFFFFFFFL));
        float pos0z = Float.intBitsToFloat((int) (CUBE_VERTEX_ZW[positions[0]] & 0xFFFFFFFFL));

        float pos1x = Float.intBitsToFloat((int) (CUBE_VERTEX_XY[positions[1]] & 0xFFFFFFFFL));
        float pos1y = Float.intBitsToFloat((int) ((CUBE_VERTEX_XY[positions[1]] >> 32) & 0xFFFFFFFFL));
        float pos1z = Float.intBitsToFloat((int) (CUBE_VERTEX_ZW[positions[1]] & 0xFFFFFFFFL));

        float pos2x = Float.intBitsToFloat((int) (CUBE_VERTEX_XY[positions[2]] & 0xFFFFFFFFL));
        float pos2y = Float.intBitsToFloat((int) ((CUBE_VERTEX_XY[positions[2]] >> 32) & 0xFFFFFFFFL));
        float pos2z = Float.intBitsToFloat((int) (CUBE_VERTEX_ZW[positions[2]] & 0xFFFFFFFFL));

        return NormalHelper.computeTangent(unpackedX, unpackedY, unpackedZ, pos0x, pos0y, pos0z, uv0.x, uv0.y, pos1x, pos1y, pos1z, uv1.x, uv1.y, pos2x, pos2y, pos2z, uv2.x, uv2.y);
    }
}
