package com.example.dragonmaid.combat;

import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.data.HumanoidWater;
import com.iafenvoy.uranus.animation.IAnimatedEntity;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.FireDragonEntity;
import com.iafenvoy.iceandfire.entity.HydraEntity;
import com.iafenvoy.iceandfire.entity.IceDragonEntity;
import com.iafenvoy.iceandfire.entity.util.dragon.IafDragonAttacks.Air;
import com.iafenvoy.iceandfire.entity.util.dragon.IafDragonAttacks.Ground;
import com.iafenvoy.iceandfire.registry.IafDamageTypes;
import com.iafenvoy.iceandfire.registry.IafSounds;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.Holder;

@EventBusSubscriber(
   modid = "dragonmaid"
)
public final class KnowledgeSupport {
   private static final Map<DragonBaseEntity, Long> LAST_BREATH = new WeakHashMap<>();
   private static DoubleValue rescueRange;
   private static DoubleValue protectionRange;
   private static IntValue breathCooldown;
   private static IntValue hydraFireSeconds;
   private static IntValue protectionTicks;

   private KnowledgeSupport() {
   }

   public static void configure(Builder builder) {
      builder.push("knowledgeSupport");
      rescueRange = builder.comment("Maximum distance to extinguish a burning owner, with a clear breath path.")
         .translation("config.dragonmaid.knowledgeSupport.rescueRange")
         .defineInRange("rescueRange", 12.0, 1.0, 48.0);
      protectionRange = builder.comment("Maximum distance for level-three elemental protection.")
         .translation("config.dragonmaid.knowledgeSupport.protectionRange")
         .defineInRange("protectionRange", 16.0, 1.0, 48.0);
      breathCooldown = builder.comment("Minimum ticks between ice breaths used to extinguish the owner.")
         .translation("config.dragonmaid.knowledgeSupport.breathCooldownTicks")
         .defineInRange("breathCooldownTicks", 40, 5, 400);
      hydraFireSeconds = builder.comment(
            "Burn duration when knowledge II+ fire dragon breath damages a hydra. Normal combat range and cooldown; no extra hunger cost."
         )
         .translation("config.dragonmaid.knowledgeSupport.hydraFireSeconds")
         .defineInRange("hydraFireSeconds", 20, 1, 600);
      protectionTicks = builder.comment("Duration of a level-three protection effect after it is granted.")
         .translation("config.dragonmaid.knowledgeSupport.protectionTicks")
         .defineInRange("protectionTicks", 60, 21, 600);
      builder.pop();
   }

   @SubscribeEvent
   public static void tick(EntityTickEvent.Post event) {
      if (event.getEntity() instanceof DragonBaseEntity dragon && !dragon.level().isClientSide && Math.floorMod(dragon.tickCount + dragon.getId(), 5) == 0) {
         assist(dragon);
      }
   }

   public static void assist(DragonBaseEntity dragon) {
      boolean ice = dragon instanceof IceDragonEntity;
      if ((ice || dragon instanceof FireDragonEntity)
         && dragon.level() instanceof ServerLevel level
         && dragon.isTame()
         && dragon.getOwnerUUID() != null
         && dragon.isAlive()
         && !dragon.isModelDead()
         && !dragon.isRemoved()
         && !dragon.isNoAi()
         && !dragon.isSleeping()) {
         int rank = DragonForms.state(dragon).dragonmaid$getKnowledgeLevel();
         if (rank >= 2) {
            if ((ice || rank >= 3)
               && dragon.getOwner() instanceof Player owner
               && owner.isAlive()
               && !owner.isSpectator()
               && !owner.isRemoved()
               && owner.level() == level) {
               double distance = dragon.distanceToSqr(owner);
               if (rank >= 3 && within(distance, protectionRange)) {
                  if (ice && owner.isEyeInFluid(FluidTags.WATER)) {
                     protect(owner, MobEffects.WATER_BREATHING);
                  } else if (!ice && (level.dimension().equals(Level.NETHER) || owner.isOnFire())) {
                     protect(owner, MobEffects.FIRE_RESISTANCE);
                  }
               }

               if (ice && owner.isOnFire() && within(distance, rescueRange) && readyToBreathe(dragon, level) && sendExtinguishingBreath(level, dragon, owner)) {
                  owner.clearFire();
               }
            }

            if (!DragonForms.isHumanoid(dragon)
               && needsHydraFire(dragon, dragon.getTarget())
               && !dragon.isPassenger()
               && dragon.burningTarget == null
               && dragon.getAnimation() == IAnimatedEntity.NO_ANIMATION) {
               dragon.groundAttack = Ground.FIRE;
               if (dragon.onGround()) {
                  dragon.usingGroundAttack = true;
               } else if (dragon.isFlying() || dragon.isHovering()) {
                  dragon.airAttack = Air.SCORCH_STREAM;
               }
            }
         }
      }
   }

   public static boolean needsHydraFire(DragonBaseEntity dragon, LivingEntity target) {
      if (dragon instanceof FireDragonEntity
         && dragon.isTame()
         && dragon.getOwnerUUID() != null
         && DragonForms.state(dragon).dragonmaid$getKnowledgeLevel() >= 2
         && dragon.getCommand() != 1
         && !dragon.isOrderedToSit()
         && target instanceof HydraEntity hydra
         && hydra.isAlive()
         && !hydra.isRemoved()
         && hydra.level() == dragon.level()
         && !hydra.isOnFire()
         && !hydra.fireImmune()
         && !hydra.isInWaterOrBubble()
         && HumanoidTargetGoal.isEnemy(dragon, hydra)) {
         return true;
      }

      return false;
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void fireDamage(LivingDamageEvent.Pre event) {
      if (!(event.getNewDamage() <= 0.0F)
         && !event.getEntity().level().isClientSide
         && event.getEntity() instanceof HydraEntity hydra
         && event.getSource().is(IafDamageTypes.DRAGON_FIRE_TYPE)) {
         Entity attacker = event.getSource().getEntity();
         if (attacker == null) {
            attacker = event.getSource().getDirectEntity();
         }

         if (attacker instanceof Projectile projectile) {
            attacker = projectile.getOwner();
         }

         if (attacker instanceof FireDragonEntity dragon
            && dragon.isTame()
            && dragon.getOwnerUUID() != null
            && DragonForms.state(dragon).dragonmaid$getKnowledgeLevel() >= 2
            && !hydra.fireImmune()
            && !hydra.isInWaterOrBubble()) {
            hydra.igniteForSeconds(((Integer)hydraFireSeconds.get()).floatValue());
         }
      }
   }

   private static boolean within(double squaredDistance, DoubleValue setting) {
      double range = (Double)setting.get();
      return squaredDistance <= range * range;
   }

   private static boolean readyToBreathe(DragonBaseEntity dragon, ServerLevel level) {
      if (dragon.burningTarget == null && !dragon.isVehicle() && (!dragon.isPassenger() || dragon.getVehicle() == dragon.getOwner())) {
         Long last = LAST_BREATH.get(dragon);
         return last == null || level.getGameTime() < last || level.getGameTime() - last >= (long)((Integer)breathCooldown.get()).intValue();
      } else {
         return false;
      }
   }

   private static void protect(Player owner, Holder<MobEffect> effect) {
      MobEffectInstance existing = owner.getEffect(effect);
      if (existing == null || existing.getAmplifier() <= 0 && !existing.isInfiniteDuration() && existing.getDuration() <= 20) {
         owner.addEffect(new MobEffectInstance(effect, (Integer)protectionTicks.get(), 0, true, false, true));
      }
   }

   private static boolean sendExtinguishingBreath(ServerLevel level, DragonBaseEntity dragon, LivingEntity recipient) {
      if (!dragon.hasLineOfSight(recipient)) {
         return false;
      } else {
         Vec3 start = DragonForms.isHumanoid(dragon) ? HumanoidWater.breathOrigin(dragon) : dragon.getHeadPosition();
         Vec3 end = recipient.getBoundingBox().getCenter();
         if (level.clip(new ClipContext(start, end, Block.COLLIDER, Fluid.NONE, dragon)).getType() != Type.MISS) {
            return false;
         } else {
            LAST_BREATH.put(dragon, level.getGameTime());
            dragon.getLookControl().setLookAt(recipient, 40.0F, 35.0F);
            int steps = Math.max(2, Math.min(12, (int)Math.ceil(start.distanceTo(end))));

            for (int i = 1; i <= steps; i++) {
               Vec3 p = start.lerp(end, (double)i / (double)steps);
               level.sendParticles(ParticleTypes.SNOWFLAKE, p.x, p.y, p.z, 2, 0.075, 0.075, 0.075, 0.01);
            }

            if (!dragon.isSilent()) {
               level.playSound(null, start.x, start.y, start.z, IafSounds.ICEDRAGON_BREATH.get(), dragon.getSoundSource(), 0.8F, 1.2F);
            }

            return true;
         }
      }
   }
}
