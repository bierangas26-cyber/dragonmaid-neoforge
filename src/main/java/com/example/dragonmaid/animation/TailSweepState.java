package com.example.dragonmaid.animation;

public record TailSweepState(long round, int revision, long startedAt, float yaw, long cancelledAt) {
    public static final double PLAYBACK_SPEED = 1.25;
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

    public static int hitFrame(int age) {
        double d0 = Math.max(13.0, Math.min(15.0, clipTick((double)age)));
        return (int)((d0 - 13.0) * 4.0);
    }

    public boolean running(long now) {
        return this.cancelledAt < 0L && now >= this.startedAt && (double)(now - this.startedAt) < 24.0;
    }

    public TailSweepState cancel(long now) {
        return this.cancelledAt >= 0L ? this : new TailSweepState(this.round, this.revision + 1, this.startedAt, this.yaw, now);
    }
}
