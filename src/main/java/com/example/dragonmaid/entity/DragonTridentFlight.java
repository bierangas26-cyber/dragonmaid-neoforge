package com.example.dragonmaid.entity;

import com.example.dragonmaid.accessory.DragonHands;
import com.example.dragonmaid.combat.GuardEscort;
import com.example.dragonmaid.combat.BladeCompat;
import com.example.dragonmaid.combat.HumanoidCombat;
import com.example.dragonmaid.combat.HumanoidTargetGoal;
import com.example.dragonmaid.combat.LongswordProjectiles;
import com.example.dragonmaid.combat.SpecialWeapons;
import com.example.dragonmaid.combat.TailSweepSync;
import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.data.HumanoidLife;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.GorgonEntity;
import javax.annotation.Nullable;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.projectile.AbstractArrow.Pickup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class DragonTridentFlight extends ThrownTrident {
   private static final int FLIGHT_TIMEOUT_TICKS = 80;
   private static final int RETURN_DELAY_TICKS = 10;
   private static final int WAITING = -1;
   public static final float LAUNCH_SPEED = 2.5F;
   private static final double DRAG = 0.99F;
   private static final double GRAVITY = 0.05F;
   private static final double[] AIM_HEIGHTS = new double[]{0.5, 0.9, 0.2};
   private static final EntityDataAccessor<Boolean> GLINT = SynchedEntityData.defineId(DragonTridentFlight.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> RETURN_STRENGTH = SynchedEntityData.defineId(DragonTridentFlight.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<ItemStack> DISPLAY_STACK = SynchedEntityData.defineId(DragonTridentFlight.class, EntityDataSerializers.ITEM_STACK);
   private ItemStack original = ItemStack.EMPTY;
   private long returnAt;
   private long returnStartsAt;
   private int loyalty;
   private LivingEntity reflector;

   public DragonTridentFlight(EntityType<? extends ThrownTrident> type, Level level) {
      super(type, level);
      this.pickup = Pickup.DISALLOWED;
   }

   public static Vec3 launchOrigin(DragonBaseEntity dragon) {
      return launchOrigin(dragon, dragon.yBodyRot, dragon.getXRot());
   }

   private static Vec3 launchOrigin(DragonBaseEntity dragon, float yaw, float pitchDegrees) {
      Vec3 forward = Vec3.directionFromRotation(0.0F, yaw);
      Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
      double scale = (double)DragonForms.modelScale(dragon) / 16.0;
      boolean airborne = BladeCompat.flightAirborne(dragon);
      double lift = airborne ? 2.3 : 2.8;
      double reach = airborne ? 12.5 : 8.4;
      float pitch = Mth.clamp(pitchDegrees * (float) (Math.PI / 180.0), -0.9F, 0.9F);
      double sin = (double)Mth.sin(pitch);
      double cos = (double)Mth.cos(pitch);
      return dragon.position()
         .add(right.scale((airborne ? 6.8 : 7.0) * scale))
         .add(0.0, (22.0 + lift * cos - reach * sin) * scale, 0.0)
         .add(forward.scale((reach * cos + lift * sin) * scale));
   }

   @Nullable
   public static DragonTridentFlight.Shot prepareTargetShot(DragonBaseEntity dragon, LivingEntity target) {
      return targetShot(dragon, target, true);
   }

   @Nullable
   public static DragonTridentFlight.Shot targetShot(DragonBaseEntity dragon, LivingEntity target) {
      return targetShot(dragon, target, false);
   }

   @Nullable
   private static DragonTridentFlight.Shot targetShot(DragonBaseEntity dragon, LivingEntity target, boolean afterTurning) {
      Vec3 eye = dragon.getEyePosition();
      Vec3 currentOrigin = launchOrigin(dragon);
      AABB box = target.getBoundingBox();
      Vec3 center = box.getCenter();

      for (double height : AIM_HEIGHTS) {
         Vec3 aim = new Vec3(center.x, box.minY + box.getYsize() * height, center.z);
         if (clearSegment(dragon, eye, aim)) {
            Vec3 origin = currentOrigin;
            if (afterTurning) {
               Vec3 offset = aim.subtract(eye);
               double horizontal = offset.horizontalDistanceSqr();
               float yaw = horizontal > 1.0E-4 ? (float)(Mth.atan2(offset.z, offset.x) * 180.0F / (float)Math.PI) - 90.0F : dragon.yBodyRot;
               float pitch = (float)(-Math.atan2(offset.y, Math.sqrt(horizontal)) * 180.0F / (float)Math.PI);
               origin = launchOrigin(dragon, yaw, pitch);
            }

            if (clearSegment(dragon, eye, origin)) {
               DragonTridentFlight.Ballistic arc = ballistic(aim.subtract(origin));
               if (clearArc(dragon, origin, arc)) {
                  return new DragonTridentFlight.Shot(aim, origin, arc.velocity());
               }
            }
         }
      }

      return null;
   }

   private static boolean clearSegment(DragonBaseEntity dragon, Vec3 start, Vec3 end) {
      return dragon.level().clip(new ClipContext(start, end, Block.COLLIDER, Fluid.NONE, dragon)).getType() == Type.MISS;
   }

   private static boolean clearArc(DragonBaseEntity dragon, Vec3 point, DragonTridentFlight.Ballistic arc) {
      Vec3 velocity = arc.velocity();

      for (int tick = 0; (double)tick < arc.flightTicks(); tick++) {
         Vec3 next = point.add(velocity.scale(Math.min(1.0, arc.flightTicks() - (double)tick)));
         if (!clearSegment(dragon, point, next)) {
            return false;
         }

         point = next;
         velocity = velocity.scale(0.99F).add(0.0, -0.05F, 0.0);
      }

      return true;
   }

   @Nullable
   public static DragonTridentFlight launchAt(DragonBaseEntity dragon, Vec3 target) {
      Vec3 origin = launchOrigin(dragon);
      Vec3 velocity = ballistic(target.subtract(origin)).velocity();
      return launch(dragon, origin, velocity, (float)velocity.length());
   }

   @Nullable
   public static DragonTridentFlight launchAt(DragonBaseEntity dragon, DragonTridentFlight.Shot shot) {
      return launch(dragon, shot.origin(), shot.velocity(), (float)shot.velocity().length());
   }

   private static DragonTridentFlight.Ballistic ballistic(Vec3 offset) {
      double horizontalSqr = offset.horizontalDistanceSqr();
      double travel = 0.0;
      double drop = 0.0;
      double step = 1.0;
      double fall = 0.0;
      double bestSpeedSqr = Double.POSITIVE_INFINITY;
      double bestTravel = 1.0;
      double bestDrop = 0.0;
      int bestTick = 1;

      for (int tick = 1; tick <= 80; tick++) {
         double nextTravel = travel + step;
         double nextDrop = drop + fall;
         double y = offset.y + nextDrop;
         double speedSqr = (horizontalSqr + y * y) / (nextTravel * nextTravel);
         if (speedSqr <= 6.25) {
            double lower = 0.0;
            double upper = 1.0;

            for (int i = 0; i < 12; i++) {
               double fraction = (lower + upper) * 0.5;
               double distance = travel + step * fraction;
               double height = offset.y + drop + fall * fraction;
               if (horizontalSqr + height * height > 6.25 * distance * distance) {
                  lower = fraction;
               } else {
                  upper = fraction;
               }
            }

            return new DragonTridentFlight.Ballistic(offset.add(0.0, drop + fall * upper, 0.0).normalize().scale(2.5), (double)(tick - 1) + upper);
         }

         if (speedSqr < bestSpeedSqr) {
            bestSpeedSqr = speedSqr;
            bestTravel = nextTravel;
            bestDrop = nextDrop;
            bestTick = tick;
         }

         travel = nextTravel;
         drop = nextDrop;
         step *= 0.99F;
         fall = fall * 0.99F + 0.05F;
      }

      return new DragonTridentFlight.Ballistic(offset.add(0.0, bestDrop, 0.0).scale(1.0 / bestTravel), (double)bestTick);
   }

   @Nullable
   public static DragonTridentFlight launch(DragonBaseEntity dragon, Vec3 direction, float speed) {
      return launch(dragon, launchOrigin(dragon), direction, speed);
   }

   @Nullable
   private static DragonTridentFlight launch(DragonBaseEntity dragon, Vec3 origin, Vec3 direction, float speed) {
      if (dragon.level() instanceof ServerLevel level
         && canHoldFlight(dragon)
         && !TailSweepSync.isBusy(dragon)
         && Double.isFinite(direction.lengthSqr())
         && !(direction.lengthSqr() < 1.0E-6)
         && Float.isFinite(speed)
         && !(speed <= 0.0F)) {
         DragonHands hands = DragonHands.inventory(dragon);
         ItemStack weapon = DragonHands.mainHand(dragon);
         if (!BladeCompat.tridentAway(dragon) && SpecialWeapons.throwableIceTrident(dragon, weapon)) {
            if (!clearSegment(dragon, dragon.getEyePosition(), origin)) {
               return null;
            }

            DragonTridentFlight flight = new DragonTridentFlight((EntityType<? extends ThrownTrident>)BladeCompat.tridentFlightType(), level);
            flight.original = weapon;
            flight.returnAt = level.getGameTime() + 80L;
            flight.loyalty = BladeCompat.loyalty(weapon);
            flight.entityData.set(GLINT, weapon.hasFoil());
            flight.entityData.set(DISPLAY_STACK, weapon.copyWithCount(1));
            flight.setOwner(dragon);
            flight.setPos(origin);
            flight.shoot(direction.x, direction.y, direction.z, speed, 0.0F);
            if (level.addFreshEntity(flight) && BladeCompat.trackFlight(dragon, flight)) {
               dragon.playSound(SoundEvents.TRIDENT_THROW.value(), 1.0F, 1.0F);
               return flight;
            }

            flight.discard();
            return null;
         }

         return null;
      }

      return null;
   }

   public long returnAt() {
      return this.returnAt;
   }

   public int loyalty() {
      return this.loyalty;
   }

   public void markReflectedBy(LivingEntity defender) {
      this.reflector = defender;
   }

   public boolean validFor(DragonBaseEntity dragon) {
      return dragon.level() == this.level()
         && this.getOwner() == dragon
         && this.level().getGameTime() < this.returnAt
         && canHoldFlight(dragon)
         && DragonHands.mainHand(dragon) == this.original
         && SpecialWeapons.throwableIceTrident(dragon, this.original);
   }

   private static boolean canHoldFlight(DragonBaseEntity dragon) {
      return dragon.isAddedToLevel()
         && dragon.isAlive()
         && !dragon.isRemoved()
         && !dragon.isModelDead()
         && !HumanoidLife.isInjured(dragon)
         && !GorgonEntity.isStoneMob(dragon)
         && !dragon.isNoAi()
         && !dragon.isSleeping()
         && !dragon.isPassenger()
         && (
            dragon.isVehicle()
               ? dragon.getControllingPassenger() instanceof Player pilot && BladeCompat.flightValidRider(dragon, pilot)
               : dragon.getCommand() != 1 && !dragon.isOrderedToSit()
         );
   }

   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(GLINT, false);
      builder.define(RETURN_STRENGTH, 0);
      builder.define(DISPLAY_STACK, ItemStack.EMPTY);
   }

   public boolean isFoil() {
      return (Boolean)this.entityData.get(GLINT);
   }

   public ItemStack displayStack() {
      return (ItemStack)this.entityData.get(DISPLAY_STACK);
   }

   public void tick() {
      Entity owner = this.getOwner();
      int strength = (Integer)this.entityData.get(RETURN_STRENGTH);
      if (!this.level().isClientSide) {
         if (!(owner instanceof DragonBaseEntity dragon) || !this.validFor(dragon) || !BladeCompat.ownsFlight(dragon, this)) {
            this.discard();
            return;
         }

         if (strength == -1 && this.level().getGameTime() >= this.returnStartsAt) {
            strength = this.loyalty + 1;
            this.entityData.set(RETURN_STRENGTH, strength);
            this.setNoPhysics(true);
            this.hasImpulse = true;
            this.playSound(SoundEvents.TRIDENT_RETURN, 1.0F, 1.0F);
         }
      }

      if (strength == -1) {
         this.setDeltaMovement(Vec3.ZERO);
      } else {
         if (strength > 0 && owner instanceof DragonBaseEntity dragon) {
            this.flyBack(dragon, strength);
            return;
         }

         super.tick();
      }
   }

   private void flyBack(DragonBaseEntity dragon, int strength) {
      Vec3 forward = Vec3.directionFromRotation(0.0F, dragon.yBodyRot);
      double scale = (double)DragonForms.modelScale(dragon) / 16.0;
      Vec3 hand = dragon.position().add(-forward.z * 5.5 * scale, 12.5 * scale, forward.x * 5.5 * scale);
      Vec3 start = this.position();
      Vec3 toHand = hand.subtract(start);
      this.setPos(start.add(0.0, toHand.y * 0.015 * (double)strength, 0.0));
      this.setDeltaMovement(this.getDeltaMovement().scale(0.95).add(toHand.normalize().scale(0.05 * (double)strength)));
      super.tick();
      if (!this.level().isClientSide) {
         Vec3 step = this.position().subtract(start);
         double along = Mth.clamp(toHand.dot(step) / Math.max(step.lengthSqr(), 1.0E-8), 0.0, 1.0);
         if (toHand.subtract(step.scale(along)).lengthSqr() <= 0.12249999999999998) {
            BladeCompat.finishFlight(dragon, this);
         }
      }
   }

   private void waitAfterImpact() {
      this.returnStartsAt = this.level().getGameTime() + 10L;
      this.returnAt = this.returnStartsAt + 80L;
      this.entityData.set(RETURN_STRENGTH, -1);
      this.setDeltaMovement(Vec3.ZERO);
      this.setNoGravity(true);
      this.hasImpulse = true;
   }

   protected void onHitBlock(BlockHitResult hit) {
      super.onHitBlock(hit);
      if (!this.level().isClientSide) {
         this.setPos(hit.getLocation().subtract(this.getDeltaMovement().normalize().scale(0.05)));
         this.waitAfterImpact();
      }
   }

   protected boolean canHitEntity(Entity entity) {
      return !this.level().isClientSide
         && (Integer)this.entityData.get(RETURN_STRENGTH) == 0
         && this.getOwner() instanceof DragonBaseEntity dragon
         && entity instanceof LivingEntity living
         && super.canHitEntity(entity)
         && (
            this.reflector != null
               ? living == dragon
                  && (
                     this.reflector instanceof DragonBaseEntity reflectingDragon
                        ? HumanoidTargetGoal.isEnemy(reflectingDragon, living)
                        : LongswordProjectiles.canReturnTo(this.reflector, living)
                  )
               : HumanoidTargetGoal.isEnemy(dragon, living) && GuardEscort.reachableTarget(dragon, living)
         );
   }

   protected void onHitEntity(EntityHitResult hit) {
      if (this.canHitEntity(hit.getEntity()) && this.getOwner() instanceof DragonBaseEntity dragon && hit.getEntity() instanceof LivingEntity victim) {
         this.setPos(hit.getLocation());
         this.waitAfterImpact();
         if (this.reflector != null) {
            victim.hurt(this.damageSources().trident(this, this.reflector), 8.0F + BladeCompat.damageBonus(this.original, victim));
         } else if (HumanoidCombat.thrownTrident(dragon, victim, this.original, this.damageSources().trident(this, dragon))) {
            BladeCompat.postAttackEffects(dragon, victim);
            this.original.hurtAndBreak(1, dragon, EquipmentSlot.MAINHAND);
            DragonHands.inventory(dragon).setChanged();
         }

         this.playSound(SoundEvents.TRIDENT_HIT, 1.0F, 1.0F);
      }
   }

   public void playerTouch(Player player) {
   }

   protected ItemStack getPickupItem() {
      return ItemStack.EMPTY;
   }

   private static record Ballistic(Vec3 velocity, double flightTicks) {
   }

   public static record Shot(Vec3 aim, Vec3 origin, Vec3 velocity) {
   }
}