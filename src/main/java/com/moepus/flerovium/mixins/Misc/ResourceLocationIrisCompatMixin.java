package com.moepus.flerovium.mixins.Misc;

import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = ResourceLocation.class, priority = 900, remap = false)
public abstract class ResourceLocationIrisCompatMixin {
    /**
     * @author MoePus
     * @reason Replace Iris' cancellable HEAD inject with allocation-free equivalent behavior.
     */
    @Overwrite
    public static boolean isValidPath(String path) {
        if ("DUMMY".equals(path)) {
            return false;
        }

        for (int i = 0; i < path.length(); i++) {
            if (!validPathChar(path.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * @author MoePus
     * @reason Replace Iris' cancellable HEAD inject with allocation-free equivalent behavior.
     */
    @Overwrite
    public static boolean validPathChar(char pathChar) {
        return pathChar == '_'
                || pathChar == '-'
                || pathChar >= 'a' && pathChar <= 'z'
                || pathChar >= 'A' && pathChar <= 'Z'
                || pathChar >= '0' && pathChar <= '9'
                || pathChar == '/'
                || pathChar == '.';
    }
}
