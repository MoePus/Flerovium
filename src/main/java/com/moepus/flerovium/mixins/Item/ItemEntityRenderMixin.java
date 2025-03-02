package com.moepus.flerovium.mixins.Item;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static net.neoforged.neoforge.common.NeoForge.EVENT_BUS;

@Mixin(value = ItemEntityRenderer.class, remap = false)
public abstract class ItemEntityRenderMixin extends EntityRenderer<ItemEntity> {
    protected ItemEntityRenderMixin(EntityRendererProvider.Context p_174008_) {
        super(p_174008_);
    }

    @Redirect(
            method = "render(Lnet/minecraft/world/entity/item/ItemEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"
            )
    )
    private void skipSuperRender(EntityRenderer instance, Entity entity, float p_115037_, float p_115038_, PoseStack p_115039_, MultiBufferSource p_115040_, int p_115041_) {
        RenderNameTagEvent renderNameTagEvent = new RenderNameTagEvent(entity, entity.getDisplayName(), this, p_115039_, p_115040_, p_115041_, p_115038_);
        EVENT_BUS.post(renderNameTagEvent);
        // skip render
    }
}
