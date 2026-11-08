package com.example.dragonmaid.combat;

import com.example.dragonmaid.DragonMaidConfig;
import com.example.dragonmaid.accessory.MiniatureForm;
import com.example.dragonmaid.data.DragonForms;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.util.Comparator;
import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.monster.Enemy;

public final class NativeHostileTargetGoal extends Goal {
   private final DragonBaseEntity dragon;
   private LivingEntity selected;
   private int nextScan;
   private int unseenTicks;

   public NativeHostileTargetGoal(DragonBaseEntity dragon) {
      this.dragon = dragon;
      this.setFlags(EnumSet.of(Flag.TARGET));
   }

   private boolean available() {
      return !DragonForms.isHumanoid(this.dragon)
         && DragonForms.state(this.dragon).dragonmaid$getKnowledgeLevel() >= 1
         && WildDragonCombat.canAcquireTargets(this.dragon)
         && this.dragon.canMove()
         && this.dragon.getControllingPassenger() == null;
   }

   private boolean eligible(LivingEntity candidate) {
      return candidate instanceof Enemy
         && HumanoidTargetGoal.isEnemy(this.dragon, candidate)
         && GuardEscort.reachableTarget(this.dragon, candidate)
         && (GuardEscort.owner(this.dragon) != null || this.dragon.distanceToSqr(candidate) <= 576.0);
   }

   public boolean canUse() {
      if (this.available() && this.dragon.getTarget() == null && this.dragon.tickCount >= this.nextScan) {
         this.nextScan = this.dragon.tickCount + 10;
         LivingEntity owner = GuardEscort.owner(this.dragon);
         LivingEntity origin = (LivingEntity)(owner == null ? this.dragon : owner);
         double range = owner == null
            ? 24.0
            : (
               MiniatureForm.hasDragonEffect(this.dragon)
                  ? MiniatureEscortGoal.combatRadius()
                  : (Double)DragonMaidConfig.GUARD_COMBAT_RADIUS.get() + BreathProfile.forDragon(this.dragon).range()
            );
         this.selected = this.dragon
            .level()
            .getEntitiesOfClass(Mob.class, origin.getBoundingBox().inflate(range), candidate -> this.eligible(candidate) && this.dragon.hasLineOfSight(candidate))
            .stream()
            .min(Comparator.comparingDouble(this.dragon::distanceToSqr))
            .orElse(null);
         return this.selected != null;
      } else {
         return false;
      }
   }

   public void start() {
      this.unseenTicks = 0;
      this.dragon.setTarget(this.selected);
   }

   public boolean canContinueToUse() {
      return this.available()
         && this.eligible(this.selected)
         && this.unseenTicks <= 40
         && (this.dragon.getTarget() == this.selected || this.selected.getVehicle() == this.dragon);
   }

   public boolean requiresUpdateEveryTick() {
      return true;
   }

   public void tick() {
      this.unseenTicks = this.dragon.hasLineOfSight(this.selected) ? 0 : this.unseenTicks + 1;
   }

   public void stop() {
      if (this.dragon.getTarget() == this.selected) {
         this.dragon.setTarget(null);
      }

      this.selected = null;
   }
}
