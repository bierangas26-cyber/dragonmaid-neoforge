package com.example.dragonmaid.combat;

import com.example.dragonmaid.animation.TailSweepState;
import com.example.dragonmaid.network.DragonNetwork;
import com.example.dragonmaid.network.TailSweepMessage;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.StartTracking;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import com.example.dragonmaid.compat.net.PacketDistributor;

@EventBusSubscriber(
   modid = "dragonmaid"
)
public final class TailSweepSync {
   private static final Map<DragonBaseEntity, TailSweepState> STATES = new WeakHashMap<>();
   private static long nextRound;

   private TailSweepSync() {
   }

   public static boolean isRunning(DragonBaseEntity dragon) {
      TailSweepState state = STATES.get(dragon);
      return state != null && state.running(dragon.level().getGameTime());
   }

   public static boolean isBusy(DragonBaseEntity dragon) {
      TailSweepState state = STATES.get(dragon);
      return state != null
         && state.cancelledAt() < 0L
         && dragon.level().getGameTime() >= state.startedAt()
         && (double)dragon.level().getGameTime() < (double)state.startedAt() + state.duration() + 4.0;
   }

   public static TailSweepState begin(DragonBaseEntity dragon, float yaw) {
      TailSweepState state = new TailSweepState(++nextRound, DragonCombatAccessories.wild(dragon) ? 3 : 1, dragon.level().getGameTime(), yaw, -1L);
      STATES.put(dragon, state);
      send(dragon, state);
      return state;
   }

   public static void cancel(DragonBaseEntity dragon) {
      TailSweepState previous = STATES.get(dragon);
      if (previous != null && previous.cancelledAt() < 0L && !((double)dragon.level().getGameTime() >= (double)previous.startedAt() + previous.duration() + 4.0)
         )
       {
         TailSweepState state = new TailSweepState(previous.round(), previous.revision() + 1, previous.startedAt(), previous.yaw(), dragon.level().getGameTime());
         STATES.put(dragon, state);
         send(dragon, state);
      }
   }

   private static void send(DragonBaseEntity dragon, TailSweepState state) {
      if (!dragon.level().isClientSide) {
         DragonNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> dragon), packet(dragon, state));
      }
   }

   private static TailSweepMessage packet(DragonBaseEntity dragon, TailSweepState state) {
      return new TailSweepMessage(dragon.level().dimension().location(), dragon.getId(), dragon.getUUID(), state);
   }

   @SubscribeEvent
   public static void tracking(StartTracking event) {
      if (event.getEntity() instanceof ServerPlayer player && event.getTarget() instanceof DragonBaseEntity dragon) {
         TailSweepState state = STATES.get(dragon);
         if (state != null && state.cancelledAt() < 0L && (double)dragon.level().getGameTime() < (double)state.startedAt() + state.duration() + 4.0) {
            DragonNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet(dragon, state));
         }

         return;
      }
   }

   @SubscribeEvent
   public static void leaving(EntityLeaveLevelEvent event) {
      if (!event.getLevel().isClientSide && event.getEntity() instanceof DragonBaseEntity dragon) {
         STATES.remove(dragon);
      }
   }

   @SubscribeEvent
   public static void stopped(ServerStoppedEvent event) {
      STATES.clear();
   }
}
