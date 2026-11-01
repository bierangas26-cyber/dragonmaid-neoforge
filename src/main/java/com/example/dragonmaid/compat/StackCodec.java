package com.example.dragonmaid.compat;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/** ItemStack NBT (de)serialisation for the 1.21.1 registry-aware codec API. */
public final class StackCodec {
    private StackCodec() { }

    public static HolderLookup.Provider provider() {
        var server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? RegistryAccess.EMPTY : server.registryAccess();
    }

    public static CompoundTag save(ItemStack stack, CompoundTag tag) {
        Tag saved = stack.save(provider(), tag);
        return saved instanceof CompoundTag compound ? compound : tag;
    }

    public static ItemStack load(CompoundTag tag) {
        return ItemStack.parseOptional(provider(), tag);
    }
}
