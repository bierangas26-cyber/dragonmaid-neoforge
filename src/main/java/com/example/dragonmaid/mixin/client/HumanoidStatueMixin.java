package com.example.dragonmaid.mixin.client;

import com.example.dragonmaid.client.HumanoidStatueRenderer;
import com.iafenvoy.iceandfire.entity.StoneStatueEntity;
import com.iafenvoy.iceandfire.render.entity.StoneStatueEntityRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * IAF CE 2.0：RenderStoneStatue → StoneStatueEntityRenderer（renamed/moved）。
 * 1.21.1 render 签名不变：(T, float yaw, float partial, PoseStack, MultiBufferSource, int light)。
 */
@Mixin(remap = false, value = {StoneStatueEntityRenderer.class})
public abstract class HumanoidStatueMixin {
    public HumanoidStatueMixin() {
    }

    @Inject(remap = false, method = {"render(Lcom/iafenvoy/iceandfire/entity/StoneStatueEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
        at = {@At("HEAD")},
        cancellable = true)
    private void dragonmaid$humanStoneModel(
        StoneStatueEntity statue, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci
    ) {
        if (HumanoidStatueRenderer.render(statue, yaw, partial, pose, buffers, light)) {
            ci.cancel();
        }
    }
}
