package com.example.dragonmaid.mixin;

import com.example.dragonmaid.petrification.DragonPetrification;
import com.iafenvoy.iceandfire.entity.GorgonEntity;
import com.iafenvoy.iceandfire.entity.StoneStatueEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(remap = false, value = {GorgonEntity.class})
public abstract class GorgonPetrificationMixin {
    public GorgonPetrificationMixin() {
    }

    @Redirect(remap = false, method = {"aiStep"},
        at = @At(
            value = "INVOKE",
            target = "Lcom/iafenvoy/iceandfire/entity/StoneStatueEntity;buildStatueEntity(Lnet/minecraft/world/entity/LivingEntity;)Lcom/iafenvoy/iceandfire/entity/StoneStatueEntity;"
        ))
    private StoneStatueEntity dragonmaid$preserveDragonSnapshot(LivingEntity target) {
        return DragonPetrification.buildStatue(target);
    }

    @Redirect(remap = false, method = {"aiStep"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"
        ))
    private boolean dragonmaid$commitDragonStatue(Level level, Entity statue) {
        return DragonPetrification.addStatue(level, statue);
    }

    @Redirect(remap = false, method = {"aiStep"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;remove(Lnet/minecraft/world/entity/Entity$RemovalReason;)V"
        ))
    private void dragonmaid$deferDragonRemoval(LivingEntity target, RemovalReason reason) {
        if (!DragonPetrification.protects(target)) {
            target.remove(reason);
        }
    }
}
