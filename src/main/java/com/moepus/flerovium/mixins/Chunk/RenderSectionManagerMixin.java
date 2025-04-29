package com.moepus.flerovium.mixins.Chunk;

import it.unimi.dsi.fastutil.objects.ReferenceSet;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildOutput;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import org.apache.commons.lang3.ArrayUtils;
import org.spongepowered.asm.mixin.*;

import java.util.ArrayList;

// Copied from https://github.com/CaffeineMC/sodium/pull/2886
// Avoid Unnecessary Graph Searches by Checking for State Changes
// By douira
@Mixin(value = RenderSectionManager.class, remap = false)
public abstract class RenderSectionManagerMixin {
    @Shadow
    @Final
    private ReferenceSet<RenderSection> sectionsWithGlobalEntities;

    @Unique
    boolean flerovium$touched = false;

    @Shadow
    private ArrayList<ChunkBuildOutput> collectChunkBuildResults() {
        return null;
    }

    @Shadow
    private void processChunkBuildResults(ArrayList<ChunkBuildOutput> results) {
    }

    @Shadow
    private boolean needsUpdate;

    /**
     * @author MoePus
     * @reason Skip not changed sections
     */
    @Overwrite
    private void updateSectionInfo(RenderSection render, BuiltSectionInfo info) {
        boolean isChanged = flerovium$isSectionChanged(render, info);
        render.setInfo(info);

        if (info == null || ArrayUtils.isEmpty(info.globalBlockEntities)) {
            flerovium$touched |= this.sectionsWithGlobalEntities.remove(render);
        } else {
            flerovium$touched |= this.sectionsWithGlobalEntities.add(render);
        }
        flerovium$touched |= isChanged;
    }

    @Unique
    private boolean flerovium$isSectionChanged(Object section, BuiltSectionInfo info) {
        RenderSectionAccessor accessor = (RenderSectionAccessor) section;
        if (info == null) {
            return accessor.getBuilt();
        }
        return !accessor.getBuilt()
                || accessor.getFlags() != info.flags
                || accessor.getVisibilityData() != info.visibilityData;
    }

    /**
     * @author MoePus
     * @reason Skip not changed sections causing unnecessary graph searches
     */
    @Overwrite
    public void uploadChunks() {
        var results = this.collectChunkBuildResults();

        if (results.isEmpty()) {
            return;
        }

        flerovium$touched = false;
        this.processChunkBuildResults(results);

        for (var result : results) {
            result.delete();
        }

        this.needsUpdate |= flerovium$touched;
    }
}
