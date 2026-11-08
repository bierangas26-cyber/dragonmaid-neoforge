package com.example.dragonmaid.mixin.client;

import com.example.dragonmaid.combat.HumanoidMeleeMode;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.2: 近战模式在配置界面上的“当前值”字符串也走翻译。
 */
@Pseudo
@Mixin(remap = false, targets = {"fuzs.forgeconfigscreens.client.gui.screens.ConfigScreen$EnumEntry"})
public abstract class ForgeConfigMeleeValueMixin {
    @Inject(
        remap = false,
        method = {"valueToString(Ljava/lang/Enum;)Ljava/lang/String;"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void dragonmaid$translatedValue(Enum<?> value, CallbackInfoReturnable<String> cir) {
        if (value instanceof HumanoidMeleeMode mode) {
            cir.setReturnValue(Component.translatable(mode.translationKey()).getString());
        }
    }
}
