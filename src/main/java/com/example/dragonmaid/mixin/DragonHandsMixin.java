package com.example.dragonmaid.mixin;

import com.example.dragonmaid.accessory.DragonHands;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.2: 给龙挂上双手容器（DragonHands），随实体 NBT 存档。
 * 上游 1.2 同名 mixin；移植时只需把注入的方法目标名保持 1.21.1 官方名（remap = false 下即运行时名）。
 */
@Mixin(remap = false, value = {DragonBaseEntity.class})
public abstract class DragonHandsMixin implements DragonHands.Holder {
    @Unique
    private final DragonHands dragonmaid$hands = new DragonHands((DragonBaseEntity)(Object)this);

    @Override
    public DragonHands dragonmaid$getHands() {
        return this.dragonmaid$hands;
    }

    @Inject(remap = false, method = {"addAdditionalSaveData"}, at = {@At("TAIL")})
    private void dragonmaid$writeHands(CompoundTag tag, CallbackInfo ci) {
        if (this.dragonmaid$hands.isEmpty()) {
            tag.remove("DragonMaidHands");
        } else {
            tag.put("DragonMaidHands", this.dragonmaid$hands.saveHands());
        }
    }

    @Inject(remap = false, method = {"readAdditionalSaveData"}, at = {@At("TAIL")})
    private void dragonmaid$readHands(CompoundTag tag, CallbackInfo ci) {
        this.dragonmaid$hands.loadHands(tag.getCompound("DragonMaidHands"));
    }
}
