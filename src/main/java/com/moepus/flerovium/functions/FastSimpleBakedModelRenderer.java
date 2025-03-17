package com.moepus.flerovium.functions;

import com.mojang.blaze3d.vertex.PoseStack;
import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import net.caffeinemc.mods.sodium.api.texture.SpriteUtil;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.api.util.ColorMixer;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.common.EntityVertex;
import net.caffeinemc.mods.sodium.client.model.quad.BakedQuadView;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import org.joml.Math;
import org.joml.Matrix3f;
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
    private static final boolean[] SHOULD_RENDER = new boolean[Direction.values().length];

    public static void prepareFaces(FastSimpleBakedModel model) {
        SHOULD_RENDER[0] = model.shouldRenderFace(Direction.DOWN);
        SHOULD_RENDER[1] = model.shouldRenderFace(Direction.UP);
        SHOULD_RENDER[2] = model.shouldRenderFace(Direction.NORTH);
        SHOULD_RENDER[3] = model.shouldRenderFace(Direction.SOUTH);
        SHOULD_RENDER[4] = model.shouldRenderFace(Direction.WEST);
        SHOULD_RENDER[5] = model.shouldRenderFace(Direction.EAST);
    }

    private static int getQuadNormal(Matrix3f mat, int baked) {
        final float factor = PACK_FACTOR[(baked & 0x808080) == 0 ? 0 : 1];
        final int tmp = (baked - 0x7e7e7f) & 0xfdfdfd;
        if ((tmp & 0xff0000) == 0) return packSafe(mat.m20, mat.m21, mat.m22, factor); // South_North
        if ((tmp & 0xff) == 0) return packSafe(mat.m00, mat.m01, mat.m02, factor); // East_West
        if ((tmp & 0xff00) == 0) return packSafe(mat.m10, mat.m11, mat.m12, factor); // Up_Down

        float unpackedX = NormI8.unpackX(baked);
        float unpackedY = NormI8.unpackY(baked);
        float unpackedZ = NormI8.unpackZ(baked);
        float x = MatrixHelper.transformNormalX(mat, unpackedX, unpackedY, unpackedZ);
        float y = MatrixHelper.transformNormalY(mat, unpackedX, unpackedY, unpackedZ);
        float z = MatrixHelper.transformNormalZ(mat, unpackedX, unpackedY, unpackedZ);
        return packSafe(x, y, z);
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

    private static void putBulkData(VertexBufferWriter writer, PoseStack.Pose pose, BakedQuad bakedQuad,
                                    int light, int overlay, int color) {
        int[] vertices = bakedQuad.getVertices();
        if (vertices.length != VERTEX_COUNT * STRIDE) return;
        Matrix4f pose_matrix = pose.pose();
        final int n = getQuadNormal(pose.normal(), vertices[7]);
        final int c = color != -1 ? ColorMixer.mulComponentWise(color, vertices[3]) : vertices[3];
        final int baked = vertices[6];
        final int l = Math.max(((baked & 0xffff) << 16) | (baked >> 16), light);
        final long buffer = BUFFER_PTR;
        int READ_PTR = 0;
        long WRITE_PTR = buffer;
        for (int index = 0; index < VERTEX_COUNT; index++) {
            final float x = Float.intBitsToFloat(vertices[READ_PTR]), y = Float.intBitsToFloat(vertices[READ_PTR + 1]), z = Float.intBitsToFloat(vertices[READ_PTR + 2]);
            final int u = vertices[READ_PTR + 4], v = vertices[READ_PTR + 5];
            EntityVertex.write(WRITE_PTR, MatrixHelper.transformPositionX(pose_matrix, x, y, z), MatrixHelper.transformPositionY(pose_matrix, x, y, z), MatrixHelper.transformPositionZ(pose_matrix, x, y, z), c, Float.intBitsToFloat(u), Float.intBitsToFloat(v), overlay, l, n);

            READ_PTR += STRIDE;
            WRITE_PTR += EntityVertex.STRIDE;
        }

        BUFFER_PTR = WRITE_PTR;
        BUFFED_VERTEX += VERTEX_COUNT;
        if (isBufferMax()) flush(writer);
    }

    private static void renderQuadList(PoseStack.Pose pose, VertexBufferWriter
            writer, List<BakedQuad> bakedQuads, int light, int overlay, ItemStack itemStack, ItemColors
                                               itemColors) {
        for (BakedQuad bakedQuad : bakedQuads) {
            BakedQuadView quad = (BakedQuadView) bakedQuad;
            if (!SHOULD_RENDER[bakedQuad.getDirection().ordinal()]) {
                if (quad.getSprite() != null)
                    SpriteUtil.INSTANCE.markSpriteActive(quad.getSprite());
                continue;
            }
            int color = 0xFFFFFFFF;
            if (quad.hasColor()) {
                color = ColorARGB.toABGR((itemColors.getColor(itemStack, quad.getColorIndex())));
            }
            putBulkData(writer, pose, bakedQuad, light, overlay, color);
            if (quad.getSprite() != null)
                SpriteUtil.INSTANCE.markSpriteActive(quad.getSprite());
        }
    }

    public static void render(FastSimpleBakedModel model, ItemStack itemStack, int packedLight,
                              int packedOverlay, PoseStack poseStack, VertexBufferWriter writer, ItemColors itemColors) {
        PoseStack.Pose pose = poseStack.last();
        prepareFaces(model);

        for (Direction direction : Direction.values()) {
            renderQuadList(pose, writer, model.getQuads(null, direction, null), packedLight, packedOverlay, itemStack, itemColors);
        }
        renderQuadList(pose, writer, model.getQuads(null, null, null), packedLight, packedOverlay, itemStack, itemColors);

        flush(writer);
    }
}
