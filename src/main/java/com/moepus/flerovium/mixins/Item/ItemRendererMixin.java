package com.moepus.flerovium.mixins.Item;

import com.moepus.flerovium.Flerovium;
import com.moepus.flerovium.functions.DummyModel;
import com.moepus.flerovium.functions.FastSimpleBakedModelRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ItemRenderer.class, priority = 100)
public abstract class ItemRendererMixin {
    @Final
    @Shadow
    private ItemColors itemColors;

    @Unique
    private static final DummyModel flerovium$dummy = new DummyModel();

    @Inject(method = "renderModelLists(Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/item/ItemStack;IILcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;)V", at = @At("HEAD"), cancellable = true)
    public void renderModelLists(BakedModel model, ItemStack itemStack, int packedLight, int packedOverlay, PoseStack poseStack, VertexConsumer vertexConsumer, CallbackInfo ci) {
        if (model == flerovium$dummy) ci.cancel();
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderModelLists(Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/item/ItemStack;IILcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;)V"))
    public BakedModel onRrenderModelLists(BakedModel model, ItemStack stack, int light, int overlay, PoseStack poseStack, VertexConsumer vertexConsumer, @Local(ordinal = 0) BakedModel originalModel, @Local ItemDisplayContext itemDisplayContext) {
        if (originalModel.getClass() != SimpleBakedModel.class)
            return model;
        if (model.getClass() != SimpleBakedModel.class)
            return model;

        VertexBufferWriter writer = VertexBufferWriter.tryOf(vertexConsumer);
        if (writer == null) return model;

        int faces = flerovium$decideCull(((SimpleBakedModel) originalModel).getTransforms(), itemDisplayContext, poseStack.last());
        FastSimpleBakedModelRenderer.render((SimpleBakedModel) model, faces, stack, light, overlay, poseStack, writer, itemColors);

        return flerovium$dummy;
    }

    @Unique
    private int flerovium$decideCull(ItemTransforms transforms, ItemDisplayContext itemDisplayContext, PoseStack.Pose pose) {
        final int extraCull = 0b1000000;
        if (RenderSystem.modelViewMatrix.m32() != 0 || itemDisplayContext == ItemDisplayContext.GUI) { // In GUI
            if (itemDisplayContext != ItemDisplayContext.GUI) {
                return 0b111111;
            }
            if (transforms.gui == ItemTransform.NO_TRANSFORM) { // Item
                if (pose.pose().m20() == 0 && pose.pose().m21() == 0) { // Not per-transformed
                    return 1 << Direction.SOUTH.ordinal();
                }
            } else if (transforms.gui.rotation.equals(30.0F, 225.0F, 0.0F)) { // Block
                return (1 << Direction.UP.ordinal()) | (1 << Direction.NORTH.ordinal()) |
                        (1 << Direction.EAST.ordinal());
            } else if (transforms.gui.rotation.equals(30.0F, 135.0F, 0.0F)) { // Block
                return (1 << Direction.UP.ordinal()) | (1 << Direction.NORTH.ordinal()) |
                        (1 << Direction.WEST.ordinal());
            }
            return 0b111111;
        }
        // In World
        int faces = 0b111111;
        if (transforms.gui == ItemTransform.NO_TRANSFORM && pose.pose().m32() < -10.0F) { // Item Far away
            faces &= ((1 << Direction.NORTH.ordinal()) | (1 << Direction.SOUTH.ordinal()));
        }
        if (Flerovium.config.itemBackFaceCulling && pose.pose().m32() < -3.0F) {
            faces |= extraCull;
        }
        return faces;
    }
}
