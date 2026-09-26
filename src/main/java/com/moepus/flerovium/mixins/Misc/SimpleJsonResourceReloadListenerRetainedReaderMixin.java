package com.moepus.flerovium.mixins.Misc;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.moepus.flerovium.functions.RetainedResourceReaders;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;

@Mixin(value = SimpleJsonResourceReloadListener.class, remap = false)
public abstract class SimpleJsonResourceReloadListenerRetainedReaderMixin {
    @WrapMethod(method = "scanDirectory")
    private static void flerovium$clearRetainedReadersAfterScanDirectory(
            ResourceManager resourceManager,
            String directory,
            Gson gson,
            Map<ResourceLocation, JsonElement> output,
            Operation<Void> original
    ) {
        try {
            original.call(resourceManager, directory, gson, output);
        } finally {
            RetainedResourceReaders.clearRetained();
        }
    }

    @Redirect(
            method = "scanDirectory",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/packs/resources/Resource;openAsReader()Ljava/io/BufferedReader;"
            )
    )
    private static BufferedReader flerovium$openRetainedReader(Resource resource) throws IOException {
        return RetainedResourceReaders.open(resource);
    }
}
