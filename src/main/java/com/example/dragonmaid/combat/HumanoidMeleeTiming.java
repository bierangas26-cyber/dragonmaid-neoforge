package com.example.dragonmaid.combat;

public record HumanoidMeleeTiming(int strike, boolean sword, boolean fast, boolean opening, boolean exiting, long start) {
   private static final int CLOCK_PERIOD = 32768;
   private static final int OPEN = 4;
   private static final int EXIT = 8;
   private static final int SWORD = 16;
   private static final int FAST = 32;

   public static HumanoidMeleeTiming begin(HumanoidMeleeTiming previous, boolean sword, boolean fast, long now) {
      boolean chain = previous != null && !previous.exiting && previous.sword == sword;
      int next = chain ? previous.strike % 3 + 1 : 1;
      return new HumanoidMeleeTiming(next, sword, fast, !chain, false, now);
   }

   public HumanoidMeleeTiming exit(long now) {
      return new HumanoidMeleeTiming(this.strike, this.sword, this.fast, false, true, now);
   }

   public int encode() {
      return ((int)this.start & 32767) << 6 | this.strike | (this.opening ? 4 : 0) | (this.exiting ? 8 : 0) | (this.sword ? 16 : 0) | (this.fast ? 32 : 0);
   }

   public static HumanoidMeleeTiming decode(int bits, long now) {
      if ((bits & 3) == 0) {
         return null;
      } else {
         int clock = bits >>> 6;
         int elapsed = Math.floorMod((int)now - clock + 16384, 32768) - 16384;
         return new HumanoidMeleeTiming(bits & 3, (bits & 16) != 0, (bits & 32) != 0, (bits & 4) != 0, (bits & 8) != 0, now - (long)elapsed);
      }
   }

   private int ticks(int authored) {
      return this.fast ? (authored * 5 + 5) / 6 : authored;
   }

   public int hitTick() {
      return this.ticks((this.opening ? 6 : 0) + (this.sword && this.strike == 3 ? 7 : 6));
   }

   public int duration() {
      return this.ticks(this.exiting ? 6 : (this.opening ? 6 : 0) + 24);
   }

   public HumanoidMeleeTiming.Frame frame(long now, float partial) {
      double age = (double)Math.max(0.0F, (float)(now - this.start) + partial) * (this.fast ? 1.2 : 1.0);
      String family = this.sword ? "animation.dragon_shared.sword_" : "animation.dragon_shared.claw_";
      if (this.exiting) {
         return new HumanoidMeleeTiming.Frame(this.sword ? family + "exit_" + this.strike : family + "exit", age);
      } else {
         return this.opening && age < 6.0
            ? new HumanoidMeleeTiming.Frame(family + "enter", age)
            : new HumanoidMeleeTiming.Frame(family + this.strike, this.opening ? age - 6.0 : age);
      }
   }

   public static record Frame(String clip, double tick) {
   }
}
