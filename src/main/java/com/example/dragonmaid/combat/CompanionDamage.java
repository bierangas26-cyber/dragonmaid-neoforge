package com.example.dragonmaid.combat;

import com.example.dragonmaid.config.CompanionBalanceConfig;
import com.example.dragonmaid.data.CompanionGrowth;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.registry.IafDamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(
   modid = "dragonmaid"
)
public final class CompanionDamage {
   private CompanionDamage() {
   }

   public static double multiplier(DamageSource source) {
      Entity attacker = source.getEntity();
      if (!(attacker instanceof DragonBaseEntity)) {
         attacker = source.getDirectEntity();
      }

      if (attacker instanceof Projectile projectile) {
         attacker = projectile.getOwner();
      }

      if (attacker instanceof DragonBaseEntity dragon && CompanionGrowth.owned(dragon)) {
         CompanionBalanceConfig.Values config = CompanionBalanceConfig.values();
         if (source.is(IafDamageTypes.DRAGON_FIRE_TYPE)) {
            return config.fireDamage();
         }

         if (source.is(IafDamageTypes.DRAGON_ICE_TYPE)) {
            return config.iceDamage();
         }

         if (source.is(IafDamageTypes.DRAGON_LIGHTNING_TYPE)) {
            return config.lightningDamage();
         }

         if (source.is(DamageTypes.MOB_ATTACK)) {
            return config.meleeMultiplier();
         }

         return 1.0;
      }

      return 1.0;
   }

   @SubscribeEvent(
      priority = EventPriority.HIGH
   )
   public static void hurt(LivingDamageEvent.Pre event) {
      if (!event.getEntity().level().isClientSide && !(event.getNewDamage() <= 0.0F) && Float.isFinite(event.getNewDamage())) {
         double factor = multiplier(event.getSource());
         if (factor != 1.0) {
            event.setNewDamage((float)Math.min(Float.MAX_VALUE, (double)event.getNewDamage() * factor));
         }
      }
   }
}
