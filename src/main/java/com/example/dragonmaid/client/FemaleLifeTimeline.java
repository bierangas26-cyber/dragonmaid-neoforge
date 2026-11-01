package com.example.dragonmaid.client;

final class FemaleLifeTimeline {
    private FemaleLifeTimeline.Phase phase = FemaleLifeTimeline.Phase.ACTIVE;
    private double started;
    private double last = Double.NaN;
    private int generation;
    private int revision;

    FemaleLifeTimeline() {
    }

    FemaleLifeTimeline.Frame update(
        double time,
        boolean fallen,
        boolean injured,
        boolean freshFall,
        int nextGeneration,
        double enterTicks,
        double breathingTicks,
        double deathTicks,
        double recoveryTicks,
        boolean canRecoverSeated
    ) {
        boolean flag = false;
        boolean flag1 = !Double.isFinite(this.last) || time < this.last || time - this.last > 5.0 || this.generation != nextGeneration;
        if (flag1) {
            this.phase = !fallen
                ? FemaleLifeTimeline.Phase.ACTIVE
                : (injured ? (freshFall ? FemaleLifeTimeline.Phase.ENTER : FemaleLifeTimeline.Phase.INJURED) : FemaleLifeTimeline.Phase.DEAD);
            this.started = time;
            this.revision++;
        } else if (!fallen) {
            if ((this.phase == FemaleLifeTimeline.Phase.ENTER || this.phase == FemaleLifeTimeline.Phase.INJURED) && canRecoverSeated) {
                this.phase = FemaleLifeTimeline.Phase.RECOVERING;
                this.started = time;
                this.revision++;
            } else if (this.phase != FemaleLifeTimeline.Phase.ACTIVE && (this.phase != FemaleLifeTimeline.Phase.RECOVERING || !canRecoverSeated)) {
                this.phase = FemaleLifeTimeline.Phase.ACTIVE;
                this.started = time;
                this.revision++;
            }
        } else if (injured) {
            if (this.phase == FemaleLifeTimeline.Phase.ACTIVE
                || this.phase == FemaleLifeTimeline.Phase.RECOVERING
                || this.phase == FemaleLifeTimeline.Phase.DEAD
                || this.phase == FemaleLifeTimeline.Phase.DYING) {
                this.phase = FemaleLifeTimeline.Phase.ENTER;
                this.started = time;
                this.revision++;
            }
        } else if (this.phase != FemaleLifeTimeline.Phase.DYING && this.phase != FemaleLifeTimeline.Phase.DEAD) {
            this.phase = FemaleLifeTimeline.Phase.DYING;
            this.started = time;
            this.revision++;
        }

        if (this.phase == FemaleLifeTimeline.Phase.ENTER && time - this.started >= enterTicks) {
            this.started += enterTicks;
            this.phase = FemaleLifeTimeline.Phase.INJURED;
            this.revision++;
        }

        if (this.phase == FemaleLifeTimeline.Phase.DYING && time - this.started >= deathTicks) {
            this.phase = FemaleLifeTimeline.Phase.DEAD;
            this.started = time;
            this.revision++;
        }

        if (this.phase == FemaleLifeTimeline.Phase.RECOVERING && time - this.started >= recoveryTicks) {
            this.phase = FemaleLifeTimeline.Phase.ACTIVE;
            this.started = time;
            this.revision++;
            flag = true;
        }

        this.generation = nextGeneration;
        this.last = time;
        double d0 = Math.max(0.0, time - this.started);
        if (this.phase == FemaleLifeTimeline.Phase.INJURED) {
            d0 %= Math.max(1.0, breathingTicks);
        }

        if (this.phase == FemaleLifeTimeline.Phase.DEAD || this.phase == FemaleLifeTimeline.Phase.ACTIVE) {
            d0 = 0.0;
        }

        return new FemaleLifeTimeline.Frame(this.phase, d0, this.revision, flag);
    }

    static record Frame(FemaleLifeTimeline.Phase phase, double tick, int revision, boolean resumeSeated) {
    }

    static enum Phase {
        ACTIVE,
        ENTER,
        INJURED,
        RECOVERING,
        DYING,
        DEAD;

        private Phase() {
        }
    }
}
