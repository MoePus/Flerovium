package com.moepus.flerovium.mixins.Item;

import com.moepus.flerovium.View.SimpleBakedModelView;
import me.jellysquid.mods.sodium.client.model.quad.BakedQuadView;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.SimpleBakedModel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(SimpleBakedModel.class)
public abstract class SimpleBakedModelMixin implements SimpleBakedModelView {
    @Shadow
    @Final
    protected List<BakedQuad> unculledFaces;

    @Unique
    private boolean flerovium$hasUnassignedFaces = false;

    @Inject(method = "<init>(Ljava/util/List;Ljava/util/Map;ZZZLnet/minecraft/client/renderer/texture/TextureAtlasSprite;Lnet/minecraft/client/renderer/block/model/ItemTransforms;Lnet/minecraft/client/renderer/block/model/ItemOverrides;Lnet/minecraftforge/client/RenderTypeGroup;)V", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        for (BakedQuad quad : this.unculledFaces) {
            BakedQuadView view = (BakedQuadView) quad;
            if (view.getNormalFace() == ModelQuadFacing.UNASSIGNED) {
                flerovium$hasUnassignedFaces = true;
                break;
            }
        }
    }

    @Override
    public boolean hasUnassignedFaces() {
        return this.flerovium$hasUnassignedFaces;
    }
}
