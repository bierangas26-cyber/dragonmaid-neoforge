package com.example.dragonmaid.mixin.client;

import com.example.dragonmaid.config.GlobalBalanceConfig;
import java.util.LinkedHashMap;
import java.util.Map;
import net.neoforged.fml.config.ModConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.2: 配置选择界面里隐藏被镜像到 COMMON 的 SERVER 配置文件（它们由全局文件代替）。
 */
@Pseudo
@Mixin(remap = false, targets = {"fuzs.forgeconfigscreens.client.gui.screens.SelectConfigScreen"})
public abstract class ForgeConfigSelectionMixin {
    @Shadow(remap = false)
    @Final
    @Mutable
    private Map<ModConfig, ?> configs;

    @Inject(remap = false, method = {"<init>"}, at = {@At("RETURN")})
    private void dragonmaid$globalFiles(CallbackInfo ci) {
        if (this.configs.keySet().stream().anyMatch(GlobalBalanceConfig::transport)) {
            LinkedHashMap<ModConfig, ?> visible = new LinkedHashMap<>(this.configs);
            visible.keySet().removeIf(GlobalBalanceConfig::transport);
            this.configs = visible;
        }
    }
}
