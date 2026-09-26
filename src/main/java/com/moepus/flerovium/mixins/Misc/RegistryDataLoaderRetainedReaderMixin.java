package com.moepus.flerovium.mixins.Misc;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.moepus.flerovium.functions.RetainedResourceReaders;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

@Mixin(value = RegistryDataLoader.class,remap = false)
public abstract class RegistryDataLoaderRetainedReaderMixin {
    @WrapMethod(method = "load(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/core/RegistryAccess;Ljava/util/List;)Lnet/minecraft/core/RegistryAccess$Frozen;")
    private static RegistryAccess.Frozen flerovium$clearRetainedReadersAfterLoad(
            ResourceManager resourceManager,
            RegistryAccess registryAccess,
            List<RegistryDataLoader.RegistryData<?>> registryData,
            Operation<RegistryAccess.Frozen> original
    ) {
        try {
            return original.call(resourceManager, registryAccess, registryData);
        } finally {
            RetainedResourceReaders.clearRetained();
        }
    }

    @Redirect(
            method = "loadElementFromResource",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/packs/resources/Resource;openAsReader()Ljava/io/BufferedReader;"
            )
    )
    private static BufferedReader flerovium$openRetainedReader(Resource resource) throws IOException {
        return RetainedResourceReaders.open(resource);
    }
}
