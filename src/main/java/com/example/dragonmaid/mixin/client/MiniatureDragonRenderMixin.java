package com.example.dragonmaid.mixin.client;

import com.example.dragonmaid.accessory.MiniatureForm;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.render.entity.DragonBaseEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 1.2: 迷你形态下，渲染尺寸与阴影半径都要按 {@link MiniatureForm#visualSize(DragonBaseEntity)} 收放。
 *
 * <p>1.20.1 目标：{@code com.github.alexthe666.iceandfire.client.render.entity.RenderDragonBase#scale}
 * → 1.21.1 / IAF CE 2.2：{@code com.iafenvoy.iceandfire.render.entity.DragonBaseEntityRenderer#scale}。
 * 方法描述符 javap 实测：{@code (Lcom/iafenvoy/iceandfire/entity/DragonBaseEntity;Lcom/mojang/blaze3d/vertex/PoseStack;F)V}；
 * 该方法内部只有一处 {@code invokevirtual DragonBaseEntity.getRenderSize()F}（javap -c 实测），
 * 因此 Redirect 精确命中，不影响其它调用。
 */
@Mixin(remap = false, value = {DragonBaseEntityRenderer.class})
public abstract class MiniatureDragonRenderMixin {
    @Redirect(
        remap = false,
        method = {"scale(Lcom/iafenvoy/iceandfire/entity/DragonBaseEntity;Lcom/mojang/blaze3d/vertex/PoseStack;F)V"},
        at = @At(
            value = "INVOKE",
            target = "Lcom/iafenvoy/iceandfire/entity/DragonBaseEntity;getRenderSize()F"
        )
    )
    private float dragonmaid$modelAndShadowScale(DragonBaseEntity dragon) {
        return MiniatureForm.visualSize(dragon);
    }
}
