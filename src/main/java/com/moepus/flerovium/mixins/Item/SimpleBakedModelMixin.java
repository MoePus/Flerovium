package com.moepus.flerovium.mixins.Item;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.IBakedModelExtension;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;

import java.util.List;

@Mixin(value = SimpleBakedModel.class, remap = false)
public abstract class SimpleBakedModelMixin implements IBakedModelExtension {
    @Shadow(remap = false)
    @Final
    @Mutable
    protected List<net.minecraft.client.renderer.RenderType> itemRenderTypes;
    @Shadow(remap = false)
    @Final
    @Mutable
    protected List<net.minecraft.client.renderer.RenderType> fabulousItemRenderTypes;

    /**
     * @author MoePus
     * @reason Cache Render Types
     */
    @Overwrite(remap = false)
    public @NotNull List<RenderType> getRenderTypes(@NotNull ItemStack itemStack, boolean fabulous) {
        if (!fabulous) {
            if (itemRenderTypes == null) {
                itemRenderTypes = IBakedModelExtension.super.getRenderTypes(itemStack, fabulous);
            }
            return itemRenderTypes;
        }
        if (fabulousItemRenderTypes == null) {
            fabulousItemRenderTypes = IBakedModelExtension.super.getRenderTypes(itemStack, fabulous);
        }
        return fabulousItemRenderTypes;
    }
}
