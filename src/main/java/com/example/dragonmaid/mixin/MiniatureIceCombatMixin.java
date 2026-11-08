package com.example.dragonmaid.mixin;

import com.example.dragonmaid.accessory.MiniatureForm;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.IceDragonEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * IceDragonEntity 专属：CE 2.2 只有 Ice 声明 shootIceAtMob，因此从 MiniatureVariantCombatMixin 拆出。
 */
@Mixin({IceDragonEntity.class})
public abstract class MiniatureIceCombatMixin {
   @Inject(
      method = {"shootIceAtMob"},
      at = {@At("HEAD")},
      remap = false,
      cancellable = true
   )
   private void dragonmaid$replaceMiniBreath(LivingEntity enemy, CallbackInfo ci) {
      if (MiniatureForm.isMatureMini((DragonBaseEntity)(Object)this)) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"riderShootFire"},
      at = {@At("HEAD")},
      remap = false,
      cancellable = true
   )
   private void dragonmaid$replaceRiddenBreath(Entity rider, CallbackInfo ci) {
      if (MiniatureForm.isMatureMini((DragonBaseEntity)(Object)this)) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"doHurtTarget"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void dragonmaid$noMountedMelee(Entity target, CallbackInfoReturnable<Boolean> cir) {
      DragonBaseEntity dragon = (DragonBaseEntity)(Object)this;
      if (dragon.isPassenger() && MiniatureForm.isMatureMini(dragon)) {
         cir.setReturnValue(false);
      }
   }
}
