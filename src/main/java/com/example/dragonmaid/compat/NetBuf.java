package com.example.dragonmaid.compat;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Component (de)serialisation helpers replacing the removed FriendlyByteBuf#writeComponent. */
public final class NetBuf {
    private NetBuf() { }

    public static void writeComponent(FriendlyByteBuf buf, Component component) {
        buf.writeUtf(Component.Serializer.toJson(component, RegistryAccess.EMPTY));
    }

    public static MutableComponent readComponent(FriendlyByteBuf buf) {
        MutableComponent component = Component.Serializer.fromJson(buf.readUtf(), RegistryAccess.EMPTY);
        return component == null ? Component.empty() : component;
    }
}
