package com.example.dragonmaid.compat;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class StackNbt {
    private StackNbt() { }

    public static CompoundTag getTag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    public static CompoundTag getOrCreateTag(ItemStack stack) { return new WriteBackTag(stack); }

    public static boolean hasTag(ItemStack stack) { return stack.has(DataComponents.CUSTOM_DATA); }

    public static void setTag(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static boolean isSameItemSameTags(ItemStack a, ItemStack b) {
        return ItemStack.isSameItemSameComponents(a, b);
    }

    public static CompoundTag getTagElement(ItemStack stack, String key) {
        CompoundTag tag = getTag(stack);
        return tag.contains(key, 10) ? tag.getCompound(key) : null;
    }
}
