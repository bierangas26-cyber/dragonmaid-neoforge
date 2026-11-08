package com.example.dragonmaid.mixin;

import com.example.dragonmaid.movement.PerchFlight;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Player.class})
abstract class PerchPlayerTravelMixin implements PerchFlight.Controls {
   @Unique
   private int dragonmaid$flightInput;

   @Override
   public int dragonmaid$perchKeys() {
      return this.dragonmaid$flightInput;
   }

   @Override
   public void dragonmaid$perchKeys(int keys) {
      this.dragonmaid$flightInput = keys;
   }

   @Inject(
      method = {"travel"},
      at = {@At("HEAD")}
   )
   private void dragonmaid$carryPlayer(Vec3 movement, CallbackInfo ci) {
      PerchFlight.beforeTravel((Player)(Object)this);
   }
}
