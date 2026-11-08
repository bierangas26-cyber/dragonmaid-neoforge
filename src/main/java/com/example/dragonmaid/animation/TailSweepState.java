package com.example.dragonmaid.animation;

public record TailSweepState(long round, int revision, long startedAt, float yaw, long cancelledAt) {
   public static final double PLAYBACK_SPEED = 1.25;
   public static final double FAST_PLAYBACK_SPEED = 1.5;
   public static final int CLIP_DURATION = 30;
   public static final double DURATION = 24.0;
   public static final int FADE_TICKS = 4;
   private static final int CLIP_HIT_START = 13;
   private static final int CLIP_HIT_END = 15;
   public static final int SWING_TICK = (int)Math.ceil(8.8);
   public static final int HIT_START = (int)Math.ceil(10.4);
   public static final int HIT_END = (int)Math.ceil(12.0);

   public static double clipTick(double age) {
      return Math.max(0.0, Math.min(30.0, age * 1.25));
   }

   public boolean fast() {
      return this.revision >= 3;
   }

   public double speed() {
      return this.fast() ? 1.5 : 1.25;
   }

   public double duration() {
      return 30.0 / this.speed();
   }

   public double sample(double age) {
      return Math.max(0.0, Math.min(30.0, age * this.speed()));
   }

   public int swingTick() {
      return (int)Math.ceil(11.0 / this.speed());
   }

   public int hitStart() {
      return (int)Math.ceil(13.0 / this.speed());
   }

   public int hitEnd() {
      return (int)Math.ceil(15.0 / this.speed());
   }

   public int hitFrameAt(int age) {
      double tick = Math.max(13.0, Math.min(15.0, this.sample((double)age)));
      return (int)((tick - 13.0) * 4.0);
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
