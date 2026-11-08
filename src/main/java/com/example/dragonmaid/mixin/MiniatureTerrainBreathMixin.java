package com.example.dragonmaid.mixin;

import com.example.dragonmaid.accessory.MiniatureForm;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 迷你龙不在地形上留下燃烧痕迹。
 *
 * <p>IAF CE 2.2 删除了 1.20.1 的 stimulateFire(x,y,z,sync)；S2C 的 DragonSetBurnBlockS2CPayload
 * 现在只设置 burningTarget/setBreathingFire，实际燃烧发生在 DragonBaseEntity#updateBurnTarget()。
 * 因此这里注入基类的 updateBurnTarget（基类声明，三个龙种子类都适用）。</p>
 */
@Mixin({DragonBaseEntity.class})
public abstract class MiniatureTerrainBreathMixin {
   @Inject(
      method = {"updateBurnTarget"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void dragonmaid$stopTerrainBreath(CallbackInfo ci) {
      if (MiniatureForm.isMatureMini((DragonBaseEntity)(Object)this)) {
         ci.cancel();
      }
   }
}
