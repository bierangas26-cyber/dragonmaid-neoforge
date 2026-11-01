package com.example.dragonmaid.combat;

import com.example.dragonmaid.compat.IafCompatConfig;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.util.dragon.DragonUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

final class HumanoidHome {
    private static final int PATH_INTERVAL = 40;
    private static final int MAX_FAILED_PATHS = 3;
    private static final int NO_PROGRESS_TICKS = 200;
    private final DragonBaseEntity dragon;
    private BlockPos home;
    private boolean navigating;
    private int nextPathTick;
    private int failedPaths;
    private int lastProgressTick;
    private int quietSinceTick;
    private double closestDistance;
    private Boolean previousSleepTime;

    HumanoidHome(DragonBaseEntity dragon) {
        this.dragon = dragon;
    }

    void reset() {
        if (this.navigating) {
            this.dragon.getNavigation().stop();
        }

        this.navigating = false;
        this.home = null;
        this.failedPaths = 0;
        this.nextPathTick = 0;
        this.quietSinceTick = this.dragon.tickCount;
        this.previousSleepTime = null;
    }

    private BlockPos currentHome() {
        return this.dragon.isTame() && this.dragon.hasHomePosition && this.dragon.homePos != null && DragonUtils.isInHomeDimension(this.dragon)
            ? this.dragon.homePos.getPosition()
            : null;
    }

    boolean move() {
        BlockPos blockpos = this.currentHome();
        if (blockpos != null && this.dragon.getCommand() == 0 && !this.dragon.isPassenger() && !this.dragon.isVehicle()) {
            if (!blockpos.equals(this.home)) {
                this.reset();
                this.home = blockpos.immutable();
                this.closestDistance = this.dragon.distanceToSqr(Vec3.atCenterOf(this.home));
                this.lastProgressTick = this.dragon.tickCount;
            }

            boolean flag = IafCompatConfig.doDragonsSleep && !this.dragon.isTimeToWake();
            if (this.previousSleepTime == null || this.previousSleepTime != flag) {
                this.failedPaths = 0;
                this.previousSleepTime = flag;
            }

            Vec3 vec3 = Vec3.atCenterOf(this.home);
            double d0 = this.dragon.distanceToSqr(vec3);
            double d1 = Math.max(1.0, (double)this.dragon.getBbWidth() * 10.0);
            double d2 = Math.max(1.0, (double)this.dragon.getRestrictRadius());
            boolean flag1 = flag ? d0 > d1 : d0 > d2 * d2;
            if (!flag1) {
                if (this.navigating) {
                    this.dragon.getNavigation().stop();
                }

                this.navigating = false;
                this.failedPaths = 0;
                this.closestDistance = d0;
                this.lastProgressTick = this.dragon.tickCount;
                return false;
            } else {
                if (d0 + 1.0 < this.closestDistance) {
                    this.closestDistance = d0;
                    this.lastProgressTick = this.dragon.tickCount;
                    this.failedPaths = 0;
                }

                if (this.navigating && (this.dragon.getNavigation().isDone() || this.dragon.tickCount - this.lastProgressTick >= 200)) {
                    this.dragon.getNavigation().stop();
                    this.navigating = false;
                    this.failedPaths++;
                }

                if (this.failedPaths >= 3) {
                    if (flag
                        && this.dragon.tickCount - this.quietSinceTick >= 20
                        && this.dragon.onGround()
                        && !this.dragon.isInWater()
                        && !this.dragon.isFlying()
                        && !this.dragon.isHovering()
                        && this.dragon.getTarget() == null
                        && this.dragon.getPassengers().isEmpty()) {
                        this.dragon.setInSittingPose(true);
                        if (this.dragon.isSleeping()) {
                            return true;
                        }
                    }

                    if (!flag && this.dragon.tickCount >= this.nextPathTick + 200) {
                        this.failedPaths = 0;
                    }

                    return false;
                } else {
                    if (!this.navigating && this.dragon.tickCount >= this.nextPathTick) {
                        this.nextPathTick = this.dragon.tickCount + 40;
                        if (this.dragon.level() instanceof ServerLevel serverlevel) {
                            Vec3 vec31 = d0 > 576.0 ? this.localWaypoint(serverlevel, vec3) : vec3;
                            if (vec31 != null
                                && serverlevel.hasChunkAt(BlockPos.containing(vec31))
                                && this.dragon.getNavigation().moveTo(vec31.x, vec31.y, vec31.z, 1.0)) {
                                this.navigating = true;
                                this.lastProgressTick = this.dragon.tickCount;
                            } else {
                                this.failedPaths++;
                            }
                        } else {
                            this.failedPaths++;
                        }
                    }

                    return true;
                }
            }
        } else {
            this.reset();
            return false;
        }
    }

    private Vec3 localWaypoint(ServerLevel level, Vec3 destination) {
        Vec3 vec3 = this.dragon.position();
        Vec3 vec31 = destination.subtract(vec3);
        double d0 = Math.sqrt(vec31.x * vec31.x + vec31.z * vec31.z);
        if (d0 < 0.01) {
            return null;
        } else {
            double d1 = vec31.x / d0;
            double d2 = vec31.z / d0;
            double[] adouble = new double[]{0.0, 0.45, -0.45};

            for (int i = 0; i < adouble.length; i++) {
                double d3 = adouble[(i + this.failedPaths) % adouble.length];
                double d4 = vec3.x + d1 * 12.0 - d2 * 12.0 * d3;
                double d5 = vec3.z + d2 * 12.0 + d1 * 12.0 * d3;
                Vec3 vec32 = new Vec3(d4, vec3.y + Math.max(-3.0, Math.min(3.0, vec31.y)), d5);
                if (level.hasChunkAt(BlockPos.containing(vec32))) {
                    return vec32;
                }
            }

            return null;
        }
    }

    Vec3 limitWander(Vec3 position) {
        BlockPos blockpos = this.currentHome();
        if (position != null && blockpos != null) {
            double d0 = Math.max(1.0, (double)this.dragon.getRestrictRadius());
            return position.distanceToSqr(Vec3.atCenterOf(blockpos)) <= d0 * d0 ? position : null;
        } else {
            return position;
        }
    }
}
