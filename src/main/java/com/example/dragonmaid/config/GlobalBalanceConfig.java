package com.example.dragonmaid.config;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.example.dragonmaid.DragonMaidConfig;
import com.example.dragonmaid.recall.RecallConfig;
import java.util.ArrayList;
import java.util.List;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.ValueSpec;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

/**
 * 1.2: 把“世界级”配置项镜像到 COMMON 配置文件（dragonmaid-common.toml / dragonmaid-recall-common.toml），
 * 这样整合包作者可以在世界创建前设定默认值；服务器启动时再回写到 SERVER 配置。
 * NeoForge 1.21.1: ModConfigSpec 提供 getSpec()/getValues()/define(List, ValueSpec, Supplier)，可直接镜像。
 */
@EventBusSubscriber(modid = "dragonmaid")
public final class GlobalBalanceConfig {
    private static final List<GlobalBalanceConfig.Binding> BINDINGS = new ArrayList<>();
    public static final ModConfigSpec GAMEPLAY = mirror(DragonMaidConfig.SPEC);
    public static final ModConfigSpec RECALL = mirror(RecallConfig.SPEC);

    private GlobalBalanceConfig() {
    }

    public static void register() {
        ModLoadingContext.get().getActiveContainer().registerConfig(Type.COMMON, GAMEPLAY, "dragonmaid-common.toml");
        if (RECALL != null) {
            ModLoadingContext.get().getActiveContainer().registerConfig(Type.COMMON, RECALL, "dragonmaid-recall-common.toml");
        }
    }

    public static boolean transport(ModConfig config) {
        return "dragonmaid".equals(config.getModId()) && config.getType() == Type.SERVER;
    }

    private static ModConfigSpec mirror(ModConfigSpec session) {
        Builder builder = new Builder();
        copyDefinitions(builder, session, session.getValues(), List.of());
        return builder.build();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void copyDefinitions(Builder builder, ModConfigSpec session, UnmodifiableConfig values, List<String> parent) {
        values.valueMap().forEach((key, value) -> {
            ArrayList<String> path = new ArrayList<>(parent);
            path.add(key);
            if (value instanceof UnmodifiableConfig children) {
                copyDefinitions(builder, session, children, path);
            } else if (value instanceof ConfigValue<?> field) {
                ValueSpec definition = (ValueSpec)session.getSpec().get(path);
                ConfigValue<?> global;
                if (definition.getDefault() instanceof Enum choice) {
                    builder.comment(definition.getComment()).translation(definition.getTranslationKey()).worldRestart();
                    global = builder.defineEnum(path, choice);
                } else {
                    global = builder.define(path, definition, definition::getDefault);
                }

                BINDINGS.add(new GlobalBalanceConfig.Binding(global, field));
            }
        });
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void starting(ServerAboutToStartEvent event) {
        applyForNextSession();
    }

    public static void applyForNextSession() {
        for (GlobalBalanceConfig.Binding binding : BINDINGS) {
            copy(binding);
        }

        CompanionBalanceConfig.capture();
    }

    /** 泛型捕获：ConfigValue<?> 之间不能直接 set(get())，用原始类型 helper 复制。 */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void copy(GlobalBalanceConfig.Binding binding) {
        ((ConfigValue)binding.session).set(((ConfigValue)binding.global).get());
    }

    private static record Binding(ConfigValue<?> global, ConfigValue<?> session) {
    }
}
