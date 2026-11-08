package com.example.dragonmaid.combat;

import com.example.dragonmaid.DragonMaidConfig;
import com.example.dragonmaid.accessory.DragonAccessories;
import com.example.dragonmaid.accessory.DragonHands;
import com.example.dragonmaid.compat.HumanoidForge;
import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.data.DragonState;
import com.example.dragonmaid.data.HumanoidWater;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.LightningDragonEntity;
import java.util.EnumSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

public final class HumanoidGoal extends Goal {
   private static final double MELEE_PURSUIT_RANGE = 4.0;
   private final DragonBaseEntity dragon;
   private final GuardEscort escort;
   private final HumanoidHome home;
   private final HumanoidTailSweep tailSweep;
   private final HumanoidIdleLook idleLook;
   private int meleeCooldown;
   private int tailSweepCooldown;
   private int breathCooldown;
   private int breathTicks;
   private int windupTicks;
   private int burstElapsed;
   private int chargeCooldown;
   private HumanoidMeleeTiming melee;
   private LivingEntity meleeTarget;
   private int lightningReserve = -1;
   private int lightningUseRemainder;
   private int lightningTicks;
   private int lightningAimTicks;
   private boolean lightningExhausted;
   private LivingEntity previousTarget;
   private boolean defendingThisTick;

   public HumanoidGoal(DragonBaseEntity dragon) {
      this.dragon = dragon;
      this.escort = new GuardEscort(dragon);
      this.home = new HumanoidHome(dragon);
      this.tailSweep = new HumanoidTailSweep(dragon);
      this.idleLook = new HumanoidIdleLook(dragon);
      this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
   }

   public CompoundTag saveCombatState() {
      int recovery = this.windupTicks <= 0 && this.breathTicks <= 0 ? this.breathCooldown : Math.max(this.breathCooldown, this.recoveryTicks());
      int reserve = this.lightningReserve < 0 ? HumanoidCombatState.lightningCapacity(this.dragon.getDragonStage()) : this.lightningReserve;
      return new HumanoidCombatState(
            this.meleeCooldown, recovery, this.chargeCooldown, reserve, this.lightningExhausted, this.lightningUseRemainder, this.tailSweepCooldown
         )
         .save(this.dragon.getDragonStage());
   }

   public void loadCombatState(CompoundTag dragonMaid) {
      HumanoidCombatState state = HumanoidCombatState.read(dragonMaid, this.dragon.getDragonStage());
      this.meleeCooldown = state.meleeCooldown();
      this.tailSweepCooldown = state.tailSweepCooldown();
      this.breathCooldown = state.breathCooldown();
      this.chargeCooldown = state.chargeCooldown();
      this.lightningReserve = state.lightningReserve();
      this.lightningExhausted = state.lightningExhausted();
      this.lightningUseRemainder = state.lightningUseRemainder();
      this.windupTicks = this.breathTicks = this.burstElapsed = this.lightningTicks = this.lightningAimTicks = 0;
      this.melee = null;
      this.meleeTarget = null;
      this.previousTarget = null;
      this.tailSweep.cancel();
      this.escort.reset();
      this.dragon.setTarget(null);
      this.dragon.burningTarget = null;
      HumanoidCombat.stopBreath(this.dragon);
      DragonForms.state(this.dragon).dragonmaid$setAction(0);
   }

   public boolean canUse() {
      return DragonForms.isHumanoid(this.dragon);
   }

   public boolean canContinueToUse() {
      return this.canUse();
   }

   public boolean requiresUpdateEveryTick() {
      return true;
   }

   public void tickImmobileLook() {
      if (this.dragon.isImmobile()) {
         if (!HumanoidIdleLook.seatedGlance(this.dragon)) {
            this.idleLook.reset();
         } else {
            this.idleLook.tick();
            this.dragon.getLookControl().tick();
            HumanoidIdleLook.limitSeatedHead(this.dragon);
         }
      }
   }

   public void stop() {
      this.stop(true);
   }

   private void stop(boolean resetIdleLook) {
      DragonShield.of(this.dragon).lower(this.dragon);
      if (resetIdleLook) {
         this.idleLook.reset();
      }

      this.tailSweep.cancel();
      this.home.reset();
      this.dragon.getNavigation().stop();
      HumanoidCombat.stopBreath(this.dragon);
      DragonForms.state(this.dragon).dragonmaid$setAction(0);
      this.dragon.burningTarget = null;
      this.cancelBurst();
      this.escort.reset();
      this.lightningAimTicks = 0;
      this.melee = null;
      this.meleeTarget = null;
      this.previousTarget = null;
      BreathAnimationSync.flush(this.dragon);
   }

   private void cancelBurst() {
      BreathAnimationSync.phase(this.dragon, 4);
      if (this.windupTicks > 0 || this.breathTicks > 0) {
         this.breathCooldown = this.recoveryTicks();
      }

      this.windupTicks = 0;
      this.breathTicks = 0;
      this.burstElapsed = 0;
   }

   private void recoverLightning() {
      if (this.dragon instanceof LightningDragonEntity) {
         int capacity = BreathProfile.forStage(this.dragon.getDragonStage()).stage() * 20;
         int remaining = Math.min(capacity * 10, this.lightningReserve * 10 - this.lightningUseRemainder + 10);
         this.lightningReserve = (remaining + 9) / 10;
         this.lightningUseRemainder = this.lightningReserve * 10 - remaining;
         if (remaining == capacity * 10) {
            this.lightningExhausted = false;
         }
      }
   }

   private int recoveryTicks() {
      return DragonAccessories.breathRecoveryTicks(this.dragon, (Integer)DragonMaidConfig.BREATH_RECOVERY.get());
   }

   private boolean consumeLightning() {
      int cost = DragonAccessories.enhancesBreath(this.dragon) ? 9 : 10;
      int remaining = Math.max(0, this.lightningReserve * 10 - this.lightningUseRemainder - cost);
      this.lightningReserve = (remaining + 9) / 10;
      this.lightningUseRemainder = this.lightningReserve * 10 - remaining;
      return remaining == 0;
   }

   private void launchCharge(LivingEntity target) {
      if (this.dragon.getDragonStage() >= 5 && this.chargeCooldown == 0 && SafeDragonCharges.launch(this.dragon, target) != null) {
         this.chargeCooldown = (Integer)DragonMaidConfig.CHARGE_COOLDOWN.get();
      }
   }

   public void tick() {
      BreathAnimationSync.beginTick(this.dragon);
      this.defendingThisTick = false;

      try {
         HumanoidMeleeTiming previous = this.melee;
         this.tickCombat();
         this.finishUnchainedStrike(previous);
      } finally {
         if (!this.defendingThisTick) {
            DragonShield.of(this.dragon).lower(this.dragon);
         }

         BreathAnimationSync.flush(this.dragon);
      }
   }

   private void beginStrike() {
      HumanoidMeleeMode selectedMode = (HumanoidMeleeMode)DragonMaidConfig.HUMANOID_MELEE_MODE.get();
      if (selectedMode != HumanoidMeleeMode.ANIMATED) {
         this.melee = null;
         this.meleeTarget = null;
         DragonForms.state(this.dragon).dragonmaid$setAction(0);
         this.meleeCooldown = DragonCombatAccessories.wild(this.dragon) ? 20 : 24;
         HumanoidCombat.claw(this.dragon, HumanoidCombat.clawReach(this.dragon));
      } else {
         long now = this.dragon.level().getGameTime();
         boolean meleeWeapon = !DragonHands.mainHand(this.dragon).isEmpty();
         this.meleeTarget = this.dragon.getTarget();
         this.melee = HumanoidMeleeTiming.begin(this.melee, meleeWeapon, DragonCombatAccessories.wild(this.dragon), now);
         this.meleeCooldown = this.melee.duration();
         DragonForms.state(this.dragon).dragonmaid$setMeleeMotion(this.melee.encode());
         this.dragon.getNavigation().stop();
      }
   }

   private void finishUnchainedStrike(HumanoidMeleeTiming previous) {
      if (previous != null && previous == this.melee && !previous.exiting()) {
         long now = this.dragon.level().getGameTime();
         if (now - previous.start() >= (long)previous.duration()) {
            this.meleeTarget = null;
            int action = DragonForms.state(this.dragon).dragonmaid$getAction();
            if (action != 0 && action != 1) {
               this.melee = null;
            } else {
               this.melee = previous.exit(now);
               DragonForms.state(this.dragon).dragonmaid$setMeleeMotion(this.melee.encode());
            }
         }
      }
   }

   private boolean tickCommittedStrike() {
      if (this.melee == null) {
         return false;
      } else {
         DragonState state = DragonForms.state(this.dragon);
         long age = this.dragon.level().getGameTime() - this.melee.start();
         if (DragonMaidConfig.HUMANOID_MELEE_MODE.get() == HumanoidMeleeMode.ANIMATED
            && state.dragonmaid$getAction() == 1
            && !this.dragon.isModelDead()
            && this.dragon.getCommand() != 1
            && !this.dragon.isSleeping()
            && !this.dragon.isPassenger()
            && !this.dragon.isNoAi()
            && age >= 0L
            && age <= (long)this.melee.duration()
            && (age != (long)this.melee.duration() || !this.melee.exiting())) {
            if (age == (long)this.melee.duration()) {
               return false;
            } else {
               LivingEntity target = this.meleeTarget;
               if (target != null && target.isAlive()) {
                  this.dragon.getLookControl().setLookAt(target, 30.0F, 30.0F);
               }

               if (!this.melee.exiting() && age == (long)this.melee.hitTick() && (this.dragon.getCommand() != 2 || GuardEscort.owner(this.dragon) != null)) {
                  HumanoidCombat.claw(this.dragon, HumanoidCombat.clawReach(this.dragon), target, 5.0);
               }

               this.dragon.getNavigation().stop();
               this.idleLook.reset();
               HumanoidCombat.stopBreath(this.dragon);
               this.recoverLightning();
               return true;
            }
         } else {
            this.melee = null;
            this.meleeTarget = null;
            if (state.dragonmaid$getAction() == 1) {
               state.dragonmaid$setAction(0);
            }

            return false;
         }
      }
   }

   private void tickCombat() {
      if (this.lightningReserve < 0) {
         this.lightningReserve = BreathProfile.forStage(this.dragon.getDragonStage()).stage() * 20;
      }

      if (this.meleeCooldown > 0) {
         this.meleeCooldown--;
      }

      if (this.tailSweepCooldown > 0) {
         this.tailSweepCooldown--;
      }

      if (this.breathCooldown > 0) {
         this.breathCooldown--;
      }

      if (this.chargeCooldown > 0) {
         this.chargeCooldown--;
      }

      if (this.tailSweep.tick()) {
         this.idleLook.reset();
         this.home.reset();
         HumanoidCombat.stopBreath(this.dragon);
         this.recoverLightning();
      } else if (!this.tickCommittedStrike()) {
         if (HumanoidForge.isWorking(this.dragon)) {
            this.home.reset();
         }

         if (HumanoidForge.tryWork(this.dragon)) {
            this.idleLook.reset();
            this.cancelBurst();
            this.escort.reset();
            this.lightningAimTicks = 0;
            this.recoverLightning();
         } else if (!this.dragon.isModelDead() && !this.dragon.isSleeping() && !this.dragon.isPassenger()) {
            if (this.dragon.getCommand() == 1) {
               this.stop(false);
               this.idleLook.tick();
               this.recoverLightning();
            } else {
               LivingEntity owner = GuardEscort.owner(this.dragon);
               if (owner != null) {
                  this.escort.tryTeleport(owner);
               } else {
                  this.escort.reset();
               }

               LivingEntity target = this.dragon.getTarget();
               if (target != null) {
                  this.home.reset();
               }

               if (target != null
                  && (
                     this.dragon.getCommand() == 2 && owner == null
                        || !HumanoidTargetGoal.isEnemy(this.dragon, target)
                        || !GuardEscort.reachableTarget(this.dragon, target)
                  )) {
                  this.dragon.setTarget(null);
                  target = null;
               }

               if (target != null || this.dragon.getCommand() != 0) {
                  this.home.reset();
               }

               if (target != this.previousTarget) {
                  this.escort.targetChanged();
                  this.previousTarget = target;
               }

               boolean usedBreath = false;
               boolean charging = false;
               BreathProfile profile = BreathProfile.forDragon(this.dragon);
               double baseReach = HumanoidCombat.clawReach(this.dragon);
               double reach = DragonCombatAccessories.clawRadius(this.dragon, baseReach);
               boolean pursueMelee = false;
               if (target != null) {
                  this.dragon.getLookControl().setLookAt(target, 30.0F, 30.0F);
                  double distance = (double)this.dragon.distanceTo(target);
                  boolean visible = this.dragon.hasLineOfSight(target);
                  DragonShield shield = DragonShield.of(this.dragon);
                  if (this.windupTicks == 0
                     && this.breathTicks == 0
                     && this.lightningAimTicks == 0
                     && !HumanoidRoar.isRoaring(this.dragon)
                     && shield.canDefend(this.dragon, target, visible, distance)) {
                     if (this.melee != null && !this.melee.exiting()) {
                        this.melee = this.melee.exit(this.dragon.level().getGameTime());
                        this.meleeTarget = null;
                        DragonForms.state(this.dragon).dragonmaid$setMeleeMotion(this.melee.encode());
                        return;
                     }

                     if (shield.raise(this.dragon)) {
                        this.defendingThisTick = true;
                        this.dragon.getNavigation().stop();
                        this.idleLook.reset();
                        HumanoidCombat.stopBreath(this.dragon);
                        DragonForms.state(this.dragon).dragonmaid$setAction(0);
                        this.recoverLightning();
                        return;
                     }
                  }

                  boolean rekindleHydra = KnowledgeSupport.needsHydraFire(this.dragon, target);
                  boolean wildMelee = !rekindleHydra && DragonCombatAccessories.wild(this.dragon) && visible && distance <= 6.0;
                  if (!wildMelee && !rekindleHydra) {
                     HumanoidRoar.tryRoar(this.dragon);
                  }

                  if (HumanoidRoar.isRoaring(this.dragon)) {
                     this.idleLook.reset();
                     if (owner != null) {
                        this.escort.move(owner, target, profile, false, reach);
                     } else {
                        this.dragon.getNavigation().stop();
                     }

                     HumanoidCombat.stopBreath(this.dragon);
                     this.recoverLightning();
                     DragonForms.state(this.dragon).dragonmaid$setAction(5);
                     return;
                  }

                  boolean inBreathRange = profile.inRange(this.dragon, target);
                  boolean clearShot = profile.canHit(this.dragon, target);
                  boolean breathReady = this.dragon instanceof LightningDragonEntity
                     ? !this.lightningExhausted && this.lightningReserve > 0
                     : this.windupTicks > 0 || this.breathTicks > 0 || this.breathCooldown == 0;
                  if (wildMelee && (this.windupTicks > 0 || this.breathTicks > 0)) {
                     this.cancelBurst();
                  }

                  if (!breathReady || wildMelee) {
                     this.lightningAimTicks = 0;
                  } else if (!(this.dragon instanceof LightningDragonEntity)) {
                     if (this.breathTicks == 0 && this.windupTicks == 0 && this.breathCooldown == 0) {
                        this.windupTicks = (Integer)DragonMaidConfig.BREATH_WINDUP.get();
                        this.burstElapsed = 0;
                        BreathAnimationSync.begin(this.dragon, this.windupTicks, false);
                     }

                     if (clearShot) {
                        int until = this.windupTicks > 0 ? this.windupTicks : (14 - this.burstElapsed % 14) % 14;
                        BreathAnimationSync.forecast(this.dragon, until);
                     }

                     if (this.windupTicks > 0) {
                        charging = true;
                        this.windupTicks--;
                        BreathAnimationSync.preparationRemaining(this.dragon, this.windupTicks);
                        if (this.windupTicks == 0) {
                           this.breathTicks = profile.duration();
                           BreathAnimationSync.phase(this.dragon, 1);
                        }
                     } else if (this.breathTicks > 0 && clearShot) {
                        boolean emit = this.burstElapsed % 14 == 0;
                        HumanoidCombat.breath(this.dragon, target, emit);
                        if (emit && visible && inBreathRange) {
                           this.launchCharge(target);
                        }

                        this.burstElapsed++;
                        usedBreath = true;
                        if (--this.breathTicks == 0) {
                           this.breathCooldown = this.recoveryTicks();
                           BreathAnimationSync.phase(this.dragon, 3);
                        }
                     } else {
                        charging = true;
                     }
                  } else {
                     int maxAim = (Integer)DragonMaidConfig.BREATH_WINDUP.get();
                     BreathAnimationSync.begin(this.dragon, maxAim, true);
                     this.lightningAimTicks = Math.min(maxAim, this.lightningAimTicks + 1);
                     BreathAnimationSync.preparationRemaining(this.dragon, maxAim - this.lightningAimTicks);
                     if (clearShot || owner != null && this.lightningAimTicks >= maxAim) {
                        if (this.dragon.getDragonStage() >= 5 && visible && inBreathRange && this.chargeCooldown > 0) {
                           BreathAnimationSync.forecast(this.dragon, this.chargeCooldown);
                        }

                        this.lightningAimTicks = maxAim;
                        BreathAnimationSync.phase(this.dragon, 2);
                        HumanoidCombat.breath(this.dragon, target, this.lightningTicks++ % 10 == 0);
                        if (visible && inBreathRange) {
                           this.launchCharge(target);
                        }

                        usedBreath = true;
                        if (this.consumeLightning()) {
                           this.lightningExhausted = true;
                           BreathAnimationSync.phase(this.dragon, 3);
                        }
                     } else {
                        charging = true;
                     }
                  }

                  boolean frostyTailReady = NativeTailReach.enhanced(this.dragon)
                     && this.tailSweepCooldown == 0
                     && distance <= HumanoidTailSweep.reach(this.dragon);
                  pursueMelee = !usedBreath && !charging && (wildMelee || distance <= Math.max(4.0, reach) || frostyTailReady);
                  if (pursueMelee && visible && this.meleeCooldown == 0) {
                     if (distance <= HumanoidTailSweep.reach(this.dragon) && this.tailSweepCooldown == 0 && this.tailSweep.tryStart(target)) {
                        this.idleLook.reset();
                        this.tailSweepCooldown = DragonCombatAccessories.wild(this.dragon) ? 96 : 120;
                        this.meleeCooldown = DragonCombatAccessories.wild(this.dragon) ? 20 : 24;
                        this.melee = null;
                        this.meleeTarget = null;
                        this.cancelBurst();
                        this.recoverLightning();
                        return;
                     }

                     if (distance <= reach) {
                        this.beginStrike();
                     }
                  }

                  if (owner == null) {
                     if (!usedBreath && (!charging || !clearShot) && (!visible || (pursueMelee ? !(distance <= reach) : !clearShot))) {
                        if (this.dragon.getNavigation().isDone() || this.dragon.tickCount % 10 == 0) {
                           this.dragon.getNavigation().moveTo(target, 1.1);
                        }
                     } else {
                        this.dragon.getNavigation().stop();
                     }
                  }
               } else {
                  this.cancelBurst();
                  this.lightningAimTicks = 0;
                  if (this.dragon.getCommand() == 0 && this.home.move()) {
                     this.idleLook.reset();
                     HumanoidCombat.stopBreath(this.dragon);
                     this.recoverLightning();
                     DragonForms.state(this.dragon).dragonmaid$setAction(0);
                     return;
                  }

                  if (this.dragon.getCommand() == 0 && this.dragon.getNavigation().isDone() && this.dragon.tickCount % 80 == 0) {
                     Vec3 position = HumanoidWater.wander(this.dragon);
                     if (position == null) {
                        position = DefaultRandomPos.getPos(this.dragon, 8, 4);
                     }

                     position = this.home.limitWander(position);
                     if (position != null) {
                        this.dragon.getNavigation().moveTo(position.x, position.y, position.z, 0.8);
                     }
                  } else if (owner == null && this.dragon.getCommand() == 2) {
                     this.dragon.getNavigation().stop();
                  }
               }

               if (owner != null) {
                  this.escort.move(owner, target, profile, pursueMelee, reach);
               }

               if (!usedBreath) {
                  HumanoidCombat.stopBreath(this.dragon);
                  this.recoverLightning();
               }

               DragonForms.state(this.dragon).dragonmaid$setAction(usedBreath ? 2 : (charging ? 3 : (this.melee != null ? 1 : 0)));
               this.idleLook.tick();
            }
         } else {
            this.stop();
            this.recoverLightning();
         }
      }
   }
}
