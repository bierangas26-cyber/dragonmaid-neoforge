package com.example.dragonmaid.combat;

import com.example.dragonmaid.accessory.DragonAccessories;
import com.example.dragonmaid.data.DragonForms;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.DragonChargeEntity;
import com.iafenvoy.iceandfire.entity.FireDragonChargeEntity;
import com.iafenvoy.iceandfire.entity.IceDragonChargeEntity;
import com.iafenvoy.iceandfire.entity.LightningDragonChargeEntity;
import com.iafenvoy.iceandfire.entity.IceDragonEntity;
import com.iafenvoy.iceandfire.entity.LightningDragonEntity;
import com.iafenvoy.iceandfire.registry.IafEntities;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.joml.Vector3f;

public final class SafeDragonCharges {
   private static final String SAFE = "DragonMaidSafeCharge";
   private static final String AGE = "DragonMaidChargeAge";
   public static final int MAX_AGE_TICKS = 160;

   private SafeDragonCharges() {
   }

   public static boolean isSafe(DragonChargeEntity charge) {
      return charge.getPersistentData().getBoolean("DragonMaidSafeCharge");
   }

   public static DragonChargeEntity launch(DragonBaseEntity dragon, LivingEntity target) {
      return HumanoidTargetGoal.isEnemy(dragon, target) && dragon.hasLineOfSight(target) ? launch(dragon, target.getBoundingBox().getCenter()) : null;
   }

   static DragonChargeEntity launch(DragonBaseEntity dragon, Vec3 aim) {
      if (dragon.level() instanceof ServerLevel level && DragonForms.isHumanoid(dragon) && dragon.getDragonStage() >= 5) {
         Vec3 origin = dragon.getEyePosition();
         Vec3 direction = aim.subtract(origin).normalize();
         if (direction.lengthSqr() < 0.5) {
            return null;
         }

         DragonChargeEntity charge = (DragonChargeEntity)(dragon instanceof IceDragonEntity
            ? new IceDragonChargeEntity(
               (EntityType)IafEntities.ICE_DRAGON_CHARGE.get(), level, dragon, direction.x, direction.y, direction.z
            )
            : (
               dragon instanceof LightningDragonEntity
                  ? new LightningDragonChargeEntity(
                     (EntityType)IafEntities.LIGHTNING_DRAGON_CHARGE.get(), level, dragon, direction.x, direction.y, direction.z
                  )
                  : new FireDragonChargeEntity(
                     (EntityType)IafEntities.FIRE_DRAGON_CHARGE.get(), level, dragon, direction.x, direction.y, direction.z
                  )
            ));
         charge.setPos(origin);
         charge.setDeltaMovement(direction.scale(0.65));
         charge.getPersistentData().putBoolean("DragonMaidSafeCharge", true);
         charge.getPersistentData().putInt("DragonMaidChargeStage", dragon.getDragonStage());
         charge.getPersistentData().putFloat("DragonMaidChargeBreathMultiplier", DragonAccessories.breathDamageMultiplier(dragon));
         if (!level.addFreshEntity(charge)) {
            return null;
         }

         BreathAnimationSync.shot(dragon, 2);
         return charge;
      }

      return null;
   }

   public static void age(DragonChargeEntity charge) {
      if (!charge.level().isClientSide && isSafe(charge)) {
         int age = charge.getPersistentData().getInt("DragonMaidChargeAge") + 1;
         charge.getPersistentData().putInt("DragonMaidChargeAge", age);
         if (age >= 160) {
            charge.discard();
         }
      }
   }

   public static void impact(DragonChargeEntity charge, HitResult hit) {
      if (charge.level() instanceof ServerLevel level && hit.getType() != Type.MISS) {
         Vec3 center = hit.getLocation().subtract(charge.getDeltaMovement().normalize().scale(0.05));
         if (charge.getOwner() instanceof DragonBaseEntity dragon && dragon.isAlive() && !dragon.isModelDead()) {
            int stage = charge.getPersistentData().getInt("DragonMaidChargeStage");
            CompoundTag data = charge.getPersistentData();
            float bonus = data.contains("DragonMaidChargeBreathMultiplier")
               ? data.getFloat("DragonMaidChargeBreathMultiplier")
               : DragonAccessories.breathDamageMultiplier(dragon);

            for (LivingEntity enemy : level.getEntitiesOfClass(
               LivingEntity.class, new AABB(center, center).inflate(3.0), entity -> HumanoidTargetGoal.isEnemy(dragon, entity)
            )) {
               if (!(enemy.getBoundingBox().getCenter().distanceToSqr(center) > 9.0)
                  && level.clip(new ClipContext(center, enemy.getBoundingBox().getCenter(), Block.COLLIDER, Fluid.NONE, charge)).getType() == Type.MISS) {
                  HumanoidCombat.damage(dragon, enemy, stage, charge.getDamage() * (float)stage * bonus);
               }
            }
         }

         impactParticles(level, charge, center);
         level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.0F, 0.95F);
         charge.discard();
         return;
      }
   }

   private static void impactParticles(ServerLevel level, DragonChargeEntity charge, Vec3 center) {
      boolean ice = charge instanceof IceDragonChargeEntity;
      boolean lightning = charge instanceof LightningDragonChargeEntity;
      ParticleOptions primary = ice ? ParticleTypes.SNOWFLAKE : (lightning ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.FLAME);
      ParticleOptions accent = (ParticleOptions)(ice
         ? new DustParticleOptions(new Vector3f(0.45F, 0.8F, 1.0F), 1.8F)
         : (lightning ? new DustParticleOptions(new Vector3f(0.7F, 0.4F, 1.0F), 1.8F) : ParticleTypes.LARGE_SMOKE));
      level.sendParticles(primary, center.x, center.y, center.z, 48, 1.1, 1.1, 1.1, 0.2);
      level.sendParticles(accent, center.x, center.y, center.z, 24, 1.4, 1.4, 1.4, 0.06);

      for (int i = 0; i < 48; i++) {
         double y = 1.0 - 2.0 * ((double)i + 0.5) / 48.0;
         double radius = Math.sqrt(1.0 - y * y);
         double angle = (double)i * 2.399963229728653;
         double x = Math.cos(angle) * radius;
         double z = Math.sin(angle) * radius;
         level.sendParticles(primary, center.x + x * 1.5, center.y + y * 1.5, center.z + z * 1.5, 0, x * 0.4, y * 0.4, z * 0.4, 1.0);
      }

      for (int i = 0; i < 40; i++) {
         double angle = (double)i * Math.PI * 2.0 / 40.0;
         level.sendParticles(
            primary,
            center.x + Math.cos(angle) * 2.6,
            center.y,
            center.z + Math.sin(angle) * 2.6,
            0,
            Math.cos(angle) * 0.18,
            0.03,
            Math.sin(angle) * 0.18,
            1.0
         );
      }

      if (lightning) {
         level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 16, 0.7, 0.7, 0.7, 0.25);
      }
   }
}
