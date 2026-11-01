package com.example.dragonmaid.mixin.client;

import com.example.dragonmaid.combat.DragonArmor;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(remap = false, targets = {"snownee.jade.addon.vanilla.EntityArmorProvider"})
public abstract class JadeDragonArmorMixin {
    public JadeDragonArmorMixin() {
    }

    @Redirect(remap = false, method = {"appendTooltip"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getArmorValue()I"
            ),
        require = 0)
    private int dragonmaid$showDragonArmor(LivingEntity entity) {
        if (entity instanceof DragonBaseEntity entitydragonbase && entitydragonbase.isTame()) {
            return (int)DragonArmor.rawArmor(entitydragonbase);
        }

        return entity.getArmorValue();
    }
}
