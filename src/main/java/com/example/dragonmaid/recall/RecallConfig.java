package com.example.dragonmaid.recall;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public final class RecallConfig {
    public static final ModConfigSpec SPEC;
    public static final IntValue FREE_DISTANCE;
    public static final IntValue NEAR_DISTANCE;
    public static final IntValue FAR_DISTANCE;
    public static final IntValue NEAR_COST;
    public static final IntValue FAR_COST;
    public static final IntValue REMOTE_COST;
    public static final IntValue CROSS_DIMENSION_COST;
    public static final IntValue LOAD_TIMEOUT_TICKS;
    public static final IntValue SEARCH_RADIUS;
    public static final BooleanValue CREATIVE_FREE;

    private RecallConfig() {
    }

    static {
        Builder builder = new Builder();
        builder.push("dragonRecall");
        FREE_DISTANCE = builder.comment("Same-dimension free radius in blocks.").defineInRange("freeDistance", 128, 0, 30000000);
        NEAR_DISTANCE = builder.defineInRange("nearDistance", 1024, 0, 30000000);
        FAR_DISTANCE = builder.defineInRange("farDistance", 4096, 0, 30000000);
        NEAR_COST = builder.defineInRange("nearExperiencePoints", 20, 0, 1000000);
        FAR_COST = builder.defineInRange("farExperiencePoints", 50, 0, 1000000);
        REMOTE_COST = builder.defineInRange("remoteExperiencePoints", 100, 0, 1000000);
        CROSS_DIMENSION_COST = builder.comment("Cross-dimension cost is at least one experience point above the highest same-dimension cost.")
            .defineInRange("crossDimensionExperiencePoints", 150, 0, 1000000);
        CREATIVE_FREE = builder.define("creativeFree", true);
        LOAD_TIMEOUT_TICKS = builder.comment("Temporary source chunk loading deadline. No permanently forced chunks.")
            .defineInRange("loadTimeoutTicks", 200, 20, 1200);
        SEARCH_RADIUS = builder.comment("Maximum landing search radius around the altar; only already loaded destination chunks are searched.")
            .defineInRange("landingSearchRadius", 24, 8, 48);
        builder.pop();
        SPEC = builder.build();
    }
}
