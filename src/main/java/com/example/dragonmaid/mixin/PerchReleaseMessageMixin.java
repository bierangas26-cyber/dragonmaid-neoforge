package com.example.dragonmaid.mixin;

import com.example.dragonmaid.movement.PerchFlight;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.network.ServerNetworkHandlers;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(
   value = {ServerNetworkHandlers.class},
   remap = false
)
abstract class PerchReleaseMessageMixin {
   @Redirect(
      method = {"lambda$handleStartRidingMob$1"},
      at = @At(
         value = "INVOKE",
         remap = true,
         target = "Lnet/minecraft/world/entity/TamableAnimal;stopRiding()V"
      )
   )
   private static void dragonmaid$ignoreHeldSneakRelease(TamableAnimal animal) {
      if (animal instanceof DragonBaseEntity dragon && dragon.getVehicle() instanceof Player owner && PerchFlight.controlsDismount(owner, dragon)) {
         return;
      }

      animal.stopRiding();
   }
}
