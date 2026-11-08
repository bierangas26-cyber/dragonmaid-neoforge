package com.example.dragonmaid.combat;

import com.example.dragonmaid.entity.ModEntities;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.LightningDragonChargeEntity;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class MiniatureLightningCharge extends LightningDragonChargeEntity {
   private static final EntityDataAccessor<Boolean> LARGE = SynchedEntityData.defineId(MiniatureLightningCharge.class, EntityDataSerializers.BOOLEAN);
   private int stage;
   private int life;
   private float damage;
   private double remaining;

   public MiniatureLightningCharge(EntityType<? extends MiniatureLightningCharge> type, Level level) {
      super(type, level);
   }

   static void launch(DragonBaseEntity dragon, Vec3 origin, Vec3 direction) {
      MiniatureLightningCharge shot = new MiniatureLightningCharge(
         (EntityType<? extends MiniatureLightningCharge>)ModEntities.MINI_LIGHTNING.get(), dragon.level()
      );
      shot.setOwner(dragon);
      shot.setPos(origin);
      shot.stage = dragon.getDragonStage();
      shot.entityData.set(LARGE, shot.stage >= 5);
      shot.damage = MiniatureCombat.damage(dragon);
      shot.remaining = (Double)MiniatureBalance.range.get();
      shot.setDeltaMovement(direction.scale((Double)MiniatureBalance.boltSpeed.get()));
      if (dragon.level().addFreshEntity(shot)) {
         MiniatureBreathSounds.launch(dragon, origin);
      }
   }

   public boolean enhanced() {
      return (Boolean)this.entityData.get(LARGE);
   }

   public double blastRadius() {
      return this.enhanced()
         ? Math.max((Double)MiniatureBalance.elderRadius.get(), (Double)MiniatureBalance.boltRadius.get())
         : (Double)MiniatureBalance.boltRadius.get();
   }

   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(LARGE, false);
   }

   public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
      super.onSyncedDataUpdated(key);
      if (LARGE.equals(key)) {
         this.refreshDimensions();
      }
   }

   public EntityDimensions getDimensions(Pose pose) {
      return EntityDimensions.scalable(this.enhanced() ? 0.48F : 0.28F, this.enhanced() ? 0.48F : 0.28F);
   }

   public void tick() {
      if (!this.level().isClientSide) {
         if ((double)(++this.life) > Math.ceil((Double)MiniatureBalance.range.get() / (Double)MiniatureBalance.boltSpeed.get()) + 2.0
            || this.remaining <= 1.0E-4
            || !(this.getOwner() instanceof DragonBaseEntity owner)
            || !owner.isAlive()
            || owner.isModelDead()) {
            this.discard();
            return;
         }

         double step = this.getDeltaMovement().length();
         if (step > this.remaining) {
            this.setDeltaMovement(this.getDeltaMovement().scale(this.remaining / step));
         }

         this.remaining = this.remaining - Math.min(step, this.remaining);
      }

      super.tick();
   }

   protected float getInertia() {
      return 1.0F;
   }

   protected ParticleOptions getTrailParticle() {
      return ParticleTypes.ELECTRIC_SPARK;
   }

   protected boolean canHitMob(Entity candidate) {
      if (this.getOwner() instanceof DragonBaseEntity dragon && candidate instanceof LivingEntity living && HumanoidTargetGoal.isEnemy(dragon, living)) {
         return true;
      }

      return false;
   }

   protected void onHit(HitResult hit) {
      if (!this.isRemoved() && this.level() instanceof ServerLevel level && hit.getType() != Type.MISS) {
         Vec3 center = this.contactPoint(hit).subtract(this.getDeltaMovement().normalize().scale(0.04));
         double radius = this.blastRadius();
         if (this.getOwner() instanceof DragonBaseEntity dragon && dragon.isAlive() && !dragon.isModelDead()) {
            for (LivingEntity enemy : level.getEntitiesOfClass(
               LivingEntity.class, new AABB(center, center).inflate(radius), target -> HumanoidTargetGoal.isEnemy(dragon, target)
            )) {
               AABB box = enemy.getBoundingBox();
               Vec3 sample = box.contains(center) ? center : box.clip(center, box.getCenter()).orElse(box.getCenter());
               if (center.distanceToSqr(sample) <= radius * radius
                  && MiniatureCombat.clear(dragon, center, box.getCenter())
                  && HumanoidCombat.damage(dragon, enemy, this.stage, this.damage)) {
                  enemy.knockback(this.enhanced() ? 0.65 : 0.4, dragon.getX() - enemy.getX(), dragon.getZ() - enemy.getZ());
               }
            }
         }

         level.sendParticles(
            ParticleTypes.ELECTRIC_SPARK,
            center.x,
            center.y,
            center.z,
            this.enhanced() ? 28 : 13,
            radius / 4.0,
            radius / 4.0,
            radius / 4.0,
            0.12
         );
         level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 1, 0.0, 0.0, 0.0, 0.0);
         if (!this.isSilent() && this.getOwner() instanceof DragonBaseEntity dragon) {
            MiniatureBreathSounds.impact(dragon, center, this.enhanced() ? 0.8F : 0.55F);
         }

         this.discard();
      }
   }

   private Vec3 contactPoint(HitResult hit) {
      if (hit instanceof EntityHitResult entityHit) {
         Vec3 from = this.position();
         AABB bounds = entityHit.getEntity().getBoundingBox().inflate(0.3F);
         return bounds.contains(from) ? from : bounds.clip(from, from.add(this.getDeltaMovement())).orElse(hit.getLocation());
      } else {
         return hit.getLocation();
      }
   }

   public void addAdditionalSaveData(CompoundTag nbt) {
      super.addAdditionalSaveData(nbt);
      nbt.putInt("ShotStage", this.stage);
      nbt.putFloat("ShotDamage", this.damage);
      nbt.putInt("ShotLife", this.life);
      nbt.putDouble("ShotRemaining", this.remaining);
   }

   public void readAdditionalSaveData(CompoundTag nbt) {
      super.readAdditionalSaveData(nbt);
      this.stage = nbt.getInt("ShotStage");
      this.entityData.set(LARGE, this.stage >= 5);
      this.damage = nbt.getFloat("ShotDamage");
      this.life = nbt.getInt("ShotLife");
      this.remaining = nbt.getDouble("ShotRemaining");
   }
}
