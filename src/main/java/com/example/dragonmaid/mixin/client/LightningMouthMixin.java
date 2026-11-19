package com.example.dragonmaid.mixin.client;

import com.example.dragonmaid.client.ClientMouthEffects;
import com.example.dragonmaid.data.DragonForms;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.LightningDragonEntity;
import com.iafenvoy.iceandfire.render.entity.LightningDragonEntityRenderer;
import com.iafenvoy.iceandfire.render.misc.LightningBoltData;
import com.iafenvoy.iceandfire.render.misc.LightningBoltData.BoltRenderInfo;
import com.iafenvoy.iceandfire.render.misc.LightningBoltData.SpawnFunction;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 1.2.7: 雷电龙嘴部/世界闪电的绘制接管 + 远距离 LOD。
 *
 * <p>IAF CE 2.2 迁移点（javap 核对）：</p>
 * <ul>
 *   <li>渲染器 {@code RenderLightningDragon} → {@code LightningDragonEntityRenderer}，
 *       且 render 直接重载在 {@link LightningDragonEntity} 上（不是 DragonBaseEntity）。</li>
 *   <li>{@code LightningBoltData} 搬到 {@code com.iafenvoy.iceandfire.render.misc}；
 *       构造签名 {@code (BoltRenderInfo, Vec3, Vec3, int)}、{@code spawn(SpawnFunction)}、
 *       {@code BoltRenderInfo(float,float,float,float,Vector4f,float)}、{@code SpawnFunction.delay(float)}
 *       与 1.2.7 上游完全一致。</li>
 * </ul>
 *
 * <p>新增的 1.2.7 行为：人形龙离相机很远（&gt;24 格）时改用低段数、慢扩散的电弧，省顶点也更好看。</p>
 */
@Mixin(remap = false, value = {LightningDragonEntityRenderer.class})
public abstract class LightningMouthMixin {
    @Unique
    private static final BoltRenderInfo dragonmaid$DISTANT_BOLT = new BoltRenderInfo(0.5F, 0.25F, 0.06F, 0.1F, new Vector4f(0.7F, 0.45F, 0.89F, 0.8F), 0.8F);
    @Unique
    private static final SpawnFunction dragonmaid$DISTANT_SPAWN = SpawnFunction.delay(1.0F);

    /** 预览界面（开局伙伴/龙面板）里不要画世界闪电。 */
    @Redirect(remap = false, method = {"render(Lcom/iafenvoy/iceandfire/entity/LightningDragonEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
        at = @At(
            value = "INVOKE",
            target = "Lcom/iafenvoy/iceandfire/entity/LightningDragonEntity;hasLightningTarget()Z"
        ))
    private boolean dragonmaid$showWorldLightning(LightningDragonEntity dragon) {
        return dragon.hasLightningTarget() && !ClientMouthEffects.isGuiPreview();
    }

    /** 嘴部原点改走本 mod 的嘴部特效锚点。 */
    @Redirect(remap = false, method = {"render(Lcom/iafenvoy/iceandfire/entity/LightningDragonEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
        at = @At(
            value = "INVOKE",
            target = "Lcom/iafenvoy/iceandfire/entity/LightningDragonEntity;getHeadPosition()Lnet/minecraft/world/phys/Vec3;"
        ))
    private Vec3 dragonmaid$mouthOrigin(LightningDragonEntity dragon) {
        return ClientMouthEffects.lightningStart(dragon);
    }

    /** 远距离时降低段数。 */
    @Redirect(remap = false, method = {"render(Lcom/iafenvoy/iceandfire/entity/LightningDragonEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
        at = @At(
            value = "NEW",
            target = "(Lcom/iafenvoy/iceandfire/render/misc/LightningBoltData$BoltRenderInfo;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;I)Lcom/iafenvoy/iceandfire/render/misc/LightningBoltData;"
        ))
    private LightningBoltData dragonmaid$boltDetail(
        BoltRenderInfo style,
        Vec3 from,
        Vec3 to,
        int segments,
        LightningDragonEntity dragon,
        float yaw,
        float partial,
        PoseStack pose,
        MultiBufferSource buffers,
        int light
    ) {
        return dragonmaid$distantBolt(dragon, from, to)
            ? new LightningBoltData(dragonmaid$DISTANT_BOLT, from, to, Math.min(segments, 9))
            : new LightningBoltData(style, from, to, segments);
    }

    /** 远距离时用慢扩散的 spawn 函数。 */
    @Redirect(remap = false, method = {"render(Lcom/iafenvoy/iceandfire/entity/LightningDragonEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
        at = @At(
            value = "INVOKE",
            target = "Lcom/iafenvoy/iceandfire/render/misc/LightningBoltData;spawn(Lcom/iafenvoy/iceandfire/render/misc/LightningBoltData$SpawnFunction;)Lcom/iafenvoy/iceandfire/render/misc/LightningBoltData;"
        ))
    private LightningBoltData dragonmaid$boltDensity(
        LightningBoltData bolt,
        SpawnFunction spawn,
        LightningDragonEntity dragon,
        float yaw,
        float partial,
        PoseStack pose,
        MultiBufferSource buffers,
        int light
    ) {
        Vec3 end = new Vec3(dragon.getLightningTargetX(), dragon.getLightningTargetY(), dragon.getLightningTargetZ());
        return bolt.spawn(dragonmaid$distantBolt(dragon, dragon.getEyePosition(), end) ? dragonmaid$DISTANT_SPAWN : spawn);
    }

    @Unique
    private static boolean dragonmaid$distantBolt(DragonBaseEntity dragon, Vec3 from, Vec3 to) {
        if (!DragonForms.isHumanoid(dragon)) {
            return false;
        }

        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vec3 segment = to.subtract(from);
        double length = segment.lengthSqr();
        double along = length < 1.0E-6 ? 0.0 : Mth.clamp(camera.subtract(from).dot(segment) / length, 0.0, 1.0);
        return camera.distanceToSqr(from.add(segment.scale(along))) > 576.0;
    }
}
