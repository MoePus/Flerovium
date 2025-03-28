package com.moepus.flerovium.mixins.Item;

import com.moepus.flerovium.functions.DummyModel;
import com.moepus.flerovium.functions.FastSimpleBakedModelRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
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

@Mixin(value = ItemRenderer.class, priority = 100, remap = false)
public abstract class ItemRendererMixin {
    @Final
    @Shadow
    private ItemColors itemColors;

    @Unique
    private static final DummyModel flerovium$dummy = new DummyModel();

    @Unique
    private static final boolean[] SHOULD_RENDER = new boolean[Direction.values().length];

    @Inject(method = "renderModelLists", at = @At("HEAD"), cancellable = true)
    public void renderModelLists(BakedModel model, ItemStack itemStack, int packedLight, int packedOverlay, PoseStack poseStack, VertexConsumer vertexConsumer, CallbackInfo ci) {
        if (model == flerovium$dummy) ci.cancel();
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderModelLists(Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/item/ItemStack;IILcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;)V"))
    public BakedModel onRrenderModelLists(BakedModel model, ItemStack stack, int light, int overlay, PoseStack poseStack, VertexConsumer vertexConsumer, @Local(ordinal = 0) BakedModel originalModel, @Local ItemDisplayContext itemDisplayContext) {
        BakedModel renderModel = model;
        BakedModel parentModel = originalModel;
        if (model instanceof ForwardingBakedModel fowardingBakedModel && originalModel instanceof ForwardingBakedModel fowardingOriginalModel) {
            if (!fowardingBakedModel.isVanillaAdapter()) return model;

            renderModel = fowardingBakedModel.getWrappedModel();
            parentModel = fowardingOriginalModel.getWrappedModel();
            if (renderModel == null || parentModel == null) return model;
        }

        if (parentModel.getClass() != SimpleBakedModel.class) return model;
        if (renderModel.getClass() != SimpleBakedModel.class) return model;

        VertexBufferWriter writer = VertexBufferWriter.tryOf(vertexConsumer);
        if (writer == null) return model;

        flerovium$decideCull(((SimpleBakedModel) parentModel).getTransforms(), itemDisplayContext, poseStack.last());
        FastSimpleBakedModelRenderer.render((SimpleBakedModel) renderModel, SHOULD_RENDER, stack, light, overlay, poseStack, writer, itemColors);

        return flerovium$dummy;
    }

    @Unique
    private void flerovium$decideCull(ItemTransforms transforms, ItemDisplayContext itemDisplayContext, PoseStack.Pose pose) {
        for (Direction direction : Direction.values()) {
            SHOULD_RENDER[direction.ordinal()] = true;
        }

        if (itemDisplayContext == ItemDisplayContext.GUI) { // In GUI
            if (transforms.gui == ItemTransform.NO_TRANSFORM) { // Item
                if (pose.pose().m20() == 0 && pose.pose().m21() == 0) { // Not per-transformed
                    SHOULD_RENDER[Direction.DOWN.ordinal()] = false;
                    SHOULD_RENDER[Direction.UP.ordinal()] = false;
                    SHOULD_RENDER[Direction.NORTH.ordinal()] = false;
                    SHOULD_RENDER[Direction.EAST.ordinal()] = false;
                    SHOULD_RENDER[Direction.WEST.ordinal()] = false;
                    return;
                }
            } else if (transforms.gui.rotation.equals(30.0F, 225.0F, 0.0F)) { // Block
                SHOULD_RENDER[Direction.DOWN.ordinal()] = false;
                SHOULD_RENDER[Direction.SOUTH.ordinal()] = false;
                SHOULD_RENDER[Direction.WEST.ordinal()] = false;
                return;
            } else if (transforms.gui.rotation.equals(30.0F, 135.0F, 0.0F)) { // Block
                SHOULD_RENDER[Direction.DOWN.ordinal()] = false;
                SHOULD_RENDER[Direction.SOUTH.ordinal()] = false;
                SHOULD_RENDER[Direction.EAST.ordinal()] = false;
                return;
            }
            // Don't know what object is this
            // Do nothing
            return;
        }
        // In World
        if (transforms.gui == ItemTransform.NO_TRANSFORM) { // Non Block Item Far away
            float distance = pose.pose().m30() * pose.pose().m30() + pose.pose().m31() * pose.pose().m31() + pose.pose().m32() * pose.pose().m32();
            if (distance > 144.0F) {
                SHOULD_RENDER[Direction.DOWN.ordinal()] = false;
                SHOULD_RENDER[Direction.UP.ordinal()] = false;
                SHOULD_RENDER[Direction.EAST.ordinal()] = false;
                SHOULD_RENDER[Direction.WEST.ordinal()] = false;
                return;
            }
        }

        // Objects near the camera or Blocks far away
        // Do nothing
    }
}
