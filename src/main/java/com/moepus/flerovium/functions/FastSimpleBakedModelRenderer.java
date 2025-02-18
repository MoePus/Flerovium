package com.moepus.flerovium.functions;

import com.mojang.blaze3d.vertex.PoseStack;
import me.jellysquid.mods.sodium.client.model.color.interop.ItemColorsExtension;
import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil;
import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.common.ModelVertex;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.renderer.block.model.BakedQuad;
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
    private static final int[] CUBE_NORMALS = new int[Direction.values().length];
    private static int LAST_TINT_INDEX = -1;
    private static int LAST_TINT = -1;
    private static final int DONT_RENDER = -1;


    public static void prepareNormals(FastSimpleBakedModel model, PoseStack.Pose pose) {
        Matrix4f mat = pose.pose();

        CUBE_NORMALS[0] = model.shouldRenderFace(Direction.DOWN) ? normal2Int(-mat.m10(), -mat.m11(), -mat.m12()) : DONT_RENDER;
        CUBE_NORMALS[1] = model.shouldRenderFace(Direction.UP) ? normal2Int(mat.m10(), mat.m11(), mat.m12()) : DONT_RENDER;
        CUBE_NORMALS[2] = model.shouldRenderFace(Direction.NORTH) ? normal2Int(-mat.m20(), -mat.m21(), -mat.m22()) : DONT_RENDER;
        CUBE_NORMALS[3] = model.shouldRenderFace(Direction.SOUTH) ? normal2Int(mat.m20(), mat.m21(), mat.m22()) : DONT_RENDER;
        CUBE_NORMALS[4] = model.shouldRenderFace(Direction.WEST) ? normal2Int(-mat.m00(), -mat.m01(), -mat.m02()) : DONT_RENDER;
        CUBE_NORMALS[5] = model.shouldRenderFace(Direction.EAST) ? normal2Int(mat.m00(), mat.m01(), mat.m02()) : DONT_RENDER;


        if (model.isNeedExtraCulling()) {
            float scalar = 127 * Math.invsqrt(Math.fma(mat.m30(), mat.m30(), Math.fma(mat.m31(), mat.m31(), mat.m32() * mat.m32())));
            byte viewX = (byte) (mat.m30() * scalar);
            byte viewY = (byte) (mat.m31() * scalar);
            byte viewZ = (byte) (mat.m32() * scalar);
            for (int i = 0; i < 6; i++) {
                if (CUBE_NORMALS[i] != DONT_RENDER && cullBackFace(viewX, viewY, viewZ, CUBE_NORMALS[i])) {
                    CUBE_NORMALS[i] = DONT_RENDER;
                }
            }
        }
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

    private static void putBulkData(VertexBufferWriter writer, PoseStack.Pose pose, BakedQuad bakedQuad,
                                    int light, int overlay, int color) {
        int[] vertices = bakedQuad.getVertices();
        if (vertices.length != VERTEX_COUNT * STRIDE) return;
        Matrix4f pose_matrix = pose.pose();
        final int n = getQuadNormal(pose.normal(), vertices[7]);
        final int c = color != -1 ? multiplyIntBytes(color, vertices[3]) : vertices[3];
        final int baked = vertices[6];
        final int l = Math.max(((baked & 0xffff) << 16) | (baked >> 16), light);
        final long buffer = BUFFER_PTR;
        int READ_PTR = 0;
        long WRITE_PTR = buffer;
        for (int index = 0; index < VERTEX_COUNT; index++) {
            final float x = Float.intBitsToFloat(vertices[READ_PTR]), y = Float.intBitsToFloat(vertices[READ_PTR + 1]), z = Float.intBitsToFloat(vertices[READ_PTR + 2]);
            final int u = vertices[READ_PTR + 4], v = vertices[READ_PTR + 5];
            ModelVertex.write(WRITE_PTR, MatrixHelper.transformPositionX(pose_matrix, x, y, z), MatrixHelper.transformPositionY(pose_matrix, x, y, z), MatrixHelper.transformPositionZ(pose_matrix, x, y, z), c, Float.intBitsToFloat(u), Float.intBitsToFloat(v), overlay, l, n);

            READ_PTR += STRIDE;
            WRITE_PTR += ModelVertex.STRIDE;
        }

        BUFFER_PTR = WRITE_PTR;
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

    private static void renderQuadList(PoseStack.Pose pose, VertexBufferWriter
            writer, List<BakedQuad> bakedQuads, int light, int overlay, ItemStack itemStack, ItemColor
                                               colorProvider) {
        for (BakedQuad bakedQuad : bakedQuads) {
            int normal = CUBE_NORMALS[bakedQuad.getDirection().ordinal()];
            if (normal == DONT_RENDER) continue;
            int color = colorProvider != null && bakedQuad.getTintIndex() != -1 ? GetItemTint(bakedQuad.getTintIndex(), itemStack, colorProvider) : -1;
            putBulkData(writer, pose, bakedQuad, light, overlay, color);
        }
        if (pose.pose().m32() > -8.0F) { // Do animation for item in GUI or nearby in world
            for (BakedQuad bakedQuad : bakedQuads) {
                SpriteUtil.markSpriteActive(bakedQuad.getSprite());
            }
        }
    }

    public static void render(FastSimpleBakedModel model, ItemStack itemStack, int packedLight,
                              int packedOverlay, PoseStack poseStack, VertexBufferWriter writer, ItemColors itemColors) {
        PoseStack.Pose pose = poseStack.last();
        prepareNormals(model, pose);
        ItemColor colorProvider = !itemStack.isEmpty() ? ((ItemColorsExtension) itemColors).sodium$getColorProvider(itemStack) : null;

        LAST_TINT_INDEX = LAST_TINT = -1;
        for (Direction direction : Direction.values()) {
            renderQuadList(pose, writer, model.getQuads(null, direction, null), packedLight, packedOverlay, itemStack, colorProvider);
        }
        renderQuadList(pose, writer, model.getQuads(null, null, null), packedLight, packedOverlay, itemStack, colorProvider);

        flush(writer);
    }
}
