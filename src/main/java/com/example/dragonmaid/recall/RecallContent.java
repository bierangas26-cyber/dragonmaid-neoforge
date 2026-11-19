package com.example.dragonmaid.recall;

import com.example.dragonmaid.DragonMaid;
import com.example.dragonmaid.block.DragonRecallAltarBlock;
import com.example.dragonmaid.item.ModItems;
import com.example.dragonmaid.menu.ModMenus;
import com.example.dragonmaid.menu.RecallMenu;
import com.example.dragonmaid.network.DragonNetwork;
import com.example.dragonmaid.network.RecallRequestMessage;
import com.example.dragonmaid.network.RecallRosterMessage;
import com.example.dragonmaid.block.RecallAltarBlockEntity;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunk.EntityCreationType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import com.example.dragonmaid.compat.net.NetworkDirection;
import net.neoforged.neoforge.registries.DeferredHolder;
import com.example.dragonmaid.compat.RO;
import com.example.dragonmaid.compat.Reg;

public final class RecallContent {
    private static final Reg<BlockEntityType<?>> BLOCK_ENTITIES = Reg.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, DragonMaid.MODID);
    /** 1.2.7: recall altar block entity (registered lazily so the block is already registered). */
    public static final RO<BlockEntityType<?>> ALTAR_ENTITY = BLOCK_ENTITIES.register(
        "dragon_recall_altar",
        () -> BlockEntityType.Builder.of(RecallAltarBlockEntity::new, (Block) DragonMaid.DRAGON_RECALL_ALTAR.get()).build(null)
    );

    private RecallContent() {
    }

    public static RO<BlockEntityType<?>> altarEntity() {
        return ALTAR_ENTITY;
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
        // 1.2.7: the altar block entity needs the mod event bus; DragonMaid.setup() only hands us the container.
        BLOCK_ENTITIES.register(modContainer.getEventBus());
        // 1.2.7: back-fill altars that already exist in old saves (chunk load attaches the block entity).
        NeoForge.EVENT_BUS.addListener(RecallContent::attachExistingAltars);
        RecallCommandArchive.setup();
        modContainer.registerConfig(ModConfig.Type.SERVER, RecallConfig.SPEC, "dragonmaid-recall-sync-server.toml");
        RecallService.setCompletionListener((player, result) -> {
            if (player.containerMenu instanceof RecallMenu recallmenu) {
                recallmenu.completed(result.message());
            }
        });
    }

    private static void attachExistingAltars(ChunkEvent.Load event) {
        if (!(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }

        Block altar = DragonMaid.DRAGON_RECALL_ALTAR.get();
        chunk.findBlocks(state -> state.is(altar), (pos, state) -> chunk.getBlockEntity(pos.immutable(), EntityCreationType.IMMEDIATE));
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
