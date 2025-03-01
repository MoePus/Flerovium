package com.moepus.flerovium.mixins.Item;

import com.moepus.flerovium.View.SimpleBakedModelView;
import net.caffeinemc.mods.sodium.client.model.quad.BakedQuadView;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.IBakedModelExtension;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(SimpleBakedModel.class)
public abstract class SimpleBakedModelMixin implements SimpleBakedModelView, IBakedModelExtension {
    @Shadow
    @Final
    protected List<BakedQuad> unculledFaces;
    @Shadow(remap = false)
    @Final
    @Mutable
    protected List<net.minecraft.client.renderer.RenderType> itemRenderTypes;
    @Shadow(remap = false)
    @Final
    @Mutable
    protected List<net.minecraft.client.renderer.RenderType> fabulousItemRenderTypes;
    @Unique
    private boolean flerovium$hasUnassignedFaces = false;

    @Inject(method = "<init>(Ljava/util/List;Ljava/util/Map;ZZZLnet/minecraft/client/renderer/texture/TextureAtlasSprite;Lnet/minecraft/client/renderer/block/model/ItemTransforms;Lnet/minecraft/client/renderer/block/model/ItemOverrides;Lnet/neoforged/neoforge/client/RenderTypeGroup;)V", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        for (BakedQuad quad : this.unculledFaces) {
            BakedQuadView view = (BakedQuadView) quad;
            if (view.getNormalFace() == ModelQuadFacing.UNASSIGNED) {
                flerovium$hasUnassignedFaces = true;
                break;
            }
        }
    }

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

    @Override
    public boolean hasUnassignedFaces() {
        return this.flerovium$hasUnassignedFaces;
    }
}
