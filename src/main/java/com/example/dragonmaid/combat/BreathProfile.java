package com.example.dragonmaid.combat;

import com.example.dragonmaid.DragonMaidConfig;
import com.example.dragonmaid.data.HumanoidWater;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.LightningDragonEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public record BreathProfile(int stage, int duration, double range, double halfAngle, double speed) {
   public static final int EMIT_INTERVAL = 14;

   public static BreathProfile forDragon(DragonBaseEntity dragon) {
      BreathProfile profile = forStage(dragon.getDragonStage());
      return dragon instanceof LightningDragonEntity ? profile : fireIceForStage(profile.stage);
   }

   public static BreathProfile fireIceForStage(int stage) {
      int tier = Mth.clamp(stage, 3, 5);
      int duration = tier == 3 ? 1 : 15;
      return new BreathProfile(
         tier,
         duration,
         (Double)DragonMaidConfig.FIRE_ICE_BREATH_RANGE.get()
            * (14.0 + (double)(tier - 3) * 4.0)
            / 22.0
            * (Double)DragonMaidConfig.HUMANOID_BREATH_RANGE_MULTIPLIER.get(),
         Math.toRadians((double)(6 + (tier - 3) * 3)),
         (Double)DragonMaidConfig.BREATH_SPEED.get()
      );
   }

   public boolean inRange(DragonBaseEntity dragon, LivingEntity target) {
      return distanceToTargetSqr(dragon, target, HumanoidWater.breathOrigin(dragon)) <= this.range * this.range;
   }

   public static double distanceToTargetSqr(DragonBaseEntity dragon, LivingEntity target, Vec3 start) {
      AABB box = target.getBoundingBox();
      if (!WildDragonCombat.isHostileWildDragon(dragon, target)) {
         return start.distanceToSqr(box.getCenter());
      } else {
         return box.contains(start) ? 0.0 : start.distanceToSqr(box.clip(start, box.getCenter()).orElse(box.getCenter()));
      }
   }

   public boolean canHit(DragonBaseEntity dragon, LivingEntity target) {
      return this.canHitFrom(dragon, target, HumanoidWater.breathOrigin(dragon));
   }

   public boolean canHitFrom(DragonBaseEntity dragon, LivingEntity target, Vec3 start) {
      return distanceToTargetSqr(dragon, target, start) <= this.range * this.range
         && dragon.level().clip(new ClipContext(start, target.getBoundingBox().getCenter(), Block.COLLIDER, Fluid.NONE, dragon)).getType() == Type.MISS;
   }

   public static BreathProfile forStage(int stage) {
      int tier = Mth.clamp(stage, 3, 5);
      return new BreathProfile(
         tier,
         12 + (tier - 3) * 6,
         (Double)DragonMaidConfig.BREATH_RANGE.get() * (0.625 + (double)(tier - 3) * 0.1875) * (Double)DragonMaidConfig.HUMANOID_BREATH_RANGE_MULTIPLIER.get(),
         Math.toRadians((double)(12 + (tier - 3) * 6)),
         (Double)DragonMaidConfig.BREATH_SPEED.get()
      );
   }
}
