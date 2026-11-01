package com.example.dragonmaid.client;

final class ForgeBreathTimeline {
    private static final double BLEND_TICKS = 5.0;
    private static final double FIRST_ANIMATION_TICKS = 29.0;
    private double last = Double.NaN;
    private double phaseAt;
    private double transitionAt;
    private int generation;
    private int revision;
    private boolean working;
    private float transitionWeight;

    ForgeBreathTimeline() {
    }

    BreathTimeline.Frame update(double time, boolean nextWorking, boolean interrupt, int nextGeneration) {
        boolean flag = !Double.isFinite(this.last) || time < this.last || time - this.last > 5.0 || this.generation != nextGeneration;
        if (flag) {
            this.working = nextWorking && !interrupt;
            this.transitionWeight = this.working ? 1.0F : 0.0F;
            this.transitionAt = time - 5.0;
            this.phaseAt = time - 29.0;
            this.revision++;
        } else {
            float f = this.weight(time);
            boolean flag1 = nextWorking && !interrupt;
            if (interrupt) {
                this.working = false;
                this.transitionWeight = 0.0F;
                this.transitionAt = time - 5.0;
            } else if (this.working != flag1) {
                this.working = flag1;
                this.transitionWeight = f;
                this.transitionAt = time;
                if (this.working) {
                    this.phaseAt = time;
                    this.revision++;
                }
            }
        }

        this.generation = nextGeneration;
        this.last = time;
        float f1 = this.weight(time);
        if (f1 <= 0.0F) {
            return null;
        } else {
            double d1 = this.working ? time : this.transitionAt;
            double d0 = Math.max(0.0, Math.min(29.0, (d1 - this.phaseAt) * 2.0));
            return new BreathTimeline.Frame(-1L - (long)this.revision, 0, false, f1, d0, 8.0, Double.POSITIVE_INFINITY);
        }
    }

    private float weight(double time) {
        float f = (float)Math.max(0.0, Math.min(1.0, (time - this.transitionAt) / 5.0));
        f = f * f * (3.0F - 2.0F * f);
        return this.working ? this.transitionWeight + (1.0F - this.transitionWeight) * f : this.transitionWeight * (1.0F - f);
    }
}
