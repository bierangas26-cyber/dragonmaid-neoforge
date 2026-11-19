package com.example.dragonmaid.config;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.example.dragonmaid.DragonMaidConfig;
import com.example.dragonmaid.recall.RecallConfig;
import java.util.ArrayList;
import java.util.List;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.event.config.ModConfigEvent.Loading;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.ValueSpec;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

/**
 * 1.2.7 双层配置架构（上游 GlobalBalanceConfig 168 行）：把 SERVER 配置镜像是两套 spec——
 * <ul>
 *   <li>{@code global}：dragonmaid-common.toml / dragonmaid-recall-common.toml（整合包作者在世界创建前设默认值）</li>
 *   <li>{@code world}：dragonmaid-world-server.toml / dragonmaid-recall-world-server.toml（本地存档覆盖全局，
 *       由 {@code useWorldSettings} 开关决定是否生效；starterCompanion 段被显式排除，不参与存档覆盖）</li>
 * </ul>
 * 服务器启动时按"本地存档是否覆盖"把 global 或 world 的值写回 session（DragonMaidConfig / RecallConfig）。
 *
 * <p>NeoForge 1.21.1 移植：ForgeConfigSpec -> ModConfigSpec；ModLoadingContext 走 getActiveContainer()；
 * {@code ModConfig#getConfigData()} 在 NeoForge 已移除，因此 initializeWorld 只判断 global spec 是否已加载。
 */
@EventBusSubscriber(modid = "dragonmaid")
public final class GlobalBalanceConfig {
    private static final Scope GAMEPLAY_SCOPE = mirror(DragonMaidConfig.SPEC);
    private static final Scope RECALL_SCOPE = mirror(RecallConfig.SPEC);
    public static final ModConfigSpec GAMEPLAY = GAMEPLAY_SCOPE.global();
    public static final ModConfigSpec RECALL = RECALL_SCOPE == null ? null : RECALL_SCOPE.global();
    public static final ModConfigSpec WORLD_GAMEPLAY = GAMEPLAY_SCOPE.world();
    public static final ModConfigSpec WORLD_RECALL = RECALL_SCOPE == null ? null : RECALL_SCOPE.world();

    private GlobalBalanceConfig() {
    }

    public static void register() {
        ModContainer container = ModLoadingContext.get().getActiveContainer();
        container.registerConfig(Type.COMMON, GAMEPLAY, "dragonmaid-common.toml");
        container.registerConfig(Type.SERVER, WORLD_GAMEPLAY, "dragonmaid-world-server.toml");
        if (RECALL_SCOPE != null) {
            container.registerConfig(Type.COMMON, RECALL, "dragonmaid-recall-common.toml");
            container.registerConfig(Type.SERVER, WORLD_RECALL, "dragonmaid-recall-world-server.toml");
        }
    }

    /** True for the plain session SERVER configs (they are forwarded to clients). */
    public static boolean transport(ModConfig config) {
        return "dragonmaid".equals(config.getModId()) && config.getType() == Type.SERVER && !worldOverride(config);
    }

    /** True for the per-world override SERVER configs (never synced to clients). */
    public static boolean worldOverride(ModConfig config) {
        return worldScope(config) != null;
    }

    /** Used by the config UI to hide Dragon Maid's bookkeeping entries. */
    public static boolean internalEntry(Object key) {
        return key == GAMEPLAY_SCOPE.initialized() || RECALL_SCOPE != null && key == RECALL_SCOPE.initialized();
    }

    private static Scope worldScope(ModConfig config) {
        if (config.getSpec() == WORLD_GAMEPLAY) {
            return GAMEPLAY_SCOPE;
        }

        return RECALL_SCOPE != null && config.getSpec() == WORLD_RECALL ? RECALL_SCOPE : null;
    }

    private static Scope mirror(ModConfigSpec session) {
        Builder global = new Builder();
        Builder world = new Builder();
        BooleanValue enabled = world.comment("Use the settings in this file for this local world, including when sharing it over LAN.")
            .translation("config.dragonmaid.useWorldSettings")
            .worldRestart()
            .define("useWorldSettings", false);
        BooleanValue initialized = world.comment("Tracks the initial copy of global values. Managed by Dragon Maid.")
            .define("initialized", false);
        ArrayList<Binding> bindings = new ArrayList<>();
        copyDefinitions(global, world, session, session.getValues(), List.of(), bindings);
        return new Scope(global.build(), world.build(), enabled, initialized, List.copyOf(bindings));
    }

    private static void copyDefinitions(
        Builder global, Builder world, ModConfigSpec session, UnmodifiableConfig values, List<String> parent, List<Binding> bindings
    ) {
        values.valueMap().forEach((key, value) -> {
            ArrayList<String> path = new ArrayList<>(parent);
            path.add(key);
            if (value instanceof UnmodifiableConfig children) {
                copyDefinitions(global, world, session, children, path, bindings);
            } else if (value instanceof ConfigValue<?> field) {
                ValueSpec definition = (ValueSpec) session.getSpec().get(path);
                // 1.2.7: the starter companion section deliberately stays global-only.
                ConfigValue<?> worldField = path.get(0).equals("starterCompanion") ? null : define(world, path, definition);
                bindings.add(new Binding(define(global, path, definition), worldField, field));
            }
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ConfigValue<?> define(Builder builder, List<String> path, ValueSpec definition) {
        ConfigValue<?> defined;
        if (definition.getDefault() instanceof Enum choice) {
            builder.comment(definition.getComment()).translation(definition.getTranslationKey()).worldRestart();
            defined = builder.defineEnum(path, choice);
        } else {
            defined = builder.define(path, definition, definition::getDefault);
        }

        return defined;
    }

    /** Copies the global defaults into a freshly created per-world file (once). */
    public static void initializeWorld(ModConfig config) {
        Scope scope = worldScope(config);
        if (scope == null || !scope.global().isLoaded() || scope.initialized().get()) {
            return;
        }

        if (!scope.enabled().get()) {
            for (Binding binding : scope.bindings()) {
                if (binding.world() != null) {
                    copy(binding.global(), binding.world());
                }
            }
        }

        scope.initialized().set(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void starting(ServerAboutToStartEvent event) {
        applyForNextSession(!event.getServer().isDedicatedServer());
    }

    public static void applyForNextSession() {
        applyForNextSession(false);
    }

    private static void applyForNextSession(boolean local) {
        apply(GAMEPLAY_SCOPE, local);
        if (RECALL_SCOPE != null) {
            apply(RECALL_SCOPE, local);
        }

        CompanionBalanceConfig.capture();
    }

    private static void apply(Scope scope, boolean local) {
        boolean override = local && scope.enabled().get();
        for (Binding binding : scope.bindings()) {
            copy(override && binding.world() != null ? binding.world() : binding.global(), binding.session());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void copy(ConfigValue<?> source, ConfigValue target) {
        target.set(source.get());
    }

    private record Binding(ConfigValue<?> global, ConfigValue<?> world, ConfigValue<?> session) {
    }

    private record Scope(ModConfigSpec global, ModConfigSpec world, BooleanValue enabled, BooleanValue initialized, List<Binding> bindings) {
    }

    /** Keeps a fresh per-world file in sync with the global file until it is first used. */
    @EventBusSubscriber(modid = "dragonmaid", bus = Bus.MOD)
    public static final class WorldConfigLoading {
        @SubscribeEvent
        public static void loading(Loading event) {
            GlobalBalanceConfig.initializeWorld(event.getConfig());
        }
    }
}
