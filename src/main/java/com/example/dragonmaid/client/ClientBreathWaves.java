package com.example.dragonmaid.client;

import com.example.dragonmaid.combat.BreathFan;
import com.example.dragonmaid.network.BreathImpactMessage;
import com.example.dragonmaid.network.BreathWaveMessage;
import com.example.dragonmaid.particle.ModParticles;
import com.iafenvoy.iceandfire.entity.IceDragonEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(
    modid = "dragonmaid",
    value = {Dist.CLIENT}
)
public final class ClientBreathWaves {
    private static final List<ClientBreathWaves.Front> ACTIVE = new ArrayList<>();
    private static final Map<ClientBreathWaves.ImpactKey, Long> SEEN = new HashMap<>();
    private static ClientLevel currentLevel;

    private ClientBreathWaves() {
    }

    public static void accept(BreathWaveMessage message) {
        ClientLevel clientlevel = Minecraft.getInstance().level;
        syncLevel(clientlevel);
        if (clientlevel != null && clientlevel.dimension().location().equals(message.dimension())) {
            if (BreathVisuals.near(message.fan().origin(), 64.0 + message.fan().range())) {
                double d0 = (double)Math.max(0L, clientlevel.getGameTime() - message.gameTime()) * message.fan().speed();
                if (!(d0 >= message.fan().range())) {
                    if (message.ice()
                        && message.sourceUuid() != null
                        && Math.abs(clientlevel.getGameTime() - message.gameTime()) <= 2L
                        && clientlevel.getEntity(message.sourceId()) instanceof IceDragonEntity entityicedragon
                        && entityicedragon.getUUID().equals(message.sourceUuid())) {
                        Vec3 vec3 = ClientMouthEffects.waterMouth(entityicedragon);
                        if (vec3 != null) {
                            bridge(clientlevel, vec3, message.fan().origin());
                        }
                    }

                    if (ACTIVE.size() >= 48) {
                        ACTIVE.remove(0);
                    }

                    ACTIVE.add(new ClientBreathWaves.Front(message.fan(), message.ice(), d0, clientlevel.random.nextLong()));
                }
            }
        }
    }

    static void bridge(ClientLevel level, Vec3 mouth, Vec3 origin) {
        Vec3 vec3 = origin.subtract(mouth);
        double d0 = vec3.length();
        if (!(d0 < 0.03) && !(d0 > 2.5) && BreathVisuals.near(mouth, 64.0) && BreathVisuals.ray(level)) {
            if (level.clip(new ClipContext(mouth, origin, Block.COLLIDER, Fluid.NONE, Minecraft.getInstance().player)).getType() == Type.MISS) {
                int i = BreathVisuals.quality() == 0 ? 16 : (BreathVisuals.quality() == 1 ? 8 : 4);
                int j = Math.min(i, Math.max(2, (int)Math.ceil(d0 * 8.0) + 1));

                for (int k = 0; k < j && BreathVisuals.streamAvailable(level); k++) {
                    Vec3 vec31 = mouth.lerp(origin, (double)k / (double)(j - 1));
                    BreathVisuals.emit(level, (ParticleOptions)ModParticles.ICE_BREATH.get(), vec31, Vec3.ZERO, false, 0.8F);
                }
            }
        }
    }

    public static void impact(BreathImpactMessage message) {
        ClientLevel clientlevel = Minecraft.getInstance().level;
        syncLevel(clientlevel);
        if (clientlevel != null
            && clientlevel.dimension().location().equals(message.dimension())
            && Math.abs(clientlevel.getGameTime() - message.gameTime()) <= 10L) {
            ClientBreathWaves.ImpactKey clientbreathwaves$impactkey = new ClientBreathWaves.ImpactKey(message.waveId(), message.gameTime());
            if (SEEN.putIfAbsent(clientbreathwaves$impactkey, clientlevel.getGameTime()) == null) {
                if (SEEN.size() > 256) {
                    SEEN.remove(SEEN.keySet().iterator().next());
                }

                for (Vec3 vec3 : message.points()) {
                    BreathVisuals.impact(clientlevel, vec3, Vec3.ZERO, message.ice(), true, 1.0F);
                }
            }
        }
    }

    private static void syncLevel(ClientLevel level) {
        if (currentLevel != level) {
            ACTIVE.clear();
            SEEN.clear();
            currentLevel = level;
        }

        BreathVisuals.begin(level);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        // 1.21.1: ClientTickEvent is split into Pre/Post; this handler only runs on Post.
        {
            Minecraft minecraft = Minecraft.getInstance();
            syncLevel(minecraft.level);
            if (currentLevel != null && !minecraft.isPaused()) {
                SEEN.values().removeIf(t -> currentLevel.getGameTime() - t > 40L);
                BreathVisuals.tick(currentLevel);
                if (!ACTIVE.isEmpty()) {
                    Collections.rotate(ACTIVE, 1);
                }

                ACTIVE.removeIf(front -> !front.tick(currentLevel));
            }
        }
    }

    private static final class Front {
        private final BreathFan fan;
        private final Vec3 right;
        private final Vec3 up;
        private final boolean ice;
        private final long seed;
        private final double[] stops = new double[45];
        private final Vec3[] hitPoints = new Vec3[45];
        private final Vec3[] normals = new Vec3[45];
        private final boolean[] ready = new boolean[45];
        private final boolean[] blocked = new boolean[45];
        private final boolean[] impacted = new boolean[45];
        private final List<Vec3> surfaceBursts = new ArrayList<>();
        private double distance;
        private int age;

        Front(BreathFan fan, boolean ice, double distance, long seed) {
            this.fan = fan;
            this.ice = ice;
            this.distance = distance;
            this.seed = seed;
            this.right = fan.right();
            this.up = fan.up();
        }

        private Vec3 point(double along, double side, double height) {
            return this.fan
                .center(along)
                .add(this.right.scale(side * this.fan.width(along)))
                .add(this.up.scale(height * this.fan.height(along)));
        }

        private boolean collision(ClientLevel level, int cell, BreathScatter.Sample sample) {
            if (this.ready[cell]) {
                return true;
            } else if (!BreathVisuals.ray(level)) {
                return false;
            } else {
                Vec3 vec3 = this.point(0.0, sample.side(), sample.height());
                Vec3 vec31 = this.point(this.fan.range(), sample.side(), sample.height());
                BlockHitResult blockhitresult = level.clip(new ClipContext(vec3, vec31, Block.COLLIDER, Fluid.NONE, Minecraft.getInstance().player));
                this.blocked[cell] = blockhitresult.getType() == Type.BLOCK;
                this.stops[cell] = this.blocked[cell]
                    ? Math.max(0.0, blockhitresult.getLocation().subtract(this.fan.origin()).dot(this.fan.forward()) - 0.1)
                    : this.fan.range();
                this.hitPoints[cell] = blockhitresult.getLocation();
                this.normals[cell] = this.blocked[cell] ? Vec3.atLowerCornerOf(blockhitresult.getDirection().getNormal()) : this.fan.forward().scale(-1.0);
                this.ready[cell] = true;
                return true;
            }
        }

        boolean tick(ClientLevel level) {
            double d0 = Math.min(this.fan.range(), this.distance + this.fan.speed());
            if (!BreathVisuals.near(this.fan.center((this.distance + d0) * 0.5), 64.0 + this.fan.width(d0))) {
                this.age++;
                this.distance = d0;
                return this.distance < this.fan.range();
            } else {
                Vec3 vec3 = null;
                int i = BreathScatter.count(BreathVisuals.quality());

                for (int j = 0; j < i; j++) {
                    BreathFxTuning.Role breathfxtuning$role = this.ice ? BreathFxTuning.role(j, this.age) : BreathFxTuning.fireRole(j, this.age, i);
                    if (breathfxtuning$role != BreathFxTuning.Role.SKIP) {
                        BreathScatter.Sample breathscatter$sample = this.ice
                            ? BreathScatter.sample(this.seed, this.age, j)
                            : BreathScatter.fireSample(this.seed, this.age, j);
                        int k = BreathScatter.cell(breathscatter$sample.side(), breathscatter$sample.height());
                        if (this.collision(level, k, breathscatter$sample)) {
                            double d1 = this.stops[k];
                            if (this.blocked[k] && !this.impacted[k] && d0 >= d1) {
                                this.impacted[k] = true;
                                Vec3 vec31 = this.hitPoints[k];
                                if (this.surfaceBursts.size() < 8 && this.surfaceBursts.stream().noneMatch(p -> p.distanceToSqr(vec31) < 0.6400000000000001)) {
                                    this.surfaceBursts.add(vec31);
                                    BreathVisuals.impact(level, vec31, this.normals[k], this.ice, false, 0.85F);
                                }
                            }

                            if (!this.blocked[k] && d0 >= this.fan.range()) {
                                vec3 = this.point(this.fan.range(), breathscatter$sample.side(), breathscatter$sample.height());
                            }

                            if (!(this.distance >= d1) && BreathVisuals.streamAvailable(level)) {
                                double d4 = this.distance + (Math.min(d0, d1) - this.distance) * breathscatter$sample.depth();
                                Vec3 vec32 = this.point(d4, breathscatter$sample.side(), breathscatter$sample.height());
                                if (BreathVisuals.near(vec32, 64.0)) {
                                    Vec3 vec33 = this.point(this.fan.range(), breathscatter$sample.side(), breathscatter$sample.height())
                                        .subtract(this.point(0.0, breathscatter$sample.side(), breathscatter$sample.height()))
                                        .normalize();
                                    double d2 = Math.max(0.0, d1 - d4);
                                    boolean flag = this.ice && j % 6 == 2 && j / 6 % 3 == 0;
                                    ParticleOptions particleoptions;
                                    double d3;
                                    float f;
                                    if (breathfxtuning$role == BreathFxTuning.Role.CORE) {
                                        particleoptions = this.ice
                                            ? (flag ? BreathVisuals.ICE_CHIP : (ParticleOptions)ModParticles.ICE_BREATH.get())
                                            : (
                                                BreathFxTuning.fireCoreSpark(j, this.age, i)
                                                    ? (ParticleOptions)ModParticles.FIRE_SPARK.get()
                                                    : (ParticleOptions)ModParticles.FIRE_CORE.get()
                                            );
                                        d3 = this.ice ? Math.min(0.65, d2 / 13.0) : Math.min(0.9, d2 / 15.0);
                                        f = this.ice ? (flag ? 0.8F : 1.1F) : BreathFxTuning.fireCoreScale();
                                    } else if (breathfxtuning$role == BreathFxTuning.Role.EMBER) {
                                        particleoptions = (ParticleOptions)ModParticles.FIRE_EMBER.get();
                                        d3 = 0.0;
                                        f = 1.0F;
                                    } else if (breathfxtuning$role == BreathFxTuning.Role.FRAGMENT) {
                                        particleoptions = this.ice
                                            ? (j % 12 == 5 ? BreathVisuals.ICE_CHIP : (ParticleOptions)ModParticles.ICE_SHARD.get())
                                            : (ParticleOptions)ModParticles.FIRE_BREATH.get();
                                        d3 = Math.min(this.ice ? 0.2 : 0.12, d2 / 18.0);
                                        f = this.ice ? 0.65F : 0.6F;
                                    } else {
                                        particleoptions = (ParticleOptions)(this.ice ? (ParticleOptions)ModParticles.ICE_MIST.get() : ParticleTypes.SMOKE);
                                        d3 = Math.min(0.065, d2 / 18.0);
                                        f = 0.65F;
                                    }

                                    Vec3 vec34 = vec33.scale(d3);
                                    if (breathfxtuning$role == BreathFxTuning.Role.FRAGMENT) {
                                        vec34 = vec34.add(this.right.scale(breathscatter$sample.side() * 0.045))
                                            .add(this.up.scale(breathscatter$sample.height() * 0.035))
                                            .add(BreathVisuals.scatter(level, 0.025))
                                            .add(0.0, this.ice ? 0.07 : 0.018, 0.0);
                                    } else if (breathfxtuning$role == BreathFxTuning.Role.HAZE) {
                                        vec34 = vec34.add(0.0, this.ice ? -0.002 : 0.018, 0.0);
                                    }

                                    if (this.ice && breathfxtuning$role == BreathFxTuning.Role.CORE) {
                                        f *= (float)Math.min(1.8, Math.sqrt(Math.max(1.0, this.fan.width(d4) / 1.5)));
                                    }

                                    BreathVisuals.emit(level, particleoptions, vec32, vec34, false, f);
                                }
                            }
                        }
                    }
                }

                if (vec3 != null) {
                    BreathVisuals.impact(level, vec3, this.fan.forward().scale(-1.0), this.ice, false, 0.65F);
                }

                this.age++;
                this.distance = d0;
                return this.distance < this.fan.range();
            }
        }
    }

    private static record ImpactKey(long wave, long tick) {
    }
}
