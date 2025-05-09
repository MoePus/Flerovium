package com.moepus.flerovium.mixins.Item;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.IForgeBakedModel;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;

import java.util.List;

@Mixin(SimpleBakedModel.class)
public abstract class SimpleBakedModelMixin implements IForgeBakedModel {
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
                itemRenderTypes = IForgeBakedModel.super.getRenderTypes(itemStack, fabulous);
            }
            return itemRenderTypes;
        }
        if (fabulousItemRenderTypes == null) {
            fabulousItemRenderTypes = IForgeBakedModel.super.getRenderTypes(itemStack, fabulous);
        }
        return fabulousItemRenderTypes;
    }
}
