package com.example.dragonmaid.mixin;

import com.example.dragonmaid.accessory.MiniatureForm;
import com.example.dragonmaid.combat.MiniaturePounce;
import com.example.dragonmaid.config.CompanionBalanceConfig;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.util.dragon.IafDragonLogic;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({IafDragonLogic.class})
abstract class MiniaturePounceLogicMixin {
   @Shadow(
      remap = false
   )
   @Final
   private DragonBaseEntity dragon;

   @Redirect(
      method = {"updateDragonAttack"},
      remap = false,
      at = @At(
         value = "INVOKE",
         remap = true,
         target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V"
      )
   )
   private void dragonmaid$smallWingAndTailKnockback(LivingEntity victim, double strength, double x, double z) {
      victim.knockback(MiniatureForm.hasDragonEffect(this.dragon) ? Math.min(strength, CompanionBalanceConfig.values().knockbackCap()) : strength, x, z);
   }

   @Redirect(
      method = {"updateDragonServer"},
      remap = false,
      at = @At(
         value = "INVOKE",
         remap = true,
         target = "Lnet/minecraft/world/phys/AABB;expandTowards(DDD)Lnet/minecraft/world/phys/AABB;"
      )
   )
   private AABB dragonmaid$smallTackleContact(AABB bounds, double x, double y, double z) {
      return MiniaturePounce.isPouncing(this.dragon) ? bounds.inflate(0.3) : bounds.expandTowards(x, y, z);
   }

   @Redirect(
      method = {"updateDragonServer"},
      remap = false,
      at = @At(
         value = "INVOKE",
         target = "Lcom/iafenvoy/iceandfire/entity/util/dragon/IafDragonLogic;attackTarget(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/player/Player;F)Z"
      )
   )
   private boolean dragonmaid$matureTackleDamage(IafDragonLogic logic, Entity target, Player rider, float nativeDamage) {
      float damage = MiniaturePounce.isPouncing(this.dragon) ? (float)this.dragon.getAttributeValue(Attributes.ATTACK_DAMAGE) : nativeDamage;
      return logic.attackTarget(target, rider, damage);
   }
}
