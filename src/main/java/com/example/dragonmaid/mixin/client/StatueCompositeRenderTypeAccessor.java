package com.example.dragonmaid.mixin.client;

import net.minecraft.client.renderer.RenderType.CompositeState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(remap = false, targets = {"net.minecraft.client.renderer.RenderType$CompositeRenderType"})
public interface StatueCompositeRenderTypeAccessor {
    @Accessor(remap = false, value = "state")
    CompositeState dragonmaid$statueCompositeState();
}
