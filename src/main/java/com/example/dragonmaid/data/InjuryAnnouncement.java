package com.example.dragonmaid.data;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;

/** 1.2: 龙进入“受伤倒地”时给主人发一条带坐标的提示。 */
public final class InjuryAnnouncement {
    private InjuryAnnouncement() {
    }

    public static void notifyOwner(DragonBaseEntity dragon) {
        if (dragon.level() instanceof ServerLevel level && dragon.getOwnerUUID() != null && level.getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES)) {
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(dragon.getOwnerUUID());
            if (owner == null) {
                return;
            }

            BlockPos position = dragon.blockPosition();
            owner.sendSystemMessage(
                Component.translatable(
                    "message.dragonmaid.dragon_injured",
                    new Object[]{
                        dragon.getDisplayName(),
                        Component.translatable(level.dimension().location().toLanguageKey("dimension")),
                        position.getX(),
                        position.getY(),
                        position.getZ()
                    }
                )
            );
            return;
        }
    }
}
