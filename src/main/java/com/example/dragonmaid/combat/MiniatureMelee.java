package com.example.dragonmaid.combat;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

public final class MiniatureMelee {
   private MiniatureMelee() {
   }

   public static boolean inReach(DragonBaseEntity dragon, Entity target) {
      return inReach(dragon, target, 0.65);
   }

   public static boolean inReach(DragonBaseEntity dragon, Entity target, double margin) {
      AABB a = dragon.getBoundingBox();
      AABB b = target.getBoundingBox();
      double x = Math.max(0.0, Math.max(a.minX - b.maxX, b.minX - a.maxX));
      double y = Math.max(0.0, Math.max(a.minY - b.maxY, b.minY - a.maxY));
      double z = Math.max(0.0, Math.max(a.minZ - b.maxZ, b.minZ - a.maxZ));
      return x * x + y * y + z * z <= margin * margin;
   }
}
