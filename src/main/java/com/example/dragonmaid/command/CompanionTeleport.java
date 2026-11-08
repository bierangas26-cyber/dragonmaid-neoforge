package com.example.dragonmaid.command;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/** 1.2: /dragonmaid teleport on|off —— 关闭宠物式自动传送的玩家级开关。 */
public final class CompanionTeleport {
    private static final String KEY = "DragonMaidDisableAutoTeleport";

    private CompanionTeleport() {
    }

    public static boolean enabled(Player player) {
        return !player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getBoolean(KEY);
    }

    public static boolean allowed(DragonBaseEntity dragon) {
        if (dragon.getOwner() instanceof Player player && !enabled(player)) {
            return false;
        }

        return true;
    }

    public static void set(Player player, boolean enabled) {
        CompoundTag data = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (enabled) {
            data.remove(KEY);
        } else {
            data.putBoolean(KEY, true);
        }

        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, data);
    }
}
