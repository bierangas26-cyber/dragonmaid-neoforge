package com.example.dragonmaid.data;

import com.example.dragonmaid.movement.HumanoidMoveControl;
import com.example.dragonmaid.movement.WaterMotion;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.IceDragonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class HumanoidWater {
    private HumanoidWater() {
    }

    public static boolean active(DragonBaseEntity dragon) {
        return dragon.isInWater() && !dragon.isPassenger() && !dragon.isSleeping() && !dragon.isModelDead();
    }

    public static boolean submerged(DragonBaseEntity dragon) {
        return active(dragon) && dragon.isEyeInFluid(FluidTags.WATER);
    }

    public static boolean canDive(DragonBaseEntity dragon) {
        return dragon instanceof IceDragonEntity;
    }

    public static Vec3 breathOrigin(DragonBaseEntity dragon) {
        if (DragonForms.isHumanoid(dragon) && canDive(dragon) && submerged(dragon) && !dragon.onGround()) {
            double d0 = (double)DragonForms.modelScale(dragon);
            double d1 = Math.toRadians((double)dragon.yBodyRot);
            double d2 = Math.toRadians((double)dragon.getYHeadRot());
            double d3 = Math.toRadians((double)Math.max(-90.0F, Math.min(90.0F, dragon.getXRot())));
            Vec3 vec3 = dragon.position().add(0.0, 0.3 * d0, 0.0);
            Vec3 vec31 = new Vec3(-Math.sin(d1), 0.0, Math.cos(d1));
            Vec3 vec32 = new Vec3(-Math.sin(d2), 0.0, Math.cos(d2));
            Vec3 vec33 = vec3.add(vec31.scale(1.15 * d0 * Math.cos(d3)))
                .add(vec32.scale(0.25 * d0 * Math.cos(d3)))
                .add(0.0, -1.15 * d0 * Math.sin(d3), 0.0);
            BlockHitResult blockhitresult = dragon.level().clip(new ClipContext(vec3, vec33, Block.COLLIDER, Fluid.NONE, dragon));
            if (blockhitresult.getType() == Type.BLOCK) {
                Vec3 vec34 = blockhitresult.getLocation().subtract(vec3);
                return vec34.lengthSqr() < 1.0E-4 ? vec3 : blockhitresult.getLocation().subtract(vec34.normalize().scale(0.01));
            } else {
                return vec33;
            }
        } else {
            return dragon.getEyePosition();
        }
    }

    public static boolean travel(DragonBaseEntity dragon, Vec3 movement) {
        if (!active(dragon)) {
            return false;
        } else {
            if (dragon.isControlledByLocalInstance()) {
                HumanoidMoveControl humanoidmovecontrol = dragon.getMoveControl() instanceof HumanoidMoveControl humanoidmovecontrol1 ? humanoidmovecontrol1 : null;
                boolean flag1 = humanoidmovecontrol != null && humanoidmovecontrol.steering();
                double d0 = canDive(dragon) && submerged(dragon)
                    ? 0.0
                    : WaterMotion.surfaceError(dragon.getFluidHeight(FluidTags.WATER), (double)dragon.getEyeHeight());
                boolean flag = false;
                if (flag1) {
                    BlockPos blockpos = BlockPos.containing(humanoidmovecontrol.getWantedX(), humanoidmovecontrol.getWantedY(), humanoidmovecontrol.getWantedZ());
                    BlockPos blockpos1 = BlockPos.containing(
                        humanoidmovecontrol.getWantedX(), humanoidmovecontrol.getWantedY() + (double)dragon.getEyeHeight(), humanoidmovecontrol.getWantedZ()
                    );
                    if (dragon.level().hasChunkAt(blockpos)) {
                        if (canDive(dragon) && dragon.level().getFluidState(blockpos1).is(FluidTags.WATER)) {
                            d0 = humanoidmovecontrol.getWantedY() - dragon.getY();
                        } else if (dragon.horizontalCollision
                            && humanoidmovecontrol.getWantedY() > dragon.getY() + 0.2
                            && !dragon.level().getFluidState(blockpos).is(FluidTags.WATER)
                            && !dragon.level().getBlockState(blockpos.below()).getCollisionShape(dragon.level(), blockpos.below()).isEmpty()
                            && Math.abs(humanoidmovecontrol.getWantedX() - dragon.getX()) < 2.0
                            && Math.abs(humanoidmovecontrol.getWantedZ() - dragon.getZ()) < 2.0) {
                            flag = true;
                            d0 = humanoidmovecontrol.getWantedY() - dragon.getY();
                        } else {
                            d0 = WaterMotion.surfaceError(dragon.getFluidHeight(FluidTags.WATER), (double)dragon.getEyeHeight());
                        }
                    }
                }

                if (flag1) {
                    dragon.moveRelative(dragon.getSpeed() * 0.2F, new Vec3(movement.x, 0.0, movement.z));
                }

                Vec3 vec3 = dragon.getDeltaMovement();
                dragon.setDeltaMovement(vec3.x, WaterMotion.verticalSpeed(vec3.y, d0, flag), vec3.z);
                dragon.move(MoverType.SELF, dragon.getDeltaMovement());
                dragon.setDeltaMovement(dragon.getDeltaMovement().multiply(0.8, 1.0, 0.8));
                dragon.fallDistance = 0.0F;
            }

            dragon.calculateEntityAnimation(false);
            return true;
        }
    }

    public static Vec3 wander(DragonBaseEntity dragon) {
        if (canDive(dragon) && active(dragon)) {
            for (int i = 0; i < 10; i++) {
                BlockPos blockpos = dragon.blockPosition()
                    .offset(dragon.getRandom().nextInt(13) - 6, dragon.getRandom().nextInt(7) - 3, dragon.getRandom().nextInt(13) - 6);
                Vec3 vec3 = Vec3.atBottomCenterOf(blockpos);
                BlockPos blockpos1 = BlockPos.containing(vec3.add(0.0, (double)dragon.getBbHeight(), 0.0));
                if (dragon.level().hasChunkAt(blockpos)
                    && dragon.level().getFluidState(blockpos).is(FluidTags.WATER)
                    && dragon.level().getFluidState(blockpos1).is(FluidTags.WATER)
                    && dragon.level().noCollision(dragon, dragon.getBoundingBox().move(vec3.subtract(dragon.position())))) {
                    return vec3;
                }
            }

            return null;
        } else {
            return null;
        }
    }
}
