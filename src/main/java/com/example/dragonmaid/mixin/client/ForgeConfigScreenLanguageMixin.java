package com.example.dragonmaid.mixin.client;

import com.example.dragonmaid.client.ConfigEntryLanguage;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/*
 * 上游 1.2 用第三方模组 Forge Config Screens（mod id: fuzs.forgeconfigscreens）渲染配置界面，
 * 这 7 个 mixin 都是 @Pseudo（目标缺失时静默不生效）。
 *
 * 2026-11 移植现状（重要）：
 *  - 本工程 libs/ 与目标整合包都没有该模组的 1.21.1 NeoForge 版本，
 *    因此 target 类/方法的真实签名无法用 javap 校验（上游是 1.20.1 Forge 版）。
 *  - NeoForge 1.21.1 自带 net.neoforged.neoforge.client.gui.ConfigurationScreen，
 *    常规配置界面并不依赖该第三方模组。
 *  - 这里按上游源码语义 + NeoForge 类型（ModConfig / ModConfigSpec）移植，保持 @Pseudo，
 *    将来把该模组加进来时只需按实际签名校正 method= 描述符。
 */

@Pseudo
@Mixin(remap = false, targets = {"fuzs.forgeconfigscreens.client.gui.screens.ConfigScreen"})
public abstract class ForgeConfigScreenLanguageMixin {
    @Inject(remap = false, method = {"create"}, at = {@At("HEAD")})
    private static void dragonmaid$localize(
        Screen parent,
        Component title,
        ModConfig config,
        Map<Object, Object> entries,
        CallbackInfoReturnable<Object> cir
    ) {
        if ("dragonmaid".equals(config.getModId()) && config.getSpec() instanceof ModConfigSpec spec) {
            for (Entry<Object, Object> entry : entries.entrySet()) {
                if (entry.getValue() instanceof ConfigEntryLanguage translated) {
                    if (entry.getKey() instanceof ModConfigSpec.ConfigValue<?> configValue) {
                        ModConfigSpec.ValueSpec metadata = spec.getSpec().getRaw(configValue.getPath());
                        if (metadata != null && metadata.getTranslationKey() != null) {
                            translated.dragonmaid$languageKey(metadata.getTranslationKey());
                        }
                    } else {
                        translated.dragonmaid$languageKey("config.dragonmaid.category." + translated.dragonmaid$entryPath());
                    }
                }
            }
        }
    }
}
