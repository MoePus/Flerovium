package com.moepus.flerovium;

import net.neoforged.fml.VersionChecker;
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

    private boolean isVersionAllowed(ModFileInfo mod, String targetVersion) {
        VersionRange verrange;
        try {
            verrange = VersionRange.createFromVersionSpec(targetVersion);
        } catch (InvalidVersionSpecificationException e) {
            return false;
        }

        return verrange.containsVersion(mod.getMods().get(0).getVersion());
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return switch (mixinClassName) {
            case "com.moepus.flerovium.mixins.Entity.EntityRendererMixin" -> Flerovium.config.entityBackFaceCulling &&
                    isVersionAllowed(LoadingModList.get().getModFileById("sodium"), "[0.7.0,)");
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