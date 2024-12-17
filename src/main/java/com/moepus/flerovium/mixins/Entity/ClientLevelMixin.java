package com.moepus.flerovium.mixins.Entity;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.CommonLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

import javax.annotation.Nullable;
import java.util.List;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin implements CommonLevelAccessor {
    @Override
    public @NotNull List<VoxelShape> getEntityCollisions(@Nullable Entity entity, AABB aabb)
    {
        return List.of();
    }
}
