package com.example.dragonmaid.mixin.client;

import com.example.dragonmaid.client.ConfigEntryLanguage;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
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
@Mixin(remap = false, targets = {"fuzs.forgeconfigscreens.client.gui.data.EntryData"})
public abstract class ForgeConfigEntryLanguageMixin implements ConfigEntryLanguage {
    @Shadow(remap = false)
    @Final
    private String path;
    @Unique
    private String dragonmaid$key;
    @Unique
    private Component dragonmaid$title;

    @Override
    public String dragonmaid$entryPath() {
        return this.path;
    }

    @Override
    public void dragonmaid$languageKey(String key) {
        this.dragonmaid$key = key;
        this.dragonmaid$title = Component.translatable(key);
    }

    @Inject(remap = false, method = {"getTitle"}, at = {@At("HEAD")}, cancellable = true)
    private void dragonmaid$title(CallbackInfoReturnable<Component> cir) {
        if (this.dragonmaid$key != null && I18n.exists(this.dragonmaid$key)) {
            cir.setReturnValue(this.dragonmaid$title);
        }
    }

    @Inject(remap = false, method = {"getComment"}, at = {@At("HEAD")}, cancellable = true)
    private void dragonmaid$description(CallbackInfoReturnable<String> cir) {
        if (this.dragonmaid$key != null && I18n.exists(this.dragonmaid$key + ".tooltip")) {
            cir.setReturnValue(I18n.get(this.dragonmaid$key + ".tooltip", new Object[0]));
        }
    }
}
