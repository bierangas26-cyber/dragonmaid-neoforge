package com.example.dragonmaid.combat;

import com.example.dragonmaid.accessory.DragonAccessories;
import com.example.dragonmaid.accessory.DragonAccessoryBehavior;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.IceDragonEntity;

public final class NativeTailReach {
   public static final double MULTIPLIER = 1.2;

   private NativeTailReach() {
   }

   public static boolean enhanced(DragonBaseEntity dragon) {
      return dragon.isTame() && dragon instanceof IceDragonEntity && DragonAccessories.hasEffect(dragon, DragonAccessoryBehavior.ICE_TAIL);
   }

   public static double radius(DragonBaseEntity dragon) {
      return ((double)dragon.getRenderSize() * 0.2574 * 2.0 + 2.0) * 1.2;
   }
}
