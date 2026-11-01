package com.example.dragonmaid.client;

import software.bernie.geckolib.loading.math.MathValue;
import software.bernie.geckolib.loading.math.value.Constant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joml.Quaternionf;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.EasingType;
import software.bernie.geckolib.animation.keyframe.AnimationPoint;
import software.bernie.geckolib.animation.keyframe.BoneAnimation;
import software.bernie.geckolib.animation.keyframe.Keyframe;
import software.bernie.geckolib.animation.keyframe.KeyframeStack;
import software.bernie.geckolib.animation.state.BoneSnapshot;

final class SharedAnimationPose {
    private static final int CHANNELS = 9;
    private final GeoBone[] bones;
    private final Map<String, Integer> offsets = new HashMap<>();
    private final float[] bind;

    SharedAnimationPose(Collection<? extends GeoBone> registered) {
        this.bones = registered.toArray(GeoBone[]::new);
        this.bind = new float[this.bones.length * 9];

        for (int i = 0; i < this.bones.length; i++) {
            int j = i * 9;
            this.offsets.put(this.bones[i].getName(), j);
            BoneSnapshot bonesnapshot = this.bones[i].getInitialSnapshot();
            this.bind[j] = bonesnapshot.getRotX();
            this.bind[j + 1] = bonesnapshot.getRotY();
            this.bind[j + 2] = bonesnapshot.getRotZ();
            this.bind[j + 3] = bonesnapshot.getOffsetX();
            this.bind[j + 4] = bonesnapshot.getOffsetY();
            this.bind[j + 5] = bonesnapshot.getOffsetZ();
            this.bind[j + 6] = bonesnapshot.getScaleX();
            this.bind[j + 7] = bonesnapshot.getScaleY();
            this.bind[j + 8] = bonesnapshot.getScaleZ();
        }
    }

    boolean matches(Collection<? extends GeoBone> registered) {
        return registered.size() == this.bones.length && !registered.isEmpty() && registered.iterator().next() == this.bones[0];
    }

    float[] buffer() {
        return (float[])this.bind.clone();
    }

    void reset(float[] pose) {
        System.arraycopy(this.bind, 0, pose, 0, this.bind.length);
    }

    void resetBone(float[] pose, String name) {
        Integer integer = this.offsets.get(name);
        if (integer != null) {
            System.arraycopy(this.bind, integer, pose, integer, 9);
        }
    }

    void mixBone(String name, float[] source, float weight, float[] target) {
        Integer integer = this.offsets.get(name);
        if (integer != null) {
            for (int i = integer; i < integer + 9; i++) {
                target[i] += (source[i] - target[i]) * weight;
            }
        }
    }

    void positionScale(String name, float[] layer, float[] target) {
        Integer integer = this.offsets.get(name);
        if (integer != null) {
            System.arraycopy(layer, integer + 3, target, integer + 3, 6);
        }
    }

    void eyelids(float[] layer, float weight, float[] target) {
        this.positionScale("sleep_eyelids", layer, target);
        Integer integer = this.offsets.get("sleep_eyelids");
        if (integer != null) {
            target[integer + 7] *= Math.max(0.0F, Math.min(1.0F, weight));
        }
    }

    void set(float[] pose, String name, int channel, float value) {
        Integer integer = this.offsets.get(name);
        if (integer != null) {
            pose[integer + channel] = value;
        }
    }

    void add(float[] pose, String name, int channel, float value) {
        Integer integer = this.offsets.get(name);
        if (integer != null) {
            pose[integer + channel] += value;
        }
    }

    void blendChannel(String name, int channel, float[] source, float weight, float[] target) {
        Integer integer = this.offsets.get(name);
        if (integer != null) {
            target[integer + channel] += (source[integer + channel] - target[integer + channel]) * weight;
        }
    }

    void capture(float[] pose) {
        for (int i = 0; i < this.bones.length; i++) {
            int j = i * 9;
            GeoBone coregeobone = this.bones[i];
            pose[j] = coregeobone.getRotX();
            pose[j + 1] = coregeobone.getRotY();
            pose[j + 2] = coregeobone.getRotZ();
            pose[j + 3] = coregeobone.getPosX();
            pose[j + 4] = coregeobone.getPosY();
            pose[j + 5] = coregeobone.getPosZ();
            pose[j + 6] = coregeobone.getScaleX();
            pose[j + 7] = coregeobone.getScaleY();
            pose[j + 8] = coregeobone.getScaleZ();
        }
    }

    void apply(float[] pose) {
        for (int i = 0; i < this.bones.length; i++) {
            int j = i * 9;
            GeoBone coregeobone = this.bones[i];
            coregeobone.updateRotation(pose[j], pose[j + 1], pose[j + 2]);
            coregeobone.updatePosition(pose[j + 3], pose[j + 4], pose[j + 5]);
            coregeobone.updateScale(pose[j + 6], pose[j + 7], pose[j + 8]);
            coregeobone.resetStateChanges();
        }
    }

    void sample(Animation animation, double time, float[] pose) {
        this.sample(animation, time, pose, true);
    }

    void sampleOnce(Animation animation, double time, float[] pose) {
        this.sample(animation, time, pose, false);
    }

    private void sample(Animation animation, double time, float[] pose, boolean loop) {
        this.reset(pose);
        if (animation != null) {
            double d0 = tick(animation, time, loop);

            for (BoneAnimation boneanimation : animation.boneAnimations()) {
                Integer integer = this.offsets.get(boneanimation.boneName());
                if (integer != null) {
                    this.sampleTrack(boneanimation, d0, integer, pose);
                }
            }
        }
    }

    void sampleBone(Animation animation, double time, String name, float[] pose) {
        Integer integer = this.offsets.get(name);
        if (integer != null) {
            System.arraycopy(this.bind, integer, pose, integer, 9);
            if (animation != null) {
                double d0 = tick(animation, time, true);

                for (BoneAnimation boneanimation : animation.boneAnimations()) {
                    if (name.equals(boneanimation.boneName())) {
                        this.sampleTrack(boneanimation, d0, integer, pose);
                        return;
                    }
                }
            }
        }
    }

    private static double tick(Animation animation, double time, boolean loop) {
        return animation.length() <= 0.0
            ? 0.0
            : (loop ? (time % animation.length() + animation.length()) % animation.length() : Math.max(0.0, Math.min(time, animation.length())));
    }

    private void sampleTrack(BoneAnimation bone, double tick, int o, float[] pose) {
        KeyframeStack<Keyframe<MathValue>> keyframestack = bone.rotationKeyFrames();
        KeyframeStack<Keyframe<MathValue>> keyframestack1 = bone.positionKeyFrames();
        KeyframeStack<Keyframe<MathValue>> keyframestack2 = bone.scaleKeyFrames();
        pose[o] += value(keyframestack.xKeyframes(), tick, true, -1, 0.0F);
        pose[o + 1] = pose[o + 1] + value(keyframestack.yKeyframes(), tick, true, -1, 0.0F);
        pose[o + 2] = pose[o + 2] + value(keyframestack.zKeyframes(), tick, true, 1, 0.0F);
        pose[o + 3] = value(keyframestack1.xKeyframes(), tick, false, 1, this.bind[o + 3]);
        pose[o + 4] = value(keyframestack1.yKeyframes(), tick, false, 1, this.bind[o + 4]);
        pose[o + 5] = value(keyframestack1.zKeyframes(), tick, false, 1, this.bind[o + 5]);
        pose[o + 6] = value(keyframestack2.xKeyframes(), tick, false, 1, this.bind[o + 6]);
        pose[o + 7] = value(keyframestack2.yKeyframes(), tick, false, 1, this.bind[o + 7]);
        pose[o + 8] = value(keyframestack2.zKeyframes(), tick, false, 1, this.bind[o + 8]);
    }

    void rotations(Animation animation, float[] layer, float weight, boolean additive, float[] target) {
        if (animation != null && !(weight <= 0.0F)) {
            for (BoneAnimation boneanimation : animation.boneAnimations()) {
                Integer integer = this.offsets.get(boneanimation.boneName());
                if (integer != null) {
                    KeyframeStack<Keyframe<MathValue>> keyframestack = boneanimation.rotationKeyFrames();
                    if (!keyframestack.xKeyframes().isEmpty()) {
                        this.rotation(integer, layer, weight, additive, target);
                    }

                    if (!keyframestack.yKeyframes().isEmpty()) {
                        this.rotation(integer + 1, layer, weight, additive, target);
                    }

                    if (!keyframestack.zKeyframes().isEmpty()) {
                        this.rotation(integer + 2, layer, weight, additive, target);
                    }
                }
            }
        }
    }

    private void rotation(int offset, float[] layer, float weight, boolean additive, float[] target) {
        target[offset] += (layer[offset] - (additive ? this.bind[offset] : target[offset])) * weight;
    }

    private static float value(List<Keyframe<MathValue>> keys, double tick, boolean rotation, int sign, float fallback) {
        if (keys.isEmpty()) {
            return fallback;
        } else {
            double d0 = 0.0;

            for (Keyframe<MathValue> keyframe : keys) {
                double d1 = d0 + keyframe.length();
                if (d1 > tick) {
                    double d2 = numeric(keyframe.startValue(), rotation, sign);
                    double d3 = numeric(keyframe.endValue(), rotation, sign);
                    return (float)EasingType.lerpWithOverride(new AnimationPoint(keyframe, tick - d0, keyframe.length(), d2, d3), null);
                }

                d0 = d1;
            }

            return (float)numeric(keys.get(keys.size() - 1).endValue(), rotation, sign);
        }
    }

    private static double numeric(MathValue value, boolean rotation, int sign) {
        return rotation && !(value instanceof Constant) ? Math.toRadians(value.get()) * (double)sign : value.get();
    }

    static void mix(float[] from, float[] to, float weight, float[] output) {
        float f = Math.max(0.0F, Math.min(1.0F, weight));

        for (int i = 0; i < output.length; i++) {
            float f1 = to[i] - from[i];
            if (i % 9 < 3) {
                float f2 = (float) (Math.PI * 2);
                f1 %= f2;
                if ((double)f1 > Math.PI) {
                    f1 -= f2;
                }

                if ((double)f1 < -Math.PI) {
                    f1 += f2;
                }
            }

            output[i] = from[i] + f1 * f;
        }
    }

    static void mixLife(float[] from, float[] target, float weight) {
        Quaternionf quaternionf = new Quaternionf();
        Quaternionf quaternionf1 = new Quaternionf();

        for (int i = 0; i < target.length; i += 9) {
            quaternionf.rotationZYX(from[i + 2], from[i + 1], from[i]);
            quaternionf1.rotationZYX(target[i + 2], target[i + 1], target[i]);
            quaternionf.slerp(quaternionf1, weight).normalize();
            double d0 = (double)quaternionf.x;
            double d1 = (double)quaternionf.y;
            double d2 = (double)quaternionf.z;
            double d3 = (double)quaternionf.w;
            double d4 = Math.max(-1.0, Math.min(1.0, 2.0 * (d3 * d1 - d2 * d0)));
            if (Math.abs(d4) > 0.999999) {
                target[i] = 0.0F;
                target[i + 1] = (float)Math.copySign(Math.PI / 2, d4);
                target[i + 2] = (float)Math.atan2(2.0 * (d3 * d2 - d0 * d1), 1.0 - 2.0 * (d0 * d0 + d2 * d2));
            } else {
                target[i] = (float)Math.atan2(2.0 * (d3 * d0 + d1 * d2), 1.0 - 2.0 * (d0 * d0 + d1 * d1));
                target[i + 1] = (float)Math.asin(d4);
                target[i + 2] = (float)Math.atan2(2.0 * (d3 * d2 + d0 * d1), 1.0 - 2.0 * (d1 * d1 + d2 * d2));
            }

            for (int j = i + 3; j < i + 9; j++) {
                target[j] = from[j] + (target[j] - from[j]) * weight;
            }
        }
    }

    static final class Transition {
        private final float[] previous;
        private final float[] source;
        private double lastTime = Double.NaN;
        private double start;
        private int mode;
        private int generation;
        private boolean quaternionBlend;

        Transition(int size) {
            this.previous = new float[size];
            this.source = new float[size];
        }

        void apply(float[] target, int nextMode, double time, int nextGeneration) {
            this.apply(target, nextMode, time, nextGeneration, false);
        }

        void apply(float[] target, int nextMode, double time, int nextGeneration, boolean quaternionLife) {
            this.apply(target, nextMode, time, nextGeneration, quaternionLife, 0.0);
        }

        void apply(float[] target, int nextMode, double time, int nextGeneration, boolean quaternionLife, double actionAge) {
            if (Double.isFinite(this.lastTime) && !(time < this.lastTime) && !(time - this.lastTime > 5.0) && this.generation == nextGeneration) {
                if (this.mode != nextMode) {
                    System.arraycopy(this.previous, 0, this.source, 0, target.length);
                    this.quaternionBlend = quaternionLife && (this.mode < 0 || nextMode < 0);
                    this.mode = nextMode;
                    this.start = time - Math.max(0.0, actionAge);
                }

                float f = (float)Math.min(1.0, Math.max(0.0, (time - this.start) / 4.0));
                if (f < 1.0F) {
                    float f1 = f * f * (3.0F - 2.0F * f);
                    if (this.quaternionBlend) {
                        SharedAnimationPose.mixLife(this.source, target, f1);
                    } else {
                        SharedAnimationPose.mix(this.source, target, f1, target);
                    }
                }
            } else {
                this.mode = nextMode;
                this.generation = nextGeneration;
                this.start = time - 4.0;
            }

            System.arraycopy(target, 0, this.previous, 0, target.length);
            this.lastTime = time;
        }
    }
}
