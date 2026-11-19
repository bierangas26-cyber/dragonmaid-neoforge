package com.example.dragonmaid.mixin.client;

import com.example.dragonmaid.config.GlobalBalanceConfig;
import com.example.dragonmaid.config.HumanoidRidingConfig;
import net.minecraft.network.chat.Component;
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
 * 1.2: 给「全局平衡」两个 COMMON 配置文件起可翻译的名字/作用域（世界级配置）。
 */
@Pseudo
@Mixin(remap = false, targets = {"fuzs.forgeconfigscreens.client.gui.components.ConfigSelectionList$ConfigListEntry"})
public abstract class ForgeConfigFileLabelMixin {
    @Shadow(remap = false)
    @Final
    private ModConfig config;
    @Shadow(remap = false)
    @Final
    @Mutable
    private Component nameComponent;
    @Shadow(remap = false)
    @Final
    @Mutable
    private Component typeComponent;

    @Inject(remap = false, method = {"<init>"}, at = {@At("RETURN")})
    private void dragonmaid$translatedFile(CallbackInfo ci) {
        if (this.config.getSpec() == GlobalBalanceConfig.GAMEPLAY || this.config.getSpec() == GlobalBalanceConfig.RECALL) {
            this.nameComponent = Component.translatable(
                this.config.getSpec() == GlobalBalanceConfig.GAMEPLAY
                    ? "config.dragonmaid.file.gameplay"
                    : "config.dragonmaid.file.recall"
            );
            this.typeComponent = Component.translatable("config.dragonmaid.scope.global");
        } else if (GlobalBalanceConfig.worldOverride(this.config)) {
            // 1.2.7: 每个存档覆盖用的 world 级配置单独起名/作用域
            this.nameComponent = Component.translatable(
                this.config.getSpec() == GlobalBalanceConfig.WORLD_GAMEPLAY
                    ? "config.dragonmaid.file.worldGameplay"
                    : "config.dragonmaid.file.worldRecall"
            );
            this.typeComponent = Component.translatable("config.dragonmaid.scope.world");
        } else if (this.config.getSpec() == HumanoidRidingConfig.SPEC) {
            this.nameComponent = Component.translatable("config.dragonmaid.file.client");
            this.typeComponent = Component.translatable("config.dragonmaid.scope.client");
        }
    }
}
