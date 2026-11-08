package com.example.dragonmaid.command;

import com.iafenvoy.iceandfire.config.IafCommonConfig;
import com.iafenvoy.iceandfire.entity.DragonEggEntity;
import com.iafenvoy.iceandfire.item.block.entity.EggInIceBlockEntity;
import com.iafenvoy.iceandfire.registry.IafDragonTypes;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Comparator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

/** 1.2: /dragonmaid egg remaining <seconds> —— 把最近一枚龙蛋的孵化倒计时改到剩余 seconds 秒。 */
public final class EggIncubation {
    private EggIncubation() {
    }

    public static int setNearest(CommandSourceStack source, int seconds) throws CommandSyntaxException {
        if (!source.hasPermission(2)) {
            throw CompanionCommands.error("permission");
        } else {
            ServerPlayer player = source.getPlayerOrException();
            ServerLevel level = player.serverLevel();
            Vec3 origin = player.position();
            DragonEggEntity entity = level.getEntitiesOfClass(DragonEggEntity.class, player.getBoundingBox().inflate(32.0), e -> e.distanceToSqr(player) <= 1024.0)
                .stream()
                .min(Comparator.comparingDouble(player::distanceToSqr))
                .orElse(null);
            EggInIceBlockEntity frozen = null;
            double closest = entity == null ? 1024.0 : entity.distanceToSqr(player);
            int minX = Math.floorDiv(player.blockPosition().getX() - 32, 16);
            int maxX = Math.floorDiv(player.blockPosition().getX() + 32, 16);
            int minZ = Math.floorDiv(player.blockPosition().getZ() - 32, 16);
            int maxZ = Math.floorDiv(player.blockPosition().getZ() + 32, 16);

            for (int cx = minX; cx <= maxX; cx++) {
                for (int cz = minZ; cz <= maxZ; cz++) {
                    LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                    if (chunk != null) {
                        for (BlockEntity block : chunk.getBlockEntities().values()) {
                            if (block instanceof EggInIceBlockEntity) {
                                EggInIceBlockEntity egg = (EggInIceBlockEntity)block;
                                if (egg.type != null) {
                                    double distance = Vec3.atCenterOf(egg.getBlockPos()).distanceToSqr(origin);
                                    if (distance < closest) {
                                        frozen = egg;
                                        entity = null;
                                        closest = distance;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (entity == null && frozen == null) {
                throw CompanionCommands.error("egg");
            } else {
                int ticks = Math.multiplyExact(seconds, 20);
                if (frozen != null) {
                    frozen.age = IafCommonConfig.INSTANCE.dragon.eggBornTime.getValue() - ticks;
                    frozen.setChanged();
                    level.sendBlockUpdated(frozen.getBlockPos(), frozen.getBlockState(), frozen.getBlockState(), 3);
                } else {
                    // IAF CE 2.2 的 DragonEggEntity#getEggType 返回 DragonColor（不是枚举），冰蛋靠 dragonType 判断
                    if (entity.getEggType().getType() == IafDragonTypes.ICE) {
                        throw CompanionCommands.error("ice_egg");
                    }

                    long age = (long)IafCommonConfig.INSTANCE.dragon.eggBornTime.getValue() + 1L - (long)ticks;
                    if (age > 2147483647L) {
                        throw CompanionCommands.error("range");
                    }

                    entity.setDragonAge((int)age);
                }

                source.sendSuccess(() -> Component.translatable("debug.dragonmaid.egg", new Object[]{seconds}), false);
                return 1;
            }
        }
    }
}
