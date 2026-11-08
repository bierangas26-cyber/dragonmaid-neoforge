package com.example.dragonmaid.mixin;

import com.example.dragonmaid.data.DragonEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Player.class})
public abstract class PlayerAbsorptionAlertMixin {
   @Unique
   private float dragonmaid$absorptionBeforeHit;

   @Inject(
      method = {"actuallyHurt"},
      at = {@At("HEAD")}
   )
   private void dragonmaid$beforeHit(DamageSource source, float amount, CallbackInfo ci) {
      this.dragonmaid$absorptionBeforeHit = ((Player)(Object)this).getAbsorptionAmount();
   }

   @Inject(
      method = {"actuallyHurt"},
      at = {@At("RETURN")}
   )
   private void dragonmaid$absorbedHit(DamageSource source, float amount, CallbackInfo ci) {
      Player player = (Player)(Object)this;
      if (!player.level().isClientSide && amount > 0.0F && player.getAbsorptionAmount() < this.dragonmaid$absorptionBeforeHit) {
         DragonEvents.recordOwnerDamage(player, source);
      }
   }
}
