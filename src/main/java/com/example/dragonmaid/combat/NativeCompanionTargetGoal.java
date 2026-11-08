package com.example.dragonmaid.combat;

import com.example.dragonmaid.DragonMaidConfig;
import com.example.dragonmaid.compat.CompanionRelations;
import com.example.dragonmaid.data.DragonForms;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.util.Comparator;
import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;

public final class NativeCompanionTargetGoal extends Goal {
   private final DragonBaseEntity dragon;
   private LivingEntity selected;
   private int nextCompanionScanTick;

   public NativeCompanionTargetGoal(DragonBaseEntity dragon) {
      this.dragon = dragon;
      this.setFlags(EnumSet.of(Flag.TARGET));
   }

   private boolean available() {
      return !DragonForms.isHumanoid(this.dragon)
         && CompanionDefense.canRespond(this.dragon)
         && WildDragonCombat.canAcquireTargets(this.dragon)
         && this.dragon.canMove();
   }

   private boolean eligible(LivingEntity target) {
      return CompanionDefense.canAssist(this.dragon, target);
   }

   public boolean canUse() {
      if (!this.available()) {
         return false;
      } else {
         LivingEntity attacker = CompanionDefense.recentAttacker(this.dragon);
         LivingEntity current = this.dragon.getTarget();
         if (current != null && current != attacker && HumanoidTargetGoal.isEnemy(this.dragon, current)) {
            return false;
         } else if (this.eligible(attacker) && this.dragon.hasLineOfSight(attacker)) {
            this.selected = attacker;
            return true;
         } else {
            LivingEntity owner = GuardEscort.owner(this.dragon);
            if (this.dragon.tickCount < this.nextCompanionScanTick) {
               return false;
            } else if (owner != null || this.dragon.getCommand() == 0 && CompanionDefense.canRespond(this.dragon)) {
               this.nextCompanionScanTick = this.dragon.tickCount + 10;
               LivingEntity origin = (LivingEntity)(owner == null ? this.dragon : owner);
               double range = owner == null ? 24.0 : (Double)DragonMaidConfig.GUARD_COMBAT_RADIUS.get() + BreathProfile.forDragon(this.dragon).range();
               this.selected = this.dragon
                  .level()
                  .getEntitiesOfClass(
                     LivingEntity.class,
                     origin.getBoundingBox().inflate(range),
                     candidate -> {
                        if (candidate instanceof Mob mob
                           && mob.getTarget() != null
                           && mob.getTarget().isAlive()
                           && CompanionRelations.protectedOwners(mob.getTarget()).contains(this.dragon.getOwnerUUID())
                           && CompanionDefense.isThreat(this.dragon, candidate)
                           && this.eligible(candidate)
                           && this.dragon.hasLineOfSight(candidate)) {
                           return true;
                        }

                        return false;
                     }
                  )
                  .stream()
                  .min(Comparator.comparingDouble(this.dragon::distanceToSqr))
                  .orElse(null);
               return this.selected != null;
            } else {
               return false;
            }
         }
      }
   }

   public boolean canContinueToUse() {
      if (this.available() && this.eligible(this.selected) && CompanionDefense.isThreat(this.dragon, this.selected)) {
         LivingEntity current = this.dragon.getTarget();
         return current == null || current == this.selected || !HumanoidTargetGoal.isEnemy(this.dragon, current);
      } else {
         return false;
      }
   }

   public void start() {
      if (this.selected.getVehicle() != this.dragon) {
         this.dragon.setTarget(this.selected);
      }
   }

   public boolean requiresUpdateEveryTick() {
      return true;
   }

   public void tick() {
      if (this.selected.getVehicle() != this.dragon && this.dragon.getTarget() == null && this.dragon.hasLineOfSight(this.selected)) {
         this.dragon.setTarget(this.selected);
      }
   }

   public void stop() {
      if (this.selected != null && this.dragon.getTarget() == this.selected && (this.dragon.getCommand() == 1 || !this.eligible(this.selected))) {
         this.dragon.setTarget(null);
      }

      this.selected = null;
   }
}
