package com.example.dragonmaid.mixin;

import com.example.dragonmaid.data.DragonForms;
import com.iafenvoy.iceandfire.entity.FireDragonEntity;
import com.iafenvoy.iceandfire.entity.IceDragonEntity;
import com.iafenvoy.iceandfire.entity.LightningDragonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(remap = false, value = {FireDragonEntity.class, IceDragonEntity.class, LightningDragonEntity.class})
public abstract class DragonVariantMixin {
    public DragonVariantMixin() {
    }

    @Inject(remap = false, method = {"aiStep"},
        at = {@At(
            value = "INVOKE",
            target = "Lcom/iafenvoy/iceandfire/entity/DragonBaseEntity;aiStep()V",
            shift = Shift.AFTER
        )},
        cancellable = true)
    private void dragonmaid$smallBody(CallbackInfo ci) {
        if (DragonForms.isHumanoid((Entity)(Object)this)) {
            ci.cancel();
        }
    }

    @Inject(remap = false, method = {"doHurtTarget"},
        at = {@At("HEAD")},
        cancellable = true)
    private void dragonmaid$customMelee(Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (DragonForms.isHumanoid((Entity)(Object)this)) {
            cir.setReturnValue(false);
        }
    }

    /**
     * IAF 2.1.13 的 stimulateFire(x,y,z,sync) 在 CE 2.0 已重构为每个子类 override 的
     * breathFireAtPos(BlockPos)（父类 DragonBaseEntity 里是 protected abstract）。
     */
    @Inject(remap = false, method = {"breathFireAtPos"},
        at = {@At("HEAD")},
        cancellable = true)
    private void dragonmaid$noTerrainBreath(BlockPos pos, CallbackInfo ci) {
        if (DragonForms.isHumanoid((Entity)(Object)this)) {
            ci.cancel();
        }
    }
}
