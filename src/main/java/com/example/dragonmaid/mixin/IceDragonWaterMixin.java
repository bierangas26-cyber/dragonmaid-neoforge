package com.example.dragonmaid.mixin;

import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.data.HumanoidWater;
import com.iafenvoy.iceandfire.entity.IceDragonEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(remap = false, value = {IceDragonEntity.class})
public abstract class IceDragonWaterMixin {
    public IceDragonWaterMixin() {
    }

    @Inject(remap = false, method = {"travel"},
        at = {@At("HEAD")},
        cancellable = true)
    private void dragonmaid$humanWaterTravel(Vec3 movement, CallbackInfo ci) {
        IceDragonEntity entityicedragon = (IceDragonEntity)(Object)this;
        if (DragonForms.isHumanoid(entityicedragon) && HumanoidWater.travel(entityicedragon, movement)) {
            ci.cancel();
        }
    }

    @Inject(remap = false, method = {"useFlyingPathFinder"},
        at = {@At("HEAD")},
        cancellable = true)
    private void dragonmaid$keepAmphibiousNavigation(CallbackInfoReturnable<Boolean> cir) {
        if (DragonForms.isHumanoid((IceDragonEntity)(Object)this)) {
            cir.setReturnValue(false);
        }
    }
}
