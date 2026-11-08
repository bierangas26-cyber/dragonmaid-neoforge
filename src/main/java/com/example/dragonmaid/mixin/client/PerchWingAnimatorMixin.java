package com.example.dragonmaid.mixin.client;

import com.example.dragonmaid.client.PerchWingMotion;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.render.model.animator.DragonTabulaModelAnimator;
import com.iafenvoy.uranus.client.model.TabulaModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.2: 龙被玩家「栖息携带」时，在龙模型动画之后叠加拍翅动画。
 *
 * <p>Citadel -> IAF CE（uranus）：{@code com.github.alexthe666.citadel.client.model.TabulaModel}
 * → {@code com.iafenvoy.uranus.client.model.TabulaModel}；目标动画器改为
 * {@code com.iafenvoy.iceandfire.render.model.animator.DragonTabulaModelAnimator}。
 *
 * <p>javap 实测：
 * <ul>
 *   <li>目标方法描述符 {@code (Lcom/iafenvoy/uranus/client/model/TabulaModel;Lcom/iafenvoy/iceandfire/entity/DragonBaseEntity;FFFFFF)V}；
 *       同类的 {@code (TabulaModel;Entity;FFFFFF)} 是桥方法，内部 checkcast 后调本方法 —— 注入本方法即可。</li>
 *   <li>{@code protected TabulaModel<T>[] flyPoses} 字段仍在（非 final）。</li>
 *   <li>子类（Fire/Ice/Lightning 龙动画器）都没有覆写 setRotationAngles。</li>
 * </ul>
 */
@Mixin(remap = false, value = {DragonTabulaModelAnimator.class})
public abstract class PerchWingAnimatorMixin {
    @Shadow(remap = false)
    protected TabulaModel<?>[] flyPoses;

    @Inject(
        remap = false,
        method = {"setRotationAngles(Lcom/iafenvoy/uranus/client/model/TabulaModel;Lcom/iafenvoy/iceandfire/entity/DragonBaseEntity;FFFFFF)V"},
        at = {@At("TAIL")}
    )
    private void dragonmaid$flapWhileCarrying(
        TabulaModel<?> model,
        DragonBaseEntity dragon,
        float swing,
        float amount,
        float age,
        float yaw,
        float pitch,
        float scale,
        CallbackInfo ci
    ) {
        if (dragon.isPassenger()) {
            PerchWingMotion.apply(model, this.flyPoses, dragon, age);
        }
    }
}
