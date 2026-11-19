package com.example.dragonmaid.combat;

import java.util.Optional;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class KatanaSweep {
   private static final double HALF_THICKNESS = 1.0;
   private static final double HALF_ARC_COS = Math.cos(Math.toRadians(120.0));
   private final Vec3 forward;
   private final Vec3 side;
   private final Vec3 normal;

   public KatanaSweep(float yaw, float pitch) {
      this.forward = Vec3.directionFromRotation(pitch, yaw);
      this.side = Vec3.directionFromRotation(0.0F, yaw + 90.0F);
      this.normal = this.side.cross(this.forward);
   }

   public Vec3 direction(double angle) {
      return this.forward.scale(Math.cos(angle)).add(this.side.scale(Math.sin(angle)));
   }

   public Vec3 normal() {
      return this.normal;
   }

   boolean hits(LivingEntity wielder, Vec3 origin, AABB target, double reach) {
      double reachSqr = reach * reach;
      Vec3 nearest = new Vec3(
         Mth.clamp(origin.x, target.minX, target.maxX),
         Mth.clamp(origin.y, target.minY, target.maxY),
         Mth.clamp(origin.z, target.minZ, target.maxZ)
      );
      if (nearest.distanceToSqr(origin) > reachSqr) {
         return false;
      } else {
         Optional<Vec3> aimed = target.clip(origin, origin.add(this.forward.scale(reach)));
         if (aimed.isPresent() && this.canStrike(wielder, origin, aimed.get(), reachSqr)) {
            return true;
         } else if (this.canStrike(wielder, origin, nearest, reachSqr)) {
            return true;
         } else {
            double[] xs = new double[]{nearest.x, target.minX, target.maxX};
            double[] ys = new double[]{nearest.y, target.minY, target.maxY};
            double[] zs = new double[]{nearest.z, target.minZ, target.maxZ};

            for (double x : xs) {
               for (double y : ys) {
                  for (double z : zs) {
                     if ((x != nearest.x || y != nearest.y || z != nearest.z)
                        && this.canStrike(wielder, origin, new Vec3(x, y, z), reachSqr)) {
                        return true;
                     }
                  }
               }
            }

            return false;
         }
      }
   }

   private boolean canStrike(LivingEntity wielder, Vec3 origin, Vec3 point, double reachSqr) {
      Vec3 delta = point.subtract(origin);
      if (!(delta.lengthSqr() > reachSqr + 1.0E-6) && !(Math.abs(delta.dot(this.normal)) > 1.0)) {
         double ahead = delta.dot(this.forward);
         return ahead < HALF_ARC_COS * Math.hypot(ahead, delta.dot(this.side))
            ? false
            : wielder.level().clip(new ClipContext(origin, point, Block.COLLIDER, Fluid.NONE, wielder)).getType() == Type.MISS;
      } else {
         return false;
      }
   }
}
