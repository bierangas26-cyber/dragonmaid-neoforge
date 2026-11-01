package com.example.dragonmaid.combat;

import com.example.dragonmaid.DragonMaidConfig;
import com.example.dragonmaid.data.HumanoidWater;
import com.example.dragonmaid.movement.HumanoidHazards;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.github.alexthe666.iceandfire.entity.props.EntityData;
import com.github.alexthe666.iceandfire.entity.props.EntityDataProvider;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class GuardEscort {
    private static final double FOLLOW_DISTANCE = 3.0;
    private final DragonBaseEntity dragon;
    private boolean returning;
    private int pathCooldown;
    private int teleportCooldown;
    private Vec3 failedOwnerPosition;
    private Vec3 failedTargetPosition;

    public GuardEscort(DragonBaseEntity dragon) {
        this.dragon = dragon;
    }

    public static LivingEntity owner(DragonBaseEntity dragon) {
        LivingEntity livingentity = dragon.getOwner();
        return dragon.getCommand() == 2
                && livingentity != null
                && livingentity.isAlive()
                && !livingentity.isSpectator()
                && livingentity.level() == dragon.level()
            ? livingentity
            : null;
    }

    public static boolean reachableTarget(DragonBaseEntity dragon, LivingEntity target) {
        LivingEntity livingentity = owner(dragon);
        if (livingentity == null) {
            return true;
        } else {
            double d0 = WildDragonCombat.isHostileWildDragon(dragon, target)
                ? WildDragonCombat.targetRange()
                : (Double)DragonMaidConfig.GUARD_COMBAT_RADIUS.get() + BreathProfile.forDragon(dragon).range();
            return WildDragonCombat.distanceToSqr(dragon, livingentity, target) <= d0 * d0;
        }
    }

    public void reset() {
        this.returning = false;
        this.pathCooldown = 0;
        this.teleportCooldown = 0;
        this.clearFailedPositions();
    }

    public void targetChanged() {
        this.dragon.getNavigation().stop();
        this.pathCooldown = 0;
        this.clearFailedPositions();
    }

    public void tryTeleport(LivingEntity owner) {
        if (this.teleportCooldown > 0) {
            this.teleportCooldown--;
        } else {
            double d0 = Math.max((Double)DragonMaidConfig.GUARD_TELEPORT_DISTANCE.get(), (Double)DragonMaidConfig.GUARD_COMBAT_RADIUS.get() + 1.0);
            if (!(this.dragon.level() instanceof ServerLevel serverlevel)
                || this.dragon.distanceToSqr(owner) <= d0 * d0
                || this.dragon.isLeashed()
                || hasChain(this.dragon)
                || this.dragon.isPassenger()
                || this.dragon.isVehicle()) {
                return;
            }

            if (!HumanoidWater.canDive(this.dragon) || !owner.isInWater()) {
                this.teleportCooldown = 10;
                BlockPos blockpos1 = owner.blockPosition();

                for (int i = 0; i < 10; i++) {
                    BlockPos blockpos = blockpos1.offset(
                        this.dragon.getRandom().nextInt(7) - 3, this.dragon.getRandom().nextInt(3) - 1, this.dragon.getRandom().nextInt(7) - 3
                    );
                    if ((
                            !(Math.abs((double)blockpos.getX() - owner.getX()) < 2.0)
                                || !(Math.abs((double)blockpos.getZ() - owner.getZ()) < 2.0)
                        )
                        && serverlevel.hasChunkAt(blockpos)
                        && !serverlevel.isOutsideBuildHeight(blockpos)
                        && HumanoidHazards.pathType(
                                this.dragon,
                                serverlevel,
                                blockpos.getX(),
                                blockpos.getY(),
                                blockpos.getZ(),
                                WalkNodeEvaluator.getPathTypeStatic(new net.minecraft.world.level.pathfinder.PathfindingContext(serverlevel, this.dragon), blockpos.mutable())
                            )
                            == PathType.WALKABLE
                        && !(serverlevel.getBlockState(blockpos.below()).getBlock() instanceof LeavesBlock)) {
                        Vec3 vec3 = new Vec3((double)blockpos.getX() + 0.5, (double)blockpos.getY(), (double)blockpos.getZ() + 0.5);
                        AABB aabb = this.dragon.getBoundingBox().move(vec3.subtract(this.dragon.position()));
                        if (!(aabb.maxY > (double)serverlevel.getMaxBuildHeight())
                            && serverlevel.getWorldBorder().isWithinBounds(aabb)
                            && serverlevel.noCollision(this.dragon, aabb)
                            && !serverlevel.containsAnyLiquid(aabb)) {
                            this.dragon.moveTo(vec3.x, vec3.y, vec3.z, this.dragon.getYRot(), this.dragon.getXRot());
                            this.dragon.setDeltaMovement(Vec3.ZERO);
                            this.dragon.fallDistance = 0.0F;
                            this.dragon.getNavigation().stop();
                            this.reset();
                            this.teleportCooldown = 10;
                            return;
                        }
                    }
                }
            }
        }
    }

    public static boolean hasChain(DragonBaseEntity dragon) {
        return EntityDataProvider.getCapability(dragon).map(data -> {
            if (data != null && data.chainData != null) {
                List<Entity> list = data.chainData.getChainedTo();
                return list != null && !list.isEmpty();
            } else {
                return false;
            }
        }).orElse(false);
    }

    public void move(LivingEntity owner, LivingEntity target, BreathProfile profile, boolean pursueMelee, double clawReach) {
        if (this.pathCooldown > 0) {
            this.pathCooldown--;
        }

        double d0 = (Double)DragonMaidConfig.GUARD_COMBAT_RADIUS.get();
        double d1 = this.dragon.distanceToSqr(owner);
        if (this.failedOwnerPosition != null
            && (
                d1 > 9.0
                    || this.failedOwnerPosition.distanceToSqr(owner.position()) >= 1.0
                    || target != null && this.failedTargetPosition.distanceToSqr(target.position()) >= 1.0
            )) {
            this.pathCooldown = 0;
            this.clearFailedPositions();
        }

        if (d1 > d0 * d0 && !this.returning) {
            this.returning = true;
            this.dragon.getNavigation().stop();
            this.pathCooldown = 0;
        }

        if (this.returning && d1 <= 9.0) {
            this.returning = false;
        }

        if (!this.returning && !this.withinBoundary(this.dragon.getNavigation().getPath(), owner, d0)) {
            this.dragon.getNavigation().stop();
            this.pathCooldown = 0;
        }

        if (!this.returning && target != null) {
            if (pursueMelee && (double)this.dragon.distanceTo(target) <= clawReach && this.dragon.hasLineOfSight(target)) {
                this.dragon.getNavigation().stop();
            } else if (!pursueMelee && profile.canHit(this.dragon, target)) {
                this.dragon.getNavigation().stop();
            } else {
                Vec3 vec3 = target.position().subtract(owner.position());
                double d2 = vec3.length();
                double d3 = pursueMelee ? d2 : Math.max(0.0, d2 - profile.range() * 0.85);
                d3 = Math.min(d0 - 1.0, d3);
                Vec3 vec31 = owner.position().add(vec3.normalize().scale(d3));
                if (!pursueMelee) {
                    vec31 = new Vec3(vec31.x, owner.getY(), vec31.z);
                }

                if (this.pathCooldown <= 0) {
                    this.pathCooldown = 10;
                    this.clearFailedPositions();
                    Vec3 vec32 = new Vec3(-vec3.z, 0.0, vec3.x).normalize();
                    if (vec32.lengthSqr() < 0.5) {
                        vec32 = new Vec3(1.0, 0.0, 0.0);
                    }

                    Path path = null;

                    for (int i : new int[]{0, 2, -2, 4, -4}) {
                        if (pursueMelee && i != 0) {
                            break;
                        }

                        Vec3 vec33 = vec31.add(vec32.scale((double)i));
                        if (!(vec33.distanceToSqr(owner.position()) > d0 * d0)) {
                            Path path1 = this.dragon.getNavigation().createPath(vec33.x, vec33.y, vec33.z, 0);
                            if (path1 != null && path1.getEndNode() != null && this.withinBoundary(path1, owner, d0)) {
                                if (path == null) {
                                    path = path1;
                                }

                                Node node = path1.getEndNode();
                                Vec3 vec34 = new Vec3(
                                    (double)node.x + 0.5, (double)((float)node.y + this.dragon.getEyeHeight()), (double)node.z + 0.5
                                );
                                Vec3 vec35 = target.getBoundingBox().getCenter();
                                if (pursueMelee
                                    || vec34.distanceTo(vec35) <= profile.range()
                                        && this.dragon.level().clip(new ClipContext(vec34, vec35, Block.COLLIDER, Fluid.NONE, this.dragon)).getType()
                                            == Type.MISS) {
                                    this.dragon.getNavigation().moveTo(path1, 1.2);
                                    return;
                                }
                            }
                        }
                    }

                    if (path != null) {
                        this.dragon.getNavigation().moveTo(path, 1.2);
                    } else {
                        this.dragon.getNavigation().stop();
                        if (d1 > 9.0) {
                            this.pathCooldown = 0;
                            this.follow(owner, d0);
                        } else {
                            this.failedOwnerPosition = owner.position();
                            this.failedTargetPosition = target.position();
                        }
                    }
                }
            }
        } else {
            this.follow(owner, d0);
        }
    }

    private void clearFailedPositions() {
        this.failedOwnerPosition = null;
        this.failedTargetPosition = null;
    }

    private void follow(LivingEntity owner, double radius) {
        if (this.dragon.distanceToSqr(owner) <= 9.0) {
            this.dragon.getNavigation().stop();
        } else if (this.pathCooldown == 0) {
            this.pathCooldown = 10;
            Path path = this.dragon.getNavigation().createPath(owner, 0);
            if (path == null || !this.returning && !this.withinBoundary(path, owner, radius)) {
                this.dragon.getNavigation().stop();
            } else {
                this.dragon.getNavigation().moveTo(path, 1.2);
            }
        }
    }

    private boolean withinBoundary(Path path, LivingEntity owner, double radius) {
        if (path == null) {
            return true;
        } else {
            for (int i = path.getNextNodeIndex(); i < path.getNodeCount(); i++) {
                Node node = path.getNode(i);
                if (owner.position().distanceToSqr(new Vec3((double)node.x + 0.5, (double)node.y, (double)node.z + 0.5)) > radius * radius) {
                    return false;
                }
            }

            return true;
        }
    }
}
