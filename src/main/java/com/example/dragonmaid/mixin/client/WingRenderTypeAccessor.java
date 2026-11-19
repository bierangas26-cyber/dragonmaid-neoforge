package com.example.dragonmaid.mixin.client;

import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 1.2.7: 读 {@code RenderType#sortOnUpload}，用来给连续翼膜挑一个"不排序"的 RenderType 变体。
 * 1.21.1 该字段仍是 {@code public final boolean sortOnUpload}。
 */
@Mixin({RenderType.class})
public interface WingRenderTypeAccessor {
    @Accessor("sortOnUpload")
    boolean dragonmaid$wingSortOnUpload();
}
