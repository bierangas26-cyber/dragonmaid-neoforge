package com.example.dragonmaid.mixin.client;

import com.example.dragonmaid.combat.HumanoidMeleeMode;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.2: 近战模式枚举项在配置界面里显示翻译名。
 */
@Pseudo
@Mixin(remap = false, targets = {"fuzs.forgeconfigscreens.client.gui.screens.EditEnumScreen$EnumList$Entry"})
public abstract class ForgeConfigMeleeLabelMixin {
    @Shadow(remap = false)
    @Final
    private Enum<?> value;
    @Shadow(remap = false)
    @Final
    @Mutable
    private Component name;

    @Inject(remap = false, method = {"<init>"}, at = {@At("RETURN")})
    private void dragonmaid$translatedLabel(CallbackInfo ci) {
        if (this.value instanceof HumanoidMeleeMode mode) {
            this.name = Component.translatable(mode.translationKey());
        }
    }
}
