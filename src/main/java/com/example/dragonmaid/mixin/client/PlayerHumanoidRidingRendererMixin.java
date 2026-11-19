package com.example.dragonmaid.mixin.client;

import com.example.dragonmaid.client.HumanoidRidingPose;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.2.7: 骑乘/被抱时接管玩家渲染的基准旋转。
 *
 * <p>1.21.1 把 {@code PlayerRenderer} 挪到了 {@code net.minecraft.client.renderer.entity.player}，
 * 且 {@code setupRotations} 多了一个 {@code float scale} 参数（6 参）。</p>
 */
@Mixin({PlayerRenderer.class})
abstract class PlayerHumanoidRidingRendererMixin {
    @Inject(
        method = {"setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void dragonmaid$ridingBasis(AbstractClientPlayer player, PoseStack pose, float bob, float yaw, float partial, float scale, CallbackInfo ci) {
        if (HumanoidRidingPose.placePlayer(player, pose, partial)) {
            ci.cancel();
        }
    }
}
