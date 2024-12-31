package com.moepus.flerovium.functions;

import com.mojang.blaze3d.vertex.PoseStack;
import me.jellysquid.mods.sodium.client.render.immediate.model.ModelCuboid;
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

    private static final Vector3f[] CUBE_CORNERS = new Vector3f[NUM_CUBE_VERTICES];
    private static final int[][] CUBE_VERTICES = new int[][]{
            {VERTEX_X2_Y1_Z2, VERTEX_X1_Y1_Z2, VERTEX_X1_Y1_Z1, VERTEX_X2_Y1_Z1},
            {VERTEX_X2_Y2_Z1, VERTEX_X1_Y2_Z1, VERTEX_X1_Y2_Z2, VERTEX_X2_Y2_Z2},
            {VERTEX_X2_Y1_Z1, VERTEX_X1_Y1_Z1, VERTEX_X1_Y2_Z1, VERTEX_X2_Y2_Z1},
            {VERTEX_X1_Y1_Z2, VERTEX_X2_Y1_Z2, VERTEX_X2_Y2_Z2, VERTEX_X1_Y2_Z2},
            {VERTEX_X2_Y1_Z2, VERTEX_X2_Y1_Z1, VERTEX_X2_Y2_Z1, VERTEX_X2_Y2_Z2},
            {VERTEX_X1_Y1_Z1, VERTEX_X1_Y1_Z2, VERTEX_X1_Y2_Z2, VERTEX_X1_Y2_Z1},
    };

    private static final Vector3f[][] VERTEX_POSITIONS = new Vector3f[NUM_CUBE_FACES][NUM_FACE_VERTICES];
    private static final Vector3f[][] VERTEX_POSITIONS_MIRRORED = new Vector3f[NUM_CUBE_FACES][NUM_FACE_VERTICES];

    private static final Vector2f[][] VERTEX_TEXTURES = new Vector2f[NUM_CUBE_FACES][NUM_FACE_VERTICES];
    private static final Vector2f[][] VERTEX_TEXTURES_MIRRORED = new Vector2f[NUM_CUBE_FACES][NUM_FACE_VERTICES];

    private static final int[] CUBE_NORMALS = new int[NUM_CUBE_FACES];
    private static final int[] CUBE_NORMALS_MIRRORED = new int[NUM_CUBE_FACES];

    static {
        for (int cornerIndex = 0; cornerIndex < NUM_CUBE_VERTICES; cornerIndex++) {
            CUBE_CORNERS[cornerIndex] = new Vector3f();
        }

        for (int quadIndex = 0; quadIndex < NUM_CUBE_FACES; quadIndex++) {
            for (int vertexIndex = 0; vertexIndex < NUM_FACE_VERTICES; vertexIndex++) {
                VERTEX_TEXTURES[quadIndex][vertexIndex] = new Vector2f();
                VERTEX_POSITIONS[quadIndex][vertexIndex] = CUBE_CORNERS[CUBE_VERTICES[quadIndex][vertexIndex]];
            }
        }

        for (int quadIndex = 0; quadIndex < NUM_CUBE_FACES; quadIndex++) {
            for (int vertexIndex = 0; vertexIndex < NUM_FACE_VERTICES; vertexIndex++) {
                VERTEX_TEXTURES_MIRRORED[quadIndex][vertexIndex] = VERTEX_TEXTURES[quadIndex][3 - vertexIndex];
                VERTEX_POSITIONS_MIRRORED[quadIndex][vertexIndex] = VERTEX_POSITIONS[quadIndex][3 - vertexIndex];
            }
        }
    }

    public static void renderCuboidFast(PoseStack.Pose matrices, VertexBufferWriter writer, ModelCuboid cuboid, int light, int overlay, int color) {
        prepareVertices(matrices, cuboid);

        var vertexCount = emitQuads(cuboid, color, overlay, light, matrices);

        STACK.push();
        writer.push(STACK, SCRATCH_BUFFER, vertexCount, ModelVertex.FORMAT);
        STACK.pop();
    }

    private static int emitQuads(ModelCuboid cuboid, int color, int overlay, int light, PoseStack.Pose matrices) {
        final var positions = cuboid.mirror ? VERTEX_POSITIONS_MIRRORED : VERTEX_POSITIONS;
        final var textures = cuboid.mirror ? VERTEX_TEXTURES_MIRRORED : VERTEX_TEXTURES;
        final var normals = cuboid.mirror ? CUBE_NORMALS_MIRRORED : CUBE_NORMALS;

        var vertexCount = 0;

        long ptr = SCRATCH_BUFFER;

        boolean cullBackFace = false;
        byte viewX = 0, viewY = 0, viewZ = 0;
        if (matrices.pose().m32() < -16.0F) {
            cullBackFace = true;
            Matrix4f mat = matrices.pose();
            float scalar = 127 * Math.invsqrt(Math.fma(mat.m30(), mat.m30(), Math.fma(mat.m31(), mat.m31(), mat.m32() * mat.m32())));
            viewX = (byte) (mat.m30() * scalar);
            viewY = (byte) (mat.m31() * scalar);
            viewZ = (byte) (mat.m32() * scalar);
        }
        for (int quadIndex = 0; quadIndex < NUM_CUBE_FACES; quadIndex++) {
            if (!cuboid.shouldDrawFace(quadIndex)) {
                continue;
            }
            int normal = normals[quadIndex];
            if (cullBackFace && cullBackFace(viewX, viewY, viewZ, normal)) {
                continue;
            }

            emitVertex(ptr, positions[quadIndex][0], color, textures[quadIndex][0], overlay, light, normal);
            ptr += ModelVertex.STRIDE;

            emitVertex(ptr, positions[quadIndex][1], color, textures[quadIndex][1], overlay, light, normal);
            ptr += ModelVertex.STRIDE;

            emitVertex(ptr, positions[quadIndex][2], color, textures[quadIndex][2], overlay, light, normal);
            ptr += ModelVertex.STRIDE;

            emitVertex(ptr, positions[quadIndex][3], color, textures[quadIndex][3], overlay, light, normal);
            ptr += ModelVertex.STRIDE;

            vertexCount += 4;
        }

        return vertexCount;
    }

    private static void emitVertex(long ptr, Vector3f pos, int color, Vector2f tex, int overlay, int light, int normal) {
        ModelVertex.write(ptr, pos.x, pos.y, pos.z, color, tex.x, tex.y, overlay, light, normal);
    }

    private static void prepareVertices(PoseStack.Pose matrices, ModelCuboid cuboid) {
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
        CUBE_CORNERS[VERTEX_X1_Y1_Z1].x = Math.fma(pose.m00(), cuboid.x1, Math.fma(pose.m10(), cuboid.y1, Math.fma(pose.m20(), cuboid.z1, pose.m30())));
        CUBE_CORNERS[VERTEX_X1_Y1_Z1].y = Math.fma(pose.m01(), cuboid.x1, Math.fma(pose.m11(), cuboid.y1, Math.fma(pose.m21(), cuboid.z1, pose.m31())));
        CUBE_CORNERS[VERTEX_X1_Y1_Z1].z = Math.fma(pose.m02(), cuboid.x1, Math.fma(pose.m12(), cuboid.y1, Math.fma(pose.m22(), cuboid.z1, pose.m32())));

        float lx = cuboid.x2 - cuboid.x1, ly = cuboid.y2 - cuboid.y1, lz = cuboid.z2 - cuboid.z1;
        float vxx = pose.m00() * lx, vxy = pose.m01() * lx, vxz = pose.m02() * lx;
        float vyx = pose.m10() * ly, vyy = pose.m11() * ly, vyz = pose.m12() * ly;
        float vzx = pose.m20() * lz, vzy = pose.m21() * lz, vzz = pose.m22() * lz;

        CUBE_CORNERS[VERTEX_X1_Y1_Z1].add(vxx, vxy, vxz, CUBE_CORNERS[VERTEX_X2_Y1_Z1]);
        CUBE_CORNERS[VERTEX_X2_Y1_Z1].add(vyx, vyy, vyz, CUBE_CORNERS[VERTEX_X2_Y2_Z1]);
        CUBE_CORNERS[VERTEX_X1_Y1_Z1].add(vyx, vyy, vyz, CUBE_CORNERS[VERTEX_X1_Y2_Z1]);
        CUBE_CORNERS[VERTEX_X1_Y1_Z1].add(vzx, vzy, vzz, CUBE_CORNERS[VERTEX_X1_Y1_Z2]);
        CUBE_CORNERS[VERTEX_X2_Y1_Z1].add(vzx, vzy, vzz, CUBE_CORNERS[VERTEX_X2_Y1_Z2]);
        CUBE_CORNERS[VERTEX_X2_Y1_Z2].add(vyx, vyy, vyz, CUBE_CORNERS[VERTEX_X2_Y2_Z2]);
        CUBE_CORNERS[VERTEX_X1_Y1_Z2].add(vyx, vyy, vyz, CUBE_CORNERS[VERTEX_X1_Y2_Z2]);

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

        // When mirroring is used, the normals for EAST and WEST are swapped.
        CUBE_NORMALS_MIRRORED[FACE_NEG_Y] = CUBE_NORMALS[FACE_NEG_Y];
        CUBE_NORMALS_MIRRORED[FACE_POS_Y] = CUBE_NORMALS[FACE_POS_Y];
        CUBE_NORMALS_MIRRORED[FACE_NEG_Z] = CUBE_NORMALS[FACE_NEG_Z];
        CUBE_NORMALS_MIRRORED[FACE_POS_Z] = CUBE_NORMALS[FACE_POS_Z];
        CUBE_NORMALS_MIRRORED[FACE_POS_X] = CUBE_NORMALS[FACE_NEG_X]; // mirrored
        CUBE_NORMALS_MIRRORED[FACE_NEG_X] = CUBE_NORMALS[FACE_POS_X]; // mirrored
    }

    private static void buildVertexTexCoord(Vector2f[] uvs, float u1, float v1, float u2, float v2) {
        uvs[0].set(u2, v1);
        uvs[1].set(u1, v1);
        uvs[2].set(u1, v2);
        uvs[3].set(u2, v2);
    }
}
