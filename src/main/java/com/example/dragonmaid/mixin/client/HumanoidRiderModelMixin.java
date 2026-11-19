package com.example.dragonmaid.mixin.client;

import com.example.dragonmaid.client.HumanoidRidingPose;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.2.7: 在 {@code LivingEntityRenderer.render} 里给"被龙抱着/抱龙"的玩家套上自定义骨骼姿势，
 * 渲染结束时（popPose 处）恢复，避免污染同一模型的其它渲染。
 *
 * <p>1.21.1 的 {@code LivingEntityRenderer#model} 是 {@code protected M model}（擦除为 EntityModel），
 * 这里按擦除类型 shadow。</p>
 */
@Mixin({LivingEntityRenderer.class})
abstract class HumanoidRiderModelMixin {
    @Shadow
    protected EntityModel<?> model;

    @Unique
    private HumanoidRidingPose.PlayerParts dragonmaid$riderParts;

    @Inject(
        method = {"render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
        at = {@At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V",
            shift = Shift.AFTER
        )}
    )
    private void dragonmaid$applyRider(LivingEntity entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (entity instanceof AbstractClientPlayer player && this.model instanceof PlayerModel<?> playerModel) {
            this.dragonmaid$riderParts = HumanoidRidingPose.animatePlayer(playerModel, player, partial);
        }
    }

    @Inject(
        method = {"render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
        at = {@At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V")}
    )
    private void dragonmaid$restoreRider(LivingEntity entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (this.dragonmaid$riderParts != null) {
            this.dragonmaid$riderParts.restore();
            this.dragonmaid$riderParts = null;
        }
    }
}
