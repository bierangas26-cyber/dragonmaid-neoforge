package com.example.dragonmaid.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class ReinforcedHornItemEntity extends ItemEntity {
    private static final String RISING = "DragonMaidHornRising";
    private static final String HOVERING = "DragonMaidHornHovering";
    private boolean rising;
    private boolean hovering;

    public ReinforcedHornItemEntity(EntityType<? extends ItemEntity> type, Level level) {
        super(type, level);
        this.setUnlimitedLifetime();
        this.lifespan = Integer.MAX_VALUE;
    }

    public static ReinforcedHornItemEntity fromDrop(Level level, ItemEntity original, ItemStack stack) {
        ReinforcedHornItemEntity reinforcedhornitementity = new ReinforcedHornItemEntity(
            (EntityType<? extends ItemEntity>)ModEntities.REINFORCED_HORN.get(), level
        );
        reinforcedhornitementity.load(original.saveWithoutId(new CompoundTag()));
        reinforcedhornitementity.setItem(stack.copy());
        return reinforcedhornitementity;
    }

    public void tick() {
        if (!this.level().isClientSide && !this.getItem().isEmpty()) {
            boolean flag = this.getY() < (double)this.level().getMinBuildHeight() + 1.0;
            boolean flag1 = this.isInWater() || this.isInLava();
            if (flag || flag1) {
                this.rising = true;
                this.hovering = false;
                this.setNoGravity(true);
                Vec3 vec3 = this.getDeltaMovement();
                double d0 = flag ? 0.16 : 0.08;
                this.setDeltaMovement(vec3.x * 0.85, d0, vec3.z * 0.85);
                this.hasImpulse = true;
                this.fallDistance = 0.0F;
            } else if (this.rising || this.hovering) {
                this.rising = false;
                this.hovering = true;
                this.setNoGravity(true);
                if (this.getDeltaMovement().lengthSqr() != 0.0) {
                    this.setDeltaMovement(Vec3.ZERO);
                    this.hasImpulse = true;
                }

                this.fallDistance = 0.0F;
            }
        }

        super.tick();
    }

    public boolean fireImmune() {
        return true;
    }

    public boolean ignoreExplosion() {
        return true;
    }

    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    protected void onBelowWorld() {
    }

    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("DragonMaidHornRising", this.rising);
        tag.putBoolean("DragonMaidHornHovering", this.hovering);
    }

    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.rising = tag.getBoolean("DragonMaidHornRising");
        this.hovering = tag.getBoolean("DragonMaidHornHovering");
        this.setUnlimitedLifetime();
        this.lifespan = Integer.MAX_VALUE;
        if (this.rising || this.hovering) {
            this.setNoGravity(true);
        }
    }

    public int getAge() {
        return this.tickCount;
    }
}
