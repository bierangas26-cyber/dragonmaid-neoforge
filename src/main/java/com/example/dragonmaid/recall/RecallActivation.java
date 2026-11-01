package com.example.dragonmaid.recall;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class RecallActivation {
    private static RecallActivation.Rule rule = (level, altar, player) -> true;

    private RecallActivation() {
    }

    public static boolean isActive(ServerLevel level, BlockPos altar, ServerPlayer player) {
        return rule.isActive(level, altar, player);
    }

    public static void setRule(RecallActivation.Rule activationRule) {
        rule = Objects.requireNonNull(activationRule);
    }

    @FunctionalInterface
    public interface Rule {
        boolean isActive(ServerLevel var1, BlockPos var2, ServerPlayer var3);
    }
}
