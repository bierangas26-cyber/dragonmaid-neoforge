package com.example.dragonmaid.mixin;

import com.example.dragonmaid.accessory.DragonAccessories;
import com.example.dragonmaid.accessory.DragonAccessoryBehavior;
import com.example.dragonmaid.data.DragonForms;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({DragonBaseEntity.class})
public abstract class DragonRoarAccessoryMixin {
   @Redirect(
      method = {"roar"},
      remap = false,
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/LivingEntity;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z",
         remap = true
      )
   )
   private boolean dragonmaid$enhanceNativeRoar(LivingEntity target, MobEffectInstance original) {
      DragonBaseEntity dragon = (DragonBaseEntity)(Object)this;
      if (!dragon.level().isClientSide
         && !DragonForms.isHumanoid(dragon)
         && (original.getEffect() == MobEffects.DAMAGE_BOOST || original.getEffect() == MobEffects.WEAKNESS)
         && DragonAccessories.hasEffect(dragon, DragonAccessoryBehavior.ROAR_MAJESTY)) {
         original = new MobEffectInstance(
            original.getEffect(), Math.round((float)original.getDuration() * 1.25F), 1, original.isAmbient(), original.isVisible(), original.showIcon()
         );
      }

      return target.addEffect(original);
   }
}
