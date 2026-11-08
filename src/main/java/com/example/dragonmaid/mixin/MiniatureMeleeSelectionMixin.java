package com.example.dragonmaid.mixin;

import com.example.dragonmaid.accessory.MiniatureForm;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.util.dragon.IafDragonAttacks.Ground;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({DragonBaseEntity.class})
abstract class MiniatureMeleeSelectionMixin {
   @Inject(
      method = {"randomizeAttacks"},
      remap = false,
      at = {@At("RETURN")}
   )
   private void dragonmaid$preferWingsOnGround(CallbackInfo ci) {
      DragonBaseEntity dragon = (DragonBaseEntity)(Object)this;
      if (dragon.onGround()
         && !dragon.level().isClientSide
         && MiniatureForm.isMatureMini(dragon)
         && !dragon.isPassenger()
         && !dragon.isFlying()
         && !dragon.isHovering()
         && dragon.getControllingPassenger() == null) {
         Ground choice = dragon.groundAttack;
         if (choice == Ground.FIRE || (choice == Ground.TAIL_WHIP || choice == Ground.SHAKE_PREY) && dragon.getRandom().nextBoolean()) {
            dragon.groundAttack = Ground.WING_BLAST;
         }
      }
   }
}
