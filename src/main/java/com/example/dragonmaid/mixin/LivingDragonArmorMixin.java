package com.example.dragonmaid.mixin;

import com.example.dragonmaid.combat.DragonArmor;
import com.example.dragonmaid.combat.DragonCombatAccessories;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.minecraft.world.damagesource.CombatRules;
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

    /** 1.2.7: 龙的总护甲值改成"先天 + 装备 + 饰品"合并计算（上限 40），所以直接接管 getArmorValue。 */
    @Inject(remap = false, method = {"getArmorValue"}, at = {@At("HEAD")}, cancellable = true)
    private void dragonmaid$totalArmor(CallbackInfoReturnable<Integer> cir) {
        if ((Object)this instanceof DragonBaseEntity dragon && dragon.isTame()) {
            cir.setReturnValue(DragonArmor.totalArmor(dragon));
        }
    }

    @Inject(remap = false, method = {"getDamageAfterArmorAbsorb"},
        at = {@At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/damagesource/CombatRules;getDamageAfterAbsorb(Lnet/minecraft/world/entity/LivingEntity;FLnet/minecraft/world/damagesource/DamageSource;FF)F"
        )},
        cancellable = true)
    private void dragonmaid$dragonArmor(DamageSource source, float damage, CallbackInfoReturnable<Float> cir) {
        LivingEntity victim = (LivingEntity)(Object)this;
        if (victim instanceof DragonBaseEntity entitydragonbase && entitydragonbase.isTame()) {
            float f = (float)entitydragonbase.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
            cir.setReturnValue(DragonArmor.afterArmor(damage, DragonArmor.armorForDamage(entitydragonbase), f));
            return;
        }

        // 1.2.7: 火龙斧砍人时忽略 40% 护甲（原版 CombatRules 在 1.21.1 只剩 5 参重载）。
        if (DragonCombatAccessories.isFireAxeHit(victim, source)) {
            float armor = (float)victim.getArmorValue() * 0.6F;
            float toughness = (float)victim.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
            cir.setReturnValue(CombatRules.getDamageAfterAbsorb(victim, damage, source, armor, toughness));
        }
    }
}
