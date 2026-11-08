package com.example.dragonmaid.mixin.client;

import com.example.dragonmaid.combat.HumanoidMeleeMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.2: 人形近战模式枚举在配置界面里只列出可选值（按模组兼容性过滤）。
 */
@Pseudo
@Mixin(remap = false, targets = {"fuzs.forgeconfigscreens.client.gui.screens.EditEnumScreen"})
public abstract class ForgeConfigMeleeChoicesMixin {
    @Shadow(remap = false)
    @Final
    @Mutable
    private Enum<?>[] allValues;

    @Inject(remap = false, method = {"<init>"}, at = {@At("RETURN")})
    private void dragonmaid$availableModes(CallbackInfo ci) {
        if (this.allValues instanceof HumanoidMeleeMode[]) {
            this.allValues = HumanoidMeleeMode.selectableValues(false);
        }
    }
}
