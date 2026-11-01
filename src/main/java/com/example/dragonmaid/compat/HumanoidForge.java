package com.example.dragonmaid.compat;

import com.example.dragonmaid.combat.GuardEscort;
import com.example.dragonmaid.combat.HumanoidCombat;
import com.example.dragonmaid.combat.HumanoidTargetGoal;
import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.data.HumanoidWater;
import com.iafenvoy.iceandfire.registry.IafBlocks;
import iafcompat.IafCompat;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.item.block.entity.DragonForgeBlockEntity;
import com.iafenvoy.iceandfire.item.block.entity.DragonForgeInputBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class HumanoidForge {
    public static final double RANGE = 8.0;
    public static final double ESCORT_WORK_DISTANCE = 8.0;
    private static final int SEARCH_RADIUS = (int)Math.ceil(8.0);
    public static final int ACTION = 4;

    private HumanoidForge() {
    }

    public static boolean eligible(DragonBaseEntity dragon) {
        return DragonForms.isHumanoid(dragon)
            && dragon.isTame()
            && dragon.getDragonStage() >= 3
            && !DragonForms.state(dragon).dragonmaid$isRecoveryRest()
            && dragon.isAlive()
            && !dragon.isModelDead()
            && !dragon.isSleeping()
            && !dragon.isNoAi()
            && !dragon.isPassenger()
            && !dragon.isVehicle()
            && commandAllowsWork(dragon)
            && (dragon.getTarget() == null || !HumanoidTargetGoal.isEnemy(dragon, dragon.getTarget()));
    }

    private static boolean commandAllowsWork(DragonBaseEntity dragon) {
        int i = dragon.getCommand();
        if (i == 0 || i == 1) {
            return true;
        } else if (i != 2) {
            return false;
        } else {
            LivingEntity livingentity = GuardEscort.owner(dragon);
            return livingentity != null && dragon.distanceToSqr(livingentity) <= 64.0;
        }
    }

    public static DragonForgeBlockEntity readyCore(DragonBaseEntity dragon, BlockPos input) {
        if (input != null && eligible(dragon) && dragon.level().hasChunkAt(input)) {
            Level level = dragon.level();
            if (!(level.getBlockEntity(input) instanceof DragonForgeInputBlockEntity)) {
                return null;
            } else {
                Block block = level.getBlockState(input).getBlock();
                int i = block == IafBlocks.DRAGONFORGE_FIRE_INPUT.get()
                    ? 0
                    : (block == IafBlocks.DRAGONFORGE_ICE_INPUT.get() ? 1 : (block == IafBlocks.DRAGONFORGE_LIGHTNING_INPUT.get() ? 2 : -1));
                if (i != IafCompat.dragonTypeIndex(dragon)) {
                    return null;
                } else {
                    Vec3 vec3 = Vec3.atCenterOf(input);
                    Vec3 vec31 = HumanoidWater.breathOrigin(dragon);
                    if (vec31.distanceToSqr(vec3) > 64.0) {
                        return null;
                    } else {
                        BlockHitResult blockhitresult = level.clip(
                            new ClipContext(vec31, vec3, net.minecraft.world.level.ClipContext.Block.COLLIDER, Fluid.NONE, dragon)
                        );
                        if (blockhitresult.getType() != Type.MISS && !blockhitresult.getBlockPos().equals(input)) {
                            return null;
                        } else {
                            for (Direction direction : Plane.HORIZONTAL) {
                                BlockPos blockpos = input.relative(direction);
                                if (level.hasChunkAt(blockpos)
                                    && level.getBlockEntity(blockpos) instanceof DragonForgeBlockEntity tileentitydragonforge
                                    && dragon.dragonType.equals(tileentitydragonforge.getDragonType())
                                    && tileentitydragonforge.assembled()
                                    && tileentitydragonforge.canSmelt()) {
                                    return tileentitydragonforge;
                                }
                            }

                            return null;
                        }
                    }
                }
            }
        } else {
            return null;
        }
    }

    public static boolean isWorking(DragonBaseEntity dragon) {
        return DragonForms.state(dragon).dragonmaid$getAction() == 4 && readyCore(dragon, dragon.burningTarget) != null;
    }

    public static boolean tryWork(DragonBaseEntity dragon) {
        if (dragon.level().isClientSide) {
            return false;
        } else {
            DragonForgeBlockEntity tileentitydragonforge = readyCore(dragon, dragon.burningTarget);
            if (tileentitydragonforge == null) {
                dragon.burningTarget = null;
                if (eligible(dragon) && dragon.tickCount % 20 == 0) {
                    Vec3 vec3 = HumanoidWater.breathOrigin(dragon);
                    BlockPos blockpos = BlockPos.containing(vec3);
                    double d0 = Double.MAX_VALUE;

                    for (BlockPos blockpos1 : BlockPos.betweenClosed(
                        blockpos.offset(-SEARCH_RADIUS, -SEARCH_RADIUS, -SEARCH_RADIUS), blockpos.offset(SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS)
                    )) {
                        double d2 = vec3.distanceToSqr(Vec3.atCenterOf(blockpos1));
                        if (!(d2 > 64.0) && !(d2 >= d0) && dragon.level().hasChunkAt(blockpos1)) {
                            Block block = dragon.level().getBlockState(blockpos1).getBlock();
                            if (block == IafBlocks.DRAGONFORGE_FIRE_INPUT.get()
                                || block == IafBlocks.DRAGONFORGE_ICE_INPUT.get()
                                || block == IafBlocks.DRAGONFORGE_LIGHTNING_INPUT.get()) {
                                DragonForgeBlockEntity tileentitydragonforge1 = readyCore(dragon, blockpos1);
                                if (tileentitydragonforge1 != null) {
                                    tileentitydragonforge = tileentitydragonforge1;
                                    dragon.burningTarget = blockpos1.immutable();
                                    d0 = d2;
                                }
                            }
                        }
                    }
                }
            }

            if (tileentitydragonforge == null) {
                if (DragonForms.state(dragon).dragonmaid$getAction() == 4) {
                    HumanoidCombat.stopBreath(dragon);
                    DragonForms.state(dragon).dragonmaid$setAction(0);
                }

                return false;
            } else {
                Vec3 vec31 = Vec3.atCenterOf(dragon.burningTarget);
                dragon.getNavigation().stop();
                dragon.getLookControl().setLookAt(vec31.x, vec31.y, vec31.z, 30.0F, 30.0F);
                if (dragon.getCommand() == 1 && dragon.isOrderedToSit()) {
                    double d3 = vec31.x - dragon.getX();
                    double d1 = vec31.z - dragon.getZ();
                    if (d3 * d3 + d1 * d1 > 1.0E-8) {
                        float f = (float)(Mth.atan2(d1, d3) * (180.0 / Math.PI)) - 90.0F;
                        float f1 = Mth.clamp(Mth.wrapDegrees(f - dragon.getYRot()), -10.0F, 10.0F);
                        dragon.setYRot(dragon.getYRot() + f1);
                        dragon.yBodyRot = dragon.getYRot();
                    }

                    dragon.getLookControl().tick();
                }

                DragonForms.state(dragon).dragonmaid$setAction(4);
                ForgeCooperation.fuelHuman(tileentitydragonforge, dragon);
                HumanoidCombat.forgeStream(dragon, vec31);
                return true;
            }
        }
    }
}
