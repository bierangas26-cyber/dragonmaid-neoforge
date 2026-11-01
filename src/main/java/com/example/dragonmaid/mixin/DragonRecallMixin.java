package com.example.dragonmaid.mixin;

import com.example.dragonmaid.recall.RecallTracking;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(remap = false, value = {DragonBaseEntity.class})
public abstract class DragonRecallMixin {
    public DragonRecallMixin() {
    }

    @Inject(remap = false, method = {"growDragon"},
        at = {@At("TAIL")})
    private void dragonmaid$recallGrowth(int days, CallbackInfo ci) {
        RecallTracking.changed((DragonBaseEntity)(Object)this);
    }

    @Inject(remap = false, method = {"setModelDead"},
        at = {@At("TAIL")})
    private void dragonmaid$recallInjury(boolean dead, CallbackInfo ci) {
        RecallTracking.changed((DragonBaseEntity)(Object)this);
    }

    @Inject(remap = false, method = {"addAdditionalSaveData"},
        at = {@At("TAIL")})
    private void dragonmaid$recallSave(CompoundTag tag, CallbackInfo ci) {
        RecallTracking.observe((DragonBaseEntity)(Object)this);
    }
}
