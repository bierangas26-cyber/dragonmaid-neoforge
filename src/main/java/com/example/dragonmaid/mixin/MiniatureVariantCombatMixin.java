package com.example.dragonmaid.mixin;

import com.example.dragonmaid.accessory.MiniatureForm;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.FireDragonEntity;
import com.iafenvoy.iceandfire.entity.LightningDragonEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 迷你龙（成熟迷你形态）的原生吐息/骑手吐息替换。
 *
 * <p>IAF CE 2.2 里只有 Fire/Lightning 声明了 shootFireAtMob（Ice 是 shootIceAtMob，
 * 见 MiniatureIceCombatMixin），所以这里按 CE 的方法分布拆分注入目标，避免 Mixin 找不到方法。</p>
 */
@Mixin({FireDragonEntity.class, LightningDragonEntity.class})
public abstract class MiniatureVariantCombatMixin {
   @Inject(
      method = {"shootFireAtMob"},
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
