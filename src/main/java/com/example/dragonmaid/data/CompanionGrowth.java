package com.example.dragonmaid.data;

import com.example.dragonmaid.accessory.MiniatureForm;
import com.example.dragonmaid.combat.HumanoidKnockback;
import com.example.dragonmaid.config.CompanionBalanceConfig;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * 1.2: 已驯服龙的成长曲线（生命/近战/移速/护甲端点）与褪鳞计算。
 * 1.21.1: IAF CE 2.2 用 DragonBaseEntity#refreshDirtyAttributes() 重算属性，不再需要 Forge 版的 @Invoker 访问器。
 */
public final class CompanionGrowth {
    private static final Map<DragonBaseEntity, Integer> APPLIED_AGE = new WeakHashMap<>();

    private CompanionGrowth() {
    }

    public static boolean owned(DragonBaseEntity dragon) {
        UUID owner = dragon.getOwnerUUID();
        return dragon.isTame() && owner != null && (owner.getMostSignificantBits() != 0L || owner.getLeastSignificantBits() != 0L);
    }

    public static double growthFraction(DragonBaseEntity dragon) {
        int end = owned(dragon) ? CompanionBalanceConfig.values().fullGrowthDays() : 125;
        return Mth.clamp((double)dragon.getAgeInTicks() / 24000.0 / (double)end, 0.0, 1.0);
    }

    public static void refresh(DragonBaseEntity dragon) {
        if (!dragon.level().isClientSide && owned(dragon)) {
            CompanionBalanceConfig.Values config = CompanionBalanceConfig.values();
            double days = (double)Mth.clamp(dragon.getAgeInDays(), 0, config.fullGrowthDays());
            double healthEnd = config.maxHealth() > 0.0 ? config.maxHealth() : dragon.maximumHealth;
            double damageEnd = config.maxAttack() > 0.0 ? config.maxAttack() : dragon.maximumDamage;
            float health = dragon.getHealth();
            dragon.getAttribute(Attributes.MAX_HEALTH)
                .setBaseValue((double)Math.max(1L, Math.round(curve(Math.min(dragon.minimumHealth, healthEnd), healthEnd, days, config.fullGrowthDays()))));
            dragon.getAttribute(Attributes.ATTACK_DAMAGE)
                .setBaseValue((double)Math.max(0L, Math.round(curve(Math.min(dragon.minimumDamage, damageEnd), damageEnd, days, config.fullGrowthDays()))));
            dragon.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(curve(dragon.minimumSpeed, dragon.maximumSpeed, days, config.fullGrowthDays()));
            if (config.limitArmorGrowth()) {
                dragon.getAttribute(Attributes.ARMOR).setBaseValue(curve(dragon.minimumArmor, dragon.maximumArmor, days, config.fullGrowthDays()));
            }

            if (health > dragon.getMaxHealth()) {
                dragon.setHealth(dragon.getMaxHealth());
            }

            MiniatureForm.refreshMovement(dragon);
            HumanoidKnockback.refresh(dragon);
            APPLIED_AGE.put(dragon, dragon.getAgeInDays());
        }
    }

    private static double curve(double minimum, double maximum, double days, int fullGrowth) {
        return minimum + (maximum - minimum) / (double)fullGrowth * days;
    }

    public static void check(DragonBaseEntity dragon) {
        Integer previous = APPLIED_AGE.get(dragon);
        if (owned(dragon)) {
            if (previous == null || previous != dragon.getAgeInDays()) {
                refresh(dragon);
            }
        } else if (APPLIED_AGE.remove(dragon) != null) {
            dragon.refreshDirtyAttributes();
        }
    }

    public static int scales(long days, int quantity, double chance, RandomSource random) {
        if (days <= 0L || quantity <= 0 || chance <= 0.0) {
            return 0;
        } else if (chance >= 1.0) {
            return (int)Math.min(64L, days * (long)quantity);
        } else {
            int amount = 0;

            for (double logFailure = Math.log1p(-chance); days > 0L && amount < 64; amount = Math.min(64, amount + quantity)) {
                long missed = (long)Math.floor(Math.log1p(-random.nextDouble()) / logFailure);
                if (missed >= days) {
                    break;
                }

                days -= missed + 1L;
            }

            return amount;
        }
    }
}
