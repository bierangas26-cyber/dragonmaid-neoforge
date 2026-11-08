package com.example.dragonmaid.client;

/**
 * 1.2: 配置界面本地化桥接口。
 *
 * <p>上游 1.2 通过第三方模组 Forge Config Screens（{@code fuzs.forgeconfigscreens}）渲染
 * Forge 的配置界面，并用这层接口给配置条目套上本模组的 i18n key。NeoForge 1.21.1 由
 * 内置的 {@code net.neoforged.neoforge.client.gui.ConfigurationScreen} 提供配置界面，
 * 且本工程当前没有引入该第三方模组 —— 因此这个接口只被 {@code mixin/client/ForgeConfig*}
 * 的 {@code @Pseudo} mixin 使用（目标缺失时 mixin 静默不生效）。保留接口本身是为了让
 * mixin 源码与上游保持同构，将来若接入该模组可以直接启用。
 */
public interface ConfigEntryLanguage {
    /** 配置条目在 TOML 里的路径（用于 {@code config.dragonmaid.category.<path>}）。 */
    String dragonmaid$entryPath();

    /** 设置该条目的 i18n translation key。 */
    void dragonmaid$languageKey(String key);
}
