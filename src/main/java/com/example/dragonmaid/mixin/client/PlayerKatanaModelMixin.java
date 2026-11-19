package com.example.dragonmaid.mixin.client;

import com.example.dragonmaid.client.PlayerKatanaPose;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.2.7: 玩家太刀/长剑的手臂与鞘姿态（第一人称刀动画的第三人称部分）。
 */
@Mixin({PlayerModel.class})
abstract class PlayerKatanaModelMixin {
    @Inject(
        method = {"setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V"},
        at = {@At("TAIL")}
    )
    private void dragonmaid$animateKatana(LivingEntity entity, float walk, float stride, float age, float yaw, float pitch, CallbackInfo ci) {
        if (entity instanceof AbstractClientPlayer player) {
            PlayerKatanaPose.apply((PlayerModel<?>) (Object) this, player, age, pitch);
        }
    }
}
