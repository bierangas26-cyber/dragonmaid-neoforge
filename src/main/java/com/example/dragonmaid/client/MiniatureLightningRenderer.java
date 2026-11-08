package com.example.dragonmaid.client;

import com.example.dragonmaid.combat.MiniatureLightningCharge;
import com.example.dragonmaid.entity.ModEntities;
import com.iafenvoy.iceandfire.entity.LightningDragonChargeEntity;
import com.iafenvoy.iceandfire.render.entity.LightningDragonChargeEntityRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;

/**
 * 1.2: 迷你形态的闪电吐息实体渲染器 —— 按蓄力与否缩放原版闪电球。
 *
 * <p>IAF CE 2.2 对应类：{@code com.iafenvoy.iceandfire.render.entity.LightningDragonChargeEntityRenderer}
 * / {@code LightningDragonChargeEntity}。NeoForge 的渲染器注册事件是
 * {@code EntityRenderersEvent.RegisterRenderers#registerEntityRenderer}.
 */
@EventBusSubscriber(modid = "dragonmaid", bus = Bus.MOD, value = {Dist.CLIENT})
public final class MiniatureLightningRenderer extends LightningDragonChargeEntityRenderer {
    public MiniatureLightningRenderer(Context context) {
        super(context);
    }

    @SubscribeEvent
    public static void register(RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.MINI_LIGHTNING.get(), MiniatureLightningRenderer::new);
    }

    @Override
    public void render(
        LightningDragonChargeEntity shot,
        float yaw,
        float partial,
        PoseStack pose,
        MultiBufferSource buffers,
        int light
    ) {
        float scale = shot instanceof MiniatureLightningCharge mini && mini.enhanced() ? 0.54F : 0.32F;
        pose.pushPose();
        pose.scale(scale, scale, scale);
        super.render(shot, yaw, partial, pose, buffers, light);
        pose.popPose();
    }
}
