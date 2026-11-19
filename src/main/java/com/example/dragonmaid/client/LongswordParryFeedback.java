package com.example.dragonmaid.client;

import net.minecraft.util.Mth;

public final class LongswordParryFeedback {
   private static final int FADE_TICKS = 10;
   private int sequence = -1;
   private int successTick = -1;

   public void start(int useSequence) {
      this.sequence = useSequence;
      this.successTick = -1;
   }

   public boolean succeed(int useSequence, int now) {
      if (!this.spent() && (useSequence < 0 || useSequence == this.sequence)) {
         this.successTick = now;
         return true;
      } else {
         return false;
      }
   }

   public boolean spent() {
      return this.successTick >= 0;
   }

   public float successAge(int now, float partialTick) {
      return (float)(now - this.successTick) + partialTick;
   }

   public static float windowFill(int useTicks, float partialTick, int windupTicks, int windowTicks) {
      float elapsed = (float)useTicks + partialTick;
      if (useTicks < windupTicks) {
         return Mth.clamp(elapsed / (float)windupTicks, 0.0F, 1.0F);
      } else {
         return windowTicks <= 0 ? 0.0F : Mth.clamp(1.0F - (elapsed - (float)windupTicks) / (float)windowTicks, 0.0F, 1.0F);
      }
   }

   public static int fadeAlpha(float age) {
      return Math.round(255.0F * Math.max(0.0F, 1.0F - age / 10.0F));
   }
}
