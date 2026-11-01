package com.example.dragonmaid.data;

import net.minecraft.nbt.CompoundTag;

public final class LifeStateMigration {
    public static final int SAVE_VERSION = 6;

    private LifeStateMigration() {
    }

    public static boolean trackedOnLoad(CompoundTag state, int deathStage) {
        return state.contains("LifeTracked")
            ? state.getBoolean("LifeTracked")
            : state.contains("Executed") && state.getBoolean("Humanoid") && (state.getBoolean("Executed") || deathStage == 0);
    }

    public static boolean restoreLegacyInjuryAi(CompoundTag entity, boolean tamed, boolean hasOwner, int deathStage) {
        if (tamed && hasOwner && deathStage == 0 && entity.contains("DragonMaid", 10) && entity.contains("NoAI", 1) && entity.getBoolean("NoAI")) {
            CompoundTag compoundtag = entity.getCompound("DragonMaid");
            if (!compoundtag.contains("Version", 3)) {
                return false;
            } else {
                int i = compoundtag.getInt("Version");
                return i >= 1
                    && i <= 5
                    && compoundtag.contains("LifeTracked", 1)
                    && compoundtag.getBoolean("LifeTracked")
                    && compoundtag.contains("Executed", 1)
                    && !compoundtag.getBoolean("Executed");
            }
        } else {
            return false;
        }
    }
}
