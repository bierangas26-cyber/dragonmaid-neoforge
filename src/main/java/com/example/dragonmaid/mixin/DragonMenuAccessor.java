package com.example.dragonmaid.mixin;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.screen.menu.DragonMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(remap = false, value = {DragonMenu.class})
public interface DragonMenuAccessor {
    @Accessor(remap = false, value = "dragon")
    DragonBaseEntity dragonmaid$getDragon();
}
