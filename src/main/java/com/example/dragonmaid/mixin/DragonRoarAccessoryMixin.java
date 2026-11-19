package com.example.dragonmaid.mixin;

import com.example.dragonmaid.accessory.DragonAccessories;
import com.example.dragonmaid.accessory.DragonAccessoryBehavior;
import com.example.dragonmaid.combat.HumanoidRoar;
import com.example.dragonmaid.data.DragonForms;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({DragonBaseEntity.class})
public abstract class DragonRoarAccessoryMixin {
   /**
    * 1.2.7: 雷电龙（非人形、已驯服、知识足够）的原版咆哮附带回击一次；由 {@link HumanoidRoar#onNativeRoar} 自行限频。
    * 1.21.1 的 IAF CE 2.2 仍把动画设在 {@code DragonBaseEntity#setAnimation(com.iafenvoy.uranus.animation.Animation)}。
    */
   @Inject(
      method = {"roar"},
      remap = false,
      at = {@At(
         value = "INVOKE",
         shift = Shift.AFTER,
         target = "Lcom/iafenvoy/iceandfire/entity/DragonBaseEntity;setAnimation(Lcom/iafenvoy/uranus/animation/Animation;)V"
      )}
   )
   private void dragonmaid$lightningCombatRoar(CallbackInfo ci) {
      HumanoidRoar.onNativeRoar((DragonBaseEntity)(Object)this);
   }

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
