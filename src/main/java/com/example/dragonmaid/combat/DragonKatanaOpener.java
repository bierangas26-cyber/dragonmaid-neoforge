package com.example.dragonmaid.combat;

import com.example.dragonmaid.accessory.DragonHands;
import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.data.DragonState;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.LightningDragonEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

final class DragonKatanaOpener {
   private static final double LEAVE_MARGIN = 0.5;
   private static final int APPROACH_TICKS = 12;
   private final DragonBaseEntity dragon;
   private boolean close;
   private boolean opening;
   private long leftAt = -1L;
   private long approachUntil = -1L;
   private long nextPath;
   private float charge;
   private float committedCharge;
   private LivingEntity target;
   private ItemStack weapon = ItemStack.EMPTY;
   private long started;

   DragonKatanaOpener(DragonBaseEntity dragon) {
      this.dragon = dragon;
   }

   void observe(LivingEntity enemy) {
      if (!(this.dragon instanceof LightningDragonEntity)
         || !SpecialWeapons.trained(this.dragon)
         || this.dragon.getCommand() == 1
         || this.dragon.isSleeping()
         || this.dragon.isPassenger()
         || this.dragon.isModelDead()) {
         this.leave();
      } else if (enemy != null && enemy.isAlive() && enemy.level() == this.dragon.level()) {
         double reach = DragonCombatAccessories.clawRadius(this.dragon, HumanoidCombat.clawReach(this.dragon));
         double distance = (double)this.dragon.distanceTo(enemy);
         if (this.close) {
            if (distance > reach + 0.5) {
               this.leave();
            }
         } else if (distance <= reach) {
            long now = this.dragon.level().getGameTime();
            this.charge = this.leftAt < 0L ? 1.0F : KatanaCharge.progress(now - this.leftAt);
            this.close = this.opening = true;
         }
      } else {
         this.leave();
      }
   }

   void leave() {
      if (this.close) {
         this.close = false;
         this.opening = false;
         this.approachUntil = -1L;
         this.leftAt = this.dragon.level().getGameTime();
      }
   }

   void consumeOpening() {
      this.opening = false;
      this.approachUntil = -1L;
   }

   boolean tryStart(LivingEntity enemy, boolean visible, double distance) {
      if (this.opening && visible && this.dragon.onGround() && HumanoidTailSweep.canAct(this.dragon) && !HumanoidRoar.isRoaring(this.dragon)) {
         ItemStack held = BladeCompat.availableMainHand(this.dragon);
         if (!SpecialWeapons.isKatana(held)) {
            return false;
         } else {
            long now = this.dragon.level().getGameTime();
            if (distance > KatanaCharge.radius(this.charge)) {
               if (this.approachUntil < 0L) {
                  this.approachUntil = now + 12L;
                  this.nextPath = now;
               }

               if (now >= this.approachUntil) {
                  this.consumeOpening();
                  return false;
               } else {
                  if (now >= this.nextPath) {
                     this.nextPath = now + 6L;
                     this.dragon.getNavigation().moveTo(enemy, 0.85);
                  }

                  return true;
               }
            } else {
               this.consumeOpening();
               this.started = now;
               this.committedCharge = this.charge;
               this.weapon = held;
               this.target = enemy;
               this.dragon.getNavigation().stop();
               BladeMotion.begin(this.dragon, 11);
               int chargeTicks = Math.round(this.committedCharge * 60.0F);
               BladeCombat.showChargeParticles(this.dragon, (long)chargeTicks);
               BladeCombat.playChargeCue(this.dragon, KatanaCharge.stage((long)chargeTicks));
               return true;
            }
         }
      } else {
         return false;
      }
   }

   boolean tick() {
      if (this.target == null) {
         return false;
      } else {
         long drawTick = this.dragon.level().getGameTime() - this.started - 5L;
         if (drawTick >= 31L) {
            this.clear();
            return false;
         } else if (HumanoidTailSweep.canAct(this.dragon) && SpecialWeapons.trained(this.dragon) && BladeCompat.availableMainHand(this.dragon) == this.weapon) {
            boolean canAim = this.dragon.getTarget() == this.target
               && HumanoidTargetGoal.isEnemy(this.dragon, this.target)
               && GuardEscort.reachableTarget(this.dragon, this.target);
            if (!canAim && drawTick < 0L) {
               this.cancel();
               return false;
            } else {
               if (canAim && drawTick <= 5L) {
                  this.dragon.getLookControl().setLookAt(this.target, 30.0F, 30.0F);
               }

               if (drawTick == 0L) {
                  BladeMotion.begin(this.dragon, 12);
               }

               if (drawTick == 5L && canAim) {
                  BladeCombat.draw(this.dragon, this.weapon, this.committedCharge);
               }

               return true;
            }
         } else {
            this.cancel();
            return false;
         }
      }
   }

   void cancel() {
      if (this.target != null) {
         this.clear();
         BladeMotion.begin(this.dragon, 13);
      }
   }

   private void clear() {
      this.target = null;
      this.weapon = ItemStack.EMPTY;
      DragonState state = DragonForms.state(this.dragon);
      int action = state.dragonmaid$getAction();
      if (action == 11 || action == 12) {
         state.dragonmaid$setAction(0);
      }
   }
}
