package com.example.dragonmaid.compat;

import java.util.function.Supplier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

/** Client-only execution helper standing in for Forge's DistExecutor. */
public final class DistExecutor {
    private DistExecutor() { }

    public static void unsafeRunWhenOn(Dist dist, Supplier<Runnable> runnable) {
        if (FMLEnvironment.dist == dist) {
            runnable.get().run();
        }
    }

    public static void runWhenOn(Dist dist, Supplier<Runnable> runnable) {
        unsafeRunWhenOn(dist, runnable);
    }
}
