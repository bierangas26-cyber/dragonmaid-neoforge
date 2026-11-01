package com.example.dragonmaid.learning;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.example.dragonmaid.DragonMaidConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent.Loading;
import net.neoforged.fml.loading.FMLPaths;

/** Migrates the old knowledge-balance defaults in the server TOML before the spec reads it. */
@EventBusSubscriber(
    modid = "dragonmaid",
    bus = Bus.MOD
)
public final class KnowledgeBalanceMigration {
    private static final String REVISION = "knowledgeBalanceRevision";

    private KnowledgeBalanceMigration() {
    }

    @SubscribeEvent
    public static void onLoading(Loading event) {
        ModConfig modconfig = event.getConfig();
        if (modconfig.getSpec() != DragonMaidConfig.SPEC || modconfig.getType() != ModConfig.Type.SERVER) {
            return;
        }
        Path path = FMLPaths.CONFIGDIR.get().resolve("dragonmaid-server.toml");
        if (!Files.exists(path)) {
            return;
        }
        CommentedFileConfig data = CommentedFileConfig.builder(path).preserveInsertionOrder().build();
        try {
            data.load();
            if (migrate(data)) {
                data.save();
            }
        } catch (RuntimeException ignored) {
            // A malformed/hand-edited config must never block startup.
        } finally {
            data.close();
        }
    }

    public static boolean migrate(CommentedConfig data) {
        Number number = (Number)data.getOrElse("knowledgeBalanceRevision", 0);
        if (number.intValue() >= 2) {
            return false;
        } else {
            if (number.intValue() < 1) {
                replaceOldDefault(data, "manuscriptExperience", 5, 10);
                replaceOldDefault(data, "experienceBottleExperience", 10, 12);
            }

            replaceOldDefault(data, "manuscriptExperience", 10, 8);
            data.set("knowledgeBalanceRevision", 2);
            return true;
        }
    }

    private static void replaceOldDefault(CommentedConfig data, String key, int oldValue, int newValue) {
        if (data.get(key) instanceof Number number && number.doubleValue() == (double)oldValue) {
            data.set(key, newValue);
        }
    }
}
