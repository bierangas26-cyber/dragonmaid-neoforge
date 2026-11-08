package com.example.dragonmaid.combat;

import com.example.dragonmaid.DragonMaidConfig;
import com.example.dragonmaid.accessory.DragonAccessories;
import com.example.dragonmaid.accessory.DragonAccessoryBehavior;
import com.example.dragonmaid.compat.CompanionRelations;
import com.example.dragonmaid.data.DragonForms;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.GorgonEntity;
import com.iafenvoy.iceandfire.registry.IafItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;

public final class HumanoidRoar {
   private static final String NEXT_ROAR = "DragonMaidNextRoar";

   private HumanoidRoar() {
   }

   public static boolean isRoaring(DragonBaseEntity dragon) {
      return (dragon.getAnimation() == DragonBaseEntity.ANIMATION_ROAR || dragon.getAnimation() == DragonBaseEntity.ANIMATION_EPIC_ROAR)
         && dragon.getAnimationTick() < 20;
   }

   public static void tryRoar(DragonBaseEntity dragon) {
      if (dragon.level() instanceof ServerLevel level
         && DragonForms.isHumanoid(dragon)
         && !TailSweepSync.isBusy(dragon)
         && dragon.isTame()
         && dragon.isAlive()
         && !dragon.isModelDead()
         && dragon.getDragonStage() >= 4
         && !dragon.isOrderedToSit()
         && !dragon.isSleeping()
         && !dragon.isPassenger()
         && !dragon.isNoAi()
         && !GorgonEntity.isStoneMob(dragon)) {
         long now = level.getGameTime();
         if (dragon.getPersistentData().getLong("DragonMaidNextRoar") > now) {
            return;
         }

         LivingEntity target = dragon.getTarget();
         double radius = dragon.getDragonStage() >= 5 ? 16.0 : 12.0;
         if (target != null && HumanoidTargetGoal.isEnemy(dragon, target) && !(dragon.distanceToSqr(target) > radius * radius) && dragon.hasLineOfSight(target)) {
            dragon.getPersistentData().putLong("DragonMaidNextRoar", now + (long)((Integer)DragonMaidConfig.ROAR_COOLDOWN.get()).intValue());
            dragon.setAnimation(dragon.getDragonStage() >= 5 ? DragonBaseEntity.ANIMATION_EPIC_ROAR : DragonBaseEntity.ANIMATION_ROAR);
            dragon.setAnimationTick(0);
            dragon.playSound(dragon.getRoarSound(), 1.5F, dragon.getDragonStage() >= 5 ? 0.85F : 1.0F);
            int duration = dragon.getDragonStage() >= 5 ? 300 : 200;
            boolean enhanced = DragonAccessories.hasEffect(dragon, DragonAccessoryBehavior.ROAR_MAJESTY);
            if (enhanced) {
               duration = Math.round((float)duration * 1.25F);
            }

            for (LivingEntity living : level.getEntitiesOfClass(
               LivingEntity.class,
               dragon.getBoundingBox().inflate(radius),
               e -> e != dragon && e.isAlive() && dragon.distanceToSqr(e) <= radius * radius && dragon.hasLineOfSight(e)
            )) {
               if (isAlly(dragon, living)) {
                  living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, enhanced ? 1 : 0));
               } else if (HumanoidTargetGoal.isEnemy(dragon, living)) {
                  if (living instanceof DragonBaseEntity) {
                     DragonBaseEntity other = (DragonBaseEntity)living;
                     if (other.getDragonStage() >= dragon.getDragonStage()) {
                        continue;
                     }
                  }

                  if (!living.getItemBySlot(EquipmentSlot.HEAD).is((Item)IafItems.EARPLUGS.get())) {
                     living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, enhanced ? 1 : 0));
                  }
               }
            }

            Vec3 mouth = dragon.getEyePosition();
            level.sendParticles(ParticleTypes.POOF, mouth.x, mouth.y, mouth.z, 16, 0.35, 0.2, 0.35, 0.12);
            return;
         }

         return;
      }
   }

   private static boolean isAlly(DragonBaseEntity dragon, LivingEntity living) {
      return CompanionRelations.isAlly(dragon, living);
   }
}
