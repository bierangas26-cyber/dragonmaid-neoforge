package com.example.dragonmaid.client;

import com.example.dragonmaid.animation.BreathState;

final class BreathTimeline {
    private BreathState state;
    private double cancelledAt = Double.NaN;
    private int forecastRevision;
    private int forecastSequence;
    private long forecastAt = -1L;
    private BreathTimeline.EyeFrame releasedEye = BreathTimeline.EyeFrame.OPEN;
    private double eyeReleaseAt = Double.NaN;

    BreathTimeline() {
    }

    boolean accept(BreathState next) {
        if (this.state == null
            || next.round() >= this.state.round()
                && (
                    next.round() != this.state.round()
                        || next.revision() > this.state.revision()
                            && next.sequence() >= this.state.sequence()
                            && !Double.isFinite(this.cancelledAt)
                            && (this.state.running() || !next.running())
                )) {
            if (this.state == null || next.round() != this.state.round()) {
                this.cancelledAt = Double.NaN;
                this.forecastRevision = 0;
                this.forecastSequence = 0;
                this.forecastAt = -1L;
                this.releasedEye = BreathTimeline.EyeFrame.OPEN;
                this.eyeReleaseAt = Double.NaN;
            } else if (this.state.running() && (next.phase() == 4 || next.phase() == 3 && next.firstAt() < 0L)) {
                this.releaseEye((double)next.endedAt());
            }

            this.state = next;
            if (!this.state.running() || this.state.sequence() >= this.forecastSequence) {
                this.forecastAt = -1L;
            }

            return true;
        } else {
            return false;
        }
    }

    boolean forecast(long round, int revision, int sequence, long at, double now) {
        if (this.state != null
            && round == this.state.round()
            && revision > this.forecastRevision
            && this.state.running()
            && !Double.isFinite(this.cancelledAt)
            && (this.state.lightning() || this.state.firstAt() >= 0L)) {
            long i = sequence > this.state.sequence() && (double)at - now >= 2.0 ? at : -1L;
            if (this.forecastAt >= 0L && this.forecastAt != i) {
                this.releaseEye(now);
            }

            this.forecastRevision = revision;
            this.forecastSequence = sequence;
            this.forecastAt = i;
            return true;
        } else {
            return false;
        }
    }

    BreathTimeline.EyeFrame eye(double now) {
        if (this.state == null) {
            return BreathTimeline.EyeFrame.OPEN;
        } else {
            if (!Double.isFinite(this.cancelledAt) && this.state.phase() != 4) {
                if (!this.state.lightning()) {
                    if (this.state.firstAt() < 0L && this.state.running()) {
                        return new BreathTimeline.EyeFrame(true, this.frame(now).firstTick(), 1.0F);
                    }

                    if (this.state.firstAt() >= 0L && now - (double)this.state.firstAt() < 1.0) {
                        return new BreathTimeline.EyeFrame(true, 20.0 + Math.max(0.0, now - (double)this.state.firstAt()), 1.0F);
                    }
                }

                double d0 = now - (double)this.state.shotAt();
                if (this.state.sequence() > 0 && (this.state.shotType() & 4) == 0 && d0 >= 0.0 && d0 < 2.0) {
                    return new BreathTimeline.EyeFrame(false, 2.0 + d0, 1.0F);
                }

                if (this.state.running()
                    && this.forecastAt >= 0L
                    && this.state.sequence() < this.forecastSequence
                    && now >= (double)(this.forecastAt - 2L)
                    && now < (double)(this.forecastAt + 2L)) {
                    return new BreathTimeline.EyeFrame(false, now - (double)(this.forecastAt - 2L), 1.0F);
                }

                if (Double.isFinite(this.eyeReleaseAt) && (double)this.state.shotAt() >= this.eyeReleaseAt) {
                    return BreathTimeline.EyeFrame.OPEN;
                }
            }

            if (!Double.isFinite(this.eyeReleaseAt)) {
                return BreathTimeline.EyeFrame.OPEN;
            } else {
                float f = this.releasedEye.weight() * (1.0F - smooth((now - this.eyeReleaseAt) / 2.0));
                return f > 0.0F ? new BreathTimeline.EyeFrame(this.releasedEye.first(), this.releasedEye.tick(), f) : BreathTimeline.EyeFrame.OPEN;
            }
        }
    }

    private void releaseEye(double now) {
        this.releasedEye = this.eye(now);
        this.eyeReleaseAt = now;
    }

    void cancel(double now) {
        if (this.state != null && !Double.isFinite(this.cancelledAt)) {
            this.releaseEye(now);
            this.cancelledAt = now;
        }
    }

    BreathState state() {
        return this.state;
    }

    BreathTimeline.Frame frame(double now) {
        if (this.state == null) {
            return new BreathTimeline.Frame(0L, 0, false, 0.0F, 0.0, 8.0, Double.POSITIVE_INFINITY);
        } else {
            boolean flag = Double.isFinite(this.cancelledAt) || this.state.phase() == 4;
            double d0 = Double.isFinite(this.cancelledAt) ? this.cancelledAt : (double)this.state.endedAt();
            double d1 = flag ? Math.min(now, d0) : now;
            double d2 = this.state.firstAt() >= 0L
                ? Math.min(29.0, 20.0 + Math.max(0.0, d1 - (double)this.state.firstAt()))
                : (
                    this.state.phase() == 1
                        ? 20.0
                        : Math.min(20.0, Math.max(0.0, d1 - (double)this.state.prepareAt()) * 20.0 / (double)this.state.prepareTicks())
                );
            double d3 = this.state.sequence() > 0 ? d1 - (double)this.state.shotAt() : Double.POSITIVE_INFINITY;
            double d4 = (this.state.shotType() & 4) == 0 && this.state.sequence() > 0 ? Math.min(8.0, 2.0 + Math.max(0.0, d3)) : 8.0;
            float f = smooth((d1 - (double)this.state.prepareAt()) / 4.0);
            if (flag || this.state.phase() == 3) {
                double d5 = flag
                    ? d0
                    : Math.max(d0, this.state.sequence() == 0 ? d0 : (double)(this.state.shotAt() + (long)((this.state.shotType() & 4) != 0 ? 9 : 6)));
                f *= 1.0F - smooth((now - d5) / 5.0);
            }

            return new BreathTimeline.Frame(this.state.round(), this.state.sequence(), this.state.lightning(), f, d2, d4, d3);
        }
    }

    private static float smooth(double value) {
        float f = (float)Math.max(0.0, Math.min(1.0, value));
        return f * f * (3.0F - 2.0F * f);
    }

    static record EyeFrame(boolean first, double tick, float weight) {
        static final BreathTimeline.EyeFrame OPEN = new BreathTimeline.EyeFrame(false, 2.0, 0.0F);
    }

    static record Frame(long round, int sequence, boolean lightning, float weight, double firstTick, double followTick, double shotAge) {
    }
}
