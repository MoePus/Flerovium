package com.moepus.flerovium.functions;

import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class MatrixStuff {
    public static void rotateXY(Matrix4f dest, float sinX, float cosX, float sinY, float cosY) {
        final float lm00 = dest.m00(), lm01 = dest.m01(), lm02 = dest.m02(), lm03 = dest.m03(),
                lm10 = dest.m10(), lm11 = dest.m11(), lm12 = dest.m12(), lm13 = dest.m13(),
                lm20 = dest.m20(), lm21 = dest.m21(), lm22 = dest.m22(), lm23 = dest.m23();

        final float m_sinX = -sinX;
        final float m_sinY = -sinY;

        final float xm20 = Math.fma(lm10, m_sinX, lm20 * cosX);
        final float xm21 = Math.fma(lm11, m_sinX, lm21 * cosX);
        final float xm22 = Math.fma(lm12, m_sinX, lm22 * cosX);
        final float xm23 = Math.fma(lm13, m_sinX, lm23 * cosX);
        final float xm10 = Math.fma(lm10, cosX, lm20 * sinX);
        final float xm11 = Math.fma(lm11, cosX, lm21 * sinX);
        final float xm12 = Math.fma(lm12, cosX, lm22 * sinX);
        final float xm13 = Math.fma(lm13, cosX, lm23 * sinX);
        final float nm00 = Math.fma(lm00, cosY, xm20 * m_sinY);
        final float nm01 = Math.fma(lm01, cosY, xm21 * m_sinY);
        final float nm02 = Math.fma(lm02, cosY, xm22 * m_sinY);
        final float nm03 = Math.fma(lm03, cosY, xm23 * m_sinY);
        final float ym20 = Math.fma(lm00, sinY, xm20 * cosY);
        final float ym21 = Math.fma(lm01, sinY, xm21 * cosY);
        final float ym22 = Math.fma(lm02, sinY, xm22 * cosY);
        final float ym23 = Math.fma(lm03, sinY, xm23 * cosY);
        dest.set(nm00, nm01, nm02, nm03,
                xm10, xm11, xm12, xm13,
                ym20, ym21, ym22, ym23,
                dest.m30(), dest.m31(), dest.m32(), dest.m33());
    }

    public static void rotateXY(Matrix3f dest, float sinX, float cosX, float sinY, float cosY) {
        final float m_sinX = -sinX;
        final float m_sinY = -sinY;

        final float nm10 = Math.fma(dest.m10, cosX, dest.m20 * sinX);
        final float nm11 = Math.fma(dest.m11, cosX, dest.m21 * sinX);
        final float nm12 = Math.fma(dest.m12, cosX, dest.m22 * sinX);
        final float nm20 = Math.fma(dest.m10, m_sinX, dest.m20 * cosX);
        final float nm21 = Math.fma(dest.m11, m_sinX, dest.m21 * cosX);
        final float nm22 = Math.fma(dest.m12, m_sinX, dest.m22 * cosX);
        final float nm00 = Math.fma(dest.m00, cosY, nm20 * m_sinY);
        final float nm01 = Math.fma(dest.m01, cosY, nm21 * m_sinY);
        final float nm02 = Math.fma(dest.m02, cosY, nm22 * m_sinY);
        dest.m20 = Math.fma(dest.m00, sinY, nm20 * cosY);
        dest.m21 = Math.fma(dest.m01, sinY, nm21 * cosY);
        dest.m22 = Math.fma(dest.m02, sinY, nm22 * cosY);
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m02 = nm02;
        dest.m10 = nm10;
        dest.m11 = nm11;
        dest.m12 = nm12;
    }
}
