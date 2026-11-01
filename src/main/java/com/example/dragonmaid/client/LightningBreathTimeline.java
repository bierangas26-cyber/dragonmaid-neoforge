package com.example.dragonmaid.client;

final class LightningBreathTimeline {
    private LightningBreathTimeline.Phase phase = LightningBreathTimeline.Phase.NONE;
    private double started;
    private double last = Double.NaN;
    private int generation;
    private int revision;

    LightningBreathTimeline() {
    }

    LightningBreathTimeline.Frame update(
        double time, boolean breathing, boolean interrupt, int nextGeneration, double startTicks, double loopTicks, double endTicks
    ) {
        boolean flag = !Double.isFinite(this.last) || time < this.last || time - this.last > 5.0 || this.generation != nextGeneration;
        if (flag) {
            this.phase = breathing && !interrupt ? LightningBreathTimeline.Phase.LOOP : LightningBreathTimeline.Phase.NONE;
            this.started = time;
            this.revision++;
        } else if (interrupt) {
            if (this.phase != LightningBreathTimeline.Phase.NONE) {
                this.phase = LightningBreathTimeline.Phase.NONE;
                this.started = time;
                this.revision++;
            }
        } else if (!breathing || this.phase != LightningBreathTimeline.Phase.NONE && this.phase != LightningBreathTimeline.Phase.END) {
            if (!breathing && (this.phase == LightningBreathTimeline.Phase.START || this.phase == LightningBreathTimeline.Phase.LOOP)) {
                this.phase = LightningBreathTimeline.Phase.END;
                this.started = time;
                this.revision++;
            }
        } else {
            this.phase = LightningBreathTimeline.Phase.START;
            this.started = time;
            this.revision++;
        }

        this.generation = nextGeneration;
        this.last = time;
        if (this.phase == LightningBreathTimeline.Phase.START && time - this.started >= startTicks) {
            this.started += startTicks;
            this.phase = LightningBreathTimeline.Phase.LOOP;
        }

        if (this.phase == LightningBreathTimeline.Phase.END && time - this.started >= endTicks) {
            this.phase = LightningBreathTimeline.Phase.NONE;
            this.started = time;
            this.revision++;
        }

        double d0 = Math.max(0.0, time - this.started);
        if (this.phase == LightningBreathTimeline.Phase.LOOP) {
            d0 %= Math.max(1.0, loopTicks);
        }

        return new LightningBreathTimeline.Frame(this.phase, d0, this.revision);
    }

    static record Frame(LightningBreathTimeline.Phase phase, double tick, int revision) {
        boolean active() {
            return this.phase != LightningBreathTimeline.Phase.NONE;
        }
    }

    static enum Phase {
        NONE,
        START,
        LOOP,
        END;

        private Phase() {
        }
    }
}
