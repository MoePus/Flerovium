package com.moepus.flerovium.mixins.Sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientLevel.class, remap = false)
public abstract class ClientLevelMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(
            method = "playSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZJ)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onPlaySoundDistanceCull(
            double x, double y, double z, SoundEvent soundEvent, SoundSource source, float volume, float pitch, boolean distanceDelay, long seed, CallbackInfo ci
    ) {
        if (distanceDelay)
            return;
        
        double d = this.minecraft.gameRenderer.getMainCamera().getPosition().distanceToSqr(x, y, z);
        double r = soundEvent.getRange(volume);
        if (d > r * r + 1) {
            ci.cancel();
        }
    }
}
