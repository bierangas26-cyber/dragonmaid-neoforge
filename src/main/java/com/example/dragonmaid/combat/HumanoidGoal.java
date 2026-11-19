package com.example.dragonmaid.combat;

import com.example.dragonmaid.DragonMaidConfig;
import com.example.dragonmaid.accessory.DragonAccessories;
import com.example.dragonmaid.accessory.DragonHands;
import com.example.dragonmaid.compat.HumanoidForge;
import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.data.DragonState;
import com.example.dragonmaid.data.HumanoidWater;
import com.example.dragonmaid.entity.DragonTridentFlight;
import com.example.dragonmaid.movement.HumanoidFlight;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.IceDragonEntity;
import com.iafenvoy.iceandfire.entity.LightningDragonEntity;
import java.util.EnumSet;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class HumanoidGoal extends Goal {
   public static final int MOUNTED_DIRECT_STRIKE_ACTION = 15;
   private static final int MOUNTED_DIRECT_STRIKE_POSE_TICKS = 6;
   private static final double MELEE_PURSUIT_RANGE = 4.0;
   private final DragonBaseEntity dragon;
   private final GuardEscort escort;
   private final HumanoidHome home;
   private final HumanoidTailSweep tailSweep;
   private final HumanoidArmedSweep armedSweep;
   private final HumanoidWingThrust wingThrust;
   private final HumanoidIdleLook idleLook;
   private final DragonKatanaOpener katana;
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
   private boolean flightCombat;
   private boolean mountedCombat;
   private int mountedDirectStrikePoseTicks;
   private long airThrowStarted = -1L;
   private LivingEntity airThrowTarget;
   private ItemStack preparedTrident = ItemStack.EMPTY;
   private double preparedAimHeight;

   public HumanoidGoal(DragonBaseEntity dragon) {
      this.dragon = dragon;
      this.escort = new GuardEscort(dragon);
      this.home = new HumanoidHome(dragon);
      this.tailSweep = new HumanoidTailSweep(dragon);
      this.armedSweep = new HumanoidArmedSweep(dragon);
      this.wingThrust = new HumanoidWingThrust(dragon);
      this.idleLook = new HumanoidIdleLook(dragon);
      this.katana = new DragonKatanaOpener(dragon);
      this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
   }

   public CompoundTag saveCombatState() {
      int recovery = this.windupTicks <= 0 && this.breathTicks <= 0 ? this.breathCooldown : Math.max(this.breathCooldown, this.recoveryTicks());
      int reserve = this.lightningReserve < 0 ? HumanoidCombatState.lightningCapacity(this.dragon.getDragonStage()) : this.lightningReserve;
      CompoundTag saved = new HumanoidCombatState(
            this.meleeCooldown, recovery, this.chargeCooldown, reserve, this.lightningExhausted, this.lightningUseRemainder, this.tailSweepCooldown
         )
         .save(this.dragon.getDragonStage());
      saved.putInt("ArmedTailCooldown", this.armedSweep.cooldown());
      saved.putInt("WaterfowlCooldown", this.armedSweep.waterfowlCooldown());
      return saved;
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
      this.mountedDirectStrikePoseTicks = 0;
      this.melee = null;
      this.meleeTarget = null;
      this.clearPreparedThrow();
      this.previousTarget = null;
      this.tailSweep.cancel();
      this.katana.cancel();
      this.katana.leave();
      this.armedSweep.loadCooldown(dragonMaid.getCompound("Combat").getInt("ArmedTailCooldown"));
      this.armedSweep.loadWaterfowlCooldown(dragonMaid.getCompound("Combat").getInt("WaterfowlCooldown"));
      this.wingThrust.cancel();
      this.wingThrust.refresh();
      this.escort.reset();
      this.dragon.setTarget(null);
      this.dragon.burningTarget = null;
      HumanoidCombat.stopBreath(this.dragon);
      DragonForms.state(this.dragon).dragonmaid$setAction(0);
   }

   public boolean canUse() {
      return DragonForms.isHumanoid(this.dragon) && !this.dragon.isVehicle();
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
      if (!this.mountedCombat || !this.dragon.isVehicle()) {
         this.stop(true);
      }
   }

   private void stop(boolean resetIdleLook) {
      DragonShield.of(this.dragon).lower(this.dragon);
      if (resetIdleLook) {
         this.idleLook.reset();
      }

      this.tailSweep.cancel();
      this.katana.cancel();
      this.katana.leave();
      this.armedSweep.cancel();
      this.wingThrust.cancel();
      this.wingThrust.observeCombat();
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
      this.mountedDirectStrikePoseTicks = 0;
      this.clearPreparedThrow();
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

   private void moveWhileGuarding(LivingEntity owner, LivingEntity target, BreathProfile profile, double reach) {
      if (owner != null) {
         this.escort.move(owner, target, profile, true, reach);
      } else if (this.dragon.distanceToSqr(target) <= reach * reach) {
         this.dragon.getNavigation().stop();
      } else if (this.dragon.getNavigation().isDone() || this.dragon.tickCount % 10 == 0) {
         this.dragon.getNavigation().moveTo(target, 1.0);
      }
   }

   private void launchCharge(LivingEntity target) {
      if (this.dragon.getDragonStage() >= 5 && this.chargeCooldown == 0 && SafeDragonCharges.launch(this.dragon, target) != null) {
         this.chargeCooldown = (Integer)DragonMaidConfig.CHARGE_COOLDOWN.get();
      }
   }

   private void launchRiderCharge(Vec3 aim) {
      if (this.chargeCooldown == 0 && SafeDragonCharges.launch(this.dragon, aim) != null) {
         this.chargeCooldown = (Integer)DragonMaidConfig.CHARGE_COOLDOWN.get();
      }
   }

   public void tick() {
      if (!this.dragon.isVehicle()) {
         this.wingThrust.observeCombat();
         this.leaveMountedCombat();
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
   }

   private void beginStrike(LivingEntity target) {
      this.katana.consumeOpening();
      HumanoidMeleeMode selectedMode = (HumanoidMeleeMode)DragonMaidConfig.HUMANOID_MELEE_MODE.get();
      if (selectedMode != HumanoidMeleeMode.ANIMATED) {
         this.melee = null;
         this.meleeTarget = null;
         DragonForms.state(this.dragon).dragonmaid$setAction(0);
         this.meleeCooldown = DragonCombatAccessories.wild(this.dragon) ? 20 : 24;
         double reach = HumanoidCombat.clawReach(this.dragon);
         HumanoidCombat.claw(this.dragon, reach, target, DragonCombatAccessories.clawRadius(this.dragon, reach));
      } else {
         long now = this.dragon.level().getGameTime();
         ItemStack weapon = DragonHands.availableMainHand(this.dragon);
         this.meleeTarget = target;
         this.melee = HumanoidMeleeTiming.begin(
            this.melee,
            !weapon.isEmpty(),
            SpecialWeapons.fireAxe(this.dragon, weapon),
            SpecialWeapons.trained(this.dragon) && SpecialWeapons.isIceTrident(this.dragon, weapon),
            DragonCombatAccessories.wild(this.dragon),
            now
         );
         this.meleeCooldown = this.melee.duration();
         DragonForms.state(this.dragon).dragonmaid$setMeleeMotion(this.melee.encode());
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
      return this.tickCommittedStrike(null);
   }

   private boolean tickCommittedStrike(Player pilot) {
      if (this.melee == null) {
         return false;
      } else {
         DragonState state = DragonForms.state(this.dragon);
         long age = this.dragon.level().getGameTime() - this.melee.start();
         if (DragonMaidConfig.HUMANOID_MELEE_MODE.get() == HumanoidMeleeMode.ANIMATED
            && state.dragonmaid$getAction() == 1
            && !this.dragon.isModelDead()
            && (pilot != null || this.dragon.getCommand() != 1)
            && !this.dragon.isSleeping()
            && !this.dragon.isPassenger()
            && !this.dragon.isNoAi()
            && age >= 0L
            && age <= (long)this.melee.duration()
            && (age != (long)this.melee.duration() || !this.melee.exiting())) {
            if (age == (long)this.melee.duration()) {
               if (this.melee.throwingTrident()) {
                  this.melee = null;
                  this.meleeTarget = null;
                  this.clearPreparedThrow();
               }

               return false;
            } else {
               LivingEntity owner = GuardEscort.owner(this.dragon);
               LivingEntity target = this.melee.exiting() ? this.dragon.getTarget() : this.meleeTarget;
               if (pilot == null && target != null && target.isAlive()) {
                  this.dragon.getLookControl().setLookAt(target, 30.0F, 30.0F);
               }

               if (this.melee.throwingTrident()) {
                  this.dragon.getNavigation().stop();
                  this.idleLook.reset();
                  BreathProfile profile = BreathProfile.forDragon(this.dragon);
                  boolean canAim = target != null
                     && (this.dragon.getCommand() != 2 || owner != null)
                     && HumanoidTargetGoal.isEnemy(this.dragon, target)
                     && GuardEscort.reachableTarget(this.dragon, target);
                  if (age <= (long)this.melee.hitTick() && !this.tickTargetedThrow(canAim ? target : null, profile, age == (long)this.melee.hitTick())) {
                     this.melee = null;
                     this.meleeTarget = null;
                     this.clearPreparedThrow();
                     state.dragonmaid$setAction(0);
                     return false;
                  } else {
                     if (!canAim || this.tickBreath(target, profile, true) != 2) {
                        HumanoidCombat.stopBreath(this.dragon);
                        this.recoverLightning();
                     }

                     return true;
                  }
               } else {
                  double baseReach = HumanoidCombat.clawReach(this.dragon);
                  if (!this.melee.exiting()
                     && age == (long)this.melee.hitTick()
                     && (!this.melee.axe() || SpecialWeapons.fireAxe(this.dragon, DragonHands.availableMainHand(this.dragon)))
                     && (!this.melee.trident() || SpecialWeapons.isIceTrident(this.dragon, DragonHands.availableMainHand(this.dragon)))
                     && (pilot == null ? this.dragon.getCommand() != 2 || owner != null : HumanoidFlight.groundedForMelee(this.dragon))) {
                     HumanoidCombat.claw(this.dragon, baseReach, this.meleeTarget, 5.0 + SpecialWeapons.longswordExtraReach(this.dragon));
                  }

                  if (pilot != null) {
                     this.tickRiderBreath(pilot, !this.melee.exiting(), true);
                     return true;
                  } else {
                     this.idleLook.reset();
                     boolean canPursue = target != null
                        && (this.dragon.getCommand() != 2 || owner != null)
                        && HumanoidTargetGoal.isEnemy(this.dragon, target)
                        && GuardEscort.reachableTarget(this.dragon, target);
                     BreathProfile profile = BreathProfile.forDragon(this.dragon);
                     if (canPursue) {
                        double reach = DragonCombatAccessories.clawRadius(this.dragon, baseReach);
                        if (owner != null) {
                           this.escort.move(owner, target, profile, true, reach);
                        } else if (this.dragon.distanceToSqr(target) <= reach * reach && this.dragon.hasLineOfSight(target)) {
                           this.dragon.getNavigation().stop();
                        } else if (this.dragon.tickCount % (this.dragon.getNavigation().isDone() ? 5 : 10) == 0) {
                           this.dragon.getNavigation().moveTo(target, 1.1);
                        }
                     } else {
                        this.dragon.getNavigation().stop();
                     }

                     boolean canBreathe = canPursue && !this.melee.exiting();
                     int breathAction = canBreathe ? this.tickBreath(target, profile, true) : 0;
                     if (!canBreathe) {
                        this.cancelBurst();
                     }

                     if (breathAction != 2) {
                        HumanoidCombat.stopBreath(this.dragon);
                        this.recoverLightning();
                     }

                     return true;
                  }
               }
            }
         } else {
            this.melee = null;
            this.meleeTarget = null;
            this.clearPreparedThrow();
            if (state.dragonmaid$getAction() == 1) {
               state.dragonmaid$setAction(0);
            }

            return false;
         }
      }
   }

   private boolean breathReady() {
      return this.dragon instanceof LightningDragonEntity
         ? !this.lightningExhausted && this.lightningReserve > 0
         : this.windupTicks > 0 || this.breathTicks > 0 || this.breathCooldown == 0;
   }

   private boolean lightningTargetTooClose(LivingEntity target) {
      double reach = DragonCombatAccessories.clawRadius(this.dragon, HumanoidCombat.clawReach(this.dragon));
      return this.dragon.distanceToSqr(target) <= reach * reach;
   }

   private int tickBreath(LivingEntity target, BreathProfile profile, boolean meleeOverlay) {
      return this.tickBreath(target, profile, meleeOverlay, null);
   }

   private int tickBreath(LivingEntity target, BreathProfile profile, boolean meleeOverlay, Vec3 riderAim) {
      if (!(this.dragon instanceof LightningDragonEntity) || !meleeOverlay && (riderAim != null || !this.lightningTargetTooClose(target))) {
         if (!this.breathReady()) {
            this.lightningAimTicks = 0;
            return 0;
         } else {
            boolean usedBreath = false;
            boolean charging = false;
            boolean visible = target != null && this.dragon.hasLineOfSight(target);
            boolean inBreathRange = target != null && profile.inRange(this.dragon, target);
            boolean clearShot = riderAim != null || profile.canHit(this.dragon, target);
            Vec3 aim = riderAim != null ? riderAim : target.getBoundingBox().getCenter();
            if (this.dragon instanceof LightningDragonEntity) {
               int maxAim = (Integer)DragonMaidConfig.BREATH_WINDUP.get();
               BreathAnimationSync.begin(this.dragon, maxAim, true, meleeOverlay);
               this.lightningAimTicks = Math.min(maxAim, this.lightningAimTicks + 1);
               BreathAnimationSync.preparationRemaining(this.dragon, maxAim - this.lightningAimTicks);
               if (clearShot || GuardEscort.owner(this.dragon) != null && this.lightningAimTicks >= maxAim) {
                  if (this.dragon.getDragonStage() >= 5 && visible && inBreathRange && this.chargeCooldown > 0) {
                     BreathAnimationSync.forecast(this.dragon, this.chargeCooldown);
                  }

                  this.lightningAimTicks = maxAim;
                  if (BreathAnimationSync.activate(this.dragon)) {
                     this.wingThrust.refresh();
                  }

                  boolean emit = this.lightningTicks++ % 10 == 0;
                  HumanoidCombat.breath(this.dragon, aim, emit);
                  if (emit && meleeOverlay) {
                     BreathAnimationSync.shot(this.dragon, 1);
                  }

                  if (visible && inBreathRange) {
                     this.launchCharge(target);
                  } else if (riderAim != null && emit) {
                     this.launchRiderCharge(riderAim);
                  }

                  usedBreath = true;
                  if (this.consumeLightning()) {
                     this.lightningExhausted = true;
                     BreathAnimationSync.phase(this.dragon, 3);
                  }
               } else {
                  charging = true;
               }
            } else {
               if (this.breathTicks == 0 && this.windupTicks == 0 && this.breathCooldown == 0) {
                  this.windupTicks = (Integer)DragonMaidConfig.BREATH_WINDUP.get();
                  this.burstElapsed = 0;
                  BreathAnimationSync.begin(this.dragon, this.windupTicks, false, meleeOverlay);
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
                  boolean emitx = this.burstElapsed % 14 == 0;
                  HumanoidCombat.breath(this.dragon, aim, emitx);
                  if (emitx && visible && inBreathRange) {
                     this.launchCharge(target);
                  } else if (riderAim != null && emitx) {
                     this.launchRiderCharge(riderAim);
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
            }

            return usedBreath ? 2 : (charging ? 3 : 0);
         }
      } else {
         this.cancelBurst();
         this.lightningAimTicks = 0;
         return 0;
      }
   }

   private void advanceCooldowns() {
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
   }

   private void tickCombat() {
      this.advanceCooldowns();
      HumanoidFlight.Phase flight = HumanoidFlight.phase(this.dragon);
      if (flight != HumanoidFlight.Phase.GROUND) {
         if (!this.flightCombat) {
            this.stop();
            this.flightCombat = true;
         }

         this.tickAutonomousFlightCombat(flight);
      } else {
         if (this.flightCombat) {
            this.clearPreparedThrow();
            this.flightCombat = false;
         }

         this.armedSweep.observe(this.dragon.getTarget());
         this.katana.observe(this.dragon.getTarget());
         if (!this.armedSweep.tick() && !this.wingThrust.tick() && !this.tailSweep.tick()) {
            if (this.katana.tick()) {
               HumanoidCombat.stopBreath(this.dragon);
               this.recoverLightning();
            } else if (!this.tickCommittedStrike()) {
               boolean working = HumanoidForge.isWorking(this.dragon);
               if (working) {
                  this.home.reset();
               }

               if (HumanoidForge.tryWork(this.dragon)) {
                  if (!working) {
                     this.wingThrust.refresh();
                  }

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
                  } else if (this.dragon.isUsingItem() || DragonForms.state(this.dragon).dragonmaid$getAction() != 9 || BladeMotion.age(this.dragon) >= 11) {
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

                     if (owner != null && this.dragon.getDragonStage() >= 4 && HumanoidFlight.of(this.dragon).walkToTakeoff(owner)) {
                        this.idleLook.reset();
                        if (this.windupTicks > 0 || this.breathTicks > 0 || this.lightningAimTicks > 0) {
                           this.cancelBurst();
                        }

                        this.lightningAimTicks = 0;
                        HumanoidCombat.stopBreath(this.dragon);
                        this.recoverLightning();
                        DragonForms.state(this.dragon).dragonmaid$setAction(0);
                     } else {
                        if (target != this.previousTarget) {
                           this.escort.targetChanged();
                           if (target == null) {
                              DragonShield.of(this.dragon).forgetRangedPressure();
                           }

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
                           double tailReach = HumanoidTailSweep.reach(this.dragon);
                           if (this.melee == null
                              && !HumanoidRoar.isRoaring(this.dragon)
                              && shield.defendWhileApproaching(this.dragon, distance, Math.max(reach, tailReach))) {
                              this.defendingThisTick = true;
                              this.idleLook.reset();
                              this.cancelBurst();
                              this.lightningAimTicks = 0;
                              HumanoidCombat.stopBreath(this.dragon);
                              this.recoverLightning();
                              this.moveWhileGuarding(owner, target, profile, reach);
                              return;
                           }

                           if (this.meleeCooldown == 0) {
                              boolean canAdvance = owner == null || this.escort.canAdvanceForAttack(owner);
                              if (canAdvance && this.armedSweep.tryWaterfowl(target) || this.wingThrust.tryStart(target, visible, distance)) {
                                 this.beginExclusiveStrike();
                                 return;
                              }

                              if (canAdvance && this.armedSweep.canApproachWaterfowl(target)) {
                                 if (owner != null) {
                                    this.escort.move(owner, target, profile, true, reach);
                                 } else if (this.dragon.getNavigation().isDone() || this.dragon.tickCount % 10 == 0) {
                                    this.dragon.getNavigation().moveTo(target, 1.1);
                                 }

                                 if (!this.dragon.getNavigation().isDone()) {
                                    this.cancelBurst();
                                    this.lightningAimTicks = 0;
                                    HumanoidCombat.stopBreath(this.dragon);
                                    this.recoverLightning();
                                    this.idleLook.reset();
                                    DragonForms.state(this.dragon).dragonmaid$setAction(0);
                                    return;
                                 }
                              }
                           }

                           if (this.meleeCooldown == 0 && this.katana.tryStart(target, visible, distance)) {
                              this.idleLook.reset();
                              this.cancelBurst();
                              this.lightningAimTicks = 0;
                              HumanoidCombat.stopBreath(this.dragon);
                              this.recoverLightning();
                              return;
                           }

                           if (this.meleeCooldown == 0 && this.armedSweep.tryStart(target)) {
                              this.beginExclusiveStrike();
                              return;
                           }

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
                                 this.idleLook.reset();
                                 HumanoidCombat.stopBreath(this.dragon);
                                 if (shield.longswordRaised(this.dragon)) {
                                    this.moveWhileGuarding(owner, target, profile, reach);
                                 } else {
                                    this.dragon.getNavigation().stop();
                                    DragonForms.state(this.dragon).dragonmaid$setAction(0);
                                 }

                                 this.recoverLightning();
                                 return;
                              }
                           }

                           boolean rekindleHydra = KnowledgeSupport.needsHydraFire(this.dragon, target);
                           boolean wildMelee = !rekindleHydra && DragonCombatAccessories.wild(this.dragon) && visible && distance <= 6.0;
                           double meleeApproach = Math.max(4.0, reach);
                           if (visible
                              && distance > Math.max(meleeApproach, tailReach)
                              && !wildMelee
                              && !rekindleHydra
                              && this.melee == null
                              && this.meleeCooldown == 0
                              && !HumanoidRoar.isRoaring(this.dragon)
                              && this.windupTicks == 0
                              && this.breathTicks == 0
                              && this.lightningAimTicks == 0
                              && !this.dragon.isBreathingFire()) {
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

                           boolean closeMelee = visible && distance <= reach;
                           boolean frostyTailReady = NativeTailReach.enhanced(this.dragon) && this.tailSweepCooldown == 0 && distance <= tailReach;
                           if (this.meleeCooldown == 0 && visible && (distance <= meleeApproach || wildMelee || frostyTailReady)) {
                              if (distance <= tailReach && this.tailSweepCooldown == 0 && this.tailSweep.tryStart(target)) {
                                 this.katana.consumeOpening();
                                 this.idleLook.reset();
                                 this.tailSweepCooldown = DragonCombatAccessories.wild(this.dragon) ? 96 : 120;
                                 this.meleeCooldown = DragonCombatAccessories.wild(this.dragon) ? 20 : 24;
                                 this.melee = null;
                                 this.meleeTarget = null;
                                 this.cancelBurst();
                                 this.lightningAimTicks = 0;
                                 this.recoverLightning();
                                 return;
                              }

                              if (closeMelee) {
                                 this.beginStrike(target);
                                 int breathAction = this.tickBreath(target, profile, true);
                                 if (breathAction != 2) {
                                    HumanoidCombat.stopBreath(this.dragon);
                                    this.recoverLightning();
                                 }

                                 if (this.melee == null) {
                                    DragonForms.state(this.dragon).dragonmaid$setAction(0);
                                 }

                                 return;
                              }
                           }

                           boolean clearShot = profile.canHit(this.dragon, target);
                           if (this.melee == null
                              && this.meleeCooldown == 0
                              && this.dragon.onGround()
                              && !this.dragon.isInWaterOrBubble()
                              && distance >= Math.max(6.0, reach + 1.0)
                              && visible
                              && profile.inRange(this.dragon, target)
                              && DragonMaidConfig.HUMANOID_MELEE_MODE.get() == HumanoidMeleeMode.ANIMATED
                              && SpecialWeapons.throwableIceTrident(this.dragon, DragonHands.availableMainHand(this.dragon))) {
                              DragonTridentFlight.Shot shot = DragonTridentFlight.prepareTargetShot(this.dragon, target);
                              if (shot != null) {
                                 this.melee = HumanoidMeleeTiming.tridentThrow(this.dragon.level().getGameTime());
                                 this.meleeTarget = target;
                                 this.prepareTrident(DragonHands.availableMainHand(this.dragon), target, shot);
                                 this.meleeCooldown = this.melee.duration();
                                 DragonForms.state(this.dragon).dragonmaid$setMeleeMotion(this.melee.encode());
                                 this.tickCommittedStrike();
                                 return;
                              }
                           }

                           if (wildMelee) {
                              this.cancelBurst();
                              this.lightningAimTicks = 0;
                           }

                           int breathActionx = wildMelee ? 0 : this.tickBreath(target, profile, false);
                           usedBreath = breathActionx == 2;
                           charging = breathActionx == 3;
                           pursueMelee = !usedBreath && !charging && (wildMelee || distance <= meleeApproach || frostyTailReady);
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
                  }
               } else {
                  this.stop();
                  this.recoverLightning();
               }
            }
         } else {
            if (this.armedSweep.waterfowlAttack()) {
               this.katana.consumeOpening();
            }

            this.cancelBurst();
            this.lightningAimTicks = 0;
            this.idleLook.reset();
            this.home.reset();
            HumanoidCombat.stopBreath(this.dragon);
            this.recoverLightning();
         }
      }
   }

   private void clearPreparedThrow() {
      if (AirTridentThrow.playing(this.dragon)) {
         DragonForms.state(this.dragon).dragonmaid$setAction(0);
      }

      this.airThrowStarted = -1L;
      this.airThrowTarget = null;
      this.preparedTrident = ItemStack.EMPTY;
   }

   private boolean holdsPreparedTrident() {
      return DragonHands.availableMainHand(this.dragon) == this.preparedTrident && SpecialWeapons.throwableIceTrident(this.dragon, this.preparedTrident);
   }

   private void prepareTrident(ItemStack weapon, LivingEntity target, DragonTridentFlight.Shot shot) {
      this.preparedTrident = weapon;
      if (shot != null) {
         AABB box = target.getBoundingBox();
         this.preparedAimHeight = (shot.aim().y - box.minY) / box.getYsize();
      }
   }

   private boolean tickTargetedThrow(LivingEntity target, BreathProfile profile, boolean release) {
      if (target != null && this.holdsPreparedTrident() && profile.inRange(this.dragon, target)) {
         AABB box = target.getBoundingBox();
         Vec3 center = box.getCenter();
         Vec3 aim = new Vec3(center.x, box.minY + box.getYsize() * this.preparedAimHeight, center.z);
         this.faceTridentTarget(aim);
         if (!release) {
            return true;
         } else {
            DragonTridentFlight.Shot shot = DragonTridentFlight.targetShot(this.dragon, target);
            return shot != null && DragonTridentFlight.launchAt(this.dragon, shot) != null;
         }
      } else {
         return false;
      }
   }

   private void faceTridentTarget(Vec3 aim) {
      Vec3 toward = aim.subtract(this.dragon.position());
      if (toward.horizontalDistanceSqr() > 1.0E-4) {
         float desired = (float)(Mth.atan2(toward.z, toward.x) * 180.0F / (float)Math.PI) - 90.0F;
         float yaw = Mth.approachDegrees(this.dragon.yBodyRot, desired, 20.0F);
         this.dragon.setYRot(yaw);
         this.dragon.setYBodyRot(yaw);
      }

      this.aimTrident(aim);
   }

   private void aimTrident(Vec3 center) {
      this.dragon.getLookControl().setLookAt(center.x, center.y, center.z, 30.0F, 90.0F);
      Vec3 sight = center.subtract(this.dragon.getEyePosition());
      float pitch = (float)(-Math.atan2(sight.y, sight.horizontalDistance()) * 180.0F / (float)Math.PI);
      this.dragon.setXRot(Mth.approachDegrees(this.dragon.getXRot(), pitch, 30.0F));
   }

   private boolean tickAirThrow(HumanoidFlight.Phase flight, Player pilot) {
      boolean canThrow = pilot == null
         ? flight == HumanoidFlight.Phase.AIR && this.dragon.getDragonStage() >= 4 && this.dragon.getCommand() != 1 && !this.dragon.isOrderedToSit()
         : !HumanoidFlight.groundedForMelee(this.dragon) && !this.dragon.isInWaterOrBubble() && !this.dragon.isInLava();
      if (canThrow || pilot != null && this.airThrowStarted >= 0L && !this.dragon.isInWaterOrBubble() && !this.dragon.isInLava()) {
         long now = this.dragon.level().getGameTime();
         BreathProfile profile = BreathProfile.forDragon(this.dragon);
         if (this.airThrowStarted >= 0L) {
            long age = now - this.airThrowStarted;
            if (age >= 0L && age < 32L) {
               if (age <= 13L) {
                  boolean release = age == 13L;
                  if (pilot == null) {
                     if (!this.validThrowTarget(this.airThrowTarget) || !this.tickTargetedThrow(this.airThrowTarget, profile, release)) {
                        this.clearPreparedThrow();
                        return false;
                     }
                  } else {
                     if (!this.holdsPreparedTrident()) {
                        this.clearPreparedThrow();
                        return false;
                     }

                     if (release) {
                        Vec3 start = pilot.getEyePosition();
                        Vec3 aim = this.riderAim(pilot, start, profile.range());
                        LivingEntity target = this.aimedRiderTarget(start, aim, profile.range(), profile, true);
                        this.aimTrident(aim);
                        DragonTridentFlight.Shot shot = target == null ? null : DragonTridentFlight.targetShot(this.dragon, target);
                        if ((shot == null ? DragonTridentFlight.launchAt(this.dragon, aim) : DragonTridentFlight.launchAt(this.dragon, shot)) == null) {
                           this.clearPreparedThrow();
                           return false;
                        }
                     } else {
                        this.dragon.setXRot(pilot.getXRot());
                     }
                  }
               }

               return true;
            } else {
               this.clearPreparedThrow();
               return false;
            }
         } else {
            LivingEntity target = pilot == null ? this.dragon.getTarget() : null;
            ItemStack weapon = DragonHands.availableMainHand(this.dragon);
            if (this.meleeCooldown <= 0
               && this.windupTicks <= 0
               && this.breathTicks <= 0
               && SpecialWeapons.throwableIceTrident(this.dragon, weapon)
               && (
                  pilot == null
                     ? this.validThrowTarget(target) && profile.inRange(this.dragon, target) && !(this.dragon.distanceToSqr(target) < 36.0)
                     : HumanoidFlight.of(this.dragon).wantsStrike()
               )) {
               DragonTridentFlight.Shot shot = pilot == null ? DragonTridentFlight.prepareTargetShot(this.dragon, target) : null;
               if (pilot == null && shot == null) {
                  return false;
               } else {
                  this.airThrowStarted = now;
                  this.airThrowTarget = target;
                  this.prepareTrident(weapon, target, shot);
                  this.meleeCooldown = 32;
                  this.idleLook.reset();
                  HumanoidCombat.stopBreath(this.dragon);
                  if (shot != null) {
                     this.faceTridentTarget(shot.aim());
                  } else {
                     this.dragon.setXRot(pilot.getXRot());
                  }

                  AirTridentThrow.begin(this.dragon, now);
                  return true;
               }
            } else {
               return false;
            }
         }
      } else {
         if (this.airThrowStarted >= 0L) {
            this.clearPreparedThrow();
         }

         return false;
      }
   }

   private boolean validThrowTarget(LivingEntity target) {
      return target != null && HumanoidTargetGoal.isEnemy(this.dragon, target) && GuardEscort.reachableTarget(this.dragon, target);
   }

   private void leaveMountedCombat() {
      if (this.mountedCombat) {
         this.stop(true);
         HumanoidFlight.of(this.dragon).clearRiderInput();
         this.mountedCombat = false;
      }
   }

   public void tickMountedCombat() {
      if (this.dragon.getControllingPassenger() instanceof Player pilot && HumanoidFlight.validRider(this.dragon, pilot)) {
         if (!this.mountedCombat) {
            this.stop(true);
            this.mountedCombat = true;
         }

         this.advanceCooldowns();
         if (this.mountedDirectStrikePoseTicks > 0) {
            this.mountedDirectStrikePoseTicks--;
         }

         BreathAnimationSync.beginTick(this.dragon);

         try {
            HumanoidMeleeTiming previous = this.melee;
            HumanoidFlight input = HumanoidFlight.of(this.dragon);
            boolean grounded = (this.melee != null || input.wantsStrike() || this.airThrowStarted >= 0L) && HumanoidFlight.groundedForMelee(this.dragon);
            if (this.melee != null && !grounded) {
               this.melee = null;
               this.meleeTarget = null;
               DragonForms.state(this.dragon).dragonmaid$setAction(0);
            }

            if (this.tickCommittedStrike(pilot)) {
               return;
            }

            DragonShield shield = DragonShield.of(this.dragon);
            if (this.airThrowStarted < 0L && (shield.riderGuarding() || !input.wantsStrike() || grounded)
               || !(this.dragon instanceof IceDragonEntity)
               || !this.tickAirThrow(HumanoidFlight.phase(this.dragon), pilot)) {
               int action = DragonForms.state(this.dragon).dragonmaid$getAction();
               if (shield.riderGuarding()
                  || input.wantsGuard()
                     && action != 9
                     && this.melee == null
                     && this.mountedDirectStrikePoseTicks == 0
                     && !this.dragon.isNoAi()
                     && !TailSweepSync.isBusy(this.dragon)
                     && HumanoidFlight.emptyHands(pilot)
                     && shield.raiseForRider(this.dragon)) {
                  this.stopRiderBreath(0);
                  return;
               }

               if (action == 9) {
                  if (BladeMotion.age(this.dragon) >= 11) {
                     BladeMotion.begin(this.dragon, 10);
                  }

                  return;
               }

               if (action == 10 && BladeMotion.frame(this.dragon, 0.0F) != null) {
                  return;
               }

               if (input.wantsStrike() && this.meleeCooldown == 0 && grounded) {
                  double reach = DragonCombatAccessories.clawRadius(this.dragon, HumanoidCombat.clawReach(this.dragon));
                  this.beginStrike(this.aimedRiderTarget(pilot, reach));
                  if (this.melee == null) {
                     this.mountedDirectStrikePoseTicks = 6;
                  }

                  this.tickRiderBreath(pilot, true, this.melee != null);
               } else {
                  this.tickRiderBreath(pilot, true, false);
               }

               this.finishUnchainedStrike(previous);
               return;
            }

            this.recoverLightning();
         } finally {
            BreathAnimationSync.flush(this.dragon);
         }

         return;
      }

      this.leaveMountedCombat();
   }

   private void tickRiderBreath(Player pilot, boolean allowed, boolean meleeOverlay) {
      int action = 0;
      if (allowed && HumanoidFlight.of(this.dragon).wantsBreath() && this.breathReady() && (!meleeOverlay || !(this.dragon instanceof LightningDragonEntity))) {
         BreathProfile profile = BreathProfile.forDragon(this.dragon);
         Vec3 start = pilot.getEyePosition();
         Vec3 end = this.riderAim(pilot, start, profile.range());
         LivingEntity target = this.aimedRiderTarget(start, end, profile.range(), profile, false);
         Vec3 aim = target == null ? end : target.getBoundingBox().getCenter();
         this.dragon.setXRot(pilot.getXRot());
         action = this.tickBreath(target, profile, meleeOverlay, aim);
      }

      this.stopRiderBreath(action);
      if (!meleeOverlay) {
         if (action == 0 && this.mountedDirectStrikePoseTicks > 0) {
            action = 15;
         }

         DragonForms.state(this.dragon).dragonmaid$setAction(action);
      }
   }

   private void stopRiderBreath(int action) {
      if (action == 0) {
         if (this.windupTicks > 0 || this.breathTicks > 0 || this.lightningAimTicks > 0) {
            this.cancelBurst();
         }

         this.lightningAimTicks = 0;
      }

      if (action != 2) {
         HumanoidCombat.stopBreath(this.dragon);
         this.recoverLightning();
      }
   }

   private void tickAutonomousFlightCombat(HumanoidFlight.Phase phase) {
      HumanoidFlight flight = HumanoidFlight.of(this.dragon);
      LivingEntity target = this.dragon.getTarget();
      if (phase != HumanoidFlight.Phase.AIR
         || !flight.allowsCombat()
         || target != null && (!HumanoidTargetGoal.isEnemy(this.dragon, target) || !GuardEscort.reachableTarget(this.dragon, target))) {
         if (this.airThrowStarted >= 0L) {
            this.clearPreparedThrow();
         }

         this.tickFlightBreath(phase, null);
         this.idleLook.reset();
      } else if (!(this.dragon instanceof IceDragonEntity) || !this.tickAirThrow(phase, null)) {
         int action = this.tickFlightBreath(phase, target);
         if (target == null) {
            this.idleLook.tick();
         } else {
            this.idleLook.reset();
            if (action == 0) {
               flight.glance(target.getBoundingBox().getCenter(), true);
            }
         }
      }
   }

   private Vec3 riderAim(Player pilot, Vec3 start, double range) {
      Vec3 aim = start.add(pilot.getLookAngle().scale(range));
      return this.dragon.level().clip(new ClipContext(start, aim, Block.COLLIDER, Fluid.NONE, pilot)).getLocation();
   }

   private LivingEntity aimedRiderTarget(Player pilot, double reach) {
      Vec3 start = pilot.getEyePosition();
      return this.aimedRiderTarget(start, this.riderAim(pilot, start, reach), reach, null, false);
   }

   private LivingEntity aimedRiderTarget(Vec3 start, Vec3 end, double reach, BreathProfile profile, boolean trident) {
      LivingEntity best = null;
      double nearest = start.distanceToSqr(end);

      for (LivingEntity candidate : this.dragon
         .level()
         .getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(1.0), entity -> HumanoidTargetGoal.isEnemy(this.dragon, entity))) {
         Optional<Vec3> hit = candidate.getBoundingBox().inflate(0.35).clip(start, end);
         if (!hit.isEmpty()) {
            double distance = hit.get().distanceToSqr(start);
            if (distance < nearest
               && (
                  profile == null
                     ? this.dragon.distanceToSqr(candidate) <= reach * reach && this.dragon.hasLineOfSight(candidate)
                     : (trident ? profile.inRange(this.dragon, candidate) : profile.canHit(this.dragon, candidate))
               )) {
               best = candidate;
               nearest = distance;
            }
         }
      }

      return best;
   }

   private int tickFlightBreath(HumanoidFlight.Phase flight, LivingEntity target) {
      int action = 0;
      if (flight == HumanoidFlight.Phase.AIR && target != null) {
         BreathProfile profile = BreathProfile.forDragon(this.dragon);
         if (this.breathReady()
            && (!(this.dragon instanceof LightningDragonEntity) || !this.lightningTargetTooClose(target))
            && profile.canHit(this.dragon, target)) {
            action = HumanoidFlight.aimBreath(this.dragon, target.getBoundingBox().getCenter()) ? this.tickBreath(target, profile, false) : 3;
         }
      }

      if (action == 0) {
         if (this.windupTicks > 0 || this.breathTicks > 0 || this.lightningAimTicks > 0) {
            this.cancelBurst();
         }

         this.lightningAimTicks = 0;
      }

      if (action != 2) {
         HumanoidCombat.stopBreath(this.dragon);
         this.recoverLightning();
      }

      DragonForms.state(this.dragon).dragonmaid$setAction(action);
      return action;
   }

   private void beginExclusiveStrike() {
      this.katana.consumeOpening();
      this.idleLook.reset();
      this.home.reset();
      this.melee = null;
      this.meleeTarget = null;
      this.meleeCooldown = 24;
      this.cancelBurst();
      this.lightningAimTicks = 0;
      HumanoidCombat.stopBreath(this.dragon);
      this.recoverLightning();
   }
}
