package com.moepus.flerovium.mixins.Chunk;

import com.moepus.flerovium.functions.Chunk.Occlusion;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.occlusion.GraphDirectionSet;
import me.jellysquid.mods.sodium.client.render.chunk.occlusion.OcclusionCuller;
import me.jellysquid.mods.sodium.client.render.chunk.occlusion.VisibilityEncoding;
import me.jellysquid.mods.sodium.client.render.viewport.Viewport;
import me.jellysquid.mods.sodium.client.util.collections.ReadQueue;
import me.jellysquid.mods.sodium.client.util.collections.WriteQueue;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.*;

@Mixin(value = OcclusionCuller.class, remap = false)
public abstract class OcclusionCullerMixin {
    @Shadow
    private static boolean isSectionVisible(RenderSection section, Viewport viewport, float searchDistance) {
        return false;
    }

    @Shadow
    private static int getOutwardDirections(SectionPos origin, RenderSection section) {
        return 0;
    }

    @Shadow
    private static void visitNeighbors(final WriteQueue<RenderSection> queue, RenderSection section, int outgoing, int frame) {
    }


    /**
     * @author MoePus
     * @reason Cull More Chunks
     */
    @Overwrite
    private static void processQueue(OcclusionCuller.Visitor visitor,
                                     Viewport viewport,
                                     float searchDistance,
                                     boolean useOcclusionCulling,
                                     int frame,
                                     ReadQueue<RenderSection> readQueue,
                                     WriteQueue<RenderSection> writeQueue) {
        RenderSection section;

        while ((section = readQueue.dequeue()) != null) {
            boolean visible = isSectionVisible(section, viewport, searchDistance);
            visitor.visit(section, visible);

            if (!visible) {
                continue;
            }

            int connections;
            {
                if (useOcclusionCulling) {
                    long visibilityData = flerovium$processVisibilityByAngleOcculusion(viewport, section, section.getVisibilityData());

                    // When using occlusion culling, we can only traverse into neighbors for which there is a path of
                    // visibility through this chunk. This is determined by taking all the incoming paths to this chunk and
                    // creating a union of the outgoing paths from those.
                    connections = VisibilityEncoding.getConnections(visibilityData, section.getIncomingDirections());
                } else {
                    // Not using any occlusion culling, so traversing in any direction is legal.
                    connections = GraphDirectionSet.ALL;
                }

                // We can only traverse *outwards* from the center of the graph search, so mask off any invalid
                // directions.
                connections &= getOutwardDirections(viewport.getChunkCoord(), section);
            }

            visitNeighbors(writeQueue, section, connections, frame);
        }
    }

    // Picked from https://github.com/CaffeineMC/sodium/pull/2811
    @Unique
    private static long flerovium$processVisibilityByAngleOcculusion(Viewport viewport, RenderSection section, long visibilityData) {
        double dx = Math.abs(viewport.getTransform().x - section.getCenterX());
        double dy = Math.abs(viewport.getTransform().y - section.getCenterY());
        double dz = Math.abs(viewport.getTransform().z - section.getCenterZ());

        long mask = 0L;

        if (dx < dy || dx < dz) {
            mask |= Occlusion.ThroughEastWest;
        }
        if (dy < dx || dy < dz) {
            mask |= Occlusion.ThroughUpDown;
        }
        if (dz < dx || dz < dy) {
            mask |= Occlusion.ThroughNorthSouth;
        }

        return visibilityData & ~mask;
    }
}
