package com.example.dragonmaid.mixin;

import com.example.dragonmaid.combat.HumanoidTargetGoal;
import com.example.dragonmaid.combat.SafeDragonCharges;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.DragonChargeEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(remap = false, value = {DragonChargeEntity.class})
public abstract class DragonChargeMixin {
    public DragonChargeMixin() {
    }

    @Inject(remap = false, method = {"onHit"},
        at = {@At("HEAD")},
        cancellable = true)
    private void dragonmaid$safeImpact(HitResult hit, CallbackInfo ci) {
        DragonChargeEntity entitydragoncharge = (DragonChargeEntity)(Object)this;
        if (SafeDragonCharges.isSafe(entitydragoncharge)) {
            SafeDragonCharges.impact(entitydragoncharge, hit);
            ci.cancel();
        }
    }

    @Inject(remap = false, method = {"canHitMob"},
        at = {@At("HEAD")},
        cancellable = true)
    private void dragonmaid$noFriendlyCollision(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        DragonChargeEntity entitydragoncharge = (DragonChargeEntity)(Object)this;
        if (SafeDragonCharges.isSafe(entitydragoncharge)) {
            boolean flag;
            label16: {
                if (entitydragoncharge.getOwner() instanceof DragonBaseEntity entitydragonbase
                    && entity instanceof LivingEntity livingentity
                    && HumanoidTargetGoal.isEnemy(entitydragonbase, livingentity)) {
                    flag = true;
                    break label16;
                }

                flag = false;
            }

            cir.setReturnValue(flag);
        }
    }

    @Inject(remap = false, method = {"tick"},
        at = {@At("HEAD")},
        cancellable = true)
    private void dragonmaid$expire(CallbackInfo ci) {
        DragonChargeEntity entitydragoncharge = (DragonChargeEntity)(Object)this;
        SafeDragonCharges.age(entitydragoncharge);
        if (entitydragoncharge.isRemoved()) {
            ci.cancel();
        }
    }
}
