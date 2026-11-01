package com.example.dragonmaid.client;

final class BreathFxBudget {
    private long tick = Long.MIN_VALUE;
    private int particles;
    private int rays;
    private int limit;
    private int rayLimit;

    BreathFxBudget() {
    }

    void reset() {
        this.tick = Long.MIN_VALUE;
    }

    void begin(long now, int quality) {
        if (now != this.tick) {
            this.tick = now;
            this.particles = this.rays = 0;
            this.limit = quality == 0 ? 180 : (quality == 1 ? 80 : 28);
            this.rayLimit = quality == 0 ? 64 : (quality == 1 ? 32 : 12);
        }
    }

    boolean particle(boolean impact) {
        if (this.particles >= (impact ? this.limit : this.limit * 2 / 3)) {
            return false;
        } else {
            this.particles++;
            return true;
        }
    }

    boolean available(boolean impact) {
        return this.particles < (impact ? this.limit : this.limit * 2 / 3);
    }

    boolean ray() {
        if (this.rays >= this.rayLimit) {
            return false;
        } else {
            this.rays++;
            return true;
        }
    }

    int particles() {
        return this.particles;
    }

    int remaining(boolean impact) {
        return Math.max(0, (impact ? this.limit : this.limit * 2 / 3) - this.particles);
    }
}
