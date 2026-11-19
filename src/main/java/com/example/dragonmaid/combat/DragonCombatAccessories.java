package com.example.dragonmaid.combat;

import com.example.dragonmaid.accessory.DragonAccessories;
import com.example.dragonmaid.accessory.DragonAccessoryBehavior;
import com.example.dragonmaid.accessory.DragonHands;
import com.example.dragonmaid.compat.CompanionRelations;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import iafcompat.EntityDataCompat;
import com.iafenvoy.iceandfire.entity.FireDragonEntity;
import com.iafenvoy.iceandfire.entity.IceDragonEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class DragonCombatAccessories {
   private static final float FANG_LIFE_STEAL = 0.2F;
   private static final ThreadLocal<DragonCombatAccessories.MeleeAttempt> MELEE = new ThreadLocal<>();

   private DragonCombatAccessories() {
   }

   public static boolean wild(DragonBaseEntity dragon) {
      return DragonAccessories.hasEffect(dragon, DragonAccessoryBehavior.WILD_RHYTHM);
   }

   public static float meleeMultiplier(DragonBaseEntity dragon) {
      if (!DragonAccessories.hasEffect(dragon, DragonAccessoryBehavior.MELEE_DAMAGE)) {
         return 1.0F;
      } else {
         return DragonAccessories.hasEffect(dragon, DragonAccessoryBehavior.CLAW_SWEEP) ? 1.15F : 1.1F;
      }
   }

   public static float tailMultiplier(DragonBaseEntity dragon) {
      return dragon instanceof IceDragonEntity && DragonAccessories.hasEffect(dragon, DragonAccessoryBehavior.ICE_TAIL) ? 1.4F : 1.0F;
   }

   public static double clawRadius(DragonBaseEntity dragon, double base) {
      return DragonAccessories.hasEffect(dragon, DragonAccessoryBehavior.CLAW_SWEEP) ? base * 1.2 : base;
   }

   public static void onActualHealthLoss(LivingEntity victim, DamageSource source, float previous) {
      DragonCombatAccessories.MeleeAttempt attempt = MELEE.get();
      if (attempt != null && attempt.lifeStealRate != 0.0F && attempt.victim == victim && attempt.source == source) {
         float loss = Math.max(0.0F, previous - Math.max(0.0F, victim.getHealth()));
         DragonBaseEntity dragon = attempt.dragon;
         if (loss > 0.0F && dragon.isAlive() && !dragon.isModelDead() && !dragon.isRemoved()) {
            dragon.heal(loss * attempt.lifeStealRate);
         }
      }
   }

   private static float lifeStealRate(boolean fang, boolean primaryHit, boolean armed) {
      if (!fang) {
         return 0.0F;
      } else if (primaryHit) {
         return 0.2F;
      } else {
         return armed ? 0.0F : 0.1F;
      }
   }

   public static boolean melee(DragonBaseEntity dragon, Entity victim, DamageSource source, float base, boolean tail) {
      return melee(dragon, victim, source, base, tail, false, true);
   }

   static boolean melee(DragonBaseEntity dragon, Entity victim, DamageSource source, float base, boolean tail, boolean fireAxe, boolean primaryHit) {
      if (victim instanceof LivingEntity living && !dragon.level().isClientSide) {
         boolean own = dragon.isTame() && !CompanionRelations.isAlly(dragon, living);
         boolean fang = own && dragon instanceof FireDragonEntity && DragonAccessories.hasEffect(dragon, DragonAccessoryBehavior.FIRE_FANG);
         float amount = own ? base * meleeMultiplier(dragon) * (tail ? tailMultiplier(dragon) : 1.0F) : base;
         DragonCombatAccessories.MeleeAttempt previous = MELEE.get();
         boolean armedSecondary = fang && !primaryHit && !BladeCompat.availableMainHand(dragon).isEmpty();
         MELEE.set(new DragonCombatAccessories.MeleeAttempt(dragon, living, source, lifeStealRate(fang, primaryHit, armedSecondary), own && fireAxe));

         boolean success;
         try {
            if (tail) {
               success = TailShieldBreak.hurt(dragon, living, source, () -> victim.hurt(source, amount));
            } else if (own && fireAxe) {
               success = TailShieldBreak.fireAxeHit(dragon, living, source, () -> victim.hurt(source, amount));
            } else {
               success = victim.hurt(source, amount);
            }
         } finally {
            if (previous == null) {
               MELEE.remove();
            } else {
               MELEE.set(previous);
            }
         }

         if (success && own) {
            if (fang) {
               poison(living, dragon);
            }

            if (tail && dragon instanceof IceDragonEntity && DragonAccessories.hasEffect(dragon, DragonAccessoryBehavior.ICE_TAIL)) {
               freezeTail(living);
               living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2), dragon);
               living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 2), dragon);
            }
         }

         return success;
      }

      return victim.hurt(source, base);
   }

   public static boolean humanoidTail(DragonBaseEntity dragon, LivingEntity victim, DamageSource source, float base) {
      return humanoidTail(dragon, victim, source, base, true, false);
   }

   static boolean humanoidTail(DragonBaseEntity dragon, LivingEntity victim, DamageSource source, float base, boolean primaryHit, boolean armedSweep) {
      return humanoidTail(dragon, victim, source, base, primaryHit, armedSweep, armedSweep ? 1.75F : 1.5F);
   }

   static boolean humanoidTail(
      DragonBaseEntity dragon, LivingEntity victim, DamageSource source, float base, boolean primaryHit, boolean armedSweep, float knockbackMultiplier
   ) {
      boolean own = dragon.isTame() && !CompanionRelations.isAlly(dragon, victim);
      boolean fang = own && dragon instanceof FireDragonEntity && DragonAccessories.hasEffect(dragon, DragonAccessoryBehavior.FIRE_FANG);
      DragonCombatAccessories.MeleeAttempt previous = MELEE.get();
      float rate = lifeStealRate(fang, primaryHit, armedSweep);
      MELEE.set(new DragonCombatAccessories.MeleeAttempt(dragon, victim, source, rate, false));

      boolean success;
      try {
         success = TailShieldBreak.hurt(
            dragon,
            victim,
            source,
            () -> TailSweepKnockback.hurt(victim, source, own ? base * meleeMultiplier(dragon) * tailMultiplier(dragon) : base, knockbackMultiplier)
         );
      } finally {
         if (previous == null) {
            MELEE.remove();
         } else {
            MELEE.set(previous);
         }
      }

      if (success && own) {
         if (fang) {
            poison(victim, dragon);
         }

         if (dragon instanceof IceDragonEntity && DragonAccessories.hasEffect(dragon, DragonAccessoryBehavior.ICE_TAIL)) {
            freezeTail(victim);
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2), dragon);
            victim.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 2), dragon);
         }
      }

      return success;
   }

   public static boolean isFireAxeHit(LivingEntity victim, DamageSource source) {
      DragonCombatAccessories.MeleeAttempt attempt = MELEE.get();
      return attempt != null && attempt.fireAxe && attempt.victim == victim && attempt.source == source;
   }

   private static void freezeTail(LivingEntity victim) {
      if (victim.isAlive() && !(victim instanceof IceDragonEntity)) {
         EntityDataCompat.setFrozen(victim, Math.max(60, victim.getTicksFrozen()));
      }
   }

   public static boolean fireFang(DragonBaseEntity dragon) {
      return dragon instanceof FireDragonEntity && DragonAccessories.hasEffect(dragon, DragonAccessoryBehavior.FIRE_FANG);
   }

   public static void poison(LivingEntity target, DragonBaseEntity dragon) {
      if (target.isAlive() && !CompanionRelations.isAlly(dragon, target)) {
         target.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 1), dragon);
      }
   }

   private static record MeleeAttempt(DragonBaseEntity dragon, LivingEntity victim, DamageSource source, float lifeStealRate, boolean fireAxe) {
   }
}