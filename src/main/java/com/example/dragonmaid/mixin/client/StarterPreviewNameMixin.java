package com.example.dragonmaid.mixin.client;

import com.example.dragonmaid.client.StarterCompanionScreen;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.2.7: 开局伙伴界面的 3D 预览里不画名字牌（假 Level 里的实体不该有 UI 名条）。
 */
@Mixin({EntityRenderer.class})
public abstract class StarterPreviewNameMixin {
    @Inject(
        method = {"render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void dragonmaid$skipPreviewNameTag(Entity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int packedLight, CallbackInfo ci) {
        if (StarterCompanionScreen.isPreview(entity)) {
            ci.cancel();
        }
    }
}
