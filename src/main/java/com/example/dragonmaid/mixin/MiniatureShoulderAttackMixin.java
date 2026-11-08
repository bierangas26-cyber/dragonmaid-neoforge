package com.example.dragonmaid.mixin;

import com.example.dragonmaid.accessory.MiniatureForm;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.util.dragon.IafDragonLogic;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {IafDragonLogic.class},
   remap = false
)
public abstract class MiniatureShoulderAttackMixin {
   @Shadow
   @Final
   private DragonBaseEntity dragon;

   @Inject(
      method = {"attackTarget"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void dragonmaid$turretOnly(Entity victim, Player rider, float amount, CallbackInfoReturnable<Boolean> cir) {
      if (this.dragon.isPassenger() && MiniatureForm.isMatureMini(this.dragon)) {
         cir.setReturnValue(false);
      }
   }
}
