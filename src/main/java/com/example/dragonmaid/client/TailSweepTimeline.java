package com.example.dragonmaid.client;

import com.example.dragonmaid.animation.TailSweepState;

final class TailSweepTimeline {
    static final double DURATION = 24.0;
    static final int RECOVERY_TICKS = 4;
    private TailSweepState state;

    TailSweepTimeline() {
    }

    TailSweepState state() {
        return this.state;
    }

    boolean accept(TailSweepState next) {
        if (Float.isFinite(next.yaw()) && next.revision() >= 0) {
            if (this.state == null
                || next.round() >= this.state.round()
                    && (
                        next.round() != this.state.round()
                            || next.revision() > this.state.revision()
                                && (this.state.cancelledAt() < 0L || next.cancelledAt() >= 0L)
                                && next.startedAt() == this.state.startedAt()
                                && Float.compare(next.yaw(), this.state.yaw()) == 0
                    )) {
                this.state = next;
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    TailSweepTimeline.Frame frame(double now) {
        if (this.state != null && !(now < (double)this.state.startedAt())) {
            double d0 = (double)this.state.startedAt() + 24.0;
            if (this.state.cancelledAt() >= 0L) {
                d0 = Math.min(d0, (double)Math.max(this.state.startedAt(), this.state.cancelledAt()));
            }

            if (now >= d0 + 4.0) {
                return null;
            } else {
                double d1 = Math.max(0.0, Math.min(now, d0) - (double)this.state.startedAt());
                double d2 = Math.max(0.0, Math.min(1.0, (now - d0) / 4.0));
                float f = (float)(1.0 - d2 * d2 * (3.0 - 2.0 * d2));
                return new TailSweepTimeline.Frame(TailSweepState.clipTick(d1), d1, f, this.state.yaw());
            }
        } else {
            return null;
        }
    }

    static record Frame(double tick, double age, float weight, float yaw) {
    }
}
