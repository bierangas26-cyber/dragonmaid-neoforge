package com.example.dragonmaid.mixin;

import com.example.dragonmaid.movement.HumanoidFlight;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.2.7: 人形龙在空中飞行时，乘客按潜行不会真的下坐骑（要先落地/被放下）。
 */
@Mixin({Player.class})
abstract class PlayerHumanoidFlightMixin {
    @Inject(method = {"wantsToStopRiding"}, at = {@At("HEAD")}, cancellable = true)
    private void dragonmaid$stayMountedInFlight(CallbackInfoReturnable<Boolean> result) {
        if (HumanoidFlight.dismountInAir((Player) (Object) this)) {
            result.setReturnValue(false);
        }
    }
}
