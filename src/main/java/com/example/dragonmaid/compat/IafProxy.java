package com.example.dragonmaid.compat;

/** Stand-in for IceAndFire.PROXY helpers that no longer exist in IAF CE. */
public final class IafProxy {
    private IafProxy() { }

    /** The dragon previewed in the dragon-details GUI. Not tracked by IAF CE, so unknown here. */
    public static net.minecraft.world.entity.Entity getReferencedMob() {
        return null;
    }

    public static void dropMessage(Object message) {
        // Dragon-forge sync packets are no longer routed through IAF; intentionally dropped.
    }
}
