package com.moepus.flerovium.functions;

import com.mojang.blaze3d.vertex.PoseStack;
import me.jellysquid.mods.sodium.client.render.immediate.model.ModelCuboid;
import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.LightAttribute;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.NormalAttribute;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.OverlayAttribute;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.TextureAttribute;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.common.ModelVertex;
import org.joml.*;
import org.joml.Math;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import static com.moepus.flerovium.functions.MathUtil.*;

public class FastEntityRenderer {

    private static final int NUM_CUBE_VERTICES = 8;
    private static final int NUM_CUBE_FACES = 6;
    private static final int NUM_FACE_VERTICES = 4;

    private static final int
            FACE_NEG_Y = 0, // DOWN
            FACE_POS_Y = 1, // UP
            FACE_NEG_Z = 2, // NORTH
            FACE_POS_Z = 3, // SOUTH
            FACE_NEG_X = 4, // WEST
            FACE_POS_X = 5; // EAST

    private static final int
            VERTEX_X1_Y1_Z1 = 0,
            VERTEX_X2_Y1_Z1 = 1,
            VERTEX_X2_Y2_Z1 = 2,
            VERTEX_X1_Y2_Z1 = 3,
            VERTEX_X1_Y1_Z2 = 4,
            VERTEX_X2_Y1_Z2 = 5,
            VERTEX_X2_Y2_Z2 = 6,
            VERTEX_X1_Y2_Z2 = 7;


    private static final long SCRATCH_BUFFER = MemoryUtil.nmemAlignedAlloc(64, NUM_CUBE_FACES * NUM_FACE_VERTICES * ModelVertex.STRIDE);
    private static final MemoryStack STACK = MemoryStack.create();

    static class Vertex {
        public long xy;
        public long zw;

        public void set(float x, float y, float z, int color) {
            this.xy = compose(Float.floatToRawIntBits(x), Float.floatToRawIntBits(y));
            this.zw = compose(Float.floatToRawIntBits(z), color);
        }
    }

    private static final Vertex[] CUBE_CORNERS = new Vertex[NUM_CUBE_VERTICES];
    private static final int[][] CUBE_VERTICES = new int[][]{
            {VERTEX_X2_Y1_Z2, VERTEX_X1_Y1_Z2, VERTEX_X1_Y1_Z1, VERTEX_X2_Y1_Z1},
            {VERTEX_X2_Y2_Z1, VERTEX_X1_Y2_Z1, VERTEX_X1_Y2_Z2, VERTEX_X2_Y2_Z2},
            {VERTEX_X2_Y1_Z1, VERTEX_X1_Y1_Z1, VERTEX_X1_Y2_Z1, VERTEX_X2_Y2_Z1},
            {VERTEX_X1_Y1_Z2, VERTEX_X2_Y1_Z2, VERTEX_X2_Y2_Z2, VERTEX_X1_Y2_Z2},
            {VERTEX_X2_Y1_Z2, VERTEX_X2_Y1_Z1, VERTEX_X2_Y2_Z1, VERTEX_X2_Y2_Z2},
            {VERTEX_X1_Y1_Z1, VERTEX_X1_Y1_Z2, VERTEX_X1_Y2_Z2, VERTEX_X1_Y2_Z1},
    };

    private static final Vertex[][] VERTEX_POSITIONS = new Vertex[NUM_CUBE_FACES][NUM_FACE_VERTICES];
    private static final long[][] VERTEX_TEXTURES = new long[NUM_CUBE_FACES][NUM_FACE_VERTICES];

    private static final int[] CUBE_NORMALS = new int[NUM_CUBE_FACES];
    private static final int[] CUBE_NORMALS_MIRRORED = new int[NUM_CUBE_FACES];

    static {
        for (int cornerIndex = 0; cornerIndex < NUM_CUBE_VERTICES; cornerIndex++) {
            CUBE_CORNERS[cornerIndex] = new Vertex();
        }

        for (int quadIndex = 0; quadIndex < NUM_CUBE_FACES; quadIndex++) {
            for (int vertexIndex = 0; vertexIndex < NUM_FACE_VERTICES; vertexIndex++) {
                VERTEX_POSITIONS[quadIndex][vertexIndex] = CUBE_CORNERS[CUBE_VERTICES[quadIndex][vertexIndex]];
            }
        }
    }

    public static void renderCuboidFast(PoseStack.Pose matrices, VertexBufferWriter writer, ModelCuboid cuboid, int light, int overlay, int color) {
        prepareVertices(matrices, cuboid, color);

        var vertexCount = emitQuads(cuboid, overlay, light);

        STACK.push();
        writer.push(STACK, SCRATCH_BUFFER, vertexCount, ModelVertex.FORMAT);
        STACK.pop();
    }

    private static int emitQuads(ModelCuboid cuboid, int overlay, int light) {
        final var normals = cuboid.mirror ? CUBE_NORMALS_MIRRORED : CUBE_NORMALS;
        final long packedOverlayLight = compose(overlay, light);

        var vertexCount = 0;

        long ptr = SCRATCH_BUFFER;

        for (int quadIndex = 0; quadIndex < NUM_CUBE_FACES; quadIndex++) {
            if (!cuboid.shouldDrawFace(quadIndex)) {
                continue;
            }
            int normal = normals[quadIndex];
            if (normal == -1) {
                continue;
            }
            int xor = cuboid.mirror ? 3 : 0;

            emitVertex(ptr, VERTEX_POSITIONS[quadIndex][0 ^ xor], VERTEX_TEXTURES[quadIndex][0 ^ xor], packedOverlayLight, normal);
            ptr += ModelVertex.STRIDE;

            emitVertex(ptr, VERTEX_POSITIONS[quadIndex][1 ^ xor], VERTEX_TEXTURES[quadIndex][1 ^ xor], packedOverlayLight, normal);
            ptr += ModelVertex.STRIDE;

            emitVertex(ptr, VERTEX_POSITIONS[quadIndex][2 ^ xor], VERTEX_TEXTURES[quadIndex][2 ^ xor], packedOverlayLight, normal);
            ptr += ModelVertex.STRIDE;

            emitVertex(ptr, VERTEX_POSITIONS[quadIndex][3 ^ xor], VERTEX_TEXTURES[quadIndex][3 ^ xor], packedOverlayLight, normal);
            ptr += ModelVertex.STRIDE;

            vertexCount += 4;
        }

        return vertexCount;
    }

    private static void emitVertex(long ptr, Vertex vertex, long uv, long packedOverlayLight, int normal) {
        MemoryUtil.memPutLong(ptr + 0L, vertex.xy);
        MemoryUtil.memPutLong(ptr + 8L, vertex.zw); // overlaps with color attribute
        MemoryUtil.memPutLong(ptr + 16L, uv);
        MemoryUtil.memPutLong(ptr + 24L, packedOverlayLight);
        NormalAttribute.set(ptr + 32L, normal);
    }

    private static void prepareVertices(PoseStack.Pose matrices, ModelCuboid cuboid, int color) {
        Matrix4f pose = matrices.pose();

        /**
         *  Build a Cube from a Vertex and 3 Vectors
         *
         *  Using one vertex (P1) and three vectors (X, Y, Z):
         *
         *            P8 +----------------+ P7
         *            /|                 /|
         *           / |                / |
         *        P4 +----------------+ P3|
         *          |  |              |  |
         *          |  |              |  |
         *          |  P5 +-----------|--+ P6
         *          | /               | /
         *          |/                |/
         *        P1 +----------------+ P2
         *
         *  Vertices:
         *  P2 = P1 + X
         *  P3 = P2 + Y
         *  P4 = P1 + Y
         *  P5 = P1 + Z
         *  P6 = P2 + Z
         *  P7 = P6 + Y
         *  P8 = P5 + Y
         */
        float p1x = MatrixHelper.transformPositionX(pose, cuboid.x1, cuboid.y1, cuboid.z1);
        float p1y = MatrixHelper.transformPositionY(pose, cuboid.x1, cuboid.y1, cuboid.z1);
        float p1z = MatrixHelper.transformPositionZ(pose, cuboid.x1, cuboid.y1, cuboid.z1);
        CUBE_CORNERS[VERTEX_X1_Y1_Z1].set(p1x, p1y, p1z, color);

        float lx = cuboid.x2 - cuboid.x1, ly = cuboid.y2 - cuboid.y1, lz = cuboid.z2 - cuboid.z1;
        float vxx = pose.m00() * lx, vxy = pose.m01() * lx, vxz = pose.m02() * lx;
        float vyx = pose.m10() * ly, vyy = pose.m11() * ly, vyz = pose.m12() * ly;
        float vzx = pose.m20() * lz, vzy = pose.m21() * lz, vzz = pose.m22() * lz;

        float p2x = p1x + vxx;
        float p2y = p1y + vxy;
        float p2z = p1z + vxz;
        CUBE_CORNERS[VERTEX_X2_Y1_Z1].set(p2x, p2y, p2z, color);

        float p3x = p2x + vyx;
        float p3y = p2y + vyy;
        float p3z = p2z + vyz;
        CUBE_CORNERS[VERTEX_X2_Y2_Z1].set(p3x, p3y, p3z, color);

        float p4x = p1x + vyx;
        float p4y = p1y + vyy;
        float p4z = p1z + vyz;
        CUBE_CORNERS[VERTEX_X1_Y2_Z1].set(p4x, p4y, p4z, color);

        float p5x = p1x + vzx;
        float p5y = p1y + vzy;
        float p5z = p1z + vzz;
        CUBE_CORNERS[VERTEX_X1_Y1_Z2].set(p5x, p5y, p5z, color);

        float p6x = p2x + vzx;
        float p6y = p2y + vzy;
        float p6z = p2z + vzz;
        CUBE_CORNERS[VERTEX_X2_Y1_Z2].set(p6x, p6y, p6z, color);

        float p7x = p3x + vzx;
        float p7y = p3y + vzy;
        float p7z = p3z + vzz;
        CUBE_CORNERS[VERTEX_X2_Y2_Z2].set(p7x, p7y, p7z, color);

        float p8x = p4x + vzx;
        float p8y = p4y + vzy;
        float p8z = p4z + vzz;
        CUBE_CORNERS[VERTEX_X1_Y2_Z2].set(p8x, p8y, p8z, color);

        buildVertexTexCoord(VERTEX_TEXTURES[FACE_NEG_Y], cuboid.u1, cuboid.v0, cuboid.u2, cuboid.v1);
        buildVertexTexCoord(VERTEX_TEXTURES[FACE_POS_Y], cuboid.u2, cuboid.v1, cuboid.u3, cuboid.v0);
        buildVertexTexCoord(VERTEX_TEXTURES[FACE_NEG_Z], cuboid.u1, cuboid.v1, cuboid.u2, cuboid.v2);
        buildVertexTexCoord(VERTEX_TEXTURES[FACE_POS_Z], cuboid.u4, cuboid.v1, cuboid.u5, cuboid.v2);
        buildVertexTexCoord(VERTEX_TEXTURES[FACE_NEG_X], cuboid.u2, cuboid.v1, cuboid.u4, cuboid.v2);
        buildVertexTexCoord(VERTEX_TEXTURES[FACE_POS_X], cuboid.u0, cuboid.v1, cuboid.u1, cuboid.v2);
    }

    public static void prepareNormals(PoseStack.Pose matrices) {
        Matrix3f normal = matrices.normal();
        CUBE_NORMALS[FACE_NEG_Y] = normal2Int(-normal.m10, -normal.m11, -normal.m12);
        CUBE_NORMALS[FACE_POS_Y] = normal2Int(normal.m10, normal.m11, normal.m12);
        CUBE_NORMALS[FACE_NEG_Z] = normal2Int(-normal.m20, -normal.m21, -normal.m22);
        CUBE_NORMALS[FACE_POS_Z] = normal2Int(normal.m20, normal.m21, normal.m22);
        CUBE_NORMALS[FACE_POS_X] = normal2Int(-normal.m00, -normal.m01, -normal.m02);
        CUBE_NORMALS[FACE_NEG_X] = normal2Int(normal.m00, normal.m01, normal.m02);

        if (matrices.pose().m32() < -16.0F) {
            Matrix4f mat = matrices.pose();
            float scalar = 127 * Math.invsqrt(Math.fma(mat.m30(), mat.m30(), Math.fma(mat.m31(), mat.m31(), mat.m32() * mat.m32())));
            byte viewX = (byte) (mat.m30() * scalar);
            byte viewY = (byte) (mat.m31() * scalar);
            byte viewZ = (byte) (mat.m32() * scalar);
            if (cullBackFace(viewX, viewY, viewZ, CUBE_NORMALS[FACE_NEG_Y])) CUBE_NORMALS[FACE_NEG_Y] = -1;
            if (cullBackFace(viewX, viewY, viewZ, CUBE_NORMALS[FACE_POS_Y])) CUBE_NORMALS[FACE_POS_Y] = -1;
            if (cullBackFace(viewX, viewY, viewZ, CUBE_NORMALS[FACE_NEG_Z])) CUBE_NORMALS[FACE_NEG_Z] = -1;
            if (cullBackFace(viewX, viewY, viewZ, CUBE_NORMALS[FACE_POS_Z])) CUBE_NORMALS[FACE_POS_Z] = -1;
            if (cullBackFace(viewX, viewY, viewZ, CUBE_NORMALS[FACE_NEG_X])) CUBE_NORMALS[FACE_NEG_X] = -1;
            if (cullBackFace(viewX, viewY, viewZ, CUBE_NORMALS[FACE_POS_X])) CUBE_NORMALS[FACE_POS_X] = -1;
        }

        // When mirroring is used, the normals for EAST and WEST are swapped.
        CUBE_NORMALS_MIRRORED[FACE_NEG_Y] = CUBE_NORMALS[FACE_NEG_Y];
        CUBE_NORMALS_MIRRORED[FACE_POS_Y] = CUBE_NORMALS[FACE_POS_Y];
        CUBE_NORMALS_MIRRORED[FACE_NEG_Z] = CUBE_NORMALS[FACE_NEG_Z];
        CUBE_NORMALS_MIRRORED[FACE_POS_Z] = CUBE_NORMALS[FACE_POS_Z];
        CUBE_NORMALS_MIRRORED[FACE_POS_X] = CUBE_NORMALS[FACE_NEG_X]; // mirrored
        CUBE_NORMALS_MIRRORED[FACE_NEG_X] = CUBE_NORMALS[FACE_POS_X]; // mirrored
    }

    private static void buildVertexTexCoord(long[] uvs, float u1, float v1, float u2, float v2) {
        uvs[0] = compose(Float.floatToRawIntBits(u2), Float.floatToRawIntBits(v1));
        uvs[1] = compose(Float.floatToRawIntBits(u1), Float.floatToRawIntBits(v1));
        uvs[2] = compose(Float.floatToRawIntBits(u1), Float.floatToRawIntBits(v2));
        uvs[3] = compose(Float.floatToRawIntBits(u2), Float.floatToRawIntBits(v2));
    }
}
