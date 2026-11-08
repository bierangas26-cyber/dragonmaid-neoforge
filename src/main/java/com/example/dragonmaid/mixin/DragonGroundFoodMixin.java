package com.example.dragonmaid.mixin;

import com.example.dragonmaid.accessory.DragonStorage;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.ai.DragonAITargetItemsGoal;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({DragonAITargetItemsGoal.class})
public abstract class DragonGroundFoodMixin extends TargetGoal {
   protected DragonGroundFoodMixin(Mob mob, boolean mustSee) {
      super(mob, mustSee);
   }

   @Inject(
      method = {"canUse", "canContinueToUse"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void dragonmaid$useBackpackPolicy(CallbackInfoReturnable<Boolean> result) {
      if (this.mob instanceof DragonBaseEntity dragon && dragon.isTame() && dragon.getOwnerUUID() != null && DragonStorage.unlockedSlots(dragon) > 0) {
         result.setReturnValue(false);
      }
   }
}
