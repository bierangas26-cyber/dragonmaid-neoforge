package com.example.dragonmaid.combat;

import com.example.dragonmaid.accessory.MiniatureForm;
import com.example.dragonmaid.data.CompanionGrowth;
import com.example.dragonmaid.network.BreathImpactMessage;
import com.example.dragonmaid.network.BreathWaveMessage;
import com.example.dragonmaid.network.DragonNetwork;
import com.iafenvoy.iceandfire.config.IafCommonConfig;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.IceDragonEntity;
import com.iafenvoy.iceandfire.entity.LightningDragonEntity;
import com.iafenvoy.iceandfire.entity.util.dragon.IafDragonAttacks.Ground;
import iafcompat.EntityDataCompat;
import com.iafenvoy.iceandfire.registry.IafDamageTypes;
import com.iafenvoy.iceandfire.registry.IafDamageTypes.CustomEntityDamageSource;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import com.example.dragonmaid.compat.net.PacketDistributor;

@EventBusSubscriber(
   modid = "dragonmaid"
)
public final class MiniatureCombat {
   private static final Map<DragonBaseEntity, MiniatureCombat.Aim> AIMS = new WeakHashMap<>();

   private MiniatureCombat() {
   }

   @SubscribeEvent
   public static void livingTick(EntityTickEvent.Post event) {
      if (event.getEntity() instanceof DragonBaseEntity dragon && dragon.level() instanceof ServerLevel level) {
         tick(dragon, level);
      }
   }

   public static void tick(DragonBaseEntity dragon, ServerLevel level) {
      MiniaturePounce.tick(dragon);
      if (MiniaturePounce.isPouncing(dragon)) {
         MiniatureCombat.Aim aim = AIMS.get(dragon);
         if (aim != null) {
            aim.cancel();
         }
      } else if (MiniatureForm.isMatureMini(dragon) && !dragon.isNoAi() && !dragon.isSleeping() && dragon.burningTarget == null) {
         double range = (Double)MiniatureBalance.range.get();
         boolean perched = dragon.isPassenger();
         LivingEntity target;
         if (perched && dragon.getVehicle() instanceof Player carrier && dragon.isOwnedBy(carrier)) {
            target = MiniatureShoulder.target(dragon, range);
         } else {
            if (perched || dragon.getCommand() == 1) {
               AIMS.remove(dragon);
               return;
            }

            if (dragon.getControllingPassenger() instanceof Player rider) {
               target = dragon.isStriking() ? firstEnemy(dragon, rider.getEyePosition(), rider.getEyePosition().add(rider.getLookAngle().scale(range))) : null;
            } else {
               target = dragon.getTarget();
            }
         }

         long now = level.getGameTime();
         if (target != null && HumanoidTargetGoal.isEnemy(dragon, target) && !(dragon.distanceToSqr(target) > range * range)) {
            MiniatureCombat.Aim aim = AIMS.computeIfAbsent(dragon, ignored -> new MiniatureCombat.Aim());
            double pounceRange = (Double)MiniatureBalance.pounceRange.get();
            if (!perched && dragon.getControllingPassenger() == null && dragon.distanceToSqr(target) <= pounceRange * pounceRange) {
               aim.cancel();
               if (dragon.onGround() && !dragon.isFlying() && !dragon.isHovering()) {
                  if (dragon.groundAttack == Ground.FIRE) {
                     dragon.groundAttack = Ground.WING_BLAST;
                  }

                  if (now >= aim.pounceAt && MiniaturePounce.tryStart(dragon, target)) {
                     aim.pounceAt = now + (long)((Integer)MiniatureBalance.pounceCooldown.get()).intValue();
                  }
               }
            } else if (now >= aim.readyAt) {
               Vec3 origin = dragon.getHeadPosition();
               Vec3 point = target.getBoundingBox().getCenter();
               if (clear(dragon, dragon.getEyePosition(), origin) && clear(dragon, origin, point)) {
                  dragon.getLookControl().setLookAt(target, 60.0F, 45.0F);
                  if (!target.getUUID().equals(aim.target)) {
                     aim.target = target.getUUID();
                     aim.followUp = false;
                     aim.shotAt = now + (long)((Integer)MiniatureBalance.windup.get()).intValue();
                     if (dragon.getAnimation() == DragonBaseEntity.NO_ANIMATION) {
                        dragon.setAnimation(DragonBaseEntity.ANIMATION_FIRECHARGE);
                     }
                  }

                  if (now < aim.shotAt) {
                     if ((aim.shotAt - now) % 6L == 0L) {
                        level.sendParticles(element(dragon), origin.x, origin.y, origin.z, 3, 0.08, 0.08, 0.08, 0.012);
                     }
                  } else {
                     fire(dragon, level, origin, point);
                     if (dragon.getDragonStage() >= 4 && !aim.followUp) {
                        aim.followUp = true;
                        aim.shotAt = now + (long)((Integer)MiniatureBalance.betweenShots.get()).intValue();
                     } else {
                        aim.cancel();
                        aim.readyAt = now + (long)((Integer)MiniatureBalance.recovery.get()).intValue();
                     }
                  }
               } else {
                  aim.cancel();
               }
            }
         } else {
            MiniatureCombat.Aim old = AIMS.get(dragon);
            if (old != null) {
               old.cancel();
               if (now >= old.readyAt && now >= old.pounceAt) {
                  AIMS.remove(dragon);
               }
            }
         }
      } else {
         AIMS.remove(dragon);
      }
   }

   static void fire(DragonBaseEntity dragon, ServerLevel level, Vec3 origin, Vec3 point) {
      Vec3 direction = point.subtract(origin).normalize();
      if (!(direction.lengthSqr() < 0.5)) {
         int stage = dragon.getDragonStage();
         if (dragon instanceof LightningDragonEntity) {
            MiniatureLightningCharge.launch(dragon, origin, direction);
         } else {
            MiniatureBreathSounds.launch(dragon, origin);
            Vec3 desiredEnd = origin.add(direction.scale((Double)MiniatureBalance.range.get()));
            BlockHitResult blockHit = level.clip(new ClipContext(origin, desiredEnd, Block.COLLIDER, Fluid.NONE, dragon));
            Vec3 end = blockHit.getLocation();
            LivingEntity hit = firstEnemy(dragon, origin, end);
            if (hit != null) {
               end = hit.getBoundingBox().clip(origin, end).orElse(origin);
               elementalHit(dragon, hit, stage);
            }

            if (hit != null || blockHit.getType() == Type.BLOCK) {
               MiniatureBreathSounds.impact(dragon, end, 0.55F);
            }

            boolean ice = dragon instanceof IceDragonEntity;
            BreathFan beam = new BreathFan(origin, direction, Math.max(0.1, origin.distanceTo(end)), 0.0, BreathProfile.forDragon(dragon).speed());
            DragonNetwork.CHANNEL
               .send(PacketDistributor.TRACKING_ENTITY.with(() -> dragon), new BreathWaveMessage(level.dimension().location(), beam, ice, level.getGameTime()));
            if (hit != null) {
               long shotId = (long)dragon.getId() << 32 ^ level.getGameTime();
               DragonNetwork.CHANNEL
                  .send(
                     PacketDistributor.TRACKING_ENTITY.with(() -> dragon),
                     new BreathImpactMessage(level.dimension().location(), shotId, level.getGameTime(), ice, List.of(end))
                  );
            }
         }
      }
   }

   private static void elementalHit(DragonBaseEntity dragon, LivingEntity hit, int stage) {
      boolean ice = dragon instanceof IceDragonEntity;
      CustomEntityDamageSource source = ice ? IafDamageTypes.causeDragonIceDamage(dragon) : IafDamageTypes.causeDragonFireDamage(dragon);
      if (hit.hurt(source, damage(dragon))) {
         if (ice) {
            EntityDataCompat.setFrozen(hit, stage * 50);
            hit.setTicksFrozen(Math.max(hit.getTicksFrozen(), hit.getTicksRequiredToFreeze() + stage * 20));
         } else {
            hit.igniteForSeconds((float)(5 + 5 * stage));
            if (DragonCombatAccessories.fireFang(dragon)) {
               DragonCombatAccessories.poison(hit, dragon);
            }
         }
      }
   }

   public static float damage(DragonBaseEntity dragon) {
      double growth = CompanionGrowth.growthFraction(dragon);
      if (dragon instanceof LightningDragonEntity) {
         return (float)(
            ((Double)IafCommonConfig.INSTANCE.dragon.attackDamageLightning.getValue()).doubleValue()
               * 5.0
               * Math.min(0.95, (Double)MiniatureBalance.lightningBase.get() + (Double)MiniatureBalance.lightningGrowth.get() * growth)
         );
      } else {
         double nativeDamage = dragon instanceof IceDragonEntity ? ((Double)IafCommonConfig.INSTANCE.dragon.attackDamageIce.getValue()).doubleValue() : ((Double)IafCommonConfig.INSTANCE.dragon.attackDamageFire.getValue()).doubleValue();
         return (float)(
            nativeDamage
               * (double)dragon.getDragonStage()
               * ((Double)MiniatureBalance.breathMultiplier.get() + (Double)MiniatureBalance.breathGrowth.get() * growth)
         );
      }
   }

   private static ParticleOptions element(DragonBaseEntity dragon) {
      return dragon instanceof LightningDragonEntity
         ? ParticleTypes.ELECTRIC_SPARK
         : (dragon instanceof IceDragonEntity ? ParticleTypes.SNOWFLAKE : ParticleTypes.FLAME);
   }

   static boolean clear(DragonBaseEntity dragon, Vec3 from, Vec3 to) {
      return dragon.level().clip(new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, dragon)).getType() == Type.MISS;
   }

   private static LivingEntity firstEnemy(DragonBaseEntity dragon, Vec3 from, Vec3 to) {
      LivingEntity first = null;
      double nearest = Double.POSITIVE_INFINITY;

      for (LivingEntity living : dragon.level()
         .getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(0.2), other -> HumanoidTargetGoal.isEnemy(dragon, other))) {
         AABB bounds = living.getBoundingBox();
         Vec3 point = bounds.contains(from) ? from : (Vec3)bounds.clip(from, to).orElse(null);
         if (point != null && from.distanceToSqr(point) < nearest) {
            nearest = from.distanceToSqr(point);
            first = living;
         }
      }

      return first;
   }

   private static final class Aim {
      UUID target;
      long shotAt;
      long readyAt;
      long pounceAt;
      boolean followUp;

      void cancel() {
         this.target = null;
         this.followUp = false;
      }
   }
}
