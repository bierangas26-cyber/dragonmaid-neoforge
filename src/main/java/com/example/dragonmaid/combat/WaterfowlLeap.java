package com.example.dragonmaid.combat;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;

record WaterfowlLeap(Vec3 velocity, int flightTicks, double gravity) {
   static final double STANDOFF = 2.4;
   static final double MAX_DISTANCE = 12.0;
   private static final double HORIZONTAL_DRAG = 0.91F;
   private static final double VERTICAL_DRAG = 0.98F;
   private static final int MAX_LOW_ARC_TICKS = 17;
   private static final double MAX_LAUNCH_SPEED = 1.28;
   private static final double MAX_JUMP_HEIGHT = 2.8;

   static WaterfowlLeap toward(DragonBaseEntity dragon, LivingEntity enemy) {
      Vec3 offset = enemy.position().subtract(dragon.position());
      Vec3 horizontal = new Vec3(offset.x, 0.0, offset.z);
      double distance = horizontal.length();
      double travel = Math.max(0.0, distance - 2.4);
      if (!dragon.isNoGravity() && !dragon.hasEffect(MobEffects.SLOW_FALLING) && !dragon.hasEffect(MobEffects.LEVITATION)) {
         double gravity = dragon.getAttributeValue(Attributes.GRAVITY);
         Vec3 heading = horizontal.normalize();
         int preferredTicks = (int)Math.round(Mth.lerp(Mth.clamp(travel / 9.6, 0.0, 1.0), 14.0, 17.0));
         double gravityFactor = 0.98F * gravity / 0.01999998F;
         double maxLift = Double.POSITIVE_INFINITY;

         for (int tick = 1; tick <= 17; tick++) {
            double sum = dragSum(0.98F, tick);
            maxLift = Math.min(maxLift, (2.8 + gravityFactor * ((double)tick - sum)) / sum);
         }

         for (int ticks = preferredTicks; ticks >= 14; ticks--) {
            double verticalSum = dragSum(0.98F, ticks);
            double lift = Math.min(maxLift, (gravityFactor * ((double)ticks - verticalSum) - 0.01) / verticalSum);
            double previousSum = dragSum(0.98F, ticks - 1);
            double forward = travel / dragSum(0.91F, ticks);
            if (lift > 0.0 && lift * previousSum > gravityFactor * ((double)(ticks - 1) - previousSum) && forward * forward + lift * lift <= 1.6384) {
               return new WaterfowlLeap(new Vec3(heading.x * forward, lift, heading.z * forward), ticks, gravity);
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private static double dragSum(double drag, int ticks) {
      return (1.0 - Math.pow(drag, (double)ticks)) / (1.0 - drag);
   }

   boolean clear(DragonBaseEntity dragon) {
      Vec3 position = dragon.position();
      Vec3 delta = Vec3.ZERO;
      Vec3 tickVelocity = this.velocity;
      AABB box = dragon.getBoundingBox().deflate(0.01);

      for (int tick = 0; tick < this.flightTicks; tick++) {
         Vec3 next = delta.add(tickVelocity);
         if (tick == this.flightTicks - 1) {
            next = new Vec3(next.x, Math.max(0.0, next.y), next.z);
         }

         AABB swept = box.move(delta).expandTowards(next.subtract(delta));
         if (!HumanoidArmedSweep.clear(dragon, swept) || dragon.level().containsAnyLiquid(swept)) {
            return false;
         }

         delta = next;
         tickVelocity = new Vec3(tickVelocity.x * 0.91F, (tickVelocity.y - this.gravity) * 0.98F, tickVelocity.z * 0.91F);
      }

      return HumanoidArmedSweep.supported(dragon, position.add(delta), position.y);
   }
}