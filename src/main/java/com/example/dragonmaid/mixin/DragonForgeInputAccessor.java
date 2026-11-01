package com.example.dragonmaid.mixin;

import com.iafenvoy.iceandfire.item.block.entity.DragonForgeBlockEntity;
import com.iafenvoy.iceandfire.item.block.entity.DragonForgeInputBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(remap = false, value = {DragonForgeInputBlockEntity.class})
public interface DragonForgeInputAccessor {
    @Accessor(remap = false, value = "core")
    DragonForgeBlockEntity dragonmaid$getForgeCore();
}
