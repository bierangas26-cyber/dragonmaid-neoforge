package com.example.dragonmaid.mixin;

import com.example.dragonmaid.combat.DragonArmor;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(remap = false, value = {LivingEntity.class})
public abstract class LivingDragonArmorMixin {
    public LivingDragonArmorMixin() {
    }

    @Inject(remap = false, method = {"getDamageAfterArmorAbsorb"},
        at = {@At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/damagesource/CombatRules;getDamageAfterAbsorb(Lnet/minecraft/world/entity/LivingEntity;FLnet/minecraft/world/damagesource/DamageSource;FF)F"
        )},
        cancellable = true)
    private void dragonmaid$dragonArmor(DamageSource source, float damage, CallbackInfoReturnable<Float> cir) {
        if (((Object)this) instanceof DragonBaseEntity entitydragonbase && entitydragonbase.isTame()) {
            float f = (float)entitydragonbase.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
            cir.setReturnValue(DragonArmor.afterArmor(damage, (float)((int)DragonArmor.rawArmor(entitydragonbase)), f));
        }
    }
}
