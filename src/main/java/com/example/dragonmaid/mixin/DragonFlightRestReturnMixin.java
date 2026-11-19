package com.example.dragonmaid.mixin;

import com.example.dragonmaid.combat.RestReturnGoal;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.util.dragon.IafDragonFlightManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.2.7: 龙在"打完架飞回去休息"途中，禁止 IAF 的飞行管理器改写飞行目标。
 *
 * <p>上游目标是 Citadel/IAF 1.20.1 的 {@code EntityDragonBase.flightManager}；
 * IAF CE 2.2 把它搬到了 {@code com.iafenvoy.iceandfire.entity.util.dragon.IafDragonFlightManager}，
 * 字段名仍是 {@code dragon}，方法仍是 {@code update()}。</p>
 */
@Mixin(remap = false, value = {IafDragonFlightManager.class})
public abstract class DragonFlightRestReturnMixin {
    @Shadow(remap = false)
    @Final
    private DragonBaseEntity dragon;

    @Inject(remap = false, method = {"update"}, at = {@At("HEAD")}, cancellable = true)
    private void dragonmaid$keepRestFlightTarget(CallbackInfo ci) {
        if (RestReturnGoal.returningInFlight(this.dragon)) {
            ci.cancel();
        }
    }
}
