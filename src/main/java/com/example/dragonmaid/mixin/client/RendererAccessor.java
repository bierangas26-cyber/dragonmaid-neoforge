package com.example.dragonmaid.mixin.client;

import net.minecraft.client.renderer.entity.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(remap = false, value = {EntityRenderer.class})
public interface RendererAccessor {
    @Accessor(remap = false, value = "shadowRadius")
    void dragonmaid$setShadowRadius(float var1);
}
