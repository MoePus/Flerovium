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

    private float nxX, nxY, nxZ, negNxW;
    private float pxX, pxY, pxZ, negPxW;
    private float nyX, nyY, nyZ, negNyW;
    private float pyX, pyY, pyZ, negPyW;
    private float nzX, nzY, nzZ, negNzW;
    private float pzX, pzY, pzZ, negPzW;

    public FastSimpleFrustum(SimpleFrustum frustum) {
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
        pzX = planes[5].x;
        pzY = planes[5].y;
        pzZ = planes[5].z;

        final float size = CHUNK_SECTION_SIZE;
        negNxW = -(planes[0].w + nxX * (nxX < 0 ? -size : size) +
                nxY * (nxY < 0 ? -size : size) +
                nxZ * (nxZ < 0 ? -size : size));
        negPxW = -(planes[1].w + pxX * (pxX < 0 ? -size : size) +
                pxY * (pxY < 0 ? -size : size) +
                pxZ * (pxZ < 0 ? -size : size));
        negNyW = -(planes[2].w + nyX * (nyX < 0 ? -size : size) +
                nyY * (nyY < 0 ? -size : size) +
                nyZ * (nyZ < 0 ? -size : size));
        negPyW = -(planes[3].w + pyX * (pyX < 0 ? -size : size) +
                pyY * (pyY < 0 ? -size : size) +
                pyZ * (pyZ < 0 ? -size : size));
        negNzW = -(planes[4].w + nzX * (nzX < 0 ? -size : size) +
                nzY * (nzY < 0 ? -size : size) +
                nzZ * (nzZ < 0 ? -size : size));
        negPzW = -(planes[5].w + pzX * (pzX < 0 ? -size : size) +
                pzY * (pzY < 0 ? -size : size) +
                pzZ * (pzZ < 0 ? -size : size));
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

    public boolean testCubeQuick(float x, float y, float z) {
        // Skip far plane checks because it has been ensured by searchDistance and isWithinRenderDistance check in OcclusionCuller
        return nxX * x + nxY * y + nxZ * z >= negNxW &&
                pxX * x + pxY * y + pxZ * z >= negPxW &&
                nyX * x + nyY * y + nyZ * z >= negNyW &&
                pyX * x + pyY * y + pyZ * z >= negPyW &&
                nzX * x + nzY * y + nzZ * z >= negNzW;
    }

    public boolean testCubeWithExtend(float floatOriginX, float floatOriginY, float floatOriginZ, float extend) {
        float minX = floatOriginX - extend;
        float maxX = floatOriginX + extend;
        float minY = floatOriginY - extend;
        float maxY = floatOriginY + extend;
        float minZ = floatOriginZ - extend;
        float maxZ = floatOriginZ + extend;

        return nxX * (nxX < 0 ? minX : maxX) + nxY * (nxY < 0 ? minY : maxY) + nxZ * (nxZ < 0 ? minZ : maxZ) >= negNxW &&
                pxX * (pxX < 0 ? minX : maxX) + pxY * (pxY < 0 ? minY : maxY) + pxZ * (pxZ < 0 ? minZ : maxZ) >= negPxW &&
                nyX * (nyX < 0 ? minX : maxX) + nyY * (nyY < 0 ? minY : maxY) + nyZ * (nyZ < 0 ? minZ : maxZ) >= negNyW &&
                pyX * (pyX < 0 ? minX : maxX) + pyY * (pyY < 0 ? minY : maxY) + pyZ * (pyZ < 0 ? minZ : maxZ) >= negPyW &&
                nzX * (nzX < 0 ? minX : maxX) + nzY * (nzY < 0 ? minY : maxY) + nzZ * (nzZ < 0 ? minZ : maxZ) >= negNzW &&
                pzX * (pzX < 0 ? minX : maxX) + pzY * (pzY < 0 ? minY : maxY) + pzZ * (pzZ < 0 ? minZ : maxZ) >= negPzW;
    }
}
