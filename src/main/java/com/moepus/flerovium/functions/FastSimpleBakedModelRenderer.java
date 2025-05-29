package com.moepus.flerovium.functions;

import com.mojang.blaze3d.vertex.PoseStack;
import me.jellysquid.mods.sodium.client.model.color.interop.ItemColorsExtended;
import me.jellysquid.mods.sodium.client.model.quad.BakedQuadView;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil;
import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.common.ModelVertex;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.util.List;

import static com.moepus.flerovium.functions.MathUtil.*;

public class FastSimpleBakedModelRenderer {
    private static final MemoryStack STACK = MemoryStack.create();
    private static final int VERTEX_COUNT = 4;
    private static final int BUFFER_VERTEX_COUNT = 48;
    private static final int STRIDE = 8;
    private static final long SCRATCH_BUFFER = MemoryUtil.nmemAlignedAlloc(64, BUFFER_VERTEX_COUNT * ModelVertex.STRIDE);
    private static long BUFFER_PTR = SCRATCH_BUFFER;
    private static int BUFFED_VERTEX = 0;
    private static int LAST_TINT_INDEX = -1;
    private static int LAST_TINT = -1;

    public static int multiplyIntBytes(int a, int b) {
        int first = ((a & 0xff) * (b & 0xff) + 127) / 255;
        int second = (((a >>> 8) & 0xff) * ((b >>> 8) & 0xff) + 127) / 255;
        int third = (((a >>> 16) & 0xff) * ((b >>> 16) & 0xff) + 127) / 255;
        return first | second << 8 | third << 16 | (b & 0xff000000);
    }

    private static void flush(VertexBufferWriter writer) {
        if (BUFFED_VERTEX == 0) return;
        STACK.push();
        writer.push(STACK, SCRATCH_BUFFER, BUFFED_VERTEX, ModelVertex.FORMAT);
        STACK.pop();
        BUFFER_PTR = SCRATCH_BUFFER;
        BUFFED_VERTEX = 0;
    }

    private static boolean isBufferMax() {
        return BUFFED_VERTEX >= BUFFER_VERTEX_COUNT;
    }

    // Check for functional storage, they are doing rotation by mul the pose only but not normal.
    // Sign for X axis should be the same.
    private static boolean checkNormalRotateEqual(PoseStack.Pose pose) {
        return ((Float.floatToRawIntBits(pose.pose().m00()) ^ Float.floatToRawIntBits(pose.normal().m00())) >> 31) +
                ((Float.floatToRawIntBits(pose.pose().m10()) ^ Float.floatToRawIntBits(pose.normal().m10())) >> 31) +
                ((Float.floatToRawIntBits(pose.pose().m20()) ^ Float.floatToRawIntBits(pose.normal().m20())) >> 31)
                == 0;
    }

    private static void putBulkData(VertexBufferWriter writer, PoseStack.Pose pose, BakedQuad bakedQuad,
                                    int light, int overlay, int color, int faces) {
        int[] vertices = bakedQuad.getVertices();
        if (vertices.length != VERTEX_COUNT * STRIDE) return;
        Matrix4f pose_matrix = pose.pose();
        int baked_normal = vertices[7];
        float unpackedX = NormI8.unpackX(baked_normal);
        float unpackedY = NormI8.unpackY(baked_normal);
        float unpackedZ = NormI8.unpackZ(baked_normal);
        float nx = MatrixHelper.transformNormalX(pose.normal(), unpackedX, unpackedY, unpackedZ);
        float ny = MatrixHelper.transformNormalY(pose.normal(), unpackedX, unpackedY, unpackedZ);
        float nz = MatrixHelper.transformNormalZ(pose.normal(), unpackedX, unpackedY, unpackedZ);

        float x = Float.intBitsToFloat(vertices[0]), y = Float.intBitsToFloat(vertices[1]), z = Float.intBitsToFloat(vertices[2]);
        float pos0_x = MatrixHelper.transformPositionX(pose_matrix, x, y, z);
        float pos0_y = MatrixHelper.transformPositionY(pose_matrix, x, y, z);
        float pos0_z = MatrixHelper.transformPositionZ(pose_matrix, x, y, z);
        x = Float.intBitsToFloat(vertices[STRIDE * 2]);
        y = Float.intBitsToFloat(vertices[STRIDE * 2 + 1]);
        z = Float.intBitsToFloat(vertices[STRIDE * 2 + 2]);

        float pos2_x = MatrixHelper.transformPositionX(pose_matrix, x, y, z);
        float pos2_y = MatrixHelper.transformPositionY(pose_matrix, x, y, z);
        float pos2_z = MatrixHelper.transformPositionZ(pose_matrix, x, y, z);

        if ((faces & 0b1000000) != 0) { // Backface culling
            if (checkNormalRotateEqual(pose))
                if ((pos0_x + pos2_x) * nx + (pos0_y + pos2_y) * ny + (pos0_z + pos2_z) * nz > 0)
                    if (((BakedQuadView) bakedQuad).getNormalFace() != ModelQuadFacing.UNASSIGNED)
                        return;
        }
        int n = packSafe(nx, ny, nz);

        x = Float.intBitsToFloat(vertices[STRIDE]);
        y = Float.intBitsToFloat(vertices[STRIDE + 1]);
        z = Float.intBitsToFloat(vertices[STRIDE + 2]);
        float pos1_x = MatrixHelper.transformPositionX(pose_matrix, x, y, z);
        float pos1_y = MatrixHelper.transformPositionY(pose_matrix, x, y, z);
        float pos1_z = MatrixHelper.transformPositionZ(pose_matrix, x, y, z);

        x = Float.intBitsToFloat(vertices[STRIDE * 3]);
        y = Float.intBitsToFloat(vertices[STRIDE * 3 + 1]);
        z = Float.intBitsToFloat(vertices[STRIDE * 3 + 2]);
        float pos3_x = MatrixHelper.transformPositionX(pose_matrix, x, y, z);
        float pos3_y = MatrixHelper.transformPositionY(pose_matrix, x, y, z);
        float pos3_z = MatrixHelper.transformPositionZ(pose_matrix, x, y, z);

        final int c = color != -1 ? multiplyIntBytes(color, vertices[3]):vertices[3];
        final int baked = vertices[6];
        final int l = Math.max(((baked & 0xffff) << 16) | (baked >> 16), light);
        ModelVertex.write(BUFFER_PTR, pos0_x, pos0_y, pos0_z, c, Float.intBitsToFloat(vertices[4]),
                Float.intBitsToFloat(vertices[5]), overlay, l, n);
        BUFFER_PTR += ModelVertex.STRIDE;
        ModelVertex.write(BUFFER_PTR, pos1_x, pos1_y, pos1_z, c, Float.intBitsToFloat(vertices[STRIDE + 4]),
                Float.intBitsToFloat(vertices[STRIDE + 5]), overlay, l, n);
        BUFFER_PTR += ModelVertex.STRIDE;
        ModelVertex.write(BUFFER_PTR, pos2_x, pos2_y, pos2_z, c, Float.intBitsToFloat(vertices[STRIDE * 2 + 4]),
                Float.intBitsToFloat(vertices[STRIDE * 2 + 5]), overlay, l, n);
        BUFFER_PTR += ModelVertex.STRIDE;
        ModelVertex.write(BUFFER_PTR, pos3_x, pos3_y, pos3_z, c, Float.intBitsToFloat(vertices[STRIDE * 3 + 4]),
                Float.intBitsToFloat(vertices[STRIDE * 3 + 5]), overlay, l, n);
        BUFFER_PTR += ModelVertex.STRIDE;

        BUFFED_VERTEX += VERTEX_COUNT;
        if (isBufferMax()) flush(writer);
    }

    public static int GetItemTint(int tintIndex, ItemStack itemStack, ItemColor colorProvider) {
        if (tintIndex == LAST_TINT_INDEX) return LAST_TINT;
        int tint = colorProvider.getColor(itemStack, tintIndex);
        LAST_TINT = ColorARGB.toABGR(tint, 255);
        LAST_TINT_INDEX = tintIndex;
        return LAST_TINT;
    }

    private static void renderQuadList(PoseStack.Pose pose, VertexBufferWriter writer, int faces, List<BakedQuad> bakedQuads,
                                       int light, int overlay, ItemStack itemStack, ItemColor colorProvider) {
        for (BakedQuad bakedQuad : bakedQuads) {
            if ((faces & (1 << bakedQuad.getDirection().ordinal())) == 0) continue;
            int color = colorProvider != null && bakedQuad.getTintIndex() != -1 ? GetItemTint(bakedQuad.getTintIndex(), itemStack, colorProvider):-1;
            putBulkData(writer, pose, bakedQuad, light, overlay, color, faces);
        }
        if (pose.pose().m32() > -8.0F) { // Do animation for item in GUI or nearby in world
            for (BakedQuad bakedQuad : bakedQuads) {
                SpriteUtil.markSpriteActive(bakedQuad.getSprite());
            }
        }
    }

    public static void render(SimpleBakedModel model, int faces, ItemStack itemStack, int packedLight, int packedOverlay,
                              PoseStack poseStack, VertexBufferWriter writer, ItemColors itemColors) {
        PoseStack.Pose pose = poseStack.last();
        ItemColor colorProvider = !itemStack.isEmpty() ? ((ItemColorsExtended) itemColors).sodium$getColorProvider(itemStack):null;

        LAST_TINT_INDEX = LAST_TINT = -1;
        for (Direction direction : Direction.values()) {
            renderQuadList(pose, writer, faces, model.getQuads(null, direction, null), packedLight, packedOverlay, itemStack, colorProvider);
        }
        renderQuadList(pose, writer, faces, model.getQuads(null, null, null), packedLight, packedOverlay, itemStack, colorProvider);

        flush(writer);
    }
}
