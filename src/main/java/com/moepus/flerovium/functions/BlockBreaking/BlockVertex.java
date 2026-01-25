package com.moepus.flerovium.functions.BlockBreaking;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.*;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatDescription;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatRegistry;

public class BlockVertex {
	public static final VertexFormatDescription FORMAT;
	public static final int STRIDE = 32;

	public BlockVertex() {
	}

	public static void write(long ptr, float x, float y, float z, int color, float u, float v, int light, int normal) {
		PositionAttribute.put(ptr + 0L, x, y, z);
		ColorAttribute.set(ptr + 12L, color);
		TextureAttribute.put(ptr + 16L, u, v);
		LightAttribute.set(ptr + 24L, light);
		NormalAttribute.set(ptr + 28L, normal);
	}

	static {
		FORMAT = VertexFormatRegistry.instance().get(DefaultVertexFormat.BLOCK);
	}
}
