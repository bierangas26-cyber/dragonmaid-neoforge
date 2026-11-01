package com.example.dragonmaid.mixin;

import com.example.dragonmaid.recall.RecallTracking;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.util.UUID;
import net.minecraft.world.entity.TamableAnimal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(remap = false, value = {TamableAnimal.class})
public abstract class RecallTameMixin {
    public RecallTameMixin() {
    }

    @Inject(remap = false, method = {"setOwnerUUID"},
        at = {@At("TAIL")})
    private void dragonmaid$recallOwner(UUID owner, CallbackInfo ci) {
        if (((Object)this) instanceof DragonBaseEntity entitydragonbase) {
            RecallTracking.changed(entitydragonbase);
        }
    }
}
