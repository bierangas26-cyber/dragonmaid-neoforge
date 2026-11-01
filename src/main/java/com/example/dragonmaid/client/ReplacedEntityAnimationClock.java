package com.example.dragonmaid.client;

final class ReplacedEntityAnimationClock {
    private ReplacedEntityAnimationClock() {
    }

    static double tick(double entityTick, float partialTick, boolean libraryInterpolates) {
        return libraryInterpolates ? entityTick : entityTick + (double)partialTick;
    }
}
