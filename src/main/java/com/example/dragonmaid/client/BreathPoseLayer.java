package com.example.dragonmaid.client;

import software.bernie.geckolib.animation.Animation;

final class BreathPoseLayer {
    private final float[] first;
    private final float[] pulse;
    private final float[] carry;
    private final float[] previousPulse;
    private long round = -1L;
    private int sequence;
    private double lastTime = Double.NaN;
    private boolean lastPulseActive;
    private boolean retrigger;

    BreathPoseLayer(SharedAnimationPose rig) {
        this.first = rig.buffer();
        this.pulse = rig.buffer();
        this.carry = rig.buffer();
        this.previousPulse = rig.buffer();
    }

    void apply(SharedAnimationPose rig, Animation firstAnimation, Animation followAnimation, BreathTimeline.Frame frame, double now, float[] target) {
        if (frame.round() != this.round || !Double.isFinite(this.lastTime) || now < this.lastTime || now - this.lastTime > 5.0) {
            this.round = frame.round();
            this.sequence = frame.sequence();
            rig.reset(this.carry);
            rig.reset(this.previousPulse);
            this.retrigger = false;
        } else if (this.sequence != frame.sequence()) {
            this.retrigger = this.lastPulseActive && now - this.lastTime <= 2.0;
            if (this.retrigger) {
                System.arraycopy(this.previousPulse, 0, this.carry, 0, this.carry.length);
            } else {
                rig.reset(this.carry);
            }

            this.sequence = frame.sequence();
        }

        this.lastTime = now;
        this.lastPulseActive = frame.followTick() >= 2.0 && frame.followTick() < 8.0 && frame.weight() > 0.0F;
        if (frame.weight() <= 0.0F) {
            rig.reset(this.previousPulse);
            rig.reset(this.carry);
        } else {
            if (!frame.lightning()) {
                rig.sampleOnce(firstAnimation, frame.firstTick(), this.first);
                rig.rotations(firstAnimation, this.first, frame.weight(), false, target);
            }

            rig.sampleOnce(followAnimation, frame.followTick(), this.pulse);
            float f = (float)Math.max(0.0, Math.min(1.0, frame.shotAge() / 1.5));
            if (this.retrigger) {
                SharedAnimationPose.mix(this.carry, this.pulse, f * f * (3.0F - 2.0F * f), this.pulse);
            }

            System.arraycopy(this.pulse, 0, this.previousPulse, 0, this.pulse.length);
            rig.rotations(followAnimation, this.pulse, frame.weight(), true, target);
        }
    }
}
