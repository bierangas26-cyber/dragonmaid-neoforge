package com.example.dragonmaid.mixin;

import com.example.dragonmaid.inventory.DragonBagPickupGoal;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.ai.DragonAIEscortGoal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({DragonAIEscortGoal.class})
public abstract class DragonEscortLootMixin {
   @Shadow(
      remap = false
   )
   @Final
   private DragonBaseEntity dragon;

   @Inject(
      method = {"tick"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void dragonmaid$nearbyLootBeforeReturn(CallbackInfo ci) {
      if (this.dragon.getTarget() == null && this.dragon.getCommand() == 2 && DragonBagPickupGoal.deferEscort(this.dragon, this.dragon.getOwner())) {
         ci.cancel();
      }
   }
}
