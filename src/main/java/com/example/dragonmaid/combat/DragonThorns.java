package com.example.dragonmaid.combat;

import com.example.dragonmaid.compat.CompanionRelations;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class DragonThorns {
   private DragonThorns() {
   }

   public static void onHealthLoss(DragonBaseEntity dragon, DamageSource source, float oldHealth) {
      if (!dragon.level().isClientSide
         && !source.is(DamageTypes.THORNS)
         && source.getEntity() instanceof LivingEntity attacker
         && attacker != dragon
         && !attacker.isRemoved()
         && attacker.isAlive()
         && attacker.level() == dragon.level()
         && !CompanionRelations.isAlly(dragon, attacker)) {
         if (attacker instanceof Player player
            && (
               player.isCreative()
                  || player.isSpectator()
                  || dragon.getServer() == null
                  || !dragon.getServer().isPvpAllowed()
                  || dragon.getOwner() instanceof Player owner && !owner.canHarmPlayer(player)
            )) {
            return;
         }

         float lost = oldHealth - dragon.getHealth();
         if (Float.isFinite(lost) && lost > 0.0F) {
            attacker.hurt(dragon.damageSources().thorns(dragon), Math.min(8.0F, lost * 0.25F));
         }
      }
   }
}
