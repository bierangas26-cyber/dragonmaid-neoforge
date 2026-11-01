package com.example.dragonmaid.mixin;

import com.example.dragonmaid.data.DragonForms;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(remap = false, value = {Mob.class})
public abstract class HumanoidIdleLookBodyMixin {
    public HumanoidIdleLookBodyMixin() {
    }

    @Inject(remap = false, method = {"tickHeadTurn"},
        at = {@At("HEAD")},
        cancellable = true)
    private void dragonmaid$keepIdleBody(float yaw, float amount, CallbackInfoReturnable<Float> cir) {
        if (((Object)this) instanceof DragonBaseEntity entitydragonbase
            && DragonForms.isHumanoid(entitydragonbase)
            && DragonForms.state(entitydragonbase).dragonmaid$isIdleLooking()) {
            double d0 = entitydragonbase.getX() - entitydragonbase.xo;
            double d1 = entitydragonbase.getZ() - entitydragonbase.zo;
            if (entitydragonbase.isAlive()
                && !entitydragonbase.isRemoved()
                && !entitydragonbase.isNoAi()
                && !entitydragonbase.isModelDead()
                && !entitydragonbase.isSleeping()
                && entitydragonbase.getCommand() != 1
                && !DragonForms.isSeated(entitydragonbase)
                && !entitydragonbase.isPassenger()
                && !entitydragonbase.isVehicle()
                && entitydragonbase.onGround()
                && !entitydragonbase.isInWaterOrBubble()
                && !entitydragonbase.isInLava()
                && !entitydragonbase.isTackling()
                && !entitydragonbase.isBreathingFire()
                && DragonForms.state(entitydragonbase).dragonmaid$getAction() == 0
                && !(d0 * d0 + d1 * d1 >= 4.0E-4)
                && !(entitydragonbase.getDeltaMovement().horizontalDistanceSqr() >= 4.0E-4)) {
                cir.setReturnValue(amount);
                return;
            }

            return;
        }
    }
}
