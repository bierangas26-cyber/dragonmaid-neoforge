package com.example.dragonmaid.network;

import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.data.HumanoidLife;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import com.example.dragonmaid.compat.net.PacketDistributor;

public final class ExecutionConfirmation {
    private static final Map<ServerPlayer, ExecutionConfirmation.Pending> PENDING = new WeakHashMap<>();

    private ExecutionConfirmation() {
    }

    private static boolean canExecute(ServerPlayer player, DragonBaseEntity dragon) {
        return player.isAlive()
            && !player.isSpectator()
            && player.mayBuild()
            && !dragon.isRemoved()
            && dragon.level() == player.level()
            && dragon.isOwnedBy(player)
            && HumanoidLife.isInjured(dragon)
            && dragon.getHealth() == 0.0F
            && player.getMainHandItem().is(HumanoidLife.EXECUTION_TOOLS)
            && dragon.getBoundingBox().inflate(6.0).contains(player.getEyePosition())
            && player.hasLineOfSight(dragon);
    }

    public static void request(ServerPlayer player, DragonBaseEntity dragon) {
        if (canExecute(player, dragon)) {
            ExecutionConfirmation.Pending executionconfirmation$pending = new ExecutionConfirmation.Pending(
                dragon.getUUID(), player.level().dimension(), UUID.randomUUID(), player.level().getGameTime() + 600L
            );
            PENDING.put(player, executionconfirmation$pending);
            DragonNetwork.CHANNEL
                .send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new ExecutionPromptMessage(executionconfirmation$pending.dragon, executionconfirmation$pending.token, dragon.getName())
                );
        }
    }

    public static void respond(ServerPlayer player, UUID token, boolean confirmed) {
        ExecutionConfirmation.Pending executionconfirmation$pending = PENDING.get(player);
        if (executionconfirmation$pending != null && executionconfirmation$pending.token.equals(token)) {
            PENDING.remove(player);
            if (confirmed) {
                if (executionconfirmation$pending.dimension.equals(player.level().dimension())
                    && player.level().getGameTime() <= executionconfirmation$pending.expires
                    && player.serverLevel().getEntity(executionconfirmation$pending.dragon) instanceof DragonBaseEntity entitydragonbase
                    && canExecute(player, entitydragonbase)) {
                    DragonForms.state(entitydragonbase).dragonmaid$setExecuted(true);
                    entitydragonbase.setHealth(0.0F);
                    entitydragonbase.setInSittingPose(false);
                    entitydragonbase.setBreathingFire(false);
                    entitydragonbase.setTarget(null);
                    DragonForms.state(entitydragonbase).dragonmaid$setAction(0);
                    player.displayClientMessage(Component.translatable("message.dragonmaid.executed"), true);
                } else {
                    player.displayClientMessage(Component.translatable("message.dragonmaid.execution_expired"), true);
                }
            }
        }
    }

    private static record Pending(UUID dragon, ResourceKey<Level> dimension, UUID token, long expires) {
    }
}
