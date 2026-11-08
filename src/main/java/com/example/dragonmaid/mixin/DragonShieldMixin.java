package com.example.dragonmaid.mixin;

import com.example.dragonmaid.accessory.DragonHands;
import com.example.dragonmaid.combat.DragonShield;
import com.example.dragonmaid.data.DragonForms;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({DragonBaseEntity.class})
public abstract class DragonShieldMixin extends TamableAnimal implements DragonShield.Holder {
   @Unique
   private final DragonShield dragonmaid$guard = new DragonShield();

   protected DragonShieldMixin(EntityType<? extends TamableAnimal> type, Level level) {
      super(type, level);
   }

   @Unique
   private DragonBaseEntity dragonmaid$selfForShield() {
      return (DragonBaseEntity)(Object)this;
   }

   @Override
   public DragonShield dragonmaid$shield() {
      return this.dragonmaid$guard;
   }

   @Inject(
      method = {"tick"},
      at = {@At("HEAD")}
   )
   private void dragonmaid$interruptShield(CallbackInfo ci) {
      this.dragonmaid$guard.releaseIfInterrupted(this.dragonmaid$selfForShield());
   }

   public ItemStack getItemInHand(InteractionHand hand) {
      return hand == InteractionHand.OFF_HAND && this.dragonmaid$guard != null && this.dragonmaid$guard.usesOffhand(this.dragonmaid$selfForShield())
         ? DragonHands.offHand(this.dragonmaid$selfForShield())
         : super.getItemInHand(hand);
   }

   public void setItemInHand(InteractionHand hand, ItemStack stack) {
      if (hand == InteractionHand.OFF_HAND && this.dragonmaid$guard != null && this.dragonmaid$guard.usesOffhand(this.dragonmaid$selfForShield())) {
         DragonHands.inventory(this.dragonmaid$selfForShield()).setItem(1, stack);
      } else {
         super.setItemInHand(hand, stack);
      }
   }

   public ItemStack getUseItem() {
      return this.level().isClientSide && DragonShield.isRaised(this.dragonmaid$selfForShield())
         ? DragonHands.offHand(this.dragonmaid$selfForShield())
         : super.getUseItem();
   }

   public boolean isBlocking() {
      if (this.dragonmaid$guard != null && this.dragonmaid$guard.guarding()) {
         DragonBaseEntity dragon = this.dragonmaid$selfForShield();
         return DragonShield.isRaised(dragon)
            && DragonHands.offHand(dragon) == this.getUseItem()
            && dragon.getCommand() != 1
            && !dragon.isSleeping()
            && !dragon.isPassenger()
            && this.dragonmaid$guard.pastWindup(dragon);
      } else {
         return super.isBlocking();
      }
   }

   protected void hurtCurrentlyUsedShield(float amount) {
      DragonBaseEntity dragon = this.dragonmaid$selfForShield();
      if (!this.dragonmaid$guard.guarding() || !DragonShield.isRaised(dragon)) {
         super.hurtCurrentlyUsedShield(amount);
      } else if (!(amount < 3.0F)) {
         ItemStack shield = DragonHands.offHand(dragon);
         shield.hurtAndBreak(1 + Mth.floor(amount), (net.minecraft.server.level.ServerLevel)dragon.level(), dragon, item -> dragon.playSound(SoundEvents.SHIELD_BREAK, 0.8F, 0.8F + dragon.getRandom().nextFloat() * 0.4F));
         if (shield.isEmpty()) {
            this.dragonmaid$guard.lower(dragon);
         }

         DragonHands.inventory(dragon).setChanged();
      }
   }

   protected void blockUsingShield(LivingEntity attacker) {
      super.blockUsingShield(attacker);
      DragonBaseEntity dragon = this.dragonmaid$selfForShield();
      if (this.dragonmaid$guard.guarding()) {
         ItemStack var10000;
         label19: {
            if (attacker instanceof DragonBaseEntity other && DragonForms.isHumanoid(other)) {
               var10000 = DragonHands.mainHand(other);
               break label19;
            }

            var10000 = attacker.getMainHandItem();
         }

         ItemStack weapon = var10000;
         if (attacker.canDisableShield()) {
            DragonShield.disable(dragon);
         }
      }
   }
}
