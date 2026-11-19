package com.example.dragonmaid.animation;

public record TailSweepState(long round, int revision, long startedAt, float yaw, long cancelledAt) {
   public static final double PLAYBACK_SPEED = 1.25;
   public static final double FAST_PLAYBACK_SPEED = 1.5;
   public static final int CLIP_DURATION = 30;
   public static final double DURATION = 24.0;
   public static final int FADE_TICKS = 4;
   public static final int WATERFOWL = 7;
   public static final int THRUST = 9;
   public static final int QUICK_WATERFOWL = 11;
   public static final int WATERFOWL_SLOW_TAKEOFF = 13;
   public static final int MIN_WATERFOWL_FLIGHT = 14;
   public static final int MAX_WATERFOWL_FLIGHT = 24;
   private static final int IMPULSE_WATERFOWL_BASE = 15;
   public static final int WATERFOWL_APPROACH = 18;
   public static final int WATERFOWL_DIVE = 34;
   public static final int QUICK_WATERFOWL_APPROACH = 12;
   public static final int QUICK_WATERFOWL_DIVE = 20;
   private static final int WATERFOWL_TAKEOFF_FRAME = 6;
   private static final int WATERFOWL_START_EXTENSION = 2;
   private static final int WATERFOWL_RECOVERY = 18;
   public static final int THRUST_ATTACK = 7;
   public static final int THRUST_EXIT = 37;
   public static final int THRUST_HIT = 23;
   private static final int CLIP_HIT_START = 13;
   private static final int CLIP_HIT_END = 15;
   public static final int SWING_TICK = (int)Math.ceil(8.8);
   public static final int HIT_START = (int)Math.ceil(10.4);
   public static final int HIT_END = (int)Math.ceil(12.0);

   public static double clipTick(double age) {
      return Math.max(0.0, Math.min(30.0, age * 1.25));
   }

   public int motion() {
      return (this.revision & 1) == 0 ? this.revision - 1 : this.revision;
   }

   public static int waterfowlLeapMotion(int flightTicks) {
      return 15 + 2 * (flightTicks - 14);
   }

   private boolean impulseWaterfowl() {
      return this.motion() >= 15 && this.motion() <= waterfowlLeapMotion(24);
   }

   public boolean armed() {
      return this.motion() == 5 || this.waterfowl();
   }

   public boolean waterfowl() {
      return this.motion() == 7 || this.compactWaterfowl();
   }

   private boolean compactWaterfowl() {
      return this.motion() == 11 || this.motion() == 13 || this.impulseWaterfowl();
   }

   public boolean thrust() {
      return this.motion() == 9;
   }

   public boolean fast() {
      return this.revision == 3 || this.revision == 4;
   }

   public double speed() {
      return !this.armed() && !this.thrust() ? (this.fast() ? 1.5 : 1.25) : 1.0;
   }

   public double duration() {
      if (this.compactWaterfowl()) {
         return (double)(this.diveTick() + 8 + 18);
      } else {
         return switch (this.motion()) {
            case 5 -> 54.0;
            default -> 30.0 / this.speed();
            case 7 -> 82.0;
            case 9 -> 45.0;
         };
      }
   }

   public double sample(double age) {
      double tick = Math.max(0.0, Math.min(this.duration(), age));
      if (!this.compactWaterfowl()) {
         return tick * this.speed();
      } else {
         int diveStart = this.diveTick();
         int takeoff = this.waterfowlTakeoffTick();
         double approach = this.impulseWaterfowl() ? (double)takeoff + (double)(diveStart - takeoff) * 0.55 : (double)(12 + this.startExtension());
         if (tick < (double)takeoff) {
            return tick * 6.0 / (double)takeoff;
         } else if (tick < approach) {
            return 6.0 + (tick - (double)takeoff) * 12.0 / (approach - (double)takeoff);
         } else if (tick < (double)diveStart) {
            return 18.0 + (tick - approach) * 16.0 / ((double)diveStart - approach);
         } else {
            double dive = tick - (double)diveStart;
            return 34.0 + (dive <= 8.0 ? dive : 8.0 + (dive - 8.0) * 40.0 / 18.0);
         }
      }
   }

   private int startExtension() {
      return this.motion() == 13 ? 2 : 0;
   }

   public int waterfowlTakeoffTick() {
      return this.motion() != 13 && !this.impulseWaterfowl() ? 4 : 5;
   }

   public int diveTick() {
      if (this.impulseWaterfowl()) {
         int flightTicks = 14 + (this.motion() - 15) / 2;
         return this.waterfowlTakeoffTick() + flightTicks - 5;
      } else {
         return this.compactWaterfowl() ? 20 + this.startExtension() : 34;
      }
   }

   public int swingTick() {
      return this.waterfowl() ? this.diveTick() + 4 : (this.armed() ? 22 : (int)Math.ceil(11.0 / this.speed()));
   }

   public int hitStart() {
      return this.waterfowl() ? this.diveTick() + 5 : (this.armed() ? 23 : (int)Math.ceil(13.0 / this.speed()));
   }

   public int hitEnd() {
      return this.waterfowl() ? this.diveTick() + 8 : (this.armed() ? 26 : (int)Math.ceil(15.0 / this.speed()));
   }

   public int hitFrameAt(int age) {
      if (this.waterfowl()) {
         return Math.max(0, Math.min(10, (age - this.hitStart()) * 4));
      } else if (this.armed()) {
         return (int)(Math.max(23.0, Math.min(25.5, this.sample((double)age))) * 4.0 - 92.0);
      } else {
         double tick = Math.max(13.0, Math.min(15.0, this.sample((double)age)));
         return (int)((tick - 13.0) * 4.0);
      }
   }

   public double segmentStart(double tick) {
      if (this.waterfowl()) {
         return tick >= 34.0 ? 34.0 : (tick >= 18.0 ? 18.0 : 0.0);
      } else if (this.thrust()) {
         return tick >= 37.0 ? 37.0 : (tick >= 7.0 ? 7.0 : 0.0);
      } else {
         return 0.0;
      }
   }

   public String animation(double segmentStart) {
      if (this.waterfowl()) {
         return "animation.dragon_shared.waterfowl_blade_tail." + switch ((int)segmentStart) {
            case 18 -> "approach";
            case 34 -> "dive";
            default -> "start";
         };
      } else if (this.thrust()) {
         return "animation.dragon_lightning.wing_thrust." + switch ((int)segmentStart) {
            case 7 -> "attack";
            case 37 -> "exit";
            default -> "enter";
         };
      } else {
         return this.armed() ? "animation.dragon_shared.armed_tail_spin" : "animation.dragon_shared.tail_sweep";
      }
   }

   public static int hitFrame(int age) {
      double tick = Math.max(13.0, Math.min(15.0, clipTick((double)age)));
      return (int)((tick - 13.0) * 4.0);
   }

   public boolean running(long now) {
      return this.cancelledAt < 0L && now >= this.startedAt && (double)(now - this.startedAt) < this.duration();
   }

   public TailSweepState cancel(long now) {
      return this.cancelledAt >= 0L ? this : new TailSweepState(this.round, this.revision + 1, this.startedAt, this.yaw, now);
   }
}
