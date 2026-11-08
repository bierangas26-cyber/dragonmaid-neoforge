package com.example.dragonmaid.mixin;

import com.example.dragonmaid.accessory.MiniatureForm;
import com.example.dragonmaid.combat.MiniatureMelee;
import com.example.dragonmaid.combat.MiniaturePounce;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.FireDragonEntity;
import com.iafenvoy.iceandfire.entity.IceDragonEntity;
import com.iafenvoy.iceandfire.entity.LightningDragonEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({FireDragonEntity.class, IceDragonEntity.class, LightningDragonEntity.class})
abstract class MiniaturePounceVariantMixin {
   @Inject(
      method = {"aiStep"},
      at = {@At("TAIL")}
   )
   private void dragonmaid$capNativeTackleAcceleration(CallbackInfo ci) {
      MiniaturePounce.limitSpeed((DragonBaseEntity)(Object)this);
   }

   @Inject(
      method = {"doHurtTarget"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void dragonmaid$doNotBiteDuringTackle(Entity target, CallbackInfoReturnable<Boolean> cir) {
      DragonBaseEntity dragon = (DragonBaseEntity)(Object)this;
      if (MiniaturePounce.isPouncing(dragon) || MiniatureForm.hasDragonEffect(dragon) && !MiniatureMelee.inReach(dragon, target)) {
         cir.setReturnValue(false);
      }
   }

   @Redirect(
      method = {"aiStep"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/AABB;intersects(Lnet/minecraft/world/phys/AABB;)Z"
      )
   )
   private boolean dragonmaid$letNativeLogicResolveContact(AABB bounds, AABB target) {
      return !MiniaturePounce.isPouncing((DragonBaseEntity)(Object)this) && bounds.intersects(target);
   }
}
