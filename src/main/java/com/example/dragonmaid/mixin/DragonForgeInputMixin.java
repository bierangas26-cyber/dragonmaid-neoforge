package com.example.dragonmaid.mixin;

import com.example.dragonmaid.combat.GuardEscort;
import com.example.dragonmaid.data.DragonForms;
import com.iafenvoy.iceandfire.registry.IafBlocks;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.item.block.entity.DragonForgeInputBlockEntity;
import iafcompat.IafCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(remap = false, value = {DragonForgeInputBlockEntity.class})
public abstract class DragonForgeInputMixin {
    public DragonForgeInputMixin() {
    }

    @Inject(remap = false, method = {"lureDragons"},
        at = {@At("HEAD")},
        cancellable = true)
    private void dragonmaid$lureAllVisibleDragons(CallbackInfo ci) {
        DragonForgeInputBlockEntity tileentitydragonforgeinput = (DragonForgeInputBlockEntity)(Object)this;
        Level level = tileentitydragonforgeinput.getLevel();
        if (level == null) {
            ci.cancel();
        } else {
            BlockPos blockpos = tileentitydragonforgeinput.getBlockPos();
            Block block = level.getBlockState(blockpos).getBlock();
            int i = block == IafBlocks.DRAGONFORGE_FIRE_INPUT.get()
                ? 0
                : (block == IafBlocks.DRAGONFORGE_ICE_INPUT.get() ? 1 : (block == IafBlocks.DRAGONFORGE_LIGHTNING_INPUT.get() ? 2 : -1));
            Vec3 vec3 = Vec3.atCenterOf(blockpos);
            AABB aabb = new AABB(blockpos).inflate(50.0);

            for (DragonBaseEntity entitydragonbase : level.getEntitiesOfClass(DragonBaseEntity.class, aabb)) {
                if (!DragonForms.isHumanoid(entitydragonbase)) {
                    boolean flag = blockpos.equals(entitydragonbase.burningTarget);
                    boolean flag1 = i >= 0
                        && IafCompat.dragonTypeIndex(entitydragonbase) == i
                        && entitydragonbase.isAlive()
                        && !entitydragonbase.isModelDead()
                        && !entitydragonbase.isSleeping()
                        && !entitydragonbase.isBaby()
                        && !DragonForms.state(entitydragonbase).dragonmaid$isRecoveryRest()
                        && entitydragonbase.distanceToSqr(vec3) < 115.0 * (double)entitydragonbase.getDragonStage();
                    if (flag1 && !entitydragonbase.isTame()) {
                        flag1 = GuardEscort.hasChain(entitydragonbase);
                    }

                    if (flag1) {
                        BlockHitResult blockhitresult = level.clip(
                            new ClipContext(
                                entitydragonbase.getEyePosition(), vec3, net.minecraft.world.level.ClipContext.Block.COLLIDER, Fluid.NONE, entitydragonbase
                            )
                        );
                        flag1 = blockhitresult.getType() == Type.MISS || blockpos.equals(blockhitresult.getBlockPos());
                    }

                    if (!flag1 || entitydragonbase.burningTarget != null && !flag) {
                        if (!flag1 && flag) {
                            entitydragonbase.burningTarget = null;
                            entitydragonbase.setBreathingFire(false);
                        }
                    } else {
                        entitydragonbase.burningTarget = blockpos;
                    }
                }
            }

            ci.cancel();
        }
    }
}
