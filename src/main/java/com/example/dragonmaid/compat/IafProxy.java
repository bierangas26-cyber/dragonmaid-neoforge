package com.example.dragonmaid.compat;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.screen.menu.DragonMenu;
import net.minecraft.world.entity.Entity;

/** Helpers replacing IceAndFire.PROXY calls that no longer exist in IAF CE. */
public final class IafProxy {
    /**
     * The dragon currently previewed by the dragon-details GUI.
     * IAF CE exposes it through {@link DragonMenu#getDragon()} (1.20's IAF.PROXY field is gone),
     * so {@code ClientEvents} publishes the resolved dragon here every client tick.
     */
    private static volatile DragonBaseEntity referencedMob;

    private IafProxy() { }

    /** Last dragon resolved by the client GUI hooks (may be null). */
    public static Entity getReferencedMob() {
        return referencedMob;
    }

    public static void setReferencedMob(DragonBaseEntity dragon) {
        referencedMob = dragon;
    }

    /** Dragon carried by the opened IAF dragon menu (1.21 IAF CE API). */
    public static DragonBaseEntity getMenuDragon(DragonMenu menu) {
        return menu == null ? null : menu.getDragon();
    }

    public static void clearReferencedMob() {
        referencedMob = null;
    }

    public static void dropMessage(Object message) {
        // Dragon-forge sync packets are no longer routed through IAF; intentionally dropped.
    }
}
