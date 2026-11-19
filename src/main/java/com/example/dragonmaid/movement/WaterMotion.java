package com.example.dragonmaid.movement;

public final class WaterMotion {
    public static final double MAX_VERTICAL_SPEED = 0.09;
    public static final double SHORE_SPEED = 0.18;

    private WaterMotion() {
    }

    public static double surfaceError(double depth, double eyeHeight) {
        return depth - Math.max(0.2, eyeHeight - 0.25);
    }

    /** 1.2.7: client-visible bob so a dragon treading water does not look frozen. */
    public static double surfaceBob(int tick, int entityId) {
        return Math.sin(((double) tick + (double) entityId * 7.0) * 0.1) * 0.06;
    }

    public static double verticalSpeed(double previous, double heightError, boolean leavingWater) {
        double limit = leavingWater ? SHORE_SPEED : MAX_VERTICAL_SPEED;
        double velocity = previous * 0.5 + heightError * 0.1;
        if (leavingWater && heightError > 0.0) {
            // 1.2.7: guarantee enough lift to break the surface when climbing ashore.
            velocity = Math.max(0.16, velocity);
        }

        return Math.max(-limit, Math.min(limit, velocity));
    }
}
