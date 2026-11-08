package com.example.dragonmaid.combat;

import com.example.dragonmaid.data.HumanoidWater;
import com.example.dragonmaid.network.BreathImpactMessage;
import com.example.dragonmaid.network.BreathWaveMessage;
import com.example.dragonmaid.network.DragonNetwork;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.IceDragonEntity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.event.level.LevelEvent.Unload;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import com.example.dragonmaid.compat.net.PacketDistributor;

@EventBusSubscriber(
   modid = "dragonmaid"
)
public final class MovingBreaths {
   private static final Map<ServerLevel, List<MovingBreaths.Wave>> ACTIVE = new WeakHashMap<>();
   private static final int MAX_PER_DRAGON = 4;
   private static long nextWaveId;

   private MovingBreaths() {
   }

   public static void emit(DragonBaseEntity dragon, LivingEntity target, BreathProfile profile) {
      if (dragon.level() instanceof ServerLevel level && profile.canHit(dragon, target)) {
         List<MovingBreaths.Wave> waves = ACTIVE.computeIfAbsent(level, ignored -> new ArrayList<>());
         if (waves.stream().filter(w -> w.owner.equals(dragon.getUUID())).count() >= 4L) {
            return;
         }

         Vec3 origin = HumanoidWater.breathOrigin(dragon);
         Vec3 direction = target.getBoundingBox().getCenter().subtract(origin).normalize();
         if (direction.lengthSqr() < 0.5) {
            return;
         }

         BreathFan fan = new BreathFan(origin, direction, profile.range(), profile.halfAngle(), profile.speed());
         waves.add(
            new MovingBreaths.Wave(
               ++nextWaveId,
               dragon.getUUID(),
               profile.stage(),
               fan,
               HumanoidCombat.breathDamageAmount(dragon, profile.stage()),
               DragonCombatAccessories.fireFang(dragon)
            )
         );
         BreathAnimationSync.shot(dragon, 1);
         DragonNetwork.CHANNEL
            .send(
               PacketDistributor.TRACKING_ENTITY.with(() -> dragon),
               new BreathWaveMessage(
                  level.dimension().location(), fan, dragon instanceof IceDragonEntity, level.getGameTime(), dragon.getId(), dragon.getUUID()
               )
            );
         return;
      }
   }

   @SubscribeEvent
   public static void tick(LevelTickEvent.Post event) {
      if (event.getLevel() instanceof ServerLevel level) {
         advance(level);
      }
   }

   public static void advance(ServerLevel level) {
      List<MovingBreaths.Wave> waves = ACTIVE.get(level);
      if (waves != null) {
         waves.removeIf(wave -> !wave.tick(level));
         if (waves.isEmpty()) {
            ACTIVE.remove(level);
         }
      }
   }

   @SubscribeEvent
   public static void unload(Unload event) {
      ACTIVE.remove(event.getLevel());
   }

   @SubscribeEvent
   public static void stopped(ServerStoppedEvent event) {
      ACTIVE.clear();
   }

   public static int activeCount(ServerLevel level, UUID owner) {
      return (int)ACTIVE.getOrDefault(level, List.of()).stream().filter(w -> w.owner.equals(owner)).count();
   }

   private static final class Wave {
      private final long id;
      private final UUID owner;
      private final int stage;
      private final BreathFan fan;
      private final float damage;
      private final boolean poison;
      private final Set<UUID> hit = new HashSet<>();
      private double distance;
      private int reportedImpacts;

      Wave(long id, UUID owner, int stage, BreathFan fan, float damage, boolean poison) {
         this.id = id;
         this.owner = owner;
         this.stage = stage;
         this.fan = fan;
         this.damage = damage;
         this.poison = poison;
      }

      boolean tick(ServerLevel level) {
         if (level.getEntity(this.owner) instanceof DragonBaseEntity dragon && dragon.isAlive() && !dragon.isModelDead()) {
            double front = Math.min(this.fan.range(), this.distance + this.fan.speed());
            List<Vec3> impacts = null;

            for (LivingEntity enemy : level.getEntitiesOfClass(
               LivingEntity.class, this.fan.bounds(this.distance, front), e -> HumanoidTargetGoal.isEnemy(dragon, e) && !this.hit.contains(e.getUUID())
            )) {
               if (this.fan.intersects(enemy.getBoundingBox(), this.distance, front)
                  && level.clip(new ClipContext(this.fan.origin(), enemy.getBoundingBox().getCenter(), Block.COLLIDER, Fluid.NONE, dragon)).getType() == Type.MISS
                  )
                {
                  this.hit.add(enemy.getUUID());
                  if (HumanoidCombat.damage(dragon, enemy, this.stage, this.damage, this.poison)
                     && this.reportedImpacts < 24
                     && (impacts == null || impacts.size() < 6)) {
                     if (impacts == null) {
                        impacts = new ArrayList<>();
                     }

                     impacts.add(enemy.getBoundingBox().getCenter());
                     this.reportedImpacts++;
                  }
               }
            }

            if (impacts != null) {
               DragonNetwork.CHANNEL
                  .send(
                     PacketDistributor.TRACKING_ENTITY.with(() -> dragon),
                     new BreathImpactMessage(level.dimension().location(), this.id, level.getGameTime(), dragon instanceof IceDragonEntity, impacts)
                  );
            }

            this.distance = front;
            return this.distance < this.fan.range();
         }

         return false;
      }
   }
}
