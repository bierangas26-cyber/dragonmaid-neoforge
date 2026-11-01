package com.example.dragonmaid.client;

enum BreathParticleStyle {
    FIRE(true, true, 0.24F, 1.0F, 9),
    ICE(true, true, 0.19F, 1.0F, 9),
    EMBER(true, true, 0.075F, 1.0F, 12),
    SHARD(true, true, 0.085F, 1.0F, 11),
    SPARK(true, true, 0.07F, 1.0F, 8),
    WISP(false, true, 0.12F, 0.2F, 8),
    SMOKE(false, false, 0.13F, 0.22F, 12),
    MIST(false, false, 0.16F, 0.2F, 12),
    FIRE_IMPACT(true, true, 0.48F, 1.0F, 4),
    ICE_IMPACT(true, true, 0.48F, 1.0F, 4);

    final boolean solid;
    final boolean glow;
    final float size;
    final float opacity;
    final int ticks;

    private BreathParticleStyle(boolean solid, boolean glow, float size, float opacity, int ticks) {
        this.solid = solid;
        this.glow = glow;
        this.size = size;
        this.opacity = opacity;
        this.ticks = ticks;
    }

    boolean flash() {
        return this == FIRE_IMPACT || this == ICE_IMPACT;
    }

    boolean cloud() {
        return this == WISP || this == SMOKE || this == MIST;
    }

    float gravity() {
        return this == EMBER ? 0.75F : (this == SHARD ? 0.35F : (this == SPARK ? 0.18F : 0.0F));
    }

    float friction() {
        return this == EMBER ? 0.999F : (this != SHARD && this != SPARK ? 0.91F : 0.98F);
    }

    float alphaAt(float progress) {
        return this.solid ? 1.0F : this.opacity * Math.min(1.0F, 0.55F + progress * 5.0F) * (1.0F - progress * progress);
    }

    float sizeAt(float progress) {
        if (this.cloud()) {
            return 1.0F + progress * (this == MIST ? 0.45F : 0.35F);
        } else if (this == EMBER) {
            return Math.max(0.0F, 1.0F - progress * progress);
        } else if (this == SPARK) {
            return Math.max(0.0F, 1.0F - progress);
        } else {
            return this.flash()
                ? (1.0F + progress) * Math.max(0.0F, 1.0F - progress * progress)
                : Math.max(0.08F, 1.0F - Math.max(0.0F, progress - 0.55F) / 0.45F);
        }
    }
}
