package com.example.dragonmaid.mixin;

import com.example.dragonmaid.combat.DragonCombatAccessories;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({DragonBaseEntity.class})
public abstract class DragonPreyAccessoryMixin {
   @Redirect(
      method = {"updatePreyInMouth"},
      remap = false,
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
         remap = true
      )
   )
   private boolean dragonmaid$preyMelee(Entity victim, DamageSource source, float amount) {
      return DragonCombatAccessories.melee((DragonBaseEntity)(Object)this, victim, source, amount, false);
   }
}
