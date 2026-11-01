package com.example.dragonmaid.recall;

import com.example.dragonmaid.DragonMaid;
import com.example.dragonmaid.block.DragonRecallAltarBlock;
import com.example.dragonmaid.item.ModItems;
import com.example.dragonmaid.menu.ModMenus;
import com.example.dragonmaid.menu.RecallMenu;
import com.example.dragonmaid.network.DragonNetwork;
import com.example.dragonmaid.network.RecallRequestMessage;
import com.example.dragonmaid.network.RecallRosterMessage;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import com.example.dragonmaid.compat.net.NetworkDirection;
import net.neoforged.neoforge.registries.DeferredHolder;
import com.example.dragonmaid.compat.RO;

public final class RecallContent {
    private RecallContent() {
    }

    public static RO<Block> block() {
        return DragonMaid.BLOCKS.register("dragon_recall_altar", DragonRecallAltarBlock::new);
    }

    public static RO<Item> item() {
        return ModItems.ITEMS.register("dragon_recall_altar", () -> new BlockItem((Block)DragonMaid.DRAGON_RECALL_ALTAR.get(), new Properties()));
    }

    public static RO<MenuType<?>> menu() {
        return ModMenus.MENUS.register("recall", () -> IMenuTypeExtension.create(RecallMenu::new));
    }

    public static void setup(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, RecallConfig.SPEC, "dragonmaid-recall-server.toml");
        NeoForge.EVENT_BUS.register(RecallTracking.class);
        NeoForge.EVENT_BUS.register(RecallService.class);
        RecallService.setCompletionListener((player, result) -> {
            if (player.containerMenu instanceof RecallMenu recallmenu) {
                recallmenu.completed(result.message());
            }
        });
    }

    public static void network() {
        DragonNetwork.CHANNEL
            .registerMessage(
                10,
                RecallRosterMessage.class,
                RecallRosterMessage::encode,
                RecallRosterMessage::decode,
                RecallRosterMessage::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
            );
        DragonNetwork.CHANNEL
            .registerMessage(
                11,
                RecallRequestMessage.class,
                RecallRequestMessage::encode,
                RecallRequestMessage::decode,
                RecallRequestMessage::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
            );
    }
}
