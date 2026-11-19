package com.example.dragonmaid.combat;

import com.example.dragonmaid.accessory.DragonHands;
import com.example.dragonmaid.animation.TailSweepState;
import com.example.dragonmaid.data.DragonForms;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

final class HumanoidWingThrust {
   private static final int DISENGAGE_TICKS = 20;
   private final DragonBaseEntity dragon;
   private final Set<UUID> opened = new HashSet<>();
   private TailSweepState timeline;
   private LivingEntity target;
   private ItemStack weapon;
   private Vec3 heading;
   private int previous;
   private double remaining;
   private long disengagingSince = -1L;

   HumanoidWingThrust(DragonBaseEntity dragon) {
      this.dragon = dragon;
   }

   void refresh() {
      this.opened.clear();
      this.disengagingSince = -1L;
   }

   void observeCombat() {
      if (!this.opened.isEmpty()) {
         LivingEntity enemy = this.dragon.getTarget();
         boolean engaged = enemy != null
            && !this.dragon.isSleeping()
            && this.dragon.getCommand() != 1
            && !this.dragon.isPassenger()
            && !this.dragon.isVehicle()
            && (this.dragon.getCommand() != 2 || GuardEscort.owner(this.dragon) != null)
            && HumanoidTargetGoal.isEnemy(this.dragon, enemy)
            && GuardEscort.reachableTarget(this.dragon, enemy);
         long now = this.dragon.level().getGameTime();
         if (this.disengagingSince >= 0L && now - this.disengagingSince >= 20L) {
            this.refresh();
         } else if (engaged) {
            this.disengagingSince = -1L;
         } else if (this.disengagingSince < 0L) {
            this.disengagingSince = now;
         }
      }
   }

   boolean tryStart(LivingEntity enemy, boolean visible, double distance) {
      if (visible
         && !(distance > 6.0)
         && !this.opened.contains(enemy.getUUID())
         && SpecialWeapons.wingGuard(this.dragon)
         && HumanoidTailSweep.canAct(this.dragon)
         && this.dragon.onGround()
         && !this.dragon.isLeashed()
         && !this.dragon.isVehicle()
         && !HumanoidRoar.isRoaring(this.dragon)) {
         ItemStack main = BladeCompat.availableMainHand(this.dragon);
         if (SpecialWeapons.eligibleForFirstThrust(main) && !GuardEscort.hasChain(this.dragon)) {
            float yaw = HumanoidArmedSweep.face(this.dragon, enemy);
            this.timeline = TailSweepSync.begin(this.dragon, yaw, 9);
            this.target = enemy;
            this.weapon = main;
            this.heading = Vec3.directionFromRotation(0.0F, yaw);
            this.remaining = Math.min(3.3, Math.max(0.0, distance - 1.9));
            this.previous = 0;
            this.opened.add(enemy.getUUID());
            this.hold();
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   boolean tick() {
      if (this.timeline == null) {
         return false;
      } else {
         long elapsed = this.dragon.level().getGameTime() - this.timeline.startedAt();
         if (TailSweepSync.isBusy(this.dragon)
            && elapsed >= (long)this.previous
            && HumanoidTailSweep.canAct(this.dragon)
            && !this.dragon.isLeashed()
            && !this.dragon.isVehicle()
            && SpecialWeapons.wingGuard(this.dragon)
            && (elapsed > 23L || BladeCompat.availableMainHand(this.dragon) == this.weapon)) {
            if (elapsed == (long)this.previous) {
               return true;
            } else {
               this.hold();
               if (elapsed >= 13L && elapsed < 23L && this.validTarget() && this.dragon.onGround()) {
                  Vec3 toward = this.target.position().subtract(this.dragon.position());
                  double step = Math.min(0.36, Math.min(this.remaining, toward.dot(this.heading) - 1.9));
                  if (step > 0.0) {
                     Vec3 delta = this.heading.scale(step);
                     if (HumanoidArmedSweep.supported(this.dragon, this.dragon.position().add(delta), this.dragon.getY())
                        && HumanoidArmedSweep.clear(this.dragon, this.dragon.getBoundingBox().expandTowards(delta).deflate(0.01))) {
                        this.dragon.setDeltaMovement(delta.x, this.dragon.getDeltaMovement().y, delta.z);
                        this.dragon.hasImpulse = true;
                        this.remaining -= step;
                     }
                  }
               }

               if (elapsed == 23L) {
                  this.dragon.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 1.1F);
                  Vec3 toward = this.target.position().subtract(this.dragon.position());
                  double reach = HumanoidCombat.clawReach(this.dragon) + 0.4;
                  double forward = toward.dot(this.heading);
                  if (this.validTarget()
                     && toward.lengthSqr() <= reach * reach
                     && (toward.horizontalDistanceSqr() <= 0.25 || forward >= toward.horizontalDistance() * 0.5)
                     && HumanoidCombat.wingThrust(this.dragon, this.target, this.weapon)) {
                     this.dragon.playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 1.0F);
                  }
               }

               this.previous = (int)elapsed;
               return true;
            }
         } else {
            this.cancel();
            return false;
         }
      }
   }

   private boolean validTarget() {
      return this.target.level() == this.dragon.level()
         && HumanoidTargetGoal.isEnemy(this.dragon, this.target)
         && GuardEscort.reachableTarget(this.dragon, this.target)
         && this.dragon.hasLineOfSight(this.target);
   }

   private void hold() {
      this.dragon.setDeltaMovement(0.0, this.dragon.getDeltaMovement().y, 0.0);
      HumanoidArmedSweep.hold(this.dragon, this.timeline.yaw());
   }

   void cancel() {
      if (this.timeline != null) {
         TailSweepSync.cancel(this.dragon);
         this.timeline = null;
         this.target = null;
         this.weapon = null;
         this.dragon.setDeltaMovement(0.0, this.dragon.getDeltaMovement().y, 0.0);
         if (DragonForms.state(this.dragon).dragonmaid$getAction() == 6) {
            DragonForms.state(this.dragon).dragonmaid$setAction(0);
         }
      }
   }
}
