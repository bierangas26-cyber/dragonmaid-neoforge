package com.example.dragonmaid.compat.net;

import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;

public final class NetworkRegistry {
    private NetworkRegistry() { }

    public static SimpleChannel newSimpleChannel(ResourceLocation name, Supplier<String> versionSupplier,
            Predicate<String> clientAccepted, Predicate<String> serverAccepted) {
        return new SimpleChannel();
    }
}
