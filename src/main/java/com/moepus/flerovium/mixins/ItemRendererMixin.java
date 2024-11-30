package com.moepus.flerovium.mixins;

import com.moepus.flerovium.functions.FastSimpleBakedModel;
import com.moepus.flerovium.functions.FastSimpleBakedModelRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    @Final
    @Shadow
    private ItemColors itemColors;

    @Inject(method = "renderModelLists(Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/item/ItemStack;IILcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;)V", at = @At("HEAD"), cancellable = true)
    public void renderModelLists(BakedModel model, ItemStack itemStack, int packedLight, int packedOverlay, PoseStack poseStack, VertexConsumer vertexConsumer, CallbackInfo ci) {
        if (model instanceof FastSimpleBakedModel fastModel) {
            VertexBufferWriter writer = VertexBufferWriter.tryOf(vertexConsumer);
            if (writer == null)
                return;

            FastSimpleBakedModelRenderer.render(fastModel, itemStack, packedLight, packedOverlay, poseStack, writer, itemColors);
            ci.cancel();
        }
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderModelLists(Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/item/ItemStack;IILcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;)V"))
    public BakedModel onRrenderModelLists(BakedModel model, ItemStack stack, int light, int overlay, PoseStack poseStack, VertexConsumer vertexConsumer, @Local(ordinal = 0) BakedModel originalModel, @Local ItemDisplayContext itemDisplayContext) {
        if (originalModel.getClass() != SimpleBakedModel.class)
            return model;
        if (model.getClass() != SimpleBakedModel.class)
            return model;

        return new FastSimpleBakedModel((SimpleBakedModel) model, ((SimpleBakedModel) originalModel).getTransforms(), itemDisplayContext, poseStack.last());
    }
}
