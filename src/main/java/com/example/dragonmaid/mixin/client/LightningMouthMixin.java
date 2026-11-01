package com.example.dragonmaid.mixin.client;

import com.example.dragonmaid.client.ClientMouthEffects;
import com.iafenvoy.iceandfire.entity.LightningDragonEntity;
import com.iafenvoy.iceandfire.render.entity.LightningDragonEntityRenderer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * IAF CE 2.0：RenderLightningDragon → LightningDragonEntityRenderer（renamed/moved）。
 * CE 的 LightningDragonEntityRenderer 直接 override render(LightningDragonEntity, ...)，
 * 内部调用 getHeadPosition()（继承自 DragonBaseEntity）。
 */
@Mixin(remap = false, value = {LightningDragonEntityRenderer.class})
public abstract class LightningMouthMixin {
    public LightningMouthMixin() {
    }

    @Redirect(remap = false, method = {"render(Lcom/iafenvoy/iceandfire/entity/LightningDragonEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
        at = @At(
            value = "INVOKE",
            target = "Lcom/iafenvoy/iceandfire/entity/LightningDragonEntity;getHeadPosition()Lnet/minecraft/world/phys/Vec3;"
        ))
    private Vec3 dragonmaid$mouthOrigin(LightningDragonEntity dragon) {
        return ClientMouthEffects.lightningStart(dragon);
    }
}
