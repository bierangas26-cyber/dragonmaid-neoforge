package com.example.dragonmaid.client;

final class SitTimeline {
    private SitTimeline.Phase phase = SitTimeline.Phase.NONE;
    private double started;
    private double last = Double.NaN;
    private int generation;
    private int revision;
    private boolean requested;
    private boolean restoreLoop;

    SitTimeline() {
    }

    void resumeLoop() {
        this.restoreLoop = true;
    }

    SitTimeline.Frame update(double time, boolean seated, boolean interrupt, int nextGeneration, double enterTicks, double loopTicks, double exitTicks) {
        boolean flag = !Double.isFinite(this.last) || time < this.last || time - this.last > 5.0 || this.generation != nextGeneration;
        if (flag) {
            this.restoreLoop = seated;
            this.phase = seated && !interrupt ? SitTimeline.Phase.LOOP : SitTimeline.Phase.NONE;
            this.started = time;
            this.revision++;
        } else if (interrupt) {
            if (this.phase != SitTimeline.Phase.NONE) {
                this.phase = SitTimeline.Phase.NONE;
                this.started = time;
                this.revision++;
            }
        } else if (seated && (!this.requested || this.phase == SitTimeline.Phase.NONE)) {
            this.phase = this.restoreLoop ? SitTimeline.Phase.LOOP : SitTimeline.Phase.ENTER;
            this.started = time;
            this.revision++;
        } else if (!seated && this.requested && this.phase != SitTimeline.Phase.NONE) {
            this.phase = SitTimeline.Phase.EXIT;
            this.started = time;
            this.revision++;
        }

        this.requested = seated && !interrupt;
        if (!seated || this.phase == SitTimeline.Phase.LOOP) {
            this.restoreLoop = false;
        }

        this.generation = nextGeneration;
        this.last = time;
        if (this.phase == SitTimeline.Phase.ENTER && time - this.started >= enterTicks) {
            this.started += enterTicks;
            this.phase = SitTimeline.Phase.LOOP;
            this.revision++;
        }

        if (this.phase == SitTimeline.Phase.EXIT && time - this.started >= exitTicks) {
            this.phase = SitTimeline.Phase.NONE;
            this.started = time;
            this.revision++;
        }

        double d0 = Math.max(0.0, time - this.started);
        if (this.phase == SitTimeline.Phase.LOOP) {
            d0 %= Math.max(1.0, loopTicks);
        }

        return new SitTimeline.Frame(this.phase, d0, this.revision);
    }

    static record Frame(SitTimeline.Phase phase, double tick, int revision) {
        boolean active() {
            return this.phase != SitTimeline.Phase.NONE;
        }
    }

    static enum Phase {
        NONE,
        ENTER,
        LOOP,
        EXIT;

        private Phase() {
        }
    }
}
