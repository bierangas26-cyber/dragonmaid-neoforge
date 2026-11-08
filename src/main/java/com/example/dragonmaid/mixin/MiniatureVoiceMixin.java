package com.example.dragonmaid.mixin;

import com.example.dragonmaid.accessory.MiniatureForm;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.FireDragonEntity;
import com.iafenvoy.iceandfire.entity.IceDragonEntity;
import com.iafenvoy.iceandfire.entity.LightningDragonEntity;
import com.iafenvoy.iceandfire.registry.IafSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({FireDragonEntity.class, IceDragonEntity.class, LightningDragonEntity.class})
abstract class MiniatureVoiceMixin {
   @Unique
   private SoundEvent dragonmaid$childVoice(int kind) {
      DragonBaseEntity dragon = (DragonBaseEntity)(Object)this;
      if (!MiniatureForm.hasDragonEffect(dragon)) {
         return null;
      } else if (dragon instanceof IceDragonEntity) {
         return switch (kind) {
            case 0 -> IafSounds.ICEDRAGON_CHILD_IDLE.get();
            case 1 -> IafSounds.ICEDRAGON_CHILD_HURT.get();
            case 2 -> IafSounds.ICEDRAGON_CHILD_DEATH.get();
            default -> IafSounds.ICEDRAGON_CHILD_ROAR.get();
         };
      } else if (dragon instanceof LightningDragonEntity) {
         return switch (kind) {
            case 0 -> IafSounds.LIGHTNINGDRAGON_CHILD_IDLE.get();
            case 1 -> IafSounds.LIGHTNINGDRAGON_CHILD_HURT.get();
            case 2 -> IafSounds.LIGHTNINGDRAGON_CHILD_DEATH.get();
            default -> IafSounds.LIGHTNINGDRAGON_CHILD_ROAR.get();
         };
      } else {
         return switch (kind) {
            case 0 -> IafSounds.FIREDRAGON_CHILD_IDLE.get();
            case 1 -> IafSounds.FIREDRAGON_CHILD_HURT.get();
            case 2 -> IafSounds.FIREDRAGON_CHILD_DEATH.get();
            default -> IafSounds.FIREDRAGON_CHILD_ROAR.get();
         };
      }
   }

   @Inject(
      method = {"getAmbientSound"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void dragonmaid$childIdle(CallbackInfoReturnable<SoundEvent> cir) {
      SoundEvent sound = this.dragonmaid$childVoice(0);
      if (sound != null) {
         cir.setReturnValue(sound);
      }
   }

   @Inject(
      method = {"getHurtSound"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void dragonmaid$childHurt(DamageSource source, CallbackInfoReturnable<SoundEvent> cir) {
      SoundEvent sound = this.dragonmaid$childVoice(1);
      if (sound != null) {
         cir.setReturnValue(sound);
      }
   }

   @Inject(
      method = {"getDeathSound"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void dragonmaid$childDeath(CallbackInfoReturnable<SoundEvent> cir) {
      SoundEvent sound = this.dragonmaid$childVoice(2);
      if (sound != null) {
         cir.setReturnValue(sound);
      }
   }

   @Inject(
      method = {"getRoarSound"},
      remap = false,
      at = {@At("HEAD")},
      cancellable = true
   )
   private void dragonmaid$childRoar(CallbackInfoReturnable<SoundEvent> cir) {
      SoundEvent sound = this.dragonmaid$childVoice(3);
      if (sound != null) {
         cir.setReturnValue(sound);
      }
   }
}
