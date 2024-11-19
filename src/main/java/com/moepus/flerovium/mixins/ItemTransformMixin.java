package com.moepus.flerovium.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.block.model.ItemTransform;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.minecraftforge.common.util.TransformationHelper.quatFromXYZ;

@Mixin(ItemTransform.class)
public abstract class ItemTransformMixin {
    @Final
    @Shadow
    public Vector3f rotation;
    @Final
    @Shadow
    public Vector3f translation;
    @Final
    @Shadow
    public Vector3f scale;
    @Final
    @Shadow(remap = false)
    public Vector3f rightRotation;

    @Unique
    boolean flerovium$noRot = false;
    @Unique
    boolean flerovium$noTrans = false;
    @Unique
    boolean flerovium$scaleSameAndPositive = false;
    @Unique
    boolean flerovium$noRightRot = false;

    @Inject(method = "<init>(Lorg/joml/Vector3f;Lorg/joml/Vector3f;Lorg/joml/Vector3f;Lorg/joml/Vector3f;)V", at = @At("TAIL"), remap = false)
    public void init(Vector3f p_254427_, Vector3f p_254496_, Vector3f p_254022_, Vector3f rightRotation, CallbackInfo ci) {
        if (rotation.equals(0, 0, 0)) {
            flerovium$noRot = true;
        }
        if (translation.equals(0, 0, 0)) {
            flerovium$noTrans = true;
        }
        if (scale.x() == scale.y() && scale.y() == scale.z() && scale.x() > 0) {
            flerovium$scaleSameAndPositive = true;
        }
        if (rightRotation.equals(0, 0, 0)) {
            flerovium$noRightRot = true;
        }
    }

    @Inject(method = "apply(ZLcom/mojang/blaze3d/vertex/PoseStack;)V", at = @At("HEAD"), cancellable = true)
    public void apply(boolean doFlip, PoseStack pose, CallbackInfo ci) {
        if ((Object) this != ItemTransform.NO_TRANSFORM) {
            final float flip = doFlip ? -1 : 1;
            if (!flerovium$noTrans) {
                pose.translate(flip * translation.x(), translation.y(), translation.z());
            }
            if (!flerovium$noRot) {
                pose.mulPose(quatFromXYZ(rotation.x(), rotation.y() * flip, rotation.z() * flip, true));
            }
            if (flerovium$scaleSameAndPositive) {
                pose.last().pose().scale(scale.x(), scale.x(), scale.x());
            } else {
                pose.scale(scale.x(), scale.y(), scale.z());
            }
            if (!flerovium$noRightRot) {
                pose.mulPose(quatFromXYZ(rightRotation.x(), rightRotation.y() * flip, rightRotation.z() * flip, true));
            }
        }
        ci.cancel();
    }
}
