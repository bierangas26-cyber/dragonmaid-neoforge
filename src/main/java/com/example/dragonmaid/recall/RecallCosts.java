package com.example.dragonmaid.recall;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public final class RecallCosts {
    private RecallCosts() {
    }

    public static int quote(ServerLevel destination, BlockPos altar, ResourceKey<Level> sourceDimension, BlockPos source) {
        if (!destination.dimension().equals(sourceDimension)) {
            int l = Math.max((Integer)RecallConfig.NEAR_COST.get(), Math.max((Integer)RecallConfig.FAR_COST.get(), (Integer)RecallConfig.REMOTE_COST.get()));
            return Math.max((Integer)RecallConfig.CROSS_DIMENSION_COST.get(), l + 1);
        } else {
            double d0 = altar.distSqr(source);
            int i = (Integer)RecallConfig.FREE_DISTANCE.get();
            int j = Math.max(i, (Integer)RecallConfig.NEAR_DISTANCE.get());
            int k = Math.max(j, (Integer)RecallConfig.FAR_DISTANCE.get());
            if (d0 <= (double)i * (double)i) {
                return 0;
            } else if (d0 <= (double)j * (double)j) {
                return (Integer)RecallConfig.NEAR_COST.get();
            } else {
                return d0 <= (double)k * (double)k ? (Integer)RecallConfig.FAR_COST.get() : (Integer)RecallConfig.REMOTE_COST.get();
            }
        }
    }

    public static boolean isFree(ServerPlayer player) {
        return player.getAbilities().instabuild && (Boolean)RecallConfig.CREATIVE_FREE.get();
    }

    public static long availablePoints(ServerPlayer player) {
        long i = (long)Math.max(0, player.experienceLevel);
        long j = i <= 16L ? i * i + 6L * i : (i <= 31L ? (5L * i * i - 81L * i + 720L) / 2L : (9L * i * i - 325L * i + 4440L) / 2L);
        return Math.max(0L, j + (long)Math.round(player.experienceProgress * (float)player.getXpNeededForNextLevel()));
    }
}
