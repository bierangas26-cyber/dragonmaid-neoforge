package com.example.dragonmaid.combat;

import com.example.dragonmaid.compat.HumanoidForge;
import com.example.dragonmaid.data.DragonForms;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.util.List;
import java.util.UUID;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

public final class HumanoidIdleLook {
    public static final double RANGE = 6.0;
    public static final int SCAN_INTERVAL = 20;
    public static final int MIN_LOOK_TICKS = 40;
    public static final int LOOK_TICK_VARIATION = 40;
    public static final int MIN_PAUSE_TICKS = 20;
    public static final int PAUSE_TICK_VARIATION = 60;
    public static final float MAX_YAW = 45.0F;
    public static final float MAX_PITCH = 25.0F;
    private final DragonBaseEntity dragon;
    private LivingEntity watched;
    private UUID lastWatched;
    private int lookUntilTick;
    private int nextAttemptTick;
    private int nextVisibilityTick;

    public HumanoidIdleLook(DragonBaseEntity dragon) {
        this.dragon = dragon;
    }

    public static boolean eligible(DragonBaseEntity dragon) {
        double d0 = dragon.getX() - dragon.xo;
        double d1 = dragon.getZ() - dragon.zo;
        return !dragon.level().isClientSide
            && DragonForms.isHumanoid(dragon)
            && dragon.isAlive()
            && !dragon.isRemoved()
            && !dragon.isModelDead()
            && !dragon.isNoAi()
            && !dragon.isSleeping()
            && dragon.getCommand() != 1
            && !DragonForms.isSeated(dragon)
            && !dragon.isPassenger()
            && !dragon.isVehicle()
            && dragon.onGround()
            && !dragon.isInWaterOrBubble()
            && !dragon.isInLava()
            && dragon.getNavigation().isDone()
            && d0 * d0 + d1 * d1 < 4.0E-4
            && dragon.getDeltaMovement().horizontalDistanceSqr() < 4.0E-4
            && dragon.getTarget() == null
            && !dragon.isTackling()
            && !dragon.isBreathingFire()
            && dragon.burningTarget == null
            && DragonForms.state(dragon).dragonmaid$getAction() == 0
            && dragon.getAttackAnim(1.0F) <= 0.0F
            && !HumanoidRoar.isRoaring(dragon)
            && !TailSweepSync.isBusy(dragon)
            && !HumanoidForge.isWorking(dragon);
    }

    private boolean valid(LivingEntity target) {
        return target != null
            && target != this.dragon
            && target.level() == this.dragon.level()
            && target.isAlive()
            && !target.isRemoved()
            && !target.isSpectator()
            && !target.isInvisible()
            && target.canBeSeenByAnyone()
            && this.dragon.distanceToSqr(target) <= 36.0;
    }

    private void pause() {
        this.watched = null;
        this.nextAttemptTick = this.dragon.tickCount + 20 + this.dragon.getRandom().nextInt(60);
        DragonForms.state(this.dragon).dragonmaid$setIdleLooking(false);
    }

    public boolean tick() {
        if (!eligible(this.dragon)) {
            this.reset();
            return false;
        } else {
            int i = this.dragon.tickCount;
            if (this.watched != null) {
                if (i >= this.lookUntilTick || !this.valid(this.watched)) {
                    this.pause();
                    return false;
                }

                if (i >= this.nextVisibilityTick) {
                    this.nextVisibilityTick = i + 20;
                    if (!this.dragon.hasLineOfSight(this.watched)) {
                        this.pause();
                        return false;
                    }
                }
            }

            if (this.watched == null) {
                if (i < this.nextAttemptTick) {
                    return false;
                }

                List<LivingEntity> list = this.dragon
                    .level()
                    .getEntitiesOfClass(LivingEntity.class, this.dragon.getBoundingBox().inflate(6.0, 3.0, 6.0), target -> this.valid(target) && this.dragon.hasLineOfSight(target));
                if (list.isEmpty()) {
                    this.pause();
                    return false;
                }

                if (list.size() > 1 && this.lastWatched != null) {
                    list.removeIf(target -> this.lastWatched.equals(target.getUUID()));
                }

                this.watched = list.get(this.dragon.getRandom().nextInt(list.size()));
                this.lastWatched = this.watched.getUUID();
                this.lookUntilTick = i + 40 + this.dragon.getRandom().nextInt(40);
                this.nextVisibilityTick = i + 20;
            }

            double d4 = this.watched.getX() - this.dragon.getX();
            double d0 = this.watched.getZ() - this.dragon.getZ();
            double d1 = Math.sqrt(d4 * d4 + d0 * d0);
            if (d1 < 1.0E-4) {
                this.pause();
                return false;
            } else {
                float f = (float)(Mth.atan2(d0, d4) * 180.0F / (float)Math.PI) - 90.0F;
                float f1 = Mth.clamp(Mth.wrapDegrees(f - this.dragon.yBodyRot), -45.0F, 45.0F);
                double d2 = (double)((this.dragon.yBodyRot + f1) * (float) (Math.PI / 180.0));
                float f2 = (float)(Mth.atan2(this.watched.getEyeY() - this.dragon.getEyeY(), d1) * 180.0F / (float)Math.PI);
                double d3 = Math.tan((double)(Mth.clamp(f2, -25.0F, 25.0F) * (float) (Math.PI / 180.0))) * d1;
                this.dragon
                    .getLookControl()
                    .setLookAt(this.dragon.getX() - Math.sin(d2) * d1, this.dragon.getEyeY() + d3, this.dragon.getZ() + Math.cos(d2) * d1, 10.0F, 8.0F);
                DragonForms.state(this.dragon).dragonmaid$setIdleLooking(true);
                return true;
            }
        }
    }

    public void reset() {
        if (!this.dragon.level().isClientSide) {
            if (this.watched != null) {
                this.pause();
            } else {
                DragonForms.state(this.dragon).dragonmaid$setIdleLooking(false);
            }
        } else {
            this.watched = null;
        }
    }
}
