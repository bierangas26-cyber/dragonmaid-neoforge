package com.example.dragonmaid.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;

public final class FadingBreathParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final BreathParticleStyle style;
    private final float originalSize;
    private float sizeMultiplier = 1.0F;

    FadingBreathParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites, BreathParticleStyle style) {
        super(level, x, y, z, dx, dy, dz);
        this.sprites = sprites;
        this.style = style;
        this.hasPhysics = false;
        this.lifetime = style.ticks + (style.flash() ? 0 : this.random.nextInt(4));
        this.friction = style.friction();
        this.gravity = style.gravity();
        this.quadSize = this.originalSize = style.size * (0.8F + this.random.nextFloat() * 0.4F);
        this.alpha = style.alphaAt(0.0F);
        switch (style) {
            case ICE:
            case ICE_IMPACT:
                this.setColor(0.82F, 0.96F, 1.0F);
                break;
            case MIST:
                this.setColor(0.68F, 0.85F, 1.0F);
                break;
            case SHARD:
                this.setColor(0.48F, 0.79F, 1.0F);
                break;
            case SMOKE:
                this.setColor(0.13F, 0.105F, 0.09F);
                break;
            case EMBER:
                this.setColor(1.0F, 0.73F, 0.14F);
                break;
            case SPARK:
                this.setColor(1.0F, 0.82F, 0.28F);
                break;
            case WISP:
                this.setColor(1.0F, 0.42F, 0.035F);
                break;
            default:
                this.setColor(1.0F, 0.95F, 0.67F);
        }

        if (style == BreathParticleStyle.SHARD) {
            this.roll = this.oRoll = this.random.nextFloat() * 6.28F;
        }

        if (style == BreathParticleStyle.SPARK) {
            this.roll = this.oRoll = this.random.nextFloat() * 6.28F;
        }

        this.setSpriteFromAge(sprites);
        this.setParticleSpeed(dx, dy, dz);
    }

    void visualScale(float scale) {
        this.sizeMultiplier = scale;
        this.quadSize = this.originalSize * scale;
    }

    public void tick() {
        super.tick();
        if (this.isAlive()) {
            this.setSpriteFromAge(this.sprites);
            float f = Mth.clamp((float)this.age / (float)this.lifetime, 0.0F, 1.0F);
            this.alpha = this.style.alphaAt(f);
            this.quadSize = this.originalSize * this.sizeMultiplier * this.style.sizeAt(f);
            if (this.style == BreathParticleStyle.SMOKE || this.style == BreathParticleStyle.WISP) {
                this.yd += 0.004;
            }

            if (this.style == BreathParticleStyle.MIST) {
                this.yd -= 8.0E-4;
            }

            if (this.style == BreathParticleStyle.FIRE) {
                this.gCol = 0.95F - f * 0.6F;
                this.bCol = 0.67F * (1.0F - f);
                this.yd += 0.003;
            }

            if (this.style == BreathParticleStyle.SPARK) {
                this.gCol = 0.82F - f * 0.35F;
                this.bCol = 0.28F * (1.0F - f);
                this.oRoll = this.roll;
                this.roll += 0.09F;
            }

            if (this.style == BreathParticleStyle.SHARD) {
                this.oRoll = this.roll;
                this.roll += 0.16F;
            }

            if (this.style == BreathParticleStyle.EMBER) {
                this.gCol = 0.73F - f * 0.45F;
                this.bCol = 0.14F * (1.0F - f);
                if (this.age == 3 || this.age == 7) {
                    BreathVisuals.trail(this.level, this.xo, this.yo, this.zo);
                }
            }
        }
    }

    public ParticleRenderType getRenderType() {
        return this.style.solid ? ParticleRenderType.PARTICLE_SHEET_OPAQUE : ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public int getLightColor(float partial) {
        return this.style.glow ? 15728880 : super.getLightColor(partial);
    }
}
