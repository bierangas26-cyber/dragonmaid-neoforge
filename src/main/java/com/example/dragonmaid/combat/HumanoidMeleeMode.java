package com.example.dragonmaid.combat;

import java.util.Locale;

public enum HumanoidMeleeMode {
   ANIMATED,
   LEGACY;

   public String translationKey() {
      return "config.dragonmaid.humanoidMeleeMode." + this.name().toLowerCase(Locale.ROOT);
   }

   public static HumanoidMeleeMode[] selectableValues(boolean epicFightLoaded) {
      return values();
   }
}
