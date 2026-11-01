package com.example.dragonmaid.combat;

import com.example.dragonmaid.animation.TailSweepState;
import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.data.HumanoidLife;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.GorgonEntity;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class HumanoidTailSweep {
    public static final int COOLDOWN_TICKS = 120;
    public static final int ACTION = 6;
    public static final float DAMAGE_MULTIPLIER = 1.5F;
    public static final double REACH_MULTIPLIER = 1.5;
    private final DragonBaseEntity dragon;
    private final Set<UUID> attempted = new HashSet<>();
    private long startedAt = -1L;
    private float yaw;
    private float damage;
    private int previousElapsed = -1;
    private boolean swung;

    public HumanoidTailSweep(DragonBaseEntity dragon) {
        this.dragon = dragon;
    }

    public static double reach(DragonBaseEntity dragon) {
        return HumanoidCombat.clawReach(dragon) * 1.5;
    }

    public static boolean canAct(DragonBaseEntity dragon) {
        return !dragon.level().isClientSide
            && DragonForms.isHumanoid(dragon)
            && dragon.getDragonStage() >= 3
            && dragon.isAlive()
            && !dragon.isRemoved()
            && !dragon.isModelDead()
            && !HumanoidLife.isInjured(dragon)
            && dragon.getCommand() != 1
            && !dragon.isOrderedToSit()
            && !dragon.isSleeping()
            && !dragon.isPassenger()
            && !dragon.isNoAi()
            && !GorgonEntity.isStoneMob(dragon)
            && !dragon.isInWaterOrBubble()
            && !dragon.isInLava();
    }

    public boolean active() {
        return this.startedAt >= 0L;
    }

    public boolean tryStart(LivingEntity target) {
        if (target != null
            && this.dragon.level() instanceof ServerLevel serverlevel
            && canAct(this.dragon)
            && this.dragon.onGround()
            && !this.active()
            && !HumanoidRoar.isRoaring(this.dragon)
            && HumanoidTargetGoal.isEnemy(this.dragon, target)) {
            Vec3 vec3 = target.position().subtract(this.dragon.position());
            float f = vec3.horizontalDistanceSqr() < 1.0E-6 ? this.dragon.getYRot() : (float)(Mth.atan2(vec3.z, vec3.x) * (180.0 / Math.PI)) - 90.0F;
            List<TailSweepGeometry.Frame> list = TailSweepGeometry.attackFrames(this.dragon, f);
            boolean flag = false;

            for (LivingEntity livingentity : serverlevel.getEntitiesOfClass(LivingEntity.class, bounds(list), e -> HumanoidTargetGoal.isEnemy(this.dragon, e))) {
                if (this.dragon.hasLineOfSight(livingentity) && this.contact(serverlevel, livingentity, list, 0, list.size() - 1) != null) {
                    flag = true;
                    break;
                }
            }

            if (!flag) {
                return false;
            } else {
                this.yaw = f;
                this.startedAt = serverlevel.getGameTime();
                this.previousElapsed = -1;
                this.swung = false;
                this.damage = (float)this.dragon.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.5F;
                this.attempted.clear();
                HumanoidCombat.stopBreath(this.dragon);
                this.dragon.setDeltaMovement(0.0, this.dragon.getDeltaMovement().y, 0.0);
                TailSweepSync.begin(this.dragon, this.yaw);
                this.holdPose();
                return true;
            }
        } else {
            return false;
        }
    }

    public boolean tick() {
        if (!this.active()) {
            return false;
        } else {
            long i = this.dragon.level().getGameTime() - this.startedAt;
            if (canAct(this.dragon) && i >= 0L && i >= (long)this.previousElapsed && TailSweepSync.isBusy(this.dragon)) {
                int j = (int)i;
                this.holdPose();
                if (!this.swung && j >= TailSweepState.SWING_TICK && j <= TailSweepState.HIT_END) {
                    this.dragon.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.9F, 0.8F);
                    this.swung = true;
                }

                if (j >= TailSweepState.HIT_START
                    && j <= TailSweepState.HIT_END
                    && j != this.previousElapsed
                    && this.dragon.level() instanceof ServerLevel serverlevel) {
                    List<TailSweepGeometry.Frame> list = TailSweepGeometry.attackFrames(this.dragon, this.yaw);
                    int k = TailSweepState.hitFrame(j - 1);
                    int l = TailSweepState.hitFrame(j);
                    this.wind(serverlevel, list, k, l);
                    this.hit(serverlevel, list, k, l);
                }

                this.previousElapsed = j;
                return true;
            } else {
                this.cancel();
                return false;
            }
        }
    }

    public void cancel() {
        if (this.active()) {
            TailSweepSync.cancel(this.dragon);
            this.startedAt = -1L;
            this.previousElapsed = -1;
            this.attempted.clear();
            if (DragonForms.state(this.dragon).dragonmaid$getAction() == 6) {
                DragonForms.state(this.dragon).dragonmaid$setAction(0);
            }
        }
    }

    private void holdPose() {
        this.dragon.getNavigation().stop();
        this.dragon.setSpeed(0.0F);
        this.dragon.xxa = this.dragon.zza = 0.0F;
        this.dragon.setYRot(this.yaw);
        this.dragon.yRotO = this.yaw;
        this.dragon.yBodyRot = this.dragon.yBodyRotO = this.yaw;
        this.dragon.yHeadRot = this.dragon.yHeadRotO = this.yaw;
        this.dragon
            .getLookControl()
            .setLookAt(
                this.dragon.getX() - (double)Mth.sin(this.yaw * (float) (Math.PI / 180.0)),
                this.dragon.getEyeY(),
                this.dragon.getZ() + (double)Mth.cos(this.yaw * (float) (Math.PI / 180.0)),
                30.0F,
                30.0F
            );
        DragonForms.state(this.dragon).dragonmaid$setAction(6);
    }

    private void hit(ServerLevel level, List<TailSweepGeometry.Frame> frames, int from, int to) {
        for (LivingEntity livingentity : level.getEntitiesOfClass(
            LivingEntity.class, bounds(frames.subList(from, to + 1)), e -> HumanoidTargetGoal.isEnemy(this.dragon, e)
        )) {
            if (!this.attempted.contains(livingentity.getUUID())
                && HumanoidTargetGoal.isEnemy(this.dragon, livingentity)
                && this.dragon.hasLineOfSight(livingentity)) {
                Vec3 vec3 = this.contact(level, livingentity, frames, from, to);
                if (vec3 != null) {
                    this.attempted.add(livingentity.getUUID());
                    if (TailSweepKnockback.hurt(livingentity, level.damageSources().mobAttack(this.dragon), this.damage)) {
                        level.sendParticles(ParticleTypes.CRIT, vec3.x, vec3.y, vec3.z, 8, 0.12, 0.12, 0.12, 0.14);
                        level.playSound(
                            null,
                            livingentity.getX(),
                            livingentity.getY(),
                            livingentity.getZ(),
                            SoundEvents.PLAYER_ATTACK_STRONG,
                            SoundSource.NEUTRAL,
                            0.65F,
                            0.85F
                        );
                    }
                }
            }
        }
    }

    private void wind(ServerLevel level, List<TailSweepGeometry.Frame> frames, int from, int to) {
        int i = from == 0 ? 0 : from + 1;

        for (int j = i; j <= to; j++) {
            Vec3 vec3 = frames.get(j).trailPoint();
            Vec3 vec31 = vec3.subtract(frames.get(Math.max(0, j - 1)).trailPoint()).normalize().scale(0.035);
            level.sendParticles(ParticleTypes.POOF, vec3.x, vec3.y, vec3.z, 0, vec31.x, 0.01, vec31.z, 1.0);
        }

        Vec3 vec32 = frames.get((from + to) / 2).trailPoint();
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, vec32.x, vec32.y, vec32.z, 0, 1.1, 0.0, 0.0, 1.0);
    }

    private Vec3 contact(ServerLevel level, LivingEntity enemy, List<TailSweepGeometry.Frame> frames, int from, int to) {
        for (int i = from; i <= to; i++) {
            List<TailSweepGeometry.Sphere> list = frames.get(i).spheres();
            List<TailSweepGeometry.Sphere> list1 = frames.get(Math.max(from, i - 1)).spheres();

            for (int j = 0; j < list.size(); j++) {
                TailSweepGeometry.Sphere tailsweepgeometry$sphere = list1.get(j);
                TailSweepGeometry.Sphere tailsweepgeometry$sphere1 = list.get(j);
                Vec3 vec3 = TailSweepCollision.contact(
                    tailsweepgeometry$sphere.center(),
                    tailsweepgeometry$sphere1.center(),
                    enemy.getBoundingBox(),
                    Math.max(tailsweepgeometry$sphere.radius(), tailsweepgeometry$sphere1.radius())
                );
                if (vec3 != null) {
                    Vec3 vec31 = new Vec3(this.dragon.getX(), vec3.y, this.dragon.getZ());
                    if (level.clip(new ClipContext(vec31, vec3, Block.COLLIDER, Fluid.NONE, this.dragon)).getType() == Type.MISS) {
                        return vec3;
                    }
                }
            }
        }

        return null;
    }

    private static AABB bounds(List<TailSweepGeometry.Frame> frames) {
        AABB aabb = null;

        for (int i = 0; i < frames.size(); i++) {
            List<TailSweepGeometry.Sphere> list = frames.get(i).spheres();
            List<TailSweepGeometry.Sphere> list1 = frames.get(Math.max(0, i - 1)).spheres();
            List<TailSweepGeometry.Sphere> list2 = frames.get(Math.min(frames.size() - 1, i + 1)).spheres();

            for (int j = 0; j < list.size(); j++) {
                TailSweepGeometry.Sphere tailsweepgeometry$sphere = list.get(j);
                double d0 = Math.max(tailsweepgeometry$sphere.radius(), Math.max(list1.get(j).radius(), list2.get(j).radius()));
                Vec3 vec3 = tailsweepgeometry$sphere.center();
                AABB aabb1 = new AABB(vec3, vec3).inflate(d0);
                aabb = aabb == null ? aabb1 : aabb.minmax(aabb1);
            }
        }

        return aabb;
    }
}
