package com.example.dragonmaid.combat;

import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.data.HumanoidLife;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.GorgonEntity;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class RestReturnGoal extends Goal {
   private static final int PATH_INTERVAL = 20;
   private static final int STUCK_TICKS = 160;
   private final DragonBaseEntity dragon;
   private RestReturnGoal.Spot spot;
   private boolean returning;
   private int nextPathTick;
   private int lastProgressTick;
   private double closestDistance;

   public RestReturnGoal(DragonBaseEntity dragon) {
      this.dragon = dragon;
      this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
   }

   public RestReturnGoal.Spot spot() {
      return this.spot;
   }

   public void interrupt(RestReturnGoal.Spot spot) {
      if (this.returning) {
         this.dragon.getNavigation().stop();
         if (this.dragon.isFlying() || this.dragon.isHovering()) {
            this.dragon.flightManager.setFlightTarget(null);
         }
      }

      this.returning = false;
      this.spot = spot;
   }

   public boolean returning() {
      return this.returning;
   }

   public static boolean returningInFlight(DragonBaseEntity dragon) {
      return (dragon.isFlying() || dragon.isHovering()) && BladeCompat.isReturningToRest(DragonForms.state(dragon));
   }

   public boolean begin() {
      if (this.dragon.level() instanceof ServerLevel level && this.spot != null && this.spot.dimension.equals(level.dimension().location())) {
         BlockPos destination = BlockPos.containing(this.spot.position);
         if (level.isInWorldBounds(destination) && level.getWorldBorder().isWithinBounds(destination) && level.hasChunkAt(destination) && !this.arrived()) {
            this.returning = true;
            this.lastProgressTick = this.nextPathTick = this.dragon.tickCount;
            this.closestDistance = this.dragon.distanceToSqr(this.spot.position);
            this.dragon.getNavigation().stop();
            return true;
         }

         return false;
      }

      return false;
   }

   private boolean arrived() {
      return this.dragon.onGround() && this.dragon.distanceToSqr(this.spot.position) <= 1.0 && !this.dragon.isFlying() && !this.dragon.isHovering();
   }

   public boolean canUse() {
      return this.returning
         && this.dragon.isAlive()
         && !this.dragon.isModelDead()
         && !this.dragon.isNoAi()
         && !HumanoidLife.isInjured(this.dragon)
         && !GorgonEntity.isStoneMob(this.dragon)
         && !this.dragon.isPassenger()
         && !this.dragon.isVehicle()
         && this.dragon.getTarget() == null;
   }

   public boolean canContinueToUse() {
      return this.canUse();
   }

   public boolean requiresUpdateEveryTick() {
      return true;
   }

   public void tick() {
      if (this.returning && this.spot != null) {
         Vec3 destination = this.spot.position;
         double distance = this.dragon.distanceToSqr(destination);
         if ((this.dragon.isFlying() || this.dragon.isHovering()) && (this.dragon.onGround() || distance <= 2.25)) {
            this.dragon.setFlying(false);
            this.dragon.setHovering(false);
            this.dragon.flightManager.setFlightTarget(null);
         }

         if (!this.arrived() && this.dragon.tickCount - this.lastProgressTick < 160 && this.dragon.level().hasChunkAt(BlockPos.containing(destination))) {
            if (distance + 0.25 < this.closestDistance) {
               this.closestDistance = distance;
               this.lastProgressTick = this.dragon.tickCount;
            }

            if (this.dragon.isFlying() || this.dragon.isHovering()) {
               this.dragon.flightManager.setFlightTarget(destination);
            } else if (this.dragon.getNavigation().isDone()) {
               Vec3 offset = destination.subtract(this.dragon.position());
               double directReach = Math.min(6.0, Math.max(2.5, (double)this.dragon.getBbWidth() * 1.5));
               if (this.dragon.onGround() && Math.abs(offset.y) < 0.25 && distance <= directReach * directReach && this.clearGroundApproach(offset)) {
                  this.dragon.getMoveControl().setWantedPosition(destination.x, this.dragon.getY(), destination.z, 1.0);
               } else if (this.dragon.tickCount >= this.nextPathTick) {
                  this.nextPathTick = this.dragon.tickCount + 20;
                  this.dragon.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.0);
               }
            }
         } else {
            this.dragon.setCommand(1);
            if (this.dragon.onGround()) {
               this.dragon.setDeltaMovement(0.0, this.dragon.getDeltaMovement().y, 0.0);
            }
         }
      }
   }

   private boolean clearGroundApproach(Vec3 offset) {
      Vec3 horizontal = new Vec3(offset.x, 0.0, offset.z);
      AABB swept = this.dragon.getBoundingBox().expandTowards(horizontal);
      if (this.dragon
            .level()
            .hasChunksAt(BlockPos.containing(swept.minX, swept.minY, swept.minZ), BlockPos.containing(swept.maxX, swept.maxY, swept.maxZ))
         && this.dragon.level().noCollision(this.dragon, swept)) {
         int steps = (int)Math.ceil(horizontal.length() * 2.0);

         for (int step = 1; step <= steps; step++) {
            Vec3 point = this.dragon.position().add(horizontal.scale((double)step / (double)steps));
            BlockPos ground = BlockPos.containing(point.add(0.0, -0.1, 0.0));
            if (!this.dragon.level().hasChunkAt(ground) || !this.dragon.level().getBlockState(ground).isFaceSturdy(this.dragon.level(), ground, Direction.UP)) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public static record Spot(ResourceLocation dimension, Vec3 position) {
      public static RestReturnGoal.Spot at(DragonBaseEntity dragon) {
         return new RestReturnGoal.Spot(dragon.level().dimension().location(), dragon.position());
      }

      public CompoundTag save() {
         CompoundTag tag = new CompoundTag();
         tag.putString("Dimension", this.dimension.toString());
         tag.putDouble("X", this.position.x);
         tag.putDouble("Y", this.position.y);
         tag.putDouble("Z", this.position.z);
         return tag;
      }

      public static RestReturnGoal.Spot load(CompoundTag tag) {
         ResourceLocation dimension = ResourceLocation.tryParse(tag.getString("Dimension"));
         if (dimension != null && tag.contains("X", 99) && tag.contains("Y", 99) && tag.contains("Z", 99)) {
            Vec3 pos = new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"));
            return Double.isFinite(pos.lengthSqr()) ? new RestReturnGoal.Spot(dimension, pos) : null;
         } else {
            return null;
         }
      }
   }
}