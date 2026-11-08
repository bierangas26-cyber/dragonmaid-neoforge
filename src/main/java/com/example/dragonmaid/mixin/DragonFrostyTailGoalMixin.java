package com.example.dragonmaid.mixin;

import com.example.dragonmaid.combat.NativeTailReach;
import com.example.dragonmaid.data.DragonForms;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.util.dragon.IafDragonAttacks.Ground;
import com.iafenvoy.iceandfire.entity.ai.DragonAIAttackMeleeGoal;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {DragonAIAttackMeleeGoal.class},
   remap = false
)
public abstract class DragonFrostyTailGoalMixin {
   @Shadow
   protected DragonBaseEntity dragon;

   @Inject(
      method = {"getAttackReachSqr"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void dragonmaid$frostyTailStartReach(LivingEntity target, CallbackInfoReturnable<Double> cir) {
      if (!DragonForms.isHumanoid(this.dragon) && NativeTailReach.enhanced(this.dragon) && this.dragon.groundAttack == Ground.TAIL_WHIP) {
         double original = (Double)cir.getReturnValue();
         double strikeRadius = NativeTailReach.radius(this.dragon);
         double extended = Math.min(original * 1.2 * 1.2, Math.max(original, strikeRadius * strikeRadius));
         cir.setReturnValue(extended);
      }
   }
}
