package com.example.dragonmaid.config;

import com.example.dragonmaid.DragonMaidConfig;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.config.ModConfigEvent.Loading;
import net.neoforged.fml.event.config.ModConfigEvent.Unloading;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * 1.2: 伙伴成长/伤害/褪鳞/迷你化的全局平衡配置。
 * 这些条目定义在 DragonMaidConfig.SPEC（SERVER 同步配置）里，本类只做“读取快照 + 默认值”缓存。
 */
@EventBusSubscriber(modid = "dragonmaid", bus = Bus.MOD)
public final class CompanionBalanceConfig {
    public static IntValue FULL_GROWTH_DAYS;
    public static IntValue VISUAL_AGE;
    public static IntValue MOVEMENT_AGE;
    public static DoubleValue MAX_HEALTH;
    public static DoubleValue MAX_ATTACK;
    public static DoubleValue MELEE_MULTIPLIER;
    public static DoubleValue FIRE_DAMAGE;
    public static DoubleValue ICE_DAMAGE;
    public static DoubleValue LIGHTNING_DAMAGE;
    public static DoubleValue SCALE_CHANCE;
    public static DoubleValue MOVEMENT_MULTIPLIER;
    public static DoubleValue KNOCKBACK_CAP;
    public static BooleanValue LIMIT_ARMOR_GROWTH;
    private static final CompanionBalanceConfig.Values DEFAULTS = new CompanionBalanceConfig.Values(
        125, 0.0, 0.0, 1.0, true, 1.0, 1.0, 1.0, 1.0, 10, 10, 1.1, 0.66
    );
    private static volatile CompanionBalanceConfig.Values active = DEFAULTS;
    private static volatile boolean captured;

    private CompanionBalanceConfig() {
    }

    public static void define(Builder b) {
        b.comment(
                new String[]{
                    "Owned dragons only, in both forms. Restart the world/server after editing these sections.",
                    "Native stages still start at 0/25/50/75/100 days. Actual age keeps increasing for shedding."
                }
            )
            .push("companionGrowth");
        FULL_GROWTH_DAYS = b.comment(
                new String[]{
                    "Age at which health, base melee and movement growth reach their endpoints.",
                    "Also the last day before mature scale shedding begins. Does not rewrite stored age or stage thresholds."
                }
            )
            .translation("config.dragonmaid.fullGrowthDays")
            .worldRestart()
            .defineInRange("fullGrowthDays", 125, 1, 10000);
        MAX_HEALTH = b.comment(
                new String[]{
                    "Health endpoint before other mods' attribute modifiers. 0 inherits IAF dragonHealth.",
                    "The newborn endpoint remains IAF's minimumHealth; actual health is never healed by configuration changes.",
                    "Minecraft's attribute limits still apply unless another mod raises them."
                }
            )
            .translation("config.dragonmaid.maximumHealth")
            .worldRestart()
            .defineInRange("maximumHealth", 0.0, 0.0, 1000000.0);
        MAX_ATTACK = b.comment(
                new String[]{
                    "Base melee endpoint before companionDamage.meleeMultiplier, weapons and accessories.",
                    "0 inherits IAF's maximumDamage (1 + dragonAttackDamage)."
                }
            )
            .translation("config.dragonmaid.maximumBaseMeleeDamage")
            .worldRestart()
            .defineInRange("maximumBaseMeleeDamage", 0.0, 0.0, 1000000.0);
        LIMIT_ARMOR_GROWTH = b.comment(
                new String[]{
                    "Cap OWNED dragons' innate armor growth at fullGrowthDays (normally 20 innate armor).",
                    "False preserves IAF's uncapped age-based innate armor. Equipment and accessory armor are separate; wild dragons are untouched."
                }
            )
            .translation("config.dragonmaid.limitInnateArmorGrowth")
            .worldRestart()
            .define("limitInnateArmorGrowth", true);
        b.pop();
        b.comment(
                new String[]{
                    "Final outgoing elemental damage multipliers for owned dragons: native, humanoid, miniature and charge attacks.",
                    "IAF's own settings, species formulas, accessories and target defenses still apply. Restart the world/server."
                }
            )
            .push("companionDamage");
        MELEE_MULTIPLIER = b.comment(
                new String[]{
                    "Outgoing mob-attack damage multiplier, including claws, bites, wings, tails and tackles.",
                    "Applies once after existing weapon, enchantment and accessory bonuses; does not rewrite the base attack attribute."
                }
            )
            .translation("config.dragonmaid.meleeMultiplier")
            .worldRestart()
            .defineInRange("meleeMultiplier", 1.0, 0.0, 100.0);
        FIRE_DAMAGE = b.translation("config.dragonmaid.fireMultiplier").worldRestart().defineInRange("fireMultiplier", 1.0, 0.0, 100.0);
        ICE_DAMAGE = b.translation("config.dragonmaid.iceMultiplier").worldRestart().defineInRange("iceMultiplier", 1.0, 0.0, 100.0);
        LIGHTNING_DAMAGE = b.translation("config.dragonmaid.lightningMultiplier").worldRestart().defineInRange("lightningMultiplier", 1.0, 0.0, 100.0);
        b.pop();
        b.comment("Mature scale quantity and probability. Restart the world/server.").push("matureShedding");
        SCALE_CHANCE = b.comment(
                new String[]{
                    "Independent chance per eligible NEW growth day, 0..1. 1 is guaranteed; 0 disables drops.",
                    "No retroactive drops after a config change. Inventory first, ground only for overflow."
                }
            )
            .translation("config.dragonmaid.dropChancePerDay")
            .worldRestart()
            .defineInRange("dropChancePerDay", 1.0, 0.0, 1.0);
        b.pop();
        b.comment(
                new String[]{
                    "Faerie's Wish: size, movement and knockback are independent. Real age/stage/combat knowledge are retained.",
                    "SERVER config is synchronized by NeoForge before entities load. Restart the world/server to change physical size safely."
                }
            )
            .push("miniatureSize");
        VISUAL_AGE = b.comment("Maximum dragon-form appearance age. Limited to first-stage sizes so shoulder carrying remains a juvenile-sized feature.")
            .translation("config.dragonmaid.appearanceAgeDays")
            .worldRestart()
            .defineInRange("appearanceAgeDays", 10, 1, 25);
        MOVEMENT_AGE = b.comment("Reference age on IAF's native 125-day movement curve, independent of appearanceAgeDays.")
            .translation("config.dragonmaid.movementReferenceAgeDays")
            .worldRestart()
            .defineInRange("movementReferenceAgeDays", 10, 0, 125);
        MOVEMENT_MULTIPLIER = b.comment("Multiplier on that young-dragon movement reference; never increases the real dragon's unmodified movement attribute.")
            .translation("config.dragonmaid.movementMultiplier")
            .worldRestart()
            .defineInRange("movementMultiplier", 1.1, 0.05, 5.0);
        KNOCKBACK_CAP = b.comment("Maximum native miniature wing/tail melee knockback strength; separate from breath/bolt knockback.")
            .translation("config.dragonmaid.meleeKnockbackCap")
            .worldRestart()
            .defineInRange("meleeKnockbackCap", 0.66, 0.0, 2.0);
        b.pop();
    }

    public static CompanionBalanceConfig.Values values() {
        return active;
    }

    public static void capture() {
        active = new CompanionBalanceConfig.Values(
            FULL_GROWTH_DAYS.get(),
            MAX_HEALTH.get(),
            MAX_ATTACK.get(),
            MELEE_MULTIPLIER.get(),
            LIMIT_ARMOR_GROWTH.get(),
            FIRE_DAMAGE.get(),
            ICE_DAMAGE.get(),
            LIGHTNING_DAMAGE.get(),
            SCALE_CHANCE.get(),
            VISUAL_AGE.get(),
            MOVEMENT_AGE.get(),
            MOVEMENT_MULTIPLIER.get(),
            KNOCKBACK_CAP.get()
        );
        captured = true;
    }

    @SubscribeEvent
    public static void config(ModConfigEvent event) {
        if (event.getConfig().getSpec() == DragonMaidConfig.SPEC) {
            if (event instanceof Unloading) {
                captured = false;
                active = DEFAULTS;
            } else if (event instanceof Loading || !captured || ServerLifecycleHooks.getCurrentServer() == null) {
                capture();
            }
        }
    }

    public static record Values(
        int fullGrowthDays,
        double maxHealth,
        double maxAttack,
        double meleeMultiplier,
        boolean limitArmorGrowth,
        double fireDamage,
        double iceDamage,
        double lightningDamage,
        double scaleChance,
        int visualAge,
        int movementAge,
        double movementMultiplier,
        double knockbackCap
    ) {
    }
}
