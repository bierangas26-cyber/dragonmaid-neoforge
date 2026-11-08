package com.example.dragonmaid.combat;

import com.example.dragonmaid.DragonMaidConfig;
import com.example.dragonmaid.accessory.MiniatureForm;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;

public final class MiniatureEscortGoal extends Goal {
   private final DragonBaseEntity dragon;
   private final GuardEscort escort;
   private boolean returning;
   private int nextPathTick;

   public static double combatRadius() {
      return (Double)MiniatureBalance.guardRadius.get();
   }

   public MiniatureEscortGoal(DragonBaseEntity dragon) {
      this.dragon = dragon;
      this.escort = new GuardEscort(dragon);
      this.setFlags(EnumSet.of(Flag.MOVE));
   }

   private LivingEntity owner() {
      return MiniatureForm.hasDragonEffect(this.dragon) && WildDragonCombat.canAcquireTargets(this.dragon) && this.dragon.getControllingPassenger() == null
         ? GuardEscort.owner(this.dragon)
         : null;
   }

   private boolean needsReturn() {
      LivingEntity owner = this.owner();
      if (owner == null) {
         return this.returning = false;
      } else {
         double radius = combatRadius();
         double distance = this.dragon.distanceToSqr(owner);
         if (distance > radius * radius) {
            this.returning = true;
         }

         boolean airborne = this.dragon.isFlying() || this.dragon.isHovering() || !this.dragon.onGround() && !this.dragon.isInWaterOrBubble();
         double close = airborne ? radius * 0.8333333333333334 : Math.min(4.0, radius * 0.5);
         if (distance <= close * close) {
            this.returning = false;
         }

         return this.returning;
      }
   }

   public boolean rejects(LivingEntity target) {
      LivingEntity owner = this.owner();
      if (owner != null && target != null) {
         double radius = combatRadius();
         return this.needsReturn() || WildDragonCombat.distanceToSqr(this.dragon, owner, target) > radius * radius;
      } else {
         return false;
      }
   }

   public void checkTarget() {
      if (this.dragon.getTarget() != null && this.rejects(this.dragon.getTarget())) {
         this.dragon.setTarget(null);
      }
   }

   public boolean canUse() {
      return this.needsReturn();
   }

   public boolean canContinueToUse() {
      return this.needsReturn();
   }

   public boolean requiresUpdateEveryTick() {
      return true;
   }

   public void start() {
      this.nextPathTick = 0;
      this.dragon.getNavigation().stop();
   }

   public void tick() {
      LivingEntity owner = this.owner();
      if (owner != null) {
         this.dragon.setTarget(null);
         this.dragon.setBreathingFire(false);
         double teleportDistance = Math.max((Double)DragonMaidConfig.GUARD_TELEPORT_DISTANCE.get(), combatRadius() + 8.0);
         if (this.dragon.distanceToSqr(owner) > teleportDistance * teleportDistance) {
            this.escort.tryTeleport(owner);
         }

         if (this.dragon.tickCount >= this.nextPathTick) {
            this.nextPathTick = this.dragon.tickCount + 10;
            if (!this.dragon.isFlying() && !this.dragon.isHovering()) {
               this.dragon.getNavigation().moveTo(owner, 1.2);
            } else {
               this.dragon.flightManager.setFlightTarget(owner.getEyePosition());
            }
         }
      }
   }

   public void stop() {
      this.returning = false;
      this.escort.reset();
   }
}
