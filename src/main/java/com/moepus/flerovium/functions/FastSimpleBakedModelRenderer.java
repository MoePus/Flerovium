package com.moepus.flerovium.functions;

import com.mojang.blaze3d.vertex.PoseStack;
import me.jellysquid.mods.sodium.client.model.color.interop.ItemColorsExtended;
import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil;
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

    private static int packUnsafe(float x, float y, float z) {
        int normX = (int) (x * 127.0f) & 255;
        int normY = (int) (y * 127.0f) & 255;
        int normZ = (int) (z * 127.0f) & 255;

        return (normZ << 16) | (normY << 8) | normX;
    }

    public static int normal2Int(float x, float y, float z) {
        float scalar = Math.invsqrt(Math.fma(x, x, Math.fma(y, y, z * z)));
        return packUnsafe(x * scalar, y * scalar, z * scalar);
    }

    public static boolean cullBackFace(byte viewX, byte viewY, byte viewZ, int normal) {
        byte normalX = (byte) normal;
        byte normalY = (byte) (normal >> 8);
        byte normalZ = (byte) (normal >> 16);
        return (int) viewX * (int) normalX + (int) viewY * (int) normalY + (int) viewZ * (int) normalZ > 768;
    }

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

    private static int getQuadNormal(Matrix3f mat, int baked, int dir_normal) {
        if (baked == 0x7f0000) return packUnsafe(mat.m20, mat.m21, mat.m22); // South
        if (baked == 0x810000) return packUnsafe(-mat.m20, -mat.m21, -mat.m22); // North
        if (baked == 0x7f) return packUnsafe(mat.m00, mat.m01, mat.m02); // East
        if (baked == 0x81) return packUnsafe(-mat.m00, -mat.m01, -mat.m02); // West
        if (baked == 0x7f00) return packUnsafe(mat.m10, mat.m11, mat.m12); // Up
        if (baked == 0x8100) return packUnsafe(-mat.m10, -mat.m11, -mat.m12); // Down
        if (baked == 0) return dir_normal;

        Vector3f normal = new Vector3f(NormI8.unpackX(baked), NormI8.unpackY(baked), NormI8.unpackZ(baked));
        normal.mul(mat);
        return NormI8.pack(normal);
    }

    public static int multiplyIntBytes(int a, int b) {
        int r = 0;
        for (int i = 0; i < 3; i++) {
            int af = (a >>> (i * 8)) & 0xFF;
            int bf = (b >>> (i * 8)) & 0xFF;
            r |= ((af * bf + 127) / 255) << (i * 8);
        }
        return r | (b & 0xff000000);
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

    private static void putBulkData(VertexBufferWriter writer, PoseStack.Pose pose, BakedQuad bakedQuad, int light, int overlay, int normal, int color) {
        int[] vertices = bakedQuad.getVertices();
        if (vertices.length != VERTEX_COUNT * STRIDE) return;
        Matrix4f pose_matrix = pose.pose();
        final int n = getQuadNormal(pose.normal(), vertices[7], normal);
        final long buffer = BUFFER_PTR;
        for (int index = 0; index < VERTEX_COUNT; index++) {
            final int reader = index * STRIDE;
            final float x = Float.intBitsToFloat(vertices[reader]), y = Float.intBitsToFloat(vertices[reader + 1]), z = Float.intBitsToFloat(vertices[reader + 2]);
            final Vector3f pos = new Vector3f(x, y, z).mulPosition(pose_matrix);
            final int c = color != -1 ? multiplyIntBytes(color, vertices[reader + 3]) : vertices[reader + 3];
            final float u = Float.intBitsToFloat(vertices[reader + 4]);
            final float v = Float.intBitsToFloat(vertices[reader + 5]);
            final int baked = vertices[reader + 6];
            final int l = Math.max(((baked & 0xffff) << 16) | (baked >> 16), light);
            ModelVertex.write(buffer + index * ModelVertex.STRIDE, pos.x, pos.y, pos.z, c, u, v, overlay, l, n);
        }

        BUFFER_PTR += ModelVertex.STRIDE * VERTEX_COUNT;
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

    private static void renderQuadList(PoseStack.Pose pose, VertexBufferWriter writer, List<BakedQuad> bakedQuads, int light, int overlay, ItemStack itemStack, ItemColor colorProvider) {
        for (BakedQuad bakedQuad : bakedQuads) {
            int normal = CUBE_NORMALS[bakedQuad.getDirection().ordinal()];
            if (normal == DONT_RENDER) continue;
            int color = colorProvider != null && bakedQuad.getTintIndex() != -1 ? GetItemTint(bakedQuad.getTintIndex(), itemStack, colorProvider) : -1;
            putBulkData(writer, pose, bakedQuad, light, overlay, normal, color);
        }
        if (pose.pose().m32() > -8.0F) { // Do animation for item in GUI or nearby in world
            for (BakedQuad bakedQuad : bakedQuads) {
                SpriteUtil.markSpriteActive(bakedQuad.getSprite());
            }
        }
    }

    public static void render(FastSimpleBakedModel model, ItemStack itemStack, int packedLight, int packedOverlay, PoseStack poseStack, VertexBufferWriter writer, ItemColors itemColors) {
        PoseStack.Pose pose = poseStack.last();
        prepareNormals(model, pose);
        ItemColor colorProvider = !itemStack.isEmpty() ? ((ItemColorsExtended) itemColors).sodium$getColorProvider(itemStack) : null;

        LAST_TINT_INDEX = LAST_TINT = -1;
        for (Direction direction : Direction.values()) {
            renderQuadList(pose, writer, model.getQuads(null, direction, null), packedLight, packedOverlay, itemStack, colorProvider);
        }
        renderQuadList(pose, writer, model.getQuads(null, null, null), packedLight, packedOverlay, itemStack, colorProvider);

        flush(writer);
    }
}
