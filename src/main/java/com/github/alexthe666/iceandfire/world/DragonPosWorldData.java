package com.github.alexthe666.iceandfire.world;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Stub for the IAF per-world dragon-position ledger (API removed in IAF CE). */
public final class DragonPosWorldData {
    private static final DragonPosWorldData INSTANCE = new DragonPosWorldData();

    private DragonPosWorldData() { }

    public static DragonPosWorldData get(ServerLevel level) { return INSTANCE; }

    public void addDragon(UUID uuid, BlockPos pos) { }

    public void removeDragon(UUID uuid) { }
}
