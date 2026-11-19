package com.example.dragonmaid.combat;

import com.example.dragonmaid.accessory.DragonAccessories;
import com.example.dragonmaid.accessory.DragonHands;
import com.example.dragonmaid.data.HumanoidWater;
import com.example.dragonmaid.item.CompanionBladeItem;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import iafcompat.EntityDataCompat;
import com.iafenvoy.iceandfire.entity.FireDragonEntity;
import com.iafenvoy.iceandfire.entity.IceDragonEntity;
import com.iafenvoy.iceandfire.entity.LightningDragonEntity;
import com.iafenvoy.iceandfire.registry.IafDamageTypes;
import com.iafenvoy.iceandfire.registry.IafSounds;
import com.iafenvoy.iceandfire.registry.IafDamageTypes.CustomEntityDamageSource;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class HumanoidCombat {
   public static final double PRIMARY_SAFETY_REACH = 5.0;
   private static final double WEAPON_REACH_BONUS = 1.0;
   private static final Map<DragonBaseEntity, Integer> FORGE_SOUND_TICKS = new WeakHashMap<>();

   private HumanoidCombat() {
   }

   public static void stopBreath(DragonBaseEntity dragon) {
      dragon.setBreathingFire(false);
      if (dragon instanceof LightningDragonEntity lightning) {
         lightning.setHasLightningTarget(false);
      }
   }

   public static double clawReach(DragonBaseEntity dragon) {
      return naturalClawReach(dragon) + (BladeCompat.availableMainHand(dragon).isEmpty() ? 0.0 : 1.0) + SpecialWeapons.longswordExtraReach(dragon);
   }

   public static double naturalClawReach(DragonBaseEntity dragon) {
      return switch (dragon.getDragonStage()) {
         case 4 -> 2.3;
         case 5 -> 2.5;
         default -> 2.2;
      };
   }

   public static void claw(DragonBaseEntity dragon, double reach) {
      claw(dragon, reach, dragon.getTarget(), DragonCombatAccessories.clawRadius(dragon, reach));
   }

   public static void claw(DragonBaseEntity dragon, double reach, LivingEntity primary, double primaryReach) {
      if (!TailSweepSync.isBusy(dragon) && dragon.level() instanceof ServerLevel level) {
         reach = DragonCombatAccessories.clawRadius(dragon, reach);
         ItemStack var18 = BladeCompat.availableMainHand(dragon);
         double unarmed = dragon.getAttributeValue(Attributes.ATTACK_DAMAGE);
         double armed = armedDamage(unarmed, var18);
         LivingEntity struck = null;
         Vec3 forward = primary != null && primary.level() == level ? primary.position().subtract(dragon.position()).normalize() : dragon.getLookAngle();
         if (primary != null
            && HumanoidTargetGoal.isEnemy(dragon, primary)
            && GuardEscort.reachableTarget(dragon, primary)
            && dragon.distanceToSqr(primary) <= primaryReach * primaryReach
            && dragon.hasLineOfSight(primary)
            && clawHit(dragon, primary, var18, unarmed, armed, true, 1.0)) {
            struck = primary;
         }

         for (LivingEntity enemy : level.getEntitiesOfClass(
            LivingEntity.class, dragon.getBoundingBox().inflate(reach), e -> e != primary && HumanoidTargetGoal.isEnemy(dragon, e)
         )) {
            Vec3 delta = enemy.position().subtract(dragon.position());
            if (!(delta.lengthSqr() > reach * reach)
               && !(delta.normalize().dot(forward) < 0.0)
               && GuardEscort.reachableTarget(dragon, enemy)
               && dragon.hasLineOfSight(enemy)
               && clawHit(dragon, enemy, var18, unarmed, armed, false, 1.0)
               && struck == null) {
               struck = enemy;
            }
         }

         if (struck != null) {
            applyWeaponHit(dragon, var18, struck);
         }

         if (SpecialWeapons.isLongsword(var18)) {
            BladeCombat.longswordSweepVisual(dragon, forward, reach);
         } else {
            level.sendParticles(
               ParticleTypes.SWEEP_ATTACK,
               dragon.getX() + forward.x,
               dragon.getY() + (double)dragon.getBbHeight() * 0.55,
               dragon.getZ() + forward.z,
               1,
               0.0,
               0.0,
               0.0,
               0.0
            );
         }
      }
   }

   static void applyWeaponHit(DragonBaseEntity dragon, ItemStack weapon, LivingEntity victim) {
      DragonHands hands = DragonHands.inventory(dragon);
      if (!weapon.isEmpty() && hands.getItem(0) == weapon) {
         int before = weapon.getDamageValue();
         boolean handled = weapon.getItem().hurtEnemy(weapon, victim, dragon);
         if (!handled && weapon.isDamageableItem() && weapon.getDamageValue() == before) {
            weapon.hurtAndBreak(1, dragon, EquipmentSlot.MAINHAND);
         }

         hands.setChanged();
      }
   }

   private static boolean clawHit(
      DragonBaseEntity dragon, LivingEntity enemy, ItemStack weapon, double unarmed, double armed, boolean primaryHit, double multiplier
   ) {
      double total = enchantedDamage(enemy, weapon, unarmed, armed);
      boolean fireAxe = SpecialWeapons.fireAxe(dragon, weapon);
      if (fireAxe) {
         total *= SpecialWeapons.fireAxeDamageMultiplier(enemy.getMaxHealth());
      }

      if (SpecialWeapons.isKatana(weapon)) {
         total *= (double)KatanaCharge.lightArmorMultiplier((float)enemy.getArmorValue());
      }

      float damage = (float)Math.min(Float.MAX_VALUE, total * multiplier + (double)CompanionBladeItem.undeadBonus(weapon, enemy));
      if (!DragonCombatAccessories.melee(dragon, enemy, dragon.damageSources().mobAttack(dragon), damage, false, fireAxe, primaryHit)) {
         return false;
      } else {
         if (SpecialWeapons.riptideIceTrident(dragon, weapon)) {
            EntityDataCompat.setFrozen(enemy, 60);
         }

         Vec3 delta = enemy.position().subtract(dragon.position());
         enemy.knockback(0.5 + (double)dragon.getDragonStage() * 0.1, -delta.x, -delta.z);
         return true;
      }
   }

   public static boolean thrownTrident(DragonBaseEntity dragon, LivingEntity enemy, ItemStack weapon, DamageSource source) {
      double base = dragon.getAttributeValue(Attributes.ATTACK_DAMAGE);
      float damage = (float)Math.min(Float.MAX_VALUE, enchantedDamage(enemy, weapon, base, armedDamage(base, weapon)));
      return DragonCombatAccessories.melee(dragon, enemy, source, damage, false);
   }

   private static double enchantedDamage(LivingEntity enemy, ItemStack weapon, double unarmed, double armed) {
      float enchantment = weapon.isEmpty() ? 0.0F : BladeCompat.damageBonus(weapon, enemy);
      return Math.max(unarmed, armed + (double)(Float.isFinite(enchantment) ? enchantment : 0.0F));
   }

   static boolean wingThrust(DragonBaseEntity dragon, LivingEntity enemy, ItemStack weapon) {
      double base = dragon.getAttributeValue(Attributes.ATTACK_DAMAGE);
      if (!clawHit(dragon, enemy, weapon, base, armedDamage(base, weapon), true, 2.0)) {
         return false;
      } else {
         applyWeaponHit(dragon, weapon, enemy);
         double lift = 0.48 * Math.max(0.0, 1.0 - enemy.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
         Vec3 velocity = enemy.getDeltaMovement();
         if (lift > velocity.y) {
            enemy.setDeltaMovement(velocity.x, lift, velocity.z);
            enemy.hurtMarked = true;
         }

         return true;
      }
   }

   static double armedDamage(double base, ItemStack weapon) {
      if (weapon.isEmpty()) {
         return base;
      } else {
         double added = 0.0;
         double multipliedBase = 0.0;
         double multipliedTotal = 1.0;

         for (ItemAttributeModifiers.Entry entry : weapon.getAttributeModifiers().modifiers()) {
            if (!entry.attribute().is(Attributes.ATTACK_DAMAGE)) {
               continue;
            }
            AttributeModifier modifier = entry.modifier();
            double amount = modifier.amount();
            if (Double.isFinite(amount)) {
               switch (modifier.operation()) {
                  case ADD_VALUE:
                     added += amount;
                     break;
                  case ADD_MULTIPLIED_BASE:
                     multipliedBase += amount;
                     break;
                  case ADD_MULTIPLIED_TOTAL:
                     multipliedTotal *= 1.0 + amount;
               }
            }
         }

         double damage = (base + added) * (1.0 + multipliedBase) * multipliedTotal;
         return Double.isFinite(damage) ? Math.max(base, damage) : base;
      }
   }

   public static void breath(DragonBaseEntity dragon, LivingEntity target, boolean damageTick) {
      if (dragon instanceof LightningDragonEntity || BreathProfile.forDragon(dragon).canHit(dragon, target)) {
         breath(dragon, target.getBoundingBox().getCenter(), damageTick);
      }
   }

   static void breath(DragonBaseEntity dragon, Vec3 aim, boolean damageTick) {
      if (!TailSweepSync.isBusy(dragon) && dragon.level() instanceof ServerLevel level) {
         BreathProfile var14 = BreathProfile.forDragon(dragon);
         if (!(dragon instanceof LightningDragonEntity)) {
            if (damageTick) {
               MovingBreaths.emit(dragon, aim, var14);
            }
         } else {
            Vec3 start = dragon.getEyePosition();
            Vec3 direction = aim.subtract(start).normalize();
            Vec3 desiredEnd = start.add(direction.scale(Math.min(var14.range(), start.distanceTo(aim) + 0.5)));
            Vec3 end = level.clip(new ClipContext(start, desiredEnd, Block.COLLIDER, Fluid.NONE, dragon)).getLocation();
            if (dragon instanceof LightningDragonEntity lightning) {
               if (!dragon.isBreathingFire() || dragon.tickCount % 5 == 0) {
                  dragon.playSound(IafSounds.LIGHTNINGDRAGON_BREATH.get(), 2.0F, 1.0F);
               }

               dragon.setBreathingFire(true);
               lightning.setLightningTargetVec((float)end.x, (float)end.y, (float)end.z);
               lightning.setHasLightningTarget(true);
            }

            if (damageTick) {
               double radius = 0.35;

               for (LivingEntity enemy : level.getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(radius), e -> HumanoidTargetGoal.isEnemy(dragon, e))) {
                  AABB hitbox = enemy.getBoundingBox().inflate(radius);
                  if ((hitbox.contains(start) || !hitbox.clip(start, end).isEmpty()) && dragon.hasLineOfSight(enemy)) {
                     damage(dragon, enemy, var14.stage());
                  }
               }
            }
         }
      }
   }

   public static boolean damage(DragonBaseEntity dragon, LivingEntity enemy, int stage) {
      return damage(dragon, enemy, stage, breathDamageAmount(dragon, stage));
   }

   public static float breathDamageAmount(DragonBaseEntity dragon, int stage) {
      return (float)stage * (dragon instanceof LightningDragonEntity ? 1.5F : 3.0F) * DragonAccessories.breathDamageMultiplier(dragon);
   }

   public static boolean damage(DragonBaseEntity dragon, LivingEntity enemy, int stage, float amount) {
      return damage(dragon, enemy, stage, amount, DragonCombatAccessories.fireFang(dragon));
   }

   public static boolean damage(DragonBaseEntity dragon, LivingEntity enemy, int stage, float amount, boolean poison) {
      if (dragon.level() instanceof ServerLevel && HumanoidTargetGoal.isEnemy(dragon, enemy)) {
         CustomEntityDamageSource source = dragon instanceof IceDragonEntity
            ? IafDamageTypes.causeDragonIceDamage(dragon)
            : (dragon instanceof LightningDragonEntity ? IafDamageTypes.causeDragonLightningDamage(dragon) : IafDamageTypes.causeDragonFireDamage(dragon));
         if (!enemy.hurt(source, amount)) {
            return false;
         } else {
            if (dragon instanceof FireDragonEntity) {
               enemy.igniteForSeconds(stage);
               if (poison) {
                  DragonCombatAccessories.poison(enemy, dragon);
               }
            }

            if (dragon instanceof IceDragonEntity) {
               EntityDataCompat.setFrozen(enemy, stage * 40);
               enemy.setTicksFrozen(Math.max(enemy.getTicksFrozen(), enemy.getTicksRequiredToFreeze() + stage * 20));
               enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, stage * 40, 1));
            }

            return true;
         }
      } else {
         return false;
      }
   }

   public static void forgeStream(DragonBaseEntity dragon, Vec3 end) {
      if (dragon.level() instanceof ServerLevel level) {
         int interval = dragon instanceof LightningDragonEntity ? 7 : 5;
         if (dragon.tickCount % interval == 0 && !Integer.valueOf(dragon.tickCount).equals(FORGE_SOUND_TICKS.get(dragon))) {
            FORGE_SOUND_TICKS.put(dragon, dragon.tickCount);
            dragon.playSound(
               dragon instanceof LightningDragonEntity
                  ? IafSounds.LIGHTNINGDRAGON_BREATH.get()
                  : (dragon instanceof IceDragonEntity ? IafSounds.ICEDRAGON_BREATH.get() : IafSounds.FIREDRAGON_BREATH.get()),
               4.0F,
               1.0F
            );
         }

         if (dragon instanceof LightningDragonEntity lightning) {
            dragon.setBreathingFire(true);
            lightning.setLightningTargetVec((float)end.x, (float)end.y, (float)end.z);
            lightning.setHasLightningTarget(true);
         } else {
            if (dragon.tickCount % 3 != 0) {
               return;
            }

            Vec3 start = HumanoidWater.breathOrigin(dragon);
            ParticleOptions particle = dragon instanceof IceDragonEntity ? ParticleTypes.SNOWFLAKE : ParticleTypes.FLAME;
            int steps = Math.max(1, (int)Math.ceil(start.distanceTo(end) * 4.0));

            for (int i = 0; i <= steps; i++) {
               Vec3 pos = start.lerp(end, (double)i / (double)steps);
               level.sendParticles(particle, pos.x, pos.y, pos.z, 1, 0.035, 0.035, 0.035, 0.001);
            }
         }
      }
   }
}