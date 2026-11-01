package com.example.dragonmaid.accessory;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface DragonAccessoryAdapter {
    @Nullable
    DragonAccessoryBehavior resolve(ItemStack var1);
}
