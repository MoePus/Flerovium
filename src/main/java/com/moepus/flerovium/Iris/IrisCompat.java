package com.moepus.flerovium.Iris;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.neoforged.fml.ModList;
import java.lang.reflect.Field;

public final class IrisCompat {
    private IrisCompat() {
    }

    public static final boolean IS_IRIS_INSTALLED = ModList.get().isLoaded("iris");

    static VertexFormat GetEntityVertexFormat() {
        try {
            Class<?> irisVertexFormats = Class.forName("net.irisshaders.iris.vertices.IrisVertexFormats");
            Field field = irisVertexFormats.getDeclaredField("ENTITY");
            return (VertexFormat) field.get(null);
        } catch (Exception e) {
            return null;
        }
    }
}
