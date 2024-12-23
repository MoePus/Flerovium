package com.moepus.flerovium.mixins.Entity;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.level.CommonLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin implements CommonLevelAccessor {
    @Unique
    List<VoxelShape> flerovium$getPlayerCollisions(Entity entity, AABB aabb) {
        if (aabb.getSize() < 1.0E-7D) {
            return List.of();
        }
        List<Entity> list = this.getEntities(entity, aabb.inflate(1.0E-7D), EntitySelector.NO_SPECTATORS.and(entity::canCollideWith));
        if (list.isEmpty()) {
            return List.of();
        }
        ImmutableList.Builder<VoxelShape> builder = ImmutableList.builderWithExpectedSize(list.size());

        for (Entity e : list) {
            builder.add(Shapes.create(e.getBoundingBox()));
        }

        return builder.build();
    }

    @Override
    public @NotNull List<VoxelShape> getEntityCollisions(@Nullable Entity entity, AABB aabb) {
        if (entity instanceof LocalPlayer) {
            return flerovium$getPlayerCollisions(entity, aabb);
        }
        return List.of();
    }
}
