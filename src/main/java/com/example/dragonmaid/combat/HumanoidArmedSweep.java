package com.example.dragonmaid.combat;

import com.example.dragonmaid.accessory.DragonHands;
import com.example.dragonmaid.animation.TailSweepState;
import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.item.CompanionBladeItem;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.LightningDragonEntity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class HumanoidArmedSweep {
   public static final int COOLDOWN = 400;
   public static final int TAKEOFF = 6;
   private static final int WATERFOWL_COOLDOWN = 600;
   private final DragonBaseEntity dragon;
   private final Set<UUID> attempted = new HashSet<>();
   private TailSweepState timeline;
   private LivingEntity target;
   private long readyAt;
   private long waterfowlReadyAt;
   private long combatSince = -1L;
   private long lastEnemyAt = -1L;
   private long nextCheck;
   private long nextWaterfowlCheck;
   private boolean openingUsed;
   private boolean jumped;
   private boolean landed;
   private boolean swung;
   private boolean weaponUsed;
   private boolean fireAxeUsed;
   private ItemStack waterfowlWeapon = ItemStack.EMPTY;
   private ItemStack waterfowlDamageWeapon = ItemStack.EMPTY;
   private WaterfowlLeap waterfowlLeap;
   private Vec3 takeoffOrigin;
   private double moved;
   private int previous = -1;
   private float yaw;
   private float damage;
   private TailSweepGeometry.Frame previousImpact;

   public HumanoidArmedSweep(DragonBaseEntity dragon) {
      this.dragon = dragon;
   }

   public boolean active() {
      return this.timeline != null;
   }

   boolean waterfowlAttack() {
      return this.active() && this.timeline.waterfowl();
   }

   public int cooldown() {
      return (int)Math.max(0L, Math.min(400L, this.readyAt - this.dragon.level().getGameTime()));
   }

   public void loadCooldown(int ticks) {
      this.cancel();
      this.readyAt = this.dragon.level().getGameTime() + (long)Mth.clamp(ticks, 0, 400);
   }

   int waterfowlCooldown() {
      return (int)Mth.clamp((float)(this.waterfowlReadyAt - this.dragon.level().getGameTime()), 0.0F, 600.0F);
   }

   void loadWaterfowlCooldown(int ticks) {
      this.waterfowlReadyAt = this.dragon.level().getGameTime() + (long)Mth.clamp(ticks, 0, 600);
   }

   public void observe(LivingEntity enemy) {
      long now = this.dragon.level().getGameTime();
      if (HumanoidTailSweep.canAct(this.dragon)
         && enemy != null
         && HumanoidTargetGoal.isEnemy(this.dragon, enemy)
         && GuardEscort.reachableTarget(this.dragon, enemy)) {
         if (this.lastEnemyAt < 0L || now < this.lastEnemyAt || now - this.lastEnemyAt >= 100L) {
            this.combatSince = now;
            this.openingUsed = false;
         }

         this.lastEnemyAt = now;
      }
   }

   public boolean tryStart(LivingEntity enemy) {
      long now = this.dragon.level().getGameTime();
      ItemStack weapon = BladeCompat.availableMainHand(this.dragon);
      if (!this.active()
         && now >= this.readyAt
         && now >= this.nextCheck
         && this.dragon.level() instanceof ServerLevel level
         && HumanoidTailSweep.canAct(this.dragon)
         && this.canUseArmedSweep(weapon)
         && this.dragon.onGround()
         && !HumanoidRoar.isRoaring(this.dragon)
         && enemy != null
         && HumanoidTargetGoal.isEnemy(this.dragon, enemy)
         && GuardEscort.reachableTarget(this.dragon, enemy)
         && this.dragon.hasLineOfSight(enemy)
         && !((double)this.dragon.distanceTo(enemy) > ArmedTailGeometry.reach(this.dragon) + 1.5)) {
         this.nextCheck = now + 10L;
         boolean opening = !this.openingUsed && this.combatSince >= 0L && now - this.combatSince <= 60L;
         if (!opening) {
            int enemies = 0;

            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, this.dragon.getBoundingBox().inflate(6.0), ex -> HumanoidTargetGoal.isEnemy(this.dragon, ex))) {
               if (this.dragon.distanceToSqr(e) <= 36.0 && GuardEscort.reachableTarget(this.dragon, e) && this.dragon.hasLineOfSight(e)) {
                  if (++enemies == 3) {
                     break;
                  }
               }
            }

            if (enemies < 3) {
               return false;
            }
         }

         Vec3 toward = horizontal(enemy.position().subtract(this.dragon.position()));
         Vec3 move = toward.normalize().scale(Math.min(2.4, Math.max(0.0, toward.length() - 1.8)));
         if (ArmedTailGeometry.available(this.dragon) && this.safeCorridor(move)) {
            this.readyAt = now + 400L;
            this.openingUsed = true;
            this.begin(enemy, 5);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private boolean canUseArmedSweep(ItemStack weapon) {
      return !weapon.isEmpty()
         && (!(this.dragon instanceof LightningDragonEntity) || !SpecialWeapons.isKatana(weapon))
         && (this.dragon.getDragonStage() >= 4 || SpecialWeapons.wingGuard(this.dragon));
   }

   boolean canApproachWaterfowl(LivingEntity enemy) {
      long now = this.dragon.level().getGameTime();
      if (enemy != null
         && enemy == this.dragon.getTarget()
         && !this.active()
         && now >= this.waterfowlReadyAt
         && now >= this.nextWaterfowlCheck
         && this.dragon.getDragonStage() >= 4
         && this.dragon.onGround()
         && HumanoidTailSweep.canAct(this.dragon)
         && !this.dragon.isLeashed()
         && !this.dragon.isVehicle()
         && !HumanoidRoar.isRoaring(this.dragon)
         && SpecialWeapons.wingGuard(this.dragon)
         && SpecialWeapons.isKatana(BladeCompat.availableMainHand(this.dragon))) {
         Vec3 toward = enemy.position().subtract(this.dragon.position());
         double distance = toward.horizontalDistance();
         return distance <= 12.0
            && Math.abs(toward.y) <= 1.0
            && HumanoidTargetGoal.isEnemy(this.dragon, enemy)
            && GuardEscort.reachableTarget(this.dragon, enemy)
            && this.dragon.hasLineOfSight(enemy)
            && !GuardEscort.hasChain(this.dragon)
            && ArmedTailGeometry.available(this.dragon, true);
      } else {
         return false;
      }
   }

   boolean tryWaterfowl(LivingEntity enemy) {
      if (!this.canApproachWaterfowl(enemy)) {
         return false;
      } else {
         Vec3 toward = horizontal(enemy.position().subtract(this.dragon.position()));
         if (toward.lengthSqr() > 5.76) {
            Vec3 heading = toward.normalize();
            if (this.dragon.getNavigation().isDone() || this.dragon.getDeltaMovement().dot(heading) <= 0.01) {
               return false;
            }

            BlockPos node = this.dragon.getNavigation().getPath().getNextNodePos();
            if (Vec3.atBottomCenterOf(node).subtract(this.dragon.position()).dot(heading) <= 0.0) {
               return false;
            }
         }

         long now = this.dragon.level().getGameTime();
         this.nextWaterfowlCheck = now + 10L;
         WaterfowlLeap leap = WaterfowlLeap.toward(this.dragon, enemy);
         if (leap != null && leap.clear(this.dragon)) {
            this.waterfowlLeap = leap;
            this.waterfowlWeapon = BladeCompat.availableMainHand(this.dragon);
            this.waterfowlDamageWeapon = this.waterfowlWeapon.copy();
            this.waterfowlReadyAt = now + 600L;
            this.begin(enemy, TailSweepState.waterfowlLeapMotion(leap.flightTicks()));
            return true;
         } else {
            return false;
         }
      }
   }

   private void begin(LivingEntity enemy, int motion) {
      this.yaw = face(this.dragon, enemy);
      this.timeline = TailSweepSync.begin(this.dragon, this.yaw, motion);
      this.target = enemy;
      this.takeoffOrigin = this.dragon.position();
      this.jumped = this.landed = this.swung = this.weaponUsed = this.fireAxeUsed = false;
      this.moved = 0.0;
      this.previous = -1;
      this.previousImpact = null;
      this.damage = (float)this.dragon.getAttributeValue(Attributes.ATTACK_DAMAGE) * (this.timeline.waterfowl() ? 2.5F : 2.0F);
      this.attempted.clear();
      HumanoidCombat.stopBreath(this.dragon);
      this.dragon.setDeltaMovement(0.0, this.dragon.getDeltaMovement().y, 0.0);
      hold(this.dragon, this.yaw);
   }

   public boolean tick() {
      if (!this.active()) {
         return false;
      } else {
         long age = this.dragon.level().getGameTime() - this.timeline.startedAt();
         ItemStack weapon = BladeCompat.availableMainHand(this.dragon);
         if (HumanoidTailSweep.canAct(this.dragon)
            && TailSweepSync.isBusy(this.dragon)
            && age >= (long)this.previous
            && (
               !this.timeline.waterfowl()
                  || this.dragon.getDragonStage() >= 4
                     && SpecialWeapons.wingGuard(this.dragon)
                     && !this.dragon.isLeashed()
                     && !this.dragon.isVehicle()
                     && (this.weaponUsed || weapon == this.waterfowlWeapon)
                     && (this.jumped || this.dragon.getTarget() == this.target && this.validTarget())
            )
            && (this.timeline.waterfowl() || this.weaponUsed || this.canUseArmedSweep(weapon))) {
            int elapsed = (int)age;
            boolean waterfowl = this.timeline.waterfowl();
            int takeoff = waterfowl ? this.timeline.waterfowlTakeoffTick() : 6;
            if (elapsed == this.previous) {
               return true;
            } else {
               if (elapsed >= takeoff && !this.jumped && elapsed <= takeoff + 1) {
                  if (!this.dragon.onGround() || (waterfowl ? !this.waterfowlLeap.clear(this.dragon) : !this.safeCorridor(Vec3.ZERO, 1.6, 1))) {
                     this.cancel();
                     return false;
                  }

                  this.jumped = true;
                  this.takeoffOrigin = this.dragon.position();
                  this.dragon.setOnGround(false);
                  this.dragon.setDeltaMovement(waterfowl ? this.waterfowlLeap.velocity() : new Vec3(0.0, 0.36, 0.0));
                  this.dragon.hasImpulse = true;
               }

               if (this.jumped) {
                  if (elapsed > takeoff + 1 && this.dragon.onGround()) {
                     this.landed = true;
                  }

                  if (waterfowl) {
                     if (this.dragon.getTarget() == this.target && this.target.isAlive()) {
                        this.yaw = face(this.dragon, this.target);
                     }

                     this.dragon.resetFallDistance();
                  } else if (!this.landed && elapsed < this.timeline.hitEnd()) {
                     if (this.validTarget()) {
                        this.yaw = face(this.dragon, this.target);
                     }

                     this.steer(elapsed);
                  } else {
                     this.dragon.setDeltaMovement(0.0, this.dragon.getDeltaMovement().y, 0.0);
                  }
               }

               hold(this.dragon, this.yaw);
               if (!this.swung && (!waterfowl || this.landed) && elapsed >= this.timeline.swingTick() && elapsed <= this.timeline.hitEnd()) {
                  this.dragon.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.6F, 0.7F);
                  this.swung = true;
               }

               if (this.jumped
                  && (!waterfowl || this.landed)
                  && elapsed >= this.timeline.hitStart()
                  && elapsed <= this.timeline.hitEnd()
                  && this.dragon.level() instanceof ServerLevel level) {
                  List<TailSweepGeometry.Frame> frames = ArmedTailGeometry.frames(this.dragon, this.yaw, this.timeline.waterfowl());
                  int from = this.timeline.hitFrameAt(elapsed - 1);
                  int to = this.timeline.hitFrameAt(elapsed);
                  List<TailSweepGeometry.Frame> interval = new ArrayList<>();
                  interval.add(this.previousImpact == null ? frames.get(from) : this.previousImpact);
                  if (this.timeline.waterfowl() && this.previousImpact != null && from == to) {
                     interval.add(frames.get(to));
                  }

                  for (int i = from + 1; i <= to; i++) {
                     interval.add(frames.get(i));
                  }

                  this.previousImpact = frames.get(to);
                  HumanoidTailSweep.wind(level, interval, 0, interval.size() - 1);
                  FrostyTailParticles.humanoid(level, this.dragon, interval, 0, interval.size() - 1);
                  this.hit(level, interval, 0, interval.size() - 1);
               }

               this.previous = elapsed;
               return true;
            }
         } else {
            this.cancel();
            return false;
         }
      }
   }

   private boolean validTarget() {
      return this.target != null
         && this.target.level() == this.dragon.level()
         && this.dragon.hasLineOfSight(this.target)
         && HumanoidTargetGoal.isEnemy(this.dragon, this.target)
         && GuardEscort.reachableTarget(this.dragon, this.target);
   }

   private void steer(int elapsed) {
      Vec3 velocity = this.dragon.getDeltaMovement();
      double lift = elapsed > 6 && elapsed < 23 && this.dragon.getY() - this.takeoffOrigin.y < 1.55 ? 0.035 : 0.0;
      Vec3 toward = this.validTarget() ? horizontal(this.target.position().subtract(this.dragon.position())) : Vec3.ZERO;
      double step = Math.min(0.16, Math.min(Math.max(0.0, 2.4 - this.moved), Math.max(0.0, toward.length() - 1.8)));
      Vec3 delta = toward.normalize().scale(step);
      boolean safe = step > 0.0
         && horizontal(this.dragon.position().add(delta).subtract(this.takeoffOrigin)).lengthSqr() <= 5.76
         && supported(this.dragon, this.dragon.position().add(delta), this.takeoffOrigin.y)
         && DragonForms.containedInLoadedWorld(this.dragon, this.dragon.getBoundingBox().expandTowards(delta))
         && this.dragon.level().noCollision(this.dragon, this.dragon.getBoundingBox().expandTowards(delta));
      if (safe) {
         this.moved += step;
      } else {
         delta = Vec3.ZERO;
      }

      this.dragon.setDeltaMovement(delta.x, velocity.y + lift, delta.z);
      this.dragon.hasImpulse = true;
   }

   private boolean safeCorridor(Vec3 move) {
      return this.safeCorridor(move, 1.6, 4);
   }

   private boolean safeCorridor(Vec3 move, double height, int steps) {
      for (int i = 0; i <= steps; i++) {
         Vec3 delta = move.scale((double)i / (double)steps);
         Vec3 point = this.dragon.position().add(delta);
         AABB box = this.dragon.getBoundingBox().move(delta).expandTowards(0.0, height, 0.0).deflate(0.01);
         if (!clear(this.dragon, box) || !supported(this.dragon, point, this.dragon.getY())) {
            return false;
         }
      }

      return true;
   }

   static boolean clear(DragonBaseEntity dragon, AABB box) {
      return DragonForms.containedInLoadedWorld(dragon, box) && dragon.level().noCollision(dragon, box);
   }

   static boolean supported(DragonBaseEntity dragon, Vec3 point, double groundY) {
      BlockPos below = BlockPos.containing(point.x, groundY - 0.1, point.z);
      if (!dragon.level().hasChunkAt(below)) {
         return false;
      } else {
         BlockState state = dragon.level().getBlockState(below);
         return state.getFluidState().isEmpty()
            && state.isFaceSturdy(dragon.level(), below, Direction.UP)
            && !state.is(BlockTags.FIRE)
            && !state.is(Blocks.MAGMA_BLOCK);
      }
   }

   static float face(DragonBaseEntity dragon, LivingEntity e) {
      Vec3 delta = e.position().subtract(dragon.position());
      return delta.horizontalDistanceSqr() < 1.0E-6 ? dragon.getYRot() : (float)Math.toDegrees(Mth.atan2(delta.z, delta.x)) - 90.0F;
   }

   private static Vec3 horizontal(Vec3 v) {
      return new Vec3(v.x, 0.0, v.z);
   }

   static void hold(DragonBaseEntity dragon, float yaw) {
      dragon.getNavigation().stop();
      dragon.setSpeed(0.0F);
      dragon.xxa = dragon.zza = 0.0F;
      dragon.setJumping(false);
      dragon.setYRot(yaw);
      dragon.yBodyRot = yaw;
      dragon.yHeadRot = yaw;
      dragon.getLookControl()
         .setLookAt(
            dragon.getX() - (double)Mth.sin(yaw * (float) (Math.PI / 180.0)),
            dragon.getEyeY(),
            dragon.getZ() + (double)Mth.cos(yaw * (float) (Math.PI / 180.0)),
            30.0F,
            30.0F
         );
      DragonForms.state(dragon).dragonmaid$setAction(6);
   }

   private void hit(ServerLevel level, List<TailSweepGeometry.Frame> frames, int from, int to) {
      ItemStack weapon = BladeCompat.availableMainHand(this.dragon);
      boolean waterfowl = this.timeline.waterfowl();
      ItemStack damageWeapon = waterfowl ? this.waterfowlDamageWeapon : weapon;
      boolean fireAxe = this.weaponUsed ? this.fireAxeUsed : SpecialWeapons.fireAxe(this.dragon, damageWeapon);
      LivingEntity weaponTarget = null;

      for (LivingEntity e : level.getEntitiesOfClass(
         LivingEntity.class, HumanoidTailSweep.bounds(frames.subList(from, to + 1)), ex -> HumanoidTargetGoal.isEnemy(this.dragon, ex)
      )) {
         if (!this.attempted.contains(e.getUUID()) && GuardEscort.reachableTarget(this.dragon, e) && this.dragon.hasLineOfSight(e)) {
            Vec3 point = HumanoidTailSweep.contact(level, this.dragon, e, frames, from, to);
            if (point != null) {
               this.attempted.add(e.getUUID());
               float hitDamage = this.damage;
               if (waterfowl) {
                  float enchantment = BladeCompat.damageBonus(damageWeapon, e);
                  if (Float.isFinite(enchantment)) {
                     hitDamage += enchantment;
                  }

                  hitDamage *= KatanaCharge.lightArmorMultiplier((float)e.getArmorValue());
               }

               hitDamage += CompanionBladeItem.undeadBonus(damageWeapon, e);
               if (fireAxe) {
                  hitDamage = (float)((double)hitDamage * SpecialWeapons.fireAxeDamageMultiplier(e.getMaxHealth()));
               }

               if (DragonCombatAccessories.humanoidTail(
                  this.dragon, e, level.damageSources().mobAttack(this.dragon), hitDamage, e == this.target, true, waterfowl ? 2.0F : 1.75F
               )) {
                  if (!this.weaponUsed && (weaponTarget == null || e == this.target)) {
                     weaponTarget = e;
                  }

                  level.sendParticles(ParticleTypes.CRIT, point.x, point.y, point.z, 10, 0.15, 0.15, 0.15, 0.15);
                  level.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.NEUTRAL, 1.1F, 0.75F);
               }
            }
         }
      }

      if (weaponTarget != null) {
         this.weaponUsed = true;
         this.fireAxeUsed = fireAxe;
         HumanoidCombat.applyWeaponHit(this.dragon, weapon, weaponTarget);
      }
   }

   public void cancel() {
      if (this.active()) {
         TailSweepSync.cancel(this.dragon);
         this.timeline = null;
         this.target = null;
         this.previousImpact = null;
         this.attempted.clear();
         this.waterfowlWeapon = ItemStack.EMPTY;
         this.waterfowlDamageWeapon = ItemStack.EMPTY;
         this.waterfowlLeap = null;
         if (DragonForms.state(this.dragon).dragonmaid$getAction() == 6) {
            DragonForms.state(this.dragon).dragonmaid$setAction(0);
         }
      }
   }
}
