package com.moepus.flerovium.mixins.Sound;

import com.mojang.blaze3d.audio.Library;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
    @Shadow
    private boolean loaded;

    @Shadow
    @Final
    private Library library;

    @Inject(
            method = "play",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/resources/sounds/SoundInstance;resolve(Lnet/minecraft/client/sounds/SoundManager;)Lnet/minecraft/client/sounds/WeighedSoundEvents;",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void onPlaySound(SoundInstance p_sound, CallbackInfo ci) {
        var sound = p_sound.getSound();
        if (sound == null)
            return;
        boolean shouldStream = sound.shouldStream();
        if (shouldStream)
            return;
        var pool = this.library.staticChannels;
        if (pool.getUsedCount() >= pool.getMaxCount())
            ci.cancel();
    }
}
