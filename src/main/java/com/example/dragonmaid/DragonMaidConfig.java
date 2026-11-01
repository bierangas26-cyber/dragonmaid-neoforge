package com.example.dragonmaid;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public final class DragonMaidConfig {
    public static final ModConfigSpec SPEC;
    public static final IntValue BOOK_EXPERIENCE;
    public static final IntValue MANUSCRIPT_EXPERIENCE;
    public static final IntValue EXPERIENCE_BOTTLE_EXPERIENCE;
    public static final IntValue KNOWLEDGE_BALANCE_REVISION;
    public static final DoubleValue HUMANOID_BREATH_RANGE_MULTIPLIER;
    public static final DoubleValue WILD_DRAGON_TARGET_RANGE;
    public static final DoubleValue WILD_DRAGON_BREATH_DAMAGE_MULTIPLIER;
    public static final DoubleValue WILD_DRAGON_MELEE_DAMAGE_MULTIPLIER;
    public static final DoubleValue BREATH_RANGE;
    public static final DoubleValue FIRE_ICE_BREATH_RANGE;
    public static final IntValue BREATH_WINDUP;
    public static final IntValue BREATH_RECOVERY;
    public static final DoubleValue BREATH_SPEED;
    public static final IntValue CHARGE_COOLDOWN;
    public static final IntValue ROAR_COOLDOWN;
    public static final DoubleValue GUARD_COMBAT_RADIUS;
    public static final DoubleValue GUARD_TELEPORT_DISTANCE;
    public static final IntValue MATURE_SCALES;

    private DragonMaidConfig() {
    }

    static {
        Builder builder = new Builder();
        BOOK_EXPERIENCE = builder.comment("Knowledge per ordinary book. Level one requires 300.").defineInRange("bookExperience", 5, 1, 300);
        MANUSCRIPT_EXPERIENCE = builder.comment("Knowledge per Ice and Fire manuscript.").defineInRange("manuscriptExperience", 8, 1, 300);
        EXPERIENCE_BOTTLE_EXPERIENCE = builder.comment("Knowledge per experience bottle consumed in the learning menu.")
            .defineInRange("experienceBottleExperience", 12, 1, 300);
        KNOWLEDGE_BALANCE_REVISION = builder.comment("Internal migration of the old manuscript/bottle defaults. Leave at 2 after migration.")
            .defineInRange("knowledgeBalanceRevision", 0, 0, Integer.MAX_VALUE);
        HUMANOID_BREATH_RANGE_MULTIPLIER = builder.comment(
                new String[]{
                    "Multiplier applied to both existing humanoid breath range settings and all growth stages.",
                    "Default 1.5 extends fire/ice to 21/27/33 blocks and lightning to 15/19.5/24 blocks."
                }
            )
            .defineInRange("humanoidBreathRangeMultiplier", 1.5, 1.0, 2.0);
        WILD_DRAGON_TARGET_RANGE = builder.comment(
                "Maximum local detection distance to a hostile wild dragon's body. Does not expand the owner's escort movement boundary."
            )
            .defineInRange("wildDragonTargetRange", 40.0, 16.0, 64.0);
        WILD_DRAGON_BREATH_DAMAGE_MULTIPLIER = builder.comment(
                "Damage taken by owned tamed dragons from wild dragon fire/ice/lightning breath and charges. Applies to both forms; 0.5 means half damage."
            )
            .defineInRange("wildDragonBreathDamageMultiplier", 0.5, 0.0, 1.0);
        WILD_DRAGON_MELEE_DAMAGE_MULTIPLIER = builder.comment(
                "Damage taken by owned tamed dragons from wild dragon melee attacks. Applies to both forms; 0.75 means 25 percent less damage."
            )
            .defineInRange("wildDragonMeleeDamageMultiplier", 0.75, 0.0, 1.0);
        BREATH_RANGE = builder.comment("Base stage-five humanoid lightning beam range in blocks, before humanoidBreathRangeMultiplier.")
            .defineInRange("breathRange", 16.0, 4.0, 32.0);
        FIRE_ICE_BREATH_RANGE = builder.comment(
                new String[]{
                    "Base stage-five humanoid fire/ice fan range. Stages 3/4/5 use 14/22, 18/22, 1 of this value, then humanoidBreathRangeMultiplier.",
                    "Separate from breathRange for independent fire/ice and lightning tuning."
                }
            )
            .defineInRange("fireIceBreathRange", 22.0, 4.0, 48.0);
        BREATH_WINDUP = builder.comment(
                new String[]{
                    "Fire/ice preparation ticks; completed preparation is held until range and line of sight allow emission.",
                    "Moving or being outside range does not restart preparation. Lightning keeps its escort aiming timeout."
                }
            )
            .defineInRange("breathWindupTicks", 20, 5, 100);
        BREATH_RECOVERY = builder.comment("Recovery ticks after a humanoid combat breath burst.").defineInRange("breathRecoveryTicks", 60, 10, 200);
        BREATH_SPEED = builder.comment("Fire/ice combat fan speed in blocks per tick; visuals share the same speed.")
            .defineInRange("breathTravelSpeed", 2.0, 1.0, 4.0);
        CHARGE_COOLDOWN = builder.comment("Minimum ticks between stage-five humanoid elemental balls.")
            .defineInRange("elementalChargeCooldownTicks", 200, 40, 1200);
        ROAR_COOLDOWN = builder.comment("Ticks between stage-four/five humanoid combat roars. Cooldown survives form changes and horn storage.")
            .defineInRange("humanoidRoarCooldownTicks", 600, 100, 2400);
        GUARD_COMBAT_RADIUS = builder.comment("Maximum distance from the owner for humanoid escort combat positioning.")
            .defineInRange("guardCombatRadius", 12.0, 4.0, 32.0);
        GUARD_TELEPORT_DISTANCE = builder.comment(
                new String[]{
                    "Escort distance from the owner that triggers safe pet-style teleport attempts.",
                    "The effective threshold is at least guardCombatRadius + 1."
                }
            )
            .defineInRange("guardTeleportDistance", 16.0, 6.0, 64.0);
        MATURE_SCALES = builder.comment("Scales per new growth day after 125 days. Zero disables shedding.").defineInRange("matureScalesPerDay", 1, 0, 16);
        SPEC = builder.build();
    }
}
