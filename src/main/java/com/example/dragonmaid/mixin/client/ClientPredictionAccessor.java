package com.example.dragonmaid.mixin.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 1.2.7: 取 {@code ClientLevel} 的预测处理器（太刀/长剑的客户端预测回滚要用）。
 * 1.21.1 里该方法是包可见的 {@code getBlockStatePredictionHandler()}，用 {@link Invoker} 暴露。
 */
@Mixin({ClientLevel.class})
public interface ClientPredictionAccessor {
    @Invoker("getBlockStatePredictionHandler")
    BlockStatePredictionHandler dragonmaid$predictionHandler();
}
