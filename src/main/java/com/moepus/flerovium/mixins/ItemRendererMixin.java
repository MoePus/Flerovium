package com.moepus.flerovium.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.moepus.flerovium.functions.SimpleBakedModelRenderer;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    @Final
    @Shadow
    private ItemColors itemColors;

    @Inject(method = "renderModelLists(Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/item/ItemStack;IILcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;)V", at = @At("HEAD"), cancellable = true)
    public void renderModelLists(BakedModel model, ItemStack itemStack, int packedLight, int packedOverlay, PoseStack poseStack, VertexConsumer vertexConsumer, CallbackInfo ci) {
        if (model.getClass() != SimpleBakedModel.class)
            return;

        if(poseStack.poseStack.size() <= 2)
            return;

        VertexBufferWriter writer = VertexBufferWriter.tryOf(vertexConsumer);
        if (writer == null)
            return;

        SimpleBakedModelRenderer.render(model, itemStack, packedLight, packedOverlay, poseStack, writer, itemColors);
        ci.cancel();
    }
}
