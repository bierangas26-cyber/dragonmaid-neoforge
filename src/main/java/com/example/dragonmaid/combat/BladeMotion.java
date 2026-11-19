package com.example.dragonmaid.combat;

import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.data.DragonState;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;

public final class BladeMotion {
   private static final int TIME_MASK = 2097151;
   public static final int LONG_GUARD = 7;
   public static final int LONG_IMPACT = 8;
   public static final int LONG_RIPOSTE = 9;
   public static final int LONG_EXIT = 10;
   private static final int LONGSWORD_ENTER_TICKS = 5;
   public static final int RIPOSTE_TICKS = 11;
   public static final int KATANA_CHARGE = 11;
   public static final int KATANA_RELEASE = 12;
   public static final int KATANA_CANCEL = 13;
   static final int KATANA_WINDUP = 5;
   static final int KATANA_HIT = 5;
   static final int KATANA_RELEASE_TICKS = 18;
   static final int KATANA_FINISH = 31;

   private BladeMotion() {
   }

   public static void begin(DragonBaseEntity dragon, int action) {
      BladeCompat.setBladeMotion(DragonForms.state(dragon), action, (int)dragon.level().getGameTime() & 2097151);
   }

   public static boolean isGuard(DragonBaseEntity dragon) {
      int action = DragonForms.state(dragon).dragonmaid$getAction();
      return action >= 7 && action <= 9;
   }

   public static BladeMotion.Frame frame(DragonBaseEntity dragon, float partialTick) {
      DragonState state = DragonForms.state(dragon);
      int action = state.dragonmaid$getAction();
      if (action >= 7 && action <= 13) {
         double t = (double)((float)age(dragon) + partialTick);

         return switch (action) {
            case 7 -> t < 5.0 ? once("longsword_enter", t) : loop("longsword_hold", t - 5.0);
            case 8 -> t < 6.0 ? once("longsword_impact", t) : loop("longsword_hold", t - 6.0);
            case 9 -> t < 11.0 ? once("longsword_riposte", t) : loop("longsword_hold", t - 11.0);
            case 10 -> t < 7.0 ? once("longsword_exit", t) : null;
            case 11 -> t < 5.0 ? once("katana_charge", 55.0 + t) : loop("katana_ready", t - 5.0);
            case 12 -> t < 18.0 ? once("katana_release", t) : (t < 31.0 ? once("katana_recover", t - 18.0) : null);
            case 13 -> t < 9.0 ? once("katana_cancel", t) : null;
            default -> null;
         };
      } else {
         return null;
      }
   }

   public static int age(DragonBaseEntity dragon) {
      DragonState state = DragonForms.state(dragon);
      int elapsed = (int)dragon.level().getGameTime() - state.dragonmaid$getMeleeMotion() & 2097151;
      return elapsed > 1048575 ? 0 : elapsed;
   }

   private static BladeMotion.Frame once(String clip, double t) {
      return new BladeMotion.Frame("animation.dragon_special." + clip, t, false);
   }

   private static BladeMotion.Frame loop(String clip, double t) {
      return new BladeMotion.Frame("animation.dragon_special." + clip, t, true);
   }

   public static record Frame(String clip, double ticks, boolean loop) {
   }
}