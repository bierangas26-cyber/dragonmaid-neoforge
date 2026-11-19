package com.example.dragonmaid.mixin;

import com.example.dragonmaid.combat.HumanoidIdleLook;
import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.movement.HumanoidFlight;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.2: 人形龙在"站定/坐着"时不要被原版 {@code Mob#tickHeadTurn} 拽着改身体朝向。
 *
 * <p>1.2.7 把两件事合进了同一个 {@code tickHeadTurn} 注入：</p>
 * <ol>
 *   <li>自主飞行中（{@link HumanoidFlight#autonomousInAir}）：身体锁定当前 yaw、头只按
 *       {@code maxHeadYRot} 偏转，避免飞行时被抬头逻辑扭身。</li>
 *   <li>坐着发呆（{@code HumanoidIdleLook.seatedGlance}）：限位头部并保持身体。</li>
 * </ol>
 * 两者都要求"原版 tickHeadTurn 照旧移动身体"（即返回传入的 {@code movement}），
 * 因此共用一次 {@code @At("HEAD")} 注入。</p>
 */
@Mixin(remap = false, value = {Mob.class})
public abstract class HumanoidIdleLookBodyMixin {
    public HumanoidIdleLookBodyMixin() {
    }

    @Inject(remap = false, method = {"tickHeadTurn"},
        at = {@At("HEAD")},
        cancellable = true)
    private void dragonmaid$preserveAim(float bodyYaw, float movement, CallbackInfoReturnable<Float> callback) {
        if (!((Object)this instanceof DragonBaseEntity dragon)) {
            return;
        }

        if (HumanoidFlight.autonomousInAir(dragon)) {
            float facing = dragon.getYRot();
            dragon.setYBodyRot(facing);
            dragon.setYHeadRot(Mth.rotateIfNecessary(dragon.getYHeadRot(), facing, (float)dragon.getMaxHeadYRot()));
            callback.setReturnValue(movement);
            return;
        }

        if (DragonForms.isHumanoid(dragon)
            && DragonForms.state(dragon).dragonmaid$isIdleLooking()) {
            double d0 = dragon.getX() - dragon.xo;
            double d1 = dragon.getZ() - dragon.zo;
            if (dragon.isAlive()
                && !dragon.isRemoved()
                && !dragon.isNoAi()
                && !dragon.isModelDead()
                && !dragon.isSleeping()
                && dragon.getCommand() != 1
                && !DragonForms.isSeated(dragon)
                && !dragon.isPassenger()
                && !dragon.isVehicle()
                && dragon.onGround()
                && !dragon.isInWaterOrBubble()
                && !dragon.isInLava()
                && !dragon.isTackling()
                && !dragon.isBreathingFire()
                && DragonForms.state(dragon).dragonmaid$getAction() == 0
                && !(d0 * d0 + d1 * d1 >= 4.0E-4)
                && !(dragon.getDeltaMovement().horizontalDistanceSqr() >= 4.0E-4)) {
                callback.setReturnValue(movement);
                return;
            }
        }

        if (HumanoidIdleLook.seatedGlance(dragon)) {
            HumanoidIdleLook.limitSeatedHead(dragon);
            callback.setReturnValue(movement);
        }
    }
}
