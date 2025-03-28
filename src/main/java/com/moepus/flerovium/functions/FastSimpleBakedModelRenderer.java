package com.moepus.flerovium.functions;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import net.caffeinemc.mods.sodium.api.texture.SpriteUtil;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.api.util.ColorMixer;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.client.model.quad.BakedQuadView;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import org.joml.Math;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.util.List;

import static com.moepus.flerovium.functions.MathUtil.*;

public class FastSimpleBakedModelRenderer {
    private static final MemoryStack STACK = MemoryStack.create();
    private static final int VERTEX_COUNT = 4;
    private static final int BUFFER_VERTEX_COUNT = 48;
    private static final int STRIDE = 8;
    private static final long SCRATCH_BUFFER = MemoryUtil.nmemAlignedAlloc(64, BUFFER_VERTEX_COUNT * EntityVertex.STRIDE);
    private static long BUFFER_PTR = SCRATCH_BUFFER;
    private static int BUFFED_VERTEX = 0;

    private static boolean isPerspectiveProjection() {
        return RenderSystem.modelViewMatrix.m32() == 0;
    }

    private static void flush(VertexBufferWriter writer) {
        if (BUFFED_VERTEX == 0) return;
        STACK.push();
        writer.push(STACK, SCRATCH_BUFFER, BUFFED_VERTEX, EntityVertex.FORMAT);
        STACK.pop();
        BUFFER_PTR = SCRATCH_BUFFER;
        BUFFED_VERTEX = 0;
    }

    private static boolean isBufferMax() {
        return BUFFED_VERTEX >= BUFFER_VERTEX_COUNT;
    }

    private static void putBulkData(VertexBufferWriter writer, PoseStack.Pose pose, BakedQuad bakedQuad, int light, int overlay, int color) {
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
        int n = packUnsafe(nx, ny, nz);

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

        if (isPerspectiveProjection()) {
            if ((pos0_x + pos2_x) * nx + (pos0_y + pos2_y) * ny + (pos0_z + pos2_z) * nz > 0) return;
        }
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

        final int c = color != -1 ? ColorMixer.mulComponentWise(color, vertices[3]) : vertices[3];
        final int baked = vertices[6];
        final int l = Math.max(((baked & 0xffff) << 16) | (baked >> 16), light);
        long WRITE_PTR = BUFFER_PTR;
        EntityVertex.write(WRITE_PTR, pos0_x, pos0_y, pos0_z, c, vertices[4], vertices[5], overlay, l, n);
        WRITE_PTR += EntityVertex.STRIDE;
        EntityVertex.write(WRITE_PTR, pos1_x, pos1_y, pos1_z, c, vertices[STRIDE + 4], vertices[STRIDE + 5], overlay, l, n);
        WRITE_PTR += EntityVertex.STRIDE;
        EntityVertex.write(WRITE_PTR, pos2_x, pos2_y, pos2_z, c, vertices[STRIDE * 2 + 4], vertices[STRIDE * 2 + 5], overlay, l, n);
        WRITE_PTR += EntityVertex.STRIDE;
        EntityVertex.write(WRITE_PTR, pos3_x, pos3_y, pos3_z, c, vertices[STRIDE * 3 + 4], vertices[STRIDE * 3 + 5], overlay, l, n);
        WRITE_PTR += EntityVertex.STRIDE;

        BUFFER_PTR = WRITE_PTR;
        BUFFED_VERTEX += VERTEX_COUNT;
        if (isBufferMax()) flush(writer);
    }

    private static void renderQuadList(PoseStack.Pose pose, VertexBufferWriter writer, List<BakedQuad> bakedQuads, boolean[] shouldRender,
                                       int light, int overlay, ItemStack itemStack, ItemColors itemColors) {
        for (BakedQuad bakedQuad : bakedQuads) {
            BakedQuadView quad = (BakedQuadView) bakedQuad;
            if (!shouldRender[bakedQuad.getDirection().ordinal()]) {
                if (quad.getSprite() != null) SpriteUtil.INSTANCE.markSpriteActive(quad.getSprite());
                continue;
            }
            int color = 0xFFFFFFFF;
            if (quad.hasColor()) {
                color = ColorARGB.toABGR((itemColors.getColor(itemStack, quad.getColorIndex())));
            }
            putBulkData(writer, pose, bakedQuad, light, overlay, color);
            if (quad.getSprite() != null) SpriteUtil.INSTANCE.markSpriteActive(quad.getSprite());
        }
    }

    public static void render(SimpleBakedModel model, boolean[] shouldRender, ItemStack itemStack, int packedLight, int packedOverlay, PoseStack poseStack, VertexBufferWriter writer, ItemColors itemColors) {
        PoseStack.Pose pose = poseStack.last();

        for (Direction direction : Direction.values()) {
            renderQuadList(pose, writer, model.getQuads(null, direction, null), shouldRender, packedLight, packedOverlay, itemStack, itemColors);
        }
        renderQuadList(pose, writer, model.getQuads(null, null, null), shouldRender, packedLight, packedOverlay, itemStack, itemColors);

        flush(writer);
    }
}
