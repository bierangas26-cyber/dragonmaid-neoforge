package com.example.dragonmaid.client;

import java.util.Arrays;

final class LocomotionMotion {
    static final double STRIDE_MODEL_UNITS = 12.0;
    static final double WALK_PLAYBACK_MULTIPLIER = 0.6;
    static final double SOFT_WALK_CYCLES_PER_SECOND = 1.1;
    static final double MAX_WALK_CYCLES_PER_SECOND = 1.75;
    static final float MAX_TAIL_TURN_DEGREES = 12.0F;
    static final int BLEND_TICKS = 4;
    private static int nextGeneration;
    private final float[] tail = new float[6];
    private final float[] previousTail = new float[6];
    private int lastTick = Integer.MIN_VALUE;
    private double x;
    private double z;
    private double walkTime;
    private double previousWalkTime;
    private float yaw;
    private float walkWeight;
    private float previousWalkWeight;
    private boolean moving;
    private int generation;

    LocomotionMotion() {
    }

    void update(int tick, double nextX, double nextZ, float nextYaw, float scale, boolean groundMotion) {
        this.update(tick, nextX, nextZ, nextYaw, scale, groundMotion, groundMotion);
    }

    void update(int tick, double nextX, double nextZ, float nextYaw, float scale, boolean groundMotion, boolean tailMotion) {
        if (tick != this.lastTick) {
            int i = tick - this.lastTick;
            double d0 = Math.hypot(nextX - this.x, nextZ - this.z);
            if (this.lastTick != Integer.MIN_VALUE && i > 0 && i <= 5 && !(d0 > 1.5 * (double)i) && Double.isFinite(d0)) {
                this.previousWalkTime = this.walkTime;
                this.previousWalkWeight = this.walkWeight;
                System.arraycopy(this.tail, 0, this.previousTail, 0, this.tail.length);
                double d1 = Math.max(0.1, (double)scale);
                double d2 = d0 / (double)i / d1;
                this.moving = groundMotion && d2 > (this.moving ? 0.004 : 0.01);
                this.walkWeight = approach(this.walkWeight, this.moving ? 1.0F : 0.0F, (float)i / 4.0F);
                if (this.moving) {
                    double d3 = d2 / 0.75 * 20.0 * 0.6;
                    if (d3 > 1.1) {
                        double d4 = d3 - 1.1;
                        double d5 = 0.6499999999999999;
                        d3 = 1.1 + d5 * d4 / (d5 + d4);
                    }

                    this.walkTime += d3 * (double)i;
                }

                float f = clamp(wrapDegrees(nextYaw - this.yaw) / (float)i, -30.0F, 30.0F);

                for (int j = 0; j < i; j++) {
                    for (int k = this.tail.length - 1; k > 0; k--) {
                        this.tail[k] = this.tail[k] + ((tailMotion ? this.tail[k - 1] * 0.82F : 0.0F) - this.tail[k]) * 0.24F;
                    }

                    this.tail[0] = tailMotion ? clamp(this.tail[0] * 0.9F + f * 0.85F, -12.0F, 12.0F) : this.tail[0] * 0.6F;
                }

                this.lastTick = tick;
                this.x = nextX;
                this.z = nextZ;
                this.yaw = nextYaw;
            } else {
                this.lastTick = tick;
                this.x = nextX;
                this.z = nextZ;
                this.yaw = nextYaw;
                this.moving = false;
                this.walkTime = this.previousWalkTime = 0.0;
                this.walkWeight = this.previousWalkWeight = 0.0F;
                Arrays.fill(this.tail, 0.0F);
                Arrays.fill(this.previousTail, 0.0F);
                this.generation = ++nextGeneration;
            }
        }
    }

    void restartGaitAfterAction() {
        this.moving = false;
        this.walkTime = this.previousWalkTime = 0.0;
        this.walkWeight = this.previousWalkWeight = 0.0F;
    }

    float weight(float partial) {
        return lerp(this.previousWalkWeight, this.walkWeight, partial);
    }

    double walkTick(float partial) {
        return this.previousWalkTime + (this.walkTime - this.previousWalkTime) * (double)clamp(partial, 0.0F, 1.0F);
    }

    float tailRadians(int segment, float partial) {
        return (float)Math.toRadians((double)lerp(this.previousTail[segment], this.tail[segment], partial));
    }

    int generation() {
        return this.generation;
    }

    private static float approach(float value, float target, float step) {
        return value < target ? Math.min(target, value + step) : Math.max(target, value - step);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * clamp(t, 0.0F, 1.0F);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float wrapDegrees(float value) {
        value %= 360.0F;
        return value >= 180.0F ? value - 360.0F : (value < -180.0F ? value + 360.0F : value);
    }
}
