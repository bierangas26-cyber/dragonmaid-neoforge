package com.example.dragonmaid.compat;

import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class Reg<T> {
    private final DeferredRegister<T> delegate;

    private Reg(DeferredRegister<T> delegate) { this.delegate = delegate; }

    public static <T> Reg<T> create(Registry<T> registry, String modid) {
        return new Reg<>(DeferredRegister.create(registry, modid));
    }

    public static <T> Reg<T> create(ResourceKey<Registry<T>> key, String modid) {
        return new Reg<>(DeferredRegister.create(key, modid));
    }

    public <V extends T> RO<V> register(String name, Supplier<? extends V> supplier) {
        DeferredHolder<T, V> holder = delegate.register(name, supplier);
        return new RO<>(holder);
    }

    public void register(IEventBus bus) { delegate.register(bus); }
}
