package com.example.dragonmaid.mixin;

import com.example.dragonmaid.compat.ForgeCooperation;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.util.dragon.IafDragonDestructionManager;
import com.iafenvoy.iceandfire.item.block.entity.DragonForgeInputBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(remap = false, value = {IafDragonDestructionManager.class})
public abstract class DragonForgeBreathMixin {
    public DragonForgeBreathMixin() {
    }

    @Redirect(remap = false, method = {"lambda$destroyAreaBreath$0"},
        at = @At(
            value = "INVOKE",
            target = "Lcom/iafenvoy/iceandfire/item/block/entity/DragonForgeInputBlockEntity;onHitWithFlame(Lnet/minecraft/world/entity/LivingEntity;)V"
            ))
    private static void dragonmaid$youngForgeHit(DragonForgeInputBlockEntity input, Level level, boolean canBreakBlocks, DragonBaseEntity dragon, BlockPos pos) {
        ForgeCooperation.fuelNative(input, dragon);
    }

    @Redirect(remap = false, method = {"lambda$destroyAreaBreath$1"},
        at = @At(
            value = "INVOKE",
            target = "Lcom/iafenvoy/iceandfire/item/block/entity/DragonForgeInputBlockEntity;onHitWithFlame(Lnet/minecraft/world/entity/LivingEntity;)V"
            ))
    private static void dragonmaid$adultForgeHit(
        DragonForgeInputBlockEntity input, Level level, boolean canBreakBlocks, BlockPos center, float radiusSquared, DragonBaseEntity dragon, BlockPos pos
    ) {
        ForgeCooperation.fuelNative(input, dragon);
    }
}
