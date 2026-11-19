package com.example.dragonmaid;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Legacy "asset-stage" marker from the early 1.1.0 NeoForge port.
 *
 * <p>It used to be a second {@code @Mod} entry point with the same mod id as {@link DragonMaid},
 * which made FML construct two mod containers for {@code dragonmaid} (see docs/PORT-GAP-AUDIT.md #11).
 * Since the 1.2.7 port restores the full {@link DragonMaid} entry point, this class is intentionally
 * <b>no longer a {@code @Mod}</b>: {@link DragonMaid} is the only entry point. The class is kept as a
 * harmless marker so older tooling/scripts that mention it still resolve.</p>
 */
public final class PortBootstrap {
    public static final String MODID = DragonMaid.MODID;
    public static final Logger LOGGER = LogUtils.getLogger();

    private PortBootstrap() {
    }
}
