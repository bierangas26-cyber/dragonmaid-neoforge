package com.example.dragonmaid.mixin.client;

import com.example.dragonmaid.client.ServiceDragonArmorTextures;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.render.entity.feature.DragonArmorFeatureRenderer;
import com.iafenvoy.uranus.client.model.TabulaModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 让 CE 的龙甲 layer 改用本 mod 的"服务龙甲"复合纹理（4 槽位逐层合成）。
 *
 * 1.21.1 / IAF CE 2.0 迁移点：
 * - target: LayerDragonArmor → DragonArmorFeatureRenderer，包名 client.render.entity.layer → render.entity.feature
 * - 泛型模型: citadel AdvancedEntityModel → uranus TabulaModel
 * - renderToBuffer 新签名: (PoseStack, VertexConsumer, int light, int overlay, int color)（1.20.1 是 4 个 float）
 * - ServiceDragonArmorTextures.texture() 现在可能返回 null（全身空槽）→ CE 语义：没东西画就跳过
 */
@Mixin(remap = false, value = {DragonArmorFeatureRenderer.class})
public abstract class ServiceDragonArmorMixin extends RenderLayer<DragonBaseEntity, TabulaModel<DragonBaseEntity>> {
    protected ServiceDragonArmorMixin(RenderLayerParent<DragonBaseEntity, TabulaModel<DragonBaseEntity>> parent) {
        super(parent);
    }

    @Inject(remap = false, method = {"render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILcom/iafenvoy/iceandfire/entity/DragonBaseEntity;FFFFFF)V"},
        at = {@At("HEAD")},
        cancellable = true)
    private void dragonmaid$serviceArmor(
        PoseStack pose,
        MultiBufferSource buffers,
        int light,
        DragonBaseEntity dragon,
        float limbSwing,
        float limbSwingAmount,
        float partialTick,
        float ageInTicks,
        float netHeadYaw,
        float headPitch,
        CallbackInfo callback
    ) {
        if (ServiceDragonArmorTextures.hasServiceArmor(dragon)) {
            ResourceLocation resourcelocation = ServiceDragonArmorTextures.texture(dragon);
            if (resourcelocation == null) {
                return;
            }

            this.getParentModel()
                .renderToBuffer(
                    pose, buffers.getBuffer(RenderType.entityCutoutNoCull(resourcelocation)), light, OverlayTexture.NO_OVERLAY, -1
                );
            callback.cancel();
        }
    }
}
