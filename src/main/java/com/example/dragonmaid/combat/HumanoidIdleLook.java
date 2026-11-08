package com.example.dragonmaid.combat;

import com.example.dragonmaid.compat.HumanoidForge;
import com.example.dragonmaid.data.DragonForms;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.FireDragonEntity;
import com.iafenvoy.iceandfire.entity.IceDragonEntity;
import java.util.List;
import java.util.UUID;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;

public final class HumanoidIdleLook {
   public static final double RANGE = 6.0;
   private final DragonBaseEntity dragon;
   private final GoalSelector selector;
   private UUID lastWatched;

   public HumanoidIdleLook(final DragonBaseEntity dragon) {
      this.dragon = dragon;
      this.selector = new GoalSelector(() -> dragon.level().getProfiler());
      this.selector
         .addGoal(
            1,
            new LookAtPlayerGoal(dragon, LivingEntity.class, 6.0F) {
               public boolean canUse() {
                  if (this.mob.getRandom().nextFloat() >= this.probability) {
                     return false;
                  } else {
                     List<LivingEntity> candidates = dragon.level()
                        .getEntitiesOfClass(
                           LivingEntity.class,
                           dragon.getBoundingBox().inflate(6.0, 3.0, 6.0),
                           target -> HumanoidIdleLook.this.valid(target) && dragon.hasLineOfSight(target)
                        );
                     if (candidates.size() > 1 && HumanoidIdleLook.this.lastWatched != null) {
                        candidates.removeIf(target -> HumanoidIdleLook.this.lastWatched.equals(target.getUUID()));
                     }

                     if (candidates.isEmpty()) {
                        return false;
                     } else {
                        this.lookAt = (Entity)candidates.get(dragon.getRandom().nextInt(candidates.size()));
                        HumanoidIdleLook.this.lastWatched = this.lookAt.getUUID();
                        return true;
                     }
                  }
               }
            }
         );
      this.selector.addGoal(2, new RandomLookAroundGoal(dragon) {
         private boolean still() {
            return dragon.getNavigation().isDone() && dragon.getDeltaMovement().horizontalDistanceSqr() < 4.0E-4;
         }

         public boolean canUse() {
            return this.still() && super.canUse();
         }

         public boolean canContinueToUse() {
            return this.still() && super.canContinueToUse();
         }
      });
   }

   public static boolean eligible(DragonBaseEntity dragon) {
      return !dragon.level().isClientSide && poseAllowsLooking(dragon);
   }

   public static boolean seatedGlance(DragonBaseEntity dragon) {
      return DragonForms.isSeated(dragon) && poseAllowsLooking(dragon);
   }

   public static void limitSeatedHead(DragonBaseEntity dragon) {
      dragon.yBodyRot = dragon.getYRot();
      float offset = Mth.clamp(Mth.wrapDegrees(dragon.getYHeadRot() - dragon.yBodyRot), -45.0F, 45.0F);
      dragon.setYHeadRot(dragon.yBodyRot + offset);
   }

   private static boolean poseAllowsLooking(DragonBaseEntity dragon) {
      boolean seated = DragonForms.isSeated(dragon);
      boolean seatedSpecies = dragon instanceof FireDragonEntity || dragon instanceof IceDragonEntity;
      return DragonForms.isHumanoid(dragon)
         && dragon.isAlive()
         && !dragon.isRemoved()
         && !dragon.isModelDead()
         && !dragon.isNoAi()
         && !dragon.isSleeping()
         && (seated ? seatedSpecies && dragon.onGround() && !dragon.isInWaterOrBubble() : dragon.getCommand() != 1)
         && !dragon.isPassenger()
         && !dragon.isVehicle()
         && (dragon.onGround() || dragon.isInWaterOrBubble())
         && !dragon.isInLava()
         && dragon.getTarget() == null
         && !dragon.isTackling()
         && !dragon.isBreathingFire()
         && dragon.burningTarget == null
         && DragonForms.state(dragon).dragonmaid$getAction() == 0
         && dragon.getAttackAnim(1.0F) <= 0.0F
         && !HumanoidRoar.isRoaring(dragon)
         && !TailSweepSync.isBusy(dragon)
         && !HumanoidForge.isWorking(dragon);
   }

   private boolean valid(LivingEntity target) {
      return target != this.dragon
         && target.level() == this.dragon.level()
         && target.isAlive()
         && !target.isRemoved()
         && !target.isSpectator()
         && !target.isInvisible()
         && target.canBeSeenByAnyone()
         && this.dragon.distanceToSqr(target) <= 36.0;
   }

   public boolean tick() {
      if (!eligible(this.dragon)) {
         this.reset();
         return false;
      } else {
         if ((this.dragon.level().getServer().getTickCount() + this.dragon.getId()) % 2 != 0 && this.dragon.tickCount > 1) {
            this.selector.tickRunningGoals(false);
         } else {
            this.selector.tick();
         }

         boolean looking = this.selector.getAvailableGoals().stream().anyMatch(WrappedGoal::isRunning);
         DragonForms.state(this.dragon).dragonmaid$setIdleLooking(looking);
         return looking;
      }
   }

   public void reset() {
      if (!this.dragon.level().isClientSide) {
         this.selector.getAvailableGoals().stream().filter(WrappedGoal::isRunning).forEach(WrappedGoal::stop);
         DragonForms.state(this.dragon).dragonmaid$setIdleLooking(false);
      }
   }
}
