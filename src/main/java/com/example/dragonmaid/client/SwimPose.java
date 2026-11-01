package com.example.dragonmaid.client;

final class SwimPose {
    private SwimPose() {
    }

    static void root(SharedAnimationPose rig, float[] pose, float pitchDegrees) {
        float f = (float)Math.toRadians((double)(-90.0F - Math.max(-90.0F, Math.min(90.0F, pitchDegrees))));
        rig.set(pose, "Root", 0, f);
        rig.set(pose, "Root", 4, (float)(-Math.cos((double)f) - 0.3 * Math.sin((double)f)) * 16.0F);
        rig.set(pose, "Root", 5, (float)(-Math.sin((double)f) + 0.3 * Math.cos((double)f)) * 16.0F);
    }
}
