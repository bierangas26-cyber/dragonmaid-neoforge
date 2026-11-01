package com.example.dragonmaid.combat;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;

public final class DragonArmor {
    private static final float MAX_EFFECTIVE_ARMOR = 22.5F;

    private DragonArmor() {
    }

    public static double rawArmor(DragonBaseEntity dragon) {
        AttributeInstance attributeinstance = dragon.getAttribute(Attributes.ARMOR);
        if (attributeinstance == null) {
            return 0.0;
        } else {
            double d0 = attributeinstance.getBaseValue();

            // 1.21.1: AttributeModifier 是 record（amount()/operation()）；getModifiers(Operation) 是包内私有，
            // 只能遍历公开的 getModifiers() 再按 operation 过滤（旧 Forge 的 getAmount()/getOperation() 已删）。
            for (AttributeModifier attributemodifier : attributeinstance.getModifiers()) {
                if (attributemodifier.operation() == Operation.ADD_VALUE) {
                    d0 += attributemodifier.amount();
                }
            }

            double d1 = d0;

            for (AttributeModifier attributemodifier1 : attributeinstance.getModifiers()) {
                if (attributemodifier1.operation() == Operation.ADD_MULTIPLIED_BASE) {
                    d1 += d0 * attributemodifier1.amount();
                }
            }

            for (AttributeModifier attributemodifier2 : attributeinstance.getModifiers()) {
                if (attributemodifier2.operation() == Operation.ADD_MULTIPLIED_TOTAL) {
                    d1 *= 1.0 + attributemodifier2.amount();
                }
            }

            return !Double.isFinite(d1) ? attributeinstance.getValue() : Math.max(0.0, d1);
        }
    }

    public static float afterArmor(float damage, float armor, float toughness) {
        float f = 2.0F + toughness / 4.0F;
        float f1 = Math.min(22.5F, Math.max(armor * 0.2F, armor - damage / f));
        return damage * (1.0F - f1 / 25.0F);
    }
}
