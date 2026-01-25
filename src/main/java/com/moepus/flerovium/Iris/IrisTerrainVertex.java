package com.moepus.flerovium.Iris;

import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatDescription;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatRegistry;

import net.irisshaders.iris.uniforms.CapturedRenderingState;

import org.lwjgl.system.MemoryUtil;
import net.irisshaders.iris.vertices.IrisVertexFormats;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.*;

public class IrisTerrainVertex {
	public static final VertexFormatDescription FORMAT;
	public static final int STRIDE = 52;

	public IrisTerrainVertex() {
	}

	public static void write(long ptr, float x, float y, float z, int color, float u, float v, float mid_u, float mid_v, int light, int normal, int tangent) {
		PositionAttribute.put(ptr + 0L, x, y, z);
		ColorAttribute.set(ptr + 12L, color);
		TextureAttribute.put(ptr + 16L, u, v);
		LightAttribute.set(ptr + 24L, light);
		NormalAttribute.set(ptr + 28L, normal);
		MemoryUtil.memPutShort(ptr + 32L, (short) CapturedRenderingState.INSTANCE.getCurrentRenderedEntity());
		MemoryUtil.memPutShort(ptr + 34L, (short) CapturedRenderingState.INSTANCE.getCurrentRenderedBlockEntity());
		MemoryUtil.memPutFloat(ptr + 36L, mid_u);
		MemoryUtil.memPutFloat(ptr + 40L, mid_v);
		MemoryUtil.memPutInt(ptr + 44L, tangent);
		MemoryUtil.memPutInt(ptr + 48L, 0);
	}

	static {
		FORMAT = VertexFormatRegistry.instance().get(IrisVertexFormats.TERRAIN);
	}
}
