package com.example.dragonmaid.data;

import com.example.dragonmaid.config.CompanionBalanceConfig;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * 1.2.7 humanoid health multiplier (config: humanoidHealthMultiplier).
 *
 * <p>1.21.1 port note: {@code AttributeModifier} is now a record keyed by {@link ResourceLocation}
 * (the 1.20.1 UUID constructors were removed in 1.21), so the modifier id is a namespaced
 * ResourceLocation instead of the upstream UUID f5fa59ab-8723-4ad1-b906-e90bde230c40.
 */
public final class HumanoidHealth {
    private static final ResourceLocation MODIFIER_ID =
        ResourceLocation.fromNamespaceAndPath("dragonmaid", "humanoid_health");

    private HumanoidHealth() {
    }

    public static void refresh(DragonBaseEntity dragon) {
        if (dragon.level().isClientSide) {
            return;
        }

        AttributeInstance attribute = dragon.getAttribute(Attributes.MAX_HEALTH);
        if (attribute == null) {
            return;
        }

        double multiplier = CompanionGrowth.owned(dragon) && DragonForms.isHumanoid(dragon)
            ? CompanionBalanceConfig.values().humanoidHealthMultiplier()
            : 1.0;
        double amount = multiplier - 1.0;
        AttributeModifier previous = attribute.getModifier(MODIFIER_ID);
        if (previous == null && amount == 0.0) {
            return;
        }

        if (previous != null && previous.amount() == amount && previous.operation() == Operation.ADD_MULTIPLIED_TOTAL) {
            return;
        }

        float health = dragon.getHealth();
        float maximum = dragon.getMaxHealth();
        if (previous != null) {
            attribute.removeModifier(MODIFIER_ID);
        }

        if (amount != 0.0) {
            attribute.addPermanentModifier(new AttributeModifier(MODIFIER_ID, amount, Operation.ADD_MULTIPLIED_TOTAL));
        }

        float fraction = maximum > 0.0F ? Mth.clamp(health / maximum, 0.0F, 1.0F) : 0.0F;
        dragon.setHealth(fraction * dragon.getMaxHealth());
    }
}
