package com.example.dragonmaid.combat;

import com.example.dragonmaid.data.DragonForms;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.minecraft.util.Mth;

public final class AirTridentThrow {
   public static final int ACTION = 14;
   public static final int RELEASE_TICK = 13;
   public static final int DURATION = 32;
   public static final String CLIP = "animation.dragon_ice.trident.air_throw";
   private static final int CLOCK_MASK = 2097151;

   private AirTridentThrow() {
   }

   static void begin(DragonBaseEntity dragon, long time) {
      BladeCompat.setBladeMotion(DragonForms.state(dragon), 14, (int)time & 2097151);
   }

   public static boolean playing(DragonBaseEntity dragon) {
      return DragonForms.state(dragon).dragonmaid$getAction() == 14;
   }

   public static double age(DragonBaseEntity dragon, float partial) {
      int elapsed = (int)dragon.level().getGameTime() - DragonForms.state(dragon).dragonmaid$getMeleeMotion() & 2097151;
      return (double)Mth.clamp((float)(elapsed > 1048575 ? 0 : elapsed) + partial, 0.0F, 32.0F);
   }

   public static float weight(double age) {
      float edge = Mth.clamp((float)Math.min(age, 32.0 - age) / 3.0F, 0.0F, 1.0F);
      return edge * edge * (3.0F - 2.0F * edge);
   }
}