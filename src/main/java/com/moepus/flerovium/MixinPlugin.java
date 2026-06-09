package com.moepus.flerovium;

import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class MixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    private ModFileInfo getModFile(String modId) {
        LoadingModList modList = LoadingModList.get();
        ModFileInfo modFile = modList.getModFileById(modId);
        if (modFile != null) {
            return modFile;
        }

        return modList.getPlugins().stream()
                .filter(ModFileInfo.class::isInstance)
                .map(ModFileInfo.class::cast)
                .filter(file -> file.getMods().stream().anyMatch(mod -> mod.getModId().equals(modId)))
                .findFirst()
                .orElse(null);
    }

    private boolean isVersionAllowed(String modId, String targetVersion) {
        ModFileInfo mod = getModFile(modId);
        if (mod == null) return true;
        VersionRange verrange;
        try {
            verrange = VersionRange.createFromVersionSpec(targetVersion);
        } catch (InvalidVersionSpecificationException e) {
            return false;
        }

        return mod.getMods().stream()
                .filter(modInfo -> modInfo.getModId().equals(modId))
                .anyMatch(modInfo -> verrange.containsVersion(modInfo.getVersion()));
    }

    private boolean doModExist(String modId) {
        return getModFile(modId) != null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return switch (mixinClassName) {
            case "com.moepus.flerovium.mixins.Entity.EntityRendererMixin",
                 "com.moepus.flerovium.mixins.Entity.ModelCuboidAccessor" ->
                    isVersionAllowed("sodium", "[0.7.0,)");
            case "com.moepus.flerovium.mixins.Particle.ReduceTerrainParticlesMixin" ->
                    Flerovium.config.reduceTerrainParticles && !doModExist("simulated");
            case "com.moepus.flerovium.mixins.Particle.ParticleEngineMixin" ->
                    !doModExist("asyncparticles") && !doModExist("particle_core");
            case "com.moepus.flerovium.mixins.Particle.SingleQuadParticleMixin" -> !doModExist("asyncparticles");
            case "com.moepus.flerovium.mixins.Particle.ParticleMixin" -> !doModExist("particle_core");
            case "com.moepus.flerovium.mixins.Sound.ClientLevelMixin" -> !doModExist("simulated");
            default -> true;
        };
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }
}
