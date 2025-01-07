package com.moepus.flerovium.functions;

import org.joml.Math;

public class MathUtil {

    static final public float[] PACK_FACTOR = {127.0f, -127.0f};
    public static int packUnsafe(float x, float y, float z, float factor) {
        int normX = (int) (x * factor) & 255;
        int normY = (int) (y * factor) & 255;
        int normZ = (int) (z * factor) & 255;

        return (normZ << 16) | (normY << 8) | normX;
    }
    public static int packUnsafe(float x, float y, float z) {
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

    public static long compose(int lo, int hi) {
        return ((long) hi << 32) | (lo & 0xFFFFFFFFL);
    }
}
