package com.example.dragonmaid.combat;

import com.example.dragonmaid.DragonMaidConfig;
import com.example.dragonmaid.compat.CompanionRelations;
import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.data.HumanoidLife;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.GorgonEntity;
import java.util.UUID;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

public final class WildDragonCombat {
   private WildDragonCombat() {
   }

   public static double targetRange() {
      return (Double)DragonMaidConfig.WILD_DRAGON_TARGET_RANGE.get();
   }

   private static boolean hasValidOwner(DragonBaseEntity dragon) {
      UUID owner = dragon.getOwnerUUID();
      return owner != null && (owner.getMostSignificantBits() != 0L || owner.getLeastSignificantBits() != 0L);
   }

   public static boolean isHostileWildDragon(DragonBaseEntity dragon, LivingEntity candidate) {
      if (dragon.isTame()
         && hasValidOwner(dragon)
         && candidate instanceof DragonBaseEntity wild
         && wild != dragon
         && wild.level() == dragon.level()
         && wild.isAlive()
         && !wild.isRemoved()
         && !wild.isTame()
         && wild.getOwnerUUID() == null
         && !wild.isModelDead()
         && !GorgonEntity.isStoneMob(wild)
         && !CompanionRelations.isAlly(dragon, wild)) {
         return true;
      }

      return false;
   }

   public static boolean canAcquireTargets(DragonBaseEntity dragon) {
      return !dragon.level().isClientSide
         && dragon.isTame()
         && hasValidOwner(dragon)
         && dragon.isAlive()
         && !dragon.isRemoved()
         && !dragon.isModelDead()
         && !HumanoidLife.isInjured(dragon)
         && !DragonForms.state(dragon).dragonmaid$isRecoveryRest()
         && !BladeCompat.isReturningToRest(DragonForms.state(dragon))
         && dragon.getCommand() != 1
         && !dragon.isOrderedToSit()
         && !dragon.isSleeping()
         && !dragon.isNoAi()
         && !dragon.isPassenger()
         && !GorgonEntity.isStoneMob(dragon);
   }

   public static double distanceToSqr(DragonBaseEntity dragon, LivingEntity origin, LivingEntity target) {
      if (!isHostileWildDragon(dragon, target)) {
         return origin.distanceToSqr(target);
      } else {
         AABB box = target.getBoundingBox();
         double x = Mth.clamp(origin.getX(), box.minX, box.maxX);
         double y = Mth.clamp(origin.getY(), box.minY, box.maxY);
         double z = Mth.clamp(origin.getZ(), box.minZ, box.maxZ);
         return origin.distanceToSqr(x, y, z);
      }
   }

   public static boolean withinTargetRange(DragonBaseEntity dragon, LivingEntity target) {
      double range = isHostileWildDragon(dragon, target) ? targetRange() : 24.0;
      return distanceToSqr(dragon, dragon, target) <= range * range;
   }
}