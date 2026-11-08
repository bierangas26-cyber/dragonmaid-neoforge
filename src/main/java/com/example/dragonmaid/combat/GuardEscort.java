package com.example.dragonmaid.combat;

import com.example.dragonmaid.DragonMaidConfig;
import com.example.dragonmaid.accessory.MiniatureForm;
import com.example.dragonmaid.command.CompanionTeleport;
import com.example.dragonmaid.data.HumanoidWater;
import com.example.dragonmaid.inventory.DragonBagPickupGoal;
import com.example.dragonmaid.movement.HumanoidHazards;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import iafcompat.EntityDataCompat;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
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
import net.minecraft.world.level.pathfinder.PathfindingContext;

public final class GuardEscort {
   private static final double FOLLOW_DISTANCE = 3.0;
   private final DragonBaseEntity dragon;
   private boolean returning;
   private int pathCooldown;
   private int teleportCooldown;
   private int nextPatrolTick;
   private boolean patrolling;
   private boolean settling;
   private boolean ownerFollowing;
   private boolean followingOwner;
   private boolean combatReturning;
   private LivingEntity motionOwner;
   private double lastOwnerX;
   private double lastOwnerZ;
   private double ownerTravel;
   private int ownerStillTicks;
   private Vec3 combatReturnOwnerPosition;
   private Vec3 followPathOwnerPosition;
   private double returnApproachDistance;
   private int nextChainCheckTick;
   private boolean idleChained;
   private Vec3 failedOwnerPosition;
   private Vec3 failedTargetPosition;

   public GuardEscort(DragonBaseEntity dragon) {
      this.dragon = dragon;
   }

   public static LivingEntity owner(DragonBaseEntity dragon) {
      LivingEntity owner = dragon.getOwner();
      return dragon.getCommand() == 2 && owner != null && owner.isAlive() && !owner.isSpectator() && owner.level() == dragon.level() ? owner : null;
   }

   public static boolean reachableTarget(DragonBaseEntity dragon, LivingEntity target) {
      LivingEntity owner = owner(dragon);
      if (owner == null) {
         return true;
      } else if (MiniatureForm.hasDragonEffect(dragon)) {
         double radius = MiniatureEscortGoal.combatRadius();
         return WildDragonCombat.distanceToSqr(dragon, owner, target) <= radius * radius;
      } else {
         double reach = WildDragonCombat.isHostileWildDragon(dragon, target)
            ? WildDragonCombat.targetRange()
            : (Double)DragonMaidConfig.GUARD_COMBAT_RADIUS.get() + BreathProfile.forDragon(dragon).range();
         return WildDragonCombat.distanceToSqr(dragon, owner, target) <= reach * reach;
      }
   }

   public void reset() {
      if (this.patrolling || this.settling || this.returning || this.followingOwner || this.combatReturning) {
         this.dragon.getNavigation().stop();
      }

      this.patrolling = false;
      this.settling = false;
      this.nextPatrolTick = 0;
      this.nextChainCheckTick = 0;
      this.idleChained = false;
      this.returning = false;
      this.ownerFollowing = false;
      this.followingOwner = false;
      this.combatReturning = false;
      this.combatReturnOwnerPosition = null;
      this.followPathOwnerPosition = null;
      this.motionOwner = null;
      this.ownerTravel = 0.0;
      this.ownerStillTicks = 0;
      this.pathCooldown = 0;
      this.teleportCooldown = 0;
      this.clearFailedPositions();
   }

   public void targetChanged() {
      this.dragon.getNavigation().stop();
      this.patrolling = false;
      this.settling = false;
      this.returning = false;
      this.followingOwner = false;
      this.combatReturning = false;
      this.combatReturnOwnerPosition = null;
      this.followPathOwnerPosition = null;
      this.nextPatrolTick = 0;
      this.pathCooldown = 0;
      this.clearFailedPositions();
   }

   public void tryTeleport(LivingEntity owner) {
      if (CompanionTeleport.allowed(this.dragon)) {
         if (this.teleportCooldown > 0) {
            this.teleportCooldown--;
         } else {
            double threshold = Math.max((Double)DragonMaidConfig.GUARD_TELEPORT_DISTANCE.get(), (Double)DragonMaidConfig.GUARD_COMBAT_RADIUS.get() + 1.0);
            if (this.dragon.level() instanceof ServerLevel level
               && !(this.dragon.distanceToSqr(owner) <= threshold * threshold)
               && !this.dragon.isLeashed()
               && !hasChain(this.dragon)
               && !this.dragon.isPassenger()
               && !this.dragon.isVehicle()) {
               if (owner.isInWaterOrBubble()
                  && !HumanoidWater.canDive(this.dragon)
                  && owner.getY() < this.dragon.getY()
                  && this.dragon.position().subtract(owner.position()).horizontalDistanceSqr() <= 64.0
                  && (this.dragon.isInWaterOrBubble() || level.getFluidState(this.dragon.blockPosition().below()).is(FluidTags.WATER))
                  && level.getFluidState(BlockPos.containing(this.dragon.getX(), this.dragon.getBoundingBox().maxY, this.dragon.getZ())).isEmpty()) {
                  this.teleportCooldown = 10;
                  AABB surfaceSpace = this.dragon.getBoundingBox().inflate(0.5, 0.0, 0.5).expandTowards(0.0, 1.0, 0.0);
                  if (this.loadedAndSafe(level, surfaceSpace) && level.noCollision(this.dragon, surfaceSpace)) {
                     return;
                  }
               }

               this.teleportCooldown = 10;
               BlockPos anchor = owner.blockPosition();

               for (int attempt = 0; attempt < 10; attempt++) {
                  BlockPos feet = anchor.offset(
                     this.dragon.getRandom().nextInt(7) - 3, this.dragon.getRandom().nextInt(3) - 1, this.dragon.getRandom().nextInt(7) - 3
                  );
                  if ((!(Math.abs((double)feet.getX() - owner.getX()) < 2.0) || !(Math.abs((double)feet.getZ() - owner.getZ()) < 2.0))
                     && level.hasChunkAt(feet)
                     && !level.isOutsideBuildHeight(feet)
                     && !(level.getBlockState(feet.below()).getBlock() instanceof LeavesBlock)) {
                     Vec3 destination = new Vec3((double)feet.getX() + 0.5, (double)feet.getY(), (double)feet.getZ() + 0.5);
                     AABB box = this.dragon.getBoundingBox().move(destination.subtract(this.dragon.position()));
                     if (!(box.maxY > (double)level.getMaxBuildHeight())
                        && this.loadedAndSafe(level, box)
                        && HumanoidHazards.pathType(
                              this.dragon, level, feet.getX(), feet.getY(), feet.getZ(), WalkNodeEvaluator.getPathTypeStatic(new PathfindingContext(level, this.dragon), feet.mutable())
                           )
                           == PathType.WALKABLE
                        && level.getWorldBorder().isWithinBounds(box)
                        && level.noCollision(this.dragon, box)
                        && !level.containsAnyLiquid(box)) {
                        this.finishTeleport(destination);
                        return;
                     }
                  }
               }

               if (!owner.isInWaterOrBubble()) {
                  return;
               }

               boolean canDive = HumanoidWater.canDive(this.dragon);

               for (int attemptx = 0; attemptx < (canDive ? 10 : 4); attemptx++) {
                  BlockPos column = anchor.offset(this.dragon.getRandom().nextInt(7) - 3, 0, this.dragon.getRandom().nextInt(7) - 3);
                  if ((!(Math.abs((double)column.getX() - owner.getX()) < 2.0) || !(Math.abs((double)column.getZ() - owner.getZ()) < 2.0))
                     && level.hasChunkAt(column)) {
                     int firstY = canDive ? -1 : 0;
                     int lastY = canDive ? 1 : 64;

                     for (int dy = firstY; dy <= lastY; dy++) {
                        BlockPos feet = column.offset(0, dy, 0);
                        if (!level.isOutsideBuildHeight(feet)
                           && (canDive ? level.getFluidState(feet).is(FluidTags.WATER) : level.getFluidState(feet.below()).is(FluidTags.WATER))) {
                           Vec3 destination = new Vec3((double)feet.getX() + 0.5, (double)feet.getY(), (double)feet.getZ() + 0.5);
                           AABB box = this.dragon.getBoundingBox().move(destination.subtract(this.dragon.position()));
                           if (!(box.minY < (double)level.getMinBuildHeight())
                              && !(box.maxY > (double)level.getMaxBuildHeight())
                              && this.loadedAndSafe(level, box)
                              && level.getWorldBorder().isWithinBounds(box)
                              && level.noCollision(this.dragon, box)
                              && (canDive || !level.containsAnyLiquid(box))) {
                              this.finishTeleport(destination);
                              return;
                           }
                        }
                     }
                  }
               }

               return;
            }
         }
      }
   }

   private boolean loadedAndSafe(ServerLevel level, AABB box) {
      int minX = Mth.floor(box.minX);
      int maxX = Mth.floor(box.maxX - 1.0E-7);
      int minY = Mth.floor(box.minY);
      int maxY = Mth.floor(box.maxY - 1.0E-7);
      int minZ = Mth.floor(box.minZ);
      int maxZ = Mth.floor(box.maxZ - 1.0E-7);

      for (int cx = Math.floorDiv(minX - 1, 16); cx <= Math.floorDiv(maxX + 1, 16); cx++) {
         for (int cz = Math.floorDiv(minZ - 1, 16); cz <= Math.floorDiv(maxZ + 1, 16); cz++) {
            if (!level.hasChunkAt(new BlockPos(cx * 16, minY, cz * 16))) {
               return false;
            }
         }
      }

      for (BlockPos pos : BlockPos.betweenClosed(minX, minY - 1, minZ, maxX, maxY, maxZ)) {
         if (level.isOutsideBuildHeight(pos)) {
            return false;
         }

         if (level.getFluidState(pos).is(FluidTags.LAVA)) {
            return false;
         }

         PathType type = HumanoidHazards.pathType(
            this.dragon, level, pos.getX(), pos.getY(), pos.getZ(), WalkNodeEvaluator.getPathTypeStatic(new PathfindingContext(level, this.dragon), pos.mutable())
         );
         if (type == PathType.LAVA || type.name().startsWith("DANGER_") || type.name().startsWith("DAMAGE_")) {
            return false;
         }
      }

      return true;
   }

   private void finishTeleport(Vec3 destination) {
      this.dragon.moveTo(destination.x, destination.y, destination.z, this.dragon.getYRot(), this.dragon.getXRot());
      this.dragon.setDeltaMovement(Vec3.ZERO);
      this.dragon.fallDistance = 0.0F;
      this.dragon.getNavigation().stop();
      this.reset();
      this.teleportCooldown = 10;
   }

   public static boolean hasChain(DragonBaseEntity dragon) {
      // IAF CE 2.2 删除了 citadel 的 EntityData/chainData（1.20.1 的锁链能力），本 mod 未使用锁链数据，恒为 false。
      return false;
   }

   public void move(LivingEntity owner, LivingEntity target, BreathProfile profile, boolean pursueMelee, double clawReach) {
      if (this.pathCooldown > 0) {
         this.pathCooldown--;
      }

      this.updateOwnerMotion(owner);
      double radius = (Double)DragonMaidConfig.GUARD_COMBAT_RADIUS.get();
      double ownerDistance = this.dragon.distanceToSqr(owner);
      if (target == null) {
         this.clearFailedPositions();
         if (!DragonBagPickupGoal.deferEscort(this.dragon, owner)) {
            this.patrol(owner, radius, ownerDistance);
         }
      } else {
         if (this.failedOwnerPosition != null
            && (this.failedOwnerPosition.distanceToSqr(owner.position()) >= 1.0 || target != null && this.failedTargetPosition.distanceToSqr(target.position()) >= 1.0)) {
            this.pathCooldown = 0;
            this.clearFailedPositions();
         }

         if (ownerDistance > radius * radius && !this.combatReturning) {
            this.combatReturning = true;
            this.combatReturnOwnerPosition = null;
            this.dragon.getNavigation().stop();
            this.pathCooldown = 0;
         }

         if (this.combatReturning) {
            double inner = radius * 0.72;
            double arrived = Math.min(radius - 0.1, inner + 1.0);
            if (!(ownerDistance <= arrived * arrived) || !this.dragon.getNavigation().isDone()) {
               this.combatReturn(owner, inner);
               return;
            }

            this.combatReturning = false;
            this.pathCooldown = 0;
         }

         if (!this.withinBoundary(this.dragon.getNavigation().getPath(), owner, radius)) {
            this.dragon.getNavigation().stop();
            this.pathCooldown = 0;
         }

         if (pursueMelee && (double)this.dragon.distanceTo(target) <= clawReach && this.dragon.hasLineOfSight(target)) {
            this.dragon.getNavigation().stop();
         } else if (!pursueMelee && profile.canHit(this.dragon, target)) {
            this.dragon.getNavigation().stop();
         } else {
            Vec3 fromOwner = target.position().subtract(owner.position());
            double distance = fromOwner.length();
            double advance = pursueMelee ? distance : Math.max(0.0, distance - profile.range() * 0.85);
            advance = Math.min(radius - 1.0, advance);
            Vec3 destination = owner.position().add(fromOwner.normalize().scale(advance));
            if (!pursueMelee) {
               destination = new Vec3(destination.x, owner.getY(), destination.z);
            }

            if (this.pathCooldown <= 0) {
               this.pathCooldown = 10;
               this.clearFailedPositions();
               Vec3 side = new Vec3(-fromOwner.z, 0.0, fromOwner.x).normalize();
               if (side.lengthSqr() < 0.5) {
                  side = new Vec3(1.0, 0.0, 0.0);
               }

               Path fallback = null;

               for (int offset : new int[]{0, 2, -2, 4, -4}) {
                  if (pursueMelee && offset != 0) {
                     break;
                  }

                  Vec3 candidate = destination.add(side.scale((double)offset));
                  if (!(candidate.distanceToSqr(owner.position()) > radius * radius)) {
                     Path path = this.dragon.getNavigation().createPath(candidate.x, candidate.y, candidate.z, 0);
                     if (path != null && path.getEndNode() != null && this.withinBoundary(path, owner, radius)) {
                        if (fallback == null) {
                           fallback = path;
                        }

                        Node end = path.getEndNode();
                        Vec3 eye = new Vec3((double)end.x + 0.5, (double)((float)end.y + this.dragon.getEyeHeight()), (double)end.z + 0.5);
                        Vec3 aim = target.getBoundingBox().getCenter();
                        if (pursueMelee && path.canReach()
                           || !pursueMelee
                              && eye.distanceTo(aim) <= profile.range()
                              && this.dragon.level().clip(new ClipContext(eye, aim, Block.COLLIDER, Fluid.NONE, this.dragon)).getType() == Type.MISS) {
                           this.dragon.getNavigation().moveTo(path, 1.2);
                           return;
                        }
                     }
                  }
               }

               if (fallback != null) {
                  this.dragon.getNavigation().moveTo(fallback, 1.2);
                  this.pathCooldown = 40;
                  this.failedOwnerPosition = owner.position();
                  this.failedTargetPosition = target.position();
               } else {
                  this.dragon.getNavigation().stop();
                  this.pathCooldown = 40;
                  this.failedOwnerPosition = owner.position();
                  this.failedTargetPosition = target.position();
               }
            }
         }
      }
   }

   private void patrol(LivingEntity owner, double radius, double ownerDistance) {
      double teleportDistance = Math.max((Double)DragonMaidConfig.GUARD_TELEPORT_DISTANCE.get(), radius + 1.0);
      double returnDistance = Math.min(radius, 3.0 + (teleportDistance - 3.0) * 0.6);
      double stopDistance = Math.max(3.0, returnDistance * 0.55);
      double patrolDistance = Math.max(3.0, returnDistance * 0.75);
      boolean needsEscortPath = this.returning
         || this.patrolling
         || this.settling
         || this.ownerFollowing
         || this.followingOwner
         || ownerDistance > returnDistance * returnDistance;
      if (this.dragon.isLeashed() || needsEscortPath && this.idleHasChain()) {
         if (this.returning || this.patrolling || this.settling || this.followingOwner) {
            this.dragon.getNavigation().stop();
         }

         this.returning = false;
         this.patrolling = false;
         this.settling = false;
         this.followingOwner = false;
      } else {
         if (ownerDistance > returnDistance * returnDistance && !this.returning) {
            this.returning = true;
            this.patrolling = false;
            this.settling = false;
            this.followingOwner = false;
            double approachMax = Math.min(4.0, stopDistance - 0.5);
            double approachMin = Math.min(2.0, approachMax);
            this.returnApproachDistance = approachMin + this.dragon.getRandom().nextDouble() * (approachMax - approachMin);
            this.dragon.getNavigation().stop();
            this.pathCooldown = 0;
         }

         if (this.returning) {
            if (ownerDistance > stopDistance * stopDistance) {
               this.followNearby(owner);
               return;
            }

            this.returning = false;
            this.settling = !this.dragon.getNavigation().isDone();
            this.nextPatrolTick = this.dragon.tickCount + 80 + this.dragon.getRandom().nextInt(121);
         }

         if (!this.ownerFollowing) {
            if (this.followingOwner) {
               this.followingOwner = false;
               this.settling = !this.dragon.getNavigation().isDone();
               this.nextPatrolTick = this.dragon.tickCount + 80 + this.dragon.getRandom().nextInt(121);
            }

            if (!this.dragon.isInWaterOrBubble() && !this.dragon.isInLava()) {
               if (this.settling) {
                  if (!this.withinBoundary(this.dragon.getNavigation().getPath(), owner, radius)) {
                     this.dragon.getNavigation().stop();
                     this.settling = false;
                  } else {
                     if (!this.dragon.getNavigation().isDone()) {
                        return;
                     }

                     this.settling = false;
                  }
               }

               if (this.patrolling) {
                  if (!this.withinBoundary(this.dragon.getNavigation().getPath(), owner, radius)) {
                     this.dragon.getNavigation().stop();
                     this.patrolling = false;
                  } else {
                     if (!this.dragon.getNavigation().isDone()) {
                        return;
                     }

                     this.patrolling = false;
                  }
               }

               if (this.dragon.onGround() && this.dragon.getNavigation().isDone()) {
                  if (this.nextPatrolTick == 0) {
                     this.nextPatrolTick = this.dragon.tickCount + 80 + this.dragon.getRandom().nextInt(121);
                  }

                  if (this.dragon.tickCount >= this.nextPatrolTick) {
                     this.nextPatrolTick = this.dragon.tickCount + 80 + this.dragon.getRandom().nextInt(121);
                     if (!hasChain(this.dragon)) {
                        for (int attempt = 0; attempt < 3; attempt++) {
                           Vec3 candidate = DefaultRandomPos.getPos(this.dragon, 7, 3);
                           if (candidate != null && !(candidate.distanceToSqr(owner.position()) > patrolDistance * patrolDistance)) {
                              Path path = this.dragon.getNavigation().createPath(candidate.x, candidate.y, candidate.z, 0);
                              if (path != null && path.canReach() && this.withinBoundary(path, owner, radius)) {
                                 this.patrolling = this.dragon.getNavigation().moveTo(path, 1.0);
                                 return;
                              }
                           }
                        }
                     }
                  }
               }
            } else {
               if (this.patrolling) {
                  this.dragon.getNavigation().stop();
               }

               this.patrolling = false;
               if (this.dragon.getNavigation().isDone()) {
                  this.settling = false;
               }
            }
         } else {
            if (!this.followingOwner) {
               if (this.patrolling || this.settling) {
                  this.dragon.getNavigation().stop();
               }

               this.patrolling = false;
               this.settling = false;
               this.followingOwner = true;
               this.returnApproachDistance = 2.0 + this.dragon.getRandom().nextDouble();
               this.pathCooldown = 0;
            }

            if (ownerDistance > 9.0) {
               this.followNearby(owner);
            }
         }
      }
   }

   private void clearFailedPositions() {
      this.failedOwnerPosition = null;
      this.failedTargetPosition = null;
   }

   private void updateOwnerMotion(LivingEntity owner) {
      if (this.motionOwner != owner) {
         this.motionOwner = owner;
         this.lastOwnerX = owner.getX();
         this.lastOwnerZ = owner.getZ();
         this.ownerTravel = 0.0;
         this.ownerStillTicks = 0;
         this.ownerFollowing = false;
      } else {
         double dx = owner.getX() - this.lastOwnerX;
         double dz = owner.getZ() - this.lastOwnerZ;
         this.lastOwnerX = owner.getX();
         this.lastOwnerZ = owner.getZ();
         double deltaSqr = dx * dx + dz * dz;
         if (deltaSqr > 4.0E-6) {
            this.ownerStillTicks = 0;
            if (!this.ownerFollowing) {
               this.ownerTravel = this.ownerTravel + Math.sqrt(deltaSqr);
               if (this.ownerTravel >= 1.0) {
                  this.ownerFollowing = true;
               }
            }
         } else if (++this.ownerStillTicks >= 10) {
            this.ownerStillTicks = 10;
            this.ownerTravel = 0.0;
            this.ownerFollowing = false;
         }
      }
   }

   private void combatReturn(LivingEntity owner, double innerRadius) {
      if (this.pathCooldown <= 0) {
         if (this.dragon.getNavigation().isDone() || this.combatReturnOwnerPosition == null || !(this.combatReturnOwnerPosition.distanceToSqr(owner.position()) < 1.0)) {
            this.pathCooldown = 10;
            Vec3 offset = this.dragon.position().subtract(owner.position());
            Vec3 horizontal = new Vec3(offset.x, 0.0, offset.z).normalize();
            Vec3 destination = owner.position().add(horizontal.scale(innerRadius));
            Path path = this.dragon.getNavigation().createPath(destination.x, destination.y, destination.z, 0);
            if (path != null && path.canReach()) {
               this.dragon.getNavigation().moveTo(path, 1.2);
               this.combatReturnOwnerPosition = owner.position();
            } else {
               path = this.dragon.getNavigation().createPath(owner, 0);
               if (path != null && path.canReach()) {
                  this.dragon.getNavigation().moveTo(path, 1.2);
                  this.combatReturnOwnerPosition = owner.position();
               }
            }
         }
      }
   }

   private boolean idleHasChain() {
      if (this.dragon.tickCount >= this.nextChainCheckTick) {
         this.idleChained = hasChain(this.dragon);
         this.nextChainCheckTick = this.dragon.tickCount + 20;
      }

      return this.idleChained;
   }

   private void followNearby(LivingEntity owner) {
      if (this.pathCooldown <= 0) {
         this.pathCooldown = 10;
         Path current = this.dragon.getNavigation().getPath();
         if (current == null
            || this.dragon.getNavigation().isDone()
            || !current.canReach()
            || this.followPathOwnerPosition == null
            || !(this.followPathOwnerPosition.distanceToSqr(owner.position()) < 1.0)) {
            Vec3 offset = this.dragon.position().subtract(owner.position());
            Vec3 horizontal = new Vec3(offset.x, 0.0, offset.z).normalize();
            Vec3 destination = owner.position().add(horizontal.scale(this.returnApproachDistance));
            Path path = this.dragon.getNavigation().createPath(destination.x, destination.y, destination.z, 0);
            this.followPathOwnerPosition = owner.position();
            if (path != null && path.canReach()) {
               this.dragon.getNavigation().moveTo(path, 1.2);
            } else {
               path = this.dragon.getNavigation().createPath(owner, 0);
               if (path != null) {
                  this.dragon.getNavigation().moveTo(path, 1.2);
               } else {
                  this.dragon.getNavigation().stop();
               }
            }
         }
      }
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
