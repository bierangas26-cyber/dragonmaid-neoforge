package com.example.dragonmaid.combat;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(
   modid = "dragonmaid"
)
public final class TailSweepKnockback {
   static final float ORDINARY = 1.5F;
   static final float ARMED = 1.75F;
   static final float WATERFOWL = 2.0F;
   private static final ThreadLocal<TailSweepKnockback.Attempt> ATTEMPT = new ThreadLocal<>();

   private TailSweepKnockback() {
   }

   public static boolean hurt(LivingEntity victim, DamageSource source, float damage) {
      return hurt(victim, source, damage, 1.5F);
   }

   static boolean hurt(LivingEntity victim, DamageSource source, float damage, float multiplier) {
      TailSweepKnockback.Attempt previous = ATTEMPT.get();
      ATTEMPT.set(new TailSweepKnockback.Attempt(victim, multiplier));

      boolean var5;
      try {
         var5 = victim.hurt(source, damage);
      } finally {
         if (previous == null) {
            ATTEMPT.remove();
         } else {
            ATTEMPT.set(previous);
         }
      }

      return var5;
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void knockback(LivingKnockBackEvent event) {
      TailSweepKnockback.Attempt attempt = ATTEMPT.get();
      if (attempt != null && !attempt.adjusted && event.getEntity() == attempt.victim) {
         attempt.adjusted = true;
         event.setStrength(event.getStrength() * attempt.multiplier);
      }
   }

   private static final class Attempt {
      final LivingEntity victim;
      final float multiplier;
      boolean adjusted;

      Attempt(LivingEntity victim, float multiplier) {
         this.victim = victim;
         this.multiplier = multiplier;
      }
   }
}
