package com.example.dragonmaid.movement;

public final class WaterMotion {
    public static final double MAX_VERTICAL_SPEED = 0.09;
    public static final double SHORE_SPEED = 0.18;

    private WaterMotion() {
    }

    public static double surfaceError(double depth, double eyeHeight) {
        return depth - Math.max(0.2, eyeHeight - 0.25);
    }

    public static double verticalSpeed(double previous, double heightError, boolean leavingWater) {
        double d0 = leavingWater ? 0.18 : 0.09;
        double d1 = previous * 0.5 + heightError * 0.1;
        return Math.max(-d0, Math.min(d0, d1));
    }
}
