package com.example.dragonmaid.combat;

import com.example.dragonmaid.compat.HumanoidForge;
import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.movement.HumanoidFlight;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.FireDragonEntity;
import com.iafenvoy.iceandfire.entity.IceDragonEntity;
import java.util.List;
import java.util.UUID;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.phys.Vec3;

public final class HumanoidIdleLook {
   public static final double RANGE = 6.0;
   private final DragonBaseEntity dragon;
   private final GoalSelector selector;
   private UUID lastWatched;
   private long nextAirLook;
   private long airLookUntil;
   private long nextVisibilityCheck;
   private LivingEntity airWatch;
   private Vec3 airDirection;

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
      boolean airborne = HumanoidFlight.autonomousInAir(dragon);
      return DragonForms.isHumanoid(dragon)
         && dragon.isAlive()
         && !dragon.isRemoved()
         && !dragon.isModelDead()
         && !dragon.isNoAi()
         && !dragon.isSleeping()
         && (seated ? seatedSpecies && dragon.onGround() && !dragon.isInWaterOrBubble() : dragon.getCommand() != 1)
         && !dragon.isPassenger()
         && !dragon.isVehicle()
         && (dragon.onGround() || dragon.isInWaterOrBubble() || airborne)
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
      } else if (HumanoidFlight.autonomousInAir(this.dragon)) {
         return this.tickAirLook();
      } else {
         if ((this.dragon.level().getServer().getTickCount() + this.dragon.getId()) % 2 != 0 && this.dragon.tickCount > 1) {
            this.selector.tickRunningGoals(false);
         } else {
            this.selector.tick();
         }

         boolean looking = this.selector.getAvailableGoals().stream().anyMatch(net.minecraft.world.entity.ai.goal.WrappedGoal::isRunning);
         DragonForms.state(this.dragon).dragonmaid$setIdleLooking(looking);
         return looking;
      }
   }

   private boolean tickAirLook() {
      long now = this.dragon.level().getGameTime();
      RandomSource random = this.dragon.getRandom();
      if (this.nextAirLook == 0L) {
         this.nextAirLook = now + 16L + (long)random.nextInt(41);
      }

      if (now >= this.nextAirLook) {
         this.airWatch = null;
         int choice = random.nextInt(3);
         LivingEntity owner = this.dragon.getOwner();
         if (choice == 0 && owner != null && this.valid(owner) && this.dragon.hasLineOfSight(owner)) {
            this.airWatch = owner;
         } else if (choice != 2) {
            List<LivingEntity> candidates = this.dragon
               .level()
               .getEntitiesOfClass(LivingEntity.class, this.dragon.getBoundingBox().inflate(6.0, 3.0, 6.0), target -> this.valid(target) && this.dragon.hasLineOfSight(target));
            if (candidates.size() > 1 && this.lastWatched != null) {
               candidates.removeIf(target -> this.lastWatched.equals(target.getUUID()));
            }

            if (!candidates.isEmpty()) {
               this.airWatch = candidates.get(random.nextInt(candidates.size()));
            }
         }

         if (this.airWatch != null) {
            this.lastWatched = this.airWatch.getUUID();
         }

         this.nextVisibilityCheck = now + 8L;
         this.airDirection = this.airWatch == null
            ? Vec3.directionFromRotation(random.nextFloat() * 30.0F - 15.0F, this.dragon.getYRot() + random.nextFloat() * 160.0F - 80.0F)
            : null;
         this.airLookUntil = now + 20L + (long)random.nextInt(21);
         this.nextAirLook = this.airLookUntil + 40L + (long)random.nextInt(81);
      }

      Vec3 lookAt = null;
      if (now < this.airLookUntil) {
         if (this.airWatch != null && this.valid(this.airWatch) && this.airWatchVisible(now)) {
            lookAt = this.airWatch.getEyePosition();
         } else if (this.airDirection != null) {
            lookAt = this.dragon.getEyePosition().add(this.airDirection.scale(6.0));
         }
      }

      if (lookAt != null) {
         HumanoidFlight.of(this.dragon).glance(lookAt, false);
      } else {
         this.airWatch = null;
         this.airDirection = null;
      }

      DragonForms.state(this.dragon).dragonmaid$setIdleLooking(lookAt != null);
      return lookAt != null;
   }

   private boolean airWatchVisible(long now) {
      if (now < this.nextVisibilityCheck) {
         return true;
      } else {
         this.nextVisibilityCheck = now + 8L;
         return this.dragon.getSensing().hasLineOfSight(this.airWatch);
      }
   }

   public void reset() {
      if (!this.dragon.level().isClientSide) {
         for (net.minecraft.world.entity.ai.goal.WrappedGoal wrapped : this.selector.getAvailableGoals()) {
         if (wrapped.isRunning()) {
            wrapped.stop();
         }
      }
         this.nextAirLook = this.airLookUntil = 0L;
         this.airWatch = null;
         this.airDirection = null;
         DragonForms.state(this.dragon).dragonmaid$setIdleLooking(false);
      }
   }
}