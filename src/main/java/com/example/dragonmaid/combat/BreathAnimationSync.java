package com.example.dragonmaid.combat;

import com.example.dragonmaid.animation.BreathState;
import com.example.dragonmaid.network.BreathAnimationMessage;
import com.example.dragonmaid.network.BreathForecastMessage;
import com.example.dragonmaid.network.DragonNetwork;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.StartTracking;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import com.example.dragonmaid.compat.net.PacketDistributor;

@EventBusSubscriber(
   modid = "dragonmaid"
)
public final class BreathAnimationSync {
   private static final Map<DragonBaseEntity, BreathAnimationSync.Entry> STATES = new WeakHashMap<>();
   private static long nextRound;

   private BreathAnimationSync() {
   }

   public static void beginTick(DragonBaseEntity dragon) {
      BreathAnimationSync.Entry entry = STATES.get(dragon);
      if (entry != null) {
         entry.forecastValid = false;
      }
   }

   public static void forecast(DragonBaseEntity dragon, int ticksUntilFire) {
      BreathAnimationSync.Entry entry = STATES.get(dragon);
      if (entry != null && entry.round.state().running() && ticksUntilFire > 0 && ticksUntilFire <= 6) {
         if (entry.round.state().lightning() || entry.round.state().firstAt() >= 0L) {
            long at = dragon.level().getGameTime() + (long)ticksUntilFire;
            int sequence = entry.round.state().sequence() + 1;
            entry.forecastValid = true;
            if (entry.forecastAt != at || entry.forecastSequence != sequence) {
               entry.forecastAt = at;
               entry.forecastSequence = sequence;
               entry.forecastRevision++;
            }
         }
      }
   }

   public static void begin(DragonBaseEntity dragon, int ticks, boolean lightning) {
      begin(dragon, ticks, lightning, false);
   }

   public static void begin(DragonBaseEntity dragon, int ticks, boolean lightning, boolean melee) {
      if (!TailSweepSync.isBusy(dragon)) {
         BreathAnimationSync.Entry entry = STATES.get(dragon);
         if (entry == null || !entry.round.state().running()) {
            STATES.put(dragon, new BreathAnimationSync.Entry(new BreathState.Round(++nextRound, dragon.level().getGameTime(), ticks, lightning, melee)));
         }
      }
   }

   public static void phase(DragonBaseEntity dragon, int phase) {
      updatePhase(dragon, phase);
   }

   static boolean activate(DragonBaseEntity dragon) {
      return updatePhase(dragon, 2);
   }

   private static boolean updatePhase(DragonBaseEntity dragon, int phase) {
      BreathAnimationSync.Entry entry = STATES.get(dragon);
      if (entry == null) {
         return false;
      } else {
         BreathState before = entry.round.state();
         entry.round.phase(phase, dragon.level().getGameTime());
         return entry.round.state() != before;
      }
   }

   public static void preparationRemaining(DragonBaseEntity dragon, int remaining) {
      BreathAnimationSync.Entry entry = STATES.get(dragon);
      if (entry != null) {
         entry.round.preparationRemaining(remaining, dragon.level().getGameTime());
      }
   }

   public static void shot(DragonBaseEntity dragon, int type) {
      BreathAnimationSync.Entry entry = STATES.get(dragon);
      if (entry != null) {
         entry.round.shot(type, dragon.level().getGameTime());
      }
   }

   public static void flush(DragonBaseEntity dragon) {
      BreathAnimationSync.Entry entry = STATES.get(dragon);
      if (entry != null) {
         BreathState state = entry.round.state();
         if ((!entry.forecastValid || !state.running() || state.sequence() >= entry.forecastSequence) && entry.forecastAt >= 0L) {
            entry.forecastAt = -1L;
            entry.forecastRevision++;
         }

         if (!state.lightning() && state.sequence() > entry.soundedSequence) {
            entry.soundedSequence = state.sequence();
            if (state.shotAt() == dragon.level().getGameTime()) {
               dragon.playSound(SoundEvents.BLAZE_SHOOT, 1.5F, 0.95F + dragon.getRandom().nextFloat() * 0.1F);
            }
         }

         if (entry.sentRevision != state.revision()) {
            entry.sentRevision = state.revision();
            DragonNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> dragon), packet(dragon, entry));
         }

         if (entry.sentForecastRevision != entry.forecastRevision) {
            entry.sentForecastRevision = entry.forecastRevision;
            DragonNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> dragon), forecastPacket(dragon, entry));
         }
      }
   }

   private static BreathForecastMessage forecastPacket(DragonBaseEntity dragon, BreathAnimationSync.Entry entry) {
      return new BreathForecastMessage(
         dragon.level().dimension().location(),
         dragon.getId(),
         dragon.getUUID(),
         entry.round.state().round(),
         entry.forecastRevision,
         entry.forecastSequence,
         entry.forecastAt
      );
   }

   private static BreathAnimationMessage packet(DragonBaseEntity dragon, BreathAnimationSync.Entry entry) {
      return new BreathAnimationMessage(dragon.level().dimension().location(), dragon.getId(), dragon.getUUID(), entry.round.state());
   }

   @SubscribeEvent
   public static void tracking(StartTracking event) {
      if (event.getEntity() instanceof ServerPlayer player && event.getTarget() instanceof DragonBaseEntity dragon) {
         BreathAnimationSync.Entry entry = STATES.get(dragon);
         if (entry != null) {
            DragonNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet(dragon, entry));
            if (entry.forecastAt > dragon.level().getGameTime()) {
               DragonNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), forecastPacket(dragon, entry));
            }
         }
      }
   }

   @SubscribeEvent
   public static void stopped(ServerStoppedEvent event) {
      STATES.clear();
   }

   private static final class Entry {
      BreathState.Round round;
      int sentRevision;
      int forecastRevision;
      int sentForecastRevision;
      int forecastSequence;
      int soundedSequence;
      long forecastAt = -1L;
      boolean forecastValid;

      Entry(BreathState.Round round) {
         this.round = round;
      }
   }
}
