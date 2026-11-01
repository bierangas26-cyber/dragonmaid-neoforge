package com.example.dragonmaid.client;

import com.example.dragonmaid.entity.ModEntities;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;

@EventBusSubscriber(
    modid = "dragonmaid",
    bus = Bus.MOD,
    value = {Dist.CLIENT}
)
public final class ReinforcedHornRendering {
    private ReinforcedHornRendering() {
    }

    @SubscribeEvent
    public static void register(RegisterRenderers event) {
        event.registerEntityRenderer((EntityType)ModEntities.REINFORCED_HORN.get(), ItemEntityRenderer::new);
    }
}
