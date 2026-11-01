package com.example.dragonmaid.client;

import com.example.dragonmaid.particle.ModParticles;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

final class BreathVisuals {
    private static final BreathFxBudget BUDGET = new BreathFxBudget();
    private static final BreathImpactQueue<BreathVisuals.Impact> IMPACTS = new BreathImpactQueue<>();
    private static final List<BreathVisuals.Remnant> REMNANTS = new ArrayList<>();
    private static final List<BreathVisuals.NativeLava> NATIVE_LAVA = new ArrayList<>();
    private static final List<BreathVisuals.NativeLava> FIRE_STREAM = new ArrayList<>();
    private static long lavaCleanupTick = Long.MIN_VALUE;
    static final ParticleOptions ICE_CHIP = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState());
    private static ClientLevel world;

    private BreathVisuals() {
    }

    static int quality() {
        return ((ParticleStatus)Minecraft.getInstance().options.particles().get()).getId();
    }

    static void begin(ClientLevel level) {
        if (world != level) {
            REMNANTS.clear();
            IMPACTS.clear();
            NATIVE_LAVA.clear();
            FIRE_STREAM.clear();
            lavaCleanupTick = Long.MIN_VALUE;
            BUDGET.reset();
            world = level;
        }

        if (level != null) {
            BUDGET.begin(level.getGameTime(), quality());
            if (lavaCleanupTick != level.getGameTime()) {
                NATIVE_LAVA.removeIf(entry -> !entry.particle.isAlive() || level.getGameTime() > entry.expires);
                FIRE_STREAM.removeIf(entry -> !entry.particle.isAlive() || level.getGameTime() > entry.expires);
                lavaCleanupTick = level.getGameTime();
            }
        }
    }

    static boolean ray(ClientLevel level) {
        begin(level);
        return BUDGET.ray();
    }

    static boolean streamAvailable(ClientLevel level) {
        begin(level);
        return BUDGET.available(false);
    }

    static boolean near(Vec3 point, double radius) {
        return distanceSquared(point) <= radius * radius;
    }

    private static double distanceSquared(Vec3 point) {
        Entity entity = Minecraft.getInstance().getCameraEntity();
        return entity == null ? Double.POSITIVE_INFINITY : entity.position().distanceToSqr(point);
    }

    static void emit(ClientLevel level, ParticleOptions type, Vec3 point, Vec3 velocity, boolean impact, float scale) {
        begin(level);
        boolean flag = type == ModParticles.FIRE_CORE.get() || type == ModParticles.FIRE_SPARK.get();
        boolean flag1 = type == ModParticles.FIRE_EMBER.get() || type == ParticleTypes.LAVA;
        if (!flag || FIRE_STREAM.size() < BreathFxTuning.fireCoreLimit(quality())) {
            if (flag1) {
                int i = quality() == 0 ? 48 : (quality() == 1 ? 24 : 8);
                if (NATIVE_LAVA.size() >= (impact ? i : i * 2 / 3)) {
                    return;
                }
            }

            double d0 = distanceSquared(point);
            if (!(d0 > 4096.0) && (impact || !(d0 > 1024.0) || level.random.nextInt(3) == 0) && BUDGET.particle(impact)) {
                Particle particle = Minecraft.getInstance()
                    .particleEngine
                    .createParticle(type, point.x, point.y, point.z, velocity.x, velocity.y, velocity.z);
                if (flag) {
                    if (particle != null) {
                        if (particle instanceof FadingBreathParticle fadingbreathparticle1) {
                            fadingbreathparticle1.visualScale(scale);
                        } else if (particle instanceof TextureSheetParticle texturesheetparticle) {
                            particle.scale(BreathFxTuning.fireCoreCappedScale(texturesheetparticle.getQuadSize(0.0F), scale));
                        } else {
                            particle.scale(scale);
                        }

                        FIRE_STREAM.add(new BreathVisuals.NativeLava(particle, level.getGameTime() + (long)particle.getLifetime() + 2L));
                    }
                } else if (flag1) {
                    if (particle != null) {
                        NATIVE_LAVA.add(new BreathVisuals.NativeLava(particle, level.getGameTime() + (long)particle.getLifetime() + 2L));
                    }
                } else if (type != ParticleTypes.SMOKE) {
                    if (particle instanceof FadingBreathParticle fadingbreathparticle) {
                        fadingbreathparticle.visualScale(scale);
                    } else if (particle != null && scale != 1.0F) {
                        particle.scale(scale);
                    }
                }
            }
        }
    }

    static void trail(ClientLevel level, double x, double y, double z) {
        emit(level, (ParticleOptions)ModParticles.FIRE_BREATH.get(), new Vec3(x, y, z), new Vec3(0.0, 0.008, 0.0), false, 0.13F);
    }

    static Vec3 scatter(ClientLevel level, double size) {
        return new Vec3(
            (level.random.nextDouble() * 2.0 - 1.0) * size, (level.random.nextDouble() * 2.0 - 1.0) * size, (level.random.nextDouble() * 2.0 - 1.0) * size
        );
    }

    static void impact(ClientLevel level, Vec3 point, Vec3 normal, boolean ice, boolean target, float strength) {
        begin(level);
        if (near(point, 64.0)) {
            IMPACTS.add(new BreathVisuals.Impact(point, normal, ice, target, strength), target, level.getGameTime());
        }
    }

    private static void burst(ClientLevel level, BreathVisuals.Impact hit) {
        Vec3 vec3 = hit.normal;
        Entity entity = Minecraft.getInstance().getCameraEntity();
        if (vec3.lengthSqr() < 0.1 && entity != null) {
            vec3 = entity.getEyePosition().subtract(hit.point).normalize();
        }

        Vec3 vec31 = hit.point.add(vec3.scale(hit.target ? 0.55 : 0.12));
        int i = BreathFxTuning.burstCount(hit.ice, quality(), BUDGET.remaining(true));
        float f = hit.strength * (hit.target ? 1.35F : 1.0F) * (hit.ice ? 0.8F : 1.0F);
        emit(level, hit.ice ? (ParticleOptions)ModParticles.ICE_IMPACT.get() : (ParticleOptions)ModParticles.FIRE_IMPACT.get(), vec31, Vec3.ZERO, true, f);

        for (int j = 1; j < i; j++) {
            double d0 = 1.0 - 2.0 * ((double)j - 0.5) / (double)(i - 1);
            double d1 = (double)j * 2.399963229728653;
            double d2 = Math.sqrt(Math.max(0.0, 1.0 - d0 * d0));
            Vec3 vec32 = new Vec3(Math.cos(d1) * d2, d0, Math.sin(d1) * d2);
            if (!hit.target && vec32.dot(vec3) < 0.0) {
                vec32 = vec32.subtract(vec3.scale(2.0 * vec32.dot(vec3)));
            }

            Vec3 vec33 = hit.point.add(vec32.scale(hit.target ? 0.35 : 0.08)).add(vec3.scale(0.09));
            if (j % (hit.ice ? 6 : 4) == 0) {
                emit(
                    level,
                    (ParticleOptions)(hit.ice ? (ParticleOptions)ModParticles.ICE_MIST.get() : ParticleTypes.SMOKE),
                    vec33,
                    vec32.scale(0.07).add(0.0, hit.ice ? 0.005 : 0.055, 0.0),
                    true,
                    f * 0.75F
                );
            } else if (hit.ice && j % 3 == 0) {
                emit(level, ICE_CHIP, vec33, vec32.scale(0.34 * (double)f).add(vec3.scale(0.08)), true, 0.75F);
            } else {
                emit(
                    level,
                    hit.ice ? (ParticleOptions)ModParticles.ICE_SHARD.get() : (ParticleOptions)ModParticles.FIRE_EMBER.get(),
                    vec33,
                    vec32.scale((0.24 + level.random.nextDouble() * 0.22) * (double)f).add(0.0, hit.ice ? 0.035 : 0.14, 0.0),
                    true,
                    f
                );
            }
        }

        Vec3 vec34 = null;
        if (ray(level)) {
            BlockHitResult blockhitresult = level.clip(
                new ClipContext(
                    hit.point.add(0.0, 0.2, 0.0), hit.point.add(0.0, -3.0, 0.0), Block.COLLIDER, Fluid.NONE, Minecraft.getInstance().player
                )
            );
            if (blockhitresult.getType() == Type.BLOCK && blockhitresult.getDirection() == Direction.UP) {
                vec34 = blockhitresult.getLocation().add(0.0, 0.05, 0.0);
            }
        }

        if (REMNANTS.size() < 48) {
            REMNANTS.add(new BreathVisuals.Remnant(vec31, vec34, hit.ice, level.getGameTime()));
        }
    }

    static void tick(ClientLevel level) {
        begin(level);

        for (int i = 0; i < 4 && BUDGET.remaining(true) >= 7; i++) {
            BreathVisuals.Impact breathvisuals$impact = IMPACTS.poll(level.getGameTime());
            if (breathvisuals$impact == null) {
                break;
            }

            burst(level, breathvisuals$impact);
        }

        Iterator<BreathVisuals.Remnant> iterator = REMNANTS.iterator();

        while (iterator.hasNext()) {
            BreathVisuals.Remnant breathvisuals$remnant = iterator.next();
            long j = level.getGameTime() - breathvisuals$remnant.started;
            if (j > (long)(breathvisuals$remnant.ice ? 12 : 16) || j < 0L) {
                iterator.remove();
            } else if (BreathFxTuning.residueTick(breathvisuals$remnant.ice, j)) {
                emit(
                    level,
                    (ParticleOptions)(breathvisuals$remnant.ice ? (ParticleOptions)ModParticles.ICE_MIST.get() : ParticleTypes.SMOKE),
                    breathvisuals$remnant.point.add(scatter(level, 0.18)),
                    scatter(level, 0.025).add(0.0, breathvisuals$remnant.ice ? -0.005 : 0.045, 0.0),
                    false,
                    0.6F
                );
                if (!breathvisuals$remnant.ice && j < 12L) {
                    emit(
                        level,
                        (ParticleOptions)ModParticles.FIRE_EMBER.get(),
                        breathvisuals$remnant.point.add(scatter(level, 0.24)),
                        scatter(level, 0.06).add(0.0, 0.07, 0.0),
                        false,
                        0.65F
                    );
                }

                if (breathvisuals$remnant.ground != null) {
                    Vec3 vec3 = breathvisuals$remnant.ground.add((level.random.nextDouble() - 0.5) * 1.1, 0.0, (level.random.nextDouble() - 0.5) * 1.1);
                    emit(
                        level,
                        breathvisuals$remnant.ice ? (ParticleOptions)ModParticles.ICE_MIST.get() : (ParticleOptions)ModParticles.FIRE_BREATH.get(),
                        vec3,
                        new Vec3(0.0, 0.008, 0.0),
                        false,
                        breathvisuals$remnant.ice ? 0.6F : 0.42F
                    );
                    if (breathvisuals$remnant.ice && j == 4L) {
                        emit(level, ICE_CHIP, vec3, new Vec3(0.0, 0.07, 0.0), false, 0.8F);
                    }
                }
            }
        }
    }

    private static record Impact(Vec3 point, Vec3 normal, boolean ice, boolean target, float strength) {
    }

    private static record NativeLava(Particle particle, long expires) {
    }

    private static record Remnant(Vec3 point, Vec3 ground, boolean ice, long started) {
    }
}
