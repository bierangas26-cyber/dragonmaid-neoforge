package com.example.dragonmaid.combat;

import com.example.dragonmaid.data.DragonForms;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class HumanoidKnockback {
   /** 1.21.1 的 AttributeModifier 用 ResourceLocation 作 id（UUID 版本已移除）。 */
   private static final ResourceLocation BONUS = ResourceLocation.fromNamespaceAndPath("dragonmaid", "humanoid_knockback_resistance");

   private HumanoidKnockback() {
   }

   public static void refresh(DragonBaseEntity dragon) {
      if (!dragon.level().isClientSide) {
         AttributeInstance resistance = dragon.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
         if (resistance != null) {
            double amount = !DragonForms.isHumanoid(dragon)
               ? 0.0
               : switch (dragon.getDragonStage()) {
                  case 3 -> 0.2;
                  case 4 -> 0.35;
                  case 5 -> 0.5;
                  default -> 0.0;
               };
            AttributeModifier previous = resistance.getModifier(BONUS);
            if (previous == null || previous.amount() != amount) {
               resistance.removeModifier(BONUS);
               if (amount > 0.0) {
                  resistance.addTransientModifier(new AttributeModifier(BONUS, amount, Operation.ADD_VALUE));
               }
            }
         }
      }
   }
}
