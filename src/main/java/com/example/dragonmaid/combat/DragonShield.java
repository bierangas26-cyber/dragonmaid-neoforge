package com.example.dragonmaid.combat;

import com.example.dragonmaid.accessory.DragonHands;
import com.example.dragonmaid.data.DragonForms;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.LightningDragonEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Start;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(
   modid = "dragonmaid"
)
public final class DragonShield {
   private static final int HOLD = 18;
   private static final int RECOVER = 40;
   private static final int LONGSWORD_FOLLOW_THROUGH = 3;
   private static final int SHIELD_WINDUP = 5;
   private static final int RANGED_WINDOW = 60;
   private long started;
   private long nextRaise;
   private long shieldDisabledUntil;
   private long longswordRecoveryEndsAt;
   private long lastRangedHit = -60L;
   private long rangedPressureUntil;
   private long nextRangedRaise;
   private boolean guarding;
   private boolean starting;
   private boolean rangedGuard;
   private boolean riderGuard;
   private InteractionHand guardHand = InteractionHand.OFF_HAND;

   public static DragonShield of(DragonBaseEntity dragon) {
      return ((DragonShield.Holder)dragon).dragonmaid$shield();
   }

   public static boolean hasGuardEquipment(DragonBaseEntity dragon) {
      return DragonHands.accepts(1, DragonHands.offHand(dragon))
         || dragon instanceof LightningDragonEntity && SpecialWeapons.isLongsword(BladeCompat.availableMainHand(dragon));
   }

   public static boolean isRaised(DragonBaseEntity dragon) {
      return dragon.isUsingItem()
         && dragon.getUsedItemHand() == InteractionHand.OFF_HAND
         && DragonForms.isHumanoid(dragon)
         && !dragon.isModelDead()
         && DragonHands.accepts(1, DragonHands.offHand(dragon));
   }

   public boolean usesOffhand(DragonBaseEntity dragon) {
      return this.starting && this.guardHand == InteractionHand.OFF_HAND
         || (dragon.level().isClientSide || this.guarding) && dragon.isUsingItem() && dragon.getUsedItemHand() == InteractionHand.OFF_HAND && DragonForms.isHumanoid(dragon);
   }

   public boolean usesMainHand(DragonBaseEntity dragon) {
      return this.starting && this.guardHand == InteractionHand.MAIN_HAND
         || (dragon.level().isClientSide || this.guarding)
            && dragon.isUsingItem()
            && dragon.getUsedItemHand() == InteractionHand.MAIN_HAND
            && SpecialWeapons.isLongsword(BladeCompat.availableMainHand(dragon))
            && DragonForms.isHumanoid(dragon);
   }

   public boolean longswordRaised(DragonBaseEntity dragon) {
      return this.guarding
         && this.guardHand == InteractionHand.MAIN_HAND
         && this.usesMainHand(dragon)
         && !this.longswordRecovering(dragon)
         && dragon.getUseItem() == BladeCompat.availableMainHand(dragon)
         && SpecialWeapons.canFallbackToLongsword(dragon, this.hasUsableShield(dragon));
   }

   boolean longswordRecovering(DragonBaseEntity dragon) {
      return dragon.level().getGameTime() < this.longswordRecoveryEndsAt;
   }

   void beginLongswordRecovery(DragonBaseEntity dragon, int ticks) {
      if (ticks > 0) {
         this.longswordRecoveryEndsAt = dragon.level().getGameTime() + (long)ticks;
         this.lower(dragon);
      }
   }

   private boolean hasUsableShield(DragonBaseEntity dragon) {
      return dragon.level().getGameTime() >= this.shieldDisabledUntil && DragonHands.accepts(1, DragonHands.offHand(dragon));
   }

   private boolean validUse(DragonBaseEntity dragon) {
      return this.guardHand == InteractionHand.MAIN_HAND ? this.longswordRaised(dragon) : isRaised(dragon) && DragonHands.offHand(dragon) == dragon.getUseItem();
   }

   private int holdTicks(DragonBaseEntity dragon) {
      return this.guardHand == InteractionHand.OFF_HAND ? 18 : LongswordParry.windupTicks() + LongswordParry.windowTicks(dragon.getUseItem()) + 3;
   }

   public boolean guarding() {
      return this.guarding;
   }

   public boolean riderGuarding() {
      return this.guarding && this.riderGuard;
   }

   public boolean autonomousLongswordGuard() {
      return this.guarding && !this.riderGuard && this.guardHand == InteractionHand.MAIN_HAND;
   }

   boolean raiseForRider(DragonBaseEntity dragon) {
      boolean shield = this.hasUsableShield(dragon);
      if (!dragon.isUsingItem()
         && BladeCompat.unlocked(DragonForms.state(dragon).dragonmaid$getKnowledgeLevel())
         && (shield || SpecialWeapons.canFallbackToLongsword(dragon, false))) {
         this.riderGuard = this.raise(dragon, shield ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
         return this.riderGuard;
      } else {
         return false;
      }
   }

   private boolean validRiderGuard(DragonBaseEntity dragon) {
      if (dragon.getControllingPassenger() instanceof Player rider
         && BladeCompat.flightValidRider(dragon, rider)
         && BladeCompat.flightEmptyHands(rider)
         && BladeCompat.flightWantsGuard(dragon)) {
         return true;
      }

      return false;
   }

   void recordRangedHit(long now) {
      if (now - this.lastRangedHit < 60L) {
         this.rangedPressureUntil = now + 60L;
      }

      this.lastRangedHit = now;
   }

   void forgetRangedPressure() {
      this.lastRangedHit = -60L;
      this.rangedPressureUntil = 0L;
   }

   boolean defendWhileApproaching(DragonBaseEntity dragon, double distance, double meleeReach) {
      long now = dragon.level().getGameTime();
      if (!(distance <= meleeReach)
         && now < this.rangedPressureUntil
         && SpecialWeapons.wingGuard(dragon)
         && SpecialWeapons.canFallbackToLongsword(dragon, this.hasUsableShield(dragon))
         && !this.longswordRecovering(dragon)
         && !dragon.isVehicle()) {
         if (this.guarding) {
            if (!this.longswordRaised(dragon)) {
               return false;
            } else {
               this.rangedGuard = true;
               return true;
            }
         } else if (now >= this.nextRangedRaise && !dragon.isUsingItem()) {
            this.rangedGuard = this.raise(dragon, InteractionHand.MAIN_HAND);
            if (!this.rangedGuard) {
               this.nextRangedRaise = now + 40L;
            }

            return this.rangedGuard;
         } else {
            return false;
         }
      } else {
         if (this.rangedGuard) {
            this.lower(dragon);
         }

         return false;
      }
   }

   public boolean canDefend(DragonBaseEntity dragon, LivingEntity target, boolean visible, double distance) {
      if (visible
         && !(distance > 8.0)
         && dragon.isTame()
         && !dragon.isNoAi()
         && dragon.isAlive()
         && !dragon.isVehicle()
         && (this.hasUsableShield(dragon) || !this.longswordRecovering(dragon) && SpecialWeapons.canFallbackToLongsword(dragon, false))
         && DragonForms.state(dragon).dragonmaid$getKnowledgeLevel() >= 2) {
         long now = dragon.level().getGameTime();
         return this.guarding
            ? this.validUse(dragon) && now - this.started < (long)this.holdTicks(dragon)
            : now >= this.nextRaise && !dragon.isUsingItem() && target.isAlive();
      } else {
         return false;
      }
   }

   public boolean raise(DragonBaseEntity dragon) {
      if (this.guarding) {
         return true;
      } else if (this.raise(dragon, this.hasUsableShield(dragon) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND)) {
         return true;
      } else {
         this.nextRaise = dragon.level().getGameTime() + 40L;
         return false;
      }
   }

   private boolean raise(DragonBaseEntity dragon, InteractionHand hand) {
      if (hand == InteractionHand.MAIN_HAND && this.longswordRecovering(dragon)) {
         return false;
      } else {
         this.guardHand = hand;
         this.starting = true;

         try {
            dragon.startUsingItem(this.guardHand);
         } finally {
            this.starting = false;
         }

         long now = dragon.level().getGameTime();
         this.guarding = true;
         if (!this.validUse(dragon)) {
            this.guarding = false;
            return false;
         } else {
            if (this.guardHand == InteractionHand.MAIN_HAND) {
               BladeMotion.begin(dragon, 7);
            }

            this.started = now;
            return true;
         }
      }
   }

   public void lower(DragonBaseEntity dragon) {
      if (this.guarding) {
         if (dragon.isUsingItem() && dragon.getUsedItemHand() == this.guardHand) {
            dragon.stopUsingItem();
         }

         this.guarding = false;
         if (this.guardHand == InteractionHand.MAIN_HAND && BladeMotion.isGuard(dragon)) {
            boolean finishingRiposte = this.riderGuard && DragonForms.state(dragon).dragonmaid$getAction() == 9 && BladeMotion.age(dragon) < 11;
            if (!finishingRiposte) {
               BladeMotion.begin(dragon, 10);
            }
         }

         if (!this.rangedGuard && !this.riderGuard) {
            this.nextRaise = dragon.level().getGameTime() + 40L;
         }

         this.rangedGuard = this.riderGuard = false;
      }
   }

   public static void disable(DragonBaseEntity dragon) {
      disable(dragon, 100);
   }

   public static void disable(DragonBaseEntity dragon, int ticks) {
      DragonShield shield = of(dragon);
      if (!dragon.level().isClientSide && shield.guarding && isRaised(dragon)) {
         shield.lower(dragon);
         long now = dragon.level().getGameTime();
         shield.shieldDisabledUntil = now + (long)ticks;
         shield.nextRaise = now;
         dragon.level().broadcastEntityEvent(dragon, (byte)30);
      }
   }

   public boolean pastWindup(DragonBaseEntity dragon) {
      return this.guarding && dragon.level().getGameTime() - this.started >= 5L;
   }

   public void releaseIfInterrupted(DragonBaseEntity dragon) {
      if (!dragon.level().isClientSide && this.guarding) {
         if (!this.validUse(dragon)
            || !dragon.isAlive()
            || dragon.isNoAi()
            || dragon.isSleeping()
            || dragon.isPassenger()
            || !BladeCompat.unlocked(DragonForms.state(dragon).dragonmaid$getKnowledgeLevel())
            || (this.riderGuard ? !this.validRiderGuard(dragon) : !this.validAutonomousGuard(dragon))) {
            this.lower(dragon);
         }
      }
   }

   private boolean validAutonomousGuard(DragonBaseEntity dragon) {
      if (!dragon.isVehicle() && dragon.getCommand() != 1 && dragon.getTarget() != null && dragon.getTarget().isAlive()) {
         long now = dragon.level().getGameTime();
         return this.rangedGuard ? now < this.rangedPressureUntil && SpecialWeapons.wingGuard(dragon) : now - this.started < (long)this.holdTicks(dragon);
      } else {
         return false;
      }
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public static void startTaggedShield(Start event) {
      if (event.getDuration() == 0
         && event.getEntity() instanceof DragonBaseEntity dragon
         && of(dragon).starting
         && event.getItem() == DragonHands.offHand(dragon)) {
         event.setDuration(72000);
      }
   }

   public interface Holder {
      DragonShield dragonmaid$shield();
   }
}