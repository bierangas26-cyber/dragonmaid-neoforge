package com.example.dragonmaid.petrification;

import com.example.dragonmaid.recall.RecallDragonPetrification;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.StoneStatueEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityEvent.EnteringSection;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(
    modid = "dragonmaid"
)
public final class DragonPetrification {
    public static final String MARKER = "DragonMaidPetrification";

    private DragonPetrification() {
    }

    public static boolean isRestoring(DragonBaseEntity dragon) {
        return RecallDragonPetrification.isRestoring(dragon);
    }

    public static boolean protects(Entity entity) {
        return RecallDragonPetrification.protects(entity);
    }

    public static boolean isEligibleDragon(DragonBaseEntity dragon) {
        return RecallDragonPetrification.isEligibleDragon(dragon);
    }

    public static StoneStatueEntity buildStatue(LivingEntity target) {
        return RecallDragonPetrification.buildStatue(target);
    }

    public static boolean addStatue(Level world, Entity entity) {
        return RecallDragonPetrification.addStatue(world, entity);
    }

    public static InteractionResult restore(Player player, InteractionHand hand, StoneStatueEntity statue) {
        return RecallDragonPetrification.restore(player, hand, statue);
    }

    public static InteractionResult placeStatue(UseOnContext context) {
        return RecallDragonPetrification.placeStatue(context);
    }

    @SubscribeEvent(
        priority = EventPriority.HIGHEST
    )
    public static void preventDuplicateDragon(EntityJoinLevelEvent event) {
        RecallDragonPetrification.preventDuplicateDragon(event);
    }

    @SubscribeEvent
    public static void statueMoved(EnteringSection event) {
        RecallDragonPetrification.statueMoved(event);
    }

    @SubscribeEvent
    public static void finishStatueJoin(ServerTickEvent.Post event) {
        RecallDragonPetrification.finishStatueJoin(event);
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent event) {
        RecallDragonPetrification.serverStopped(event);
    }
}
