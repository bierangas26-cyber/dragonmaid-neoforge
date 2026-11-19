package com.example.dragonmaid.combat;

public final class KatanaCharge {
   public static final int FULL_TICKS = 60;
   public static final int STAGE_TICKS = 20;
   private static final double MIN_RADIUS = 3.0;
   private static final double REACH_PER_STAGE = 0.5;
   public static final double ARC_DEGREES = 240.0;
   public static final float FULL_DAMAGE_MULTIPLIER = 2.0F;
   public static final int BASE_DAMAGE_OFFSET = -2;
   public static final float CHARGING_MOVE_SCALE = 0.4F;
   private static final int[] STAGE_COLORS = new int[]{4303615, 11760383, 16748595};

   private KatanaCharge() {
   }

   public static float progress(long ticks) {
      return (float)Math.max(0L, Math.min(ticks, 60L)) / 60.0F;
   }

   public static int stage(long ticks) {
      return (int)(Math.max(0L, Math.min(ticks, 59L)) / 20L);
   }

   public static int stageColor(int stage) {
      return STAGE_COLORS[stage];
   }

   public static double radius(float charge) {
      return 3.0 + (double)stage((long)Math.round(charge * 60.0F)) * 0.5;
   }

   public static float lightArmorMultiplier(float armorPoints) {
      return 1.0F + 0.05F * Math.max(0.0F, 10.0F - Math.max(0.0F, armorPoints));
   }

   public static float fullDamageMultiplier(float armorPoints) {
      return 2.0F * lightArmorMultiplier(armorPoints);
   }
}
