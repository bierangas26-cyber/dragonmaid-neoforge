package com.example.dragonmaid.combat;

import com.example.dragonmaid.accessory.DragonAccessories;
import com.example.dragonmaid.accessory.DragonAccessoryBehavior;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.IceDragonEntity;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class FrostyTailParticles {
   private FrostyTailParticles() {
   }

   public static boolean enabled(DragonBaseEntity dragon) {
      return dragon instanceof IceDragonEntity && DragonAccessories.hasEffect(dragon, DragonAccessoryBehavior.ICE_TAIL);
   }

   public static void humanoid(ServerLevel level, DragonBaseEntity dragon, List<TailSweepGeometry.Frame> frames, int from, int to) {
      if (enabled(dragon)) {
         int first = from == 0 ? 0 : from + 1;

         for (int index = first; index <= to; index++) {
            Vec3 center = frames.get(index).trailPoint();
            Vec3 radial = new Vec3(center.x - dragon.getX(), 0.0, center.z - dragon.getZ());
            Vec3 edge = radial.lengthSqr() < 1.0E-8 ? center : center.add(radial.normalize().scale(0.5));
            level.sendParticles(ParticleTypes.SNOWFLAKE, edge.x, edge.y, edge.z, 3, 0.06, 0.05, 0.06, 0.005);
            if ((index & 1) == 0) {
               level.sendParticles(ParticleTypes.CLOUD, edge.x, edge.y, edge.z, 1, 0.015, 0.015, 0.015, 0.005);
            }
         }
      }
   }

   public static void nativeSweep(ServerLevel level, DragonBaseEntity dragon, double reach) {
      if (enabled(dragon)) {
         LivingEntity target = dragon.getTarget();
         Vec3 toward = target == null ? dragon.getLookAngle() : target.position().subtract(dragon.position());
         if (toward.horizontalDistanceSqr() < 1.0E-8) {
            toward = dragon.getLookAngle();
         }

         double angle = Math.atan2(toward.z, toward.x);
         double y = target == null ? dragon.getY() + (double)dragon.getBbHeight() * 0.5 : target.getY() + (double)target.getBbHeight() * 0.5;

         for (int i = 0; i < 13; i++) {
            double a = angle + Math.toRadians((double)(-24 + 4 * i));
            double x = dragon.getX() + Math.cos(a) * reach;
            double z = dragon.getZ() + Math.sin(a) * reach;
            level.sendParticles(ParticleTypes.SNOWFLAKE, x, y, z, 3, 0.06, 0.05, 0.06, 0.005);
            if ((i & 1) == 0) {
               level.sendParticles(ParticleTypes.CLOUD, x, y, z, 1, 0.015, 0.015, 0.015, 0.005);
            }
         }
      }
   }
}
