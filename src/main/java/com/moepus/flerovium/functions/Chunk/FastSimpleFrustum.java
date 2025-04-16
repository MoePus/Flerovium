package com.moepus.flerovium.functions.Chunk;

import com.moepus.flerovium.mixins.Chunk.SimpleFrustumAccessor;
import me.jellysquid.mods.sodium.client.render.viewport.frustum.SimpleFrustum;
import org.joml.FrustumIntersection;
import org.joml.Vector4f;

import java.lang.reflect.Field;

public class FastSimpleFrustum {
    // The bounding box of a chunk section must be large enough to contain all possible geometry within it. Block models
    // can extend outside a block volume by +/- 1.0 blocks on all axis. Additionally, we make use of a small epsilon
    // to deal with floating point imprecision during a frustum check (see GH#2132).
    public static final float CHUNK_SECTION_RADIUS = 8.0f /* chunk bounds */;
    public static final float CHUNK_SECTION_SIZE = CHUNK_SECTION_RADIUS + 1.0f /* maximum model extent */ + 0.125f /* epsilon */;

    // all the w components are double negated
    private float nxX, nxY, nxZ, negNxW;
    private float pxX, pxY, pxZ, negPxW;
    private float nyX, nyY, nyZ, negNyW;
    private float pyX, pyY, pyZ, negPyW;
    private float nzX, nzY, nzZ, negNzW;

    public FastSimpleFrustum(Object frustum) {
        Vector4f[] planes = getFrustumPlanes(frustum);

        nxX = planes[0].x;
        nxY = planes[0].y;
        nxZ = planes[0].z;
        pxX = planes[1].x;
        pxY = planes[1].y;
        pxZ = planes[1].z;
        nyX = planes[2].x;
        nyY = planes[2].y;
        nyZ = planes[2].z;
        pyX = planes[3].x;
        pyY = planes[3].y;
        pyZ = planes[3].z;
        nzX = planes[4].x;
        nzY = planes[4].y;
        nzZ = planes[4].z;

        final float size = CHUNK_SECTION_SIZE;
        negNxW = 2 * (-(planes[0].w + nxX * (nxX < 0 ? -size : size) +
                nxY * (nxY < 0 ? -size : size) +
                nxZ * (nxZ < 0 ? -size : size)));
        negPxW = 2 * (-(planes[1].w + pxX * (pxX < 0 ? -size : size) +
                pxY * (pxY < 0 ? -size : size) +
                pxZ * (pxZ < 0 ? -size : size)));
        negNyW = 2 * (-(planes[2].w + nyX * (nyX < 0 ? -size : size) +
                nyY * (nyY < 0 ? -size : size) +
                nyZ * (nyZ < 0 ? -size : size)));
        negPyW = 2 * (-(planes[3].w + pyX * (pyX < 0 ? -size : size) +
                pyY * (pyY < 0 ? -size : size) +
                pyZ * (pyZ < 0 ? -size : size)));
        negNzW = 2 * (-(planes[4].w + nzX * (nzX < 0 ? -size : size) +
                nzY * (nzY < 0 ? -size : size) +
                nzZ * (nzZ < 0 ? -size : size)));
    }

    private static Vector4f[] getFrustumPlanes(Object frustum) {
        FrustumIntersection frustumIntersection = ((SimpleFrustumAccessor) frustum).getFrustum();
        Vector4f[] planes;
        try {
            Field planesField = FrustumIntersection.class.getDeclaredField("planes");
            planesField.setAccessible(true);
            planes = (Vector4f[]) planesField.get(frustumIntersection);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to access planes field in FrustumIntersection", e);
        }
        return planes;
    }

    public boolean testCubeQuick(float wx, float wy, float wz) {
        // Skip far plane checks because it has been ensured by searchDistance and isWithinRenderDistance check in OcclusionCuller
        return nxX * wx + nxY * wy + nxZ * wz >= negNxW &&
                pxX * wx + pxY * wy + pxZ * wz >= negPxW &&
                nyX * wx + nyY * wy + nyZ * wz >= negNyW &&
                pyX * wx + pyY * wy + pyZ * wz >= negPyW &&
                nzX * wx + nzY * wy + nzZ * wz >= negNzW;
    }
}
