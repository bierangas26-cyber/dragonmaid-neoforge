package com.example.dragonmaid.mixin;

import com.example.dragonmaid.accessory.MiniatureForm;
import com.example.dragonmaid.combat.MiniatureShoulder;
import com.example.dragonmaid.movement.MiniaturePerch;
import com.example.dragonmaid.movement.PerchFlight;
import com.iafenvoy.uranus.animation.Animation;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({DragonBaseEntity.class})
abstract class DragonShoulderMixin implements MiniatureShoulder.AimHolder {
   @Unique
   private int dragonmaid$perchTarget = -1;

   @Override
   public int dragonmaid$shoulderAim() {
      return this.dragonmaid$perchTarget;
   }

   @Override
   public void dragonmaid$shoulderAim(int id) {
      this.dragonmaid$perchTarget = id;
   }

   @Inject(
      method = {"rideTick"},
      at = {@At("HEAD")}
   )
   private void dragonmaid$endGroundAttack(CallbackInfo ci) {
      DragonBaseEntity dragon = (DragonBaseEntity)(Object)this;
      if (dragon.getVehicle() instanceof Player owner && PerchFlight.isCarrierDragon(owner, dragon)) {
         Animation attack = dragon.getAnimation();
         if (attack == DragonBaseEntity.ANIMATION_BITE
            || attack == DragonBaseEntity.ANIMATION_SHAKEPREY
            || attack == DragonBaseEntity.ANIMATION_WINGBLAST
            || attack == DragonBaseEntity.ANIMATION_TAILWHACK) {
            dragon.setAnimation(DragonBaseEntity.NO_ANIMATION);
            dragon.setAnimationTick(0);
         }

         if (dragon.isTackling()) {
            dragon.setTackling(false);
         }

         if (dragon.isHovering()) {
            dragon.setHovering(false);
         }

         if (dragon.isFlying()) {
            dragon.setFlying(false);
         }

         return;
      }
   }

   @Inject(
      method = {"updatePreyInMouth"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private void dragonmaid$releaseCarriedPrey(Entity prey, CallbackInfo ci) {
      DragonBaseEntity dragon = (DragonBaseEntity)(Object)this;
      if (dragon.getVehicle() instanceof Player owner && PerchFlight.isCarrierDragon(owner, dragon)) {
         if (!dragon.level().isClientSide && prey.getVehicle() == dragon) {
            prey.stopRiding();
         }

         ci.cancel();
      }
   }

   @Inject(
      method = {"updateRiding"},
      remap = false,
      cancellable = true,
      at = {@At(
         value = "INVOKE",
         target = "Lcom/iafenvoy/iceandfire/entity/DragonBaseEntity;getControlState()B"
      )}
   )
   private void dragonmaid$keepFlightPassengers(Entity vehicle, CallbackInfo ci) {
      DragonBaseEntity dragon = (DragonBaseEntity)(Object)this;
      if (vehicle instanceof Player player && PerchFlight.controlsDismount(player, dragon)) {
         MiniaturePerch.position(player, dragon);
         if (this.dragonmaid$perchTarget >= 0) {
            MiniatureShoulder.faceTarget(dragon);
         }

         ci.cancel();
      }
   }

   @Inject(
      method = {"updateRiding"},
      at = {@At("TAIL")},
      remap = false
   )
   private void dragonmaid$aimWhenCarried(Entity vehicle, CallbackInfo ci) {
      if (this.dragonmaid$perchTarget >= 0) {
         MiniatureShoulder.faceTarget((DragonBaseEntity)(Object)this);
      }
   }

   @Redirect(
      method = {"mobInteract"},
      at = @At(
         value = "INVOKE",
         remap = false,
         target = "Lcom/iafenvoy/iceandfire/entity/DragonBaseEntity;getDragonStage()I",
         ordinal = 2
      ),
      require = 1,
      allow = 1
   )
   private int dragonmaid$shoulderSize(DragonBaseEntity dragon) {
      return MiniatureForm.isShoulderMini(dragon) ? 1 : dragon.getDragonStage();
   }
}
