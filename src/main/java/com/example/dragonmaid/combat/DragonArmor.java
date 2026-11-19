package com.example.dragonmaid.combat;

import com.example.dragonmaid.item.ServiceDragonArmorItem;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;

public final class DragonArmor {
   private static final int MAX_TOTAL_ARMOR = 40;
   private static final float MAX_EFFECTIVE_ARMOR = 22.5F;
   private static final double SPECIALTY_BONUS = 0.5;

   private DragonArmor() {
   }

   public static int totalArmor(DragonBaseEntity dragon) {
      return (int)Math.min(40.0, rawArmor(dragon));
   }

   public static float armorForDamage(DragonBaseEntity dragon) {
      return specialtyBonus(dragon) > 0.0 ? (float)Math.min(40.0, rawArmor(dragon)) : (float)totalArmor(dragon);
   }

   public static double specialtyBonus(DragonBaseEntity dragon) {
      if (dragon.dragonInventory == null) {
         return 0.0;
      } else {
         int pieces = 0;

         for (int slot = 1; slot <= 4; slot++) {
            if (ServiceDragonArmorItem.isSpecialtyArmor(dragon.dragonInventory.getItem(slot))) {
               pieces++;
            }
         }

         return (double)pieces * 0.5;
      }
   }

   public static double rawArmor(DragonBaseEntity dragon) {
      AttributeInstance armor = dragon.getAttribute(Attributes.ARMOR);
      if (armor == null) {
         return 0.0;
      } else {
         double base = armor.getBaseValue();
         // 1.21.1: AttributeInstance#getModifiers(Operation) 不再是公开 API，改为遍历全部再按 operation() 过滤
         for (AttributeModifier modifier : armor.getModifiers()) {
            if (modifier.operation() == Operation.ADD_VALUE) {
               base += modifier.amount();
            }
         }

         double value = base;

         for (AttributeModifier modifier : armor.getModifiers()) {
            if (modifier.operation() == Operation.ADD_MULTIPLIED_BASE) {
               value += base * modifier.amount();
            }
         }

         for (AttributeModifier modifier : armor.getModifiers()) {
            if (modifier.operation() == Operation.ADD_MULTIPLIED_TOTAL) {
               value *= 1.0 + modifier.amount();
            }
         }

         return !Double.isFinite(value) ? armor.getValue() : Math.max(0.0, value);
      }
   }

   public static float afterArmor(float damage, float armor, float toughness) {
      float divisor = 2.0F + toughness / 4.0F;
      float effective = Math.min(22.5F, Math.max(armor * 0.2F, armor - damage / divisor));
      return damage * (1.0F - effective / 25.0F);
   }
}