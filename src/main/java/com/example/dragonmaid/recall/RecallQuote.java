package com.example.dragonmaid.recall;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record RecallQuote(int cost, int blocks, boolean lastKnown) {
    public static final int CROSS_DIMENSION = -1;
    public static final int UNKNOWN = -2;

    public static RecallQuote unknown(int cost) {
        return new RecallQuote(cost, -2, false);
    }

    public static RecallQuote at(
        int cost, BlockPos altar, ResourceKey<Level> destination, ResourceKey<Level> sourceDimension, BlockPos source, boolean lastKnown
    ) {
        if (sourceDimension == null || source == null) {
            return unknown(cost);
        } else {
            return !destination.equals(sourceDimension)
                ? new RecallQuote(cost, -1, lastKnown)
                : new RecallQuote(cost, (int)Math.ceil(Math.sqrt(altar.distSqr(source))), lastKnown);
        }
    }
}
