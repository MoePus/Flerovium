package com.moepus.flerovium.mixins.Entity;

import com.moepus.flerovium.functions.FastEntityRenderer;
import com.mojang.blaze3d.platform.GlStateManager;
import me.jellysquid.mods.sodium.client.model.ModelCuboidAccessor;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import java.util.Optional;

@Mixin(ModelPart.class)
public class ModelPartMixin {
    @Mutable
    @Shadow
    @Final
    private List<ModelPart.Cube> cubes;

    /**
     * @author JellySquid, embeddedt
     * @reason Rewrite entity rendering to use faster code path. Original approach of replacing the entire render loop
     * had to be neutered to accommodate mods injecting custom logic here and/or mutating the models at runtime.
     */
    @Inject(method = "compile", at = @At("HEAD"), cancellable = true)
    private void onRender(PoseStack.Pose matrixPose, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha, CallbackInfo ci) {
        VertexBufferWriter writer = VertexBufferWriter.tryOf(vertices);

        if (writer == null) {
            return;
        }

        ci.cancel();

        FastEntityRenderer.prepareNormals(matrixPose);

        var cubes = this.cubes;
        int packedColor = ColorABGR.pack(red, green, blue, alpha);

        //noinspection ForLoopReplaceableByForEach
        for(int i = 0; i < cubes.size(); i++) {
            var cube = cubes.get(i);
            var simpleCuboid = ((ModelCuboidAccessor)cube).embeddium$getSimpleCuboid();
            if(simpleCuboid != null) {
                FastEntityRenderer.renderCuboidFast(matrixPose, writer, simpleCuboid, light, overlay, packedColor);
            } else {
                // Must use slow path as this cube can't be converted to a simple cuboid
                cube.compile(matrixPose, vertices, light, overlay, red, green, blue, alpha);
            }
        }
    }
}
