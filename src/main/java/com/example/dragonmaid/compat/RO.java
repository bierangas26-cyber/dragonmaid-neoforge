package com.example.dragonmaid.compat;

import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class RO<T> implements Supplier<T> {
    private final DeferredHolder<?, ?> holder;

    public RO(DeferredHolder<?, ?> holder) { this.holder = holder; }

    @Override
    @SuppressWarnings("unchecked")
    public T get() { return (T) ((Supplier<?>) holder).get(); }

    public ResourceLocation getId() { return holder.getId(); }

    public boolean isPresent() { return holder.isBound(); }
}
