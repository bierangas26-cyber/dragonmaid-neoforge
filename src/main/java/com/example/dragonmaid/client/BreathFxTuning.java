package com.example.dragonmaid.client;

final class BreathFxTuning {
    private BreathFxTuning() {
    }

    static BreathFxTuning.Role role(int index, int age) {
        int i = index % 6;
        if (i == 0 || i == 2) {
            return BreathFxTuning.Role.CORE;
        } else if (i == 5) {
            return BreathFxTuning.Role.FRAGMENT;
        } else {
            return i == 4 && (index / 6 + age) % 6 == 0 ? BreathFxTuning.Role.HAZE : BreathFxTuning.Role.SKIP;
        }
    }

    static BreathFxTuning.Role fireRole(int index, int age, int candidates) {
        int i = index % 6;
        int j = index / 6;
        if (i != 0 && i != 2) {
            int i1 = candidates >= 54 ? 3 : (candidates >= 30 ? 2 : 1);
            if (i == 1 && (j + age) % (candidates / 6) < i1) {
                return BreathFxTuning.Role.EMBER;
            } else if (i == 5 && (j + age) % 3 == 0) {
                return BreathFxTuning.Role.FRAGMENT;
            } else {
                return role(index, age) == BreathFxTuning.Role.HAZE ? BreathFxTuning.Role.HAZE : BreathFxTuning.Role.SKIP;
            }
        } else {
            int k = j * 2 + (i == 2 ? 1 : 0);
            int l = candidates >= 54 ? 12 : (candidates >= 30 ? 6 : 3);
            return (k + age) % (candidates / 3) < l ? BreathFxTuning.Role.CORE : BreathFxTuning.Role.SKIP;
        }
    }

    static int fireCoreLifetime() {
        return 12;
    }

    static boolean fireCoreSpark(int index, int age, int candidates) {
        int i = index / 6 * 2 + (index % 6 == 2 ? 1 : 0);
        return (i + age) % (candidates / 3) % 4 == 0;
    }

    static float fireCoreScale() {
        return 0.95F;
    }

    static float fireCoreCappedScale(float originalSize, float requestedScale) {
        return originalSize > 0.0F ? Math.min(requestedScale, 0.18F / originalSize) : requestedScale;
    }

    static int fireCoreLimit(int quality) {
        return quality == 0 ? 160 : (quality == 1 ? 80 : 36);
    }

    static int burstCount(boolean ice, int quality, int available) {
        int i = Math.min(Math.max(0, available), quality == 0 ? 28 : (quality == 1 ? 17 : 7));
        return ice ? i * 2 / 3 : i;
    }

    static boolean residueTick(boolean ice, long age) {
        return ice ? age == 4L || age == 10L : age >= 0L && age <= 16L && age % 4L == 0L;
    }

    static enum Role {
        CORE,
        EMBER,
        FRAGMENT,
        HAZE,
        SKIP;

        private Role() {
        }
    }
}
