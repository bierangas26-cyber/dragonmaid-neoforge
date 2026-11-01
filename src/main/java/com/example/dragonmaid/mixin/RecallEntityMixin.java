package com.example.dragonmaid.mixin;

import com.example.dragonmaid.recall.RecallTracking;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(remap = false, value = {Entity.class})
public abstract class RecallEntityMixin {
    @Unique
    private long dragonmaid$nextVoidAttempt;

    public RecallEntityMixin() {
    }

    @Inject(remap = false, method = {"setCustomName"},
        at = {@At("TAIL")})
    private void dragonmaid$recallName(Component name, CallbackInfo ci) {
        if (((Object)this) instanceof DragonBaseEntity entitydragonbase) {
            RecallTracking.changed(entitydragonbase);
        }
    }

    @Inject(remap = false, method = {"checkBelowWorld"},
        at = {@At("HEAD")},
        cancellable = true)
    private void dragonmaid$escrowVoidDragon(CallbackInfo ci) {
        if (((Object)this) instanceof DragonBaseEntity entitydragonbase
            && !entitydragonbase.level().isClientSide
            && entitydragonbase.getY() < (double)(entitydragonbase.level().getMinBuildHeight() - 64)) {
            long i = entitydragonbase.level().getGameTime();
            if (i < this.dragonmaid$nextVoidAttempt) {
                ci.cancel();
                return;
            }

            try {
                if (RecallTracking.rescueVoid(entitydragonbase)) {
                    ci.cancel();
                }
            } catch (RuntimeException runtimeexception) {
                this.dragonmaid$nextVoidAttempt = i + 100L;
                LoggerFactory.getLogger("DragonMaid/Recall").error("Could not escrow void dragon; original retained", runtimeexception);
                ci.cancel();
            }
        }
    }
}
