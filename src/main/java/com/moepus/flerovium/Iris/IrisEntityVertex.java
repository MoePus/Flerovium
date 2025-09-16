package com.moepus.flerovium.Iris;

import com.mojang.blaze3d.vertex.VertexFormat;

import net.caffeinemc.mods.sodium.api.vertex.attributes.common.ColorAttribute;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.LightAttribute;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.NormalAttribute;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.OverlayAttribute;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.PositionAttribute;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.TextureAttribute;

import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.vertices.IrisVertexFormats;

import org.lwjgl.system.MemoryUtil;

public class IrisEntityVertex {
    public static final VertexFormat FORMAT;
    public static final int STRIDE;
    public static final long ENTITY_ID_OFFSET;
    public static final long BLOCK_ENTITY_ID_OFFSET;
    public static final long ITEM_ID_OFFSET;
    public static final long MID_U_OFFSET;
    public static final long MID_V_OFFSET;
    public static final long TANGENT_OFFSET;

    public IrisEntityVertex() {
    }

    public static void write(long ptr, float x, float y, float z, int color, float u, float v, float mid_u, float mid_v, int overlay, int light, int normal, int tangent) {
        CapturedRenderingState state = CapturedRenderingState.INSTANCE;
        PositionAttribute.put(ptr + 0L, x, y, z);
        ColorAttribute.set(ptr + 12L, color);
        TextureAttribute.put(ptr + 16L, u, v);
        OverlayAttribute.set(ptr + 24L, overlay);
        LightAttribute.set(ptr + 28L, light);
        NormalAttribute.set(ptr + 32L, normal);
        MemoryUtil.memPutShort(ptr + ENTITY_ID_OFFSET, (short) state.getCurrentRenderedEntity());
        MemoryUtil.memPutShort(ptr + BLOCK_ENTITY_ID_OFFSET, (short) state.getCurrentRenderedBlockEntity());
        MemoryUtil.memPutShort(ptr + ITEM_ID_OFFSET, (short) state.getCurrentRenderedItem());
        MemoryUtil.memPutFloat(ptr + MID_U_OFFSET, mid_u);
        MemoryUtil.memPutFloat(ptr + MID_V_OFFSET, mid_v);
        MemoryUtil.memPutInt(ptr + TANGENT_OFFSET, tangent);
    }

    public static void write(long ptr, long xy, long zc, long uv, long overlayLight, int normal, long mid_uv, int tangent) {
        CapturedRenderingState state = CapturedRenderingState.INSTANCE;
        MemoryUtil.memPutLong(ptr + 0L, xy);
        MemoryUtil.memPutLong(ptr + 8L, zc);
        MemoryUtil.memPutLong(ptr + 16L, uv);
        MemoryUtil.memPutLong(ptr + 24L, overlayLight);
        NormalAttribute.set(ptr + 32L, normal);
        MemoryUtil.memPutShort(ptr + ENTITY_ID_OFFSET, (short) state.getCurrentRenderedEntity());
        MemoryUtil.memPutShort(ptr + BLOCK_ENTITY_ID_OFFSET, (short) state.getCurrentRenderedBlockEntity());
        MemoryUtil.memPutShort(ptr + ITEM_ID_OFFSET, (short) state.getCurrentRenderedItem());
        MemoryUtil.memPutLong(ptr + MID_U_OFFSET, mid_uv);
        MemoryUtil.memPutInt(ptr + TANGENT_OFFSET, tangent);
    }

    static {
        FORMAT = IrisCompat.IS_IRIS_INSTALLED ? IrisCompat.GetEntityVertexFormat() : null;
        if (FORMAT != null) {
            STRIDE = FORMAT.getVertexSize();
            ENTITY_ID_OFFSET = FORMAT.getOffset(IrisVertexFormats.ENTITY_ID_ELEMENT);
            BLOCK_ENTITY_ID_OFFSET = ENTITY_ID_OFFSET + 2;
            ITEM_ID_OFFSET = BLOCK_ENTITY_ID_OFFSET + 2;
            MID_U_OFFSET = FORMAT.getOffset(IrisVertexFormats.MID_TEXTURE_ELEMENT);
            MID_V_OFFSET = MID_U_OFFSET + 4;
            TANGENT_OFFSET = FORMAT.getOffset(IrisVertexFormats.TANGENT_ELEMENT);
        } else {
            STRIDE = 54;
            ENTITY_ID_OFFSET = 36;
            BLOCK_ENTITY_ID_OFFSET = 38;
            ITEM_ID_OFFSET = 40;
            MID_U_OFFSET = 42;
            MID_V_OFFSET = 46;
            TANGENT_OFFSET = 50;
        }
    }
}
