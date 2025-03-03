package com.moepus.flerovium.mixins.Entity;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import net.caffeinemc.mods.sodium.client.render.immediate.model.EntityRenderer;
import net.caffeinemc.mods.sodium.client.render.immediate.model.ModelCuboid;
import org.joml.Matrix3f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static net.caffeinemc.mods.sodium.client.render.immediate.model.ModelCuboid.*;

@Mixin(value = EntityRenderer.class, remap = false)
public abstract class EntityRendererMixin {
    @Shadow
    private static void setVertex(int vertexIndex, float x, float y, float z, int color) {
    }

    @Unique
    private static int flerovium$FACE = ~0;

    /**
     * @author MoePus
     * @reason BackFace Culling
     */
    @Overwrite
    private static void prepareVertices(PoseStack.Pose matrices, ModelCuboid cuboid, int color) {
        var pose = matrices.pose();

        float vxx = (pose.m00() * cuboid.sizeX), vxy = (pose.m01() * cuboid.sizeX), vxz = (pose.m02() * cuboid.sizeX);
        float vyx = (pose.m10() * cuboid.sizeY), vyy = (pose.m11() * cuboid.sizeY), vyz = (pose.m12() * cuboid.sizeY);
        float vzx = (pose.m20() * cuboid.sizeZ), vzy = (pose.m21() * cuboid.sizeZ), vzz = (pose.m22() * cuboid.sizeZ);

        // Compute the transformed origin point of the cuboid
        float c000x = MatrixHelper.transformPositionX(pose, cuboid.originX, cuboid.originY, cuboid.originZ);
        float c000y = MatrixHelper.transformPositionY(pose, cuboid.originX, cuboid.originY, cuboid.originZ);
        float c000z = MatrixHelper.transformPositionZ(pose, cuboid.originX, cuboid.originY, cuboid.originZ);
        setVertex(VERTEX_X0_Y0_Z0, c000x, c000y, c000z, color);

        // Add the pre-multiplied vectors to find the other 7 vertices
        // This avoids needing to multiply each vertex position against the pose matrix, which eliminates many
        // floating-point operations (going from 21 flops/vert to 3 flops/vert).
        // Originally suggested by MoePus on GitHub in this pull request:
        //  https://github.com/CaffeineMC/sodium/pull/2960
        float c100x = c000x + vxx;
        float c100y = c000y + vxy;
        float c100z = c000z + vxz;
        setVertex(VERTEX_X1_Y0_Z0, c100x, c100y, c100z, color);

        float c110x = c100x + vyx;
        float c110y = c100y + vyy;
        float c110z = c100z + vyz;
        setVertex(VERTEX_X1_Y1_Z0, c110x, c110y, c110z, color);

        float c010x = c000x + vyx;
        float c010y = c000y + vyy;
        float c010z = c000z + vyz;
        setVertex(VERTEX_X0_Y1_Z0, c010x, c010y, c010z, color);

        float c001x = c000x + vzx;
        float c001y = c000y + vzy;
        float c001z = c000z + vzz;
        setVertex(VERTEX_X0_Y0_Z1, c001x, c001y, c001z, color);

        float c101x = c100x + vzx;
        float c101y = c100y + vzy;
        float c101z = c100z + vzz;
        setVertex(VERTEX_X1_Y0_Z1, c101x, c101y, c101z, color);

        float c111x = c110x + vzx;
        float c111y = c110y + vzy;
        float c111z = c110z + vzz;
        setVertex(VERTEX_X1_Y1_Z1, c111x, c111y, c111z, color);

        float c011x = c010x + vzx;
        float c011y = c010y + vzy;
        float c011z = c010z + vzz;
        setVertex(VERTEX_X0_Y1_Z1, c011x, c011y, c011z, color);

        flerovium$FACE = ~0;
        if (matrices.pose().m32() <= -16.0F && RenderSystem.getModelViewMatrix().m32() == 0) {
            Matrix3f normal = matrices.normal();

// We dont know if this cuboid is mirrored or not, so dont cull +-x faces
//            float posX = c000x + c011x;
//            float posY = c000y + c011y;
//            float posZ = c000z + c011z;
//            if (posX * normal.m00 + posY * normal.m01 + posZ * normal.m02 < 0) flerovium$FACE &= ~(1 << FACE_POS_X);
//
//            posX = c100x + c111x;
//            posY = c100y + c111y;
//            posZ = c100z + c111z;
//            if (posX * normal.m00 + posY * normal.m01 + posZ * normal.m02 > 0) flerovium$FACE &= ~(1 << FACE_NEG_X);

            float posX = c000x + c110x;
            float posY = c000y + c110y;
            float posZ = c000z + c110z;
            if (posX * normal.m20 + posY * normal.m21 + posZ * normal.m22 < 0) flerovium$FACE &= ~(1 << FACE_NEG_Z);

            posX = c001x + c111x;
            posY = c001y + c111y;
            posZ = c001z + c111z;
            if (posX * normal.m20 + posY * normal.m21 + posZ * normal.m22 > 0) flerovium$FACE &= ~(1 << FACE_POS_Z);

            posX = c000x + c101x;
            posY = c000y + c101y;
            posZ = c000z + c101z;
            if (posX * normal.m10 + posY * normal.m11 + posZ * normal.m12 < 0) flerovium$FACE &= ~(1 << FACE_NEG_Y);

            posX = c010x + c111x;
            posY = c010y + c111y;
            posZ = c010z + c111z;
            if (posX * normal.m10 + posY * normal.m11 + posZ * normal.m12 > 0) flerovium$FACE &= ~(1 << FACE_POS_Y);
        }
    }

    @Redirect(method = "emitQuads", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/immediate/model/ModelCuboid;shouldDrawFace(I)Z"))
    private static boolean onShouldDrawFace(ModelCuboid cuboid, int face) {
        return (flerovium$FACE & (1 << face)) != 0 && cuboid.shouldDrawFace(face);
    }
}
