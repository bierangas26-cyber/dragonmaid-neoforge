package com.example.dragonmaid.client;

import com.example.dragonmaid.combat.SpecialWeapons;
import com.example.dragonmaid.item.BladeEnchantments;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(
   modid = "dragonmaid",
   value = {Dist.CLIENT}
)
public final class BladeMovementInput {
   private BladeMovementInput() {
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void move(MovementInputUpdateEvent event) {
      if (event.getEntity() instanceof LocalPlayer player && !player.isPassenger() && SpecialWeapons.isKatana(player.getMainHandItem())) {
         boolean charging = charging(player);
         boolean windstepSwing = windstepSwing(player);
         boolean drawing = windstepSwing || !player.isUsingItem() && player.getCooldowns().isOnCooldown(player.getMainHandItem().getItem());
         if (!charging && !drawing) {
            return;
         }

         Input input = event.getInput();
         float scale;
         if (charging) {
            scale = 5.0F;
         } else {
            scale = !windstepSwing && !player.isMovingSlowly() ? Mth.clamp((float)player.getAttributeValue(Attributes.SNEAKING_SPEED), 0.0F, 1.0F) : 1.0F;
         }

         player.setSprinting(false);
         input.leftImpulse *= scale;
         input.forwardImpulse *= scale;
         return;
      }
   }

   public static boolean charging(LocalPlayer player) {
      return !player.isPassenger() && player.isUsingItem() && player.getUsedItemHand() == InteractionHand.MAIN_HAND && SpecialWeapons.isKatana(player.getUseItem());
   }

   public static boolean windstepSwing(LocalPlayer player) {
      return !player.isPassenger() && !player.isUsingItem() && PlayerBladeAnimation.isKatanaSwinging(player) && BladeEnchantments.hasWindstep(player.getMainHandItem());
   }
}
