package com.example.dragonmaid.combat;

import com.example.dragonmaid.accessory.MiniatureForm;
import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.data.HumanoidLife;
import com.iafenvoy.uranus.animation.Animation;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.GorgonEntity;
import com.iafenvoy.iceandfire.entity.util.dragon.IafDragonAttacks.Air;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class MiniaturePounce {
   private static final Map<DragonBaseEntity, MiniaturePounce.Flight> STARTED = new WeakHashMap<>();

   private MiniaturePounce() {
   }

   public static boolean isPouncing(DragonBaseEntity dragon) {
      return STARTED.containsKey(dragon) || nativePounce(dragon);
   }

   private static boolean nativePounce(DragonBaseEntity dragon) {
      return MiniatureForm.isMatureMini(dragon)
         && !dragon.isPassenger()
         && !dragon.isVehicle()
         && !dragon.onGround()
         && (dragon.isTackling() || dragon.isFlying() && dragon.airAttack == Air.TACKLE && dragon.getTarget() != null);
   }

   public static void tick(DragonBaseEntity dragon) {
      MiniaturePounce.Flight flight = STARTED.get(dragon);
      if (flight == null) {
         if (!nativePounce(dragon)) {
            return;
         }

         flight = new MiniaturePounce.Flight(dragon);
         STARTED.put(dragon, flight);
      }

      if (MiniatureForm.isMatureMini(dragon)
         && !dragon.isNoAi()
         && !dragon.isPassenger()
         && !dragon.isVehicle()
         && !dragon.isSleeping()
         && dragon.getCommand() != 1
         && !HumanoidLife.isInjured(dragon)
         && !GorgonEntity.isStoneMob(dragon)
         && !DragonForms.state(dragon).dragonmaid$isRecoveryRest()
         && !dragon.isInWater()) {
         dragon.setBreathingFire(false);
         long elapsed = dragon.level().getGameTime() - flight.started;
         if (dragon.onGround() && elapsed > 0L) {
            finish(dragon);
         } else if (!flight.finishing
            && dragon.isFlying()
            && !dragon.usingGroundAttack
            && elapsed < 30L
            && !dragon.horizontalCollision
            && dragon.getTarget() != null
            && HumanoidTargetGoal.isEnemy(dragon, dragon.getTarget())) {
            limitSpeed(dragon);
         } else {
            flight.finishing = true;
            dragon.setFlying(false);
            dragon.setHovering(false);
            dragon.setTackling(false);
            dragon.usingGroundAttack = true;
            Vec3 motion = dragon.getDeltaMovement();
            dragon.setDeltaMovement(motion.x * 0.4, Math.min(0.0, motion.y), motion.z * 0.4);
         }
      } else {
         finish(dragon);
      }
   }

   public static void limitSpeed(DragonBaseEntity dragon) {
      if (isPouncing(dragon)) {
         Vec3 motion = dragon.getDeltaMovement();
         double cap = Math.min((Double)MiniatureBalance.pounceSpeed.get(), MiniatureForm.movementLimit(dragon) * 3.0);
         if (motion.lengthSqr() > cap * cap) {
            dragon.setDeltaMovement(motion.normalize().scale(cap));
         }
      }
   }

   public static boolean tryStart(DragonBaseEntity dragon, LivingEntity target) {
      if (!isPouncing(dragon)
         && MiniatureForm.isMatureMini(dragon)
         && dragon.canMove()
         && !dragon.isNoAi()
         && dragon.onGround()
         && !dragon.isFlying()
         && !dragon.isHovering()
         && !dragon.isInWater()
         && !dragon.isPassenger()
         && !dragon.isVehicle()
         && dragon.getCommand() != 1
         && !dragon.isSleeping()
         && !HumanoidLife.isInjured(dragon)
         && !DragonForms.state(dragon).dragonmaid$isRecoveryRest()
         && !GorgonEntity.isStoneMob(dragon)
         && HumanoidTargetGoal.isEnemy(dragon, target)
         && dragon.hasLineOfSight(target)) {
         Animation animation = dragon.getAnimation();
         if (animation == DragonBaseEntity.NO_ANIMATION || animation == DragonBaseEntity.ANIMATION_BITE && dragon.getAnimationTick() < 4) {
            double distance = dragon.position().distanceToSqr(target.position());
            if (!MiniatureMelee.inReach(dragon, target)
               && !(distance > Math.pow((Double)MiniatureBalance.pounceRange.get(), 2.0))
               && !(Math.abs(target.getY() - dragon.getY()) > 2.0)
               && dragon.level().noCollision(dragon, dragon.getBoundingBox().move(0.0, 0.5, 0.0))) {
               STARTED.put(dragon, new MiniaturePounce.Flight(dragon));
               dragon.getNavigation().stop();
               dragon.setAnimation(DragonBaseEntity.NO_ANIMATION);
               dragon.setBreathingFire(false);
               dragon.airAttack = Air.TACKLE;
               dragon.usingGroundAttack = false;
               dragon.flyTicks = 0;
               dragon.tacklingTicks = 0;
               dragon.setHovering(false);
               dragon.setFlying(true);
               dragon.setTackling(true);
               dragon.setOnGround(false);
               dragon.flightManager.setFlightTarget(target.getEyePosition());
               return true;
            } else {
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static void finish(DragonBaseEntity dragon) {
      STARTED.remove(dragon);
      dragon.setTackling(false);
      dragon.setFlying(false);
      dragon.setHovering(false);
      dragon.usingGroundAttack = true;
   }

   private static final class Flight {
      final long started;
      boolean finishing;

      Flight(DragonBaseEntity dragon) {
         this.started = dragon.level().getGameTime();
      }
   }
}
