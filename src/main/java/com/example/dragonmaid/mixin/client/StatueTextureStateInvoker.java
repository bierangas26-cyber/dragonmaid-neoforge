package com.example.dragonmaid.mixin.client;

import java.util.Optional;
import net.minecraft.client.renderer.RenderStateShard.EmptyTextureStateShard;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(remap = false, value = {EmptyTextureStateShard.class})
public interface StatueTextureStateInvoker {
    @Invoker(remap = false, value = "cutoutTexture")
    Optional<ResourceLocation> dragonmaid$statueCutoutTexture();
}
