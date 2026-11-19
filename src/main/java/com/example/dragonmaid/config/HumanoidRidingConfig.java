package com.example.dragonmaid.config;

import com.example.dragonmaid.accessory.MiniatureForm;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.util.List;
import java.util.Locale;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.EnumValue;

/**
 * 1.2.7 client config (upstream file: dragonmaid-client.toml). Picks the riding pose per growth
 * stage and gender. Registration lives in the mod entry point (core).
 */
public final class HumanoidRidingConfig {
    public static final ModConfigSpec SPEC;
    private static final List<EnumValue<Appearance>> FEMALE;
    private static final List<EnumValue<Appearance>> MALE;

    private HumanoidRidingConfig() {
    }

    private static EnumValue<Appearance> define(Builder builder, String key, Appearance initial) {
        return builder.comment("Changes the riding pose displayed on this client. Stage 3 also applies to miniature humanoids.")
            .translation("config.dragonmaid.riding." + key)
            .defineEnum(key, initial);
    }

    public static Appearance appearance(DragonBaseEntity dragon) {
        int index = MiniatureForm.isSmall(dragon) ? 0 : Mth.clamp(dragon.getDragonStage() - 3, 0, 2);
        return (dragon.isMale() ? MALE : FEMALE).get(index).get();
    }

    static {
        Builder builder = new Builder();
        builder.push("humanoidRiding");
        FEMALE = List.of(
            define(builder, "femaleStage3", Appearance.PLAYER_CARRIES_DRAGON),
            define(builder, "femaleStage4", Appearance.DRAGON_CARRIES_PLAYER),
            define(builder, "femaleStage5", Appearance.DRAGON_CARRIES_PLAYER)
        );
        MALE = List.of(
            define(builder, "maleStage3", Appearance.PIGGYBACK),
            define(builder, "maleStage4", Appearance.PIGGYBACK),
            define(builder, "maleStage5", Appearance.PIGGYBACK)
        );
        builder.pop();
        SPEC = builder.build();
    }

    public enum Appearance {
        PLAYER_CARRIES_DRAGON,
        DRAGON_CARRIES_PLAYER,
        PIGGYBACK;

        public String translationKey() {
            return "config.dragonmaid.riding." + this.name().toLowerCase(Locale.ROOT);
        }
    }
}
