package com.moepus.flerovium.functions;

import com.moepus.flerovium.View.SimpleBakedModelView;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FastSimpleBakedModel implements BakedModel {
    private boolean face[] = new boolean[6];
    private boolean needExtraCulling = false;

    private SimpleBakedModel model;

    public FastSimpleBakedModel(SimpleBakedModel model, ItemTransforms transforms, ItemDisplayContext itemDisplayContext, PoseStack.Pose pose) {
        this.model = model;
        for (Direction direction : Direction.values()) {
            face[direction.ordinal()] = false;
        }
        DecideCull(transforms, itemDisplayContext, pose);
    }

    private void DecideCull(ItemTransforms transforms, ItemDisplayContext itemDisplayContext, PoseStack.Pose pose) {
        if (itemDisplayContext == ItemDisplayContext.GUI) { // In GUI
            if (transforms.gui == ItemTransform.NO_TRANSFORM) { // Item
                if (pose.pose().m20() == 0 && pose.pose().m21() == 0) { // Not per-transformed
                    face[Direction.SOUTH.ordinal()] = true;
                    return;
                }
            } else if (transforms.gui.rotation.equals(30.0F, 225.0F, 0.0F)) { // Block
                face[Direction.UP.ordinal()] = true;
                face[Direction.NORTH.ordinal()] = true;
                face[Direction.EAST.ordinal()] = true;
                return;
            } else if (transforms.gui.rotation.equals(30.0F, 135.0F, 0.0F)) { // Block
                face[Direction.UP.ordinal()] = true;
                face[Direction.NORTH.ordinal()] = true;
                face[Direction.WEST.ordinal()] = true;
                return;
            }
            // Don't know what object is this
            for (Direction direction : Direction.values()) {
                face[direction.ordinal()] = true;
            }
            return;
        }
        // In World
        needExtraCulling = !((SimpleBakedModelView)model).hasUnassignedFaces() && pose.pose().m32() < -3.0F && Math.abs(pose.pose().m31()) < 4F;
        if (transforms.gui == ItemTransform.NO_TRANSFORM && pose.pose().m32() < -12.0F) { // Item Far away
            face[Direction.NORTH.ordinal()] = true;
            face[Direction.SOUTH.ordinal()] = true;
            return;
        }

        // Objects near the camera or Blocks far away
        for (Direction direction : Direction.values()) {
            face[direction.ordinal()] = true;
        }
    }

    public boolean shouldRenderFace(Direction direction) {
        if (direction.ordinal() >= Direction.values().length) return false;

        return face[direction.ordinal()];
    }

    public boolean isNeedExtraCulling() {
        return needExtraCulling;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState p_235039_, @Nullable Direction p_235040_, RandomSource p_235041_) {
        return model.getQuads(p_235039_, p_235040_, p_235041_);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return model.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return model.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return model.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return model.isCustomRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return model.getParticleIcon();
    }

    @Override
    public ItemOverrides getOverrides() {
        return model.getOverrides();
    }
}
