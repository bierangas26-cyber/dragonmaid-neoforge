package com.example.dragonmaid.client;

import com.example.dragonmaid.combat.DragonShield;
import com.example.dragonmaid.movement.HumanoidFlight;
import com.example.dragonmaid.network.DragonNetwork;
import com.example.dragonmaid.network.HumanoidFlightInputMessage;
import com.iafenvoy.iceandfire.registry.IafKeyMappings;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(
   modid = "dragonmaid",
   value = {Dist.CLIENT}
)
public final class HumanoidFlightInput {
   private static DragonBaseEntity previousMount;
   private static boolean wasDescending;
   private static boolean wasBreathing;
   private static boolean wasStriking;
   private static boolean wasGuarding;

   private HumanoidFlightInput() {
   }

   @SubscribeEvent
   public static void tick(ClientTickEvent.Pre event) {
      {
         Minecraft minecraft = Minecraft.getInstance();
         LocalPlayer player = minecraft.player;
         if (player != null && player.getVehicle() instanceof DragonBaseEntity dragon && HumanoidFlight.validRider(dragon, player)) {
            boolean usable = minecraft.screen == null;
            boolean descend = usable && IafKeyMappings.DRAGON_DOWN != null && IafKeyMappings.DRAGON_DOWN.isDown();
            boolean breathe = usable && IafKeyMappings.DRAGON_BREATH != null && IafKeyMappings.DRAGON_BREATH.isDown();
            boolean strike = usable && IafKeyMappings.DRAGON_STRIKE != null && IafKeyMappings.DRAGON_STRIKE.isDown();
            boolean guard = usable && minecraft.options.keyUse.isDown() && HumanoidFlight.emptyHands(player) && DragonShield.hasGuardEquipment(dragon);
            HumanoidFlight.of(dragon).setDescend(descend);
            HumanoidFlight.of(dragon).setRiderAttacks(breathe, strike);
            if (dragon != previousMount || descend != wasDescending || breathe != wasBreathing || strike != wasStriking || guard != wasGuarding) {
               previousMount = dragon;
               wasDescending = descend;
               wasBreathing = breathe;
               wasStriking = strike;
               wasGuarding = guard;
               DragonNetwork.CHANNEL.sendToServer(new HumanoidFlightInputMessage(dragon.getId(), descend, breathe, strike, guard));
            }
         } else {
            previousMount = null;
            wasDescending = false;
            wasBreathing = false;
            wasStriking = false;
            wasGuarding = false;
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOW
   )
   public static void reserveGuardUse(InteractionKeyMappingTriggered event) {
      LocalPlayer player = Minecraft.getInstance().player;
      if (event.isUseItem()
         && player != null
         && player.getVehicle() instanceof DragonBaseEntity dragon
         && HumanoidFlight.validRider(dragon, player)
         && HumanoidFlight.emptyHands(player)
         && DragonShield.hasGuardEquipment(dragon)) {
         event.setSwingHand(false);
         event.setCanceled(true);
      }
   }
}
