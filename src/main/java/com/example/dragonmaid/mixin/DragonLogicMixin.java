package com.example.dragonmaid.mixin;

import com.example.dragonmaid.data.DragonForms;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.util.dragon.IafDragonLogic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(remap = false, value = {IafDragonLogic.class})
public abstract class DragonLogicMixin {
    @Shadow(remap = false)
    @Final
    private DragonBaseEntity dragon;

    public DragonLogicMixin() {
    }

    @Inject(remap = false, method = {"updateDragonAttack"},
        at = {@At("HEAD")},
        cancellable = true)
    private void dragonmaid$attacks(CallbackInfo ci) {
        if (DragonForms.isHumanoid(this.dragon)) {
            ci.cancel();
        }
    }
}
