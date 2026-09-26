package com.moepus.flerovium.mixins.Misc;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.moepus.flerovium.functions.RetainedResourceReaders;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

@Mixin(value = TagLoader.class, remap = false)
public abstract class TagLoaderRetainedReaderMixin {
    @WrapMethod(method = "loadAndBuild")
    private Map<ResourceLocation, Collection<Object>> turboboot$clearRetainedReadersAfterLoadAndBuild(
            ResourceManager resourceManager,
            Operation<Map<ResourceLocation, Collection<Object>>> original
    ) {
        try {
            return original.call(resourceManager);
        } finally {
            RetainedResourceReaders.clearRetained();
        }
    }

    @Redirect(
            method = "load",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/packs/resources/Resource;openAsReader()Ljava/io/BufferedReader;"
            )
    )
    private BufferedReader turboboot$openRetainedReader(Resource resource) throws IOException {
        return RetainedResourceReaders.open(resource);
    }
}
