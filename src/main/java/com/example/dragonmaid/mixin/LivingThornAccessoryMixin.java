package com.example.dragonmaid.mixin;

import com.example.dragonmaid.accessory.DragonAccessories;
import com.example.dragonmaid.accessory.DragonAccessoryBehavior;
import com.example.dragonmaid.combat.DragonCombatAccessories;
import com.example.dragonmaid.combat.DragonThorns;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.util.ArrayDeque;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LivingEntity.class})
public abstract class LivingThornAccessoryMixin {
   @Unique
   private ArrayDeque<float[]> dragonmaid$damageSnapshots;

   @Inject(
      method = {"actuallyHurt"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/LivingEntity;setHealth(F)V"
      )}
   )
   private void dragonmaid$captureActualLoss(DamageSource source, float amount, CallbackInfo ci) {
      LivingEntity living = (LivingEntity)(Object)this;
      boolean relevant = living instanceof DragonBaseEntity || source.getEntity() instanceof DragonBaseEntity || source.getDirectEntity() instanceof DragonBaseEntity;
      if (relevant || this.dragonmaid$damageSnapshots != null && !this.dragonmaid$damageSnapshots.isEmpty()) {
         boolean var10000;
         label35: {
            if (living instanceof DragonBaseEntity dragon && DragonAccessories.hasEffect(dragon, DragonAccessoryBehavior.THORNS)) {
               var10000 = true;
               break label35;
            }

            var10000 = false;
         }

         boolean reflect = var10000;
         if (this.dragonmaid$damageSnapshots == null) {
            this.dragonmaid$damageSnapshots = new ArrayDeque<>();
         }

         this.dragonmaid$damageSnapshots.push(new float[]{living.getHealth(), reflect ? 1.0F : 0.0F, relevant ? 1.0F : 0.0F});
      }
   }

   @Inject(
      method = {"actuallyHurt"},
      at = {@At(
         value = "INVOKE",
         shift = Shift.AFTER,
         target = "Lnet/minecraft/world/entity/LivingEntity;setHealth(F)V"
      )}
   )
   private void dragonmaid$applyActualLoss(DamageSource source, float amount, CallbackInfo ci) {
      if (this.dragonmaid$damageSnapshots != null && !this.dragonmaid$damageSnapshots.isEmpty()) {
         LivingEntity living = (LivingEntity)(Object)this;
         float[] snapshot = this.dragonmaid$damageSnapshots.pop();
         if (snapshot[2] != 0.0F) {
            DragonCombatAccessories.onActualHealthLoss(living, source, snapshot[0]);
            if (snapshot[1] != 0.0F) {
               DragonThorns.onHealthLoss((DragonBaseEntity)living, source, snapshot[0]);
            }
         }
      }
   }
}
