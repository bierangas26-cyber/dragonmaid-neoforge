package com.example.dragonmaid.starter;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.example.dragonmaid.config.GlobalBalanceConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

/**
 * 1.2.7 starting-companion config (starterCompanion section).
 *
 * <p>Upstream keeps these entries out of the world-override scope: the
 * {@code dragonmaid-common.toml} mirror ({@link GlobalBalanceConfig#GAMEPLAY}) is the only global
 * source, so {@code limits(true)} reads the mirrored spec and {@code limits(false)} reads the live
 * session spec. Must be called from the mod config spec builder (core).
 */
public final class StarterConfig {
    private static BooleanValue SHOW_ENTRY;
    public static IntValue MIN_DAYS;
    public static IntValue MAX_DAYS;
    public static IntValue MIN_KNOWLEDGE;
    public static IntValue MAX_KNOWLEDGE;

    private StarterConfig() {
    }

    public static void define(Builder builder) {
        builder.translation("config.dragonmaid.starterCompanion").push("starterCompanion");
        SHOW_ENTRY = builder.comment("Show the starting companion option when creating a world in this instance.")
            .translation("config.dragonmaid.starterShowEntry")
            .worldRestart()
            .define("showEntry", true);
        MIN_DAYS = builder.comment("Minimum selectable starting companion age in days.")
            .translation("config.dragonmaid.starterMinimumDays")
            .worldRestart()
            .defineInRange("minimumDays", 1, 0, 10000);
        MAX_DAYS = builder.comment("Maximum selectable starting companion age in days.")
            .translation("config.dragonmaid.starterMaximumDays")
            .worldRestart()
            .defineInRange("maximumDays", 125, 0, 10000);
        MIN_KNOWLEDGE = builder.comment("Minimum selectable starting knowledge rank.")
            .translation("config.dragonmaid.starterMinimumKnowledge")
            .worldRestart()
            .defineInRange("minimumKnowledge", 0, 0, 3);
        MAX_KNOWLEDGE = builder.comment("Maximum selectable starting knowledge rank.")
            .translation("config.dragonmaid.starterMaximumKnowledge")
            .worldRestart()
            .defineInRange("maximumKnowledge", 3, 0, 3);
        builder.pop();
    }

    public static boolean showEntry() {
        if (SHOW_ENTRY == null) {
            return true;
        }

        ConfigValue<Boolean> global = find(GlobalBalanceConfig.GAMEPLAY.getValues(), SHOW_ENTRY.getPath());
        return global == null ? SHOW_ENTRY.get() : global.get();
    }

    private static int value(IntValue session, boolean global) {
        if (!global || session == null) {
            return session == null ? 0 : session.get();
        }

        ConfigValue<Object> field = find(GlobalBalanceConfig.GAMEPLAY.getValues(), session.getPath());
        return field != null && field.get() instanceof Number number ? number.intValue() : session.get();
    }

    @SuppressWarnings("unchecked")
    private static <T> ConfigValue<T> find(UnmodifiableConfig values, java.util.List<String> path) {
        if (values == null || path == null || path.isEmpty()) {
            return null;
        }

        Object raw = values.get(path);
        return raw instanceof ConfigValue<?> value ? (ConfigValue<T>) value : null;
    }

    public static Limits limits(boolean global) {
        int a = value(MIN_DAYS, global);
        int b = value(MAX_DAYS, global);
        int c = value(MIN_KNOWLEDGE, global);
        int d = value(MAX_KNOWLEDGE, global);
        return new Limits(Math.min(a, b), Math.max(a, b), Math.min(c, d), Math.max(c, d));
    }

    public record Limits(int minDays, int maxDays, int minKnowledge, int maxKnowledge) {
        public int days(int value) {
            return Math.max(this.minDays, Math.min(this.maxDays, value));
        }

        public int knowledge(int value) {
            return Math.max(this.minKnowledge, Math.min(this.maxKnowledge, value));
        }
    }
}
