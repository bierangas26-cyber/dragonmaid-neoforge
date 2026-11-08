package com.example.dragonmaid;

import com.example.dragonmaid.combat.HumanoidMeleeMode;
import com.example.dragonmaid.combat.KnowledgeSupport;
import com.example.dragonmaid.combat.MiniatureBalance;
import com.example.dragonmaid.config.CompanionBalanceConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.EnumValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public final class DragonMaidConfig {
   public static final ModConfigSpec SPEC;
   public static final IntValue BOOK_EXPERIENCE;
   public static final IntValue MANUSCRIPT_EXPERIENCE;
   public static final IntValue EXPERIENCE_BOTTLE_EXPERIENCE;
   public static final BooleanValue GIVE_STARTER_GUIDE;
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
   public static final EnumValue<HumanoidMeleeMode> HUMANOID_MELEE_MODE;

   private DragonMaidConfig() {
   }

   static {
      Builder b = new Builder();
      BOOK_EXPERIENCE = b.comment("Knowledge per ordinary book. Level one requires 300.")
         .translation("config.dragonmaid.bookExperience")
         .defineInRange("bookExperience", 5, 1, 300);
      MANUSCRIPT_EXPERIENCE = b.comment("Knowledge per Ice and Fire manuscript.")
         .translation("config.dragonmaid.manuscriptExperience")
         .defineInRange("manuscriptExperience", 8, 1, 300);
      EXPERIENCE_BOTTLE_EXPERIENCE = b.comment("Knowledge per experience bottle consumed in the learning menu.")
         .translation("config.dragonmaid.experienceBottleExperience")
         .defineInRange("experienceBottleExperience", 12, 1, 300);
      GIVE_STARTER_GUIDE = b.comment("Give a Dragon Maid guide when a player earns the first-join advancement. Does not disable the advancement or recipe.")
         .translation("config.dragonmaid.giveStarterGuide")
         .worldRestart()
         .define("giveStarterGuide", true);
      HUMANOID_BREATH_RANGE_MULTIPLIER = b.comment(
            new String[]{
               "Multiplier applied to both existing humanoid breath range settings and all growth stages.",
               "Default 1.5 extends fire/ice to 21/27/33 blocks and lightning to 15/19.5/24 blocks."
            }
         )
         .translation("config.dragonmaid.humanoidBreathRangeMultiplier")
         .defineInRange("humanoidBreathRangeMultiplier", 1.5, 1.0, 2.0);
      WILD_DRAGON_TARGET_RANGE = b.comment(
            "Maximum local detection distance to a hostile wild dragon's body. Does not expand the owner's escort movement boundary."
         )
         .translation("config.dragonmaid.wildDragonTargetRange")
         .defineInRange("wildDragonTargetRange", 40.0, 16.0, 64.0);
      WILD_DRAGON_BREATH_DAMAGE_MULTIPLIER = b.comment(
            "Damage taken by owned tamed dragons from wild dragon fire/ice/lightning breath and charges. Applies to both forms; 0.5 means half damage."
         )
         .translation("config.dragonmaid.wildDragonBreathDamageMultiplier")
         .defineInRange("wildDragonBreathDamageMultiplier", 0.5, 0.0, 1.0);
      WILD_DRAGON_MELEE_DAMAGE_MULTIPLIER = b.comment(
            "Damage taken by owned tamed dragons from wild dragon melee attacks. Applies to both forms; 0.75 means 25 percent less damage."
         )
         .translation("config.dragonmaid.wildDragonMeleeDamageMultiplier")
         .defineInRange("wildDragonMeleeDamageMultiplier", 0.75, 0.0, 1.0);
      BREATH_RANGE = b.comment("Base stage-five humanoid lightning beam range in blocks, before humanoidBreathRangeMultiplier.")
         .translation("config.dragonmaid.breathRange")
         .defineInRange("breathRange", 16.0, 4.0, 32.0);
      FIRE_ICE_BREATH_RANGE = b.comment(
            new String[]{
               "Base stage-five humanoid fire/ice fan range. Stages 3/4/5 use 14/22, 18/22, 1 of this value, then humanoidBreathRangeMultiplier.",
               "Separate from breathRange for independent fire/ice and lightning tuning."
            }
         )
         .translation("config.dragonmaid.fireIceBreathRange")
         .defineInRange("fireIceBreathRange", 22.0, 4.0, 48.0);
      BREATH_WINDUP = b.comment(
            new String[]{
               "Fire/ice preparation ticks; completed preparation is held until range and line of sight allow emission.",
               "Moving or being outside range does not restart preparation. Lightning keeps its escort aiming timeout."
            }
         )
         .translation("config.dragonmaid.breathWindupTicks")
         .defineInRange("breathWindupTicks", 20, 5, 100);
      BREATH_RECOVERY = b.comment("Recovery ticks after a humanoid combat breath burst.")
         .translation("config.dragonmaid.breathRecoveryTicks")
         .defineInRange("breathRecoveryTicks", 60, 10, 200);
      BREATH_SPEED = b.comment("Fire/ice combat fan speed in blocks per tick; visuals share the same speed.")
         .translation("config.dragonmaid.breathTravelSpeed")
         .defineInRange("breathTravelSpeed", 2.0, 1.0, 4.0);
      CHARGE_COOLDOWN = b.comment("Minimum ticks between stage-five humanoid elemental balls.")
         .translation("config.dragonmaid.elementalChargeCooldownTicks")
         .defineInRange("elementalChargeCooldownTicks", 200, 40, 1200);
      ROAR_COOLDOWN = b.comment("Ticks between stage-four/five humanoid combat roars. Cooldown survives form changes and horn storage.")
         .translation("config.dragonmaid.humanoidRoarCooldownTicks")
         .defineInRange("humanoidRoarCooldownTicks", 600, 100, 2400);
      GUARD_COMBAT_RADIUS = b.comment("Maximum distance from the owner for humanoid escort combat positioning.")
         .translation("config.dragonmaid.guardCombatRadius")
         .defineInRange("guardCombatRadius", 12.0, 4.0, 32.0);
      GUARD_TELEPORT_DISTANCE = b.comment(
            new String[]{
               "Escort distance from the owner that triggers safe pet-style teleport attempts.", "The effective threshold is at least guardCombatRadius + 1."
            }
         )
         .translation("config.dragonmaid.guardTeleportDistance")
         .defineInRange("guardTeleportDistance", 16.0, 6.0, 64.0);
      b.push("matureShedding");
      MATURE_SCALES = b.comment("Scales per successful NEW growth day after companionGrowth.fullGrowthDays. Zero disables shedding.")
         .translation("config.dragonmaid.matureScalesPerDay")
         .worldRestart()
         .defineInRange("matureScalesPerDay", 1, 0, 16);
      b.pop();
      HUMANOID_MELEE_MODE = b.comment(
            new String[]{
               "Humanoid ordinary melee: ANIMATED uses authored combos; LEGACY hits immediately on cooldown without the combo animation.",
               "Does not change tail sweeps, breath or dragon-form combat."
            }
         )
         .translation("config.dragonmaid.humanoidMeleeMode")
         .defineEnum("humanoidMeleeMode", HumanoidMeleeMode.ANIMATED);
      KnowledgeSupport.configure(b);
      MiniatureBalance.define(b);
      CompanionBalanceConfig.define(b);
      SPEC = b.build();
   }
}
