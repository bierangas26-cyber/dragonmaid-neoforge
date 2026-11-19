package com.example.dragonmaid.combat;

import com.example.dragonmaid.accessory.DragonHands;
import com.example.dragonmaid.compat.CompanionRelations;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.util.function.BooleanSupplier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
   private static final int FIRE_AXE_DISABLE_TICKS = 40;

   private TailShieldBreak() {
   }

   public static boolean hurt(DragonBaseEntity dragon, LivingEntity target, DamageSource source, BooleanSupplier hit) {
      return hurt(dragon, target, source, hit, false);
   }

   public static boolean fireAxeHit(DragonBaseEntity dragon, LivingEntity target, DamageSource source, BooleanSupplier hit) {
      return hurt(dragon, target, source, hit, true);
   }

   private static boolean hurt(DragonBaseEntity dragon, LivingEntity target, DamageSource source, BooleanSupplier hit, boolean fireAxe) {
      if (!dragon.level().isClientSide && canBreakShield(dragon, target)) {
         TailShieldBreak.Attempt previous = ATTEMPT.get();
         TailShieldBreak.Attempt attempt = new TailShieldBreak.Attempt(target, source, fireAxe);
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
            if (fireAxe) {
               disableOrdinaryShield(target);
            } else if (target instanceof Player player) {
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

   private static void disableOrdinaryShield(LivingEntity target) {
      if (target instanceof Player player) {
         player.getCooldowns().addCooldown(Items.SHIELD, 40);
         player.stopUsingItem();
         player.level().broadcastEntityEvent(player, (byte)30);
      } else if (target instanceof DragonBaseEntity dragon) {
         DragonShield.disable(dragon, 40);
      }
   }

   public static boolean handlesAxeDisable(DragonBaseEntity defender, LivingEntity attacker) {
      TailShieldBreak.Attempt attempt = ATTEMPT.get();
      return attempt != null
         && attempt.fireAxe
         && attempt.defender == defender
         && attempt.source.getDirectEntity() == attacker
         && attempt.block != null
         && !attempt.block.isCanceled()
         && attempt.block.getBlockedDamage() > 0.0F
         && defender.getUseItem() == attempt.shield;
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
         if (attempt.fireAxe) {
            if (shield.is(Items.SHIELD)) {
               attempt.block = event;
               attempt.shield = shield;
               if (!event.isCanceled() && event.getBlockedDamage() > 0.0F) {
                  event.setShieldDamage(0.0F);
               }
            }
         } else {
            if (attempt.defender instanceof Player ? shield.canPerformAction(ItemAbilities.SHIELD_BLOCK) : DragonHands.accepts(1, shield)) {
               attempt.block = event;
               attempt.shield = shield;
            }
         }
      }
   }

   private static final class Attempt {
      final LivingEntity defender;
      final DamageSource source;
      final boolean fireAxe;
      LivingShieldBlockEvent block;
      ItemStack shield;

      Attempt(LivingEntity defender, DamageSource source, boolean fireAxe) {
         this.defender = defender;
         this.source = source;
         this.fireAxe = fireAxe;
      }
   }
}