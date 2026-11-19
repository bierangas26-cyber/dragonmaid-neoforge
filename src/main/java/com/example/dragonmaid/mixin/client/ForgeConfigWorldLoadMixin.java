package com.example.dragonmaid.mixin.client;

import com.example.dragonmaid.config.GlobalBalanceConfig;
import java.nio.file.Path;
import net.neoforged.fml.config.ModConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.2.7: 当第三方配置界面（Forge Config Screens）为某个 world 级配置文件建条目时，
 * 顺手把全局 spec 的当前值写进这个新文件（{@code useWorldSettings} 的初始镜像）。
 *
 * <p>移植说明：目标模组 {@code fuzs.forgeconfigscreens} 不是本工程的依赖，所以整类有
 * {@code @Pseudo}（目标缺失时静默跳过，不会让启动失败）。真正的初始化逻辑在
 * {@link GlobalBalanceConfig#initializeWorld(ModConfig)}，NeoForge 侧另有
 * {@code ModConfigEvent.Loading/Unloading} 的订阅兜底（见 GlobalBalanceConfig 内部）。</p>
 */
@Pseudo
@Mixin(remap = false, targets = {"fuzs.forgeconfigscreens.client.gui.components.ConfigWorldSelectionList$ConfigWorldListEntry"})
public abstract class ForgeConfigWorldLoadMixin {
    @Inject(remap = false, method = {"loadModConfig"}, at = {@At("RETURN")})
    private static void dragonmaid$seedWorldFile(ModConfig config, Path basePath, CallbackInfo ci) {
        GlobalBalanceConfig.initializeWorld(config);
    }
}
