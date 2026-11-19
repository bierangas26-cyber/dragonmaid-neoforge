package com.example.dragonmaid.client;

final class TridentGrip {
   private static final float[] ENTER = new float[]{0.0F, 0.0F, 0.18F, 6.0F, 0.4F, 10.5F};
   private static final float[] EXIT = new float[]{0.0F, 10.5F, 0.2F, 6.0F, 0.45F, 0.0F};
   private static final float[] DOWN_THRUST = new float[]{
      0.0F, 10.5F, 0.4F, 10.5F, 0.43F, 9.2F, 0.46F, 6.8F, 0.49F, 5.5F, 0.58F, 5.3F, 0.68F, 5.5F, 0.84F, 8.5F, 1.0F, 10.5F, 1.1F, 10.5F
   };
   private static final float[] THROW = new float[]{0.0F, 0.0F, 0.18F, 10.0F, 0.34F, 14.0F, 0.64F, 14.0F};
   private static final float[] AIR_THROW = new float[]{0.0F, 0.0F, 0.18F, 10.0F, 0.36F, 12.5F, 0.64F, 12.5F};

   private TridentGrip() {
   }

   static float offset(String clip, double tick) {
      float seconds = (float)(tick / 20.0);

      return switch (clip) {
         case "animation.dragon_ice.trident.melee_enter" -> sample(ENTER, seconds);
         case "animation.dragon_ice.trident.melee_exit" -> sample(EXIT, seconds);
         case "animation.dragon_ice.trident.melee_2" -> sample(DOWN_THRUST, seconds);
         case "animation.dragon_ice.trident.throw" -> sample(THROW, seconds);
         case "animation.dragon_ice.trident.air_throw" -> sample(AIR_THROW, seconds);
         default -> 10.5F;
      };
   }

   private static float sample(float[] keys, float seconds) {
      int end = keys.length - 2;
      if (seconds <= keys[0]) {
         return keys[1];
      } else {
         for (int i = 0; i < end; i += 2) {
            if (!(seconds > keys[i + 2])) {
               float t = (seconds - keys[i]) / (keys[i + 2] - keys[i]);
               float a = keys[Math.max(1, i - 1)];
               float b = keys[i + 1];
               float c = keys[i + 3];
               float d = keys[Math.min(i + 5, end + 1)];
               return b + 0.5F * t * (c - a + t * (2.0F * a - 5.0F * b + 4.0F * c - d + t * (3.0F * b - a - 3.0F * c + d)));
            }
         }

         return keys[end + 1];
      }
   }
}
