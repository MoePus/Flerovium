package com.moepus.flerovium.mixins.Block;

import com.moepus.flerovium.functions.BlockBreaking.BlockBreakingDecalGenerator;
import com.moepus.flerovium.functions.BlockBreaking.BlockBreakingRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nullable;
import java.util.SortedSet;

@Mixin(value = LevelRenderer.class, remap = false)
public abstract class CrumblingRendererMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Nullable
    private ClientLevel level;

    @Shadow
    @Final
    private RenderBuffers renderBuffers;

    @Shadow
    public abstract Frustum getFrustum();

    @Redirect(method = "renderLevel", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;long2ObjectEntrySet()Lit/unimi/dsi/fastutil/objects/ObjectSet;"))
    private ObjectSet<Long2ObjectMap.Entry<SortedSet<BlockDestructionProgress>>> fasterBlockBreakingRendering(Long2ObjectMap<SortedSet<BlockDestructionProgress>> instance, DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix) {
        Vec3 camPos = camera.getPosition();
        var set = instance.long2ObjectEntrySet();
        var newset = new ObjectOpenHashSet<Long2ObjectMap.Entry<SortedSet<BlockDestructionProgress>>>();

        PoseStack poseStack = new PoseStack();
        for (var entry : set) {
            BlockPos pos = BlockPos.of(entry.getLongKey());
            var frustum = this.getFrustum();

            float relx = pos.getX() - (float) camPos.x;
            float rely = pos.getY() - (float) camPos.y;
            float relz = pos.getZ() - (float) camPos.z;
            if (Mth.abs(relx) + Mth.abs(relz) > 65536) { // aeronautics
                newset.add(entry);
                continue;
            }

            if (!frustum.intersection.testSphere(relx, rely, relz, 0.7f)) {
                continue;
            }

            int k = entry.getValue().last().getProgress();
            if (k < 0 || k >= ModelBakery.DESTROY_TYPES.size()) continue; // sanity check
            VertexConsumer consumer =
                    this.renderBuffers
                            .crumblingBufferSource()
                            .getBuffer(ModelBakery.DESTROY_TYPES.get(k));
            poseStack.pushPose();
            poseStack.translate(relx, rely, relz);

            VertexConsumer decal = new BlockBreakingDecalGenerator(consumer, relx, rely, relz);

            ModelData modelData = level.getModelData(pos);
            BlockBreakingRenderer.renderBreakingTexture(
                    this.minecraft.getBlockRenderer(),
                    camPos,
                    level.getBlockState(pos),
                    pos,
                    level,
                    poseStack,
                    decal,
                    modelData
            );

            poseStack.popPose();
        }
        return newset;
    }
}
