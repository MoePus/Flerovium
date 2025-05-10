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
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRenderMixin extends EntityRenderer<ItemEntity> {
    protected ItemEntityRenderMixin(EntityRendererProvider.Context p_174008_) {
        super(p_174008_);
    }

    @Redirect(
            method = {"render(Lnet/minecraft/world/entity/item/ItemEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/ItemEntityRenderer;getRenderAmount(Lnet/minecraft/world/item/ItemStack;)I"
            )
    )
    private int redirectGetRenderAmount(ItemEntityRenderer instance, ItemStack itemstack, ItemEntity itemEntity) {
        int count = itemstack.getCount();
        LocalPlayer player = Minecraft.getInstance().player;
        Vec3 eye = new Vec3(player.getX(), player.getY(), player.getZ());
        int maxRender = (int) Math.ceil(360.0F / eye.distanceToSqr(itemEntity.getX(), itemEntity.getY(), itemEntity.getZ()));
        return Math.min(Math.min(count, (count - 1) / 16 + 2), Math.min(maxRender, 5));
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
        MinecraftForge.EVENT_BUS.post(renderNameTagEvent);
        // skip render
    }
}
