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
   private TailSweepState timeline;

   public HumanoidTailSweep(DragonBaseEntity dragon) {
      this.dragon = dragon;
   }

   public static double reach(DragonBaseEntity dragon) {
      return HumanoidCombat.naturalClawReach(dragon) * 1.5 * (NativeTailReach.enhanced(dragon) ? 1.2 : 1.0);
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
         && this.dragon.level() instanceof ServerLevel level
         && canAct(this.dragon)
         && this.dragon.onGround()
         && !this.active()
         && !HumanoidRoar.isRoaring(this.dragon)
         && HumanoidTargetGoal.isEnemy(this.dragon, target)) {
         Vec3 direction = target.position().subtract(this.dragon.position());
         float candidateYaw = direction.horizontalDistanceSqr() < 1.0E-6
            ? this.dragon.getYRot()
            : (float)(Mth.atan2(direction.z, direction.x) * (180.0 / Math.PI)) - 90.0F;
         List<TailSweepGeometry.Frame> frames = TailSweepGeometry.attackFrames(this.dragon, candidateYaw);
         boolean hasEnemy = false;

         for (LivingEntity enemy : level.getEntitiesOfClass(LivingEntity.class, bounds(frames), e -> HumanoidTargetGoal.isEnemy(this.dragon, e))) {
            if (this.dragon.hasLineOfSight(enemy) && this.contact(level, enemy, frames, 0, frames.size() - 1) != null) {
               hasEnemy = true;
               break;
            }
         }

         if (!hasEnemy) {
            return false;
         } else {
            this.yaw = candidateYaw;
            this.startedAt = level.getGameTime();
            this.previousElapsed = -1;
            this.swung = false;
            this.damage = (float)this.dragon.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.5F;
            this.attempted.clear();
            HumanoidCombat.stopBreath(this.dragon);
            this.dragon.setDeltaMovement(0.0, this.dragon.getDeltaMovement().y, 0.0);
            this.timeline = TailSweepSync.begin(this.dragon, this.yaw);
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
         long elapsedLong = this.dragon.level().getGameTime() - this.startedAt;
         if (canAct(this.dragon) && elapsedLong >= 0L && elapsedLong >= (long)this.previousElapsed && TailSweepSync.isBusy(this.dragon)) {
            int elapsed = (int)elapsedLong;
            this.holdPose();
            if (!this.swung && elapsed >= this.timeline.swingTick() && elapsed <= this.timeline.hitEnd()) {
               this.dragon.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.9F, 0.8F);
               this.swung = true;
            }

            if (elapsed >= this.timeline.hitStart()
               && elapsed <= this.timeline.hitEnd()
               && elapsed != this.previousElapsed
               && this.dragon.level() instanceof ServerLevel level) {
               List<TailSweepGeometry.Frame> frames = TailSweepGeometry.attackFrames(this.dragon, this.yaw);
               int from = this.timeline.hitFrameAt(elapsed - 1);
               int to = this.timeline.hitFrameAt(elapsed);
               this.wind(level, frames, from, to);
               FrostyTailParticles.humanoid(level, this.dragon, frames, from, to);
               this.hit(level, frames, from, to);
            }

            this.previousElapsed = elapsed;
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
         this.timeline = null;
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
      for (LivingEntity enemy : level.getEntitiesOfClass(LivingEntity.class, bounds(frames.subList(from, to + 1)), e -> HumanoidTargetGoal.isEnemy(this.dragon, e))) {
         if (!this.attempted.contains(enemy.getUUID()) && HumanoidTargetGoal.isEnemy(this.dragon, enemy) && this.dragon.hasLineOfSight(enemy)) {
            Vec3 contact = this.contact(level, enemy, frames, from, to);
            if (contact != null) {
               this.attempted.add(enemy.getUUID());
               if (DragonCombatAccessories.humanoidTail(this.dragon, enemy, level.damageSources().mobAttack(this.dragon), this.damage)) {
                  level.sendParticles(ParticleTypes.CRIT, contact.x, contact.y, contact.z, 8, 0.12, 0.12, 0.12, 0.14);
                  level.playSound(null, enemy.getX(), enemy.getY(), enemy.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.NEUTRAL, 0.65F, 0.85F);
               }
            }
         }
      }
   }

   private void wind(ServerLevel level, List<TailSweepGeometry.Frame> frames, int from, int to) {
      int first = from == 0 ? 0 : from + 1;

      for (int index = first; index <= to; index++) {
         Vec3 point = frames.get(index).trailPoint();
         Vec3 tangent = point.subtract(frames.get(Math.max(0, index - 1)).trailPoint()).normalize().scale(0.035);
         level.sendParticles(ParticleTypes.POOF, point.x, point.y, point.z, 0, tangent.x, 0.01, tangent.z, 1.0);
      }

      Vec3 point = frames.get((from + to) / 2).trailPoint();
      level.sendParticles(ParticleTypes.SWEEP_ATTACK, point.x, point.y, point.z, 0, 1.1, 0.0, 0.0, 1.0);
   }

   private Vec3 contact(ServerLevel level, LivingEntity enemy, List<TailSweepGeometry.Frame> frames, int from, int to) {
      for (int frame = from; frame <= to; frame++) {
         List<TailSweepGeometry.Sphere> current = frames.get(frame).spheres();
         List<TailSweepGeometry.Sphere> previous = frames.get(Math.max(from, frame - 1)).spheres();

         for (int part = 0; part < current.size(); part++) {
            TailSweepGeometry.Sphere a = previous.get(part);
            TailSweepGeometry.Sphere b = current.get(part);
            Vec3 contact = TailSweepCollision.contact(a.center(), b.center(), enemy.getBoundingBox(), Math.max(a.radius(), b.radius()));
            if (contact != null) {
               Vec3 root = new Vec3(this.dragon.getX(), contact.y, this.dragon.getZ());
               if (level.clip(new ClipContext(root, contact, Block.COLLIDER, Fluid.NONE, this.dragon)).getType() == Type.MISS) {
                  return contact;
               }
            }
         }
      }

      return null;
   }

   private static AABB bounds(List<TailSweepGeometry.Frame> frames) {
      AABB result = null;

      for (int frame = 0; frame < frames.size(); frame++) {
         List<TailSweepGeometry.Sphere> current = frames.get(frame).spheres();
         List<TailSweepGeometry.Sphere> previous = frames.get(Math.max(0, frame - 1)).spheres();
         List<TailSweepGeometry.Sphere> next = frames.get(Math.min(frames.size() - 1, frame + 1)).spheres();

         for (int part = 0; part < current.size(); part++) {
            TailSweepGeometry.Sphere sphere = current.get(part);
            double radius = Math.max(sphere.radius(), Math.max(previous.get(part).radius(), next.get(part).radius()));
            Vec3 center = sphere.center();
            AABB box = new AABB(center, center).inflate(radius);
            result = result == null ? box : result.minmax(box);
         }
      }

      return result;
   }
}
