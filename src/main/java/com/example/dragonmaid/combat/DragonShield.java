package com.example.dragonmaid.combat;

import com.example.dragonmaid.accessory.DragonHands;
import com.example.dragonmaid.data.DragonForms;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
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
   private long started;
   private long nextRaise;
   private boolean guarding;
   private boolean starting;

   public static DragonShield of(DragonBaseEntity dragon) {
      return ((DragonShield.Holder)dragon).dragonmaid$shield();
   }

   public static boolean isRaised(DragonBaseEntity dragon) {
      return dragon.isUsingItem()
         && dragon.getUsedItemHand() == InteractionHand.OFF_HAND
         && DragonForms.isHumanoid(dragon)
         && !dragon.isModelDead()
         && DragonHands.accepts(1, DragonHands.offHand(dragon));
   }

   public boolean usesOffhand(DragonBaseEntity dragon) {
      return this.starting
         || (dragon.level().isClientSide || this.guarding) && dragon.isUsingItem() && dragon.getUsedItemHand() == InteractionHand.OFF_HAND && DragonForms.isHumanoid(dragon);
   }

   public boolean guarding() {
      return this.guarding;
   }

   public boolean canDefend(DragonBaseEntity dragon, LivingEntity target, boolean visible, double distance) {
      if (visible
         && !(distance > 8.0)
         && dragon.isTame()
         && !dragon.isNoAi()
         && dragon.isAlive()
         && !dragon.isVehicle()
         && DragonHands.accepts(1, DragonHands.offHand(dragon))
         && DragonForms.state(dragon).dragonmaid$getKnowledgeLevel() >= 2) {
         long now = dragon.level().getGameTime();
         return this.guarding
            ? now - this.started < 18L && isRaised(dragon) && dragon.getUseItem() == DragonHands.offHand(dragon)
            : now >= this.nextRaise && !dragon.isUsingItem() && target.isAlive();
      } else {
         return false;
      }
   }

   public boolean raise(DragonBaseEntity dragon) {
      if (this.guarding) {
         return true;
      } else {
         this.starting = true;

         try {
            dragon.startUsingItem(InteractionHand.OFF_HAND);
         } finally {
            this.starting = false;
         }

         long now = dragon.level().getGameTime();
         if (!isRaised(dragon)) {
            this.nextRaise = now + 40L;
            return false;
         } else {
            this.guarding = true;
            this.started = now;
            return true;
         }
      }
   }

   public void lower(DragonBaseEntity dragon) {
      if (this.guarding) {
         if (dragon.isUsingItem() && dragon.getUsedItemHand() == InteractionHand.OFF_HAND) {
            dragon.stopUsingItem();
         }

         this.guarding = false;
         this.nextRaise = dragon.level().getGameTime() + 40L;
      }
   }

   public static void disable(DragonBaseEntity dragon) {
      DragonShield shield = of(dragon);
      if (!dragon.level().isClientSide && shield.guarding && isRaised(dragon)) {
         shield.lower(dragon);
         shield.nextRaise = dragon.level().getGameTime() + 100L;
         dragon.level().broadcastEntityEvent(dragon, (byte)30);
      }
   }

   public boolean pastWindup(DragonBaseEntity dragon) {
      return this.guarding && dragon.level().getGameTime() - this.started >= 5L;
   }

   public void releaseIfInterrupted(DragonBaseEntity dragon) {
      if (this.guarding) {
         if (!isRaised(dragon)
            || !dragon.isAlive()
            || dragon.isNoAi()
            || dragon.isSleeping()
            || dragon.isPassenger()
            || dragon.isVehicle()
            || dragon.getCommand() == 1
            || dragon.getTarget() == null
            || !dragon.getTarget().isAlive()
            || DragonForms.state(dragon).dragonmaid$getKnowledgeLevel() < 2
            || DragonHands.offHand(dragon) != dragon.getUseItem()
            || dragon.level().getGameTime() - this.started >= 18L) {
            this.lower(dragon);
         }
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
