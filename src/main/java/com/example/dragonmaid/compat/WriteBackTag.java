package com.example.dragonmaid.compat;

import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public class WriteBackTag extends CompoundTag {
    private final ItemStack stack;
    private boolean flushing;

    public WriteBackTag(ItemStack stack) {
        super();
        this.stack = stack;
        super.merge(StackNbt.getTag(stack));
    }

    private void flush() {
        if (!this.flushing) {
            this.flushing = true;
            try {
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(this));
            } finally {
                this.flushing = false;
            }
        }
    }

    @Override public Tag put(String key, Tag value) { Tag t = super.put(key, value); flush(); return t; }
    @Override public void putByte(String key, byte value) { super.putByte(key, value); flush(); }
    @Override public void putShort(String key, short value) { super.putShort(key, value); flush(); }
    @Override public void putInt(String key, int value) { super.putInt(key, value); flush(); }
    @Override public void putLong(String key, long value) { super.putLong(key, value); flush(); }
    @Override public void putFloat(String key, float value) { super.putFloat(key, value); flush(); }
    @Override public void putDouble(String key, double value) { super.putDouble(key, value); flush(); }
    @Override public void putString(String key, String value) { super.putString(key, value); flush(); }
    @Override public void putBoolean(String key, boolean value) { super.putBoolean(key, value); flush(); }
    @Override public void putUUID(String key, UUID value) { super.putUUID(key, value); flush(); }
    @Override public void putByteArray(String key, byte[] value) { super.putByteArray(key, value); flush(); }
    @Override public void putIntArray(String key, int[] value) { super.putIntArray(key, value); flush(); }
    @Override public void putLongArray(String key, long[] value) { super.putLongArray(key, value); flush(); }
    @Override public void remove(String key) { super.remove(key); flush(); }
}
