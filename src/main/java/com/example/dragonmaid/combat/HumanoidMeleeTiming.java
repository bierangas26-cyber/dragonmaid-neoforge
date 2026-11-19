package com.example.dragonmaid.combat;

public record HumanoidMeleeTiming(int strike, boolean sword, boolean fast, boolean opening, boolean exiting, long start, boolean axe, boolean trident) {
   private static final int CLOCK_PERIOD = 32768;
   private static final int OPEN = 4;
   private static final int EXIT = 8;
   private static final int SWORD = 16;
   private static final int FAST = 32;
   private static final int AXE_FIRST = 4;
   private static final int AXE_SECOND = 8;
   private static final int AXE_OPEN = 12;
   private static final int AXE_EXIT_FIRST = 16;
   private static final int AXE_EXIT_SECOND = 20;
   private static final int AXE_DURATION = 31;
   private static final int BLEND_TICKS = 6;
   private static final int TRIDENT_OPEN = 24;
   private static final int TRIDENT_THROW = 28;
   private static final int TRIDENT_ENTER_TICKS = 8;
   private static final int TRIDENT_EXIT_TICKS = 9;
   public static final int TRIDENT_RELEASE_TICK = 13;
   private static final int QUICK_TRIDENT_RELEASE_TICK = 10;
   private static final int QUICK_TRIDENT_DURATION = 22;
   private static final int TRIDENT_THROW_DURATION = 27;

   public HumanoidMeleeTiming(int strike, boolean sword, boolean fast, boolean opening, boolean exiting, long start) {
      this(strike, sword, fast, opening, exiting, start, false, false);
   }

   public static HumanoidMeleeTiming begin(HumanoidMeleeTiming previous, boolean sword, boolean fast, long now) {
      return begin(previous, sword, false, fast, now);
   }

   public static HumanoidMeleeTiming begin(HumanoidMeleeTiming previous, boolean sword, boolean axe, boolean fast, long now) {
      return begin(previous, sword, axe, false, fast, now);
   }

   static HumanoidMeleeTiming begin(HumanoidMeleeTiming previous, boolean sword, boolean axe, boolean trident, boolean fast, long now) {
      boolean chain = previous != null
         && !previous.exiting
         && !previous.throwingTrident()
         && previous.sword == sword
         && previous.axe == axe
         && previous.trident == trident;
      int next = chain ? previous.strike % (axe ? 2 : 3) + 1 : 1;
      return new HumanoidMeleeTiming(next, sword, fast, !chain, false, now, axe, trident);
   }

   static HumanoidMeleeTiming tridentThrow(long now) {
      return new HumanoidMeleeTiming(0, true, true, false, false, now, false, true);
   }

   public boolean throwingTrident() {
      return this.trident && this.strike == 0;
   }

   public HumanoidMeleeTiming exit(long now) {
      return new HumanoidMeleeTiming(this.strike, this.sword, this.fast, false, true, now, this.axe, this.trident);
   }

   public int encode() {
      int phase;
      if (this.trident) {
         phase = this.throwingTrident() ? 28 : (this.opening ? 24 : this.strike | 4 | 8 | (this.exiting ? 16 : 0));
      } else if (this.axe) {
         phase = this.opening ? 12 : (this.exiting ? (this.strike == 1 ? 16 : 20) : (this.strike == 1 ? 4 : 8));
      } else {
         phase = this.strike | (this.opening ? 4 : 0) | (this.exiting ? 8 : 0) | (this.sword ? 16 : 0);
      }

      return ((int)this.start & 32767) << 6 | phase | (this.fast ? 32 : 0);
   }

   public static HumanoidMeleeTiming decode(int bits, long now) {
      int clock = bits >>> 6;
      int elapsed = Math.floorMod((int)now - clock + 16384, 32768) - 16384;
      long start = now - (long)elapsed;
      boolean fast = (bits & 32) != 0;
      if ((bits & 3) != 0) {
         return (bits & 12) == 12
            ? new HumanoidMeleeTiming(bits & 3, true, fast, false, (bits & 16) != 0, start, false, true)
            : new HumanoidMeleeTiming(bits & 3, (bits & 16) != 0, (bits & 32) != 0, (bits & 4) != 0, (bits & 8) != 0, now - (long)elapsed);
      } else {
         int phase = bits & 28;
         if (phase == 24) {
            return new HumanoidMeleeTiming(1, true, fast, true, false, start, false, true);
         } else if (phase == 28) {
            return new HumanoidMeleeTiming(0, true, fast, false, false, start, false, true);
         } else {
            return phase != 0 && phase <= 20
               ? new HumanoidMeleeTiming(phase != 8 && phase != 20 ? 1 : 2, true, fast, phase == 12, phase >= 16, start, true, false)
               : null;
         }
      }
   }

   private int ticks(int authored) {
      return this.fast ? (authored * 5 + 5) / 6 : authored;
   }

   public int hitTick() {
      if (this.throwingTrident()) {
         return this.fast ? 10 : 13;
      } else if (this.trident) {
         return this.ticks((this.opening ? 8 : 0) + (this.strike == 1 ? 9 : 10));
      } else {
         return this.axe
            ? this.ticks((this.opening ? 6 : 0) + (this.strike == 1 ? 16 : 14))
            : this.ticks((this.opening ? 6 : 0) + (this.sword && this.strike == 3 ? 7 : 6));
      }
   }

   public int duration() {
      if (this.throwingTrident()) {
         return this.fast ? 22 : 27;
      } else if (this.trident) {
         int var10001;
         if (this.exiting) {
            var10001 = 9;
         } else {
            var10001 = (this.opening ? 8 : 0) + switch (this.strike) {
               case 1 -> 18;
               case 2 -> 22;
               default -> 24;
            };
         }

         return this.ticks(var10001);
      } else {
         return this.axe ? this.ticks(this.exiting ? 6 : (this.opening ? 6 : 0) + 31) : this.ticks(this.exiting ? 6 : (this.opening ? 6 : 0) + 24);
      }
   }

   public HumanoidMeleeTiming.Frame frame(long now, float partial) {
      double elapsed = (double)Math.max(0.0F, (float)(now - this.start) + partial);
      double age = elapsed * (this.fast ? 1.2 : 1.0);
      if (this.trident) {
         String prefix = "animation.dragon_ice.trident.";
         if (this.throwingTrident()) {
            return new HumanoidMeleeTiming.Frame(prefix + "throw", this.throwFrame(elapsed));
         } else if (this.exiting) {
            return new HumanoidMeleeTiming.Frame(prefix + "melee_exit", age);
         } else {
            return this.opening && age < 8.0
               ? new HumanoidMeleeTiming.Frame(prefix + "melee_enter", age)
               : new HumanoidMeleeTiming.Frame(prefix + "melee_" + this.strike, this.opening ? age - 8.0 : age);
         }
      } else if (this.axe) {
         String clip = this.strike == 1 ? "animation.dragon_special.axe_sample" : "animation.dragon_special.axe_sample2";
         if (this.exiting) {
            return new HumanoidMeleeTiming.Frame(clip, 31.0, (float)(1.0 - age / 6.0));
         } else {
            return this.opening && age < 6.0
               ? new HumanoidMeleeTiming.Frame(clip, 0.0, (float)(age / 6.0))
               : new HumanoidMeleeTiming.Frame(clip, this.opening ? age - 6.0 : age);
         }
      } else {
         String family = this.sword ? "animation.dragon_shared.sword_" : "animation.dragon_shared.claw_";
         if (this.exiting) {
            return new HumanoidMeleeTiming.Frame(this.sword ? family + "exit_" + this.strike : family + "exit", age);
         } else {
            return this.opening && age < 6.0
               ? new HumanoidMeleeTiming.Frame(family + "enter", age)
               : new HumanoidMeleeTiming.Frame(family + this.strike, this.opening ? age - 6.0 : age);
         }
      }
   }

   private double throwFrame(double elapsed) {
      if (!this.fast) {
         return elapsed;
      } else {
         return elapsed <= 10.0 ? elapsed * 13.0 / 10.0 : 13.0 + (elapsed - 10.0) * 14.0 / 12.0;
      }
   }

   public static record Frame(String clip, double tick, float weight) {
      public Frame(String clip, double tick) {
         this(clip, tick, 1.0F);
      }
   }
}
