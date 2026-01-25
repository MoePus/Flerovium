package com.moepus.flerovium.functions.BlockBreaking;

import com.moepus.flerovium.Iris.IrisTerrainVertex;
import com.mojang.blaze3d.vertex.PoseStack;
import me.jellysquid.mods.sodium.client.model.quad.BakedQuadView;
import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.minecraft.client.renderer.block.model.BakedQuad;
import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import static com.moepus.flerovium.functions.BlockBreaking.BlockBreakingDecalGenerator.calcUV;

public class BlockBreakingDecalGeneratorIris {

    static void putBulkDataIris(
            VertexBufferWriter writer,
            PoseStack.Pose pose,
            BakedQuad bakedQuad,
            int[] lightmap
    ) {
        BakedQuadView quad = (BakedQuadView) bakedQuad;
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

                int rawNormal = quad.getComputedFaceNormal();
                int normal = MatrixHelper.transformNormal(matNormal, rawNormal);
                float nx = NormI8.unpackX(rawNormal);
                float ny = NormI8.unpackY(rawNormal);
                float nz = NormI8.unpackZ(rawNormal);

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

}
