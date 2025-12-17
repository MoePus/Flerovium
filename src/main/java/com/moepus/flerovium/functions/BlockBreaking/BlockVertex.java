package com.moepus.flerovium.functions.BlockBreaking;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.*;
import org.lwjgl.system.MemoryUtil;

public class BlockVertex {
    public static final VertexFormat FORMAT;
    public static final int STRIDE = 32;

    public BlockVertex() {
    }

    public static void write(long ptr, float x, float y, float z, int color, int u, int v, int light, int normal) {
        PositionAttribute.put(ptr + 0L, x, y, z);
        ColorAttribute.set(ptr + 12L, color);
        MemoryUtil.memPutInt(ptr + 16L, u);
        MemoryUtil.memPutInt(ptr + 20L, v);
        LightAttribute.set(ptr + 24L, light);
        NormalAttribute.set(ptr + 28L, normal);
    }

    static {
        FORMAT = DefaultVertexFormat.BLOCK;
    }
}
