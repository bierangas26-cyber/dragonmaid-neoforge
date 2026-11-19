package com.example.dragonmaid.mixin.client;

import com.example.dragonmaid.client.StarterCompanionScreen;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 1.2.7: 预览 Level 里的假实体不要往外发 {@code RenderLivingEvent}，
 * 否则其它模组（Jade/光影/状态条之类）会拿到一个不在真实世界里的实体。
 *
 * <p>1.21.1 的 {@code IEventBus.post(T)} 返回事件本身（{@code <T extends Event> T post(T)}），
 * 上游 1.20.1 的 OptiFine 兜底分支在 NeoForge 下没有对应物，已去掉。</p>
 */
@Mixin({LivingEntityRenderer.class})
public abstract class StarterPreviewLivingRenderMixin {
    @Redirect(
        method = {"render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/neoforged/bus/api/IEventBus;post(Lnet/neoforged/bus/api/Event;)Lnet/neoforged/bus/api/Event;"
        )
    )
    private Event dragonmaid$skipPreviewLivingEvents(IEventBus bus, Event event) {
        if (event instanceof RenderLivingEvent<?, ?> renderEvent && StarterCompanionScreen.isPreview(renderEvent.getEntity())) {
            return event;
        }

        return bus.post(event);
    }
}
