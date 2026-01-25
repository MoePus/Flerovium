package com.moepus.flerovium.functions.BlockBreaking;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.data.ModelData;


public class BlockBreakingRenderer {
    static private final RandomSource random = RandomSource.createNewThreadLocalInstance();

    static private int calcVisibleFaces(Vec3 camPos, int blockX, int blockY, int blockZ) {
        if (RenderSystem.modelViewMatrix.m32() != 0) { // GUI
            return 0b00111111;
        }
        int faces = 0;
        double cx = camPos.x - (blockX + 0.5);
        double cy = camPos.y - (blockY + 0.5);
        double cz = camPos.z - (blockZ + 0.5);

        for (Direction dir : Direction.values()) {
            int nx = dir.getStepX();
            int ny = dir.getStepY();
            int nz = dir.getStepZ();

            // dot(normal, viewVector)
            double dot = nx * cx + ny * cy + nz * cz;

            if (dot > 0) {
                faces |= 1 << dir.ordinal();
            }
        }
        return faces;
    }

    static public void renderBreakingTexture(BlockRenderDispatcher blockRenderDispatcher, Vec3 camPos, BlockState state, BlockPos pos, BlockAndTintGetter level, PoseStack poseStack, VertexConsumer consumer, ModelData modelData) {
        if (state.getRenderShape() == RenderShape.MODEL) {
            BakedModel bakedmodel = blockRenderDispatcher.getBlockModelShaper().getBlockModel(state);
            WrappedModel wrappedModel = new WrappedModel(bakedmodel, calcVisibleFaces(camPos, pos.getX(), pos.getY(), pos.getZ()));
            long i = state.getSeed(pos);
            blockRenderDispatcher.getModelRenderer()
                    .tesselateBlock(level, wrappedModel, state, pos, poseStack, consumer, true, random, i, OverlayTexture.NO_OVERLAY, modelData, null);
        }
    }
}
