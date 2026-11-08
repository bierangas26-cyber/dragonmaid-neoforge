package com.example.dragonmaid.combat;

import com.example.dragonmaid.accessory.DragonHands;
import com.example.dragonmaid.compat.CompanionRelations;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.util.function.BooleanSupplier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(
   modid = "dragonmaid"
)
public final class TailShieldBreak {
   private static final ThreadLocal<TailShieldBreak.Attempt> ATTEMPT = new ThreadLocal<>();

   private TailShieldBreak() {
   }

   public static boolean hurt(DragonBaseEntity dragon, LivingEntity target, DamageSource source, BooleanSupplier hit) {
      if (!dragon.level().isClientSide && canBreakShield(dragon, target)) {
         TailShieldBreak.Attempt previous = ATTEMPT.get();
         TailShieldBreak.Attempt attempt = new TailShieldBreak.Attempt(target, source);
         ATTEMPT.set(attempt);

         boolean result;
         try {
            result = hit.getAsBoolean();
         } finally {
            if (previous == null) {
               ATTEMPT.remove();
            } else {
               ATTEMPT.set(previous);
            }
         }

         if (attempt.block != null
            && !attempt.block.isCanceled()
            && attempt.block.getBlockedDamage() > 0.0F
            && target.isBlocking()
            && target.getUseItem() == attempt.shield) {
            if (target instanceof Player player) {
               player.disableShield();
            } else if (target instanceof DragonBaseEntity defendingDragon) {
               DragonShield.disable(defendingDragon);
            }
         }

         return result;
      } else {
         return hit.getAsBoolean();
      }
   }

   private static boolean canBreakShield(DragonBaseEntity attacker, LivingEntity target) {
      if (target instanceof Player player) {
         if (player.isCreative() || player.isSpectator()) {
            return false;
         }

         if (attacker.getOwner() instanceof Player owner && (attacker.getServer() == null || !attacker.getServer().isPvpAllowed() || !owner.canHarmPlayer(player))) {
            return false;
         }
      } else if (!(target instanceof DragonBaseEntity dragon) || !DragonShield.isRaised(dragon)) {
         return false;
      }

      return !CompanionRelations.isAlly(attacker, target);
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST,
      receiveCanceled = true
   )
   public static void onShieldBlock(LivingShieldBlockEvent event) {
      TailShieldBreak.Attempt attempt = ATTEMPT.get();
      if (attempt != null && event.getEntity() == attempt.defender && event.getDamageSource() == attempt.source) {
         ItemStack shield = attempt.defender.getUseItem();
         if (attempt.defender instanceof Player ? shield.canPerformAction(ItemAbilities.SHIELD_BLOCK) : DragonHands.accepts(1, shield)) {
            attempt.block = event;
            attempt.shield = shield;
         }
      }
   }

   private static final class Attempt {
      final LivingEntity defender;
      final DamageSource source;
      LivingShieldBlockEvent block;
      ItemStack shield;

      Attempt(LivingEntity defender, DamageSource source) {
         this.defender = defender;
         this.source = source;
      }
   }
}
