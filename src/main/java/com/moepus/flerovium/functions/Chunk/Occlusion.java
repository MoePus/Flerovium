package com.moepus.flerovium.functions.Chunk;

import me.jellysquid.mods.sodium.client.render.chunk.occlusion.GraphDirection;
import org.spongepowered.asm.mixin.Unique;

public class Occlusion {
    public static long ThroughUpDown = between(GraphDirection.UP, GraphDirection.DOWN);

    public static long ThroughNorthSouth = between(GraphDirection.NORTH, GraphDirection.SOUTH);

    public static long ThroughEastWest = between(GraphDirection.EAST, GraphDirection.WEST);

    public static int bit(int from, int to) {
        return (from * 8) + to;
    }

    public static long dir(int from, int to) {
        return (1L << bit(from, to));
    }

    public static long between(int from, int to) {
        return dir(from, to) | dir(to, from);
    }
}
