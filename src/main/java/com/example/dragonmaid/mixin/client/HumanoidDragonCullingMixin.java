package com.example.dragonmaid.mixin.client;

import com.example.dragonmaid.client.ClientMouthEffects;
import com.example.dragonmaid.client.HumanoidWallOcclusion;
import com.example.dragonmaid.data.DragonForms;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.LightningDragonEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.2.7: 人形龙的渲染剔除。
 *
 * <p>人形形态的包围箱比原版龙小很多，IAF 给龙体算的 AABB 会让"坐下/睡觉 + 贴墙"的人形龙被误剔除；
 * 这里在 {@code EntityRenderDispatcher.shouldRender} 返回后再判一次：放大包围箱、放电中的雷电龙保底、
 * 以及贴在墙里且完全静止时才算真正不可见。</p>
 */
@Mixin({EntityRenderDispatcher.class})
public abstract class HumanoidDragonCullingMixin {
    @Inject(method = {"shouldRender"}, at = {@At("RETURN")}, cancellable = true)
    private void dragonmaid$cullHumanoid(Entity entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> callback) {
        if (!callback.getReturnValue()) {
            return;
        }

        if (!(entity instanceof DragonBaseEntity dragon)
            || !DragonForms.isHumanoid(dragon)
            || dragon.isVehicle()
            || dragon.isPassenger()
            || dragon.isLeashed()
            || ClientMouthEffects.isGuiPreview()) {
            return;
        }

        double padding = (double) DragonForms.modelScale(dragon) * 2.5;
        AABB bounds = dragon.getBoundingBox().inflate(padding);
        if (!frustum.isVisible(bounds)) {
            if (dragon instanceof LightningDragonEntity lightning && lightning.hasLightningTarget()) {
                Vec3 end = new Vec3(lightning.getLightningTargetX(), lightning.getLightningTargetY(), lightning.getLightningTargetZ());
                if (frustum.isVisible(new AABB(dragon.getEyePosition(), end).inflate(2.0))) {
                    return;
                }
            }

            callback.setReturnValue(false);
            return;
        }

        if ((DragonForms.isSeated(dragon) || dragon.isSleeping())
            && dragon.getDeltaMovement().horizontalDistanceSqr() < 0.0025
            && DragonForms.state(dragon).dragonmaid$getAction() == 0
            && !dragon.isCustomNameVisible()
            && !Minecraft.getInstance().shouldEntityAppearGlowing(dragon)
            && (!(dragon instanceof LightningDragonEntity lightning) || !lightning.hasLightningTarget())
            && HumanoidWallOcclusion.hidden(bounds, x, y, z)) {
            callback.setReturnValue(false);
        }
    }
}
