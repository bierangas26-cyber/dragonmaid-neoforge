package com.example.dragonmaid;

import java.util.Set;
import net.minecraft.world.item.Item;

/**
 * 1.2 port: the upstream mod no longer registers the template {@code Config} spec.
 * Gameplay/recall values now live in {@code DragonMaidConfig.SPEC} (server-synced, file
 * {@code dragonmaid-sync-server.toml}) plus the editable mirrors in {@code GlobalBalanceConfig}
 * ({@code dragonmaid-common.toml} / {@code dragonmaid-recall-common.toml}).
 *
 * <p>This class is kept as a harmless holder for the bootstrap logging values so the
 * {@code DragonMaid} startup log keeps working. Do not register a spec here: registering a second
 * COMMON spec under the same file name caused a hard "config file conflict" crash on startup.</p>
 */
public final class Config {
    public static boolean logDirtBlock = true;
    public static int magicNumber = 42;
    public static String magicNumberIntroduction = "The magic number is... ";
    public static Set<Item> items = Set.of();

    private Config() {
    }
}
