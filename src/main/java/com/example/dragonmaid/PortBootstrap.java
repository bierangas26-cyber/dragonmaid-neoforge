package com.example.dragonmaid;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Minimal NeoForge entry point used for the load / asset-fidelity validation build.
 * The full ported content entry point (DragonMaid) is kept in the source tree but is
 * excluded from this build stage while the Ice and Fire CE integration is still being ported.
 */
@Mod(PortBootstrap.MODID)
public class PortBootstrap {
    public static final String MODID = "dragonmaid";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PortBootstrap(IEventBus modEventBus) {
        LOGGER.info("[dragonmaid] Dragon Maid 1.1.0-beta.2 - NeoForge 1.21.1 port bootstrap (assets stage)");
    }
}
