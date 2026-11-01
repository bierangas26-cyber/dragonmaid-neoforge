package com.example.dragonmaid.combat;

import com.example.dragonmaid.data.HumanoidWater;
import com.example.dragonmaid.network.BreathImpactMessage;
import com.example.dragonmaid.network.BreathWaveMessage;
import com.example.dragonmaid.network.DragonNetwork;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.IceDragonEntity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.event.level.LevelEvent.Unload;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import com.example.dragonmaid.compat.net.PacketDistributor;

@EventBusSubscriber(
    modid = "dragonmaid"
)
public final class MovingBreaths {
    private static final Map<ServerLevel, List<MovingBreaths.Wave>> ACTIVE = new WeakHashMap<>();
    private static final int MAX_PER_DRAGON = 4;
    private static long nextWaveId;

    private MovingBreaths() {
    }

    public static void emit(DragonBaseEntity dragon, LivingEntity target, BreathProfile profile) {
        if (dragon.level() instanceof ServerLevel serverlevel && profile.canHit(dragon, target)) {
            List<MovingBreaths.Wave> list = ACTIVE.computeIfAbsent(serverlevel, ignored -> new ArrayList<>());
            if (list.stream().filter(w -> w.owner.equals(dragon.getUUID())).count() >= 4L) {
                return;
            }

            Vec3 vec3 = HumanoidWater.breathOrigin(dragon);
            Vec3 vec31 = target.getBoundingBox().getCenter().subtract(vec3).normalize();
            if (vec31.lengthSqr() < 0.5) {
                return;
            }

            BreathFan breathfan = new BreathFan(vec3, vec31, profile.range(), profile.halfAngle(), profile.speed());
            list.add(
                new MovingBreaths.Wave(++nextWaveId, dragon.getUUID(), profile.stage(), breathfan, HumanoidCombat.breathDamageAmount(dragon, profile.stage()))
            );
            BreathAnimationSync.shot(dragon, 1);
            DragonNetwork.CHANNEL
                .send(
                    PacketDistributor.TRACKING_ENTITY.with(() -> dragon),
                    new BreathWaveMessage(
                        serverlevel.dimension().location(),
                        breathfan,
                        dragon instanceof IceDragonEntity,
                        serverlevel.getGameTime(),
                        dragon.getId(),
                        dragon.getUUID()
                    )
                );
            return;
        }
    }

    @SubscribeEvent
    public static void tick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel serverlevel) {
            advance(serverlevel);
        }
    }

    public static void advance(ServerLevel level) {
        List<MovingBreaths.Wave> list = ACTIVE.get(level);
        if (list != null) {
            list.removeIf(wave -> !wave.tick(level));
            if (list.isEmpty()) {
                ACTIVE.remove(level);
            }
        }
    }

    @SubscribeEvent
    public static void unload(Unload event) {
        ACTIVE.remove(event.getLevel());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        ACTIVE.clear();
    }

    public static int activeCount(ServerLevel level, UUID owner) {
        return (int)ACTIVE.getOrDefault(level, List.of()).stream().filter(w -> w.owner.equals(owner)).count();
    }

    private static final class Wave {
        private final long id;
        private final UUID owner;
        private final int stage;
        private final BreathFan fan;
        private final float damage;
        private final Set<UUID> hit = new HashSet<>();
        private double distance;
        private int reportedImpacts;

        Wave(long id, UUID owner, int stage, BreathFan fan, float damage) {
            this.id = id;
            this.owner = owner;
            this.stage = stage;
            this.fan = fan;
            this.damage = damage;
        }

        boolean tick(ServerLevel level) {
            if (level.getEntity(this.owner) instanceof DragonBaseEntity entitydragonbase && entitydragonbase.isAlive() && !entitydragonbase.isModelDead()) {
                double d0 = Math.min(this.fan.range(), this.distance + this.fan.speed());
                List<Vec3> list = null;

                for (LivingEntity livingentity : level.getEntitiesOfClass(
                    LivingEntity.class,
                    this.fan.bounds(this.distance, d0),
                    e -> HumanoidTargetGoal.isEnemy(entitydragonbase, e) && !this.hit.contains(e.getUUID())
                )) {
                    if (this.fan.intersects(livingentity.getBoundingBox(), this.distance, d0)
                        && level.clip(new ClipContext(this.fan.origin(), livingentity.getBoundingBox().getCenter(), Block.COLLIDER, Fluid.NONE, entitydragonbase))
                                .getType()
                            == Type.MISS) {
                        this.hit.add(livingentity.getUUID());
                        if (HumanoidCombat.damage(entitydragonbase, livingentity, this.stage, this.damage)
                            && this.reportedImpacts < 24
                            && (list == null || list.size() < 6)) {
                            if (list == null) {
                                list = new ArrayList<>();
                            }

                            list.add(livingentity.getBoundingBox().getCenter());
                            this.reportedImpacts++;
                        }
                    }
                }

                if (list != null) {
                    DragonNetwork.CHANNEL
                        .send(
                            PacketDistributor.TRACKING_ENTITY.with(() -> entitydragonbase),
                            new BreathImpactMessage(level.dimension().location(), this.id, level.getGameTime(), entitydragonbase instanceof IceDragonEntity, list)
                        );
                }

                this.distance = d0;
                return this.distance < this.fan.range();
            }

            return false;
        }
    }
}
