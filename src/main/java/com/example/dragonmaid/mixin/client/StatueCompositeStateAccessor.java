package com.example.dragonmaid.mixin.client;

import net.minecraft.client.renderer.RenderStateShard.EmptyTextureStateShard;
import net.minecraft.client.renderer.RenderType.CompositeState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(remap = false, value = {CompositeState.class})
public interface StatueCompositeStateAccessor {
    @Accessor(remap = false, value = "textureState")
    EmptyTextureStateShard dragonmaid$statueTextureState();
}
