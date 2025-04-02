package com.moepus.flerovium.mixins.Entity;

import com.moepus.flerovium.functions.FastEntityRenderer;
import me.jellysquid.mods.sodium.client.render.vertex.VertexConsumerUtils;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

@Mixin(value = ModelPart.class, priority = 900)
public class ModelPartMixin {
    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V", at = @At("HEAD"), cancellable = true)
    private void onRender(PoseStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha, CallbackInfo ci) {
        VertexBufferWriter writer = VertexConsumerUtils.convertOrLog(vertices);

        if (writer == null) {
            return;
        }

        ci.cancel();

        FastEntityRenderer.render(matrices, writer, (ModelPart) (Object) this, light, overlay, ColorABGR.pack(red, green, blue, alpha));
    }
}
