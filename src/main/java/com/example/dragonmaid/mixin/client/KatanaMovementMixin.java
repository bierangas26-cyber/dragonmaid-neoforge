package com.example.dragonmaid.mixin.client;

import com.example.dragonmaid.client.BladeMovementInput;
import com.example.dragonmaid.item.BladeEnchantments;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 1.2.7: 太刀蓄力 / 风步挥砍时降低移动速度（windstep 附魔决定保留多少）。
 * 1.21.1 的 {@code Entity.moveRelative(float, Vec3)} 签名未变。
 */
@Mixin({Entity.class})
abstract class KatanaMovementMixin {
    @ModifyVariable(method = {"moveRelative"}, at = @At("HEAD"), argsOnly = true)
    private float dragonmaid$scaleKatanaMovement(float speed) {
        if ((Object) this instanceof LocalPlayer player) {
            if (BladeMovementInput.charging(player)) {
                return speed * BladeEnchantments.chargeMovement(player.getUseItem());
            }

            if (BladeMovementInput.windstepSwing(player)) {
                return speed * BladeEnchantments.chargeMovement(player.getMainHandItem());
            }
        }

        return speed;
    }
}
