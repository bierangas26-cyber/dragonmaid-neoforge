package com.example.dragonmaid.combat;

import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public final class MiniatureBalance {
   static IntValue windup;
   static IntValue betweenShots;
   static IntValue recovery;
   static IntValue pounceCooldown;
   static DoubleValue range;
   static DoubleValue pounceRange;
   static DoubleValue pounceSpeed;
   static DoubleValue boltSpeed;
   static DoubleValue boltRadius;
   static DoubleValue elderRadius;
   static DoubleValue breathMultiplier;
   static DoubleValue breathGrowth;
   static DoubleValue lightningBase;
   static DoubleValue lightningGrowth;
   static DoubleValue guardRadius;

   private MiniatureBalance() {
   }

   public static void define(Builder b) {
      b.push("miniatureCombat");
      guardRadius = b.comment("Owner-centered combat radius for miniature dragon-form guards; independent of humanoid escort.")
         .translation("config.dragonmaid.miniatureCombat.guardRadiusBlocks")
         .defineInRange("guardRadiusBlocks", 24.0, 8.0, 64.0);
      range = b.translation("config.dragonmaid.miniatureCombat.rangeBlocks").defineInRange("rangeBlocks", 34.0, 8.0, 64.0);
      windup = b.translation("config.dragonmaid.miniatureCombat.windupTicks").defineInRange("windupTicks", 32, 8, 200);
      betweenShots = b.translation("config.dragonmaid.miniatureCombat.doubleShotIntervalTicks").defineInRange("doubleShotIntervalTicks", 12, 10, 60);
      recovery = b.translation("config.dragonmaid.miniatureCombat.recoveryTicks").defineInRange("recoveryTicks", 48, 10, 300);
      pounceCooldown = b.translation("config.dragonmaid.miniatureCombat.pounceCooldownTicks").defineInRange("pounceCooldownTicks", 40, 20, 200);
      pounceRange = b.translation("config.dragonmaid.miniatureCombat.pounceRangeBlocks").defineInRange("pounceRangeBlocks", 6.0, 2.0, 8.0);
      pounceSpeed = b.comment("Caps the native tackle speed without replacing its steering.")
         .translation("config.dragonmaid.miniatureCombat.pounceMaxSpeed")
         .defineInRange("pounceMaxSpeed", 0.92, 0.3, 1.2);
      breathMultiplier = b.comment("Scales IAF's species-specific breath damage times the real growth stage.")
         .translation("config.dragonmaid.miniatureCombat.fireIceDamageMultiplier")
         .defineInRange("fireIceDamageMultiplier", 1.5, 0.0, 10.0);
      breathGrowth = b.comment("Additional damage multiplier reached at full growth.")
         .translation("config.dragonmaid.miniatureCombat.fireIceAddedMultiplierAt125Days")
         .defineInRange("fireIceAddedMultiplierAt125Days", 0.8, 0.0, 10.0);
      boltSpeed = b.translation("config.dragonmaid.miniatureCombat.boltBlocksPerTick").defineInRange("boltBlocksPerTick", 2.5, 1.0, 3.5);
      boltRadius = b.translation("config.dragonmaid.miniatureCombat.boltBlastRadius").defineInRange("boltBlastRadius", 1.4, 0.5, 5.0);
      elderRadius = b.comment("Fifth-stage radius is always at least the ordinary radius.")
         .translation("config.dragonmaid.miniatureCombat.stageFiveBoltBlastRadius")
         .defineInRange("stageFiveBoltBlastRadius", 2.5, 1.0, 8.0);
      lightningBase = b.comment("Fractions of an IAF fifth-stage large bolt. Combined power stays below that bolt.")
         .translation("config.dragonmaid.miniatureCombat.boltBaseDamageFraction")
         .defineInRange("boltBaseDamageFraction", 0.35, 0.0, 0.95);
      lightningGrowth = b.comment("Additional damage fraction reached at full growth.")
         .translation("config.dragonmaid.miniatureCombat.boltAddedDamageFractionAt125Days")
         .defineInRange("boltAddedDamageFractionAt125Days", 0.35, 0.0, 0.95);
      b.pop();
   }
}
